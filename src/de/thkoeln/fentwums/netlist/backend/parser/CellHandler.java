package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createLabel;
import static org.eclipse.elk.graph.util.ElkGraphUtil.createNode;

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
        for (String cellname : cells.keySet()) {
            currentCell = (HashMap<String, Object>) cells.get(cellname);
            currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");

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
        }
    }
}
