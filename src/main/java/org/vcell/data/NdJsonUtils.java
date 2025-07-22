package org.vcell.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class NdJsonUtils {

    public static void saveClusterInfoMapToNDJSON(Map<Double, LangevinPostprocessor.TimePointClustersInfo> clusterInfoMap,
            Path clustersFile) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        try (FileWriter writer = new FileWriter(clustersFile.toFile(), false)) {
            clusterInfoMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())  // enforce ascending timepoint order
                    .forEach(entry -> {
                        Double timePoint = entry.getKey();
                        LangevinPostprocessor.TimePointClustersInfo info = entry.getValue();

                        Map<String, Object> ndjsonObject = new LinkedHashMap<>();
                        ndjsonObject.put("timePoint", timePoint);
                        ndjsonObject.put("timePointClustersInfo", info);
                        try {
                            writer.write(objectMapper.writeValueAsString(ndjsonObject));
                            writer.write("\n");
                        } catch (IOException e) {
                            throw new RuntimeException("Error writing JSON line for timePoint " + timePoint, e);
                        }
                    });
        }
        System.out.println("Data successfully saved to NDJSON file: " + clustersFile.toAbsolutePath().getFileName());
    }

    public static Map<Double, LangevinPostprocessor.TimePointClustersInfo> loadClusterInfoMapFromNDJSON(Path clustersFile) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<Double, LangevinPostprocessor.TimePointClustersInfo> clusterInfoMap = new LinkedHashMap<>();
        try (FileReader clustersFileReader = new FileReader(clustersFile.toFile().getAbsoluteFile());
             BufferedReader reader = new BufferedReader(clustersFileReader);
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Deserialize each line into a map
                Map<String, Object> ndjsonObject = objectMapper.readValue(line,
                        TypeFactory.defaultInstance().constructMapType(LinkedHashMap.class, String.class, Object.class));

                // Extract timePoint and timePointClustersInfo
                Double timePoint = (Double) ndjsonObject.get("timePoint");
                LangevinPostprocessor.TimePointClustersInfo timePointClustersInfo = objectMapper.convertValue(ndjsonObject.get("timePointClustersInfo"),
                        LangevinPostprocessor.TimePointClustersInfo.class);

                clusterInfoMap.put(timePoint, timePointClustersInfo);
            }
        }
        System.out.println("Data successfully loaded from NDJSON file: " + clustersFile.toFile().getAbsoluteFile().getName());
        return clusterInfoMap;
    }

}
