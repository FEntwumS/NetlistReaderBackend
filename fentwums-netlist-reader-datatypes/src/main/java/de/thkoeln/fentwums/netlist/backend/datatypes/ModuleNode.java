package de.thkoeln.fentwums.netlist.backend.datatypes;

import de.thkoeln.fentwums.netlist.backend.interfaces.internal.CollapsableNode;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleNode implements CollapsableNode {
    ElkNode moduleNode;
    AbstractMap<String, CollapsableNode> children;
    List<ElkNode> childList;
    List<ElkEdge> edgeList;

    public ModuleNode(ElkNode moduleNode) {
        this.moduleNode = moduleNode;
        this.children = new ConcurrentHashMap<>();
    }

    @Override
    public ElkNode getNode() {
        return moduleNode;
    }

    @Override
    public void setNode(ElkNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    @Override
    public AbstractMap<String, CollapsableNode> getChildren() {
        return children;
    }

    @Override
    public void setChildren(AbstractMap<String, CollapsableNode> children) {
        this.children = children;
    }

    @Override
    public void setChildList(List<ElkNode> childList) {
        this.childList = childList;
    }

    @Override
    public List<ElkNode> getChildList() {
        return childList;
    }

    @Override
    public void setEdgeList(List<ElkEdge> edgeList) {
        this.edgeList = edgeList;
    }

    @Override
    public List<ElkEdge> getEdgeList() {
        return edgeList;
    }
}
