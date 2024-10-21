package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkPort;

import java.util.HashMap;

public class SignalNode {
    private String sName;
    private SignalNode hParent;
    private HashMap<String, SignalNode> hChildren;
    private SignalNode sParent;
    private HashMap<String, SignalNode> sChildren;
    private HashMap<String, SignalNode> sLateralConnections;
    private boolean sVisited;
    private boolean isSource;
    private ElkPort sPort;

    public SignalNode() {
        sName = "";
        hParent = null;
        hChildren = new HashMap<String, SignalNode>(8);
        sParent = null;
        sChildren = new HashMap<String, SignalNode>(8);
        sLateralConnections = new HashMap<String, SignalNode>(4);
        sVisited = false;
    }

    public SignalNode(String sName, SignalNode hParent, HashMap<String, SignalNode> hChildren, SignalNode sParent,
                      HashMap<String, SignalNode> sChildren, HashMap<String, SignalNode> sLateralConnections,
                      boolean isSource, ElkPort sPort) {
        this.sName = sName;
        this.hParent = hParent;
        this.hChildren = hChildren;
        this.sParent = sParent;
        this.sChildren = sChildren;
        this.sLateralConnections = sLateralConnections;
        this.sPort = sPort;

        if(hParent != null && hParent.getHChildren() != null) {
            hParent.getHChildren().put(this.sName, this);
        }

        this.isSource = isSource;

        this.sVisited = false;
    }

    public String getSName() {
        return sName;
    }

    public void setSName(String sName) {
        this.sName = sName;
    }

    public SignalNode getHParent() {
        return hParent;
    }

    public void setHParent(SignalNode hParent) {
        this.hParent = hParent;
    }

    public HashMap<String, SignalNode> getHChildren() {
        return hChildren;
    }

    public void setHChildren(HashMap<String, SignalNode> hChildren) {
        this.hChildren = hChildren;
    }

    public SignalNode getSParent() {
        return sParent;
    }

    public void setSParent(SignalNode sParent) {
        this.sParent = sParent;
    }

    public HashMap<String, SignalNode> getSChildren() {
        return sChildren;
    }

    public void setSChildren(HashMap<String, SignalNode> sChildren) {
        this.sChildren = sChildren;
    }

    public HashMap<String, SignalNode> getSLateralConnections() {
        return sLateralConnections;
    }

    public void setSLateralConnections(HashMap<String, SignalNode> sLateralConnections) {
        this.sLateralConnections = sLateralConnections;
    }

    public boolean getSVisited() {
        return sVisited;
    }

    public void setSVisited(boolean sVisited) {
        this.sVisited = sVisited;
    }

    public boolean getIsSource() {
        return isSource;
    }

    public void setIsSource(boolean isSource) {
        this.isSource = isSource;
    }

    public ElkPort getSPort() {
        return sPort;
    }

    public void setSPort (ElkPort sPort) {
        this.sPort = sPort;
    }
}
