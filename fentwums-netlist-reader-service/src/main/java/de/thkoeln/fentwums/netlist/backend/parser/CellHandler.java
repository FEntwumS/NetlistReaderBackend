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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class CellHandler {
    private static Logger logger = LoggerFactory.getLogger(CellHandler.class);

    public CellHandler() {
    }

    @SuppressWarnings("unchecked")
    public void createCells(HashMap<String, Object> cells, String modulename, ElkNode toplevel, HashMap<Integer, SignalTree> signalMap, HierarchyTree hierarchyTree) {
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
        ElkNode constTarget;
        ElkPort sink, source;
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

            if (cellname.startsWith("$flatten")) {
                currentCellPathSplit = cellname.split("\\$");
                addendum = " " + currentCellPathSplit[currentCellPathSplit.length - 1];
            } else {
                addendum = "";
            }

            if (currentCellAttributes.containsKey("hdlname")) {
                currentCellPath = (String) currentCellAttributes.get("hdlname") + addendum;
            } else if (currentCellAttributes.containsKey("scopename")) {
                currentCellPath = (String) currentCellAttributes.get("scopename") + addendum;
            } else {
                currentCellPath = cellname;
                //throw new RuntimeException("Cell contains neither hdlname nor scopename attribute. Aborting!");
            }

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

                        ElkNode newElkNode = ElkElementCreator.createNewNode(currentHierarchyPosition.getNode(), intermediateCellPath.toString());

                        newElkNode.setProperty(FEntwumSOptions.LOCATION_PATH, intermediateCellPath.toString());

                        ElkLabel newElkNodeLabel = ElkElementCreator.createNewLabel(pathFragment, newElkNode);

                        HierarchicalNode newHierarchyNode = new HierarchicalNode(pathFragment, currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<>(), new HashMap<>(), newElkNode);

                        currentHierarchyPosition.getChildren().put(pathFragment, newHierarchyNode);

                        currentHierarchyPosition = newHierarchyNode;
                    }
                }
            }

            currentConstantNodes = currentHierarchyPosition.getConstantDrivers();

            celltype = ((String) currentCell.get("type")).replaceAll("\\$", "");

            if(currentCellAttributes.containsKey("src")) {
                srcLocation = (String) currentCellAttributes.get("src");
            }

            // now that the hierarchy has been created, the actual cells can be constructed

            ElkNode newCellNode = ElkElementCreator.createNewNode(currentHierarchyPosition.getNode(), currentCellPath);

            newCellNode.setProperty(FEntwumSOptions.CELL_NAME, currentCellPathSplit[currentCellPathSplit.length - 1]);
            newCellNode.setProperty(FEntwumSOptions.CELL_TYPE, celltype);
            newCellNode.setProperty(FEntwumSOptions.LOCATION_PATH, currentCellPath);
            newCellNode.setProperty(FEntwumSOptions.SRC_LOCATION, srcLocation);

            ElkLabel newCellNodeLabel = ElkElementCreator.createNewLabel(celltype, newCellNode);

            // update hierarchy to include the new node
            newHNode = new HierarchicalNode(currentCellPathSplit[currentCellPathSplit.length - 1], currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<Vector>(), new HashMap<Integer, Bundle>(), newCellNode);

            // Create node ports

            currentCellPortDirections = (HashMap<String, Object>) currentCell.get("port_directions");
            currentCellConnections = (HashMap<String, Object>) currentCell.get("connections");

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
                        cellPort.setProperty(CoreOptions.PORT_INDEX, currentDriverIndex * maxSignals + currentPortDriverIndex);
                        cellPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);

                        ElkLabel cellPortLabel = ElkElementCreator.createNewLabel(portname + (currentCellConnectionDrivers.size() == 1 ? "" : " [" + currentPortDriverIndex + "]"), cellPort);

                        cellPort.setIdentifier(currentCellPathSplit[currentCellPathSplit.length - 1] + (int) driver);

                        if (signalMap.containsKey((int) driver)) {
                            currentSignalTree = signalMap.get((int) driver);
                        } else {
                            currentSignalTree = new SignalTree();
                            currentSignalTree.setSId((int) driver);

                            SignalNode rootNode = new SignalNode("root", null, new HashMap<String, SignalNode>(), null, new HashMap<String, SignalNode>(), new HashMap<String, SignalNode>(), false, null);

                            currentSignalTree.setHRoot(rootNode);

                            SignalNode toplevelNode = new SignalNode(modulename, rootNode, new HashMap<String, SignalNode>(), null, new HashMap<String, SignalNode>(), new HashMap<String, SignalNode>(), false, null);

                            signalMap.put((int) driver, currentSignalTree);
                        }
                        updateSignalTree(currentSignalTree, currentCellPathSplit, modulename, cellPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST, cellPort, portname, currentPortDriverIndex);
                    } else {
                        constantValues.put(currentPortDriverIndex, (String) driver);
                    }

                    currentPortDriverIndex++;
                }

                if (!constantValues.keySet().isEmpty()) {
                    createConstantSignals(newCellNode, currentConstantNodes, constantValues, side, portname, currentDriverIndex, maxSignals);
                }

                currentDriverIndex++;
            }
        }
    }

    public void updateSignalTree(SignalTree signalTree, String[] hierarchyPath, String modulename, boolean isSource, ElkPort sPort, String portname, int index) {
        SignalNode currentNode = signalTree.getHRoot().getHChildren().get(modulename);

        for (String fragment : hierarchyPath) {
            if (currentNode.getHChildren().containsKey(fragment)) {
                currentNode = currentNode.getHChildren().get(fragment);
            } else {
                currentNode = insertMissingSNode(currentNode, fragment, null);
            }
        }

        currentNode.setSName(portname);
        currentNode.setIndexInSignal(index);

        currentNode.setSVisited(true);
        currentNode.setIsSource(isSource);
        currentNode.setSPort(sPort);

        if (isSource) {
            signalTree.setSRoot(currentNode);
        }
    }

    private SignalNode insertMissingSNode(SignalNode parent, String nodename, ElkPort sPort) {
        return new SignalNode(nodename, parent, new HashMap(), null, new HashMap(), new HashMap(), false, sPort);
    }

    private void createConstantSignals(ElkNode parent, HashMap<String, ElkNode> constNodes, HashMap<Integer, String> constantValues, PortSide side, String portname, int driverIndex, int maxSignalIndex) {
        int cRangeStart = -2, cRangeEnd = cRangeStart;
        StringBuilder constantValueBuilder = new StringBuilder();
        StringBuilder constantLabelBuilder = new StringBuilder();
        ElkPort newPort = null;
        ElkEdge constantEdge = null;
        ElkNode constantNode;
        ElkLabel constantNodeLabel = null;
        ElkPort constantNodePort;
        boolean firstRange = true;
        boolean needEdge;
        int lastKey = 0;

        // Group constant drivers
        for (int key : constantValues.keySet()) {
            if (key - cRangeEnd > 1) {
                if (!firstRange) {
                    if (cRangeEnd - 1 != cRangeStart) {
                        constantLabelBuilder.append(":").append(cRangeEnd - 1).append("]");

                        // create driver
                        constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

                        constantNodeLabel = ElkElementCreator.createNewLabel(constantValueBuilder.toString(), constantNode);

                        constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ? PortSide.EAST : PortSide.WEST);

                        // create edge
                        constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
                        constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED_CONSTANT);
                    } else {
                        constantLabelBuilder.append("]");

                        // get driver, create if it does not exist yet
                        constantNode = constNodes.get(constantValues.get(key));

                        if (constantNode == null) {
                            constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

                            constantNodeLabel = ElkElementCreator.createNewLabel((String) constantValues.get(key), constantNode);

                            constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ? PortSide.EAST : PortSide.WEST);

                            constNodes.put(constantValues.get(key), constantNode);
                        } else {
                            constantNodePort = constantNode.getPorts().getFirst();
                        }

                        // create edge
                        constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
                        constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);
                    }

                    ElkElementCreator.createNewLabel(constantLabelBuilder.toString(), newPort);
                    ElkLabel constantEdgeLabel = ElkElementCreator.createNewLabel(constantValueBuilder.toString(), constantEdge);
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

            constantNodeLabel = ElkElementCreator.createNewLabel(constantValueBuilder.toString(), constantNode);

            constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ? PortSide.EAST : PortSide.WEST);

            // create edge
            constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
            constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED_CONSTANT);
        } else {
            constantLabelBuilder.append("]");

            // get driver, create if it does not exist yet
            constantNode = constNodes.get(constantValues.get(lastKey));

            if (constantNode == null) {
                constantNode = ElkElementCreator.createNewConstantDriver(parent.getParent());

                constantNodeLabel = ElkElementCreator.createNewLabel((String) constantValues.get(lastKey), constantNode);

                constantNodePort = ElkElementCreator.createNewPort(constantNode, side == PortSide.WEST ? PortSide.EAST : PortSide.WEST);

                constNodes.put(constantValues.get(lastKey), constantNode);
            } else {
                constantNodePort = constantNode.getPorts().getFirst();
            }

            // create edge
            constantEdge = ElkElementCreator.createNewEdge(newPort, constantNodePort);
            constantEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);
        }

        if (constantValues.keySet().size() == 1) {
            ElkElementCreator.createNewLabel(portname, newPort);
        } else {
            ElkElementCreator.createNewLabel(constantLabelBuilder.toString(), newPort);
        }
        ElkLabel constantEdgeLabel;

        constantEdgeLabel = ElkElementCreator.createNewLabel(constantValueBuilder.toString(), constantEdge);
        constantEdgeLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.HEAD);
    }
}
