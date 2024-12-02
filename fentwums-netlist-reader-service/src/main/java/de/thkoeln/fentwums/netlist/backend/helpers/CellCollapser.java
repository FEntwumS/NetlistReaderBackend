package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CellCollapser {
    private HierarchyTree hierarchy;
    private ElkNode groundTruth;
    private static Logger logger = LoggerFactory.getLogger(CellCollapser.class);

    public CellCollapser() {
    }

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

    public void collapseCellAt(String cellPath) {
        collapseCell(findNode(cellPath));

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
        collapseCell(hNode);

        for (String hChild : hNode.getChildren().keySet()) {
            collapseRecursively(hNode.getChildren().get(hChild));
        }
    }

    public void collapseCell(HierarchicalNode hNode) {
        ElkNode currentGraphNode = hNode.getNode();

        if (hNode.getChildList() == null) {
            hNode.setChildList(new ArrayList<>());
        }

        if(hNode.getChildList().isEmpty()) {
            hNode.getChildList().addAll(currentGraphNode.getChildren());
        }

        currentGraphNode.getChildren().clear();

        if (hNode.getEdgeList() == null) {
            hNode.setEdgeList(new ArrayList<ElkEdge>());
        }

        if (hNode.getEdgeList().isEmpty()) {
            hNode.getEdgeList().addAll(currentGraphNode.getContainedEdges());
        }

        currentGraphNode.getContainedEdges().clear();
    }

    public void expandAllCells() {
        expandRecursively(hierarchy.getRoot());
    }

    public void expandRecursively(HierarchicalNode hNode) {
        expandCell(hNode);

        for (String hChild : hNode.getChildren().keySet()) {
            expandRecursively(hNode.getChildren().get(hChild));
        }
    }

    public void expandCellAt(String cellPath) {
        expandCell(findNode(cellPath));
        // find layer in hierarchy, then restore children and edges from hierarchyTree
        // allows of storing state of collapsed nodes inside the node pointed to by cellPath without additional code
    }

    public void expandCell(HierarchicalNode hNode) {
        ElkNode currentGraphNode = hNode.getNode();
        EList<ElkNode> graphChildren = currentGraphNode.getChildren();
        EList<ElkEdge> graphContainedEdges = currentGraphNode.getContainedEdges();

        if (!graphChildren.isEmpty() || !graphContainedEdges.isEmpty()) {
            return;
        }

        ArrayList<ElkNode> storedChildren = hNode.getChildList();
        ArrayList<ElkEdge> storedEdges = hNode.getEdgeList();

        graphChildren.addAll(storedChildren);

        graphContainedEdges.addAll(storedEdges);
    }

    private HierarchicalNode findNode(String cellPath) {
        String[] cellPathSplit = cellPath.trim().split(" ");

        HierarchicalNode currentNode = hierarchy.getRoot();
        HierarchicalNode nextNode;

        for (String fragment : cellPathSplit) {
            currentNode = currentNode.getChildren().get(fragment);

            if(currentNode == null) {
                logger.atError().setMessage("Could not find cell {} from cellpath {}").addArgument(fragment).addArgument(cellPath).log();

                return null;
            }
        }

        return currentNode;
    }
}
