package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.helpers.CellPathFormatter;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class CellHandler {
    public CellHandler() {}

    @SuppressWarnings("unchecked")
    public void createCells(HashMap<String, Object> cells, String modulename, ElkNode toplevel,
                            HashMap<Integer, SignalTree> signalMap, HierarchyTree hierarchyTree) {
        HashMap<String, Object> currentCell;
        HashMap<String, Object> currentCellAttributes;
        String currentCellPath;
        String[] currentCellPathSplit;
        HierarchicalNode currentHierarchyPosition;
        String pathFragement;
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
        ElkElementCreator creator = new ElkElementCreator();

        HashMap<String, ElkNode> currentConstantNodes;

        for (String cellname : cells.keySet()) {
            side = PortSide.EAST;
            currentCell = (HashMap<String, Object>) cells.get(cellname);
            currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");

            currentHierarchyPosition = hierarchyTree.getRoot();

            // TODO check for other possible split syntaxes
            // get cell location in hierarchy
            // Check for hdlname attribute first, otherwise extract location path from cell name
//            if (currentCellAttributes.containsKey("hdlname")) {
//                currentCellPath = (String) currentCellAttributes.get("hdlname");
//            } else {
//                currentCellPath = formatter.format(cellname);
//            }

            currentCellPath = formatter.format(cellname);

            currentCellPathSplit = currentCellPath.split(" ");

            // find parent node in hierarchy
            // create it, if it doesn't yet exist
            // if the split cell path only contains one element, the toplevel is the parent node
            if(currentCellPathSplit.length > 1) {
                for (int i = 0; i < currentCellPathSplit.length - 1; i++) {
                    pathFragement = currentCellPathSplit[i];

                    if (currentHierarchyPosition.getChildren().containsKey(pathFragement)) {
                        currentHierarchyPosition = currentHierarchyPosition.getChildren().get(pathFragement);
                    } else {
                        intermediateCellPath = new StringBuilder();

                        intermediateCellPath.append(currentCellPathSplit[0]);

                        for (int j = 1; j <= i; j++) {
                            intermediateCellPath.append(" ").append(currentCellPathSplit[j]);
                        }

                        ElkNode newElkNode = creator.createNewNode(currentHierarchyPosition.getNode(),
                                intermediateCellPath.toString());

                        ElkLabel newElkNodeLabel = creator.createNewLabel(pathFragement, newElkNode);

                        HierarchicalNode newHierarchyNode = new HierarchicalNode(pathFragement,
                                currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<>(),
                                new HashMap<>(), newElkNode);

                        currentHierarchyPosition.getChildren().put(pathFragement, newHierarchyNode);

                        currentHierarchyPosition = newHierarchyNode;
                    }
                }
            }

            currentConstantNodes = currentHierarchyPosition.getConstantDrivers();

            // now that the hierarchy has been created, the actual cells can be constructed

            ElkNode newCellNode = creator.createNewNode(currentHierarchyPosition.getNode(), currentCellPath);

            ElkLabel newCellNodeLabel = creator.createNewLabel(((String) currentCell.get("type")).replaceAll("\\$",
                    ""), newCellNode);

            // update hierarchy to include the new node
            newHNode = new HierarchicalNode(currentCellPathSplit[currentCellPathSplit.length - 1],
                    currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<Vector>(),
                    new HashMap<Integer, Bundle>(), newCellNode);

            // Create node ports

            currentCellPortDirections = (HashMap<String, Object>) currentCell.get("port_directions");
            currentCellConnections = (HashMap<String, Object>) currentCell.get("connections");

            for (String portname: currentCellPortDirections.keySet()) {
                currentPortDriverIndex = 0;

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

                for(Object driver: currentCellConnectionDrivers) {

                    ElkPort cellPort = creator.createNewPort(newCellNode, side);

                    ElkLabel cellPortLabel =
                            creator.createNewLabel(portname + (currentCellConnectionDrivers.size() == 1 ? "" :
                                    " [" + currentPortDriverIndex + "]"), cellPort);


                    if(driver instanceof Integer) {
                        cellPort.setIdentifier(currentCellPathSplit[currentCellPathSplit.length - 1] + (int) driver);

                        if (signalMap.containsKey((int) driver)) {
                            currentSignalTree = signalMap.get((int) driver);
                        } else {
                            currentSignalTree = new SignalTree();
                            currentSignalTree.setSId((int) driver);

                            SignalNode rootNode = new SignalNode("root", null, new HashMap<String, SignalNode>(), null,
                                    new HashMap<String, SignalNode>(), new HashMap<String, SignalNode>(), false, null);

                            currentSignalTree.setHRoot(rootNode);

                            SignalNode toplevelNode = new SignalNode(modulename, rootNode, new HashMap<String, SignalNode>(), null,
                                    new HashMap<String, SignalNode>(), new HashMap<String, SignalNode>(), false, null);

                            signalMap.put((int) driver, currentSignalTree);
                        }
                        updateSignalTree(currentSignalTree, currentCellPathSplit, modulename,
                                cellPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST,
                                cellPort, portname, currentPortDriverIndex);
                    } else {
                        // Reuse (or create, if necessary) a cell for constant drivers

                        if (currentConstantNodes.containsKey(driver + currentPortDirection)) {
                            constTarget = currentConstantNodes.get(driver + currentPortDirection);
                        } else {
                            // invert port side
                            side = side == PortSide.WEST ? PortSide.EAST : PortSide.WEST;

                            constTarget = creator.createNewConstantDriver(currentHierarchyPosition.getNode());

                            ElkLabel constTargetLabel = creator.createNewLabel((String) driver, constTarget);

                            ElkPort constTargetPort = creator.createNewPort(constTarget, side);

                            currentConstantNodes.put(driver + currentPortDirection, constTarget);

                            // reinvert port side
                            side = side == PortSide.WEST ? PortSide.EAST : PortSide.WEST;
                        }

                        if (currentPortDirection.equals("input")) {
                            source = constTarget.getPorts().getFirst();
                            sink = cellPort;
                        } else {
                            source = cellPort;
                            sink = constTarget.getPorts().getFirst();
                        }

                        ElkEdge constantEdge = creator.createNewEdge(source, sink);

                        ElkLabel constantLabel = creator.createNewLabel((String) driver, constantEdge);
                    }

                    currentPortDriverIndex++;
                }
            }
        }
    }

    public void updateSignalTree(SignalTree signalTree, String[] hierarchyPath, String modulename, boolean isSource,
                                 ElkPort sPort, String portname, int index) {
        SignalNode currentNode = signalTree.getHRoot().getHChildren().get(modulename);

        for (String fragment: hierarchyPath) {
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
}
