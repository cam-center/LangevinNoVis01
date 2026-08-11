package edu.uchc.cam.langevin.langevinnovis01;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vcell.messaging.VCellMessaging;

import java.io.File;

public class Watchdog {

    public static final Logger lg = LogManager.getLogger(ConsolidationPostprocessor.class);

    private Global g;
    private final VCellMessaging vcellMessaging;
    private int numRuns;        // number of runs
    private final boolean useOutputFile;
    private final int watchdogTick;
    private final int watchdogTimeout;

    private String simulationName;          // model / simulation name (without extension)
    private File simulationFolder;          // top folder, where the input file is (and also the .ida and ,json files are)


    public Watchdog(Global g, int numRuns, boolean useOutputFile, VCellMessaging vcellMessaging,
                    int watchdogTick, int watchdogTimeout) {
        this.g = g;
        this.numRuns = numRuns;
        this.useOutputFile = useOutputFile;
        this.vcellMessaging = vcellMessaging;
        this.watchdogTick = watchdogTick;
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

        lg.info("Watchdog started");
        // <editor-fold defaultstate="collapsed" desc="Method Code">
        File inputFile = g.getInputFile();      // model / simulation input file (the .langevininput file)
        simulationName = inputFile.getName();
        lg.info("Watchdog analyzing input file for simulation: " + simulationName);

        String filePath = inputFile.getAbsolutePath();
        filePath = filePath.substring(0, filePath.length() - simulationName.length());  // Strip the file name off of the path
        simulationFolder = new File(filePath);

        int dotIndex = simulationName.lastIndexOf('.');     // Strip extension off the file name
        if (dotIndex > 0) {
            simulationName = simulationName.substring(0, dotIndex);
        } else {
            throw new IllegalArgumentException("Input file name must have an extension: '" + simulationName + "'");
        }
    }


    public void doWork() {

        lg.info("Watchdog entering doWork()");

        long start = System.currentTimeMillis();
        long timeoutMillis = watchdogTimeout * 1000L;
        File log0 = new File(simulationFolder, simulationName + "_0.log");

        // first while loop, we look for the first log file to be created, meaning that slurm started launching simulation tasks
        while (true) {
            // the first log file, for simulation 0 should be created very soon, although it will be empty
            // for very long simulation (may take 1 week!) the first 1% advance may take hours though
            if (log0.exists()) {
                lg.info("Found log file for run 0: " + log0.getAbsolutePath());
                break;
            }
            long elapsed = System.currentTimeMillis() - start;
            lg.info("Waiting for log file: " + log0.getAbsolutePath() + " (elapsed time: " + elapsed / 1000L + " seconds)");
            if (elapsed >= timeoutMillis) {
                lg.error("Timeout waiting for log file: " + log0.getAbsolutePath());
                throw new RuntimeException("Watchdog timeout: log file not found");
            }
            try {
                Thread.sleep(watchdogTick * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Watchdog interrupted", e);
            }
        }

        // now we start a new loop where we check for progress.
        // at watchdog tick intervals, we check the log file for new lines.
        // If we don't see any new lines we just send a worker alive event
        // vcellMessaging.sendWorkerEvent(WorkerEvent.workerAliveEvent(...
        // if we see any new lines we calculate progress and send progress event
        // vcellMessaging.sendWorkerEvent(WorkerEvent.progressEvent(.......), VCellMessaging.ThrowOnException.NO);
        lg.info("Watchdog entering progress monitoring loop for simulation: " + simulationName);
        long lastTick = System.currentTimeMillis();
        while (true) {
            // we do not timeout this! it's slurm's job to take us out of the whole script
            // we really have no good way of knowing when the first percent of the simulation may be done
            // although we can have an educated guess from the job timeout in seconds: if we divide that by 100 we
            // can get an estimate of how long the first percent may take

            long now = System.currentTimeMillis();
            long elapsed = (now - lastTick) / 1000L;

            // Log that we are alive inside the second loop
            lg.info("Watchdog progress loop tick — elapsed " + elapsed + " seconds since last tick");

            // Reset tick timer
            lastTick = now;

            // Sleep for watchdogTick seconds
            try {
                Thread.sleep(watchdogTick * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lg.warn("Watchdog interrupted during progress loop");
                return;   // allow test to kill the watchdog cleanly
            }

            // STUB: no progress parsing yet
            // Later we will:
            //   - read new lines from log0
            //   - detect progress markers
            //   - send workerAlive or progress events

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