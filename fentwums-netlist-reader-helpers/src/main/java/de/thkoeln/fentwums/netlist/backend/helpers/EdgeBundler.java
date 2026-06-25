package de.thkoeln.fentwums.netlist.backend.helpers;


import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.elkoptions.*;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.eclipse.elk.graph.util.ElkGraphUtil.updateContainment;

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

				if (portsInCurrentPortGroup.isEmpty()) {
					logger.atWarn().setMessage("Cell {} port group {} subdivision {} has only unconnected ports. Skipping...")
							.addArgument(childNode.getIdentifier()).addArgument(currentPortGroupName).addArgument(currentPortGroupSubdivision).log();
					continue;
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

						// Use the group name and group subdivision index to group the different ports.
						// This is necessary for correct bundle creation when a node uses the same signal across
						// different port group subdivisions
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

				int portInCurrentGroupCount = 0;

				for (ElkPort p :  portsInCurrentPortGroup) {
					if (p.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT_MULTIPLE)) {
						portInCurrentGroupCount += p.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_UPPER_INDEX_IN_PORT_GROUP)
													- p.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_LOWER_INDEX_IN_PORT_GROUP)
													+ 1;
					} else if (p.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.SIGNAL_MULTIPLE)) {
						portInCurrentGroupCount += p.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_UPPER_INDEX_IN_PORT_GROUP)
								- p.getProperty(FEntwumSOptions.CANONICAL_BUNDLE_LOWER_INDEX_IN_PORT_GROUP)
								+ 1;
					} else {
						portInCurrentGroupCount++;
					}
				}

				// Get full-width bundles for special handling
				int finalPortInCurrentGroupCount = portInCurrentGroupCount;
				List<BundleRange> fullsizeBundles = bundleList.stream().filter(b -> b.containedRange().upper() - b.containedRange().lower() + 1 == finalPortInCurrentGroupCount).toList();

				// sort bundles for edge crossing minimization
				bundleList.sort(BundleRange::compareTo);

				// If there is only one port in the current group and all bundles are of size 1, the existing layouting
				// infrastructure of the ELK can be used
				if (bundleList.stream().allMatch(b -> b.containedRange().singleElement()) && portsInCurrentPortGroup.size() == 1) {
					continue;
				}

				deduplicateBundlesByDriver(bundleList);

				// Check if the requested signal aggregation has already been created
				// If so, re-use it. This greatly reduces clutter when a vector is compared to a lot of different values
				if (currentPort.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)) {
					if (bundleList.size() > 1) {
						List<Object> requestedSigbits = new ArrayList<>();

						for (BundleRange currentBundle : bundleList) {
							requestedSigbits.addAll(getSigBitListFromEdges(currentBundle.associatedEdges()));
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
									// A preexisting bundle may lead to a constant being no longer necessary
									if (b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
											|| b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
										ElkNode constNode = ((ElkPort) b.associatedEdges().getFirst().getSources().getFirst()).getParent();
										constNode.getParent().getChildren().remove(constNode);
										constNode.setParent(null);
									}

									moveEdgesToSource(b.associatedEdges(), existingAgg.port(), false);
									moveEdgesToTarget(b.associatedEdges().stream().filter(edge -> !edge.getSources().isEmpty() && !edge.getTargets().getFirst().equals(currentPort)).toList(), currentPort, false);
								}

								bundleList.clear();
								break;
							}
						}
					}
				}


				if (currentPort.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
					if (bundleList.size() > 1) {
						List<String> strl = createLabelsFromBundles(bundleList, fullsizeBundles, false);

						SignalSplit split = ElkElementCreator.createSignalSplit(entityInstance, currentPort,
								strl, settings);

						// Draw edge
						ElkEdge toSplitEdge = ElkElementCreator.createNewEdge(split.inPort(), currentPort);
						toSplitEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
						toSplitEdge.setProperty(FEntwumSOptions.NO_TIP, true);

						int offset = 0;

						for (int i = 0; i + offset < bundleList.size(); i++) {
							BundleRange currentBundleRange = bundleList.get(i + offset);

							if (fullsizeBundles.contains(currentBundleRange)) {
								moveEdgesToSource(currentBundleRange.associatedEdges(), currentPort, false);
								i -= 1;
								offset += 1;
							} else {

								// Set correct edge type for internal outgoing edge and dummy edge
								if (currentBundleRange.containedRange().singleElement()) {
									ElkEdge exOutEdge = split.outPorts().get(i).getIncomingEdges().getFirst();
									exOutEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);
								}

								// Move the now bundled edges
								moveEdgesToSource(currentBundleRange.associatedEdges(), split.outPorts().get(i), false);
							}
						}
					} else if (bundleList.size() == 1) {
						// Create direct connection
						moveEdgesToSource(bundleList.getFirst().associatedEdges().stream().filter(edge -> !edge.getSources().getFirst().equals(currentPort)).toList(),
								currentPort, false);
					}
				} else {
					if (bundleList.size() > 1) {
						if (!fullsizeBundles.isEmpty()) {
							logger.atError()
									.setMessage("Entity {} cell {} port group {} is input with more than one incoming bundles, one of which is full size")
									.addArgument(entityInstance.getIdentifier())
									.addArgument(childNode.getIdentifier())
									.addArgument(currentPortGroupName)
									.log();
						}

						List<String> strl = createLabelsFromBundles(bundleList, new ArrayList<BundleRange>(), true);

						SignalAgg agg = ElkElementCreator.createSignalAgg(entityInstance, currentPort,
								strl, settings);

						// Draw edge
						ElkEdge fromAggEdge = ElkElementCreator.createNewEdge(currentPort, agg.outPort());
						fromAggEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
						List<Object> sigbitList = new ArrayList<>();

						for (int i = 0; i < bundleList.size(); i++) {
							BundleRange currentBundleRange = bundleList.get(i);

							if (fullsizeBundles.contains(currentBundleRange)) {
								logger.error("Full size bundle on multi-bundle agg encountered. Skipping...");
								continue;
							}

							for (ElkEdge e : currentBundleRange.associatedEdges()) {
								e.setProperty(FEntwumSOptions.NO_TIP, true);
							}

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
								sigbitList.addAll(getSigBitListFromEdges(currentBundleRange.associatedEdges()));
							}

							// Move the now bundled edges
							moveEdgesToTarget(currentBundleRange.associatedEdges(), agg.inPorts().get(i), false);

							ElkEdge edge = currentBundleRange.associatedEdges().getFirst();

							if (edge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)) {
								agg.inPorts().get(i).setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_SINGLE);
								agg.inPorts().get(i).setProperty(FEntwumSOptions.PORT_SHAPE, PortShape.TAG);
								agg.inPorts().get(i).setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, false);
								ElkElementCreator.setPortWidth(agg.inPorts().get(i));
								currentPort.setX(-agg.inPorts().get(i).getWidth());
							} else if (edge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
								agg.inPorts().get(i).setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_MULTIPLE);
								agg.inPorts().get(i).setProperty(FEntwumSOptions.PORT_SHAPE, PortShape.TAG);
								agg.inPorts().get(i).setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, false);
								ElkElementCreator.setPortWidth(agg.inPorts().get(i));
								currentPort.setX(-agg.inPorts().get(i).getWidth());
							}
						}

						AggSet aggSet = new AggSet(sigbitList, agg.outPort());
						aggSetList.add(aggSet);
					} else if (bundleList.size() == 1){
						// Create direct connection
						moveEdgesToTarget(bundleList.getFirst().associatedEdges().stream().filter(edge -> !edge.getTargets().getFirst().equals(currentPort)).toList(),
								currentPort, false);
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

					reworkPort.setProperty(FEntwumSOptions.NOT_CONNECTED, true);

					// Create new label
					ElkLabel newConstLabel = ElkElementCreator
							.createNewPortLabel(currentPortGroupName
									+ "["
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
									+ "["
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

	// Special fixup pass for hierarchy-crossing signals
	// They require special handling due to loading behavior:
	// All modes except for Preloading require finished instance interfaces for good display. The bundled interface may
	// therefore be needed before the constituent edges are available.
	//
 	// The fixup needs to be run _AFTER_ the "normal" bundling pass. This is required for the correct creation of the
	// necessary splitter and aggregator nodes
	public static void fixHierarchyCrossings(ElkNode entityInstance, NetlistCreationSettings settings, boolean innerFixup) {
		PortSide comparisonPortSide = PortSide.WEST;

		if (!innerFixup) {
			comparisonPortSide = PortSide.EAST;
		}

		// First, bundle the inner part of the hierarchy crossing
		for (ElkPort crossingPort : entityInstance.getPorts()) {
			List<ElkEdge> edgeList = new ArrayList<>();
			if (crossingPort.getProperty(CoreOptions.PORT_SIDE).equals(comparisonPortSide)) {
				edgeList.addAll(crossingPort.getOutgoingEdges());
			} else {
				edgeList.addAll(crossingPort.getIncomingEdges());
			}

			HashMap<ElkNode, HashMap<String, List<PortEdgeAssociation>>> destNodePortMap = new HashMap<>();

			for (ElkEdge e : edgeList) {
				ElkPort oppositePort;
				if (crossingPort.getProperty(CoreOptions.PORT_SIDE).equals(comparisonPortSide)) {
					oppositePort = (ElkPort) e.getTargets().getFirst();
				} else {
					oppositePort = (ElkPort) e.getSources().getFirst();
				}
				ElkNode oppositeNode = oppositePort.getParent();
				String oppositePortgroup = oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
				int oppositePortgroupSubdivisionIndex = oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_SPLIT_INDEX);
				String combinedPortgroupSubdivisionName = oppositePortgroup + oppositePortgroupSubdivisionIndex;
				HashMap<String, List<PortEdgeAssociation>> oppositePortgroupSubdivisionMap = destNodePortMap.computeIfAbsent(oppositeNode, k -> new HashMap<>());

				List<PortEdgeAssociation> portEdgeAssociationList = oppositePortgroupSubdivisionMap.computeIfAbsent(combinedPortgroupSubdivisionName, k -> new ArrayList<>());

				PortEdgeAssociation association = new PortEdgeAssociation(oppositePort, e);
				portEdgeAssociationList.add(association);
			}

			List<BundleRange> bundleList = new ArrayList<>();

			for (ElkNode keyNode : destNodePortMap.keySet()) {
				HashMap<String, List<PortEdgeAssociation>> innerPortgroupSubdivisionMap = destNodePortMap.get(keyNode);

				for (String portgroupSubdivisionKey : innerPortgroupSubdivisionMap.keySet()) {
					List<PortEdgeAssociation> associationList = innerPortgroupSubdivisionMap.get(portgroupSubdivisionKey);

					for (PortEdgeAssociation association : associationList) {
						ElkPort port = association.port();
						ElkEdge edge = association.edge();
						List<SignalElement> signalElements = new ArrayList<>();

						int indexInSignal = edge.getProperty(FEntwumSOptions.INDEX_IN_SIGNAL);

						if (edge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.SINGLE)) {
							String expectedSignalName = crossingPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);

							NetAssociation netAssociation =
									edge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).stream().filter(a -> a.netName().equals(expectedSignalName)).findFirst().orElse(null);

							if (netAssociation != null) {
								indexInSignal = netAssociation.indexInNet();
							}
						}

						if (edge.getProperty(FEntwumSOptions.SIGNAL_TYPE) == SignalType.SINGLE
								|| edge.getProperty(FEntwumSOptions.SIGNAL_TYPE) == SignalType.CONSTANT) {
							SignalElement toAdd = new SignalElement(indexInSignal, port,
									edge.getProperty(FEntwumSOptions.INDEX_IN_SIGNAL), edge);

							signalElements.add(toAdd);
						} else {
							for (int i = edge.getProperty(FEntwumSOptions.CANONICAL_LOWER_INDEX_IN_SIGNAL);
							     i <= edge.getProperty(FEntwumSOptions.CANONICAL_UPPER_INDEX_IN_SIGNAL);
							     i++) {
								SignalElement toAdd = new SignalElement(i, port,
										i, edge);

								signalElements.add(toAdd);
							}
						}

						bundleList.addAll(RangeCalculator.calculateRanges(signalElements, 10000));
					}
				}
			}

			bundleList.sort(BundleRange::compareTo);

			deduplicateBundlesByContainedRange(bundleList);

			if (bundleList.size() <= 1) {
				// Todo improve behavior
				continue;
			}
			ElkNode aggSplitContainingNode = entityInstance;

			if (!innerFixup) {
				aggSplitContainingNode = entityInstance.getParent();
			}


			//Now create aggs and splits

			if (crossingPort.getProperty(CoreOptions.PORT_SIDE).equals(comparisonPortSide)) {
				// Instance input -> split
				List<String> strl = createLabelsFromBundles(bundleList, new ArrayList<>(), false);

				SignalSplit split = ElkElementCreator.createSignalSplit(aggSplitContainingNode, crossingPort, strl, settings);

				// Draw incoming edge
				ElkEdge inEdge = ElkElementCreator.createNewEdge(split.inPort(), crossingPort);
				inEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
				inEdge.setProperty(FEntwumSOptions.NO_TIP, true);

				for (int i = 0; i < bundleList.size(); i++) {
					BundleRange currentBundleRange = bundleList.get(i);
					ElkPort currentOutPort = split.outPorts().get(i);

					moveEdgesToSource(currentBundleRange.associatedEdges(), currentOutPort, true);

					if (currentBundleRange.containedRange().singleElement()) {
						currentOutPort.getIncomingEdges().getFirst().setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);
					}
				}
			} else {
				// Instance output -> agg
				List<String> strl = createLabelsFromBundles(bundleList, new ArrayList<>(), true);

				SignalAgg agg = ElkElementCreator.createSignalAgg(aggSplitContainingNode, crossingPort, strl, settings);

				// Draw outgoing edge
				ElkEdge outEdge = ElkElementCreator.createNewEdge(crossingPort, agg.outPort());
				outEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);

				for (int i = 0; i < bundleList.size(); i++) {
					BundleRange currentBundleRange = bundleList.get(i);
					ElkPort currentInPort = agg.inPorts().get(i);

					moveEdgesToTarget(currentBundleRange.associatedEdges(), currentInPort, true);

					if (currentBundleRange.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
						currentInPort.setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_MULTIPLE);
					} else if (currentBundleRange.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)) {
						currentInPort.setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_SINGLE);
					}

					if (currentBundleRange.containedRange().singleElement()) {
						currentInPort.getOutgoingEdges().getFirst().setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);
					}

					for (ElkEdge e : currentBundleRange.associatedEdges()) {
						e.setProperty(FEntwumSOptions.NO_TIP, true);
					}
				}
			}
		}

		if (innerFixup) {
			// Now, bundle the outer part of the contained entity instances
			for (ElkNode childEntityInstance : entityInstance.getChildren().stream().filter(i -> i.getProperty(FEntwumSOptions.CELL_TYPE).equals("HDL_ENTITY")).toList()) {
				fixHierarchyCrossings(childEntityInstance, settings, false);
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

	private static void moveEdgesToSource(List<ElkEdge> edges, ElkPort sourcePort, boolean updateContainment) {
		for (ElkEdge edge : edges) {
			moveEdgeToSource(edge, sourcePort, updateContainment);
		}
	}

	private static void moveEdgeToSource(ElkEdge edge, ElkPort sourcePort, boolean updateContainment) {
		if (edge.getTargets().isEmpty() || edge.getSources().isEmpty()) {
			// Skip removed edges
			return;
		}

		ElkEdge existingEdge = edgeExists(sourcePort, (ElkPort) edge.getTargets().getFirst());

		if (existingEdge == null) {
			edge.getSources().getFirst().getOutgoingEdges().remove(edge);
			edge.getSources().clear();
			edge.getSources().add(sourcePort);

			sourcePort.getOutgoingEdges().add(edge);

			if (updateContainment) {
				updateContainment(edge);
			}
		} else if (!edge.equals(existingEdge)) {
			moveNetAssociationInformation(edge, existingEdge);

			removeEdgeFromGraph(edge);

			existingEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
		}
	}

	private static void moveEdgesToTarget(List<ElkEdge> edges, ElkPort targetPort, boolean updateContainment) {
		for (ElkEdge edge : edges) {
			moveEdgeToTarget(edge, targetPort, updateContainment);
		}
	}

	private static void moveEdgeToTarget(ElkEdge edge, ElkPort targetPort, boolean updateContainment) {
		if (edge.getTargets().isEmpty() || edge.getSources().isEmpty()) {
			// Skip removed edges
			return;
		}

		ElkEdge existingEdge = edgeExists((ElkPort) edge.getSources().getFirst(), targetPort);

		if (existingEdge == null) {
			edge.getTargets().getFirst().getIncomingEdges().remove(edge);
			edge.getTargets().clear();
			edge.getTargets().add(targetPort);

			targetPort.getIncomingEdges().add(edge);

			if (updateContainment) {
				updateContainment(edge);
			}
		} else if (!edge.equals(existingEdge)) {
			moveNetAssociationInformation(edge, existingEdge);

			removeEdgeFromGraph(edge);

			existingEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
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

	private static List<Object> getSigBitListFromEdges(List<ElkEdge> edgeList) {
		List<Object> sigBitList = new ArrayList<>();

		int upper = edgeList.size();

		for (int j = 0; j < upper; j++) {
			ElkEdge e = edgeList.get(j);

			if (e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
					|| e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
				ElkLabel l = e.getLabels().getFirst();
				String c = l.getText();

				sigBitList.add(c.charAt(upper - 1 - j));
			} else {
				sigBitList.add(e.getProperty(FEntwumSOptions.SIGBIT));
			}
		}

		return sigBitList;
	}

	private static List<String> createLabelsFromBundles(List<BundleRange> bundleList, List<BundleRange> excludedBundles, boolean forAgg) {
		List<String> strl = bundleList.stream().filter(b -> !excludedBundles.contains(b)).map(b -> {
			String ret = "";

			//List<String> a =

			String sharedNameAcrossBundles = findSharedNetNameAcrossBundles(bundleList, forAgg);
			String sharedOriginName = "";
			List<Range> sharedOriginBitRanges = new ArrayList<>();
			ElkPort sharedOriginPort = null;

			if (forAgg) {
				List<NetAssociation> sharedNamesInBundle = findSharedNetNameInBundle(b);

				if (sharedNamesInBundle.stream().anyMatch(a -> a.netName().equals("dat_xnor_s"))) {
					int i = 0;
				}

				List<NetAssociation> sharedUserGeneratedNamesInBundle = sharedNamesInBundle.stream().filter(a -> a.validityLevel().equals(SignalNameValidityLevel.USER_CREATED)).toList();

				if (!sharedUserGeneratedNamesInBundle.isEmpty()) {
					if (sharedUserGeneratedNamesInBundle.size() > 1) {
						List<String> portnames = b.associatedEdges().getFirst().getContainingNode().getPorts().stream().map(p -> p.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).toList();
						List<NetAssociation> nonPortSharedNamesInBundle = sharedUserGeneratedNamesInBundle.stream().filter(a -> !portnames.contains(a.netName())).toList();

						if (forAgg) {
							ElkPort sourcePort = (ElkPort) b.associatedEdges().getFirst().getSources().getFirst();
							ElkNode sourceNode = sourcePort.getParent();

							if (sourceNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("SPLIT_CONTAINER")) {
								ElkPort splitInPort = sourceNode.getPorts().stream().filter(p -> p.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)).toList().getFirst();
								ElkEdge incomingEdge = splitInPort.getIncomingEdges().getFirst();
								ElkPort actualSourcePort = (ElkPort) incomingEdge.getSources().getFirst();

								if (actualSourcePort.getParent().equals(incomingEdge.getContainingNode())) {
									sharedOriginName = actualSourcePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
									sharedOriginPort = actualSourcePort;
								}
							} else if (sourceNode.equals(b.associatedEdges().getFirst().getContainingNode())) {
								sharedOriginName = sourcePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
								sharedOriginPort = sourcePort;
							}
						}

						if (sharedOriginName.isEmpty()) {
							if (!nonPortSharedNamesInBundle.isEmpty()) {
								sharedOriginName = nonPortSharedNamesInBundle.getFirst().netName();
							} else {
								sharedOriginName = sharedUserGeneratedNamesInBundle.getFirst().netName();
							}
						}
					} else {
						sharedOriginName = sharedUserGeneratedNamesInBundle.getFirst().netName();
					}
				}

				if (sharedOriginName.isEmpty() && !sharedNamesInBundle.isEmpty()) {
					sharedOriginName = sharedNamesInBundle.getFirst().netName();
				}
			}

			if (!sharedOriginName.isEmpty() && !sharedOriginName.equals(sharedNameAcrossBundles)) {
				List<NetAssociation> allNetAssociationsInBundle = new ArrayList<>();

				for (ElkEdge e : b.associatedEdges()) {
					if (!e.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).isEmpty()) {
						allNetAssociationsInBundle.addAll(e.getProperty(FEntwumSOptions.NET_ASSOCIATIONS));
					} else {
						Map<Integer, List<NetAssociation>> bundledNetAssociations = e.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS);
						if (bundledNetAssociations != null && !bundledNetAssociations.isEmpty()) {
							for (int key : bundledNetAssociations.keySet()) {
								allNetAssociationsInBundle.addAll(bundledNetAssociations.get(key));
							}
						}
					}
				}

				String finalSharedOriginName = sharedOriginName;
				List<NetAssociation> sharedOriginNetAssociations = new ArrayList<>(allNetAssociationsInBundle.stream().filter(a -> a.netName().equals(finalSharedOriginName)).toList());

				sharedOriginNetAssociations.sort(NetAssociation::compareTo);

				int startIndex = sharedOriginNetAssociations.getFirst().indexInNet();
				int endIndex = startIndex;

				for (int i = 1; i < sharedOriginNetAssociations.size(); i++) {
					NetAssociation currrentNetAssociation = sharedOriginNetAssociations.get(i);

					if (currrentNetAssociation.indexInNet() == endIndex + 1) {
						endIndex += 1;
					} else if (currrentNetAssociation.indexInNet() > endIndex + 1) {
						sharedOriginBitRanges.add(new Range(startIndex, endIndex));
						startIndex = currrentNetAssociation.indexInNet();
						endIndex = startIndex;
					}
				}

				sharedOriginBitRanges.add(new Range(startIndex, endIndex));

				if (sharedOriginBitRanges.size() > 1) {
					logger.warn("Found split origin range for single bundle");
				}

				ret += sharedOriginName;
				if (!sharedOriginBitRanges.isEmpty()) {
					sharedOriginBitRanges.sort(Range::compareTo);
					sharedOriginBitRanges = sharedOriginBitRanges.reversed();

					if (sharedOriginBitRanges.size() > 1 || !sharedOriginBitRanges.getFirst().singleElement() || sharedOriginBitRanges.getFirst().lower() != 0 || (sharedOriginPort != null && (sharedOriginPort.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.SIGNAL_MULTIPLE) || sharedOriginPort.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT_MULTIPLE)))) {
						ret += '[';
						ret += sharedOriginBitRanges.getFirst().singleElement() ? sharedOriginBitRanges.getFirst().lower() : (Integer.toString(sharedOriginBitRanges.getFirst().upper()) + ':' + sharedOriginBitRanges.getFirst().lower());

						if (sharedOriginBitRanges.size() > 1) {
							for (int i = 1; i < sharedOriginBitRanges.size(); i++) {
								Range currentRange = sharedOriginBitRanges.get(i);
								ret += "; ";

								ret += currentRange.singleElement() ? currentRange.lower() : (Integer.toString(currentRange.upper()) + ':' + currentRange.lower());
							}
						}

						ret += ']';
					}

					ret += "⮞ ";
				}
			}


			if (sharedNameAcrossBundles.isEmpty()) {
				if (!forAgg) {
					ret += b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_NAME);
				}
			} else {
				ret += sharedNameAcrossBundles;
			}
			ret += "[";

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

		return strl;
	}

	private static void deduplicateBundlesByDriver(List<BundleRange> bundleList) {
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
	}

	private static void deduplicateBundlesByContainedRange(List<BundleRange> bundleList) {
		// Sort the bundle list
		bundleList.sort(BundleRange::compareTo);
		for (int i = 0; i < bundleList.size(); i++) {
			BundleRange compRange = bundleList.get(i);

			for (int  j = i + 1; j < bundleList.size(); j++) {
				BundleRange candidate = bundleList.get(j);

				if (candidate.containedRange().upper() > compRange.containedRange().upper()
				&&  candidate.containedRange().lower() > compRange.containedRange().lower()) {
					break;
				}

				if (candidate.containedRange().equals(compRange.containedRange())) {
					compRange.associatedEdges().addAll(candidate.associatedEdges());
					bundleList.remove(j);
					j--;
				}
			}
		}
	}

	private static void moveNetAssociationInformation(ElkEdge sourceEdge, ElkEdge targetEdge) {
		if (sourceEdge.getProperty(FEntwumSOptions.SIGBIT).equals(targetEdge.getProperty(FEntwumSOptions.SIGBIT))) {
			int sharedSigbit =  sourceEdge.getProperty(FEntwumSOptions.SIGBIT);
			for (NetAssociation a : sourceEdge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS)) {
				ElkElementCreator.addNetAssociationToEdge(targetEdge, sharedSigbit, a);
			}

			return;
		}

		Map<Integer, List<NetAssociation>> existingAssociations = sourceEdge.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS);

		if (existingAssociations == null) {
			for (NetAssociation a :  sourceEdge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS)) {
				ElkElementCreator.addNetAssociationToEdge(targetEdge, sourceEdge.getProperty(FEntwumSOptions.SIGBIT), a);
			}
		} else {
			for (int key :  existingAssociations.keySet()) {
				List<NetAssociation> associationsForCurrentKey = existingAssociations.get(key);

				for (NetAssociation a :  associationsForCurrentKey) {
					ElkElementCreator.addNetAssociationToEdge(targetEdge, key, a);
				}
			}
		}
	}

	private static String findSharedNetNameAcrossBundles(List<BundleRange> bundleList, boolean forAgg) {
		List<List<NetAssociation>> candidates = bundleList.stream().filter(b -> !(b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
						|| b.associatedEdges().getFirst().getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT))).map(EdgeBundler::findSharedNetNameInBundle).toList();

		if (candidates.isEmpty()) {
			return "";
		}

		List<NetAssociation> choices = new ArrayList<>();

		for (List<NetAssociation> l : candidates) {
			choices.addAll(l);
		}

		if (choices.size() == 1) {
			return choices.getFirst().netName();
		}

		List<NetAssociation> results = choices.stream().filter(c -> candidates.stream().allMatch(comp -> comp.stream().anyMatch(m -> m.netName().equals(c.netName())))).toList();

		ElkPort potentialOuterPort;

		if (forAgg) {
			potentialOuterPort = (ElkPort) bundleList.getFirst().associatedEdges().getFirst().getTargets().getFirst();
		} else {
			potentialOuterPort = (ElkPort) bundleList.getFirst().associatedEdges().getFirst().getSources().getFirst();
		}

		String outerPortName = "";

		if (bundleList.getFirst().associatedEdges().getFirst().getContainingNode().equals(potentialOuterPort.getParent())) {
			outerPortName = potentialOuterPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);
		}

		if (results.isEmpty()) {
			return "";
		} else {
			NetAssociation bestMatch = results.getFirst();
			int lowestBit = Integer.MAX_VALUE;
			int highestBit = Integer.MIN_VALUE;


			for (int i = 1; i < results.size(); i++) {
				NetAssociation candidate = results.get(i);
				boolean isBetterValidityLevel = (bestMatch.validityLevel().equals(SignalNameValidityLevel.YOSYS_GENERATED)
						&& !candidate.validityLevel().equals(SignalNameValidityLevel.YOSYS_GENERATED))
						|| (bestMatch.validityLevel().equals(SignalNameValidityLevel.GHDL_GENERATED)
						&& candidate.validityLevel().equals(SignalNameValidityLevel.USER_CREATED));

				List<Integer> indexList = new ArrayList<>();

				for(BundleRange b : bundleList) {
					for (ElkEdge e : b.associatedEdges()) {
						if (e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
						|| e.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
							for (NetAssociation a : e.getProperty(FEntwumSOptions.NET_ASSOCIATIONS)) {
								indexList.add(a.indexInNet());
							}
						} else {
							if (e.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).isEmpty()) {
								for (int key : e.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS).keySet()) {
									List<NetAssociation> nameMatches = e.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS).get(key).stream().filter(a -> a.netName().equals(candidate.netName())).toList();

									for  (NetAssociation a : nameMatches) {
										int index = a.indexInNet();
										indexList.add(index);

										if (index < lowestBit) {
											lowestBit = index;
										}

										if (index > highestBit) {
											highestBit = index;
										}
									}
								}
							} else {
								List<NetAssociation> nameMatches = e.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).stream().filter(a -> a.netName().equals(candidate.netName())).toList();

								for  (NetAssociation a : nameMatches) {
									int index = a.indexInNet();
									indexList.add(index);

									if (index < lowestBit) {
										lowestBit = index;
									}

									if (index > highestBit) {
										highestBit = index;
									}
								}
							}
						}
					}
				}

				if (!forAgg) {
					indexList = new ArrayList<>(new HashSet<>(indexList));
				}

				indexList.sort(Integer::compareTo);

				boolean isContiguous = true;
				if (indexList.size() != highestBit - lowestBit + 1) {
					isContiguous = false;
				} else {
					int prev = indexList.getFirst();

					for (int j = 1; j < indexList.size(); j++) {
						int curr = indexList.get(j);
						prev += 1;

						if (prev != curr) {
							isContiguous = false;
							break;
						}
					}
				}

				if (isContiguous && (isBetterValidityLevel || candidate.netName().equals(outerPortName))) {
					bestMatch = candidate;
				}
			}

			return bestMatch.netName();
		}
	}

	private static List<NetAssociation> findSharedNetNameInBundle(BundleRange bundleRange) {
		if (bundleRange.containedRange().singleElement() && !bundleRange.associatedEdges().getFirst().getProperty(FEntwumSOptions.NET_ASSOCIATIONS).isEmpty()) {
			return bundleRange.associatedEdges().getFirst().getProperty(FEntwumSOptions.NET_ASSOCIATIONS).stream().toList();
		} else {
			// Find the list of candidates
			ElkEdge edge = bundleRange.associatedEdges().getFirst();
			List<NetAssociation> candidates;

			if (edge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).isEmpty()) {
				if (edge.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS) == null) {
					return new ArrayList<>(0);
				}

				int key = edge.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS).keySet().iterator().next();
				candidates = edge.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS).get(key);
			} else {
				candidates = edge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS);
			}

			List<NetAssociation> sharedAssociations = candidates.stream().filter(candidate -> {
				for(ElkEdge currentEdge : bundleRange.associatedEdges()) {
					if (currentEdge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.CONSTANT)
						|| currentEdge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED_CONSTANT)) {
						continue;
					}

					if (currentEdge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).isEmpty()) {
						for (int key : currentEdge.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS).keySet()) {
							if (currentEdge.getProperty(FEntwumSOptions.BUNDLED_NET_ASSOCIATIONS).get(key).stream().noneMatch(a -> candidate.netName().equals(a.netName()))) {
								return false;
							}
						}
					} else {
						if(currentEdge.getProperty(FEntwumSOptions.NET_ASSOCIATIONS).stream().noneMatch(a -> candidate.netName().equals(a.netName()))) {
							return false;
						}
					}
				}

				return true;
			}).toList();

			return sharedAssociations;
		}
	}
}
