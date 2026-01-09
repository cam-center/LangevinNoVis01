package edu.uchc.cam.langevin.langevinnovis01;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.vcell.messaging.VCellMessaging;
import org.vcell.messaging.VCellMessagingLocal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MySystemTest {

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
            L_z_out: 0.010000000000000009
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
            
            'r1'       'MT0' : 'Site0' : 'Any_State'  +  'MT0' : 'Site0' : 'Any_State'  kon  10.0  koff 0.0  Bond_Length 1.0
            
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
            """;

    String expected_idafile_contents_2_lines = """
            t:r1:TOTAL_MT0:FREE_MT0:BOUND_MT0:TOTAL_MT0__Site0__state0:FREE_MT0__Site0__state0:BOUND_MT0__Site0__state0:TOTAL_MT0__Site1__state0:FREE_MT0__Site1__state0:BOUND_MT0__Site1__state0:TOTAL_MT0__Site1__state1:FREE_MT0__Site1__state1:BOUND_MT0__Site1__state1
            0.0 0 20 20 0 20 20 0 20 20 0 0 0 0
            """;

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    @Test
    public void testMySystem() throws IOException {
        // create a temporary directory
        // create an input file in the temporary directory with the contents of inputFileContents
        String sim_base_name = "sim";
        int runCounter = 0;
        Path tempDirectory = Files.createTempDirectory("test_simulation");
        Path modelFile = tempDirectory.resolve(sim_base_name+".langevinInput");
        Path logFile = tempDirectory.resolve(sim_base_name+".log");
        Path idaFile = tempDirectory.resolve(sim_base_name+".ida");
        Files.writeString(modelFile, inputFileContents);
        VCellMessaging vcellMessaging = new VCellMessagingLocal();
        try {
            // run simulation
            Global g = new Global(modelFile.toFile(), logFile.toFile());
            MySystem sys = new MySystem(g, runCounter, true, vcellMessaging);
            sys.runSystem();
            // check that the ida file was created and starts with the expected contents
            Assertions.assertTrue(Files.exists(idaFile));
            String idaFileContents = Files.readString(idaFile);
            Assertions.assertTrue(idaFileContents.startsWith(expected_idafile_contents_2_lines));
            System.out.println(idaFileContents.substring(0,500)+"\n...\n"+idaFileContents.substring(idaFileContents.length()-500));
        } finally {
            deleteDirectory(tempDirectory.toFile());
        }
    }

    /*
    this is the springsalad tutorial, as described on vcell.org/ssalad-2
    on: https://vcell.org/webstart/SpringSaLaD/SpringSaLaDUsersGuideAndTutorial.pdf
     */
    @Test
    public void loadFileFromResource() throws Exception {
        URL url = getClass().getResource("/tutorial.ssld");
        assertNotNull(url);

        File modelFile = new File(url.toURI());
        File logFile = File.createTempFile("tutorialTest", ".log");

        VCellMessaging vcellMessaging = new VCellMessagingLocal();

        try {
            Global g = new Global(modelFile, logFile);
            assertNotNull(g);

            MySystem sys = new MySystem(g, 0, true, vcellMessaging);
            assertNotNull(sys);
        } finally {
            logFile.delete();
        }
    }

    // do not run on github actions, it's somewhat long
    @DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
    @Test
    public void simulateFileFromResource() throws IOException {
        String sim_base_name = "sim";
        int runCounter = 0;

        // Load the real file from src/test/resources
        String inputFileContents;
        try (InputStream is = getClass().getResourceAsStream("/tutorial.ssld")) {
            assertNotNull(is, "Resource tutorial.ssld not found");
            inputFileContents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Path tempDirectory = Files.createTempDirectory("test_simulation");
        Path modelFile = tempDirectory.resolve(sim_base_name + ".langevinInput");
        Path logFile = tempDirectory.resolve(sim_base_name + ".log");
        Path idaFile = tempDirectory.resolve(sim_base_name + ".ida");

        Files.writeString(modelFile, inputFileContents);

        VCellMessaging vcellMessaging = new VCellMessagingLocal();
        Global g = null;
        MySystem sys = null;

        try {
            g = new Global(modelFile.toFile(), logFile.toFile());
            assertNotNull(g, "Global object should not be null");

            sys = new MySystem(g, runCounter, true, vcellMessaging);
            assertNotNull(sys, "MySystem object should not be null");

            sys.runSystem();
            Assertions.assertTrue(Files.exists(idaFile));

        } finally {
            deleteDirectory(tempDirectory.toFile());
        }
    }

}
