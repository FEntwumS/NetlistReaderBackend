package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.ArrayList;

public class HierarchicalNode {
    private String hName;
    private int lId;
    private HierarchicalNode parent;
    private ArrayList<HierarchicalNode> children;
    private boolean isLeaf;
    private ArrayList<Vector> vectors;
    private ArrayList<Bundle> possibleBundles;

    public HierarchicalNode() {
        hName = "";
        lId = 0;
        parent = null;
        children = new ArrayList<HierarchicalNode>(8);
        isLeaf = true;
        vectors = new ArrayList<>(8);
        possibleBundles = new ArrayList<>(8);
    }

    public HierarchicalNode(String hName, HierarchicalNode parent, ArrayList<HierarchicalNode> children, ArrayList<Vector> vectors, ArrayList<Bundle> possibleBundles) {
        this.hName = hName;
        this.parent = parent;
        this.children = children;
        this.vectors = vectors;
        this.possibleBundles = possibleBundles;

        this.isLeaf = children == null || children.isEmpty();

        if(parent != null && parent.getChildren() != null) {
            this.lId = parent.children.size();
            parent.children.add(this);
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

    public ArrayList<HierarchicalNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<HierarchicalNode> children) {
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
}
