package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.helpernovis.FileMapper;
import edu.uchc.cam.langevin.helpernovis.SolverResultSet;
import org.vcell.data.LangevinPostprocessor;
import org.vcell.data.NdJsonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


public class ConsolidationClusterAnalizerInput {

    // key = name of simulation run, value = the json file with cluster info map for that simulation
    private Map<String, File> nameToJsonFileMap;

    // key = run index, value = cluster info map for that run
    private Map<Integer, Map<Double, LangevinPostprocessor.TimePointClustersInfo>> allRunsClusterInfoMap;

    public ConsolidationClusterAnalizerInput() {
        nameToJsonFileMap = null;
        allRunsClusterInfoMap = null;
    }

    public Map<String, File> getNameToJsonFileMap() {
        return nameToJsonFileMap;
    }
    public Map<Integer, Map<Double, LangevinPostprocessor.TimePointClustersInfo>> getAllRunsClusterInfoMap() {
        return allRunsClusterInfoMap;
    }

    //---------------------------------------------------------------------------------------------
    public void readInputFiles(ConsolidationPostprocessor cp) throws FileNotFoundException {

        nameToJsonFileMap = FileMapper.getFileMapByName(cp.getSimulationFolder(), cp.getSimulationName(), MySystem.ClustersFileExtension);
        nameToJsonFileMap.forEach((name, file) -> System.out.println(name + " -> " + file.getAbsolutePath()));    // show results
        System.out.println("aici");


//        Path clustersFile;
//        String clustersFileName = clustersFile.toFile().getAbsoluteFile().getName();
//        Map<Double, LangevinPostprocessor.TimePointClustersInfo> loadedClusterInfoMap = NdJsonUtils.loadClusterInfoMapFromNDJSON(clustersFile);


    }

}
