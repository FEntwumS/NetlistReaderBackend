package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.Main;
import de.thkoeln.fentwums.netlist.backend.datatypes.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

/**
 * Helper class abstracting away the tedious aspects of graph creation. Calls the handlers for the different sections
 * of the netlist in order, applies post-processing (eg signal bundling) and calls the layouter
 */
public class GraphCreator {
	private ElkNode root;
	private HierarchyTree hierarchy;
	private HashMap<Integer, SignalTree> signalMap;
	private HashMap<String, NetInformation> NetInformationMap;
	private static Logger logger = LoggerFactory.getLogger(GraphCreator.class);

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
		createLabel(toplevelName, toplevelNode);
	}

	/**
	 * Gets the root of the layoutable graph
	 *
	 * @return The root
	 */
	public ElkNode getGraph() {
		return root;
	}

	/**
	 * Create a layoutable graph for a given module. Uses the different handlers and post-processing providers to
	 * transform the module into an ELK graph which can then be layouted. Also registers custom options with the
	 * layout provider to allow for data transfer of relevant attributes inside the graph
	 *
	 * @param module     The module containing the top level entity that is to be laid out
	 * @param modulename The name of the top level entity
	 * @param blackboxes HashMap containing the port directions of blackbox cells
	 */
	@SuppressWarnings("unchecked")
	public void createGraphFromNetlist(HashMap<String, Object> module, String modulename, HashMap<String, Object> blackboxes, NetlistCreationSettings settings) {
		root.setIdentifier("root");
		root.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);

		if (root.getChildren().isEmpty()) {
			ElkNode toplevelNode = createNode(root);
			toplevelNode.setIdentifier(modulename);
			ElkLabel toplevelLabel = ElkElementCreator.createNewEntityLabel(modulename, toplevelNode, settings);
			toplevelNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
			//toplevelNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
			toplevelNode.setProperty(CoreOptions.ALGORITHM, "layered");
			toplevelNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
			toplevelNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
					NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
			toplevelNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE));
			toplevelNode.setProperty(FEntwumSOptions.CELL_TYPE, "HDL_ENTITY");
		}

		try {
			// Perform simple netlist completeness check
			checkModuleCompleteness(module);
		} catch (Exception e) {
			logger.error("Netlist is not complete", e);

			throw new RuntimeException("Netlist is not complete", e);
		}

		HashMap<String, Object> ports = (HashMap<String, Object>) module.get("ports");
		HashMap<String, Object> cells = (HashMap<String, Object>) module.get("cells");
		HashMap<String, Object> netnames = (HashMap<String, Object>) module.get("netnames");
		NetInformationMap = new HashMap<String, NetInformation>();

		ElkNode toplevel = root.getChildren().getFirst();
		HierarchyTree hierarchyTree = new HierarchyTree(new HierarchicalNode(toplevel.getIdentifier(), null,
				new HashMap<>(), new ArrayList<>(), new HashMap<>(), toplevel));

		// Use the different handlers to create the layoutable graph

		PortHandler portHandler = new PortHandler();

		logger.info("Start creating ports");
		signalMap = portHandler.createPorts(ports, modulename, toplevel, settings);
		logger.info("Successfully created ports");

		CellHandler cellHandler = new CellHandler();

		logger.info("Start creating cells");
		cellHandler.createCells(cells, modulename, toplevel, signalMap, hierarchyTree, blackboxes, settings);
		logger.info("Successfully created cells");

		NetnameHandler netHandler = new NetnameHandler();

		logger.info("Start creating nets");
		netHandler.handleNetnames(netnames, modulename, signalMap, hierarchyTree, NetInformationMap, settings);
		logger.info("Successfully created nets");

		logger.info("Start recreating signal paths");
		netHandler.recreateSignals(signalMap, modulename, settings);
		logger.info("Successfully recreated signal paths");

		// Apply post-processing to optimise layout
		SelectInputRepositioner repositioner = new SelectInputRepositioner();

		logger.info("Start repositioning of select-cell inputs");
		repositioner.repositionSelect(toplevel);
		logger.info("Successfully repositioned select-cell inputs");

		// Reversing the order of output ports significantly reduces the number of edge crossings
		OutputReverser reverser = new OutputReverser();

		logger.info("Start reversing order of output ports");
		reverser.reversePorts(toplevel);
		logger.info("Successfully reversed order of output ports");

		// Bundling bundleable signals reduces visual clutter and improves the ordering of nodes in the final layout
		SignalBundler bundler = new SignalBundler();
		bundler.setHierarchy(hierarchyTree);
		bundler.setTreeMap(signalMap);

		logger.info("Start bundling signals");
		for (int key : signalMap.keySet()) {
			bundler.bundleSignalWithId(key, settings);
		}
		logger.info("Successfully bundled signals");

		// Check the graph for any obvious mistakes
		SanityChecker checker = new SanityChecker();

		// Mark any ports that are not connected to any signal
		PortMarker marker = new PortMarker();
		marker.mark(toplevel);

		checker.checkGraph(root);

		this.hierarchy = hierarchyTree;
	}

	/**
	 * Simple completeness check that ensures that the module contains all the relevant sections
	 *
	 * @param module The module containing the top level entity
	 */
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

	/**
	 * Layout the graph
	 *
	 * @return JSON string containing a representation of the layouted graph
	 */
	public String layoutGraph() {
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

	/**
	 * Gets the hierarchy
	 *
	 * @return The hierarchy
	 */
	public HierarchyTree getHierarchyTree() {
		return hierarchy;
	}

	/**
	 * Gets the HashMap containing all signal trees
	 *
	 * @return all signal trees
	 */
	public HashMap<Integer, SignalTree> getSignalTreeMap() {
		return signalMap;
	}

	/**
	 * Gets the HashMap connecting the signal names to the associated scopes and bitindices
	 *
	 * @return The HashMap
	 */
	public HashMap<String, NetInformation> getNetInformationMap() {
		return NetInformationMap;
	}
}
