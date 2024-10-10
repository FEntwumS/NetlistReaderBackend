package de.thkoeln.fentwums.netlist.backend.parser;

import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortConstraints;
import org.eclipse.elk.core.options.PortLabelPlacement;
import org.eclipse.elk.graph.ElkNode;

import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class GraphCreator {
    private ElkNode root;

    public GraphCreator() {
        root = createGraph();
    }

    public GraphCreator(ElkNode root) {
        this.root = root;
    }

    public GraphCreator(String toplevelName) {
        root = createGraph();
        ElkNode toplevelNode = createNode(root);
        toplevelNode.setIdentifier("cell");
        createLabel(toplevelName,  toplevelNode);
    }

    public ElkNode getGraph() {
        return root;
    }

    public void createGraphFromNetlist(HashMap<String, Object> module, String modulename) {
        if (root.getChildren().isEmpty()) {
            ElkNode toplevelNode = createNode(root);
            toplevelNode.setIdentifier("cell");
            createLabel(modulename,  toplevelNode);
            toplevelNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_SIDE);
            // toplevelNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, PortLabelPlacement.OUTSIDE);
        }

        try {
            checkModuleCompleteness(module);
        } catch (Exception e) {
            throw new RuntimeException("Netlist is not complete", e);
        }

        HashMap<String, Object> ports = (HashMap<String, Object>) module.get("ports");
        HashMap<String, Object> cells = (HashMap<String, Object>) module.get("cells");
        HashMap<String, Object> netnames = (HashMap<String, Object>) module.get("netnames");

        PortHandler portHandler = new PortHandler();

        portHandler.createPorts(ports, modulename, root.getChildren().getFirst());
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
