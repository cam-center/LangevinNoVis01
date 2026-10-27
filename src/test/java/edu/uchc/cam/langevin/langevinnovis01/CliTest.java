package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.cli.CliMain;
import edu.uchc.cam.langevin.helpernovis.FileMapper;
import edu.uchc.cam.langevin.helpernovis.SolverResultSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class CliTest {

    String inputFileContents =
            """
            *** TIME INFORMATION ***
            Total time: 0.0025
            dt: 1.0E-8
            dt_data: 5.0E-4
            dt_spring: 1.0E-9
            dt_image: 1.0E-4
            
            *** SYSTEM INFORMATION ***
            L_x: 0.1
            L_y: 0.1
            L_z_out: 0.01
            L_z_in: 0.09
            Partition Nx: 10
            Partition Ny: 10
            Partition Nz: 10
            
            *** MOLECULES ***
            
            MOLECULE: "MT0" Intracellular Number 20 Site_Types 2 Total_Sites 2 Total_Links 1 is2D false
            {
                 TYPE: Name "Site0" Radius 1.00000 D 1.000 Color RED STATES "state0"
                 TYPE: Name "Site1" Radius 1.00000 D 1.000 Color RED STATES "state0" "state1"
            
                 SITE 0 : Intracellular : Initial State 'state0'
                      TYPE: Name "Site0" Radius 1.00000 D 1.000 Color RED STATES "state0"
                      x 0.00000 y 0.00000 z 0.00000
                 SITE 1 : Intracellular : Initial State 'state0'
                      TYPE: Name "Site1" Radius 1.00000 D 1.000 Color RED STATES "state0" "state1"
                      x 0.00000 y 0.00000 z 0.00000
            
                 LINK: Site 0 ::: Site 1
            
                 Initial_Positions: Random
            }
            
            *** MOLECULE FILES ***
            
            MOLECULE: MT0 null
            
            *** CREATION/DECAY REACTIONS ***
            
            'MT0' : kcreate  0  kdecay  0
            
            *** STATE TRANSITION REACTIONS ***
            
            'r0' ::     'MT0' : 'Site1' : 'state0' --> 'state1'  Rate 50.0  Condition Free
            
            *** ALLOSTERIC REACTIONS ***
            
            
            *** BIMOLECULAR BINDING REACTIONS ***
            
            'r1'       'MT0' : 'Site0' : 'Any_State'  +  'MT0' : 'Site0' : 'Any_State'  kon  10.0  koff 23.0  Bond_Length 1.0
            
            *** MOLECULE COUNTERS ***
            
            'MT0' : Measure Total Free Bound
            
            *** STATE COUNTERS ***
            
            'MT0' : 'Site0' : 'state0' : Measure Total Free Bound
            'MT0' : 'Site1' : 'state0' : Measure Total Free Bound
            'MT0' : 'Site1' : 'state1' : Measure Total Free Bound
            
            *** BOND COUNTERS ***
            
            'r1' : Counted
            
            *** SITE PROPERTY COUNTERS ***
            
            'MT0' Site 0 : Track Properties true
            'MT0' Site 1 : Track Properties true
            
            *** CLUSTER COUNTERS ***
            
            Track_Clusters: true
            
            *** SYSTEM ANNOTATIONS ***
            
            
            *** MOLECULE ANNOTATIONS ***
            
            
            *** REACTION ANNOTATIONS ***
            
            
            *** SIMULATION OPTIONS ***
            
            RandomSeed: 164200191287356961681
            
            """;

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    String parent_dir = "C:/TEMP/langevin-cli-test";
    String sim_base_name = "sim";
    String temp_dir_name = "test_simulation";
    int runCounter = 0;

// --------------------------------------------------------------------------

//    @Test
//    public void testConsolidation() throws IOException {
//
//        int numRuns = 2;
//
//        String simulationFolderName = parent_dir + File.separator + temp_dir_name;
//        File simulationFolder = new File(simulationFolderName);
//
//        // read the tasks results (,ida files) and make a map where key = run name, value = task results File object
//        Map<String, File> nameToIdaFileMap = FileMapper.getFileMapByName(simulationFolder, sim_base_name, MySystem.IdaFileExtension);
//        nameToIdaFileMap.forEach((name, file) -> System.out.println(name + " -> " + file.getAbsolutePath()));    // show results
//        assertTrue(nameToIdaFileMap.size() == numRuns, "expected size " + numRuns + " but found " + nameToIdaFileMap.size());
////        assertEquals(numRuns, fileMap.size());
//
//        // read the name to Ida file map, key = run index (first index is 0), value = solver result set for the run with that index
//        Map<Integer, SolverResultSet> solverResultSetMap = FileMapper.filesToSolverResultSetMap(sim_base_name, nameToIdaFileMap);
//        solverResultSetMap.forEach((key, resultSet) -> {
//            System.out.println("Key: " + key);
//            System.out.println("Columns: " + resultSet.getColumnDescriptions());
//            System.out.println("Data:");
//            resultSet.getValues().forEach(row -> System.out.println(Arrays.toString(row)));
//        });
//        assertTrue(solverResultSetMap.size() == numRuns, "expected size " + numRuns + " but found " + solverResultSetMap.size());
//
//    }
    @Disabled("Manual-only test; excluded from CI")  // disabled for CI because it takes time
    @Test
    public void testRunAndPostCommand() throws IOException, InterruptedException {

//        Path parentFolder = Paths.get(parent_dir);
//        Path tempDirectory = parentFolder.resolve(temp_dir_name);   // use a convenient location for debugging
//        Files.createDirectories(tempDirectory);

        Path tempDirectory = Files.createTempDirectory(temp_dir_name);        // correct temp location for automatic testing
        Path modelFile = tempDirectory.resolve(sim_base_name+".langevinInput");
        Path logFile_0 = tempDirectory.resolve(sim_base_name+".log");
        Path logFile_1 = tempDirectory.resolve(sim_base_name+"_1.log");
        Path logFile_P = tempDirectory.resolve(sim_base_name+"_P.log");
        Path idaFile_0 = tempDirectory.resolve(sim_base_name+".ida");
        Path idaFile_1 = tempDirectory.resolve(sim_base_name+"_1.ida");
        Path jsonClustersFile_0 = tempDirectory.resolve(sim_base_name+".json");
        Path jsonClustersFile_1 = tempDirectory.resolve(sim_base_name+"_1.json");

        Files.writeString(modelFile, inputFileContents);
//        VCellMessaging vcellMessaging = new VCellMessagingLocal();

        assertEquals(true, modelFile.toFile().exists(), "Model file should exist");

        String[] args = {       // command arguments, run 0
                "simulate",
                modelFile.toFile().getAbsolutePath(),   // Langevin model file
                "0",                               // we absolutely need a run counter of 0 to properly initialize dirs
                "--output-log", logFile_0.toFile().getAbsolutePath(), // Output log file
                "--vc-print-status"                // Enable status printing
        };
        String[] args1 = {       // command arguments, run 1
                "simulate",
                modelFile.toFile().getAbsolutePath(),   // Langevin model file
                "1",
                "--output-log", logFile_1.toFile().getAbsolutePath(), // Output log file
                "--vc-print-status"                // Enable status printing
        };

        int exitCode = -1;
        try {
            CommandLine cmd = new CommandLine(new CliMain());
            exitCode = cmd.execute(args);

            assertEquals(0, exitCode, "Expected command to execute successfully");
            assertEquals(true, logFile_0.toFile().exists(), "Log file 0 should exist");
            assertEquals(true, idaFile_0.toFile().exists(), "ida file 0 should exist");
            assertEquals(true, jsonClustersFile_0.toFile().exists(), "json clusters file 0 should exist");

            exitCode = cmd.execute(args1);
            assertEquals(0, exitCode, "Expected command to execute successfully");
            assertEquals(true, logFile_1.toFile().exists(), "Log file 1 should exist");
            assertEquals(true, idaFile_1.toFile().exists(), "ida file 1 should exist");
            assertEquals(true, jsonClustersFile_1.toFile().exists(), "json clusters file 1 should exist");

            // -------------------------------------------------------------------------------------------------

            // final results after postprocessing
            Path consolidated_Avg = tempDirectory.resolve(sim_base_name + "_Avg" + ".ida");
            Path consolidated_Max = tempDirectory.resolve(sim_base_name + "_Max" + ".ida");
            Path consolidated_Min = tempDirectory.resolve(sim_base_name + "_Min" + ".ida");
            Path consolidated_Std = tempDirectory.resolve(sim_base_name + "_Std" + ".ida");
            Path clusters_Mean = tempDirectory.resolve(sim_base_name + "_clusters_mean" + ".csv");
            Path clusters_Overall = tempDirectory.resolve(sim_base_name + "_clusters_overall" + ".csv");
            Path clusters_Counts = tempDirectory.resolve(sim_base_name + "_clusters_counts" + ".csv");

            String[] argsP = {       // command arguments, postprocessing run
                    "postprocess",
                    modelFile.toFile().getAbsolutePath(),   // Langevin model file
                    "2",        // number of runs
                    "--output-log", logFile_P.toFile().getAbsolutePath(), // Output log file
                    "--vc-print-status"                // Enable status printing
            };
            exitCode = cmd.execute(argsP);
            assertEquals(0, exitCode, "Expected command to execute successfully");
            assertEquals(true, consolidated_Avg.toFile().exists(), consolidated_Avg.toFile().getName() + " should exist");
            assertEquals(true, consolidated_Max.toFile().exists(), consolidated_Max.toFile().getName() + " should exist");
            assertEquals(true, consolidated_Min.toFile().exists(), consolidated_Min.toFile().getName() + " should exist");
            assertEquals(true, consolidated_Std.toFile().exists(), consolidated_Std.toFile().getName() + " should exist");
            assertEquals(true, clusters_Mean.toFile().exists(), clusters_Mean.toFile().getName() + " should exist");
            assertEquals(true, clusters_Overall.toFile().exists(), clusters_Overall.toFile().getName() + " should exist");
            assertEquals(true, clusters_Counts.toFile().exists(), clusters_Counts.toFile().getName() + " should exist");

            File fileToDelete = modelFile.toFile();
            int attempts = 0;
            while (fileToDelete.exists() && attempts < 10) {
                System.gc(); // hint JVM to release file handles
                try {
                    Files.deleteIfExists(modelFile);
                } catch (IOException e) {
                    System.err.println("Failed to delete file, retrying...");
                }
                attempts++;
                Thread.sleep(200);
            }
        } finally {
            // uncomment this for automatic run
            deleteDirectory(tempDirectory.toFile());
            System.out.println("finally!");
        }
    }

    @Test
    public void testInvalidModelFile() {
        // Simulate arguments with an invalid model file path
        String[] args = {
                "simulate",
                "nonexistent_model.txt", // Nonexistent model file
                "0"                      // Run counter
        };

        CommandLine cmd = new CommandLine(new CliMain());
        int exitCode = cmd.execute(args);

        // Verify the exit code (1 means error)
        assertEquals(1, exitCode, "Expected error exit code 1 due to invalid model file");
    }

    //
    // ========================= WATCHDOG TESTS ==========================
    //
    // Remember, the watchdog uses simulation 1
    //

    /*
     * Two simple early tests in one:
     * - verify that we deal properly with missing required argument (should fail early with picocli
     *   returning error code 2)
     * - verify that we deal properly with a missing model file (should fail early with error code 1 during
     *   the execution of WatchCommand.call())
     */
    @Test
    public void testFailingWatchdogMissingArgs() throws Exception {

        // Create temp directory for the test
        Path tempDirectory = Files.createTempDirectory("test_watchdog_fail");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");

        try {

            // first test: we are missing a required parameter, picocli should return an error code 2
            // Build CLI args for watchdog
            String[] args = {
                    "watchdog",
                    modelFile.toString(),
                    "3",
                    // !!! required --watchdog-tick missing, --watchdog-cutoff,  --max-concurrent-jobs
                    "--watchdog-timeout", "20"
            };
            CommandLine cmd = new CommandLine(new CliMain());
            int exitCode = cmd.execute(args);
            // The watchdog should fail early, even before WatchCommand.call() because the required --watchdogTick missing
            assertEquals(2, exitCode, "Watchdog should fail early with code 2 when required parameter is missing");

        } finally {
            deleteDirectory(tempDirectory.toFile());
        }
    }

    @Test
    public void testFailingWatchdogMissingModel() throws Exception {

        // Create temp directory for the test
        Path tempDirectory = Files.createTempDirectory("test_watchdog_fail");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");

        try {

            // second test, model file does not exist
        // IMPORTANT: Do NOT create the model file yet
        assertFalse(modelFile.toFile().exists(), "Model file should NOT exist for this failure test");
        // Build CLI args for watchdog
            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale
            String[] args2 = {
                "watchdog",
                modelFile.toString(),       // !!! nonexistent model file
                "5",                        // valid number of runs
                "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                "--max-concurrent-jobs", "5",  // valid max concurrent jobs
                "--watchdog-tick", "3",     // check every *** seconds
                "--watchdog-timeout", "20"  // give up after *** seconds if no logfile appears at all
        };

        CommandLine cmd2 = new CommandLine(new CliMain());
        int exitCode2 = cmd2.execute(args2);
        // The watchdog should fail early, during the execution of WatchCommand.call() because the model file does not exist
        assertEquals(1, exitCode2, "Watchdog should fail early when model file is missing");

        } finally {
            deleteDirectory(tempDirectory.toFile());
        }

    }

    /*
     * Exercising the --watchdog-timeout argument:
     * For the simple case where simulation 1 never starts, so it never creates a log file
     * There is no stale log file from a previous run of simulation 1
     * Expected behavior is that the watchdog will time out and return non-zero
     */
    @Test
    public void testWatchdogMissingLogs() throws Exception {

        Path tempDirectory = Files.createTempDirectory("test_watchdog_missing_logs");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");

        try {
            // Create the model file (valid)
            Files.writeString(modelFile, inputFileContents);

            // No log files created
            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale
            String[] args = {
                "watchdog",
                modelFile.toString(),
                "3",                        // numRuns
                "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                "--max-concurrent-jobs", "3",  // valid max concurrent jobs
                "--watchdog-tick", "5",     // check every *** seconds
                "--watchdog-timeout", "12"  // give up after *** seconds if no logfile appears at all
            };

            AtomicReference<Exception> watchdogException = new AtomicReference<>();
            CommandLine cmd = new CommandLine(new CliMain());
            // capture the exception thrown inside WatchCommand.call(), so we can verify *why* the watchdog failed
            cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                watchdogException.set(ex);
                return 1;   // same exit code picocli would return by default
            });
            int exitCode = cmd.execute(args);

            // Watchdog should time out waiting for the log of run #1 and return non-zero
            // validation errors in WatchCommand.call() also return non-zero, but without an exception
            assertNotEquals(0, exitCode, "Watchdog should fail due to missing logs");
            assertNotNull(watchdogException.get(), "Watchdog should have failed with an exception, not a validation error");
            System.out.println("Exception is: " + watchdogException.get().getMessage());
            assertEquals(Watchdog.TimeoutFreshLogNotFound, watchdogException.get().getMessage(), "Watchdog should fail because of the log timeout");

        } finally {
            deleteDirectory(tempDirectory.toFile());
        }
    }

    /*
     * Exercising the --watchdog-timeout argument differently:
     * There is only a stale log file for run 1, which means simulation 1 never starts for the current batch run
     * Normally it should start rather quickly and create a fresh log file
     * Nevertheless, we'll give it a generous timeout in production code to account for the fact that slurm may need
     * to delay it a lot if the node is too busy
     * Expected behavior is that the watchdog will time out because the only log file for run 1 is older than the
     * slurm epoch (--watchdog-cutoff), so it is treated as a leftover from a previous run and ignored, and no fresh
     * log file shows up within --watchdog-timeout
     */
    @Test
    public void testWatchdogStaleLog() throws Exception {

        Path tempDirectory = Files.createTempDirectory("test_watchdog_stale_log");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");

        try {
            // Create the model file (valid)
            Files.writeString(modelFile, inputFileContents);

            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale

            // Create a stale log file for run 1
            Path logFile1 = tempDirectory.resolve("SimID_123456789_0_1.log");
            Files.writeString(logFile1, "");   // empty is fine

            // Make the log file stale by setting lastModified to 10 seconds before the slurm epoch
            File staleLog = logFile1.toFile();
            long staleTime = cutoffSec * 1000L - 10_000;   // 10 seconds older than the cutoff
            staleLog.setLastModified(staleTime);

            // make sure the precondition holds, otherwise the test would just repeat testWatchdogMissingLogs
            assertTrue(staleLog.exists() && staleLog.lastModified() < cutoffSec * 1000L, "Log file for run 1 should exist and be stale");

            // Build CLI args
            String[] args = {
                    "watchdog",
                    modelFile.toString(),
                    "3",                        // numRuns
                    "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                    "--max-concurrent-jobs", "3",  // valid max concurrent jobs
                    "--watchdog-tick", "5",     // fallback progress tick, not used while waiting for the fresh log
                    "--watchdog-timeout", "18"  // give up after *** seconds
            };

            AtomicReference<Exception> watchdogException = new AtomicReference<>();
            CommandLine cmd = new CommandLine(new CliMain());
            // capture the exception thrown inside WatchCommand.call(), so we can verify *why* the watchdog failed
            cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                watchdogException.set(ex);
                return 1;   // same exit code picocli would return by default
            });
            int exitCode = cmd.execute(args);

            // Watchdog should time out because the log file is stale
            assertNotEquals(0, exitCode, "Watchdog should fail due to stale log file being ignored");
            assertNotNull(watchdogException.get(), "Watchdog should have failed with an exception, not a validation error");
            System.out.println("Exception is: " + watchdogException.get().getMessage());
            assertEquals(Watchdog.TimeoutFreshLogNotFound, watchdogException.get().getMessage(), "Watchdog should fail because of the log timeout");

        } finally {
            deleteDirectory(tempDirectory.toFile());
        }
    }

    /*
     * There is a stale log file for run 0 but a fresh log file will be created for run 1 after a delay
     * Obviously the delat has to be shorter than the timeout
     * The watchdog should detect the fresh log file and enter the main infinite loop and start recording progress
     * We will eventually interrupt the watchdog to stop the test; an interrupt inside the progress loop makes doWork()
     * return normally, so exit code 0 proves the watchdog really got there (an interrupt in the first loop throws)
     * Note that no --vc-print-status or --vc-send_status-config is used, so the watchdog will use VCellMessagingNoop()
     * and not send any progress messages, like [[[progress:3.0%]]] or [[[alive]]] in the
     * output, we will only see lg.info messages if not commented out in the code (as they will be eventually)
     * The only thing we'll see is the simulation progress messages in the log file, and logger messages from the
     * thread simulating progress, like "log 0++"
     * Since we only write to the logs without really running simulation 1, there is no ETA to be found,
     * so the watchdog will use the fallback --watchdog-tick to compute progress
     */
    @Disabled("Manual-only test; excluded from CI")  // excluded for CI, as it takes time and we only want to visually
                                                     // verify some partial functionality. The next test is a complete
                                                     // exercise of all the functionality and that's the one
                                                     // that should be used for CI
    @Test
    public void testWatchdogEntersProgressLoop() throws Exception {

        Path tempDirectory = Files.createTempDirectory("test_watchdog_progress_loop");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");
        Thread freshLog1Thread = null;      // declared here so that finally can stop it

        try {

            // Create valid model file
            Files.writeString(modelFile, inputFileContents);

            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale

            // Create the stale log file for run 0, a leftover from a previous run; its progress must be ignored
            Path logFile0 = tempDirectory.resolve("SimID_123456789_0_0.log");

            // Write some stale progress messages
            String staleProgress =
                    "Simulation 98% complete. Elapsed time: 0.392 sec.\n" +
                            "Simulation 99% complete. Elapsed time: 0.769 sec.\n" +
                            "Simulation 100% complete. Elapsed time: 1.151 sec.\n";
            Files.writeString(logFile0, staleProgress);

            // Make the log file stale by setting lastModified to 10 seconds before the slurm epoch
            File staleLog = logFile0.toFile();
            staleLog.setLastModified(cutoffSec * 1000L - 10_000);

            // Build CLI args, note no --vc-print-status and no --vc-send_status-config , uses VCellMessagingNoop()
            String[] args = {
                    "watchdog",
                    modelFile.toString(),
                    "3",                        // numRuns
                    "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                    "--max-concurrent-jobs", "3",  // valid max concurrent jobs
                    "--watchdog-tick", "5",     // fallback progress tick, used only if no ETA ever arrives
                    "--watchdog-timeout", "18"  // timeout for first loop (won't be reached, the fresh log for run 1 shows up at 5 s)
            };

            // Run watchdog in a separate thread so that we can kill it
            AtomicInteger watchdogExitCode = new AtomicInteger(-1);    // -1 = still running / never returned
            CommandLine cmd = new CommandLine(new CliMain());
            Thread watchdogThread = new Thread(() -> watchdogExitCode.set(cmd.execute(args)));
            watchdogThread.start();

            // Create a fresh log file in another thread after a delay
            // Remember, we check for run 1, so that we know we are in batch mode and not in single run mode
            // 10 entries at 3 s intervals are more than the 30 s we wait below
            freshLog1Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 1, 10, 5000, 3000);
            freshLog1Thread.start();

            // Let watchdog run long enough to detect the fresh log and enter the infinite loop
            Thread.sleep(30000);

            // At this point, watchdog should be inside the second loop
            assertTrue(watchdogThread.isAlive(), "Watchdog should be running, but it exited with code " + watchdogExitCode.get());

            // Kill the watchdog thread
            watchdogThread.interrupt();
            watchdogThread.join(2000);   // give it 2 seconds to exit

            assertFalse(watchdogThread.isAlive(), "Watchdog thread should have been interrupted and stopped");
            assertEquals(0, watchdogExitCode.get(), "Watchdog should exit cleanly from the progress loop");

        } finally {
            if (freshLog1Thread != null) {
                freshLog1Thread.interrupt();     // stop writing before the folder is deleted
                freshLog1Thread.join(2000);
            }
            deleteDirectory(tempDirectory.toFile());
        }
    }

    /*
     * Here we really exercise the log file parser and the algorithm that detects and computes progress
     * If VcellMessagingLocal works, we should see something like [[[progress:3.0%]]] or [[[alive]]]
     * A few assertions are made to verify that the watchdog is working at all.
     */
    @Test
    public void testWatchdogMonitorsProgress() throws Exception {

        Path tempDirectory = Files.createTempDirectory("test_watchdog_monitors_progress");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");
        Thread freshLog0Thread = null;      // declared here so that finally can stop them
        Thread freshLog1Thread = null;
        Thread freshLog2Thread = null;

        try {

            // Create valid model file
            Files.writeString(modelFile, inputFileContents);

            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale

            // Build CLI args, note --vc-print-status", uses VCellMessagingLocal()
            String[] args = {
                    "watchdog",
                    modelFile.toString(),
                    "3",                        // numRuns
                    "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                    "--max-concurrent-jobs", "3",  // valid max concurrent jobs
                    "--watchdog-tick", "3",     // fallback progress tick, used only if no ETA ever arrives
                    "--watchdog-timeout", "20", // timeout in seconds for first loop (won't be reached, the fresh log for run 1 shows up at 5 s)
                    "--vc-print-status"         // uses: vcellMessaging = new VCellMessagingLocal();
            };

            // Run watchdog in a separate thread so we can kill it
            AtomicInteger watchdogExitCode = new AtomicInteger(-1);    // -1 = still running / never returned
            CommandLine cmd = new CommandLine(new CliMain());
            Thread watchdogThread = new Thread(() -> watchdogExitCode.set(cmd.execute(args)));
            watchdogThread.start();

            // Create multiple log files and keep appending percentage growth
            freshLog0Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 0, 100, 3000, 7000);
            freshLog1Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 1, 100, 5000, 8000);
            freshLog2Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 2, 7, 11000, 9000);
            freshLog0Thread.start();
            freshLog1Thread.start();
            freshLog2Thread.start();

            // Let watchdog run long enough to detect the fresh log and enter the infinite loop
            Thread.sleep(20000);

            // At this point, watchdog should be inside the second loop
            assertTrue(watchdogThread.isAlive(), "Watchdog should be running, but it exited with code " + watchdogExitCode.get());

            // we run some more, then kill the watchdog thread
            Thread.sleep(10000);
            watchdogThread.interrupt();
            watchdogThread.join(2000);   // give it 2 seconds to exit

            assertFalse(watchdogThread.isAlive(), "Watchdog thread should have been interrupted and stopped");
            assertEquals(0, watchdogExitCode.get(), "Watchdog should exit cleanly from the progress loop");

        } finally {
            for (Thread t : new Thread[] {freshLog0Thread, freshLog1Thread, freshLog2Thread}) {
                if (t != null) {
                    t.interrupt();      // stop writing before the folder is deleted
                    t.join(2000);
                }
            }
            deleteDirectory(tempDirectory.toFile());
        }
    }

    /*
     * Same as above, but we capture System.out and check the progress events the watchdog sends
     * VCellMessagingLocal prints each progress event to System.out, like [[[progress:2.6666666666666665%]]]
     * (followed by the estimated total time when an ETA is available); it reads System.out when it is created,
     * after the redirect, so the events are always captured
     * Assertions:
     *   - the watchdog is still running after the first wait, and exits with code 0 when interrupted, which proves
     *     it reached the progress loop (an interrupt in the first loop throws and returns non-zero)
     *   - there are several progress events, they never decrease, and the last one is larger than the first
     * We do NOT assert on the watchdog's lg.info() lines: log4j binds its console appender to System.out once,
     * when it initializes (log4j2.xml has no follow="true"). If an earlier test in the same JVM already logged
     * something, those lines go to the real console instead of the capture, so such assertions would pass or fail
     * depending on test order. This was most likely the cause of the earlier "Mac failure"
     * System.out is restored in finally, otherwise all output of the tests that follow would be lost
     */
    @Disabled("Manual-only test; excluded from CI")     // disabled for CI
    @Test
    public void testWatchdogMonitorsProgressWithCapture() throws Exception {

        Path tempDirectory = Files.createTempDirectory("test_watchdog_monitors_progress");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");
        PrintStream originalOut = System.out;   // restored in finally, otherwise all later output is lost
        Thread freshLog0Thread = null;          // declared here so that finally can stop them
        Thread freshLog1Thread = null;
        Thread freshLog2Thread = null;

        try {

            // Create valid model file
            Files.writeString(modelFile, inputFileContents);

            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale

            // Build CLI args, note --vc-print-status", uses VCellMessagingLocal()
            String[] args = {
                    "watchdog",
                    modelFile.toString(),
                    "3",                        // numRuns
                    "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                    "--max-concurrent-jobs", "3",  // valid max concurrent jobs
                    "--watchdog-tick", "3",     // fallback progress tick, used only if no ETA ever arrives
                    "--watchdog-timeout", "20", // timeout in seconds for first loop (won't be reached, the fresh log for run 1 shows up at 7 s)
                    "--vc-print-status"         // uses: vcellMessaging = new VCellMessagingLocal();
            };

            // Redirect System.out so that we can capture the progress events printed by VCellMessagingLocal
            // note: log4j binds to System.out when it initializes, so lg.info() lines are captured only if log4j
            // was not initialized yet by an earlier test in the same JVM - we don't assert on them
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos, true);
            System.setOut(ps);                      // redirect stdout to this

            // Run watchdog in a separate thread so we can kill it
            AtomicInteger watchdogExitCode = new AtomicInteger(-1);    // -1 = still running / never returned
            CommandLine cmd = new CommandLine(new CliMain());
            Thread watchdogThread = new Thread(() -> watchdogExitCode.set(cmd.execute(args)));
            watchdogThread.start();

            // Create multiple log files and keep appending percentage growth
            freshLog0Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 0, 100, 6000, 7000);
            freshLog1Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 1, 100, 7000, 8000);
            freshLog2Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 2, 7, 11000, 9000);
            freshLog0Thread.start();
            freshLog1Thread.start();
            freshLog2Thread.start();

            // Let watchdog run long enough to detect the fresh log and enter the infinite loop
            Thread.sleep(20000);

            // At this point, watchdog should be inside the second loop
            assertTrue(watchdogThread.isAlive(), "Watchdog should be running, but it exited with code " + watchdogExitCode.get());

            // we run some more, then kill the watchdog thread
            Thread.sleep(20000);
            watchdogThread.interrupt();
            watchdogThread.join(2000);   // give it 2 seconds to exit

            assertFalse(watchdogThread.isAlive(), "Watchdog thread should have been interrupted and stopped");
            assertEquals(0, watchdogExitCode.get(), "Watchdog should exit cleanly from the progress loop");

            // let's see what the cat brought in
            String sysOutText = baos.toString();
            System.err.println("Captured System.out:\n" + sysOutText);

            // every progress event looks like [[[progress:2.6666666666666665%]]] (optionally followed by the ETA)
            List<Double> progressValues = Pattern.compile("\\[\\[\\[progress:([0-9.]+)%").matcher(sysOutText)
                    .results().map(m -> Double.parseDouble(m.group(1))).toList();
            assertTrue(progressValues.size() >= 2, "Expected several progress events, found: " + progressValues);
            for (int i = 1; i < progressValues.size(); i++) {
                assertTrue(progressValues.get(i) >= progressValues.get(i - 1), "Progress must never decrease: " + progressValues);
            }
            assertTrue(progressValues.get(progressValues.size() - 1) > progressValues.get(0),
                    "Progress should have grown while the logs were written: " + progressValues);

        } finally {
            System.setOut(originalOut);
            for (Thread t : new Thread[] {freshLog0Thread, freshLog1Thread, freshLog2Thread}) {
                if (t != null) {
                    t.interrupt();      // stop writing before the folder is deleted
                    t.join(2000);
                }
            }
            deleteDirectory(tempDirectory.toFile());
        }
    }

    /*
     * Here we exercise sending WorkerEvent via REST to ActiveMQ server
     * Obviously it's not going to work as is because we don't have a real ActiveMQ server running
     * We'll get exceptions:
     *    java.net.ConnectException
     *       at java.net.http/jdk.internal.net.http.HttpClientImpl.send(HttpClientImpl.java:573)
     *       at java.net.http/jdk.internal.net.http.HttpClientFacade.send(HttpClientFacade.java:123)
     *       at org.vcell.messaging.VCellMessagingRest.sendWorkerEvent(VCellMessagingRest.java:165)
     *       ...
     *       Exception sending WorkerEvent via REST to ActiveMQ server: null
     */
    @Disabled       // exclude this from any automated run including github; this should be run manually with
                    // a real ActiveMQ server running and the config file pointing to it
    @Test
    public void testWatchdogMessaging() throws Exception {

        Path tempDirectory = Files.createTempDirectory("test_watchdog_monitors_progress");
        Path modelFile = tempDirectory.resolve("SimID_123456789_0_.langevinInput");
        Thread freshLog0Thread = null;      // declared here so that finally can stop them
        Thread freshLog1Thread = null;
        Thread freshLog2Thread = null;

        try {

            // Create valid model file
            Files.writeString(modelFile, inputFileContents);

            // Create a fake VCell messaging config file
            Path configFile = tempDirectory.resolve("vc_config.properties");
            Files.writeString(configFile,
                    "broker_host=localhost\n" +
                            "broker_port=8165\n" +
                            "broker_username=msg_user\n" +
                            "broker_password=msg_pswd\n" +
                            "vc_username=vcell_user\n" +
                            "simKey=123456789\n" +
                            "taskID=0\n" +
                            "jobIndex=0\n");

            long cutoffSec = System.currentTimeMillis() / 1000L;     // slurm epoch equivalent: logs older than this are stale

            // Build CLI args, note the --vc-send-status-config option, uses VCellMessagingRest(config)
            String[] args = {
                    "watchdog",
                    modelFile.toString(),
                    "3",                        // numRuns
                    "--watchdog-cutoff", Long.toString(cutoffSec),    // epoch start time
                    "--max-concurrent-jobs", "3",  // valid max concurrent jobs
                    "--vc-send-status-config", configFile.toString(),   // uses: vcellMessaging = new VCellMessagingRest(config);
                    "--watchdog-tick", "6",     // fallback progress tick, used only if no ETA ever arrives
                    "--watchdog-timeout", "20"  // timeout in seconds for first loop (won't be reached, the fresh log for run 1 shows up at 3 s)
            };

            // Run watchdog in a separate thread so we can kill it
            AtomicInteger watchdogExitCode = new AtomicInteger(-1);    // -1 = still running / never returned
            CommandLine cmd = new CommandLine(new CliMain());
            Thread watchdogThread = new Thread(() -> watchdogExitCode.set(cmd.execute(args)));
            watchdogThread.start();

            // Create multiple log files and keep appending percentage growth
            freshLog0Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 0, 100, 9000, 4000);
            freshLog1Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 1, 100, 3000, 5000);
            freshLog2Thread = createLogWriterThread(tempDirectory, "SimID_123456789_0_", 2, 7, 15000, 6000);
            freshLog0Thread.start();
            freshLog1Thread.start();
            freshLog2Thread.start();

            // Let watchdog run long enough to detect the fresh log and enter the infinite loop
            Thread.sleep(30000);

            // At this point, watchdog should be inside the second loop
            // failed REST sends don't stop it: VCellMessagingRest is called with ThrowOnException.NO
            assertTrue(watchdogThread.isAlive(), "Watchdog should be running, but it exited with code " + watchdogExitCode.get());

            // we run some more, then kill the watchdog thread
            Thread.sleep(30000);
            watchdogThread.interrupt();
            watchdogThread.join(2000);   // give it 2 seconds to exit

            assertFalse(watchdogThread.isAlive(), "Watchdog thread should have been interrupted and stopped");
            assertEquals(0, watchdogExitCode.get(), "Watchdog should exit cleanly from the progress loop");

        } finally {
            for (Thread t : new Thread[] {freshLog0Thread, freshLog1Thread, freshLog2Thread}) {
                if (t != null) {
                    t.interrupt();      // stop writing before the folder is deleted
                    t.join(2000);
                }
            }
            deleteDirectory(tempDirectory.toFile());
        }
    }

    //
    // ---------------------------- Utility functions -------------------------------------
    //
    private Thread createLogWriterThread(
            Path simulationFolder,
            String simulationName,      // ex: SimID_123456789_0_
            int logIndex,               // run index that we're simulating: 0, 1, 2...
            int numEntries,             // number of progress entries to write, normally 100 but we can stop early
            long initialDelayMillis,    // Initial delay before creating the fresh log file
            long writeIntervalMillis
    ) {
        return new Thread(() -> {
            try {
                // Build the log file path for this index
                Path logFile = simulationFolder.resolve(simulationName + logIndex + ".log");

                // Initial delay before creating the fresh log file
                // this simulates the short time between the moment we launch the watchdog and the moment
                // the simulation starts writing to the log file
                Thread.sleep(initialDelayMillis);

                // Rewrite file to make it fresh
                Files.writeString(logFile, "");
                logFile.toFile().setLastModified(System.currentTimeMillis());

                // Now write real progress steps at the specified interval
                for (int i = 1; i <= numEntries; i++) {
                    Thread.sleep(writeIntervalMillis);
                    String line = "Simulation " + i + "% complete. Elapsed time: " + (i * (writeIntervalMillis / 1000.0)) + " sec.\n";
                    System.out.println("    log " + logIndex + " at " + i + "%");   // ... + line.trim()
//                    System.err.println("    log " + logIndex + ": " + line.trim());
                    Files.writeString(logFile, line, StandardOpenOption.APPEND);
                }

            } catch (InterruptedException e) {
                // the test stopped the writer, nothing to report
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}