/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.helpernovis.FileMapper;
import org.vcell.messaging.VCellMessaging;

import java.io.*;
import java.util.*;

public class ConsolidationPostprocessor {

    private Global g;
    private final VCellMessaging vcellMessaging;
    private int numRuns;
    private final boolean useOutputFile;

    private String simulationName;       // model / simulation name (without extension)
    private File simulationFolder;       // top folder, where the input file is (and also the .ida and ,json files are)

    public ConsolidationPostprocessor(Global g, int numRuns, boolean useOutputFile, VCellMessaging vcellMessaging) {

        System.out.println("Constructor of consolidation postprocessor.");

        this.g = g;
        this.numRuns = numRuns;
        this.useOutputFile = useOutputFile;
        this.vcellMessaging = vcellMessaging;

        folderSetup();
    }

    private void folderSetup() {
        // <editor-fold defaultstate="collapsed" desc="Method Code">
        File inputFile = g.getInputFile();      // model / simulation input file (the .langevininput file)
        simulationName = inputFile.getName();

        String filePath = inputFile.getAbsolutePath();
        filePath = filePath.substring(0, filePath.length() - simulationName.length());  // Strip the file name off of the path
        simulationFolder = new File(filePath);

        int dotIndex = simulationName.lastIndexOf('.');     // Strip extension off the file name
        if (dotIndex > 0) {
            simulationName = simulationName.substring(0, dotIndex);
        } else {
            System.out.println("Expected an extension for the input file: " + simulationName);
        }
    }


    public void runConsolidation() throws InterruptedException, FileNotFoundException {

        System.out.println("Running consolidation for " + numRuns + " tasks");

        Map<String, File> fileMap = FileMapper.getFileMapByName(simulationFolder, simulationName, MySystem.IdaFileExtension);
        if(fileMap.size() != numRuns) {
            throw new RuntimeException("Expected ida file map size " + numRuns + " but found " + fileMap.size());
        }
        fileMap.forEach((name, file) -> System.out.println(name + " -> " + file.getAbsolutePath()));    // show results

        // TODO: make LangevinInput object, like ODESolverResultSet

        System.out.println("Done!");
    }

}
