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
        Map<Integer, Double> clusterSizeFrequencyMap;  // CSF (cluster size frequency), tracks frequency of each cluster size
        Map<Integer, Double> normalizedClusterSizeFrequencyMap; // normalized CSF to total clusters

        public Statistics() {
            fractionalFrequency = new LinkedHashMap<>();
            fractionOfTotalMolecules = new LinkedHashMap<>();
            clusterSizeFrequencyMap = new LinkedHashMap<>();    //  key=cluster size, value=number of clusters of that size
            normalizedClusterSizeFrequencyMap = new LinkedHashMap<>();
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
        int trivialClusters = totalMolecules - totalNonTrivialMolecules; // derived from missing molecules

        List<Integer> clusterSizes = clusterInfo.timePointClusterInfoList.stream()
                .map(cluster -> cluster.size)
                .collect(Collectors.toList());

        // CSF frequency distribution for cluster sizes
        // is there a preferred cluster size? are small clusters transitioning into large ones?
        clusterSizes.forEach(size -> stats.clusterSizeFrequencyMap.merge(size, 1.0, Double::sum));

        // normalize CSF
        stats.normalizedClusterSizeFrequencyMap = stats.clusterSizeFrequencyMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() / totalClusters));

        // FF (including trivial clusters)
        Map<Integer, Long> sizeCounts = clusterSizes.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        sizeCounts.put(1, (long) trivialClusters); // explicitly adding trivial clusters

        stats.fractionalFrequency = sizeCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue() / (double) totalClusters));

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
                .sum() / (double) totalClusters;
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

        // raw CSF aggregate frequency distribution for cluster sizes
        for (Integer size : aggregatedClusterSizes) {
            overallStats.clusterSizeFrequencyMap.merge(size, 1.0, Double::sum);
        }
        // normalize CSF
        overallStats.normalizedClusterSizeFrequencyMap = overallStats.clusterSizeFrequencyMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() / totalClusters));

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
                .sum() / (double) finalTotalClusters;
        overallStats.standardDeviation = Math.sqrt(variance);

        return overallStats;
    }

    public static ClusterStatisticsCalculator.Statistics computeMeanRunStatistics(
            Map<Integer, ClusterStatisticsCalculator.Statistics> runStatisticsMap, int numRuns) {

        ClusterStatisticsCalculator.Statistics meanStats = new ClusterStatisticsCalculator.Statistics();

        double sumACS = 0.0, sumACO = 0.0;
        List<Double> acsValues = new ArrayList<>();
        Map<Integer, Double> aggregatedFrequency = new LinkedHashMap<>();

        for (ClusterStatisticsCalculator.Statistics runStats : runStatisticsMap.values()) {
            sumACS += runStats.averageClusterSize;
            sumACO += runStats.averageClusterOccupancy;
            acsValues.add(runStats.averageClusterSize); // For SD calculation

            // CSF aggregate cluster size frequencies across runs
            runStats.clusterSizeFrequencyMap.forEach((size, count) ->
                    aggregatedFrequency.merge(size, count.doubleValue(), Double::sum));
        }

        meanStats.averageClusterSize = sumACS / numRuns;
        meanStats.averageClusterOccupancy = sumACO / numRuns;
        meanStats.clusterSizeFrequencyMap = aggregatedFrequency.entrySet().stream()     // raw CSF
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() / numRuns));
        // normalize CSF
        double totalClustersAcrossRuns = meanStats.clusterSizeFrequencyMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        meanStats.normalizedClusterSizeFrequencyMap = meanStats.clusterSizeFrequencyMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() / totalClustersAcrossRuns));

        // Compute Standard Deviation for ACS
        double varianceACS = acsValues.stream()
                .mapToDouble(acs -> Math.pow(acs - meanStats.averageClusterSize, 2))
                .sum() / numRuns;
        meanStats.standardDeviation = Math.sqrt(varianceACS);

        return meanStats;
    }

    // TODO: for the future: keep counts with how many time each reaction was triggered for each timepoint
    //  (it's the FREE in the peimary statistics)

    // we ignore trivial clusters, we'll have that info elsewhere with separate counts per each molecule
    // to detect depletion
    public static void fillEmptyClusterFrequencies(Map<?, ClusterStatisticsCalculator.Statistics> allStats, int maxClusterSize) {
        // ensure all frequency maps include every cluster size up to maxClusterSize
        for (ClusterStatisticsCalculator.Statistics stats : allStats.values()) {
            for (int size = 2; size <= maxClusterSize; size++) {
                stats.clusterSizeFrequencyMap.putIfAbsent(size, 0.0);           // fill missing sizes with 0
                stats.normalizedClusterSizeFrequencyMap.putIfAbsent(size, 0.0);
            }
        }
    }
    static int getMaxClusterSize(Map<?, ClusterStatisticsCalculator.Statistics> referenceStats) {
        // key may be Double or Integer
        int maxClusterSize = referenceStats.values().stream()   // find the longest cluster size across all statistics
                .flatMap(stats -> stats.clusterSizeFrequencyMap.keySet().stream())
                .max(Integer::compare)
                .orElse(1); // default to 1 if no clusters exist
        return maxClusterSize;
    }


}
