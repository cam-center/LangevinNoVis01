package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.cli.CliMain;
import edu.uchc.cam.langevin.helpernovis.FileMapper;
import edu.uchc.cam.langevin.helpernovis.SolverResultSet;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CliTest {

    String inputFileContents =
            """
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
}