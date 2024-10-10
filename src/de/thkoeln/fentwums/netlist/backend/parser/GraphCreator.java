package de.thkoeln.fentwums.netlist.backend.parser;

import org.eclipse.elk.graph.ElkNode;

import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class GraphCreator {
    private ElkNode root;

    public GraphCreator() {
        root = createGraph();
        root.setIdentifier("root");
    }

    public GraphCreator(ElkNode root) {
        this.root = root;

    }

    public GraphCreator(String toplevelName) {
        root = createGraph();
        root.setIdentifier("root");
        ElkNode toplevelNode = createNode(root);
        toplevelNode.setIdentifier("cell");
        createLabel(toplevelName,  toplevelNode);
    }

    public void createGraphFromNetlist(HashMap<String, Object> module, String modulename) {
        if (root.getChildren().isEmpty()) {
            ElkNode toplevelNode = createNode(root);
            toplevelNode.setIdentifier("cell");
            createLabel(modulename,  toplevelNode);
        }

        try {
            checkModuleCompleteness(module);
        } catch (Exception e) {
            throw new RuntimeException("Netlist is not complete", e);
        }

        HashMap<String, Object> ports = (HashMap<String, Object>) module.get("ports");
        HashMap<String, Object> cells = (HashMap<String, Object>) module.get("cells");
        HashMap<String, Object> netnames = (HashMap<String, Object>) module.get("netnames");
    }

    public void checkModuleCompleteness(HashMap<String, Object> module) {
        if (module == null) {
            throw new NullPointerException("module is null");
        }
        if (!module.containsKey("ports")) {
            throw new RuntimeException("Module does not contain ports");
        }

        if (!module.containsKey("cells")) {
            throw new RuntimeException("Module does not contain cells");
        }

        if (!module.containsKey("netnames")) {
            throw new RuntimeException("Module does not contain netnames");
        }
    }
}
