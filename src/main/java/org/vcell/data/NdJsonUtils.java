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
        try (FileWriter writer = new FileWriter(clustersFile.toFile().getAbsoluteFile(), false)) {
            for (Map.Entry<Double, LangevinPostprocessor.TimePointClustersInfo> entry : clusterInfoMap.entrySet()) {
                // Create an object to serialize
                Map<String, Object> ndjsonObject = new LinkedHashMap<>();
                ndjsonObject.put("timePoint", entry.getKey());
                ndjsonObject.put("timePointClustersInfo", entry.getValue());

                // Write each JSON object on a new line
                writer.write(objectMapper.writeValueAsString(ndjsonObject) + "\n");
            }
        }
        System.out.println("Data successfully saved to NDJSON file: " + clustersFile.toFile().getAbsoluteFile().getName());
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
