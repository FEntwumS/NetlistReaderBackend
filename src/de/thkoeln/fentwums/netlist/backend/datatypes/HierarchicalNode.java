package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkNode;

import java.util.ArrayList;
import java.util.HashMap;

public class HierarchicalNode {
    private String hName;
    private int lId;
    private HierarchicalNode parent;
    private HashMap<String, HierarchicalNode> children;
    private boolean isLeaf;
    private ArrayList<Vector> vectors;
    private ArrayList<Bundle> possibleBundles;
    private ElkNode node;

    public HierarchicalNode() {
        hName = "";
        lId = 0;
        parent = null;
        children = new HashMap<String, HierarchicalNode>(8);
        isLeaf = true;
        vectors = new ArrayList<>(8);
        possibleBundles = new ArrayList<>(8);
        node = null;
    }

    public HierarchicalNode(String hName, HierarchicalNode parent, HashMap<String, HierarchicalNode> children,
                            ArrayList<Vector> vectors, ArrayList<Bundle> possibleBundles, ElkNode node) {
        this.hName = hName;
        this.parent = parent;
        this.children = children;
        this.vectors = vectors;
        this.possibleBundles = possibleBundles;
        this.node = node;

        this.isLeaf = children == null || children.isEmpty();

        if(parent != null) {
            parent.getChildren().put(hName, this);
        }
    }

    public String getHName() {
        return hName;
    }

    public void setHName(String hName) {
        this.hName = hName;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public int getLId() {
        return lId;
    }

    public void setLId(int lId) {
        this.lId = lId;
    }

    public HierarchicalNode getParent() {
        return parent;
    }

    public void setParent(HierarchicalNode parent) {
        this.parent = parent;
    }

    public HashMap<String, HierarchicalNode> getChildren() {
        return children;
    }

    public void setChildren(HashMap<String, HierarchicalNode> children) {
        this.children = children;
    }

    public ArrayList<Vector> getVectors() {
        return vectors;
    }

    public void setVectors(ArrayList<Vector> vectors) {
        this.vectors = vectors;
    }

    public ArrayList<Bundle> getPossibleBundles() {
        return possibleBundles;
    }

    public void setPossibleBundles(ArrayList<Bundle> possibleBundles) {
        this.possibleBundles = possibleBundles;
    }

    public ElkNode getNode() {
        return node;
    }

    public void setNode(ElkNode node) {
        this.node = node;
    }
}
