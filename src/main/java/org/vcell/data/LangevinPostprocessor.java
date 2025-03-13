package org.vcell.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LangevinPostprocessor {

    public static final String FULL_BOND_DATA_CSV = "FullBondData.csv";
    public static final String FULL_COUNT_DATA_CSV = "FullCountData.csv";
    public static final String FULL_STATE_COUNT_DATA_CSV = "FullStateCountData.csv";
    public static final String SITE_PROPERTY_DATA_CSV__NOT_USED_ = "SitePropertyData.csv";
    public static final String MOLECULE_IDS_CSV = "MoleculeIDs.csv";
    public static final String CLUSTERS_TIME_PREFIX = "Clusters_Time_";

    /**
     * Converts Langevin's output to a singular .IDA file in dir
     * @param langevinOutputDir : The path to the folder which contains LangevinNoVis01's output files
     * @param idaFile: The path to the file to the IdaFile
     */
    public static void writeIdaFile(Path langevinOutputDir, Path idaFile) throws IOException {
//		int cluster_time = 0;
        // Initialize .ida file

        try (FileReader fullBondDataReader = new FileReader(new File(langevinOutputDir.toFile(), FULL_BOND_DATA_CSV));
             FileReader fullCountDataReader = new FileReader(new File(langevinOutputDir.toFile(), FULL_COUNT_DATA_CSV));
             FileReader fullStateCountDataReader = new FileReader(new File(langevinOutputDir.toFile(), FULL_STATE_COUNT_DATA_CSV));
             //FileReader sitePropertyDataReader = new FileReader(langevinOutputDir + SITE_PROPERTY_DATA_CSV);
             //FileReader clustersTimeReader = new FileReader(langevinOutputDir + "Clusters_Time_.csv")
        ) {
            CSVParser fullBondData = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter().withTrim().parse(fullBondDataReader);
            CSVParser fullCountData = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter().withTrim().parse(fullCountDataReader);
            CSVParser fullStateCountData = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter().withTrim().parse(fullStateCountDataReader);
            //CSVParser sitePropertyData = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter().withTrim().parse(sitePropertyDataReader);
            //CSVParser clustersTime = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter().withTrim().parse(clustersTimeReader);

            List<String> fullBondHeaders = fullBondData.getHeaderNames();
            List<String> fullCountHeaders = fullCountData.getHeaderNames();
            List<String> fullStateCountHeaders = fullStateCountData.getHeaderNames();
            //List<String> sitePropertyHeaders = sitePropertyData.getHeaderNames();
            //List<String> clustersTimeHeaders = clustersTime.getHeaderNames();

            var fullBondDataIter = fullBondData.iterator();
            var fullCountDataIter = fullCountData.iterator();
            var fullStateCountDataIter = fullStateCountData.iterator();
            //var sitePropertyDataIter = sitePropertyData.iterator();
            //var clustersTimeIter = clustersTime.iterator();

            // Create the header for the .ida file, which is a combination of all the headers from the Langevin output files
            // 'Time' headers are repeated, so we remove all 'Time' headers, and insert a single 't' header
            List<String> headers = new ArrayList<>();
            headers.add("t");
            headers.addAll(fullBondHeaders.stream().filter(s -> !s.equals("Time")).toList());
            headers.addAll(fullCountHeaders.stream().filter(s -> !s.equals("Time")).toList());
            headers.addAll(fullStateCountHeaders.stream().filter(s -> !s.equals("Time")).toList());
            //headers.addAll(sitePropertyHeaders.stream().filter(s -> !s.equals("Time")).toList());
            //headers.addAll(clustersTimeHeaders.stream().filter(s -> !s.equals("Time")).toList());
            List<String> combined_headers = headers.stream().map(s -> s.replace(" ", "_").replace(":","")).toList();


            try (FileWriter writer = new FileWriter(idaFile.toFile().getAbsoluteFile(), false)) {
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(' ').withRecordSeparator('\n'));

                // print header row
                String header_row = String.join(":", combined_headers);
                csvPrinter.print(header_row);
                csvPrinter.println();

                // print each data row
                boolean bFirstRow = true;
                while (true) {

                    CSVRecord fullBondDataRecord = fullBondDataIter.hasNext() ? fullBondDataIter.next() : null;
                    CSVRecord fullCountDataRecord = fullCountDataIter.hasNext() ? fullCountDataIter.next() : null;
                    CSVRecord fullStateCountDataRecord = fullStateCountDataIter.hasNext() ? fullStateCountDataIter.next() : null;
                    //CSVRecord sitePropertyDataRecord = sitePropertyDataIter.hasNext() ? sitePropertyDataIter.next() : null;
                    //CSVRecord clustersTimeRecord = clustersTimeIter.hasNext() ? clustersTimeIter.next() : null;

                    if (fullBondDataRecord == null || fullCountDataRecord == null || fullStateCountDataRecord == null) {
                        break;
                    }

                    // skip the rest of the rows if padded with zero times (an artifact of the way the data is written)
                    double time = fullBondDataRecord.get(0) != null ? Double.parseDouble(fullBondDataRecord.get(0)) : 0.0;
                    if (!bFirstRow && time == 0.0){
                        break;
                    }
                    bFirstRow = false;

                    // use fullBondData's first column as the time column (starts at 0 index)
                    for (int i = 0; i < fullBondDataRecord.size(); i++) {
                        csvPrinter.print(fullBondDataRecord.get(i));
                    }
                    for (int i = 1; i < fullCountDataRecord.size(); i++) {
                        csvPrinter.print(fullCountDataRecord.get(i));
                    }
                    for (int i = 1; i < fullStateCountDataRecord.size(); i++) {
                        csvPrinter.print(fullStateCountDataRecord.get(i));
                    }
                    //for (int i = 1; i < sitePropertyDataRecord.size(); i++) {
                    //    csvPrinter.print(sitePropertyDataRecord.get(i));
                    //}
                    //for (int i = 1; i < clustersTimeRecord.size(); i++) {
                    //	csvPrinter.print(clustersTimeRecord.get(i));
                    //}
                    csvPrinter.println(); // end line
                }
                csvPrinter.flush();
            }

            //
            // ==================================================================================================================
            //
            List<ClusterInfo> clusterInfoList = new ArrayList<>();
            Map<String, Integer> molecules = getMolecules(langevinOutputDir);

            File[] files = langevinOutputDir.toFile().listFiles((dir, name) -> name.startsWith(CLUSTERS_TIME_PREFIX) && name.endsWith(".csv"));
            if (files != null) {
                Integer timePointIndex = 0;
                for (File file : files) {
                    System.out.println(" ---------------------- " + file.getName());
                    int startIndex = file.getName().lastIndexOf("_") + 1; // After the first underscore
                    int endIndex = file.getName().lastIndexOf("."); // Before the ".csv"
                    String numericPart = file.getName().substring(startIndex, endIndex);
                    double time = Double.parseDouble(numericPart);
                    boolean hasNonTrivialClusters = false;

                    try (FileReader clustersTimeReader = new FileReader(file);
                         BufferedReader reader = new BufferedReader(clustersTimeReader);
                    ) {
                        String line;
                        int totalClusters = 0;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("Total clusters")) {
                                totalClusters = Integer.parseInt(line.split(",")[1].trim());
                            } else if (line.startsWith("Cluster Index")) {
                                ClusterInfo ci = new ClusterInfo();
                                ci.timePointIndex = timePointIndex;
                                ci.time = time;
                                hasNonTrivialClusters = true;   // found at least a cluster
                                String[] clusterIndexParts = line.split(",");
                                ci.clusterIndex = Integer.parseInt(clusterIndexParts[1].trim());
                                while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                                    String[] parts = line.split(",");
                                    if(parts[0].trim().equals("Size")) {
                                        ci.size = Integer.parseInt(parts[1].trim());
                                    } else {
                                        ci.clusterComponents.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                                    }
                                }
                                clusterInfoList.add(ci);
                                System.out.println("Parsed Cluster  " + ci.clusterIndex);
                            }
                        }
                    }
                    if(!hasNonTrivialClusters) {    // only trivial clusters, we save an empty ClusterInfo object
                        ClusterInfo ci = new ClusterInfo();
                        ci.timePointIndex = timePointIndex;
                        ci.time = time;
                        clusterInfoList.add(ci);
                    }
                    timePointIndex++;
                }
                System.out.println("Done all files");
            }
        }
    }

    static class ClusterInfo {  // info on non-trivial cluster (2 molecules or more)
        int timePointIndex = -1;
        double time = -1.0;
        int clusterIndex = -1;
        int size = 0;
        Map<String, Integer> clusterComponents = new LinkedHashMap<>(); // key = molecule name, value = number of molecules in the cluster
    }

    private static Map<String, Integer> getMolecules(Path langevinOutputDir) throws IOException {
        File file = new File(langevinOutputDir.toFile(), MOLECULE_IDS_CSV);
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        Map<String, Integer> sortedOccurrences = new LinkedHashMap<>(); // key = molecule name, value = number of molecules
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String key = parts[1].trim();
                    occurrences.put(key, occurrences.getOrDefault(key, 0) + 1);
                }
            }
            occurrences.keySet().stream().sorted().forEach(key -> sortedOccurrences.put(key, occurrences.get(key)));
        }
        return sortedOccurrences;
    }

	public ArrayList<String> readClustersToArray_NOT_USED_(String path) throws FileNotFoundException {

		File full = new File(path);
		File dir = new File(full.getParent());
		ArrayList<String> array = new ArrayList<>();

		String[] file_paths = dir.list();
		for (String f: file_paths) {
			if (f.contains("Clusters_Time_")) {
				try(Scanner scanner = new Scanner(new File(dir, f))) {
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						line = line.replace(",",":");
						int index = line.indexOf(":");
						if (!(line.equals(""))) {
							if (array.size() < 1) {
								array.add(line.substring(0, index).replace(" ", "_")); //
								array.add(line.substring(index + 1).trim());
							} else {
								array.add(line.substring(index + 1));
								break;
							}
						}
					}
				}
			}
		}
		return array;
	}

}
