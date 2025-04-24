package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.options.SignalType;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.NodeLabelPlacement;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

/**
 * Handles the data contained in the "ports" section of the netlist. Creates the input and output ports of the
 * toplevel entity as well as associated constant drivers
 */
public class PortHandler {
	public PortHandler() {
	}

	/**
	 * Creates the input and output ports of the design (eg the clock input)
	 *
	 * @param ports      HashMap containing the deserialized information from the "ports" section of the netlist
	 * @param modulename Name of the top level entity
	 * @param toplevel   The graph node representing the top level entity for the layouting algorithm
	 * @return HashMap containing the bitindeces and their associated signal tree for all the created ports (except
	 * ports with constant signals)
	 */
	@SuppressWarnings("unchecked")
	public HashMap<Integer, SignalTree> createPorts(HashMap<String, Object> ports, String modulename,
													ElkNode toplevel, NetlistCreationSettings settings) {
		HashMap<Integer, SignalTree> signalMap = new HashMap<>(ports.size());
		HashMap<String, Object> currentPort;
		ArrayList<Object> currentPortDrivers;
		int currentPortDriverIndex;

		HashMap<String, ElkNode> constantNodes = new HashMap<>();
		ElkNode constTarget;
		ElkPort source, sink;

		for (String portname : ports.keySet()) {
			currentPort = (HashMap<String, Object>) ports.get(portname);
			currentPortDrivers = (ArrayList<Object>) currentPort.get("bits");

			// TODO: Update
			// Not always accurate, look at offset and MSB attributes
			currentPortDriverIndex = 0;

			for (Object driver : currentPortDrivers) {
				String portDirection = (String) currentPort.get("direction");
				PortSide side = PortSide.EAST;

				// Create port for toplevel entity

				// Input ports on the left side (WEST), output ports on the right side (EAST)
				// Buffer ports (inout) are treated as outputs
				if (portDirection.equals("input")) {
					side = PortSide.WEST;
				} else {
					portDirection = "output";
				}

				ElkPort toplevelPort = createPort(toplevel);
				toplevelPort.setProperty(CoreOptions.PORT_SIDE, side);
				toplevelPort.setDimensions(10d, 10d);
				toplevelPort.setIdentifier(modulename + driver);

				// Add label to port
				ElkLabel toplevelPortLabel =
						ElkElementCreator.createNewPortLabel(portname + (currentPortDrivers.size() == 1 ? "" :
								" [" + currentPortDriverIndex + "]"), toplevelPort, settings);

				// If the port has a constant driver (or is a constant driver), a source (or sink) node needs to be
				// created
				//
				// Constant drivers can be identified using the type of driver: Constant drivers are strings, whereas
				// "true" signals are integers

				if (driver instanceof Integer) {
					signalMap.put((int) driver, createSignalTree((int) driver, portname, modulename, toplevelPort));

					toplevelPort.setIdentifier(driver.toString());
				} else {
					if (constantNodes.containsKey(driver + portDirection)) {
						constTarget = constantNodes.get(driver + portDirection);
					} else {
						side = side == PortSide.WEST ? PortSide.EAST : PortSide.WEST;

						constTarget = ElkElementCreator.createNewConstantDriver(toplevel.getParent());
						constTarget.setDimensions(20d, 20d);
						constTarget.setProperty(CoreOptions.NODE_LABELS_PLACEMENT,
								EnumSet.of(NodeLabelPlacement.H_CENTER, NodeLabelPlacement.V_CENTER,
										NodeLabelPlacement.INSIDE));

						ElkLabel constTargetLabel = ElkElementCreator.createNewConstantDriverLabel((String) driver,
								constTarget, settings);

						ElkPort constTargetPort = ElkElementCreator.createNewPort(constTarget, side);

						constantNodes.put(driver + portDirection, constTarget);
					}

					if (portDirection.equals("input")) {
						source = constTarget.getPorts().getFirst();
						sink = toplevelPort;
					} else {
						source = toplevelPort;
						sink = constTarget.getPorts().getFirst();
					}

					ElkEdge constantEdge = ElkElementCreator.createNewEdge(sink, source);

					constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);

					ElkLabel constantLabel = ElkElementCreator.createNewEdgeLabel((String) driver, constantEdge,
							settings);
					constantLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.TAIL);
				}

				currentPortDriverIndex++;
			}
		}
		return signalMap;
	}

	/**
	 * Helper method to create the signal tree for the signal indexed by port
	 *
	 * @param port       The bitindex of the signal
	 * @param portname   The name of the port
	 * @param modulename The name of the top level entity
	 * @param sPort      The associated port
	 * @return The created signal tree
	 */
	public SignalTree createSignalTree(int port, String portname, String modulename, ElkPort sPort) {
		SignalTree tree = new SignalTree();
		tree.setSId(port);
		SignalNode rootNode = new SignalNode("root", null, new HashMap<String, SignalNode>(), null,
				new HashMap<String, SignalNode>(), false, null, null);

		tree.setHRoot(rootNode);
		rootNode.setSVisited(true);

		SignalNode toplevelNode;

		if (sPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
			toplevelNode = new SignalNode(modulename, rootNode, new HashMap<String, SignalNode>(), null,
					new HashMap<String, SignalNode>(), false, null, sPort);
		} else {
			toplevelNode = new SignalNode(modulename, rootNode, new HashMap<String, SignalNode>(), null,
					new HashMap<String, SignalNode>(), false, sPort, null);
		}

		toplevelNode.setSVisited(true);

		if (sPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.WEST) {
			toplevelNode.setIsSource(true);
			tree.setSRoot(toplevelNode);
		}

		return tree;
	}
}
