package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.helpers.OutputReverser;
import de.thkoeln.fentwums.netlist.backend.helpers.SignalBundler;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
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
            toplevelNode.setIdentifier(modulename);
            ElkLabel toplevelLabel = createLabel(modulename,  toplevelNode);
            toplevelNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            toplevelNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
            toplevelNode.setProperty(CoreOptions.ALGORITHM, "layered");
            toplevelNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
            toplevelNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
                    NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
            toplevelNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE));

            toplevelLabel.setDimensions(toplevelLabel.getText().length() * 7 + 1, 10);
        }

        try {
            checkModuleCompleteness(module);
        } catch (Exception e) {
            throw new RuntimeException("Netlist is not complete", e);
        }

        HashMap<String, Object> ports = (HashMap<String, Object>) module.get("ports");
        HashMap<String, Object> cells = (HashMap<String, Object>) module.get("cells");
        HashMap<String, Object> netnames = (HashMap<String, Object>) module.get("netnames");

        HashMap<Integer, SignalTree> signalMap;

        ElkNode toplevel = root.getChildren().getFirst();
        HierarchyTree hierarchyTree = new HierarchyTree(new HierarchicalNode(toplevel.getIdentifier(), null,
                new HashMap<>(), new ArrayList<>(), new HashMap<>(), toplevel));


        PortHandler portHandler = new PortHandler();

        signalMap = portHandler.createPorts(ports, modulename, toplevel);

        CellHandler cellHandler = new CellHandler();

        cellHandler.createCells(cells, modulename, toplevel, signalMap, hierarchyTree);

        NetnameHandler netHandler = new NetnameHandler();

        netHandler.handleNetnames(netnames, modulename, signalMap, hierarchyTree);
        netHandler.recreateHierarchy(signalMap, modulename);

        OutputReverser reverser = new OutputReverser();

        reverser.reversePorts(toplevel);

        SignalBundler bundler = new SignalBundler();
        bundler.setHierarchy(hierarchyTree);
        bundler.setTreeMap(signalMap);

        for (int key : signalMap.keySet()) {
            bundler.bundleSignalWithId(key);
        }

        CellCollapser collapser = new CellCollapser();
        collapser.setGroundTruth(toplevel);
        collapser.setHierarchy(hierarchyTree);

        collapser.collapseAllCells();
        collapser.expandAllCells();

        for (String child : hierarchyTree.getRoot().getChildren().keySet()) {
            collapser.collapseRecursively(hierarchyTree.getRoot().getChildren().get(child));
        }

        collapser.expandCellAt("ws2812_inst");
        collapser.expandCellAt("ws2812_inst rtw");
        collapser.expandCellAt("ws2812_inst rtw as");
//        collapser.expandCellAt("neorv32_inst");
//        collapser.expandCellAt("neorv32_inst neorv32_uart0_inst_true");
//        collapser.expandCellAt("neorv32_inst neorv32_uart0_inst_true neorv32_uart0_inst");
        //collapser.expandCellAt("ws2812_inst rtw as 9512");

//        collapser.expandCellAt("iceduino_button_inst");
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
