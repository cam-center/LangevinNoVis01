package edu.uchc.cam.langevin.helpernovis;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class SolverResultSet implements Serializable {

    public static final String TIME = "t";
    public static final String TIME_COLUMN = TIME;
    public static final int TIME_COLUMN_INDEX = 0;

    private ArrayList<double[]> values = new ArrayList<>();  // List of rows (each row is a double[])
    private ColumnDescription[] columnDescriptions = null;





    public static void parseFile(File file, ColumnDescription[] columnDescriptions, ArrayList<double[]> values) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // read the first line (column names)
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("empty file: " + file.getName());
            }

            String[] headers = headerLine.split(":");
            columnDescriptions = new ColumnDescription[headers.length];

            for (int i = 0; i < headers.length; i++) {
                columnDescriptions[i] = new ColumnDescription(headers[i]);
            }

            // read and parse the data lines
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(" ");
                double[] rowData = Arrays.stream(tokens)
                        .mapToDouble(Double::parseDouble)
                        .toArray();
                values.add(rowData);
            }

            // evaluate triviality for each column
            for (int i = 0; i < columnDescriptions.length; i++) {
                columnDescriptions[i].evaluateTriviality(values, i);
            }
        }
    }

}
