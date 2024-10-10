package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.ArrayList;

public class Bundle {
    /**
     * Index of the signal the bundle has been created around.
     */
    private int bundleSignalIndex;

    /**
     * List of all the signal indices contained in this bundle.
     */
    private ArrayList<Integer> bundleSignalList;

    public Bundle() {
        bundleSignalIndex = 0;
        bundleSignalList = new ArrayList<>(32);
    }

    public Bundle(int bundleSignalIndex) {
        this.bundleSignalIndex = bundleSignalIndex;
        bundleSignalList = new ArrayList<>(32);
    }

    public Bundle(int bundleSignalIndex, ArrayList<Integer> bundleSignalList) {
        this.bundleSignalIndex = bundleSignalIndex;
        this.bundleSignalList = bundleSignalList;
    }

    public Bundle(Bundle bundle) {
        if (bundle == null) {
            throw new NullPointerException();
        }

        this.bundleSignalIndex = bundle.getBundleSignalIndex();
        this.bundleSignalList = new ArrayList<>(bundle.getBundleSignalList());
    }

    public int getBundleSignalIndex() {
        return bundleSignalIndex;
    }

    public void setBundleSignalIndex(int bundleSignalIndex) {
        this.bundleSignalIndex = bundleSignalIndex;
    }

    public ArrayList<Integer> getBundleSignalList() {
        return bundleSignalList;
    }

    public void setBundleSignalList(ArrayList<Integer> bundleSignalList) {
        this.bundleSignalList = bundleSignalList;
    }
}
