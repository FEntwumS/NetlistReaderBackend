package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.helpers.CellPathFormatter;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.options.SignalType;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class CellHandler {
	private static Logger logger = LoggerFactory.getLogger(CellHandler.class);

	public CellHandler() {
	}

	/**
	 * This method uses the data contained in the "cells"-section of the netlist to create their representations for
	 * layouting
	 *
	 * @param cells         Hashmap containing all cells inside the toplevel entity
	 * @param modulename    Name of the top level entity
	 * @param toplevel      Graph node representing the top level entity
	 * @param signalMap     Hashmap for storing all signals and the hierarchy levels where they exist
	 * @param hierarchyTree Tree for storing the hierarchy of the whole design (entities and cells)
	 * @param blackboxes    HashMap containing the port directions for blackbox cells
	 */
	@SuppressWarnings("unchecked")
	public void createCells(HashMap<String, Object> cells, String modulename, ElkNode toplevel, HashMap<Integer,
									SignalTree> signalMap, HierarchyTree hierarchyTree,
							HashMap<String, Object> blackboxes,
							NetlistCreationSettings settings) {
		HashMap<String, Object> currentCell;
		HashMap<String, Object> currentCellAttributes;
		String currentCellPath;
		String[] currentCellPathSplit;
		HierarchicalNode currentHierarchyPosition;
		String pathFragment;
		HashMap<String, Object> currentCellPortDirections;
		HashMap<String, Object> currentCellConnections;
		ArrayList<Object> currentCellConnectionDrivers;
		PortSide side;
		int currentPortDriverIndex;
		String currentPortDirection;
		SignalTree currentSignalTree;
		CellPathFormatter formatter = new CellPathFormatter();
		StringBuilder intermediateCellPath;
		HierarchicalNode newHNode;
		HashMap<Integer, String> constantValues;
		HashMap<String, ElkNode> currentConstantNodes;
		int currentDriverIndex, maxSignals;
		String addendum;
		String celltype;
		String srcLocation = "";
		int currentCellIndex = 0;

		for (String cellname : cells.keySet()) {
			if (currentCellIndex % 512 == 0) {
				logger.atInfo().setMessage("Cell {} of {}").addArgument(currentCellIndex).addArgument(cells.size() - 1).log();
			}

			currentCellIndex++;

			side = PortSide.EAST;
			currentCell = (HashMap<String, Object>) cells.get(cellname);
			currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");

			if (currentCellAttributes.containsKey("module") || currentCellAttributes.containsKey("module_src")) {
				continue;
			}

			currentCellPath = cellname;

			currentHierarchyPosition = hierarchyTree.getRoot();

			currentCellPath = formatter.format(currentCellPath);

			currentCellPathSplit = currentCellPath.split(" ");

			// find parent node in hierarchy
			// create it, if it doesn't yet exist
			// if the split cell path only contains one element, the toplevel is the parent node
			if (currentCellPathSplit.length > 1) {
				for (int i = 0; i < currentCellPathSplit.length - 1; i++) {
					pathFragment = currentCellPathSplit[i];

					if (currentHierarchyPosition.getChildren().containsKey(pathFragment)) {
						currentHierarchyPosition = currentHierarchyPosition.getChildren().get(pathFragment);
					} else {
						intermediateCellPath = new StringBuilder();

						intermediateCellPath.append(currentCellPathSplit[0]);

						for (int j = 1; j <= i; j++) {
							intermediateCellPath.append(" ").append(currentCellPathSplit[j]);
						}

						ElkNode newElkNode = ElkElementCreator.createNewNode(currentHierarchyPosition.getNode(),
								intermediateCellPath.toString());

						newElkNode.setProperty(FEntwumSOptions.LOCATION_PATH, intermediateCellPath.toString());
						newElkNode.setProperty(FEntwumSOptions.CELL_TYPE, "HDL_ENTITY");

						ElkLabel newElkNodeLabel = ElkElementCreator.createNewEntityLabel(pathFragment, newElkNode,
								settings);

						HierarchicalNode newHierarchyNode = new HierarchicalNode(pathFragment,
								currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<>(),
								new HashMap<>(), newElkNode);

						currentHierarchyPosition.getChildren().put(pathFragment, newHierarchyNode);

						currentHierarchyPosition = newHierarchyNode;
					}
				}
			}

			currentConstantNodes = currentHierarchyPosition.getConstantDrivers();

			celltype = ((String) currentCell.get("type")).replaceAll("\\$", "");

			if (currentCellAttributes.containsKey("src")) {
				srcLocation = currentCellAttributes.get("src").toString();
			}

			// now that the hierarchy has been created, the actual cells can be constructed

			ElkNode newCellNode = ElkElementCreator.createNewNode(currentHierarchyPosition.getNode(), currentCellPath);

			newCellNode.setProperty(FEntwumSOptions.CELL_NAME, currentCellPathSplit[currentCellPathSplit.length - 1]);
			newCellNode.setProperty(FEntwumSOptions.CELL_TYPE, celltype);
			newCellNode.setProperty(FEntwumSOptions.LOCATION_PATH, currentCellPath);
			newCellNode.setProperty(FEntwumSOptions.SRC_LOCATION, srcLocation);

			ElkLabel newCellNodeLabel = ElkElementCreator.createNewCellLabel(celltype, newCellNode, settings);

			// update hierarchy to include the new node
			newHNode = new HierarchicalNode(currentCellPathSplit[currentCellPathSplit.length - 1],
					currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<Vector>(),
					new HashMap<Integer, Bundle>(), newCellNode);

			// Create node ports

			currentCellPortDirections = (HashMap<String, Object>) currentCell.get("port_directions");
			currentCellConnections = (HashMap<String, Object>) currentCell.get("connections");

			// Check for blackbox cell; We currently cannot handle those
			if (currentCellAttributes.containsKey("module_not_derived")) {

				if (currentCellPortDirections != null) {
					logger.atInfo().setMessage("Cell {} is a blackbox. Using yosys description to add input/output " +
							"information").addArgument(cellname).log();
				} else {
					if (blackboxes.containsKey(celltype)) {
						logger.atInfo().setMessage("Using description from external file for cell {}").addArgument(cellname).log();
						currentCellPortDirections = (HashMap<String, Object>) blackboxes.get(celltype);
					} else {
						logger.atError().setMessage("Cell {} is a blackbox cell. Aborting...").addArgument(cellname).log();
						throw new RuntimeException("Cell " + cellname + " is a blackbox cell. Aborting...");
					}
				}
			}

			// get max number of signals
			maxSignals = 0;

			for (String portname : currentCellPortDirections.keySet()) {
				if (((ArrayList<Object>) currentCellConnections.get(portname)).size() > maxSignals) {
					maxSignals = ((ArrayList<Object>) currentCellConnections.get(portname)).size();
				}
			}

			currentDriverIndex = 0;
			for (String portname : currentCellPortDirections.keySet()) {
				currentPortDriverIndex = 0;
				constantValues = new HashMap<Integer, String>();

				if (currentCellConnections.keySet().size() != currentCellPortDirections.keySet().size() || !currentCellConnections.containsKey(portname)) {
					throw new RuntimeException("Mismatch between number of ports in port_directions and connections");
				}

				currentPortDirection = (String) currentCellPortDirections.get(portname);

				if (currentPortDirection.equals("input")) {
					side = PortSide.WEST;
				} else {
					currentPortDirection = "output";
					side = PortSide.EAST;
				}

				currentCellConnectionDrivers = (ArrayList<Object>) currentCellConnections.get(portname);

				for (Object driver : currentCellConnectionDrivers) {
					if (driver instanceof Integer) {
						ElkPort cellPort = ElkElementCreator.createNewPort(newCellNode, side);
						cellPort.setProperty(CoreOptions.PORT_INDEX,
								currentDriverIndex * maxSignals + currentPortDriverIndex);
						cellPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);
						cellPort.setProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP, currentPortDriverIndex);

						ElkLabel cellPortLabel =
								ElkElementCreator.createNewPortLabel(portname + (currentCellConnectionDrivers.size() == 1 ? "" : " [" + currentPortDriverIndex + "]"), cellPort, settings);

						cellPort.setIdentifier(currentCellPathSplit[currentCellPathSplit.length - 1] + (int) driver);

						if (signalMap.containsKey((int) driver)) {
							currentSignalTree = signalMap.get((int) driver);
						} else {
							currentSignalTree = new SignalTree();
							currentSignalTree.setSId((int) driver);

							SignalNode rootNode = new SignalNode("root", null, new HashMap<String, SignalNode>(), null
									, new HashMap<String, SignalNode>(), false, null, null);

							currentSignalTree.setHRoot(rootNode);

							SignalNode toplevelNode = new SignalNode(modulename, rootNode, new HashMap<String,
									SignalNode>(), null, new HashMap<String, SignalNode>(), false, null, null);

							signalMap.put((int) driver, currentSignalTree);
						}


						updateSignalTree(currentSignalTree, currentCellPathSplit, modulename,
								cellPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST, cellPort, portname,
								currentPortDriverIndex);
					} else {
						constantValues.put(currentPortDriverIndex, (String) driver);
					}

					currentPortDriverIndex++;
				}

				if (!constantValues.isEmpty()) {
					createConstantSignals(newCellNode, currentConstantNodes, constantValues, side, portname,
							currentDriverIndex, maxSignals, settings);
				}

				currentDriverIndex++;
			}
		}
	}

	/**
	 * Adds an occurance of a given signal to the relevant signal tree
	 *
	 * @param signalTree    The signal tree of the signal
	 * @param hierarchyPath Path where the occurance is located
	 * @param modulename    Name of the top level entity
	 * @param isSource      True if the occurance is the source of the signal
	 * @param cellPort      The representation of the port where the signal occured
	 * @param portname      The name of the port
	 * @param index         The signals index inside the containing vector (-1 if the signal is not contained in a
	 *                      vector)
	 */
	public void updateSignalTree(SignalTree signalTree, String[] hierarchyPath, String modulename, boolean isSource,
								 ElkPort cellPort, String portname, int index) {
		SignalNode currentNode = signalTree.getHRoot().getHChildren().get(modulename);

		for (String fragment : hierarchyPath) {
			if (currentNode.getHChildren().containsKey(fragment)) {
				currentNode = currentNode.getHChildren().get(fragment);
			} else {
				currentNode = insertMissingSNode(currentNode, fragment, null, null);
			}
		}

		currentNode.setSName(portname);
		currentNode.setIndexInSignal(index);

		currentNode.setSVisited(true);
		currentNode.setIsSource(isSource);

		/*
		// Dont remove existing ports
		if (currentNode.getInPort() != null) {
			currentNode.setInPort(inPort);
		}

		if (currentNode.getOutPort() != null) {
			currentNode.setOutPort(outPort);
		}
		*/

		if (isSource) {
			signalTree.setSRoot(currentNode);
			currentNode.setOutPort(cellPort);
		} else {
			currentNode.setInPort(cellPort);
		}
	}

	private SignalNode insertMissingSNode(SignalNode parent, String nodename, ElkPort inPort, ElkPort outPort) {
		return new SignalNode(nodename, parent, new HashMap(), null, new HashMap(), false, inPort, outPort);
	}

	/**
	 * Creates constant drivers for a port group
	 *
	 * @param parent         The containing graph node
	 * @param constNodes     HashMap containing a subset of existing constant drivers
	 * @param constantValues Hashmap containing the indices and values of the constant signals of the relevant cell
	 * @param side           The side of the port at the cell (west for input, east for output)
	 * @param portname       Name of the port
	 * @param driverIndex    Index of the port group for which the constant drivers are to be created
	 * @param maxSignalIndex Index of the signal with the highest index in the port group
	 */
	private void createConstantSignals(ElkNode parent, HashMap<String, ElkNode> constNodes,
									   HashMap<Integer, String> constantValues, PortSide side, String portname,
									   int driverIndex, int maxSignalIndex, NetlistCreationSettings settings) {
		int cRangeStart = -2, cRangeEnd = cRangeStart;
		StringBuilder constantValueBuilder = new StringBuilder();
		StringBuilder constantLabelBuilder = new StringBuilder();
		ElkPort newPort = null;
		ElkEdge constantEdge = null;
		ElkNode constantNode;
		ElkLabel constantNodeLabel = null;
		ElkPort constantNodePort;
		boolean firstRange = true;
		int lastKey = 0;

		// Group constant drivers
		for (int key : constantValues.keySet()) {
			if (key - cRangeEnd > 1) {
				if (!firstRange) {
					if (cRangeEnd - 1 != cRangeStart) {
						constantLabelBuilder.append(":").append(cRangeEnd - 1).append("]");

						// create driver
						constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

						constantNodeLabel =
								ElkElementCreator.createNewConstantDriverLabel(constantValueBuilder.toString(),
										constantNode, settings);

						constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ?
								PortSide.EAST : PortSide.WEST);

						// create edge
						constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
						constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED_CONSTANT);
					} else {
						constantLabelBuilder.append("]");

						// get driver, create if it does not exist yet
						constantNode = constNodes.get(constantValues.get(key));

						if (constantNode == null) {
							constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

							constantNodeLabel =
									ElkElementCreator.createNewConstantDriverLabel((String) constantValues.get(key),
											constantNode, settings);

							constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ?
									PortSide.EAST : PortSide.WEST);

							constNodes.put(constantValues.get(key), constantNode);
						} else {
							constantNodePort = constantNode.getPorts().getFirst();
						}

						// create edge
						constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
						constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);
					}

					ElkElementCreator.createNewPortLabel(constantLabelBuilder.toString(), newPort, settings);
					ElkLabel constantEdgeLabel = ElkElementCreator.createNewEdgeLabel(constantValueBuilder.toString(),
							constantEdge, settings);
					constantEdgeLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.HEAD);
				}

				constantLabelBuilder = new StringBuilder(portname).append(" [").append(key);
				constantValueBuilder = new StringBuilder();
				firstRange = false;

				// Skip, therefore a new range starts
				// create port for new range
				newPort = ElkElementCreator.createNewPort(parent, side);
				newPort.setProperty(CoreOptions.PORT_INDEX, driverIndex * maxSignalIndex + key);
				cRangeStart = key;
				cRangeEnd = key;
			}

			constantValueBuilder.insert(0, constantValues.get(key));

			cRangeEnd++;
			lastKey = key;
		}

		if (cRangeEnd - 1 != cRangeStart) {
			constantLabelBuilder.append(":").append(cRangeEnd - 1).append("]");

			// create driver
			constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

			constantNodeLabel = ElkElementCreator.createNewConstantDriverLabel(constantValueBuilder.toString(),
					constantNode, settings);

			constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ? PortSide.EAST :
					PortSide.WEST);

			// create edge
			constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
			constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED_CONSTANT);
		} else {
			constantLabelBuilder.append("]");

			// get driver, create if it does not exist yet
			constantNode = constNodes.get(constantValues.get(lastKey));

			if (constantNode == null) {
				constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

				constantNodeLabel =
						ElkElementCreator.createNewConstantDriverLabel((String) constantValues.get(lastKey),
								constantNode, settings);

				constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ?
						PortSide.EAST : PortSide.WEST);

				constNodes.put(constantValues.get(lastKey), constantNode);
			} else {
				constantNodePort = constantNode.getPorts().getFirst();
			}

			// create edge
			constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
			constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);
		}

		if (constantValues.size() == 1) {
			ElkElementCreator.createNewPortLabel(portname, newPort, settings);
		} else {
			ElkElementCreator.createNewPortLabel(constantLabelBuilder.toString(), newPort, settings);
		}
		ElkLabel constantEdgeLabel;

		constantEdgeLabel = ElkElementCreator.createNewEdgeLabel(constantValueBuilder.toString(), constantEdge,
				settings);
		constantEdgeLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.HEAD);
	}
}
