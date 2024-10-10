package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.ArrayList;

public class Vector {
    private String vectorName;
    private ArrayList<Bundle> vectorSignalBundles;
    private ArrayList<Integer> indexInVector;

    public Vector() {
        vectorName = "";
        vectorSignalBundles = new ArrayList<>(32);
        indexInVector = new ArrayList<>(32);
    }

    public Vector(String vectorName, ArrayList<Bundle> vectorSignalBundles, ArrayList<Integer> indexInVector) {
        this.vectorName = vectorName;
        this.vectorSignalBundles = vectorSignalBundles;
        this.indexInVector = indexInVector;
    }

    public Vector(Vector vector) {
        if(vector == null) {
            throw new NullPointerException("vector is null");
        }

        this.vectorName = vector.vectorName;
        this.vectorSignalBundles = vector.vectorSignalBundles;
        this.indexInVector = vector.indexInVector;
    }

    public String getVectorName() {
        return vectorName;
    }

    public void setVectorName(String vectorName) {
        this.vectorName = vectorName;
    }

    public ArrayList<Bundle> getVectorSignalBundles() {
        return vectorSignalBundles;
    }

    public void setVectorSignalBundles(ArrayList<Bundle> vectorSignalBundles) {
        this.vectorSignalBundles = vectorSignalBundles;
    }

    public ArrayList<Integer> getIndexInVector() {
        return indexInVector;
    }

    public void setIndexInVector(ArrayList<Integer> indexInVector) {
        this.indexInVector = indexInVector;
    }
}
