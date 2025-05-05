/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.helpernovis.FileMapper;
import edu.uchc.cam.langevin.helpernovis.SolverResultSet;
import org.vcell.messaging.VCellMessaging;

import java.io.*;
import java.util.*;

public class ConsolidationPostprocessor {

    private Global g;
    private final VCellMessaging vcellMessaging;
    private int numRuns;
    private final boolean useOutputFile;

    private String simulationName;       // model / simulation name (without extension)

    Map<String, File> nameToIdaFileMap = null;
    Map<Integer, SolverResultSet> solverResultSetMap = null;


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


    public void runConsolidation(ConsolidationPostprocessorInput cpi) throws InterruptedException, IOException {

        System.out.println("Running consolidation for " + numRuns + " tasks");
        this.nameToIdaFileMap = cpi.getNameToIdaFileMap();
        this.solverResultSetMap = cpi.getSolverResultSetMap();

        System.out.println("Done!");
    }

}
