package de.thkoeln.fentwums.netlist.backend.datatypes;

public class SignalTree {
    private int sId;
    private char sValue;
    private SignalNode hRoot;
    private SignalNode sRoot;   // ASSUMPTION: Every signal only has ONE source

    public SignalTree() {
        sId = 0;
        sValue = 'x';
        hRoot = null;
    }

    public SignalTree(int sId, char sValue, SignalNode hRoot) {
        this.sId = sId;
        this.sValue = sValue;
        this.hRoot = hRoot;
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

    public SignalNode getHRoot() {
        return hRoot;
    }

    public void setHRoot(SignalNode hRoot) {
        this.hRoot = hRoot;
    }

    public SignalNode getSRoot() {
        return sRoot;
    }

    public void setSRoot(SignalNode sRoot) {
        this.sRoot = sRoot;
    }

    public SignalNode getNodeAt(String path) {
        String[] pathSplit = path.trim().split(" ");
        SignalNode currentNode = hRoot;

        if (path.isEmpty()) {
            return currentNode;
        }

        for (String fragment : pathSplit) {
            currentNode = currentNode.getHChildren().get(fragment);

            if (currentNode == null) {
                return null;
            }
        }

        return currentNode;
    }
}
