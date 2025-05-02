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

    // TODO: bring parseFile here

}
