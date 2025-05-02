package edu.uchc.cam.langevin.helpernovis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileMapper {

    /*
     * read a directory, identifies files that match a naming convention (prefix, numeric suffix, extension)
     * returns a map with names as keys, File as values
     * sorts they key by the numeric suffix, keeps the one without a counter (if present) as first element,
     * ignores the ones with non-numeric suffixes
     */
    public static Map<String, File> getFileMapByName(File directory, String prefix, String extension) throws FileNotFoundException {

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new FileNotFoundException("The specified directory does not exist: " + directory);
        }

        Map<Integer, File> sortedMap = new TreeMap<>(); // treeMap ensures numeric sorting
        File baseFile = null;       // stores the file without a numeric counter, if present

        Pattern pattern = Pattern.compile("^" + prefix + "(?:_(\\d+))?" + extension + "$");

        for (File file : directory.listFiles()) {
            String fileName = file.getName();
            Matcher matcher = pattern.matcher(fileName);

            if (matcher.matches()) {
                String counterStr = matcher.group(1); // get the numeric counter if present
                if (counterStr == null) {
                    baseFile = file; // store the base file if it has no counter
                } else {
                    int counter = Integer.parseInt(counterStr);
                    sortedMap.put(counter, file);
                }
            }
        }

        // Merge results into LinkedHashMap (maintaining order)
        LinkedHashMap<String, File> fileMap = new LinkedHashMap<>();
        if (baseFile != null) {
            fileMap.put(prefix, baseFile); // Put the base file first
        }
        sortedMap.forEach((counter, file) -> fileMap.put(prefix + "_" + counter, file));

        return fileMap;
    }

    public static File createDirectory(String parentDir, String tempDirName) throws IOException {
        // ensure the base directory exists
        Path parentPath = Paths.get(parentDir);
        if (!Files.exists(parentPath)) {
            throw new IOException("parent directory does not exist: " + parentDir);
        }

        // construct the full directory path
        Path dirPath = parentPath.resolve(tempDirName);

        // create the directory if it does not exist
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        return dirPath.toFile(); // return as a File object
    }


    public static void main(String[] args) throws FileNotFoundException {
        File directory = new File("path/to/your/directory"); // Change to your actual path
        String prefix = "mySim";
        String extension = ".ida";

        Map<String, File> fileMap = getFileMapByName(directory, prefix, extension);

        // Print the results
        fileMap.forEach((name, file) -> System.out.println(name + " -> " + file.getAbsolutePath()));
    }
}