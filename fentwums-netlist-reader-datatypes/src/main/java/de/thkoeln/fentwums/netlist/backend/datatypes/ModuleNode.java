package de.thkoeln.fentwums.netlist.backend.datatypes;

import de.thkoeln.fentwums.netlist.backend.interfaces.ICollapsableNode;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;

import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleNode implements ICollapsableNode {
    ElkNode moduleNode;
    AbstractMap<String, ICollapsableNode> children;
    List<ElkNode> childList;
    List<ElkEdge> edgeList;
    String cellType, cellName;

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
    public AbstractMap<String, ICollapsableNode> getChildren() {
        return children;
    }

    @Override
    public void setChildren(AbstractMap<String, ICollapsableNode> children) {
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

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getCellName() {
        return cellName;
    }

    public void setCellName(String cellName) {
        this.cellName = cellName;
    }

    public boolean isLoaded() {
        return !this.moduleNode.getChildren().isEmpty() || !this.childList.isEmpty();
    }

    public boolean isVisible() {
        ElkNode parent = this.moduleNode.getParent();

        if (parent == null) {
            return false;
        }

        for (ElkNode candidate : parent.getChildren()) {
            if (candidate.equals(this.moduleNode)) {
                return true;
            }
        }

        return false;
    }
}
