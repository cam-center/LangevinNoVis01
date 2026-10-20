package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.helpernovis.SolverConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vcell.messaging.VCellMessaging;
import org.vcell.messaging.WorkerEvent;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Watchdog {

    public static final Logger lg = LogManager.getLogger(Watchdog.class);

    private Global g;
    private final VCellMessaging vcellMessaging;
    private int numRuns;        // number of runs
    private final boolean useOutputFile;
    private final long epochStartTimeMS;  // time in milliseconds, any log files older than it are to be considered stale

    // number of concurrent jobs we run effectively, used to compute number of batches
    // since ETA is the duration of one simulation, actually the whole run will be (eta * num batches)
    // ex: 7 jobs in total, 3 concurrent jobs, we have 3 batches: batch 1: jobs 0,1,2; batch 2: jobs 3,4,5; batch 3: job 6
    private final int maxConcurrentJobs;
    private int numBatches;     // see above. Computed once in setup

    // base tick interval in seconds, received as command line argument
    private final int watchdogBaseTickSec;

    // current tick interval, which is adjusted based on the ETA (if available).
    // will default to base tick interval if ETA is not available
    private int watchdogCurrentTickSec = 20;    // feels like a good compromise for the stale loop when there's no ETA yet

    // time we wait for the simulation #1 log file to be created or updated before we consider that something went
    // wrong and exit the watchdog entirely
    private final int watchdogTimeout;

    // used internally in updateProgress()
//    long watchdogStartTime;             // time when the watchdog started, used for timeout calculations
    private int[] latestPercent;        // latest percent for each run
    private long[] lastModifiedSeen;    // last modified timestamp we last processed
    private double lastBatchPercent;    // percent at previous tick

    // true when every run reported 100%, computed in the integer domain to avoid double precision traps
    // (0.999999997 instead of 1.0) - the watchdog exits gracefully when this becomes true
    private boolean allRunsFinished = false;

    // used internally in updateEta()
    private long lastEtaModSeen = 0L;

    private String simulationName;      // model / simulation name (without extension)
    private File simulationFolder;      // top folder, where the input file is (and also the .ida and ,json files are)

    private File etaFile;               // file where the ETA is written by simulation 1

    // flags to indicate if we need to send a progress / ETA event
    // this is the "ready to send" stuff we use in the while loop
    // they are computed / set in updateProgress() and updateEta()
    boolean etaChanged = false;
    boolean progressChanged = false;
    // this is the total duration of the simulation, duration of run #1 multiplied by the number of batches,
    // which is (numRuns / maxConcurrentJobs) rounded up
    // this is not remaining time!
    int estimatedTimeSec = 0;
    double lastProgress = 0.0;

    public Watchdog(Global g, int numRuns, boolean useOutputFile, VCellMessaging vcellMessaging,
                    long watchdogCutoff, int maxConcurrentJobs, int watchdogTick, int watchdogTimeout) {
        this.g = g;
        this.numRuns = numRuns;
        this.useOutputFile = useOutputFile;
        this.vcellMessaging = vcellMessaging;
        this.epochStartTimeMS = watchdogCutoff * 1000L;  // convert seconds to milliseconds
        this.maxConcurrentJobs = maxConcurrentJobs;
        this.watchdogBaseTickSec = watchdogTick;
        this.watchdogTimeout = watchdogTimeout;
    }

    public int getNumRuns() {                   // getters
        return numRuns;
    }
    public String getSimulationName() {
        return simulationName;
    }
    public File getSimulationFolder() {
        return simulationFolder;
    }
    public void setNumRuns(int numRuns) {       // setters
        this.numRuns = numRuns;
    }
    public void setSimulationName(String simulationName) {
        this.simulationName = simulationName;
    }
    public void setSimulationFolder(File simulationFolder) {
        this.simulationFolder = simulationFolder;
    }

    public void setup() {

        // we just inform the user that the watchdog started, but we do not use watchdog start time because of potential
        // race condition with start time of the simulations (there is no guarantee that the watchdog starts first)
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String dateTime = sdf.format(new Date(System.currentTimeMillis()));
        lg.info("Watchdog started at " + dateTime + " (watchdogTick=" + watchdogBaseTickSec + " seconds, watchdogTimeout=" + watchdogTimeout + " seconds)");
        lg.info("Watchdog: Single source of truth for start time of current slurm job is epochStartTimeMS = " + epochStartTimeMS + " ms");
        String epochStartDateTime = sdf.format(new Date(epochStartTimeMS));
        lg.info("Watchdog: Epoch start time (formatted): " + epochStartDateTime);

        numBatches = (numRuns + maxConcurrentJobs - 1) / maxConcurrentJobs;   // round up division (integer ceiling)
        lg.info("Watchdog: " + numRuns + " runs, " + maxConcurrentJobs + " concurrent -> " + numBatches + " batch(es)");

        latestPercent = new int[numRuns];       // initialize progress counters
        lastModifiedSeen = new long[numRuns];
        lastBatchPercent = 0;

        simulationFolder = g.getDefaultFolder();

        File inputFile = g.getInputFile();      // model / simulation input file (the .langevininput file)
        simulationName = inputFile.getName();
        int dotIndex = simulationName.lastIndexOf('.');     // Strip extension off the file name
        if (dotIndex > 0) {
            simulationName = simulationName.substring(0, dotIndex);
        } else {
            throw new IllegalArgumentException("Input file name must have an extension: '" + simulationName + "'");
        }

        etaFile = new File(simulationFolder, simulationName + ".eta");

        lg.info("Watchdog: Working folder : " + simulationFolder.getAbsolutePath());
        lg.info("Watchdog: Simulation name: " + simulationName);
        lg.info("Watchdog: Input file name: " + inputFile.getName());
        lg.info("Watchdog: ETA file name  : " + etaFile.getName());   // ex: SimID_123456789_0_.eta
    }


    public void doWork() {

        lg.info("Watchdog entering doWork()");

        long timeoutMillis = watchdogTimeout * 1000L;
        File log1 = new File(simulationFolder, simulationName + "1.log");

        boolean notifiedStaleLog = false;   // we only notify once about stale log files, to avoid spamming the log
        boolean notifiedWaitingForFreshLog = false;   // we only notify once about waiting for fresh log files, to avoid spamming the log
        //
        //  we may be running into a race condition where slurm may start the watchdog after sim 1 is already running,
        //  and the log file may be created before the watchdog starts! (and it will look stale until next 1% update,
        //  which may be hours later)
        //
        // we are going to use a slurm epoch time, acquired at the start of the slurm job, and we will use it as
        // authoritative to determine if a log file is stale or not. If the log file is older than the epoch time,
        // we assume that it belongs to a previous run and we ignore it.
        //
        // langevin_x64 simulate --output-log=/simdata/danv/SimID_327174218_0_6.log /simdata/danv/SimID_327174218_0_.langevinInput 6 -tid 0
        // langevin_x64 simulate --output-log=/simdata/danv/SimID_327174218_0_1.log /simdata/danv/SimID_327174218_0_.langevinInput 1 -tid 0
        // langevin_x64 watchdog /simdata/danv/SimID_327174218_0_.langevinInput 7 --watchdog-tick=30 --watchdog-timeout=600 --vc-send-status-config=/simdata/danv/SimID_327174218_0_.langevinMessagingConfig
        // langevin_x64 simulate --output-log=/simdata/danv/SimID_327174218_0_2.log /simdata/danv/SimID_327174218_0_.langevinInput 2 -tid 0
        // ...

        // first while loop, we look for the log file for run #1 to be created, meaning that slurm started launching simulation tasks
        // ignore stale logs from previous runs
        while (true) {
            // the first log file, for simulation 1 should be created very soon, although it will be empty
            // for very long simulation (may take 1 week!) the first 1% advance may take hours though
            long elapsed = System.currentTimeMillis() - epochStartTimeMS;   // elapsed time since the start of the slurm job

            if (log1.exists()) {
                long lastModified = log1.lastModified();

                if (lastModified >= epochStartTimeMS) {
                    lg.info("Watchdog: Found fresh log file for run #1: " + log1.getAbsolutePath()    // found fresh log file!
                            + " (lastModified=" + lastModified + ")");
                    break;   // proceed to second loop
                } else {
                    if(!notifiedStaleLog) {
                        lg.warn("Watchdog: Ignoring stale log file for run #1: " + log1.getAbsolutePath()
                                + " (lastModified=" + lastModified
                                + ", epochStartTimeMS=" + epochStartTimeMS + ")");
                        notifiedStaleLog = true;
                    }
                }
            }
            if(!notifiedWaitingForFreshLog) {
                lg.info("Watchdog: Waiting for fresh log file: " + log1.getAbsolutePath()
                        + " (elapsed time: " + (elapsed / 1000L) + " seconds)");
                notifiedWaitingForFreshLog = true;
            }

            if (elapsed >= timeoutMillis) {
                lg.error("Watchdog: Timeout waiting for fresh log file: " + log1.getAbsolutePath());
                throw new RuntimeException("Watchdog timeout: fresh log file not found");
            }

            // Sleep once per tick
            try {
                Thread.sleep(watchdogCurrentTickSec * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Watchdog interrupted: ", e);
            }
        }
        // --------------------------------------------------------------------------------
        // now we start a new loop where we check for progress and ETA.
        // at watchdog tick intervals, we check the log file for new lines.
        // If we don't see any new lines we just send a worker alive event
        // vcellMessaging.sendWorkerEvent(WorkerEvent.workerAliveEvent(...
        // if we see any new lines we calculate progress and send progress event
        // vcellMessaging.sendWorkerEvent(WorkerEvent.progressEvent(.......), VCellMessaging.ThrowOnException.NO);
        // --------------------------------------------------------------------------------
        lg.info("Watchdog entering progress and ETA monitoring loop for simulation: " + simulationName);
        vcellMessaging.sendWorkerEvent(WorkerEvent.progressEvent(0.0, System.currentTimeMillis() - epochStartTimeMS), VCellMessaging.ThrowOnException.NO);
        long lastTick = System.currentTimeMillis();
        boolean watchdogCurrentTickAdjusted = false;
        watchdogCurrentTickSec = chooseAdaptiveTick(0); // shortest initial tick interval, will be adjusted based on ETA if available
        while (true) {

            long now = System.currentTimeMillis();
            long elapsedSinceLastTick = now - lastTick;
            // we get timestamps from the solvers, but this one is authoritative to ensure monotonicity
            long elapsedSinceStart = now - epochStartTimeMS;

            // Log that we are alive inside the second loop
            lg.info("Watchdog progress loop tick — elapsed " + (elapsedSinceLastTick / 1000) + " seconds since last tick");

            etaChanged = false;
            progressChanged = false;
            lastTick = now;     // reset tick timer

            etaChanged = updateEta();
            progressChanged = updateProgress();

            // adaptive tick interval adjustment based on ETA, if available - or reset to base tick if not
            if(etaChanged) {
                // if ETA changed, we adjust the watchdog tick interval to be suitable for the estimated total simulation time
                // we don't do this too often, the eta hardcoded schedule has few entries, it's also very unlikely to bucket jump
                // note that estimatedTimeSec is the duration of the whole simulation (run #1 duration multiplied by
                // the number of batches)
                watchdogCurrentTickSec = chooseAdaptiveTick(estimatedTimeSec);
                watchdogCurrentTickAdjusted = true;
                lg.info("Watchdog: ETA changed, adjusting tick interval to " + watchdogCurrentTickSec + " seconds");
            }
            if(elapsedSinceStart >= timeoutMillis && watchdogCurrentTickAdjusted == false) {
                // no eta was ever computed in quite a long time, we assume something went wrong and
                // we reset the watchdog tick interval to the base tick interval
                lg.info("Watchdog: Timeout waiting for an ETA update, defaulting to base tick interval of " + watchdogBaseTickSec + " seconds");
                watchdogCurrentTickSec = watchdogBaseTickSec;   // reset to base tick interval
                watchdogCurrentTickAdjusted = true;   // counts as an adjustment; we only need to do this once
            }

            if(etaChanged || progressChanged) {
                // we send both progress and ETA in the same event if either changed
                vcellMessaging.sendWorkerEvent(WorkerEvent.progressEvent(lastProgress, estimatedTimeSec, elapsedSinceStart), VCellMessaging.ThrowOnException.NO);
            }

            // the tick where the last run reaches 100% is also a progress change, so the 1.0 event was just sent above
            if(allRunsFinished) {
                lg.info("Watchdog: all " + numRuns + " runs reached 100%, exiting gracefully");
                return;     // exit the watchdog gracefully
            }

            try {                   // --------------------------- sleep for watchdogTick seconds
                Thread.sleep(watchdogCurrentTickSec * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lg.warn("Watchdog interrupted during progress loop");
                return;   // allow test to kill the watchdog cleanly
            }
        }
    }

    // we choose a watchdog tick suitable for the estimated total simulation time,
    // so we don't sample too often the filesystem for long simulations where each 1% progress may take hours and
    // nothing happens in between
    private int chooseAdaptiveTick(int totalSec) {
        if (totalSec < 600)       return 10;    // < 10 minutes, tick every 10 seconds
        if (totalSec < 1800)      return 15;    // < 30 minutes, tick every 15 seconds
        if (totalSec < 3600)      return 20;    // < 1 hour, tick every 20 seconds
        if (totalSec < 7200)      return 30;    // < 2 hours, tick every 30 seconds
        if (totalSec < 14400)     return 60;    // < 4 hours, tick every 60 seconds
        // anything longer, we tick every 2 minutes, which is a nice compromise between low spamming and responsiveness
        // for very long sims we won't spam the messaging system too often anyway,
        // since we only fire events when progress changes
        return 120;
    }


    // -----------------------------------------------------------------------------------
    // this is where we read the ETA file and update the lastEta value
    // -----------------------------------------------------------------------------------
    private boolean updateEta() {

        if (etaFile == null || !etaFile.exists()) {     // If ETA file does not exist, nothing to do
            return false;
        }
        long lastMod = etaFile.lastModified();
        if (lastMod < epochStartTimeMS) {              // Ignore stale ETA (from previous runs)
            return false;
        }
        if (lastMod <= lastEtaModSeen) {                // No new ETA since last tick
            return false;
        }

        // --- Read only the tail of the file (like the log parser) ---
        String lastLine = null;
        try (RandomAccessFile raf = new RandomAccessFile(etaFile, "r")) {

            long fileLength = raf.length();
            long seekPos = Math.max(0, fileLength - 2048);   // read last ~2KB
            raf.seek(seekPos);

            String line;
            while ((line = raf.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lastLine = line.trim();   // keep overwriting -> last non-empty line
                }
            }

        } catch (Exception ex) {
            lg.warn("Failed to read ETA file tail " + etaFile + ": " + ex.getMessage());
            return false;
        }

        if (lastLine == null) {
            return false;   // empty file
        }

        // --- Parse the last ETA record ---
        // Format: totalSec=123, confidence=850, timestampSec=1727890
        int totalSec = -1;
        int confidence = -1;
        int timestampSec = -1;

        try {
            String[] parts = lastLine.split(",");
            for (String part : parts) {
                String[] kv = part.trim().split("=");
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String val = kv[1].trim();

                switch (key) {
                    case SolverConstants.EtaEstimatedTotalSec:
                        totalSec = Integer.parseInt(val);
                        break;
                    case SolverConstants.EtaConfidence:
                        confidence = Integer.parseInt(val);
                        break;
                    case SolverConstants.EtaTimestampSec:
                        timestampSec = Integer.parseInt(val);
                        break;
                }
            }
        } catch (Exception ex) {
            lg.warn("Failed to parse ETA line '" + lastLine + "': " + ex.getMessage());
            return false;
        }

        // Validate
        if (totalSec <= 0 || timestampSec <= 0) {
            lg.warn("Invalid ETA record: " + lastLine);
            return false;
        }

        // Update internal state
        // totalSec is the duration of a single run (run #1); the whole job runs numBatches batches sequentially
        long batchTotalSec = (long) totalSec * numBatches;
        estimatedTimeSec = (int) Math.min(batchTotalSec, Integer.MAX_VALUE);   // this is what we send in the event
        lastEtaModSeen = lastMod;
        lg.info(" ETA changed: singleRunSec=" + totalSec + ", numBatches=" + numBatches +
                ", estimatedTotalSec=" + estimatedTimeSec +
                ", confidence=" + confidence +
                ", timestampSec=" + timestampSec);

        return true;    // ETA changed
    }

    // -----------------------------------------------------------------------------------------------------
    // this is where we read the log files and compute the overall progress percentage for the batch of runs
    // -----------------------------------------------------------------------------------------------------
    private boolean updateProgress() {

        int sum = 0;

        for (int i = 0; i < numRuns; i++) {

            Path logFile = new File(simulationFolder, simulationName + i + ".log").toPath();
            File f = logFile.toFile();

            // If file does not exist, treat as 0%
            if (!f.exists()) {
                latestPercent[i] = 0;
                sum += 0;
                continue;
            }

            long lastMod = f.lastModified();
            // Ignore stale logs, we get here if there's a fresh log for run index 1, but we can't be sure about the others
            if (lastMod < epochStartTimeMS) {
                latestPercent[i] = 0;
                sum += 0;
                continue;
            }

            // Only re-parse if file changed since last tick
            if (lastMod > lastModifiedSeen[i]) {
                try {
                    int ret = extractLatestPercent(logFile);
                    // Only accept forward progress, progress must be monotonic and error safe
                    if (ret >= 0 && ret <= 100 && ret > latestPercent[i]) {
                        latestPercent[i] = ret;
                        lastModifiedSeen[i] = lastMod;
                    }
                } catch (Exception e) {
                    lg.warn("Failed to parse log file " + logFile + ": " + e.getMessage());
                    // do not change latestPercent[i] or lastModifiedSeen[i]
                    // that equals a progress regression, we just ignore this tick and keep the previous value
                }
            }
            sum += latestPercent[i];
        }
        allRunsFinished = (sum == 100 * numRuns);   // integer check, see comment at declaration

        double batchPercent = (double)sum / (double)numRuns;

        // Compare with previous tick
        if (batchPercent != lastBatchPercent) {
            lg.info(String.format("Batch progress changed (progressEvent): %.6f%% -> %.6f%%", lastBatchPercent, batchPercent));
            lastBatchPercent = batchPercent;        // this is used internally, here in updateProgress()
            lastProgress = lastBatchPercent/100;    // this is what we send in the event
            return true;  // progress changed

        } else {
            // reducing the spam for now by commenting out useless message
//            lg.info(String.format("Batch progress unchanged (workerAliveEvent) at %.6f%%", lastBatchPercent));
            return false;  // no progress change
        }
    }

    /*
     * Returns the most recent progress percentile from the logfile
     * Returns 0 on error, but we enforce a monotonic rule in the caller, so if progress percentile regresses
     * we just ignore it and keep the previous value
     */
    private int extractLatestPercent(Path logFile) {

        File f = logFile.toFile();
        long len = f.length();
        if (len <= 0) {
            return 0;
        }

        // Read only the last 512 bytes for speed, that's more than enough because
        // the logfile is short, we have one entry (line) for each percent of progress, 100 lines in total,
        // each line looking something like this: "Simulation 25% complete. Elapsed time: 520.906 sec."
        int readSize = (int) Math.min(len, 512);
        byte[] buf = new byte[readSize];

        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.seek(len - readSize);
            raf.readFully(buf);
        } catch (Exception e) {
            return 0;
        }

        String tail = new String(buf, StandardCharsets.UTF_8);

        // Find the last '%' in the tail
        int pctIdx = tail.lastIndexOf('%');
        if (pctIdx < 0) {
            return 0;
        }

        // Find the nearest "Simulation " BEFORE that '%' (searching backward from pctIdx)
        int simIdx = tail.lastIndexOf("Simulation ", pctIdx);
        if (simIdx < 0) {
            return 0;
        }

        // Extract the substring between "Simulation " and "%"
        int start = simIdx + "Simulation ".length();
        if (start >= pctIdx) {
            return 0; // malformed or partial write
        }

        String numStr;
        try {
            numStr = tail.substring(start, pctIdx).trim();
        } catch (StringIndexOutOfBoundsException e) {
            return 0;   // malformed tail, partial write, or misaligned indices
        }
        if (numStr.length() == 0) {
            return 0;
        }

        // Validate digits only
        for (int i = 0; i < numStr.length(); i++) {
            char c = numStr.charAt(i);
            if (c < '0' || c > '9') {
                return 0;
            }
        }

        try {
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return 0;
        }
    }

}

/*
consider to normal work flow, where at every tick we'll look for all the log files that belong to this simulation, some of them may be not present yet (remember we run totalJobs tasks, numTasks at a time). We read all present, the logs look something like:
Simulation 1% complete. Elapsed time: 0.392 sec.
Simulation 2% complete. Elapsed time: 0.769 sec.
Simulation 3% complete. Elapsed time: 1.151 sec.
Simulation 4% complete. Elapsed time: 1.549 sec.
Simulation 5% complete. Elapsed time: 1.961 sec.
...
So, the last line tells us where the sim is, like 56% complete - so it's actually 56% / totalJobs, so if we have 2 log files at 56% and 72$ out of 4 totalJobs, progress is 56/4 + 72/4 = 14 + 18 = 32%
Also keep in mind that a simulation may take a week, so the first 1% may be written to the log hours after we start monitoring.
So, we'll change the logic: at every tick we'll send a WORKING notification:
vcellMessaging.sendWorkerEvent(WorkerEvent.workerAliveEvent(...
 we'll compute progress by checking all log files. If there is progress we send a PROGRESS notification:
vcellMessaging.sendWorkerEvent(WorkerEvent.progressEvent(.......), VCellMessaging.ThrowOnException.NO); // progress message are throttled by vcellMessaging
 */