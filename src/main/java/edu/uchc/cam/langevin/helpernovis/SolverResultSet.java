package edu.uchc.cam.langevin.helpernovis;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SolverResultSet implements Serializable {

    public static final String TIME = "t";
    public static final String TIME_COLUMN = TIME;
    public static final int TIME_COLUMN_INDEX = 0;

    public ArrayList<double[]> values;  // List of rows (each row is a double[])
    public List<ColumnDescription> columnDescriptions;;

    public SolverResultSet() {
        this.columnDescriptions = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    public void setColumnDescriptions(List<ColumnDescription> columnDescriptions) {
        this.columnDescriptions = columnDescriptions;
    }

    public List<ColumnDescription> getColumnDescriptions() {
        return columnDescriptions;
    }

    public ArrayList<double[]> getValues() {
        return values;
    }

    public static void parseFile(File file, List<ColumnDescription> columnDescriptions, ArrayList<double[]> values) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // read the first line (column names)
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("empty file: " + file.getName());
            }

            String[] headers = headerLine.split(":");
            columnDescriptions.clear();
            for (String header : headers) {
                columnDescriptions.add(new ColumnDescription(header));
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
            for (int i = 0; i < columnDescriptions.size(); i++) {
                columnDescriptions.get(i).evaluateTriviality(values, i);
            }
        }
    }

}
