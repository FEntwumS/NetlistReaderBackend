package de.thkoeln.fentwums.netlist.backend.datatypes;

public class SignalTree {
    private int sId;
    private char sValue;
    private SignalTree root;

    public SignalTree() {
        sId = 0;
        sValue = 'x';
        root = null;
    }

    public SignalTree(int sId, char sValue, SignalTree root) {
        this.sId = sId;
        this.sValue = sValue;
        this.root = root;
    }

    public int getSId() {
        return sId;
    }

    public void setSId(int sId) {
        this.sId = sId;
    }

    public char getSValue() {
        return sValue;
    }

    public void setSValue(char sValue) {
        this.sValue = sValue;
    }

    public SignalTree getRoot() {
        return root;
    }

    public void setRoot(SignalTree root) {
        this.root = root;
    }
}
