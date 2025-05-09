package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.options.SignalType;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The signal bundler uses the previously stored information from the "netnames" section of the json netlist to
 * automatically bundle a given signal. A signal may only be bundled on all layers or unbundled on all layers. A
 * signal that is bundled on certain layers and not on others where it could be bundled is not intended. Whether a
 * signal can be bundled is determined by the <code>possibleBundles</code>-field of the relevant
 * <code>HierarchicalNode</code>
 */
public class SignalBundler {
	private static Logger logger = LoggerFactory.getLogger(SignalBundler.class);

	private HashMap<Integer, SignalTree> treeMap;
	private HierarchyTree hierarchy;

	public SignalBundler() {
	}

	/**
	 * Convenience method that allows bundling of a given signal with only the signals bitindex
	 *
	 * @param sId The bitindex of the signal to be bundled
	 */
	public void bundleSignalWithId(int sId, NetlistCreationSettings settings) {
		SignalNode sRoot = treeMap.get(sId).getSRoot();

		bundleRecursively(sRoot, sId, settings);
	}

	/**
	 * Determines the bits that may be bundled around the signal occurrence described by <code>sNode</code>, then
	 * continues along the signal route(-s). The found bundle is handed off to <code>bundlePorts</code> for the actual
	 * bundling
	 *
	 * @param sNode The node describing the signal occurence
	 * @param sId   The bitindex of the signal to be bundled
	 */
	public void bundleRecursively(SignalNode sNode, int sId, NetlistCreationSettings settings) {
		SignalNode nextNode;
		SignalNode bundleNode;

		String path = sNode.getAbsolutePath();

		// find the possible bundles
		HierarchicalNode hNode = hierarchy.getNodeAt(path);
		Bundle toBundle = hNode.getPossibleBundles().get(sId);

		if (toBundle == null) {
			return;
		}

		ArrayList<SignalNode> nodesToBundle = new ArrayList<>(toBundle.getBundleSignalMap().size());

		for (int isId : toBundle.getBundleSignalMap().keySet()) {
			bundleNode = treeMap.get(isId).getNodeAt(path);

			if (bundleNode != null) {
				nodesToBundle.add(bundleNode);
			}
		}

		if (nodesToBundle.size() <= 1) {
			return;
		}

		// then bundle the signal
		bundleLayer(nodesToBundle, toBundle, hNode, settings);

		// bundle the children
		for (String child : sNode.getSChildren().keySet()) {
			nextNode = sNode.getSChildren().get(child);

			if (nextNode.getIsSource() == false) {
				bundleRecursively(nextNode, sId, settings);
			}
		}
	}

	/**
	 * Bundles the ports of the signal occurrences in <code>nodesToBundle</code> into one single port. Updates the
	 * relevant edges and creates appropriate label(-s)
	 *
	 * @param nodesToBundle The signal occurrences which are to be bundled. each signal node must be at the same
	 *                      position in the hierarchy
	 */
	private void bundlePorts(ArrayList<SignalNode> nodesToBundle, NetlistCreationSettings settings) {
		ArrayList<Integer> currentSignalRange;
		ElkPort currentPort, bundlePort = null;
		ElkNode containingNode;
		int currentIndexInSignal;
		String signalName;
		StringBuilder signalRange;
		final char separator = ';';
		ElkLabel currentPortLabel;
		boolean needEdge, needOppositePort;
		ArrayList<ElkEdge> reworkEdgeList = new ArrayList<>(), removeEdgeList = new ArrayList<>();
		HashMap<ElkNode, HashMap<String, BundlingInformation>> bundlePortMap = new HashMap<>();
		BundlingInformation currentInfo;
		ElkPort oppositePort;
		List<ElkPort> unnecessaryOppositePorts = new ArrayList<>();
		List<ElkEdge> toMove = new ArrayList<>();

		for (SignalNode currentNode : nodesToBundle) {
			if (currentNode.getInPorts() == null || (currentNode.getInPorts().isEmpty() && currentNode.getOutPort() != null)) {
				continue;
			}

			for(ElkPort p : currentNode.getInPorts()) {
				currentPort = p;

				if (currentPort == null) {
					continue;
				}

				// Since the incoming edges are bundled, input ports of the toplevel are skipped
				if (currentPort.getParent().getParent().getIdentifier().equals("root")) {
					continue;
				}

				currentIndexInSignal = currentPort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP);
				signalName = currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME);

				// check if the node this port is attached to already has a bundle port
				containingNode = currentPort.getParent();

				// each port group gets its own bundling port. this prevents input and outputs ports to be bundled
				// together and improves clarity if a bundle contains bits that are used in eg two separate input port
				// groups
				if (bundlePortMap.containsKey(containingNode)) {
					if (bundlePortMap.get(containingNode).containsKey(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME))) {
						currentInfo =
								bundlePortMap.get(containingNode).get(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME));
						bundlePort = currentInfo.port();
					} else {
						bundlePort = currentPort;

						currentInfo = new BundlingInformation(currentPort, signalName, new ArrayList<>());

						currentInfo.containedSignals().add(bundlePort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP));

						bundlePortMap.get(containingNode).put(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME),
								currentInfo);

						oppositePort = (ElkPort) bundlePort.getIncomingEdges().getFirst().getSources().getFirst();

						BundlingInformation oppositeInfo = new BundlingInformation(oppositePort, oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), new ArrayList<>());
						oppositeInfo.containedSignals().add(oppositePort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP));
						includePreviousSignals(oppositeInfo);

						if (!bundlePortMap.containsKey(oppositePort.getParent())) {
							bundlePortMap.put(oppositePort.getParent(), new HashMap<>());
						}

						bundlePortMap.get(oppositePort.getParent()).put(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), oppositeInfo);

						continue;
					}

					currentInfo.containedSignals().add(currentIndexInSignal);

					reworkEdgeList.clear();
					removeEdgeList.clear();


					// bundle sinks of incoming edges

					for (ElkEdge incoming : currentPort.getIncomingEdges()) {
						needEdge = true;
						needOppositePort = true;

						if (incoming.getSources().isEmpty()) {
							continue;
						}

						oppositePort = (ElkPort) incoming.getSources().getFirst();

						if (!bundlePortMap.containsKey(oppositePort.getParent())) {
							bundlePortMap.put(oppositePort.getParent(), new HashMap<>());
						}

						if (bundlePortMap.get(oppositePort.getParent()).containsKey(oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME))) {
							BundlingInformation oppositeBundle = bundlePortMap.get(oppositePort.getParent()).get(oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

							if (!oppositeBundle.containedSignals().contains(oppositePort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP))) {
								oppositeBundle.containedSignals().add(oppositePort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP));
							}

							needOppositePort = false;
						} else {
							BundlingInformation oppositeBundle = new BundlingInformation(oppositePort, oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), new ArrayList<>());
							oppositeBundle.containedSignals().add(oppositePort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP));

							includePreviousSignals(oppositeBundle);

							bundlePortMap.get(oppositePort.getParent()).put(oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), oppositeBundle);
						}

						for (ElkEdge edge : bundlePort.getIncomingEdges()) {
							if (((ElkPort) edge.getSources().getFirst()).getParent().equals(((ElkPort) incoming.getSources().getFirst()).getParent())
									&& edge.getSources().getFirst().getProperty(FEntwumSOptions.PORT_GROUP_NAME).equals(incoming.getSources().getFirst().getProperty(FEntwumSOptions.PORT_GROUP_NAME))) {
								// if any incoming edge of the bundle port and any incoming edge of the port that is
								// currently being checked have the same source and the come from the same port group, it is marked removal

								needEdge = false;

								incoming.getContainingNode().getContainedEdges().remove(incoming);
								removeEdgeList.add(incoming);

								edge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);

								// only move edges if the opposite port is NOT the bundlePort
								// this is needed for edges that belong to ports that are bundled, but that exit the entity they belong to
								if (!oppositePort.equals(bundlePortMap.get(oppositePort.getParent()).get(oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).port())) {
									unnecessaryOppositePorts.add(oppositePort);

									for (ElkEdge e : oppositePort.getOutgoingEdges()) {
										if (!e.equals(edge)) {
											toMove.add(e);
										}
									}
								}

							}
						}

						for (ElkEdge e : toMove) {
							e.getSources().clear();
							e.getSources().add(bundlePortMap.get(oppositePort.getParent()).get(oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).port());
						}

						toMove.clear();

						if (needEdge) {
							// if the edge has not been marked for removal, it is instead marked for rework
							bundlePort.getIncomingEdges().add(incoming);
							reworkEdgeList.add(incoming);
						}
					}

					for (ElkEdge edge : reworkEdgeList) {
						// rework the edge to point to the bundle port instead of its old sink
						edge.getTargets().clear();
						edge.getTargets().add(bundlePort);

						// update the edge thickness for better layouting
						edge.setProperty(CoreOptions.EDGE_THICKNESS, 2.8d);

						// remove the edge from its old sink
						currentPort.getIncomingEdges().remove(edge);
					}

					for (ElkEdge edge : removeEdgeList) {
						// remove the edge from its source and target
						edge.getTargets().getFirst().getIncomingEdges().remove(edge);
						edge.getSources().getFirst().getOutgoingEdges().remove(edge);

						// remove the targets and sources of the edge
						edge.getTargets().clear();
						edge.getSources().clear();

						currentPort.getIncomingEdges().remove(edge);
					}

					reworkEdgeList.clear();
					removeEdgeList.clear();

					// bundle sources of outgoing edges
					// the sources of any edge actually going somewhere can be bundled
					for (ElkEdge outgoing : currentPort.getOutgoingEdges()) {

						if (outgoing.getTargets().isEmpty()) {
							continue;
						}

						bundlePort.getOutgoingEdges().add(outgoing);
						reworkEdgeList.add(outgoing);
					}

					for (ElkEdge edge : reworkEdgeList) {

						edge.getSources().clear();
						edge.getSources().add(bundlePort);

						edge.setProperty(CoreOptions.EDGE_THICKNESS, 2.8d);

						if (!currentPort.equals(bundlePort)) {
							currentPort.getOutgoingEdges().remove(edge);
						}
					}

					// Now remove the evaluated port from its parent element
					if (currentPort.getIncomingEdges().isEmpty() && currentPort.getOutgoingEdges().isEmpty() && !currentPort.equals(bundlePort)) {
						currentPort.getParent().getPorts().remove(currentPort);
					}
				} else {
					// add new entry
					bundlePortMap.put(containingNode, new HashMap<>());

					currentInfo = new BundlingInformation(currentPort, signalName, new ArrayList<>());
					currentInfo.containedSignals().add(currentIndexInSignal);

					bundlePortMap.get(containingNode).put(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME),
							currentInfo);

					bundlePort = currentPort;

					oppositePort = (ElkPort) bundlePort.getIncomingEdges().getFirst().getSources().getFirst();

					BundlingInformation oppositeInfo = new BundlingInformation(oppositePort, oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), new ArrayList<>());
					oppositeInfo.containedSignals().add(oppositePort.getProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP));

					includePreviousSignals(oppositeInfo);

					if (!bundlePortMap.containsKey(oppositePort.getParent())) {
						bundlePortMap.put(oppositePort.getParent(), new HashMap<>());
					}

					bundlePortMap.get(oppositePort.getParent()).put(oppositePort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), oppositeInfo);
				}


			}

			if (bundlePort == null) {
				continue;
			}

			List<ElkPort> l = new ArrayList<>();
			l.add(bundlePort);

			if (bundlePort.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
				currentNode.setOutPort(bundlePort);
			} else {
				currentNode.setInPorts(l);
			}
		}

		for (ElkPort p : unnecessaryOppositePorts) {
			if (p.getParent() != null && p.getIncomingEdges().isEmpty() && p.getOutgoingEdges().isEmpty()) {
				p.getParent().getPorts().remove(p);
			}
		}

		// now update labels
		//
		// contiguous ranges of bit indexes are shortened to the start of the range and the end of the range,
		// separated by a colon
		// a range containing only a single index does not get transformed or shortened
		// if a bundle contains multiple ranges, the ranges are concatenated with a semicolon followed by space as
		// separator
		for (ElkNode key : bundlePortMap.keySet()) {
			for (String portgroup : bundlePortMap.get(key).keySet()) {
				currentInfo = bundlePortMap.get(key).get(portgroup);

				currentPort = currentInfo.port();
				currentSignalRange = currentInfo.containedSignals();
				signalName = currentInfo.signalName();
				signalRange = new StringBuilder("[");

				currentSignalRange.sort(Integer::compareTo);    // Very important

				// Only one signal was Bundled in the given port group, therefore its label does not need to be updated
				if (currentSignalRange.size() == 1) {
					continue;
				}

				currentPort.setProperty(FEntwumSOptions.BUNDLED_SIGNALS, currentSignalRange);

				int cRangeStart = currentSignalRange.getFirst(), cRangeEnd = currentSignalRange.getFirst(), cVal = 0;

				for (int value : currentSignalRange) {
					if (value - cRangeEnd > 1) {
						// if the current bit index were to be added to the range, it would no longer be contiguous.
						// therefore, a new range is started

						signalRange.append(cRangeStart);

						if (cRangeStart != cRangeEnd) {
							signalRange.append(":").append(cRangeEnd);
						}

						signalRange.append(separator + " ");
						cRangeStart = value;
					}
					cRangeEnd = value;
				}

				// add last part of range
				signalRange.append(cRangeStart);

				if (cRangeStart != cRangeEnd) {
					signalRange.append(':').append(cRangeEnd);
				}

				signalRange.append(']');

				signalName += " " + signalRange;

				currentPortLabel = currentPort.getLabels().getFirst();

				currentPort.getLabels().remove(currentPortLabel);

				currentPortLabel = ElkElementCreator.createNewPortLabel(signalName, currentPort, settings);
			}
		}
	}

	private void includePreviousSignals(BundlingInformation info) {
		if (info.port().getProperty(FEntwumSOptions.BUNDLED_SIGNALS) == null || info.port().getProperty(FEntwumSOptions.BUNDLED_SIGNALS).isEmpty()) {
			return;
		}

		for (int s : info.port().getProperty(FEntwumSOptions.BUNDLED_SIGNALS)) {
			if (!info.containedSignals().contains(s)) {
				info.containedSignals().add(s);
			}
		}
	}

	/**
	 * Checks if the layer that is to be bundled is already bundled. If not, bundles the layer, else it just returns
	 *
	 * @param toBundle     The signal nodes describing the signal occurrences that are to be bundled
	 * @param bundle       The signal indices and the corresponding bit indices
	 * @param currentHNode The current position in the hierarchy
	 */
	private void bundleLayer(ArrayList<SignalNode> toBundle, Bundle bundle, HierarchicalNode currentHNode,
							 NetlistCreationSettings settings) {
		// return early if the signal is already bundled
		for (int bId : bundle.getBundleSignalMap().keySet()) {
			if (currentHNode.getCurrentlyBundledSignals().contains(bId)) {
				return;
			}
		}

		// bundle them
		bundlePorts(toBundle, settings);

		// store the indexes of the newly bundled signals
		currentHNode.getCurrentlyBundledSignals().addAll(bundle.getBundleSignalMap().keySet());
	}

	public void debundleSignalAt(String path) {
		throw new RuntimeException("Not implemented yet");
	}

	public void debundleSignalRecursively(SignalNode sNode, HierarchicalNode hNode) {
		throw new RuntimeException("Not implemented yet");
	}

	/**
	 * Gets the HashMap containing all signal trees
	 *
	 * @return The HashMap containing all signal trees
	 */
	public HashMap<Integer, SignalTree> getTreeMap() {
		return treeMap;
	}

	/**
	 * Sets the HashMap containing all signal trees
	 *
	 * @param treeMap The HashMap containing all signal trees
	 */
	public void setTreeMap(HashMap<Integer, SignalTree> treeMap) {
		this.treeMap = treeMap;
	}

	/**
	 * Gets the hierarchy of the current netlist
	 *
	 * @return The hierarchy of the current netlist
	 */
	public HierarchyTree getHierarchy() {
		return hierarchy;
	}

	/**
	 * Sets the hierarchy of the current netlist
	 *
	 * @param hierarchy The hierarchy of the current netlist
	 */
	public void setHierarchy(HierarchyTree hierarchy) {
		this.hierarchy = hierarchy;
	}

	/**
	 * Checks whether the given <code>ElkNode</code> is in any way a child of the root node
	 *
	 * @param node The node that is to be tested
	 * @return True if root is reachable, else false
	 */
	private boolean isRootReachable(ElkNode node) {
		if (node.getIdentifier().equals("root")) {
			return true;
		}
		if (node.getParent() == null) {
			return false;
		}

		return isRootReachable(node.getParent());
	}
}
