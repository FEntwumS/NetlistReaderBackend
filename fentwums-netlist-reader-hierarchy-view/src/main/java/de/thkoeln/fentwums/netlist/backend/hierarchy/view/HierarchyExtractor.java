package de.thkoeln.fentwums.netlist.backend.hierarchy.view;

import de.thkoeln.fentwums.netlist.backend.datatypes.ParameterInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.PortDirection;
import de.thkoeln.fentwums.netlist.backend.datatypes.PortInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.graph.ElkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HierarchyExtractor {
	private final static Logger logger = LoggerFactory.getLogger(HierarchyExtractor.class);

	public String extractHierarchy(HashMap<String, Object> netlist) {

		return "";
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

	}
}
