package de.thkoeln.fentwums.netlist.backend.helpers;


import de.thkoeln.fentwums.netlist.backend.datatypes.BundleRange;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalElement;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is an alternative bundling approach. It only works on a single entity instance (so multiple calls are necessary)
 * and only bundles ports  on actual  cells. The ports belonging to entity instances are not affected by this bundler
 */
public class EdgeBundler {
    public static final Logger logger = LoggerFactory.getLogger(EdgeBundler.class);

    public static void bundleEdges(ElkNode entityInstance, NetlistCreationSettings settings) {
        // Go through every child cell
        for (ElkNode childNode : entityInstance.getChildren()) {
            if (childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("HDL_ENTITY")) {
                // Skip entity instances

                continue;
            }

            HashMap<String, ElkPort> currentCellPortGroupMap = new HashMap<>();
            HashMap<String, List<SignalElement>> currentCellPortIndecesMap = new HashMap<>();
            List<ElkPort> removePortList = new ArrayList<>();

            // Go through every port, bundle as necessary
            for (ElkPort port : childNode.getPorts()) {
                String portGroupName = port.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
                List<ElkEdge> moveEdgeList = new ArrayList<>();

                if (!currentCellPortGroupMap.containsKey(portGroupName)) {
                    currentCellPortGroupMap.put(portGroupName, port);
                    List<SignalElement> signalElements = new ArrayList<>();
                    signalElements.add(
                            new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null, null));
                    currentCellPortIndecesMap.put(portGroupName, signalElements);

                } else {
                    if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                        moveEdgeList = port.getOutgoingEdges().stream().toList();
                    } else {
                        moveEdgeList = port.getIncomingEdges().stream().toList();
                    }
                    currentCellPortIndecesMap.get(portGroupName).add(
                            new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null, null));

                    removePortList.add(port);
                }

                // Move edges, if necessary
                for (ElkEdge movingEdge : moveEdgeList) {
                    ElkEdge matchingEdge = null;

                    if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                        for (ElkEdge candidate : currentCellPortGroupMap.get(portGroupName).getOutgoingEdges()) {
                            if (((ElkPort) candidate.getTargets().getFirst()).getParent().equals(((ElkPort) movingEdge.getTargets().getFirst()).getParent())) {
                                matchingEdge = candidate;
                                break;
                            }
                        }
                    } else {
                        for (ElkEdge candidate : currentCellPortGroupMap.get(portGroupName).getIncomingEdges()) {
                            if (((ElkPort) candidate.getSources().getFirst()).getParent().equals(((ElkPort) movingEdge.getSources().getFirst()).getParent())) {
                                matchingEdge = candidate;
                                break;
                            }
                        }
                    }

                    if (matchingEdge != null) {
                        matchingEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
                        movingEdge.getSources().getFirst().getOutgoingEdges().remove(movingEdge);
                        movingEdge.getSources().clear();
                        movingEdge.getTargets().getFirst().getIncomingEdges().remove(movingEdge);
                        movingEdge.getTargets().clear();
                        movingEdge.getContainingNode().getContainedEdges().remove(movingEdge);
                    } else {
                        // Delete duplicate edges

                        if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                            movingEdge.getSources().getFirst().getOutgoingEdges().remove(movingEdge);
                            movingEdge.getSources().clear();
                            movingEdge.getSources().add(currentCellPortGroupMap.get(portGroupName));
                        } else {
                            movingEdge.getTargets().getFirst().getIncomingEdges().remove(movingEdge);
                            movingEdge.getTargets().clear();
                            movingEdge.getTargets().add(currentCellPortGroupMap.get(portGroupName));
                        }
                    }
                }
            }

            // Now adapt labels
            for (String portGroupName : currentCellPortGroupMap.keySet()) {
                List<BundleRange> rangeList =
                        RangeCalculator.calculateRanges(currentCellPortIndecesMap.get(portGroupName));

                if (rangeList.size() > 1) {
                    logger.atError().setMessage("Found more than one range for group {}. Skipping...").addArgument(portGroupName).log();
                    continue;
                }

                if (rangeList.isEmpty()) {
                    logger.atError().setMessage("No ranges for group {}. Skipping...").addArgument(portGroupName).log();
                    continue;
                }

                Range containedRange = rangeList.getFirst().containedRange();
                currentCellPortGroupMap.get(portGroupName).getLabels().clear();

                ElkElementCreator.createNewEdgeLabel(portGroupName + (containedRange.singleElement() ? "" :
                        " [" + containedRange.lower() + ":" + containedRange.upper() + "]"), currentCellPortGroupMap.get(portGroupName), settings);
            }

            // Now remove unused ports
            for (ElkPort port : removePortList) {
                if (port.getIncomingEdges().isEmpty() && port.getOutgoingEdges().isEmpty()) {
                    port.getParent().getPorts().remove(port);
                }
            }
        }
    }
}
