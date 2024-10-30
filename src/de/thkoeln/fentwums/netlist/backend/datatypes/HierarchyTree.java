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

        for (String candidate : pathSplit) {
            currentNode.getChildren().get(candidate);

            if (currentNode == null) {
                System.out.println("Layer " + candidate + " in path " + path + " not found");
                return null;
            }
        }

        return currentNode;
    }
}
