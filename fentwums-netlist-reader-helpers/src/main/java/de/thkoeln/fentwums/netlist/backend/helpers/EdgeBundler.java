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
import org.eclipse.elk.graph.ElkConnectableShape;
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
		for (ElkNode childNode : List.of(entityInstance.getChildren().toArray(new ElkNode[0]))) {
			if (childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("HDL_ENTITY")
					|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("Constant driver")
					|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("SPLIT_NODE")) {
				// Skip entity instances, constant drivers/sinks, and dummy nodes used for edge layouting

				continue;
			}

			List<String> completedPortGroups = new ArrayList<>();

			// Go through the ports by group; Groups are reworked in bulk
			for (ElkPort currentPort : childNode.getPorts()) {
				List<ElkPort> portsInCurrentPortGroup = new ArrayList<>();
				List<ElkPort> edgelessPorts = new ArrayList<>();
				String currentPortGroupName = currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);

				if (completedPortGroups.contains(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME))) {
					continue;
				}

				completedPortGroups.add(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

				// Get all ports belonging to the current group
				// Edgeless ports are handled separately
				for (ElkPort port : childNode.getPorts()) {
					if (port.getProperty(FEntwumSOptions.PORT_GROUP_NAME).equals(currentPortGroupName)) {
						if (!port.getOutgoingEdges().isEmpty() || !port.getIncomingEdges().isEmpty()) {
							edgelessPorts.add(port);
						} else {
							portsInCurrentPortGroup.add(port);
						}
					}
				}

				HashMap<ElkNode, HashMap<String, List<ElkPort>>> sourceSinkGroupMap = new HashMap<>();

				// Group edge-having ports by destination/source
				for (ElkPort port : portsInCurrentPortGroup) {
					List<ElkEdge> edgeList;
					if (!port.getIncomingEdges().isEmpty()) {
						edgeList = port.getIncomingEdges();
					} else {
						edgeList = port.getOutgoingEdges();
					}

					for (ElkEdge outEdge : edgeList) {
						ElkConnectableShape target = outEdge.getTargets().getFirst();
						String sinkGroupName = target.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
						ElkNode targetNode = ((ElkPort) target).getParent();

						if (!sourceSinkGroupMap.containsKey(targetNode)) {
							sourceSinkGroupMap.put(targetNode, new HashMap<>());
						}

						HashMap<String, List<ElkPort>> currentNodeMap = sourceSinkGroupMap.get(targetNode);

						if (!currentNodeMap.containsKey(sinkGroupName)) {
							currentNodeMap.put(sinkGroupName, new ArrayList<>());
						}

						currentNodeMap.get(sinkGroupName).add(port);
					}
				}

				List<BundleRange> bundleList = new ArrayList<>();

				// Now create the bundles for the different groupings
				for (ElkNode keyNode : sourceSinkGroupMap.keySet()) {
					HashMap<String, List<ElkPort>> currentMap = sourceSinkGroupMap.get(keyNode);

					for (String groupKey : currentMap.keySet()) {
						List<ElkPort> portList = currentMap.get(groupKey);
						List<SignalElement> signalElements = new ArrayList<>();

						for (ElkPort port : portList) {
							int indexInSignal = !port.getIncomingEdges().isEmpty() ?
									port.getIncomingEdges().getFirst().getProperty(FEntwumSOptions.INDEX_IN_SIGNAL) :
									port.getOutgoingEdges().getFirst().getProperty(FEntwumSOptions.INDEX_IN_SIGNAL);

							SignalElement toAdd = new SignalElement(indexInSignal, port,
									port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP));

							signalElements.add(toAdd);
						}

						bundleList.addAll(RangeCalculator.calculateRanges(signalElements));
					}
				}

				// sort bundles for edge crossing minimization
				bundleList.sort(BundleRange::compareTo);

				// Now we can create the ports
				// First, create the bundle port at the

				// Now create the splits/aggregations
				// The unbalanced binary tree will always expand to the south (whether eastwards or westwards depends on
				// whether a sink or a source is currently being reworked)
				// Therefore, descending orders should prevent edge crossings better than ascending or random orders
				for (BundleRange range : bundleList.reversed()) {

				}

				List<SignalElement> edgelessIndexes = new ArrayList<>();

				// Collect range data for leftover bundled port
				for (ElkPort port : edgelessPorts) {
					SignalElement toAdd = new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), port, null);

					edgelessIndexes.add(toAdd);
				}

				// Calculate edgeless ranges
				List<BundleRange> edgelessRanges = RangeCalculator.calculateRanges(edgelessIndexes);

				edgelessRanges.sort(BundleRange::compareTo);

				for (BundleRange range : edgelessRanges.reversed()) {

				}
			}


			HashMap<ElkNode, HashMap<String, ElkPort>> oppositeCellPortGroupMap = new HashMap<>();
			HashMap<ElkNode, HashMap<String, List<SignalElement>>> oppositeCellPortIndecesMap = new HashMap<>();
			HashMap<String, ElkPort> currentCellPortGroupMap;
			HashMap<String, List<SignalElement>> currentCellPortIndecesMap;
			List<ElkPort> removePortList = new ArrayList<>();
			ElkNode oppositeNode;

			// Go through every port, bundle as necessary
			for (ElkPort port : childNode.getPorts()) {
				if (port.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.SIGNAL_SINGLE)
						|| port.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT_MULTIPLE)
						|| port.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT_SINGLE)) {
					// skip constant and single ports

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
						oppositeNode =
								((ElkPort) port.getOutgoingEdges().getFirst().getTargets().getFirst()).getParent();
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

					Boolean isReversed =
							currentCellPortGroupMap.get(portGroupName).getProperty(FEntwumSOptions.MSB_FIRST);

					if (rangeList.size() > 1) {

						StringBuilder builder = new StringBuilder(portGroupName);

						builder.append(" [");

						boolean first = true;

						for (BundleRange range : isReversed ? rangeList : rangeList.reversed()) {
							if (!first) {
								builder.append("; ");
							}

							if (isReversed) {
								builder.append(range.containedRange().lower());
								builder.append(":");
								builder.append(range.containedRange().upper());
							} else {
								builder.append(range.containedRange().upper());
								builder.append(":");
								builder.append(range.containedRange().lower());
							}

							first = false;
						}

						builder.append("]");

						currentCellPortGroupMap.get(portGroupName).getLabels().clear();
						ElkElementCreator.createNewPortLabel(builder.toString(),
								currentCellPortGroupMap.get(portGroupName), settings);

						logger.atWarn().setMessage("Found more than one range for group {}. Skipping...").addArgument(
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
									(isReversed ?
											" [" + containedRange.lower() + ":" + containedRange.upper() + "]" :
											" [" + containedRange.upper() + ":" + containedRange.lower() + "]")),
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
