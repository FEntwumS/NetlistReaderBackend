package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkNode;

import java.util.concurrent.ConcurrentHashMap;

public class ModuleNode {
    ElkNode moduleNode;
    ConcurrentHashMap<String, ModuleNode> childNodes;

    public ModuleNode(ElkNode moduleNode) {
        this.moduleNode = moduleNode;
        this.childNodes = new ConcurrentHashMap<>();
    }

    public ElkNode getModuleNode() {
        return moduleNode;
    }

    public void setModuleNode(ElkNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    public ConcurrentHashMap<String, ModuleNode> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(ConcurrentHashMap<String, ModuleNode> childNodes) {
        this.childNodes = childNodes;
    }
}
