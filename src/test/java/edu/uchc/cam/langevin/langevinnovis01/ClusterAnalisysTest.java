package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.cli.CliMain;
import edu.uchc.cam.langevin.g.object.GMolecule;
import edu.uchc.cam.langevin.helpernovis.FileMapper;
import edu.uchc.cam.langevin.helpernovis.SolverResultSet;
import org.junit.jupiter.api.Test;
import org.vcell.data.LangevinPostprocessor;
import org.vcell.data.NdJsonUtils;
import org.vcell.messaging.VCellMessaging;
import org.vcell.messaging.VCellMessagingNoop;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterAnalisysTest {

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    // the resources are in  \src\test\resources\simdata



    // TODO: place some simulation results in the repository and properly invoke the tests on them
    //  in a temp directory like we do in CliTest

    public static final String sim_base_name = "SimID_35189106_0_";     // use "sim" for small test
    //public static final String parent_dir = "C:/TEMP/langevin-cli-test/cluster_analysis;    // use this for small test
    public static final String parent_dir = "C:/TEMP/langevin-cli-test/cluster_analysis_big3";
//    public static final String data_dir = "C:/TEMP/langevin-cli-test/cluster_analysis_big2/sim_Folder/data";
    public static final String data_dir = "C:/TEMP/langevin-cli-test/cluster_analysis_big3/" +
        sim_base_name + "_FOLDER" + "/data";

    public static final int NumRuns = 3;           // for small test use 6
    public static final String ClustersFileExtension = ".json";

/*
 *  These tests are needed just for debugging, not intended to be run automatically
 * The CliTest.testRunAndPostCommand is doing everything done here and more
 */

//    @Test
    public void testRunClusterAnalysis() throws IOException {

        // this test needs the .json and .ida files
        VCellMessaging vcellMessaging = new VCellMessagingNoop();
        File modelFile = new File(parent_dir, sim_base_name+".langevinInput");
        File simulationFolder = new File(parent_dir);   // place of input file, and .ida and .json result files for all runs

        Global g = new Global(modelFile);
        ConsolidationPostprocessor cp = new ConsolidationPostprocessor(g, 50, false, vcellMessaging);
        cp.setSimulationFolder(simulationFolder);
        cp.setNumRuns(NumRuns);
        cp.setSimulationName(sim_base_name);

        cp.calculateLangevinPrimaryStatistics();
        File targetFile = new File(simulationFolder, sim_base_name + "_Avg.ida");
        assertTrue(targetFile.exists(), "expected file " + sim_base_name + "_Avg.ida to exist");

        cp.calculateLangevinAdvancedStatistics();   // cluster analysis
        File[] csvFiles = simulationFolder.listFiles((dir, name) -> name.endsWith(".csv"));
        int count = csvFiles != null ? csvFiles.length : 0;
        assertTrue(NumRuns == count, "number of .csv files should be equal to " + NumRuns);


        System.out.println("done");
    }

//    @Test
    public void testReadJsonFiles() throws IOException {

        // this test reads the json files
        VCellMessaging vcellMessaging = new VCellMessagingNoop();
        File modelFile = new File(parent_dir, sim_base_name+".langevinInput");
        File simulationFolder = new File(parent_dir);   // place of input file, and .ida and .json result files for all runs

        Global g = new Global(modelFile);
        ConsolidationPostprocessor cp = new ConsolidationPostprocessor(g, NumRuns, false, vcellMessaging);
        cp.setSimulationFolder(simulationFolder);
        cp.setNumRuns(NumRuns);
        cp.setSimulationName(sim_base_name);
        Map<String, Integer> molecules = ConsolidationPostprocessor.getMolecules(g);

        ConsolidationClusterAnalizerInput cai = new ConsolidationClusterAnalizerInput();
        cai.readInputFiles(cp);

        Map<String, File> nameToJsonFileMap = cai.getNameToJsonFileMap();
        Map<Integer, Map<Double, LangevinPostprocessor.TimePointClustersInfo>> allRunsClusterInfoMap = cai.getAllRunsClusterInfoMap();
        assertTrue(NumRuns == nameToJsonFileMap.size(), "number of .json files should be equal to " + NumRuns);
        assertTrue(NumRuns == allRunsClusterInfoMap.size(), "number of .json files should be equal to " + NumRuns);

        System.out.println("done");
    }

//    @Test
    public void testMakeJsonFiles() throws IOException {

        // this test makes the json files for each run
        VCellMessaging vcellMessaging = new VCellMessagingNoop();
        File modelFile = new File(parent_dir, sim_base_name+".langevinInput");
        File simulationFolder = new File(parent_dir);   // place of input file, and .ida and .json result files for all runs

        Global g = new Global(modelFile);
        ConsolidationPostprocessor cp = new ConsolidationPostprocessor(g, 4, false, vcellMessaging);
        cp.setSimulationFolder(simulationFolder);
        cp.setNumRuns(NumRuns);
        cp.setSimulationName(sim_base_name);
        Map<String, Integer> molecules = ConsolidationPostprocessor.getMolecules(g);

        for(int runCounter=0; runCounter < NumRuns; runCounter++) {

            String runDataFolderString = data_dir + "/Run" + runCounter;
            File runDataFolder = new File(runDataFolderString);
            String newClustersFileName = sim_base_name + "_" + runCounter + ClustersFileExtension;
            File clustersFile = new File(parent_dir, newClustersFileName);
            LangevinPostprocessor.writeClustersFile(runDataFolder.toPath(), clustersFile.toPath());
            Map<Double, LangevinPostprocessor.TimePointClustersInfo> loadedClusterInfoMap = NdJsonUtils.loadClusterInfoMapFromNDJSON(clustersFile.toPath());
        }

        System.out.println("done");
    }



}