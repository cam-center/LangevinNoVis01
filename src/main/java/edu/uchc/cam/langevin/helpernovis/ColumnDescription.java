package edu.uchc.cam.langevin.helpernovis;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("serial")
public class ColumnDescription implements Serializable {

    private String variableName = new String();
    private transient boolean isTrivial = false;

    public ColumnDescription(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public boolean isTrivial() {
        return isTrivial;
    }
    public void setTrivial(boolean isTrivial) {
        this.isTrivial = isTrivial;
    }

    public void evaluateTriviality(List<double[]> values, int columnIndex) {
        double firstValue = values.get(0)[columnIndex];
        isTrivial = values.stream().allMatch(row -> row[columnIndex] == firstValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDescription that = (ColumnDescription) o;
        return Objects.equals(variableName, that.variableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName);
    }

    @Override
    public String toString() {
        return "ColumnDescription{" +
                "variableName='" + variableName + '\'' +
                ", isTrivial=" + isTrivial +
                '}';
    }
}
