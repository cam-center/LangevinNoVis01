package edu.uchc.cam.langevin.helpernovis;

import java.io.*;
import java.util.*;

public class SolverResultSet implements Serializable {

    public static final String TIME = "t";
    public static final String TIME_COLUMN = TIME;
    public static final int TIME_COLUMN_INDEX = 0;

    public static final String HeaderSeparator = ":";
    public static final String ValuesSeparator = " ";

    // list of rows, each row is a double[] (values of the variables at a particular timepoint
    // since time is always the first column, for each row the first double is the time
    public ArrayList<double[]> values;
    public List<ColumnDescription> columnDescriptions;  // list of variables, each entry is a variable

    public enum DuplicateMode {
        CopyValues,
        ZeroInitialize
    }

    public SolverResultSet() {
        this.columnDescriptions = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    public static SolverResultSet deepCopy(SolverResultSet original, DuplicateMode mode) {
        SolverResultSet copy = new SolverResultSet();
        copy.columnDescriptions = new ArrayList<>(original.columnDescriptions);
        copy.values = new ArrayList<>();
        for (double[] originalRow : original.values) {
            double[] copyRow = new double[originalRow.length];
            copyRow[0] = originalRow[0];    // we always copy time
            if(mode == DuplicateMode.CopyValues) {
                // we copy the rest of the doubles in the array, from column 1 to the last column
                System.arraycopy(originalRow, 1, copyRow, 1, originalRow.length-1);
            }
            copy.values.add(copyRow);
        }
        return copy;
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

    public double[] getRow(int row){
        return values.get(row);
    }
    public int getRowCount(){
        return (values.size());
    }
//    public List<double[]> getRows(){
//        return Collections.unmodifiableList(this.values);       // see also getValues() above
//    }
    public ColumnDescription getColumnDescriptions(int index) {
        if(index < columnDescriptions.size()) {
            return columnDescriptions.get(index);
        } else {
            throw new ArrayIndexOutOfBoundsException("RowColumnResultSet:getColumnDescriptions(int index), index=" + index + ", total count=" + columnDescriptions.size());
        }
    }



    public static void parseFile(File file, List<ColumnDescription> columnDescriptions, ArrayList<double[]> values) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // read the first line (column names)
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("empty file: " + file.getName());
            }

            String[] headers = headerLine.split(HeaderSeparator);
            columnDescriptions.clear();
            for (String header : headers) {
                columnDescriptions.add(new ColumnDescription(header));
            }

            // read and parse the data lines
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(ValuesSeparator);
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
