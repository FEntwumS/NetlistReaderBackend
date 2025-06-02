package de.thkoeln.fentwums.netlist.backend.datatypes;

import de.thkoeln.fentwums.netlist.backend.interfaces.internal.CollapsableNode;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleNode implements CollapsableNode {
    ElkNode moduleNode;
    ConcurrentHashMap<String, ModuleNode> childNodes;
    List<ElkNode> childList;
    List<ElkEdge> edgeList;

    public ModuleNode(ElkNode moduleNode) {
        this.moduleNode = moduleNode;
        this.childNodes = new ConcurrentHashMap<>();
    }

    @Override
    public ElkNode getNode() {
        return moduleNode;
    }

    @Override
    public void setNode(ElkNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    public ConcurrentHashMap<String, ModuleNode> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(ConcurrentHashMap<String, ModuleNode> childNodes) {
        this.childNodes = childNodes;
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
