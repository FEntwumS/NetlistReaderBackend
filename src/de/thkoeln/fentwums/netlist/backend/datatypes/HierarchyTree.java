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

    public HierarchicalNode getNodeAt(String path) {
        String[] pathSplit = path.trim().split(" ");
        HierarchicalNode currentNode = root;

        for (int i = 1; i < pathSplit.length; i++) {
            currentNode = currentNode.getChildren().get(pathSplit[i]);

            if (currentNode == null) {
                //System.out.println("Layer " + pathSplit[i] + " in path " + path + " not found");
                return null;
            }
        }

        return currentNode;
    }
}
