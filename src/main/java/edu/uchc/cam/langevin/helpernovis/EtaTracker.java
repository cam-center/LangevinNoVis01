package edu.uchc.cam.langevin.helpernovis;

import edu.uchc.cam.langevin.langevinnovis01.MySystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EtaTracker {

    public static final Logger lg = LogManager.getLogger(MySystem.class);

    // ----------------------------------------------------------------------
    // Configurable parameters (with defaults)
    // ----------------------------------------------------------------------
    private static long DEFAULT_ETA_LOGGING_CUTOFF_MS = 60 * 60 * 1000L; // 1 hour
    private long etaLoggingCutoffMs;        // we stop eta computing after cutoff (default: 1 hour)
    private int[] etaScheduleSeconds = {    // hardcoded schedule of when to log ETA (in seconds) configurable by user
            1,2,3,4,5,6,7,8,9,10,
            20,30,40,50,60
    };
    private boolean useDefaultSchedule = true;  // after hardcoded schedule ends, continue ETA logging?
    private long defaultScheduleIntervalMs = 60_000L;   // default schedule interval (1 minute) after hardcoded schedule ends

    // ----------------------------------------------------------------------
    // Internal scheduling state
    // ----------------------------------------------------------------------
    private int etaScheduleIndex = 0;
    private long nextEtaTimeMs;
    private long startTimeMs;

    // ----------------------------------------------------------------------
    // Iteration timing statistics (Welford)
    // ----------------------------------------------------------------------
    private long iterCount = 0;
    private long minIterTime = Long.MAX_VALUE;
    private long maxIterTime = Long.MIN_VALUE;
    private double meanIterTime = 0.0;
    private double m2 = 0.0;

    // ----------------------------------------------------------------------
    // Last ETA snapshot
    // ----------------------------------------------------------------------
    private long lastEtaTimeMs = -1;
    private long lastEtaTotalNs = -1;
    private long lastEtaRemainingNs = -1;
    private long lastEtaLowNs = -1;
    private long lastEtaHighNs = -1;
    private double lastEtaProgress = -1.0;

    // ----------------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------------
    public EtaTracker() {
        // Nothing to do here
    }

    // ----------------------------------------------------------------------
    // Initialize tracker for a new run
    // ----------------------------------------------------------------------
    public void initialize(long startTimeMs) {

        this.startTimeMs = startTimeMs;
        this.etaLoggingCutoffMs = startTimeMs + DEFAULT_ETA_LOGGING_CUTOFF_MS;

        // Reset stats
        iterCount = 0;
        minIterTime = Long.MAX_VALUE;
        maxIterTime = Long.MIN_VALUE;
        meanIterTime = 0.0;
        m2 = 0.0;

        // Reset last (most recent) snapshot
        lastEtaTimeMs = -1;
        lastEtaTotalNs = -1;
        lastEtaRemainingNs = -1;
        lastEtaLowNs = -1;
        lastEtaHighNs = -1;
        lastEtaProgress = -1.0;

        this.etaScheduleIndex = 0;

        // Compute next ETA time based on schedule
        if (etaScheduleSeconds == null || etaScheduleSeconds.length == 0) { // hardcoded schedule may be null or empty
            if (useDefaultSchedule) {                                       // default schedule may be disabled
                nextEtaTimeMs = startTimeMs + defaultScheduleIntervalMs;
            } else {
                nextEtaTimeMs = Long.MAX_VALUE;   // no hardcoded schedule, no default schedule: disable ETA entirely
            }
        } else {
            nextEtaTimeMs = startTimeMs + etaScheduleSeconds[0] * 1000L;    // start with first entry in the hardcoded schedule
        }
        lg.info("ETA logging initialized. 'nextEtaTimeMs' = " + (nextEtaTimeMs - startTimeMs)  + "ms after start time.");
    }


    // ----------------------------------------------------------------------
    // Setters for tests
    // ----------------------------------------------------------------------
    public void setEtaLoggingCutoffMs(long cutoffMs) {
        this.DEFAULT_ETA_LOGGING_CUTOFF_MS = cutoffMs;
    }
    public void setEtaScheduleSeconds(int[] schedule) {
        this.etaScheduleSeconds = schedule;     // schedule may be null
    }
    public void setUseDefaultSchedule(boolean useDefault) {
        this.useDefaultSchedule = useDefault;
    }
    public void setDefaultScheduleIntervalMs(long intervalMs) {
        this.defaultScheduleIntervalMs = intervalMs;
    }

    // ----------------------------------------------------------------------
    // Update iteration statistics
    // ----------------------------------------------------------------------
    public void updateIterationStats(long iterDurationNs) {
        iterCount++;

        if (iterDurationNs < minIterTime) minIterTime = iterDurationNs;
        if (iterDurationNs > maxIterTime) maxIterTime = iterDurationNs;

        double delta = iterDurationNs - meanIterTime;
        meanIterTime += delta / iterCount;
        double delta2 = iterDurationNs - meanIterTime;
        m2 += delta * delta2;
    }

    // ----------------------------------------------------------------------
    // Maybe log ETA (if schedule and cutoff allow)
    // ----------------------------------------------------------------------
    public void maybeLog(long nowMs, int totalSteps, Logger lg) {
        if (nowMs < nextEtaTimeMs || nowMs > etaLoggingCutoffMs) {
            return;
        }
        if (iterCount <= 10) {  // prevents garbage ETA estimates during warmup phase of the simulation
            return;
        }

        // Compute variance and confidence interval
        double variance = (iterCount > 1) ? (m2 / (iterCount - 1)) : 0.0;
        double stddev = Math.sqrt(variance);
        double ci = 2.0 * stddev; // ~95% confidence band

        // Compute ETA snapshot directly into last... fields
        lastEtaTimeMs = nowMs;
        lastEtaProgress = (double)iterCount / totalSteps;

        lastEtaTotalNs = (long)(meanIterTime * totalSteps);
        lastEtaRemainingNs = (long)(meanIterTime * (totalSteps - iterCount));
        lastEtaLowNs = (long)((meanIterTime - ci) * (totalSteps - iterCount));
        lastEtaHighNs = (long)((meanIterTime + ci) * (totalSteps - iterCount));

        // Log ETA
        lg.info("ETA @ " + ((nowMs - startTimeMs)/1000) + "s: " + iterCount + " iterations completed, " +
                "Total estimated run time = " + IOHelp.formatNanoseconds(lastEtaTotalNs) +
                ", remaining=" + IOHelp.formatNanoseconds(lastEtaRemainingNs) +
                ", CI=[" + IOHelp.formatNanoseconds(lastEtaLowNs) + " .. " +
                IOHelp.formatNanoseconds(lastEtaHighNs) + "]");

        advanceSchedule();
    }

    // ----------------------------------------------------------------------
    // Advance schedule (compute when next ETA log should occur)
    // ----------------------------------------------------------------------
    private void advanceSchedule() {
        if (etaScheduleSeconds == null || etaScheduleSeconds.length == 0) {
            if (useDefaultSchedule) {
                nextEtaTimeMs += defaultScheduleIntervalMs;
            } else {
                nextEtaTimeMs = Long.MAX_VALUE; // disable ETA entirely
            }
            return;
        }
        if (etaScheduleIndex < etaScheduleSeconds.length - 1) {
            etaScheduleIndex++;
            nextEtaTimeMs = startTimeMs + etaScheduleSeconds[etaScheduleIndex] * 1000L;
        } else {
            if (useDefaultSchedule) {
                nextEtaTimeMs += defaultScheduleIntervalMs;
            } else {
                // Stop ETA logging entirely
                nextEtaTimeMs = Long.MAX_VALUE;
            }
        }
        lg.info("Advance schedule: next ETA logging scheduled at " + ((nextEtaTimeMs - startTimeMs)/1000) + "s after start time.");
    }

    // ----------------------------------------------------------------------
    // Getters for last snapshot
    // ----------------------------------------------------------------------
    public long getLastEtaTimeMs() { return lastEtaTimeMs; }
    public long getLastEtaTotalNs() { return lastEtaTotalNs; }
    public long getLastEtaRemainingNs() { return lastEtaRemainingNs; }
    public long getLastEtaLowNs() { return lastEtaLowNs; }
    public long getLastEtaHighNs() { return lastEtaHighNs; }
    public double getLastEtaProgress() { return lastEtaProgress; }

    // Optional: getters for statistics (useful for debugging or CSV output)
    public long getIterCount() { return iterCount; }
    public long getMinIterTime() { return minIterTime; }
    public long getMaxIterTime() { return maxIterTime; }
    public double getMeanIterTime() { return meanIterTime; }
    public double getVariance() { return (iterCount > 1) ? (m2 / (iterCount - 1)) : 0.0; }
}
