package de.thkoeln.fentwums.netlist.backend.datatypes;

public class HierarchyTree {
    private HierarchicalNode root;

    public HierarchyTree() {
        root = null;
    }

    public HierarchyTree(HierarchicalNode root) {
        this.root = root;
    }

    public HierarchicalNode getRoot() {
        return root;
    }

    public void setRoot(HierarchicalNode root) {
        this.root = root;
    }
}
