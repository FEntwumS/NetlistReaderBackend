package de.thkoeln.fentwums.netlist.backend.hierarchy.view;

import de.thkoeln.fentwums.netlist.backend.datatypes.ParameterInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.PortDirection;
import de.thkoeln.fentwums.netlist.backend.datatypes.PortInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createGraph;

public class HierarchyExtractor {
	private final static Logger logger = LoggerFactory.getLogger(HierarchyExtractor.class);

	public String extractHierarchy(HashMap<String, Object> netlist) {
		ElkNode root = createGraph();
		root.setIdentifier("root");

		String topName = "", topType = "";

		HashMap<String, Object> modules = (HashMap<String, Object>) netlist.get("modules");

		for (String module : modules.keySet()) {
			HashMap<String, Object> currentModule = (HashMap<String, Object>) modules.get(module);

			HashMap<String, Object> currentModuleAttributes = (HashMap<String, Object>) currentModule.get("attributes");

			if (currentModuleAttributes != null) {
				if (currentModuleAttributes.containsKey("top")) {
					topName = module;

					topType = (String) currentModule.get("type");
					break;
				}
			}
		}

		if (topName.equals("")) {
			logger.error("No top level module found. Aborting...");
			return null;
		}

		extractModuleRecursively(netlist, topType, root);

		// Layout graph

		RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
		BasicProgressMonitor monitor = new BasicProgressMonitor();

		try {
			engine.layout(root, monitor);
		} catch (Exception e) {
			logger.error("Error during layout", e);
			return null;
		}

		return ElkGraphJson.forGraph(root).omitLayout(false).omitZeroDimension(true)
				.omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();
	}

	private void extractModuleRecursively(HashMap<String, Object> netlist, String moduleType, ElkNode root) {
		if (!netlist.containsKey(moduleType)) {
			logger.atError().setMessage("Could not find module type {} in netlist").addArgument(moduleType).log();
			return;
		}

		HashMap<String, Object> module, cells, currentCell, currentCellAttributes;
		boolean isHidden = true, isDerived = true;
		String cellType;

		module = (HashMap<String, Object>) netlist.get(moduleType);
		cells = (HashMap<String, Object>) module.get("cells");

		for(String cellName : cells.keySet()) {
			currentCell = (HashMap<String, Object>) cells.get(cellName);
			currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");
			cellType = (String) currentCell.get("type");

			isHidden = currentCell.get("hide_name").equals("0");
			isDerived = !(currentCellAttributes.containsKey("module_not_derived") || cellType.contains("paramod"));

			if (!isHidden && !isDerived) {
				addModuleToHierarchy(netlist, cellType, cellName, root);

				extractModuleRecursively(netlist, cellType, root);
			}
		}
	}

	private void addModuleToHierarchy(HashMap<String, Object> netlist, String moduleType, String moduleName, ElkNode root) {
		if (!netlist.containsKey(moduleType)) {
			logger.atError().setMessage("Could not find module name {},  type {} in netlist").addArgument(moduleName).addArgument(moduleType).log();
			return;
		}

		List<PortInformation> portList = new ArrayList<>();
		List<ParameterInformation> parameterList = new ArrayList<>();

		HashMap<String, Object> module, ports, parameters, currentPort;
		module = (HashMap<String, Object>) netlist.get(moduleType);
		ports = (HashMap<String, Object>) module.get("ports");
		if (module.containsKey("parameter_default_values")) {
			logger.atInfo().setMessage("Module name {}, type {} contains parameters").addArgument(moduleName).addArgument(moduleType).log();
			parameters = (HashMap<String, Object>) module.get("parameters");
		} else {
			parameters = new HashMap<>();
		}

		// Extract port information
		for (String portName : ports.keySet()) {
			currentPort = (HashMap<String, Object>) ports.get(portName);
			List<Object> portDriverList = (ArrayList<Object>) currentPort.get("ports");

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

		// Extract parameter information
		for (String parameterName : parameters.keySet()) {
			parameterList.add(new ParameterInformation(parameterName, (String) parameters.get(parameterName)));
		}

		ElkNode newModuleNode = ElkElementCreator.createNewHierarchyContainer(root);

		createNode(moduleName, moduleType, parameterList, portList, newModuleNode);
	}

	private void createNode(String name, String type, List<ParameterInformation> parameters, List<PortInformation> ports, ElkNode parent) {
		// Create module name node
		ElkNode moduleNameNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		ElkLabel moduleNameNodeLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleNameNode, name);

		// Create module type node
		ElkNode moduleTypeNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		ElkLabel moduleTypeNodeLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleTypeNode, type);

		// Create node for parameters
		ElkNode moduleParameterNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		ElkLabel moduleParameterNodeLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleParameterNode, "Parameters");

		// Add parameters
		for (ParameterInformation parameterInformation : parameters) {
			ElkPort moduleParameterPort = ElkElementCreator.createNewSimpleHierarchyPort(moduleParameterNode, 0.0d, 0.0d);
			ElkLabel moduleParameterPortLabel = ElkElementCreator.createNewSimpleHierarchyLabel(moduleParameterPort, parameterInformation.name() + ": " + parameterInformation.value());
		}

		// Create node for ports
		ElkNode modulePortNode = ElkElementCreator.createNewSimpleHierarchyNode(parent);
		ElkLabel modulePortNodeLabel = ElkElementCreator.createNewSimpleHierarchyLabel(modulePortNode, "Ports");

		// Add ports
		for (PortInformation portInformation : ports) {
			ElkPort modulePortPort = ElkElementCreator.createNewSimpleHierarchyPort(modulePortNode, 10.0d, 10.0d);
			ElkLabel modulePortPortLabel = ElkElementCreator.createNewSimpleHierarchyLabel(modulePortPort, portInformation.name() + (portInformation.dimension().singleElement() ? " [" + portInformation.dimension().lower() + ":" + portInformation.dimension().upper() + "]" : ""));

			modulePortPort.setProperty(FEntwumSOptions.PORT_DIRECTION, portInformation.direction().name());
		}
	}
}
