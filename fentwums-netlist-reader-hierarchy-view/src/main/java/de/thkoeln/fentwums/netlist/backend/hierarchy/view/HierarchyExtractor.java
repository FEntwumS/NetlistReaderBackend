package de.thkoeln.fentwums.netlist.backend.hierarchy.view;

import de.thkoeln.fentwums.netlist.backend.datatypes.ParameterInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.PortDirection;
import de.thkoeln.fentwums.netlist.backend.datatypes.PortInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.HierarchyContainerSubNodeType;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createGraph;

public class HierarchyExtractor {
	private final static Logger logger = LoggerFactory.getLogger(HierarchyExtractor.class);

	public String extractHierarchy(HashMap<String, Object> netlist) {
		ElkNode root = createGraph();
		root.setIdentifier("root");
		root.setProperty(CoreOptions.ALGORITHM, "layered");
		root.setProperty(CoreOptions.DIRECTION, Direction.DOWN);

		String topName = "", topType = "";

		HashMap<String, Object> modules = (HashMap<String, Object>) netlist.get("modules");

		logger.info("Looking for toplevel entity");

		for (String module : modules.keySet()) {
			HashMap<String, Object> currentModule = (HashMap<String, Object>) modules.get(module);

			HashMap<String, Object> currentModuleAttributes = (HashMap<String, Object>) currentModule.get("attributes");

			if (currentModuleAttributes != null) {
				if (currentModuleAttributes.containsKey("top")) {
					topName = module;

					topType = module;
					break;
				}
			}
		}

		if (topName.equals("")) {
			logger.error("No top level module found. Aborting...");
			return null;
		}

		logger.atInfo().setMessage("Found top level entity with name {} and type {}").addArgument(topName).addArgument(topType).log();

		ElkNode ancestor = addModuleToHierarchy(netlist, topType, topName, root, null, "root");

		extractModuleRecursively(netlist, topType, root, ancestor, topName);

		// Layout graph

		logger.info("Starting layout");
		RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
		BasicProgressMonitor monitor = new BasicProgressMonitor();

		try {
			engine.layout(root, monitor);
		} catch (Exception e) {
			logger.error("Error during layout", e);
			return null;
		}
		logger.info("Finished layout");

		logger.info("Starting post-layout fixup");
		postLayoutFixup(root);
		logger.info("Finished post-layout fixup");

		return ElkGraphJson.forGraph(root).omitLayout(false).omitZeroDimension(true)
				.omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();
	}

	private void extractModuleRecursively(HashMap<String, Object> netlist, String moduleType, ElkNode root,
										  ElkNode ancestor, String ancestorName) {
		HashMap<String, Object> modules = (HashMap<String, Object>) netlist.get("modules");

		if (!modules.containsKey(moduleType)) {
			logger.atError().setMessage("Could not find module type {} in netlist").addArgument(moduleType).log();
			return;
		}

		HashMap<String, Object> module, cells, currentCell, currentCellAttributes;
		boolean isHidden = true, isDerived = true;
		String cellType;

		module = (HashMap<String, Object>) modules.get(moduleType);
		cells = (HashMap<String, Object>) module.get("cells");

		for(String cellName : cells.keySet()) {
			currentCell = (HashMap<String, Object>) cells.get(cellName);
			currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");
			cellType = (String) currentCell.get("type");

			isHidden = currentCell.get("hide_name").equals("0");
			isDerived = !(currentCellAttributes.containsKey("module_not_derived") || cellType.contains("paramod"));

			if (!isHidden && !isDerived) {
				ElkNode a = addModuleToHierarchy(netlist, cellType, ancestorName + " " + cellName, root, ancestor, ancestorName);

				extractModuleRecursively(netlist, cellType, root, a, ancestorName + " " + cellName);
			}
		}
	}

	private ElkNode addModuleToHierarchy(HashMap<String, Object> netlist, String moduleType, String moduleName,
										 ElkNode root, ElkNode ancestor, String ancestorName) {
		HashMap<String, Object> modules = (HashMap<String, Object>) netlist.get("modules");

		if (!modules.containsKey(moduleType)) {
			logger.atError().setMessage("Could not find module name {}, type {} in netlist").addArgument(moduleName).addArgument(moduleType).log();
			return null;
		}

		List<PortInformation> portList = new ArrayList<>();
		List<ParameterInformation> parameterList = new ArrayList<>();

		HashMap<String, Object> module, ports, parameters, currentPort;
		module = (HashMap<String, Object>) modules.get(moduleType);
		ports = (HashMap<String, Object>) module.get("ports");
		if (module.containsKey("parameter_default_values")) {
			logger.atInfo().setMessage("Module name {}, type {} contains parameters").addArgument(moduleName).addArgument(moduleType).log();
			parameters = (HashMap<String, Object>) module.get("parameter_default_values");
		} else {
			parameters = new HashMap<>();
		}

		// Extract port information
		for (String portName : ports.keySet()) {
			currentPort = (HashMap<String, Object>) ports.get(portName);
			List<Object> portDriverList = (ArrayList<Object>) currentPort.get("bits");

			int offset = 0;
			Range portValueRange;

			if (currentPort.containsKey("offset")) {
				offset = (int) currentPort.get("offset");
			}

			if (currentPort.containsKey("upto")) {
				portValueRange = new Range(portDriverList.size() + offset - 1, offset);
			} else {
				portValueRange = new Range(offset, portDriverList.size() + offset - 1);
			}

			PortDirection portDirection;
			if (currentPort.get("direction").equals("input")) {
				portDirection = PortDirection.IN;
			} else if (currentPort.get("direction").equals("output")) {
				portDirection = PortDirection.OUT;
			} else if (currentPort.get("direction").equals("inout")) {
				portDirection = PortDirection.INOUT;
			} else {
				portDirection = PortDirection.UNKNOWN;
			}

			portList.add(new PortInformation(portName, portValueRange, portDirection));
		}

		if (parameters != null) {
			// Extract parameter information
			for (String parameterName : parameters.keySet()) {
				parameterList.add(new ParameterInformation(parameterName, parameters.get(parameterName).toString()));
			}
		}

		ElkNode newModuleNode = ElkElementCreator.createNewHierarchyContainer(root);
		newModuleNode.setProperty(FEntwumSOptions.HIERARCHY_ANCESTOR_NAME, ancestorName);

		if (ancestor != null) {
			ElkEdge newEdge = ElkElementCreator.createNewSimpleHierarchyEdge(newModuleNode, ancestor);
			logger.info("Connected ancestor to new hierarchy container");
		}

		createNode(moduleName, moduleType, parameterList, portList, newModuleNode);

		return newModuleNode;
	}

	private void createNode(String name, String type, List<ParameterInformation> parameters, List<PortInformation> ports, ElkNode parent) {
		// Create module name node
		ElkNode moduleNameNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		moduleNameNode.setProperty(FEntwumSOptions.HIERARCHY_CONTAINER_SUB_NODE_TYPE,
								   HierarchyContainerSubNodeType.NAME);
		moduleNameNode.setProperty(CoreOptions.PARTITIONING_PARTITION, 1);

		ElkLabel moduleNameNodeLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleNameNode, name);
		ElkLabel moduleNameTitleLabel = ElkElementCreator.createNewTitleHierarchyLabel(moduleNameNode, "Name");

		// Create module type node
		ElkNode moduleTypeNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		moduleTypeNode.setProperty(FEntwumSOptions.HIERARCHY_CONTAINER_SUB_NODE_TYPE,
								   HierarchyContainerSubNodeType.TYPE);
		moduleTypeNode.setProperty(CoreOptions.PARTITIONING_PARTITION, 2);

		ElkLabel moduleTypeNodeLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleTypeNode, type);
		ElkLabel moduleTypeTitleLabel = ElkElementCreator.createNewTitleHierarchyLabel(moduleTypeNode, "Type");

		// Create node for parameters
		ElkNode moduleParameterNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		moduleParameterNode.setProperty(FEntwumSOptions.HIERARCHY_CONTAINER_SUB_NODE_TYPE,
										HierarchyContainerSubNodeType.PARAMETERS);
		moduleParameterNode.setProperty(CoreOptions.PARTITIONING_PARTITION, 3);

		ElkLabel moduleParameterTitleLabel = ElkElementCreator.createNewTitleHierarchyLabel(moduleParameterNode, "Parameters");

		if (parameters.isEmpty()) {
			ElkPort dummyPort = ElkElementCreator.createNewSimpleHierarchyPort(moduleParameterNode, 10.0d, 10.0d);
			ElkLabel dummyPortLabel = ElkElementCreator.createNewSimpleHierarchyLabel(dummyPort, " ");
		}

		// Add parameters
		for (ParameterInformation parameterInformation : parameters) {
			ElkPort moduleParameterPort = ElkElementCreator.createNewSimpleHierarchyPort(moduleParameterNode, 0.0d, 0.0d);
			ElkLabel moduleParameterPortLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleParameterPort, parameterInformation.name() + ": " + parameterInformation.value());
		}

		// Create node for ports
		ElkNode modulePortNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		modulePortNode.setProperty(FEntwumSOptions.HIERARCHY_CONTAINER_SUB_NODE_TYPE,
                                   HierarchyContainerSubNodeType.PORTS);
		modulePortNode.setProperty(CoreOptions.PARTITIONING_PARTITION, 4);

		ElkLabel modulePortTitleLabel = ElkElementCreator.createNewTitleHierarchyLabel(modulePortNode, "Ports");

		if (ports.isEmpty()) {
			ElkPort dummyPort = ElkElementCreator.createNewSimpleHierarchyPort(modulePortNode, 10.0d, 10.0d);
			ElkLabel dummyPortLabel = ElkElementCreator.createNewSimpleHierarchyLabel(dummyPort, " ");
		}

		// Add ports
		for (PortInformation portInformation : ports) {
			ElkPort modulePortPort = ElkElementCreator.createNewSimpleHierarchyPort(modulePortNode, 10.0d, 10.0d);
			ElkLabel modulePortPortLabel = ElkElementCreator.createNewSimpleHierarchyLabel(modulePortPort, portInformation.name() + (!portInformation.dimension().singleElement() ? " [" + portInformation.dimension().lower() + ":" + portInformation.dimension().upper() + "]" : ""));

			modulePortPort.setProperty(FEntwumSOptions.PORT_DIRECTION, portInformation.direction().name());
		}
	}

	private void postLayoutFixup(ElkNode root) {
		for (ElkNode container : root.getChildren()) {
			logger.info("Stretching subnodes to equal widths");
			stretchContainerSubNodes(container);

			logger.info("Centering subnode labels");
			centerContainerSubNodeLabels(container);
		}
	}

	private void stretchContainerSubNodes(ElkNode container) {
		double maxWidth = 0.0d;

		for (ElkNode subNode : container.getChildren()) {
			if (subNode.getWidth() > maxWidth) {
				maxWidth = subNode.getWidth();
			}
		}

		for (ElkNode subNode : container.getChildren()) {
			subNode.setWidth(maxWidth);
		}
	}

	private void centerContainerSubNodeLabels(ElkNode container) {
		double deltaWidth = 0.0d;
		double widestLabelWidth = 0.0d;
		double maxPortWidth = 0.0d;

		// Center name and type labels
		for (ElkNode subNode : container.getChildren()) {
			switch (subNode.getProperty(FEntwumSOptions.HIERARCHY_CONTAINER_SUB_NODE_TYPE)) {
				case NAME, TYPE:
					for (ElkLabel label : subNode.getLabels()) {
						deltaWidth = subNode.getWidth() - label.getWidth();
						label.setX(label.getX() + deltaWidth / 2.0d);
					}
					break;

				case PORTS, PARAMETERS:
					for (ElkPort port : subNode.getPorts()) {
						if (port.getLabels().getFirst().getWidth() > widestLabelWidth) {
							widestLabelWidth = port.getLabels().getFirst().getWidth();
						}

						if (port.getWidth() > maxPortWidth) {
							maxPortWidth = port.getWidth();
						}
					}

					deltaWidth = subNode.getWidth() - maxPortWidth - widestLabelWidth;

					for (ElkPort port : subNode.getPorts()) {
						port.setX(port.getX() + deltaWidth / 2.0d);
					}
					break;

				default:
					break;
			}
		}
	}
}
