package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.helpers.EdgeBundler;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import de.thkoeln.fentwums.netlist.backend.helpers.OutputReverser;
import de.thkoeln.fentwums.netlist.backend.interfaces.ICollapsableNode;
import de.thkoeln.fentwums.netlist.backend.interfaces.IGraphCreator;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.options.SizeConstraint;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createGraph;

public class HierarchicalOrchestrator implements IGraphCreator {
    private static Logger logger = LoggerFactory.getLogger(HierarchicalOrchestrator.class);
    private ElkNode root;
    private ICollapsableNode rootNode;
    private String toplevelName = "";
    private HashMap<String, Object> modules;
    private HashMap<String, Object> blackBoxes;
    private NetlistCreationSettings settings;
    private ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps;
    private ReentrantLock lock = new ReentrantLock();

    public ElkNode createGraphFromNetlist(HashMap<String, Object> netlist, String modulename, HashMap<String, Object> blackBoxes,
                                          NetlistCreationSettings settings) {
        root = createGraph();
        String topName = "";

        ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps = new ConcurrentHashMap<>();


        root.setProperty(CoreOptions.ALGORITHM, "layered");
        root.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
        root.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
        root.setIdentifier("root");

        HashMap<String, Object> modules = (HashMap<String, Object>) netlist.get("modules");

        for (String module : modules.keySet()) {
            HashMap<String, Object> currentModule = (HashMap<String, Object>) modules.get(module);

            HashMap<String, Object> currentModuleAttributes = (HashMap<String, Object>) currentModule.get("attributes");

            if (currentModuleAttributes != null) {
                if (currentModuleAttributes.containsKey("top")) {
                    topName = module;
                    break;
                }
            }
        }

        if (topName.equals("")) {
            logger.error("No top level module found. Aborting...");
            return null;
        }

        ElkNode topNode = ElkElementCreator.createNewNode(root, topName);
        topNode.setProperty(FEntwumSOptions.CELL_TYPE, "HDL_ENTITY");
        topNode.setProperty(CoreOptions.INSIDE_SELF_LOOPS_ACTIVATE, true);

        PortHandler portHandler = new PortHandler();
        CellHandler cellHandler = new CellHandler();
        NetnameHandler netnameHandler = new NetnameHandler();

        rootNode = new ModuleNode(topNode);

        this.toplevelName = topName;
        this.blackBoxes = blackBoxes;
        this.settings = settings;
        this.signalMaps = signalMaps;
        this.modules = modules;

        portHandler.createPorts(modules, signalMaps, topNode, settings, topName, topName, null);
        cellHandler.createCells(modules, topNode, signalMaps, settings, blackBoxes, (ModuleNode) rootNode, topName,
                                topName);
        netnameHandler.handleNetnames(modules, signalMaps, settings, (ModuleNode) rootNode, topName, topName);
        ((ModuleNode) rootNode).setAsLoaded();

        EdgeBundler.bundleEdges(topNode, settings);

        if (settings.getPerformanceTarget() == PerformanceTarget.Preloading) {
            for (String child : rootNode.getChildren().keySet()) {
                addModulesRecursively(modules, blackBoxes, settings, (ModuleNode) rootNode.getChildren().get(child),
                                      signalMaps, ((ModuleNode) rootNode.getChildren().get(child)).getCellType(),
                                      topName + " " + child);
            }
        }

        OutputReverser reverser = new OutputReverser();

        reverser.reversePorts(root);

        return root;
    }

    @Override
    public String layoutGraph() {
        // Layout the graph
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        BasicProgressMonitor monitor = new BasicProgressMonitor();

        try {
            engine.layout(root, monitor);
        } catch (Exception e) {
            logger.error("Error during layout", e);
        }

        return ElkGraphJson.forGraph(root).omitLayout(false).omitZeroDimension(true)
                .omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();
    }

    @Override
    public ICollapsableNode getRoot() {
        return rootNode;
    }

    @Override
    public ElkNode getGraphRoot() {
        return root;
    }

    @Override
    public HashMap<String, NetInformation> getNetInformationMap() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addModulesRecursively(HashMap<String, Object> modules, HashMap<String, Object> blackBoxes,
                                      NetlistCreationSettings settings, ModuleNode currentModuleNode,
                                      ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps,
                                      String moduleName, String instancePath) {
        CellHandler cellHandler = new CellHandler();
        NetnameHandler netnameHandler = new NetnameHandler();

        cellHandler.createCells(modules, currentModuleNode.getNode(), signalMaps, settings, blackBoxes,
                                currentModuleNode, moduleName, instancePath);
        netnameHandler.handleNetnames(modules, signalMaps, settings, currentModuleNode, moduleName, instancePath);
        currentModuleNode.setAsLoaded();

        EdgeBundler.bundleEdges(currentModuleNode.getNode(), settings);

        for (String child : currentModuleNode.getChildren().keySet()) {
            addModulesRecursively(modules, blackBoxes, settings, (ModuleNode) currentModuleNode.getChildren().get(child),
                                  signalMaps, ((ModuleNode) currentModuleNode.getChildren().get(child)).getCellType(), instancePath + " " + child);
        }

    }

    public void loadModule(ModuleNode toLoad, String instancePath) {
        CellHandler cellHandler = new CellHandler();
        NetnameHandler netnameHandler = new NetnameHandler();
        CellCollapser collapser = new CellCollapser();

        logger.atInfo().setMessage("Loading module {}").addArgument(toLoad.getCellName()).log();
        cellHandler.createCells(this.modules, toLoad.getNode(), this.signalMaps, this.settings, this.blackBoxes, toLoad,
                                toLoad.getCellType(), instancePath);
        netnameHandler.handleNetnames(this.modules, this.signalMaps, this.settings, toLoad, toLoad.getCellType(), instancePath);

        toLoad.setAsLoaded();

        EdgeBundler.bundleEdges(toLoad.getNode(), this.settings);

        collapser.collapseRecursively(toLoad);

        logger.atInfo().setMessage("Finished loading module {}").addArgument(toLoad.getCellName()).log();
    }

    public NetlistCreationSettings getSettings() {
        return settings;
    }

    private void loadClickableModulesRecursively(String instancePath, ModuleNode currentNode) {
        if (currentNode.isVisible() && !currentNode.isLoaded()) {
            loadModule(currentNode, instancePath);
        }

        for (String child : currentNode.getChildren().keySet()) {
            loadClickableModulesRecursively(instancePath + " " + child,
                                            (ModuleNode) currentNode.getChildren().get(child));
        }
    }

    public void loadModulesIntelligently() {
        try {
            lock.lock();

            logger.info("Lock acquired, beginning loading modules");
            loadClickableModulesRecursively(this.toplevelName, (ModuleNode) this.rootNode);
        } finally {
            lock.unlock();
            logger.info("Lock released");

            System.gc();
            System.gc();

            logger.info("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        }
    }

    public void waitForLock() {
        try {
            lock.lock();
        } finally {
            lock.unlock();
        }
    }
}
