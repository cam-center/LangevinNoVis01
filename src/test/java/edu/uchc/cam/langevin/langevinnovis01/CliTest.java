package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.cli.CliMain;
import edu.uchc.cam.langevin.cli.RunCommand;
import org.junit.jupiter.api.Test;
import org.vcell.messaging.VCellMessaging;
import org.vcell.messaging.VCellMessagingLocal;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testRunCommand() throws IOException {

//        Path parentFolder = Paths.get(parent_dir);
//        Path tempDirectory = parentFolder.resolve("test_simulation");   // use a convenient location for debugging
//        Files.createDirectories(tempDirectory);

        Path tempDirectory = Files.createTempDirectory(temp_dir_name);        // correct temp location for automatic testing
        Path modelFile = tempDirectory.resolve(sim_base_name+".langevinInput");
        Path logFile = tempDirectory.resolve(sim_base_name+".log");
        Path idaFile_0 = tempDirectory.resolve(sim_base_name+".ida");
        Path idaFile_1 = tempDirectory.resolve(sim_base_name+"_1.ida");
        Path jsonClustersFile_0 = tempDirectory.resolve(sim_base_name+".json");
        Path jsonClustersFile_1 = tempDirectory.resolve(sim_base_name+"_1.json");

        Files.writeString(modelFile, inputFileContents);
//        VCellMessaging vcellMessaging = new VCellMessagingLocal();

        String[] args = {       // command arguments, run 0
                "simulate",
                modelFile.toFile().getAbsolutePath(),   // Langevin model file
                "0",                               // we absolutely need a run counter of 0 to properly initialize dirs
                "--output-log", logFile.toFile().getAbsolutePath(), // Output log file
                "--vc-print-status"                // Enable status printing
        };
        String[] args1 = {       // command arguments, run 1
                "simulate",
                modelFile.toFile().getAbsolutePath(),   // Langevin model file
                "1",                               // we absolutely need a run counter of 0 to properly initialize dirs
                "--output-log", logFile.toFile().getAbsolutePath(), // Output log file
                "--vc-print-status"                // Enable status printing
        };

        int exitCode = -1;
        try {
            CommandLine cmd = new CommandLine(new CliMain());
            exitCode = cmd.execute(args);

            assertEquals(0, exitCode, "Expected command to execute successfully");
            assertEquals(true, modelFile.toFile().exists(), "Model file should exist");
            assertEquals(true, logFile.toFile().exists(), "Log file should exist");
            assertEquals(true, idaFile_0.toFile().exists(), "ida file 0 should exist");
            assertEquals(true, jsonClustersFile_0.toFile().exists(), "json clusters file 0 should exist");

            exitCode = cmd.execute(args1);
            assertEquals(0, exitCode, "Expected command to execute successfully");
            assertEquals(true, idaFile_1.toFile().exists(), "ida file 1 should exist");
            assertEquals(true, jsonClustersFile_1.toFile().exists(), "json clusters file 1 should exist");

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
        assertEquals(1, exitCode, "Expected error due to invalid model file");
    }
}