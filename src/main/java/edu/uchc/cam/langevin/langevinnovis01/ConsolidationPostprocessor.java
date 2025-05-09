/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.helpernovis.ColumnDescription;
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

    // the results
    SolverResultSet averagesResultSet;
    SolverResultSet stdResultSet;
    SolverResultSet minResultSet;
    SolverResultSet maxResultSet;


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

    public SolverResultSet getAveragesResultSet() {
        return averagesResultSet;
    }
    public SolverResultSet getStdResultSet() {
        return stdResultSet;
    }
    public SolverResultSet getMinResultSet() {
        return minResultSet;
    }
    public SolverResultSet getMaxResultSet() {
        return maxResultSet;
    }


    private File simulationFolder;       // top folder, where the input file is (and also the .ida and ,json files are)

    public ConsolidationPostprocessor(Global g, int numRuns, boolean useOutputFile, VCellMessaging vcellMessaging) {

        System.out.println("Constructor of consolidation postprocessor.");

        this.g = g;
        this.numRuns = numRuns;
        this.useOutputFile = useOutputFile;
        this.vcellMessaging = vcellMessaging;

        folderSetup();

//        ConsolidationPostProcessorOutput cpo = new ConsolidationPostProcessorOutput();
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



        try {
            SolverResultSet tempSolverResultSet = solverResultSetMap.get(0);

            // sanity check: shouldn't be, that only works for non-spatial stochastic where things are done differently

            averagesResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.ZeroInitialize);
            stdResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.ZeroInitialize);
            minResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.CopyValues);
            maxResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.CopyValues);

            calculateLangevinPrimaryStatistics();   // averages, standard deviation, min, max
//            calculateLangevinAdvancedStatistics();

        } catch(Exception dae) {        // DataAccessException ?
//            pllOut.setFailed(true);
//            pllOut.setMultiTrial(isMultiTrial);
//            return pllOut;
        }



        System.out.println("Done!");
    }

    private void calculateLangevinPrimaryStatistics() {

        int numTrials = solverResultSetMap.size();
        for(int trialIndex = 0; trialIndex < numTrials; trialIndex++) {
            SolverResultSet sourceOsrs = solverResultSetMap.get(trialIndex);
            int rowCount = sourceOsrs.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                double[] sourceRowData = sourceOsrs.getRow(row);
                double[] averageRowData = averagesResultSet.getRow(row);    // destination average
                double[] minRowData = minResultSet.getRow(row);             // destination min
                double[] maxRowData = maxResultSet.getRow(row);             // destination max

                for (int i = 1; i < averageRowData.length; i++) {   // we skip time, assuming it's always the first column
                    averageRowData[i] += sourceRowData[i] / numTrials;
                    if (minRowData[i] > sourceRowData[i]) {
                        minRowData[i] = sourceRowData[i];
                    }
                    if (maxRowData[i] < sourceRowData[i]) {
                        maxRowData[i] = sourceRowData[i];
                    }
                }
            }
        }
        for(int trialIndex = 0; trialIndex < numTrials; trialIndex++) {
            SolverResultSet sourceOsrs = solverResultSetMap.get(trialIndex);
            int rowCount = sourceOsrs.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                double[] sourceRowData = sourceOsrs.getRow(row);
                double[] averageRowData = averagesResultSet.getRow(row);
                double[] stdRowData = stdResultSet.getRow(row);    // destination std

                for (int i = 1; i < averageRowData.length; i++) {
                    double variance = Math.pow(sourceRowData[i] - averageRowData[i], 2);
                    stdRowData[i] += variance / numTrials;
                }
            }
        }
        int rowCount = stdResultSet.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            double[] stdRowData = stdResultSet.getRow(row);
            for (int i = 1; i < stdRowData.length; i++) {
                double variance = stdRowData[i];
                stdRowData[i] = Math.sqrt(variance);
            }
        }
    }

}
