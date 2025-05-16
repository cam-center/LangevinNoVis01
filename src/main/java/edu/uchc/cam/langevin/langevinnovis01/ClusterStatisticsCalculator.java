package edu.uchc.cam.langevin.langevinnovis01;

import org.vcell.data.LangevinPostprocessor;

import java.util.*;
import java.util.stream.Collectors;

public class ClusterStatisticsCalculator {

    public static class Statistics {
        double averageClusterSize;
        double standardDeviation;
        double averageClusterOccupancy;
        Map<Integer, Double> fractionalFrequency;
        Map<Integer, Double> fractionOfTotalMolecules;

        public Statistics() {
            fractionalFrequency = new LinkedHashMap<>();
            fractionOfTotalMolecules = new LinkedHashMap<>();
        }

        @Override
        public String toString() {
            return String.format("ACS: %.4f, SD: %.4f, ACO: %.4f",
                    averageClusterSize, standardDeviation, averageClusterOccupancy);
        }
    }

    public static ClusterStatisticsCalculator.Statistics computeIndividualRunStatistics(
            LangevinPostprocessor.TimePointClustersInfo clusterInfo, int totalMolecules) {

        ClusterStatisticsCalculator.Statistics stats = new ClusterStatisticsCalculator.Statistics();

        int totalClusters = clusterInfo.timePointTotalClusters;
        int totalNonTrivialMolecules = clusterInfo.timePointClusterInfoList.stream()
                .mapToInt(cluster -> cluster.size)
                .sum();
        int trivialClusters = totalMolecules - totalNonTrivialMolecules; // Derived from missing molecules

        List<Integer> clusterSizes = clusterInfo.timePointClusterInfoList.stream()
                .map(cluster -> cluster.size)
                .collect(Collectors.toList());

        // FF (including trivial clusters)
        Map<Integer, Long> sizeCounts = clusterSizes.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        sizeCounts.put(1, (long) trivialClusters); // Explicitly adding trivial clusters

        stats.fractionalFrequency = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue() / totalClusters));

        // ACS
        stats.averageClusterSize = totalMolecules / (double) totalClusters;

        // FOTM
        stats.fractionOfTotalMolecules = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (e.getKey() * e.getValue()) / (double) totalMolecules));

        // ACO
        stats.averageClusterOccupancy = stats.fractionOfTotalMolecules.entrySet().stream()
                .mapToDouble(e -> e.getKey() * e.getValue())
                .sum();

        // SD
        double mean = stats.averageClusterSize;
        double variance = sizeCounts.entrySet().stream()
                .mapToDouble(e -> e.getValue() * Math.pow(e.getKey() - mean, 2))
                .sum() / totalClusters;
        stats.standardDeviation = Math.sqrt(variance);

        return stats;
    }

    public static ClusterStatisticsCalculator.Statistics computeOverallRunStatistics(
            Map<Integer, LangevinPostprocessor.TimePointClustersInfo> allRunsAtTimepoint, int totalMoleculesPerRun) {

        int numRuns = allRunsAtTimepoint.size();
        ClusterStatisticsCalculator.Statistics overallStats = new ClusterStatisticsCalculator.Statistics();

        List<Integer> aggregatedClusterSizes = new ArrayList<>();
        int totalNonTrivialMolecules = 0;

        // merge data from all Runs at this timepoint
        int totalClusters = allRunsAtTimepoint.values().stream()
                .mapToInt(clusterInfo -> clusterInfo.timePointTotalClusters)
                .sum();

        for (LangevinPostprocessor.TimePointClustersInfo clusterInfo : allRunsAtTimepoint.values()) {
            int nonTrivialMolecules = clusterInfo.timePointClusterInfoList.stream()
                    .mapToInt(cluster -> cluster.size)
                    .sum();
            totalNonTrivialMolecules += nonTrivialMolecules;

            clusterInfo.timePointClusterInfoList.stream()
                    .map(cluster -> cluster.size)
                    .forEach(aggregatedClusterSizes::add);
        }

        int totalMoleculesAcrossRuns = totalMoleculesPerRun * numRuns;
        int trivialClusters = Math.max(0, totalMoleculesAcrossRuns - totalNonTrivialMolecules);

        // FF (fractional frequency - including trivial clusters)
        Map<Integer, Long> sizeCounts = aggregatedClusterSizes.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        sizeCounts.put(1, (long) trivialClusters); // explicitly adding trivial clusters

        final int finalTotalClusters = totalClusters; // lambda expression needs effectively final variable
        overallStats.fractionalFrequency = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue() / finalTotalClusters));

        // ACS (Average Cluster Size)
        overallStats.averageClusterSize = totalMoleculesAcrossRuns / (double) finalTotalClusters;

        // FOTM (Fraction of Total Molecules)
        overallStats.fractionOfTotalMolecules = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (e.getKey() * e.getValue()) / (double) totalMoleculesAcrossRuns));

        // ACO (Average Cluster Occupancy)
        overallStats.averageClusterOccupancy = overallStats.fractionOfTotalMolecules.entrySet().stream()
                .mapToDouble(e -> e.getKey() * e.getValue())
                .sum();

        // SD (Standard Deviation)
        double mean = overallStats.averageClusterSize;
        double variance = sizeCounts.entrySet().stream()
                .mapToDouble(e -> e.getValue() * Math.pow(e.getKey() - mean, 2))
                .sum() / finalTotalClusters;
        overallStats.standardDeviation = Math.sqrt(variance);

        return overallStats;
    }

//    public static Map<Double, ClusterStatisticsCalculator.Statistics> computeMeanRunStatistics(
//            Map<Double, Map<Integer, ClusterStatisticsCalculator.Statistics>> perTimepointPerRunStatistics, int numRuns) {
//
//        Map<Double, ClusterStatisticsCalculator.Statistics> meanRunStats = new LinkedHashMap<>();
//
//        for (Map.Entry<Double, Map<Integer, ClusterStatisticsCalculator.Statistics>> timepointEntry : perTimepointPerRunStatistics.entrySet()) {
//            double timepoint = timepointEntry.getKey();
//            Map<Integer, ClusterStatisticsCalculator.Statistics> runStatsMap = timepointEntry.getValue();
//
//            ClusterStatisticsCalculator.Statistics meanStats = new ClusterStatisticsCalculator.Statistics();
//
//            // compute Mean ACS and ACO
//            double sumACS = 0.0, sumACO = 0.0;
//            List<Double> acsValues = new ArrayList<>();
//
//            for (ClusterStatisticsCalculator.Statistics runStats : runStatsMap.values()) {
//                sumACS += runStats.averageClusterSize;
//                sumACO += runStats.averageClusterOccupancy;
//                acsValues.add(runStats.averageClusterSize);  // needed for SD computation
//            }
//
//            meanStats.averageClusterSize = sumACS / numRuns;
//            meanStats.averageClusterOccupancy = sumACO / numRuns;
//
//            // compute Standard Deviation for ACS (across runs, per timepoint)
//            double varianceACS = acsValues.stream()
//                    .mapToDouble(acs -> Math.pow(acs - meanStats.averageClusterSize, 2))
//                    .sum() / numRuns;
//            meanStats.standardDeviation = Math.sqrt(varianceACS);
//
//            meanRunStats.put(timepoint, meanStats);
//        }
//        return meanRunStats;
//    }

    // TODO: check this !!!
    public static ClusterStatisticsCalculator.Statistics computeMeanRunStatistics(
            Map<Integer, ClusterStatisticsCalculator.Statistics> runStatisticsMap, int numRuns) {

        ClusterStatisticsCalculator.Statistics meanStats = new ClusterStatisticsCalculator.Statistics();

        double sumACS = 0.0, sumACO = 0.0;
        List<Double> acsValues = new ArrayList<>();

        for (ClusterStatisticsCalculator.Statistics runStats : runStatisticsMap.values()) {
            sumACS += runStats.averageClusterSize;
            sumACO += runStats.averageClusterOccupancy;
            acsValues.add(runStats.averageClusterSize); // For SD calculation
        }

        meanStats.averageClusterSize = sumACS / numRuns;
        meanStats.averageClusterOccupancy = sumACO / numRuns;

        // Compute Standard Deviation for ACS
        double varianceACS = acsValues.stream()
                .mapToDouble(acs -> Math.pow(acs - meanStats.averageClusterSize, 2))
                .sum() / numRuns;
        meanStats.standardDeviation = Math.sqrt(varianceACS);

        return meanStats;
    }



    private static void mergeStatistics(Statistics overallStats, Statistics individualStats) {
        overallStats.averageClusterSize += individualStats.averageClusterSize;
        overallStats.averageClusterOccupancy += individualStats.averageClusterOccupancy;
        overallStats.standardDeviation += individualStats.standardDeviation;

        individualStats.fractionalFrequency.forEach((size, freq) ->
                overallStats.fractionalFrequency.merge(size, freq, Double::sum));

        individualStats.fractionOfTotalMolecules.forEach((size, fotm) ->
                overallStats.fractionOfTotalMolecules.merge(size, fotm, Double::sum));
    }


}
