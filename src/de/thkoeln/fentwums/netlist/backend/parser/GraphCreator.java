package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;

import java.util.ArrayList;
import java.util.EnumSet;
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

    @SuppressWarnings("unchecked")
    public void createGraphFromNetlist(HashMap<String, Object> module, String modulename) {
        root.setIdentifier("root");
        root.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
        root.setProperty(LayeredOptions.SPACING_BASE_VALUE, 35d);   // TODO Look for better spacing solution

        if (root.getChildren().isEmpty()) {
            ElkNode toplevelNode = createNode(root);
            toplevelNode.setIdentifier("cell");
            ElkLabel toplevelLabel = createLabel(modulename,  toplevelNode);
            toplevelNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            toplevelNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
            toplevelNode.setProperty(CoreOptions.ALGORITHM, "layered");
            toplevelNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
            toplevelNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
                    NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
            toplevelNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE));

            toplevelLabel.setDimensions(toplevelLabel.getText().length() * 7, 15);
            toplevelNode.setProperty(CoreOptions.NODE_SIZE_MINIMUM, new KVector(toplevelLabel.getWidth(), 0));
        }

        try {
            checkModuleCompleteness(module);
        } catch (Exception e) {
            throw new RuntimeException("Netlist is not complete", e);
        }

        HashMap<String, Object> ports = (HashMap<String, Object>) module.get("ports");
        HashMap<String, Object> cells = (HashMap<String, Object>) module.get("cells");
        HashMap<String, Object> netnames = (HashMap<String, Object>) module.get("netnames");

        ArrayList<SignalTree> signalTreeList;
        ArrayList<HierarchyTree> hierarchyTreeList;

        ElkNode toplevel = root.getChildren().getFirst();

        PortHandler portHandler = new PortHandler();

        signalTreeList = portHandler.createPorts(ports, modulename, toplevel);

        CellHandler cellHandler = new CellHandler();

        hierarchyTreeList = cellHandler.createCells(cells, modulename, toplevel, signalTreeList);
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
