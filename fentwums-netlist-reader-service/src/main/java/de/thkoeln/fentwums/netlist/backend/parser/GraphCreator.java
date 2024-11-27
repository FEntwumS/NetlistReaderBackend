package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import de.thkoeln.fentwums.netlist.backend.helpers.*;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.json.ElkGraphJson;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class GraphCreator {
    private ElkNode root;
    private HierarchyTree hierarchy;
    private static Logger logger = Logger.getLogger(GraphCreator.class.getName());

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
        LayoutMetaDataService service = LayoutMetaDataService.getInstance();
        service.registerLayoutMetaDataProviders(new FEntwumSOptions());

        root.setIdentifier("root");
        //root.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
        //root.setProperty(LayeredOptions.SPACING_BASE_VALUE, 35d);   // TODO Look for better spacing solution

        if (root.getChildren().isEmpty()) {
            ElkNode toplevelNode = createNode(root);
            toplevelNode.setIdentifier(modulename);
            ElkLabel toplevelLabel = ElkElementCreator.createNewLabel(modulename,  toplevelNode);
            toplevelNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            //toplevelNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
            toplevelNode.setProperty(CoreOptions.ALGORITHM, "layered");
            toplevelNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
            toplevelNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
                    NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
            toplevelNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE));
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

        SanityChecker checker = new SanityChecker();

        checker.checkGraph(root);

        CellCollapser collapser = new CellCollapser();
        collapser.setGroundTruth(toplevel);
        collapser.setHierarchy(hierarchyTree);

        collapser.collapseAllCells();
        collapser.expandAllCells();

        this.hierarchy = collapser.getHierarchy();

//        for (String child : hierarchyTree.getRoot().getChildren().keySet()) {
//            collapser.collapseRecursively(hierarchyTree.getRoot().getChildren().get(child));
//        }

//        collapser.expandCellAt("ws2812_inst");
//        collapser.expandCellAt("ws2812_inst rtw");
//        collapser.expandCellAt("ws2812_inst rtw as");
//        collapser.expandCellAt("ws2812_inst rtw ac");
//
//        collapser.expandCellAt("neorv32_inst");
//        collapser.expandCellAt("neorv32_inst neorv32_spi_inst_true.neorv32_spi_inst");

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

    public String layoutGraph() {
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        BasicProgressMonitor monitor = new BasicProgressMonitor();

        try {
            engine.layout(root, monitor);
        } catch (Exception e) {

        }

        return ElkGraphJson.forGraph(root).omitLayout(false).omitZeroDimension(true)
                .omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();
    }

    public HierarchyTree getHierarchyTree() {
        return hierarchy;
    }
}
