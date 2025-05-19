/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uchc.cam.langevin.langevinnovis01;

import edu.uchc.cam.langevin.g.object.GMolecule;
import edu.uchc.cam.langevin.helpernovis.SolverResultSet;
import org.vcell.data.LangevinPostprocessor;
import org.vcell.messaging.VCellMessaging;

import java.io.*;
import java.util.*;

public class ConsolidationPostprocessor {

    private Global g;
    private final VCellMessaging vcellMessaging;
    private int numRuns;        // number of runs
    private final boolean useOutputFile;

    private String simulationName;          // model / simulation name (without extension)
    private File simulationFolder;          // top folder, where the input file is (and also the .ida and ,json files are)


    public ConsolidationPostprocessor(Global g, int numRuns, boolean useOutputFile, VCellMessaging vcellMessaging) {
        this.g = g;
        this.numRuns = numRuns;
        this.useOutputFile = useOutputFile;
        this.vcellMessaging = vcellMessaging;

        folderSetup();
    }

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

    // -------------------------------------------------------------------------------------------------------
    public void calculateLangevinPrimaryStatistics() throws IOException {

        ConsolidationPostprocessorInput cpi = new ConsolidationPostprocessorInput();
        cpi.readInputFiles(this);

        Map<String, File> nameToIdaFileMap = cpi.getNameToIdaFileMap();
        Map<Integer, SolverResultSet> solverResultSetMap = cpi.getSolverResultSetMap();

        SolverResultSet tempSolverResultSet = solverResultSetMap.get(0);
        List<Double> timeInSecondsList = tempSolverResultSet.getColumn(SolverResultSet.TIME_COLUMN);

        // the output result sets
        SolverResultSet averagesResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.ZeroInitialize);
        SolverResultSet stdResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.ZeroInitialize);
        SolverResultSet minResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.CopyValues);
        SolverResultSet maxResultSet = SolverResultSet.deepCopy(tempSolverResultSet, SolverResultSet.DuplicateMode.CopyValues);

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
        ConsolidationPostprocessorOutput cpo = new ConsolidationPostprocessorOutput(this);
        cpo.writeResultFiles(averagesResultSet, stdResultSet, minResultSet, maxResultSet);
    }


    public void calculateLangevinAdvancedStatistics() throws IOException {

        ConsolidationClusterAnalizerInput cai = new ConsolidationClusterAnalizerInput();
        cai.readInputFiles(this);

        List<Double> timeInSecondsList = cai.getTimeInSecondsList();
        int numTimePoints = timeInSecondsList.size();
        int totalMolecules = getTotalMolecules(g);

        Map<Double, Map<Integer, ClusterStatisticsCalculator.Statistics>> perTimepointPerRunStatistics = new LinkedHashMap<>(); // results for individual runs
        Map<Double, ClusterStatisticsCalculator.Statistics> perTimepointMeanRunStatistics = new LinkedHashMap<>();              // results for mean run
        Map<Double, ClusterStatisticsCalculator.Statistics> perTimepointOverallRunStatistics = new LinkedHashMap<>();           // results for overall run

        for (int timepointIndex = 0; timepointIndex < numTimePoints; timepointIndex++) {
            double currentTimepointValue = timeInSecondsList.get(timepointIndex);
            Map<Integer, LangevinPostprocessor.TimePointClustersInfo> allRunsAtTimepoint = cai.getRow(currentTimepointValue);

            Map<Integer, ClusterStatisticsCalculator.Statistics> runStatisticsMap = new LinkedHashMap<>();  // compute Individual run statistics
            for (Map.Entry<Integer, LangevinPostprocessor.TimePointClustersInfo> entry : allRunsAtTimepoint.entrySet()) {

                int runIndex = entry.getKey();
                LangevinPostprocessor.TimePointClustersInfo clusterInfo = entry.getValue();
                ClusterStatisticsCalculator.Statistics stats = ClusterStatisticsCalculator.computeIndividualRunStatistics(clusterInfo, totalMolecules);
                runStatisticsMap.put(runIndex, stats);
            }
            perTimepointPerRunStatistics.put(currentTimepointValue, runStatisticsMap);

            ClusterStatisticsCalculator.Statistics overallStats = ClusterStatisticsCalculator.computeOverallRunStatistics(
                    allRunsAtTimepoint, totalMolecules);
            perTimepointOverallRunStatistics.put(currentTimepointValue, overallStats);

            ClusterStatisticsCalculator.Statistics meanStats = ClusterStatisticsCalculator.computeMeanRunStatistics(
                    runStatisticsMap, numRuns);
            perTimepointMeanRunStatistics.put(currentTimepointValue, meanStats);
        }

        ConsolidationClusterAnalizerOutput cao = new ConsolidationClusterAnalizerOutput(this);
        cao.writeOutput(perTimepointOverallRunStatistics, perTimepointMeanRunStatistics);
//        cao.writeOutput(perTimepointPerRunStatistics,
//            ConsolidationClusterAnalizerOutput.RunStatisticsOutputMode.BY_RUN_BY_TIMEPOINT);
//        cao.writeOutput(perTimepointPerRunStatistics,
//            ConsolidationClusterAnalizerOutput.RunStatisticsOutputMode.BY_TIMEPOINT_BY_RUN);

    }


    // -------------------------------------------------------------------------------------
    static Map<String, Integer> getMolecules(Global g) {
        Map<String, Integer> molecules = new LinkedHashMap<>();
        for(GMolecule m : g.getMolecules()) {
            molecules.put(m.getName(), m.getNumber());
        }
        return molecules;
    }
    static int getTotalMolecules(Global g) {
        int totalMolecules = 0;
        for(GMolecule m : g.getMolecules()) {
            totalMolecules += m.getNumber();
        }
        return totalMolecules;
    }

}
