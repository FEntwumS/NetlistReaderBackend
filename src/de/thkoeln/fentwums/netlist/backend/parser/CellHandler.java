package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class CellHandler {
    public CellHandler() {}

    @SuppressWarnings("unchecked")
    public void createCells(HashMap<String, Object> cells, String modulename, ElkNode toplevel,
                            ArrayList<SignalTree> signalTreeList, HierarchyTree hierarchyTree) {
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
        ElkPort sink ,source;

        HashMap<String, ElkNode> currentConstantNodes;

        for (String cellname : cells.keySet()) {
            side = PortSide.EAST;
            currentCell = (HashMap<String, Object>) cells.get(cellname);
            currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");
            currentPortDriverIndex = 0;

            currentHierarchyPosition = hierarchyTree.getRoot();

            // get cell location in hierarchy
            // Check for hdlname attribute first, otherwise extract location path from cell name
            if (currentCellAttributes.containsKey("hdlname")) {
                currentCellPath = (String) currentCellAttributes.get("hdlname");
            } else {
                currentCellPath = cellname.replaceFirst("\\$flatten", "").replaceAll("\\\\", "").replaceAll("\\.", " ");
            }

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
                        ElkNode newElkNode = createNode(currentHierarchyPosition.getNode());
                        newElkNode.setIdentifier(pathFragement);
                        newElkNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
                        newElkNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
                        newElkNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
                                NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
                        newElkNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT,
                                EnumSet.of(PortLabelPlacement.INSIDE));
                        ElkLabel newElkNodeLabel = createLabel(pathFragement, newElkNode);
                        newElkNodeLabel.setDimensions(newElkNodeLabel.getText().length() * 7, 10);
                        HierarchicalNode newHierarchyNode = new HierarchicalNode(pathFragement,
                                currentHierarchyPosition, new HashMap<String, HierarchicalNode>(), new ArrayList<>(), new ArrayList<>(), newElkNode);

                        currentHierarchyPosition.getChildren().put(pathFragement, newHierarchyNode);

                        currentHierarchyPosition = newHierarchyNode;
                    }
                }
            }

            currentConstantNodes = currentHierarchyPosition.getConstantDrivers();

            // now that the hierarchy has been created, the actual cells can be constructed

            ElkNode newCellNode = createNode(currentHierarchyPosition.getNode());
            newCellNode.setIdentifier(currentCellPathSplit[currentCellPathSplit.length - 1]);
            newCellNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            newCellNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
            newCellNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
                    NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
            newCellNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE));

            ElkLabel newCellNodeLabel = createLabel(((String) currentCell.get("type")).replaceAll("\\$", ""),
                    newCellNode);
            newCellNodeLabel.setDimensions(newCellNodeLabel.getText().length() * 7, 10);

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

                    ElkPort cellPort = createPort(newCellNode);
                    cellPort.setProperty(CoreOptions.PORT_SIDE, side);
                    cellPort.setDimensions(10, 10);

                    ElkLabel cellPortLabel = createLabel(portname + (currentCellConnectionDrivers.size() == 1 ? "" :
                                    " [" + currentPortDriverIndex + "]"), cellPort);
                    cellPortLabel.setDimensions(cellPortLabel.getText().length() * 7, 10);


                    if(driver instanceof Integer) {
                        updateSignalTree(null, currentCellPathSplit);
                        // TODO handle later :3
                    } else {
                        // Reuse (or create, if necessary) a cell for constant drivers

                        if (currentConstantNodes.containsKey(driver + currentPortDirection)) {
                            constTarget = currentConstantNodes.get(driver + currentPortDirection);
                        } else {
                            // invert port side
                            side = side == PortSide.WEST ? PortSide.EAST : PortSide.WEST;

                            constTarget = createNode(currentHierarchyPosition.getNode());
                            constTarget.setDimensions(20, 20);
                            constTarget.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
                            constTarget.setProperty(CoreOptions.NODE_LABELS_PLACEMENT,
                                    EnumSet.of(NodeLabelPlacement.H_CENTER, NodeLabelPlacement.V_CENTER, NodeLabelPlacement.INSIDE));

                            ElkLabel constTargetLabel = createLabel((String) driver, constTarget);
                            constTargetLabel.setDimensions(constTargetLabel.getText().length() * 7, 10);

                            ElkPort constTargetPort = createPort(constTarget);
                            constTargetPort.setProperty(CoreOptions.PORT_SIDE, side);
                            constTargetPort.setDimensions(10, 10);

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

                        ElkEdge constantEdge = createSimpleEdge(source, sink);
                        ElkLabel constantLabel = createLabel((String) driver, constantEdge);
                        constantLabel.setDimensions(constantLabel.getText().length() * 7, 10);
                    }

                    currentPortDriverIndex++;
                }
            }
        }
    }
    public void updateSignalTree(SignalTree signalTree, String[] hierarchyPath) {

    }
}
