package de.thkoeln.fentwums.netlist.backend.interfaces;

import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import java.util.AbstractMap;
import java.util.List;

public interface ICollapsableNode {
    public void setChildList(List<ElkNode> childList);
    public List<ElkNode> getChildList();

    public void setEdgeList(List<ElkEdge> edgeList);
    public List<ElkEdge> getEdgeList();

    public void setNode(ElkNode node);
    public ElkNode getNode();

    public void setChildren(AbstractMap<String, ICollapsableNode> children);
    public AbstractMap<String, ICollapsableNode> getChildren();
}
