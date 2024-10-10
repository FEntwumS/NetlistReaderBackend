package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.ArrayList;

public class SignalNode {
    private String sName;
    private int lId;
    private SignalNode hParent;
    private ArrayList<SignalNode> hChildren;
    private SignalNode sParent;
    private ArrayList<SignalNode> sChildren;
    private ArrayList<SignalNode> sLateralConnections;
    private boolean sVisited;

    public SignalNode() {
        sName = "";
        hParent = null;
        hChildren = new ArrayList<SignalNode>(8);
        sParent = null;
        sChildren = new ArrayList<SignalNode>(8);
        sLateralConnections = new ArrayList<SignalNode>(4);
        sVisited = false;
    }

    public SignalNode(String sName, SignalNode hParent, ArrayList<SignalNode> hChildren, SignalNode sParent, ArrayList<SignalNode> sChildren, ArrayList<SignalNode> sLateralConnections) {
        this.sName = sName;
        this.hParent = hParent;
        this.hChildren = hChildren;
        this.sParent = sParent;
        this.sChildren = sChildren;
        this.sLateralConnections = sLateralConnections;

        if(hParent != null && hParent.getHChildren() != null) {
            this.lId = hParent.getHChildren().size();
            hParent.getHChildren().add(this);
        }

        this.sVisited = false;
    }

    public String getSName() {
        return sName;
    }

    public void setSName(String sName) {
        this.sName = sName;
    }

    public int getLId() {
        return lId;
    }

    public void setLId(int lId) {
        this.lId = lId;
    }

    public SignalNode getHParent() {
        return hParent;
    }

    public void setHParent(SignalNode hParent) {
        this.hParent = hParent;
    }

    public ArrayList<SignalNode> getHChildren() {
        return hChildren;
    }

    public void setHChildren(ArrayList<SignalNode> hChildren) {
        this.hChildren = hChildren;
    }

    public SignalNode getSParent() {
        return sParent;
    }

    public void setSParent(SignalNode sParent) {
        this.sParent = sParent;
    }

    public ArrayList<SignalNode> getSChildren() {
        return sChildren;
    }

    public void setSChildren(ArrayList<SignalNode> sChildren) {
        this.sChildren = sChildren;
    }

    public ArrayList<SignalNode> getSLateralConnections() {
        return sLateralConnections;
    }

    public void setSLateralConnections(ArrayList<SignalNode> sLateralConnections) {
        this.sLateralConnections = sLateralConnections;
    }

    public boolean getSVisited() {
        return sVisited;
    }

    public void setSVisited(boolean sVisited) {
        this.sVisited = sVisited;
    }
}
