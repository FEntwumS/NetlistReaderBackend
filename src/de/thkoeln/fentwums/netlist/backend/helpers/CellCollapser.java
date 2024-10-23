package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;

import java.util.*;

public class CellCollapser {
    private HierarchyTree hierarchy;
    private ElkNode groundTruth;

    public CellCollapser() {}

    public void setHierarchy(HierarchyTree hierarchy) {
        this.hierarchy = hierarchy;
    }

    public HierarchyTree getHierarchy() {
        return hierarchy;
    }

    public void setGroundTruth(ElkNode graph) {
        this.groundTruth = graph;
    }

    public ElkNode getGroundTruth() {
        return groundTruth;
    }

    public void collapseCell(String cellPath) {
        String[] cellPathSplit = cellPath.split(" ");

        // path should contain modulename as first parameter, but we can ignore it here
        // paths longer than 1 point to nodes below the toplevel, paths with length 1 point to the toplevel
        // will clearing a nodes children lead to loss of them? what will the gc do?
        // also need to create deep copy of tree (or is it not necessary?)

        // just store references to children and contained nodes in new parameters of the hierarchyTree
        // all references will be kept, and we can easily clear the lists
    }

    public void collapseAllCells() {
        // stores the children and contained edges of each node in the hierarchyTree and clears the respective
        // ElkNodes' lists

        // just use recursion to traverse the tree

        collapseRecursively(hierarchy.getRoot());
    }

    public void collapseRecursively(HierarchicalNode hNode) {
        ElkNode currentGraphNode = hNode.getNode();

        if (hNode.getChildList() == null) {
            hNode.setChildList(new ArrayList<>());
        }

        for (ElkNode child : currentGraphNode.getChildren()) {
            hNode.getChildList().add(child);
        }

        currentGraphNode.getChildren().clear();

        if (hNode.getEdgeList() == null) {
            hNode.setEdgeList(new ArrayList<ElkEdge>());
        }

        for (ElkEdge edge : currentGraphNode.getContainedEdges()) {
            hNode.getEdgeList().add(edge);
        }

        currentGraphNode.getContainedEdges().clear();

        for (String hChild : hNode.getChildren().keySet()) {
            collapseRecursively(hNode.getChildren().get(hChild));
        }
    }

    public void expandCell(String cellPath) {
        // find layer in hierarchy, then restore children and edges from hierarchyTree
        // allows of storing state of collapsed nodes inside the node pointed to by cellPath without additional code
    }
}
