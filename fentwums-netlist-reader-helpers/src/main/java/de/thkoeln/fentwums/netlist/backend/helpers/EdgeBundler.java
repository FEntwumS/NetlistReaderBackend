package de.thkoeln.fentwums.netlist.backend.helpers;


import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.*;
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
			List<ElkPort> portsToRemoveList = new ArrayList<>();
			List<ElkPort> portsToKeepList = new ArrayList<>();

			if (childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("HDL_ENTITY")
					|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("Constant driver")
					|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("SPLIT_CONTAINER")
					|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("AGG_CONTAINER")) {
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
					portsToRemoveList.add(currentPort);

					continue;
				}

				completedPortGroups.add(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

				// Get all ports belonging to the current group
				// Edgeless ports are handled separately
				for (ElkPort port : childNode.getPorts()) {
					if (port.getProperty(FEntwumSOptions.PORT_GROUP_NAME).equals(currentPortGroupName)) {
						if (port.getOutgoingEdges().isEmpty() && port.getIncomingEdges().isEmpty()) {
							edgelessPorts.add(port);
						} else {
							portsInCurrentPortGroup.add(port);
						}
					}
				}

				// Calculate the whole range that is to be represented by the current port
				List<SignalElement> coveredSignals = new ArrayList<>();
				for (ElkPort port : portsInCurrentPortGroup) {
					coveredSignals.add(new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null,
														 null, null));
				}

				BundleRange coveredRange = RangeCalculator.calculateRanges(coveredSignals, 10000).getFirst();

				HashMap<ElkNode, HashMap<String, List<PortEdgeAssociation>>> sourceSinkGroupMap = new HashMap<>();

				// Group edge-having ports by destination/source
				for (ElkPort port : portsInCurrentPortGroup) {
					List<ElkEdge> edgeList;
					if (!port.getIncomingEdges().isEmpty()) {
						edgeList = port.getIncomingEdges();
					} else {
						edgeList = port.getOutgoingEdges();
					}

					for (ElkEdge edge : edgeList) {
						ElkConnectableShape target = edge.getTargets().getFirst();
						String groupName = target.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
						ElkNode targetNode = ((ElkPort) target).getParent();

						if (!sourceSinkGroupMap.containsKey(targetNode)) {
							sourceSinkGroupMap.put(targetNode, new HashMap<>());
						}

						HashMap<String, List<PortEdgeAssociation>> currentNodeMap = sourceSinkGroupMap.get(targetNode);

						if (!currentNodeMap.containsKey(groupName)) {
							currentNodeMap.put(groupName, new ArrayList<>());
						}

						currentNodeMap.get(groupName).add(new PortEdgeAssociation(port, edge));
					}
				}

				List<BundleRange> bundleList = new ArrayList<>();

				// Now create the bundles for the different groupings
				for (ElkNode keyNode : sourceSinkGroupMap.keySet()) {
					HashMap<String, List<PortEdgeAssociation>> currentMap = sourceSinkGroupMap.get(keyNode);

					for (String groupKey : currentMap.keySet()) {
						List<PortEdgeAssociation> portList = currentMap.get(groupKey);
						List<SignalElement> signalElements = new ArrayList<>();

						for (PortEdgeAssociation association : portList) {
							ElkPort port = association.port();
							ElkEdge edge = association.edge();

							int indexInSignal = edge.getProperty(FEntwumSOptions.INDEX_IN_SIGNAL);

							SignalElement toAdd = new SignalElement(indexInSignal, port,
									port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), edge);

							signalElements.add(toAdd);
						}

						bundleList.addAll(RangeCalculator.calculateRanges(signalElements, 10000));
					}
				}

				// sort bundles for edge crossing minimization
				bundleList.sort(BundleRange::compareTo);

				// If there is only one port in the current group and all bundles are of size 1, the existing layouting
				// infrastructure of the ELK can be used
				if (bundleList.stream().allMatch(b -> b.containedRange().singleElement()) && portsInCurrentPortGroup.size() == 1) {
					continue;
				}


				if (currentPort.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
					if (bundleList.size() > 1) {
						SignalSplit split = ElkElementCreator.createSignalSplit(entityInstance, currentPort, bundleList.size());

						// Draw edge
						ElkEdge toSplitEdge = ElkElementCreator.createNewEdge(split.inPort(), currentPort);
						toSplitEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);

						for (int i = 0; i < bundleList.size(); i++) {
							BundleRange currentBundleRange = bundleList.get(i);

							// Set correct edge type for internal outgoing edge and dummy edge
							if (currentBundleRange.containedRange().singleElement()) {
								ElkEdge exOutEdge = split.outPorts().get(i).getIncomingEdges().getFirst();
								exOutEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);
							}

							// Move the now bundled edges
							moveEdgesToSource(currentBundleRange.associatedEdges(), split.outPorts().get(i));
						}
					} else {
						// Create direct connection
						moveEdgesToSource(bundleList.getFirst().associatedEdges().stream().filter(edge -> !edge.getSources().getFirst().equals(currentPort)).toList(),
										  currentPort);
					}
				} else {
					if (bundleList.size() > 1) {
						SignalAgg agg = ElkElementCreator.createSignalAgg(entityInstance, currentPort, bundleList.size());

						// Draw edge
						ElkEdge fromAggEdge = ElkElementCreator.createNewEdge(currentPort, agg.outPort());
						fromAggEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);

						for (int i = 0; i < bundleList.size(); i++) {
							BundleRange currentBundleRange = bundleList.get(i);

							// Set correct edge type for internal incoming edge and dummy edge
							if (currentBundleRange.containedRange().singleElement()) {
								ElkEdge exInEdge = agg.inPorts().get(i).getOutgoingEdges().getFirst();
								exInEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);
							}

							// Move the now bundled edges
							moveEdgesToTarget(currentBundleRange.associatedEdges(), agg.inPorts().get(i));
						}
					} else {
						// Create direct connection
						moveEdgesToTarget(bundleList.getFirst().associatedEdges().stream().filter(edge -> !edge.getTargets().getFirst().equals(currentPort)).toList(),
										  currentPort);
					}
				}

				List<SignalElement> edgelessIndexes = new ArrayList<>();

				// Collect range data for leftover bundled port
				for (ElkPort port : edgelessPorts) {
					SignalElement toAdd = new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP),
							port, null, null);

					edgelessIndexes.add(toAdd);
				}

				// Calculate edgeless ranges
				List<BundleRange> edgelessRanges = RangeCalculator.calculateRanges(edgelessIndexes, 10000);

				edgelessRanges.sort(BundleRange::compareTo);

				// Update labels for combined rangeless ports
				for (BundleRange range : edgelessRanges.reversed()) {
					ElkPort reworkPort = (ElkPort) range.actualDrivers().getFirst();

					// Remove existing label
					reworkPort.getLabels().clear();

					// Create new label
					ElkLabel newConstLabel = ElkElementCreator
							.createNewPortLabel(currentPortGroupName
									+ " ["
									+ range.containedRange().upper()
									+ (range.containedRange().singleElement() ? "" : ":" + range.containedRange().lower())
									+ "]", reworkPort, settings);

					portsToKeepList.add(reworkPort);
				}

				// Update label for current port
				{
					// Remove existing label
					currentPort.getLabels().clear();

					// Create new label
					ElkLabel newConstLabel = ElkElementCreator
							.createNewPortLabel(currentPortGroupName
														+ " ["
														+ coveredRange.containedRange().upper()
														+ (coveredRange.containedRange().singleElement() ? "" : ":" + coveredRange.containedRange().lower())
														+ "]", currentPort, settings);

					portsToKeepList.add(currentPort);
				}
			}

			// Keep ports that were found to be necessary by removing them from the removal list
			portsToRemoveList.removeAll(portsToKeepList);

			// Remove the now unnecessary ports
			for (ElkPort toRemove : portsToRemoveList) {
				if (!toRemove.getIncomingEdges().isEmpty() || !toRemove.getOutgoingEdges().isEmpty()) {
					logger.atError()
							.setMessage("Port {} on cell {} is marked for removal, while being connected to an edge")
							.addArgument(toRemove.getLabels().getFirst())
							.addArgument(toRemove.getParent().getLabels().getFirst())
							.log();
				} else {
					toRemove.getParent().getPorts().remove(toRemove);
				}
			}


			/*HashMap<ElkNode, HashMap<String, ElkPort>> oppositeCellPortGroupMap = new HashMap<>();
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
								new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null, null, null));
						currentCellPortIndecesMap.put(portGroupName, signalElements);

					} else {
						if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
							moveEdgeList = port.getOutgoingEdges().stream().toList();
						} else {
							moveEdgeList = port.getIncomingEdges().stream().toList();
						}
						currentCellPortIndecesMap.get(portGroupName).add(
								new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null, null, null));

						removePortList.add(port);
					}
				} else {
					currentCellPortGroupMap = new HashMap<>();
					currentCellPortIndecesMap = new HashMap<>();

					oppositeCellPortGroupMap.put(oppositeNode, currentCellPortGroupMap);
					oppositeCellPortIndecesMap.put(oppositeNode, currentCellPortIndecesMap);

					List<SignalElement> signalElements = new ArrayList<>();
					signalElements.add(
							new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null, null, null));

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
			}*/
		}
	}

	private static void removeEdgesFromGraph(List<ElkEdge> edges) {
		for (ElkEdge edge : edges) {
			removeEdgeFromGraph(edge);
		}
	}

	private static void removeEdgeFromGraph(ElkEdge edge) {
		// Remove from source
		edge.getSources().getFirst().getOutgoingEdges().remove(edge);

		// Remove from sink
		edge.getTargets().getFirst().getIncomingEdges().remove(edge);

		// Remove source from edge
		edge.getSources().clear();

		// Remove sink from edge
		edge.getTargets().clear();

		// Remove edge from containing node
		edge.getContainingNode().getContainedEdges().remove(edge);

		// Remove container from edge
		edge.setContainingNode(null);
	}

	private static void moveEdgesToSource(List<ElkEdge> edges, ElkPort sourcePort) {
		for (ElkEdge edge : edges) {
			if (edgeExists(sourcePort, (ElkPort) edge.getTargets().getFirst())) {
				removeEdgeFromGraph(edge);
			} else {
				edge.getSources().getFirst().getOutgoingEdges().remove(edge);
				edge.getSources().clear();
				edge.getSources().add(sourcePort);

				sourcePort.getOutgoingEdges().add(edge);
			}
		}
	}


	private static void moveEdgesToTarget(List<ElkEdge> edges, ElkPort targetPort) {
		for (ElkEdge edge : edges) {
			if (edgeExists((ElkPort) edge.getSources().getFirst(), targetPort)) {
				removeEdgeFromGraph(edge);
			} else {
				edge.getTargets().getFirst().getIncomingEdges().remove(edge);
				edge.getTargets().clear();
				edge.getTargets().add(targetPort);

				targetPort.getIncomingEdges().add(edge);
			}
		}
	}

	private  static boolean edgeExists(ElkPort source, ElkPort sink) {
		return source.getOutgoingEdges().stream().anyMatch(edge -> edge.getTargets().getFirst().equals(sink));
	}
}
