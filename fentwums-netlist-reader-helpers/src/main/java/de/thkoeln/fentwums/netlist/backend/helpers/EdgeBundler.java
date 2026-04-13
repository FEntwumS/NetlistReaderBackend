package de.thkoeln.fentwums.netlist.backend.helpers;


import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.PortType;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * This is an alternative bundling approach. It only works on a single entity instance (so multiple calls are necessary)
 * and only bundles ports  on actual  cells. The ports belonging to entity instances are not affected by this bundler
 */
public class EdgeBundler {
	public static final Logger logger = LoggerFactory.getLogger(EdgeBundler.class);

	public static void bundleEdges(ElkNode entityInstance, NetlistCreationSettings settings) {
		List<AggSet> aggSetList = new ArrayList<>();

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
				int currentPortGroupSubdivision = currentPort.getProperty(FEntwumSOptions.PORT_GROUP_SPLIT_INDEX);
				String currentPortGroupSubdivisionIdentifier = currentPortGroupName + currentPortGroupSubdivision;

				if (completedPortGroups.contains(currentPortGroupSubdivisionIdentifier)) {
					portsToRemoveList.add(currentPort);

					continue;
				}

				completedPortGroups.add(currentPortGroupSubdivisionIdentifier);

				// Get all ports belonging to the current group
				// Edgeless ports are handled separately
				for (ElkPort port : childNode.getPorts()) {
					String portGroupSubdivisionIdentifier =
							port.getProperty(FEntwumSOptions.PORT_GROUP_NAME) + port.getProperty(FEntwumSOptions.PORT_GROUP_SPLIT_INDEX);

					if (portGroupSubdivisionIdentifier.equals(currentPortGroupSubdivisionIdentifier)) {
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
					if (port.getProperty(FEntwumSOptions.PORT_TYPE) == PortType.SIGNAL_SINGLE
						|| port.getProperty(FEntwumSOptions.PORT_TYPE) == PortType.CONSTANT_SINGLE) {
						coveredSignals.add(new SignalElement(port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP), null,
								null, null));
					} else {
						for (int i = port.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_LOWER_INDEX_IN_PORT_GROUP);
							i <= port.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_UPPER_INDEX_IN_PORT_GROUP);
							i++) {
							coveredSignals.add(new SignalElement(i, null, null, null));
						}
					}
				}

				BundleRange coveredRange = RangeCalculator.calculateRanges(coveredSignals, 10000).getFirst();

				HashMap<ElkNode, HashMap<String, List<PortEdgeAssociation>>> sourceSinkGroupMap = new HashMap<>();

				// Group edge-having ports by destination/source
				for (ElkPort port : portsInCurrentPortGroup) {
					List<ElkEdge> edgeList;
					boolean isSourcePort = false;
					if (!port.getIncomingEdges().isEmpty()) {
						edgeList = port.getIncomingEdges();
					} else {
						edgeList = port.getOutgoingEdges();
						isSourcePort = true;
					}

					for (ElkEdge edge : edgeList) {
						ElkConnectableShape target;
						if (isSourcePort) {
							target = edge.getTargets().getFirst();
						} else {
							target = edge.getSources().getFirst();
						}
						String groupName = target.getProperty(FEntwumSOptions.PORT_GROUP_NAME) + target.getProperty(FEntwumSOptions.PORT_GROUP_SPLIT_INDEX);
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

							int indexInSignal = port.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP);

							if (port.getProperty(FEntwumSOptions.PORT_TYPE) == PortType.SIGNAL_SINGLE
								|| port.getProperty(FEntwumSOptions.PORT_TYPE) == PortType.CONSTANT_SINGLE) {
								SignalElement toAdd = new SignalElement(indexInSignal, port,
										edge.getProperty(FEntwumSOptions.INDEX_IN_SIGNAL), edge);

								signalElements.add(toAdd);
							} else {
								for (int i = port.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_LOWER_INDEX_IN_PORT_GROUP);
									 i <= port.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_UPPER_INDEX_IN_PORT_GROUP);
									 i++) {
									SignalElement toAdd = new SignalElement(i, port,
											edge.getProperty(FEntwumSOptions.INDEX_IN_SIGNAL), edge);

									signalElements.add(toAdd);
								}
							}
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

				// Deduplicate bundles
				boolean done = false;
				int index = 1;
				while (!done && bundleList.size() > 1) {
					BundleRange existingRange = null;
					BundleRange testedRange = bundleList.get(index);

					for (int j = 0; j < index; j++) {
						BundleRange candidate = bundleList.get(j);

						if (testedRange.actualDrivers().size() != candidate.actualDrivers().size()) {
							continue;
						}

						if (testedRange.actualDrivers().isEmpty()) {
							break;
						}

						existingRange = candidate;

						for (int k = 0; k < testedRange.actualDrivers().size(); k++) {
							if (!testedRange.actualDrivers().get(k).equals(candidate.actualDrivers().get(k))) {
								existingRange = null;
								break;
							}
						}
					}

					if (existingRange != null) {
						existingRange.associatedEdges().addAll(testedRange.associatedEdges());
						bundleList.remove(index);
					} else {
						index++;
					}

					done = index >= bundleList.size();
				}

				// Check if the requested signal aggregation has already been created
				if (currentPort.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)) {
					if (bundleList.size() > 1) {
						List<Object> requestedSigbits = new ArrayList<>();

						for (int i = 0; i < bundleList.size(); i++) {
							BundleRange currentBundle = bundleList.get(i);

							int upper = currentBundle.associatedEdges().size();

							for (int j = 0; j < upper; j++) {
								ElkEdge e = currentBundle.associatedEdges().get(j);

								if (e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
										|| e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
									ElkLabel l = e.getLabels().getFirst();
									String c = l.getText();

									requestedSigbits.add(c.charAt(upper - 1 - j));
								} else {
									requestedSigbits.add(e.getProperty(FEntwumSOptions.SIGBIT));
								}
							}
						}

						AggSet existingAgg = null;
						for (AggSet candidate : aggSetList) {
							if (candidate.sigbits().size() != requestedSigbits.size()) {
								continue;
							}

							if (requestedSigbits.isEmpty()) {
								break;
							}

							existingAgg = candidate;

							for (int i = 0; i < requestedSigbits.size(); i++) {
								if (!candidate.sigbits().get(i).equals(requestedSigbits.get(i))) {
									existingAgg = null;
									break;
								}
							}

							if (existingAgg != null) {
								for (BundleRange b : bundleList) {
									moveEdgesToSource(b.associatedEdges(), existingAgg.port());
									moveEdgesToTarget(b.associatedEdges().stream().filter(edge -> !edge.getTargets().getFirst().equals(currentPort)).toList(), currentPort);
								}

								bundleList.clear();
								break;
							}
						}
					}
				}


				if (currentPort.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
					if (bundleList.size() > 1) {
						List<String> strl = bundleList.stream().map(b -> {
							String ret = "";

							ret += b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_NAME);
							ret += " [";

							if (b.containedRange().singleElement()) {
								ret += b.containedRange().lower();
							} else {
								ret += b.containedRange().upper();
								ret += ':';
								ret += b.containedRange().lower();
							}

							ret += ']';

							return ret;
						}).toList();

						SignalSplit split = ElkElementCreator.createSignalSplit(entityInstance, currentPort,
								strl, settings);

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
						List<String> strl = bundleList.stream().map(b -> {
							String ret = "";

							ret += b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_NAME);
							ret += " [";

							if (b.containedRange().singleElement()) {
								ret += b.containedRange().lower();
							} else {
								ret += b.containedRange().upper();
								ret += ':';
								ret += b.containedRange().lower();
							}

							ret += ']';

							return ret;
						}).toList();

						SignalAgg agg = ElkElementCreator.createSignalAgg(entityInstance, currentPort,
								strl, settings);

						// Draw edge
						ElkEdge fromAggEdge = ElkElementCreator.createNewEdge(currentPort, agg.outPort());
						fromAggEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
						List<Object> sigbitList = new ArrayList<>();

						for (int i = 0; i < bundleList.size(); i++) {
							BundleRange currentBundleRange = bundleList.get(i);

							// Set correct edge type for internal incoming edge and dummy edge
							if (currentBundleRange.containedRange().singleElement()) {
								ElkEdge exInEdge = agg.inPorts().get(i).getOutgoingEdges().getFirst();
								exInEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);

								ElkEdge e = currentBundleRange.associatedEdges().getFirst();

								if (e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
									|| e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
									int upper = currentBundleRange.associatedEdges().size();

									for (int j = 0; j < upper; j++) {
										ElkLabel l = e.getLabels().getFirst();
										String c = l.getText();

										sigbitList.add(c.charAt(upper - 1 - j));
									}
								} else {
									sigbitList.add(e.getProperty(FEntwumSOptions.SIGBIT));
								}
							} else {
								int upper = currentBundleRange.associatedEdges().size();

								for (int j = 0; j < upper; j++) {
									ElkEdge e = currentBundleRange.associatedEdges().get(j);

									if (e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
											|| e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
										ElkLabel l = e.getLabels().getFirst();
										String c = l.getText();

										sigbitList.add(c.charAt(upper - 1 - j));
									} else {
										sigbitList.add(e.getProperty(FEntwumSOptions.SIGBIT));
									}
								}
							}

							// Move the now bundled edges
							moveEdgesToTarget(currentBundleRange.associatedEdges(), agg.inPorts().get(i));
						}

						AggSet aggSet = new AggSet(sigbitList, agg.outPort());
						aggSetList.add(aggSet);
					} else if (bundleList.size() == 1){
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
									+ (range.containedRange().singleElement() ? "" :
									":" + range.containedRange().lower())
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
									+ (coveredRange.containedRange().singleElement() ? "" :
									":" + coveredRange.containedRange().lower())
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
			if (edge.getTargets().isEmpty() || edge.getSources().isEmpty()) {
				// Skip removed edges
				continue;
			}

			ElkEdge existingEdge = edgeExists(sourcePort, (ElkPort) edge.getTargets().getFirst());

			if (existingEdge == null) {
				edge.getSources().getFirst().getOutgoingEdges().remove(edge);
				edge.getSources().clear();
				edge.getSources().add(sourcePort);

				sourcePort.getOutgoingEdges().add(edge);
			} else if (!edge.equals(existingEdge)) {
				removeEdgeFromGraph(edge);

				existingEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
			}
		}
	}


	private static void moveEdgesToTarget(List<ElkEdge> edges, ElkPort targetPort) {
		for (ElkEdge edge : edges) {
			if (edge.getTargets().isEmpty() || edge.getSources().isEmpty()) {
				// Skip removed edges
				continue;
			}

			ElkEdge existingEdge = edgeExists((ElkPort) edge.getSources().getFirst(), targetPort);

			if (existingEdge == null) {
				edge.getTargets().getFirst().getIncomingEdges().remove(edge);
				edge.getTargets().clear();
				edge.getTargets().add(targetPort);

				targetPort.getIncomingEdges().add(edge);
			} else if (!edge.equals(existingEdge)) {
				removeEdgeFromGraph(edge);

				existingEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
			}
		}
	}

	private static ElkEdge edgeExists(ElkPort source, ElkPort sink) {
		List<ElkEdge> matchingEdgeList = source.getOutgoingEdges().stream().filter(edge -> edge.getTargets().getFirst().equals(sink)).toList();

		if (matchingEdgeList.isEmpty()) {
			return null;
		} else {
			return matchingEdgeList.getFirst();
		}
	}
}
