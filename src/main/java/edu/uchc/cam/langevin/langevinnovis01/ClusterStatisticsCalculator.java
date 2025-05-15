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

    public static Statistics computeIndividualRunStatistics(LangevinPostprocessor.TimePointClustersInfo clusterInfo, int totalMolecules) {
        Statistics stats = new Statistics();

        int totalClusters = clusterInfo.timePointTotalClusters;  // Includes trivial clusters
        int totalNonTrivialMolecules = clusterInfo.timePointClusterInfoList.stream()
                .mapToInt(cluster -> cluster.size)
                .sum();
        int trivialClusters = totalMolecules - totalNonTrivialMolecules;  // Derived from missing molecules
        List<Integer> clusterSizes = clusterInfo.timePointClusterInfoList.stream()
                .map(cluster -> cluster.size)
                .collect(Collectors.toList());

        // Compute fractional frequency (considering both trivial and non-trivial clusters)
        Map<Integer, Long> sizeCounts = clusterSizes.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        sizeCounts.put(1, (long) trivialClusters);  // Explicitly adding trivial clusters to the distribution

        stats.fractionalFrequency = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue() / totalClusters));

        // Compute ACS (Average Cluster Size)
        stats.averageClusterSize = sizeCounts.entrySet().stream()
                .mapToDouble(e -> e.getKey() * (e.getValue() / (double) totalClusters))
                .sum();

        // Compute Fraction of Total Molecules (Fotm)
        stats.fractionOfTotalMolecules = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (e.getKey() * e.getValue()) / (double) totalMolecules));

        // Compute Average Cluster Occupancy (ACO)
        stats.averageClusterOccupancy = stats.fractionOfTotalMolecules.entrySet().stream()
                .mapToDouble(e -> e.getKey() * e.getValue())
                .sum();

        // Compute Standard Deviation (SD)
        double mean = stats.averageClusterSize;
        double variance = sizeCounts.entrySet().stream()
                .mapToDouble(e -> e.getValue() * Math.pow(e.getKey() - mean, 2))
                .sum() / totalClusters;
        stats.standardDeviation = Math.sqrt(variance);

        return stats;
    }

    public static Map<Double, Statistics> computeOverallRunStatistics(Map<Integer, Map<Double, LangevinPostprocessor.TimePointClustersInfo>> allRunsClusterInfoMap, int totalMolecules) {
        Map<Double, Statistics> overallStats = new LinkedHashMap<>();

        for (Map<Double, LangevinPostprocessor.TimePointClustersInfo> runData : allRunsClusterInfoMap.values()) {
            for (Map.Entry<Double, LangevinPostprocessor.TimePointClustersInfo> entry : runData.entrySet()) {
                double timepoint = entry.getKey();
                LangevinPostprocessor.TimePointClustersInfo clusterInfo = entry.getValue();

                overallStats.computeIfAbsent(timepoint, k -> new Statistics());

                Statistics timePointStats = computeIndividualRunStatistics(clusterInfo, totalMolecules);
                mergeStatistics(overallStats.get(timepoint), timePointStats);
            }
        }

        return overallStats;
    }

    public static Map<Double, Statistics> computeMeanRunStatistics(Map<Integer, Map<Double, Statistics>> perRunStatistics, int numRuns) {
        Map<Double, Statistics> meanRunStats = new LinkedHashMap<>();

        for (Map<Double, Statistics> runData : perRunStatistics.values()) {
            for (Map.Entry<Double, Statistics> entry : runData.entrySet()) {
                double timepoint = entry.getKey();
                Statistics runStats = entry.getValue();

                meanRunStats.computeIfAbsent(timepoint, k -> new Statistics());

                Statistics meanStats = meanRunStats.get(timepoint);
                meanStats.averageClusterSize += runStats.averageClusterSize / numRuns;
                meanStats.averageClusterOccupancy += runStats.averageClusterOccupancy / numRuns;

                // Compute SD for Mean Run using ACS values from all runs
                double variance = perRunStatistics.values().stream()
                        .mapToDouble(r -> Math.pow(r.get(timepoint).averageClusterSize - meanStats.averageClusterSize, 2))
                        .sum() / numRuns;
                meanStats.standardDeviation = Math.sqrt(variance);
            }
        }

        return meanRunStats;
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
