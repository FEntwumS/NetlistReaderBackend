package de.thkoeln.fentwums.netlist.backend.helpers;


import de.thkoeln.fentwums.netlist.backend.datatypes.BundleRange;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalElement;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.PortType;
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
            if (childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("HDL_ENTITY")
                || childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("Constant driver")) {
                // Skip entity instances and constant drivers/sinks

                continue;
            }

            HashMap<ElkNode, HashMap<String, ElkPort>> oppositeCellPortGroupMap = new HashMap<>();
            HashMap<ElkNode, HashMap<String, List<SignalElement>>> oppositeCellPortIndecesMap = new HashMap<>();
            HashMap<String, ElkPort> currentCellPortGroupMap;
            HashMap<String, List<SignalElement>> currentCellPortIndecesMap;
            List<ElkPort> removePortList = new ArrayList<>();
            ElkNode oppositeNode;

            if (childNode.getIdentifier().contains("neorv32_iceduino_top.v:25419$10186")) {
                logger.info("Found");
            }

            // Go through every port, bundle as necessary
            for (ElkPort port : childNode.getPorts()) {
                if (port.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT)) {
                    // skip constant ports

                    continue;
                }

                String portGroupName = port.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
                List<ElkEdge> moveEdgeList = new ArrayList<>();

                if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                    if (port.getOutgoingEdges().isEmpty()) {
                        continue;
                    }

                    if (port.getOutgoingEdges().size() > 1) {
                        // Check whether more than two sinks exist
                        ElkNode firstTarget = null;

                        for (ElkEdge edge : port.getOutgoingEdges()) {
                            if (firstTarget == null) {
                                firstTarget = ((ElkPort) edge.getTargets().getFirst()).getParent();
                            } else {
                                ElkNode currentTarget = ((ElkPort) edge.getTargets().getFirst()).getParent();

                                if (!currentTarget.equals(firstTarget)) {
                                    firstTarget = childNode;
                                    break;
                                } else {
                                    // do nothing
                                }
                            }
                        }

                        oppositeNode = firstTarget;

                    } else {
                        oppositeNode = ((ElkPort) port.getOutgoingEdges().getFirst().getTargets().getFirst()).getParent();
                    }
                } else {
                    if (port.getIncomingEdges().isEmpty()) {
                        continue;
                    }

                    oppositeNode = ((ElkPort) port.getIncomingEdges().getFirst().getSources().getFirst()).getParent();
                }

                if (oppositeCellPortGroupMap.containsKey(oppositeNode)) {
                    currentCellPortGroupMap = oppositeCellPortGroupMap.get(oppositeNode);
                    currentCellPortIndecesMap = oppositeCellPortIndecesMap.get(oppositeNode);

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
                } else {
                    currentCellPortGroupMap = new HashMap<>();
                    currentCellPortIndecesMap = new HashMap<>();

                    oppositeCellPortGroupMap.put(oppositeNode, currentCellPortGroupMap);
                    oppositeCellPortIndecesMap.put(oppositeNode, currentCellPortIndecesMap);

                    List<SignalElement> signalElements = new ArrayList<>();
                    signalElements.add(
                            new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null, null));

                    currentCellPortGroupMap.put(portGroupName, port);
                    currentCellPortIndecesMap.put(portGroupName, signalElements);
                }

                // Move edges, if necessary
                for (ElkEdge movingEdge : moveEdgeList) {
                    ElkEdge matchingEdge = null;

                    if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                        for (ElkEdge candidate : currentCellPortGroupMap.get(portGroupName).getOutgoingEdges()) {
                            if (candidate.getTargets().getFirst().equals(movingEdge.getTargets().getFirst())) {
                                matchingEdge = candidate;
                                break;
                            }
                        }
                    } else {
                        for (ElkEdge candidate : currentCellPortGroupMap.get(portGroupName).getIncomingEdges()) {
                            if (candidate.getSources().getFirst().equals(movingEdge.getSources().getFirst())) {
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
            for (ElkNode node : oppositeCellPortGroupMap.keySet()) {
                currentCellPortGroupMap = oppositeCellPortGroupMap.get(node);
                currentCellPortIndecesMap = oppositeCellPortIndecesMap.get(node);

                for (String portGroupName : currentCellPortGroupMap.keySet()) {
                    List<BundleRange> rangeList = RangeCalculator.calculateRanges(
                            currentCellPortIndecesMap.get(portGroupName));

                    if (rangeList.size() > 1) {
                        logger.atError().setMessage("Found more than one range for group {}. Skipping...").addArgument(
                                portGroupName).log();
                        continue;
                    }

                    if (rangeList.isEmpty()) {
                        logger.atError().setMessage("No ranges for group {}. Skipping...").addArgument(portGroupName)
                                .log();
                        continue;
                    }

                    Range containedRange = rangeList.getFirst().containedRange();
                    currentCellPortGroupMap.get(portGroupName).getLabels().clear();

                    ElkElementCreator.createNewPortLabel(portGroupName + (containedRange.singleElement() ?
                                                                 " [" + containedRange.lower() + "]" :
                                                                 " [" + containedRange.lower() + ":" + containedRange.upper() + "]"),
                                                         currentCellPortGroupMap.get(portGroupName), settings);
                }
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
