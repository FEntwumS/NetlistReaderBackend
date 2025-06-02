package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.ModuleNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalOccurences;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.options.SizeConstraint;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createGraph;

public class HierarchicalOrchestrator {
    private static Logger logger = LoggerFactory.getLogger(HierarchicalOrchestrator.class);

    public ElkNode createGraphFromNetlist(HashMap<String, Object> netlist, HashMap<String, Object> blackBoxes,
                                          NetlistCreationSettings settings) {
        ElkNode rootNode = createGraph();
        String topName = "";

        ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps = new ConcurrentHashMap<>();


        rootNode.setProperty(CoreOptions.ALGORITHM, "layered");
        rootNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
        rootNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
        rootNode.setIdentifier("root");

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

        ElkNode topNode = ElkElementCreator.createNewNode(rootNode, topName);
        topNode.setProperty(FEntwumSOptions.CELL_TYPE, "HDL_ENTITY");

        PortHandler portHandler = new PortHandler();
        CellHandler cellHandler = new CellHandler();
        NetnameHandler netnameHandler = new NetnameHandler();

        ModuleNode currentModuleNode = new ModuleNode(topNode);

        portHandler.createPorts(modules, signalMaps, topNode, settings, topName, topName);
        cellHandler.createCells(modules, topNode, signalMaps, settings, blackBoxes, currentModuleNode, topName,
                                topName);
        netnameHandler.handleNetnames(modules, signalMaps, settings, currentModuleNode, topName, topName);

        for (String child : currentModuleNode.getChildNodes().keySet()) {
            addModulesRecursively(modules, blackBoxes, settings, currentModuleNode.getChildNodes().get(child),
                                  signalMaps, child, topName + " " + child);
        }

        // Layout the graph
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        BasicProgressMonitor monitor = new BasicProgressMonitor();

        try {
            engine.layout(rootNode, monitor);
        } catch (Exception e) {
            logger.error("Error during layout", e);
        }

        return rootNode;
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

        for (String child : currentModuleNode.getChildNodes().keySet()) {
            addModulesRecursively(modules, blackBoxes, settings, currentModuleNode.getChildNodes().get(child),
                                  signalMaps, child, instancePath + " " + child);
        }

    }
}
