package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.helpers.CellPathFormatter;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

/**
 * Handles the data contained in the "netnames" section of the netlist. Creates ports for entities as needed,
 * populates the signal trees, extracts possible bundles, creates connections between the cells
 */
public class NetnameHandler {
	private static Logger logger = LoggerFactory.getLogger(NetnameHandler.class);

	public NetnameHandler() {

	}

	/**
	 * Updates all signal trees with occurrences and bundling information from the "netnames" section of the netlist
	 *
	 * @param netnames      HashMap containing the deserialized net details
	 * @param modulename    Name of the top level entity
	 * @param signalMap     HashMap containing all signal trees
	 * @param hierarchyTree Tree of the hierarchical netlist structure
	 * @param NetInformationHashMap Stores the connections between signal names and the associated scopes and bitindices
	 */
	public void handleNetnames(HashMap<String, Object> netnames, String modulename,
							   HashMap<Integer, SignalTree> signalMap, HierarchyTree hierarchyTree, HashMap<String, NetInformation> NetInformationHashMap) {
		HashMap<String, Object> currentNet;
		HashMap<String, Object> currentNetAttributes;
		String currentNetPath;
		String[] currentNetPathSplit;
		ArrayList<Object> bitList;
		String unusedBits;
		String[] unusedBitsSplit;
		int currentBitIndex = 0;
		CellPathFormatter formatter = new CellPathFormatter();

		SignalTree currentSignalTree;
		SignalNode currentSignalNode;
		HierarchicalNode currentHNode;
		Bundle newBundle;
		HashMap<Integer, Integer> cleanedBitMap;
		HierarchicalNode childHNode;
		String srcLocation = "";
		int currentNetIndex = 0;

		for (String currentNetName : netnames.keySet()) {
			if (currentNetIndex % 512 == 0) {
				logger.atInfo().setMessage("Net {} of {}").addArgument(currentNetIndex).addArgument(netnames.size() - 1).log();
			}

			currentNetIndex++;

			currentNet = (HashMap<String, Object>) netnames.get(currentNetName);
			currentNetAttributes = (HashMap<String, Object>) currentNet.get("attributes");
			currentBitIndex = 0;

			currentNetPath = currentNetName;

			currentNetPath = formatter.format(currentNetPath);

			currentNetPathSplit = currentNetPath.split(" ");

			if (currentNetAttributes.containsKey("unused_bits")) {
				unusedBits = ((String) currentNetAttributes.get("unused_bits"));
				unusedBitsSplit = unusedBits.split(" ");
			} else {
				unusedBitsSplit = new String[0];
			}

			// Get relevant signal tree

			bitList = (ArrayList<Object>) currentNet.get("bits");

			cleanedBitMap = new HashMap<>(bitList.size());

			if (bitList.size() > 100) {
				logger.atInfo().setMessage("Net {}: Large signal; {} bits").addArgument(currentNetIndex).addArgument(bitList.size()).log();
			}

			for (Object bit : bitList) {
				srcLocation = "";

				if (bit instanceof String) {
					currentBitIndex++;
					continue;   // Constant drivers cant be routed using the signal tree
				}

				int finalCurrentBitIndex = currentBitIndex;
				if (Arrays.stream(unusedBitsSplit).anyMatch(x -> x.equals(String.valueOf(finalCurrentBitIndex)))) {
					currentBitIndex++;

					continue;
				}

				cleanedBitMap.put((int) bit, currentBitIndex);

				currentBitIndex++;

				currentSignalTree = signalMap.get((Integer) bit);

				if (currentSignalTree == null) {
					logger.atError().setMessage("Could not find signaltree for index {}. Skipping...").addArgument(bit).log();

					continue;
				}

				currentHNode = hierarchyTree.getRoot();
				currentSignalNode = currentSignalTree.getHRoot().getHChildren().get(modulename);

				for (int i = 0; i < currentNetPathSplit.length - 1; i++) {
					currentSignalNode = currentSignalNode.getHChildren().get(currentNetPathSplit[i]);
					currentHNode = currentHNode.getChildren().get(currentNetPathSplit[i]);

					if (currentSignalNode == null || currentHNode == null) {
						break;
					}
				}

				if (currentSignalNode == null) {
					logger.atDebug().setMessage("Unknown cell; Bit {}").addArgument((int) bit).log();

					continue;
				}

				if (currentHNode == null) {
					logger.atWarn().setMessage("Missing layer in hierarchy: {}").addArgument(currentNetPath).log();

					continue;
				}

				if (currentNetAttributes.containsKey("src")) {
					srcLocation = (String) currentNetAttributes.get("src");
				}

				currentSignalNode.setSrcLocation(srcLocation);
				currentSignalNode.setSVisited(true);
				currentSignalNode.setSName(currentNetPathSplit[currentNetPathSplit.length - 1]);

				if (bitList.size() - unusedBitsSplit.length > 1) {
					newBundle = new Bundle((int) bit, cleanedBitMap);

					currentHNode.getPossibleBundles().put((int) bit, newBundle);

					// Add bundle to relevant child hNodes
					for (String key : currentHNode.getChildren().keySet()) {
						childHNode = currentHNode.getChildren().get(key);

						if (currentSignalTree.getNodeAt(childHNode.getAbsolutePath()) != null && childHNode.getChildren().isEmpty()) {
							childHNode.getPossibleBundles().put((int) bit, newBundle);
						}
					}
				}

				if (bitList.size() > 1) {
					currentSignalNode.setIndexInSignal(currentBitIndex - 1);
				} else {
					currentSignalNode.setIndexInSignal(-1);
				}

				// Check if the signal originates outside the netlist (e.g. clock, ...)
				if (currentSignalTree.getSRoot() == null) {
					currentSignalTree.setSRoot(currentSignalNode);
					currentSignalNode.setIsSource(true);
				}
			}

			NetInformationHashMap.put(currentNetName, new NetInformation(currentNetPath, cleanedBitMap.keySet()));
		}
	}

	/**
	 * Recreates the routes of all not-constant signals
	 *
	 * @param signalMap  HashMap containing all signal trees
	 * @param modulename name of top level entity
	 */
	public void recreateSignals(HashMap<Integer, SignalTree> signalMap, String modulename) {
		SignalTree currentSignalTree;
		SignalNode currentSignalNode;
		int currentSignalIndex = 0;

		// For each signal, first find its source, then work backwards towards all sinks
		for (int signalIndex : signalMap.keySet()) {
			if (currentSignalIndex % 512 == 0) {
				logger.atInfo().setMessage("Signal {} of {}").addArgument(currentSignalIndex).addArgument(signalMap.size()).log();
			}
			currentSignalIndex++;

			currentSignalTree = signalMap.get(signalIndex);

			currentSignalNode = currentSignalTree.getSRoot();

			if (currentSignalNode.getHChildren().isEmpty()) {
				routeSource(currentSignalTree, currentSignalNode);
			}

			findSinks(currentSignalTree, null);
		}
	}

	/**
	 * Routes signals whose source is a cell in the design "up" through all hierarchy layers that directly or
	 * indirectly
	 * containing the source cell and where the signal occurs
	 *
	 * @param currentSignalTree signal tree of the signal that is to be routed
	 * @param precursor         Signal occurrence in the layer below the layer where the signal will be routed to
	 */
	private void routeSource(SignalTree currentSignalTree, SignalNode precursor) {
		SignalNode currentNode = precursor.getHParent();
		ElkNode currentGraphNode;
		ElkPort sink, source;
		int currentSignalIndex;

		// dont create port, if currentNode is toplevel and no port exists
		if (currentSignalTree.getHRoot().getHChildren().containsValue(currentNode) && currentNode.getSPort() == null) {
			return;
		}

		// only create connection if the signal occurred in the current layer
		if (currentNode.getSVisited()) {
			// TODO remove if necessary
			// currentNode.setIsSource(true);
			if (currentNode.getHParent() == null || currentNode.getHParent().getSVisited() == false) {
				return;
			}
			source = precursor.getSPort();

			// create port (if it doesnt exist yet)
			// should be the default (except for output signals)
			if (currentNode.getSPort() == null) {
				currentGraphNode = source.getParent().getParent();

				if (currentGraphNode.getIdentifier().equals("root")) {
					logger.error("Routing somehow reached root node");
				}

				sink = ElkElementCreator.createNewPort(currentGraphNode, PortSide.EAST);

				// Propagate port group indication to created ports
				sink.setProperty(FEntwumSOptions.PORT_GROUP_NAME, source.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

				currentNode.setSPort(sink);

				currentSignalIndex = currentNode.getIndexInSignal();

				ElkLabel sinkLabel =
						ElkElementCreator.createNewPortLabel(currentNode.getSName() + (currentSignalIndex != -1 ?
										" [" + currentSignalIndex + "]" : ""),
								sink);
			} else {
				sink = currentNode.getSPort();
			}

			// create the connecting edge
			ElkEdge newEdge = createEdgeIfNotExists(source, sink);

			if (newEdge != null) {
				newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, precursor.getSrcLocation());
				newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, precursor.getAbsolutePath());
				newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, precursor.getSName());
				newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, precursor.getIndexInSignal());
			}

			// update signal tree
			linkSignalNodes(precursor, currentNode);

			// go up one layer
			routeSource(currentSignalTree, currentNode);
		} else {
			return;
		}
	}

	/**
	 * Finds all sinks of the signal represented by the current signal tree, then initiate routing of all found sinks
	 * <p>
	 * Starts at the root of the tree (highest hierarchy layer), then recursively descends. Each node without children
	 * that is not a source then gets routed
	 *
	 * @param currentSignalTree Signal tree defining the current signal
	 * @param currentSignalNode Current position in the signal tree
	 */
	private void findSinks(SignalTree currentSignalTree, SignalNode currentSignalNode) {
		SignalNode nextNode;
		if (currentSignalNode == null) {
			currentSignalNode = currentSignalTree.getHRoot();
		}

		// Check each node of the current layer
		for (String candidate : currentSignalNode.getHChildren().keySet()) {
			nextNode = currentSignalNode.getHChildren().get(candidate);

			// Ignore sources; they have already been routed
			if (nextNode.getIsSource() && !currentSignalNode.getSName().equals("root")) {
				continue;
			}

			if (nextNode.getHChildren().isEmpty() && !nextNode.getHParent().getSName().equals("root")) {
				// Found sink; Start routing

				routeSink(currentSignalTree, nextNode);
			} else {
				// Check a layer lower for sinks

				findSinks(currentSignalTree, nextNode);
			}
		}
	}

	/**
	 * Routes a given signal from its sink towards its source. The method searches for possible source in the current
	 * layer, one layer above and in the layer below this one. This continues recursively until the source (or a
	 * signal occurrence linked to the source) has been found
	 *
	 * @param currentSignalTree
	 * @param currentSignalNode
	 */
	private void routeSink(SignalTree currentSignalTree, SignalNode currentSignalNode) {
		SignalNode precursor = currentSignalNode.getHParent();
		ElkPort source = null, sink;
		SignalNode sourceNode;
		int currentSignalIndex;


		sink = currentSignalNode.getSPort();

		// source is most likely in same layer; search there for signal source (check port side)
		for (String candidate : precursor.getHChildren().keySet()) {
			sourceNode = precursor.getHChildren().get(candidate);

			source = sourceNode.getSPort();

			if (source != null && source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST) && !sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST) && sink.getIncomingEdges().isEmpty()) {
				ElkEdge newEdge = createEdgeIfNotExists(source, sink);

				if (newEdge != null) {
					newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, sourceNode.getSrcLocation());
					newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, sourceNode.getAbsolutePath());
					newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, sourceNode.getIndexInSignal());
					newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, sourceNode.getSName());
				}

				// update signal tree
				linkSignalNodes(currentSignalNode, sourceNode);
			}
		}

		if (sink == null) {
			return;
		}

		// check if signal came from parent, construct port as necessary
		if (precursor.getHParent().getSVisited() && sink.getIncomingEdges().isEmpty()) {
			// check if precursor source port exists
			if (precursor.getSPort() == null) {
				if (sink.getParent().getParent().getIdentifier().equals("root")) {
					// TODO fixme
					logger.error("The root node seems to contain ports");

					return;
				}

				source = ElkElementCreator.createNewPort(sink.getParent().getParent(), PortSide.WEST);
				source.setProperty(FEntwumSOptions.PORT_GROUP_NAME, sink.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

				currentSignalIndex = precursor.getIndexInSignal();

				ElkLabel sourceLabel =
						ElkElementCreator.createNewPortLabel(precursor.getSName() + (currentSignalIndex != -1 ?
								" [" + currentSignalIndex +
										"]" : ""), source);

				precursor.setSPort(source);
			} else {
				source = precursor.getSPort();
			}

			if (source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)
					&& !sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
				ElkEdge newEdge = createEdgeIfNotExists(source, sink);

				if (newEdge != null) {
					newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, precursor.getSrcLocation());
					newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, precursor.getAbsolutePath());
					newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, precursor.getIndexInSignal());
					newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, precursor.getSName());
				}

				// update signal tree
				linkSignalNodes(currentSignalNode, precursor);
			}
		}

		// If the signal occurs in a containing layer and the sink to be routed is not yet connected to the source,
		// route one layer up. This can occur when a signal occurrence is not marked in the netlist

		if (!sink.getParent().getParent().getParent().getIdentifier().equals("root") && higherUse(precursor) && sink.getIncomingEdges().isEmpty()) {
			source = precursor.getSPort();

			if (sink.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
				return;
			}

			SignalNode child = null;

			if (source == null) {
				for (String candidate : precursor.getHChildren().keySet()) {
					child = precursor.getHChildren().get(candidate);

					if (child.getSPort() != null && child.getSPort().getProperty(CoreOptions.PORT_SIDE) != PortSide.WEST) {
						source = child.getSPort();
					}
				}

				if (source == null) {
					source = ElkElementCreator.createNewPort(sink.getParent().getParent(), PortSide.WEST);
					source.setProperty(FEntwumSOptions.PORT_GROUP_NAME,
							sink.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

					currentSignalIndex = precursor.getIndexInSignal();

					ElkLabel sourceLabel;

					if (precursor.getSVisited()) {
						sourceLabel =
								ElkElementCreator.createNewPortLabel(precursor.getSName() + (currentSignalIndex != -1 ?
										" " +
												"[" + currentSignalIndex +
												"]" : ""), source);
					} else {
						sourceLabel = ElkElementCreator.createNewPortLabel(String.valueOf(currentSignalTree.getSId()),
								source);
					}

					precursor.setSPort(source);
				}
			}

			ElkEdge newEdge = createEdgeIfNotExists(source, sink);

			if (newEdge != null) {
				if (child != null) {
					newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, child.getSrcLocation());
					newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, child.getAbsolutePath());
					newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, child.getIndexInSignal());
					newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, child.getSName());
				} else {
					newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, precursor.getSrcLocation());
					newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, precursor.getAbsolutePath());
					newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, precursor.getIndexInSignal());
					newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, precursor.getSName());
				}
			}

			// update signal tree
			linkSignalNodes(currentSignalNode, precursor);
		}

		// If the current layer (directly or indirectly) contains the source but is not marked, route the signal there
		String possibleSourceBelow = getSourceBelow(precursor);
		if (!possibleSourceBelow.isEmpty()) {
			String[] possibleSourceBelowSplit = possibleSourceBelow.split(" ");
			routeSourceBelow(currentSignalTree, precursor, possibleSourceBelowSplit, 0);

			// Add final link

			sourceNode = precursor.getHChildren().get(possibleSourceBelowSplit[0]);
			source = sourceNode.getSPort();

			if (source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST) && !sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
				ElkEdge newEdge = createEdgeIfNotExists(source, sink);

				if (newEdge != null) {
					newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, sourceNode.getSrcLocation());
					newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, sourceNode.getAbsolutePath());
					newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, sourceNode.getIndexInSignal());
					newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, sourceNode.getSName());
				}

				// update signal tree
				linkSignalNodes(currentSignalNode, sourceNode);
			}
		}

		if (precursor.getHParent().getHParent() != null) {
			routeSink(currentSignalTree, precursor);
		}
	}

	/**
	 * Route signal downwards towards its source
	 *
	 * @param currentSignalTree The signal tree defining the current signal
	 * @param precursor         The signal node representing the signal occurrence in the layer where the sink for
	 *                          this part of route is located
	 * @param pathSplit         The relative path of the source node in the hierarchy. Relative to the signal node
	 *                          from which this route descends
	 * @param depth             The current depth relative to the signal node from which this route descends
	 */
	private void routeSourceBelow(SignalTree currentSignalTree, SignalNode precursor, String[] pathSplit,
								  int depth) {
		SignalNode child;
		ElkPort source, sink;

		// Get the child node where the source for the current part of the source of the current path lies
		child = precursor.getHChildren().get(pathSplit[depth]);

		if (depth < pathSplit.length - 1) {
			// First descend towards the source, then route up from there
			routeSourceBelow(currentSignalTree, child, pathSplit, depth + 1);
		}

		if (depth >= 1) {
			source = child.getSPort();
			sink = precursor.getSPort();

			if (sink == null) {
				sink = ElkElementCreator.createNewPort(source.getParent().getParent(), PortSide.EAST);
				sink.setProperty(FEntwumSOptions.PORT_GROUP_NAME, source.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

				precursor.setSPort(sink);

				ElkLabel sinkLabel = ElkElementCreator.createNewPortLabel(String.valueOf(currentSignalTree.getSId()),
						sink);
			}

			if (sink.getProperty(CoreOptions.PORT_SIDE) == PortSide.WEST) {
				return;
			}
			ElkEdge newEdge = createEdgeIfNotExists(source, sink);

			if (newEdge != null) {
				newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, child.getSrcLocation());
				newEdge.setProperty(FEntwumSOptions.LOCATION_PATH, child.getAbsolutePath());
				newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, child.getIndexInSignal());
				newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, child.getSName());
			}

			linkSignalNodes(child, precursor);
		}
	}

	/**
	 * Searches for source of the current signal (the signal represented by the signal tree that precursor is a part
	 * of)
	 *
	 * @param precursor Signal node in the layer above the layer to be searched
	 * @return Returns the path of the found source relative to the first precursor or an empty string if no source
	 * could be found
	 */
	private String getSourceBelow(SignalNode precursor) {
		String ret = "";
		SignalNode child;

		// Check all nodes in current layer

		for (String candidate : precursor.getHChildren().keySet()) {
			child = precursor.getHChildren().get(candidate);

			if (child.getIsSource()) {
				// return if source is found

				return candidate;
			} else {
				// continue search

				ret = " " + getSourceBelow(child);
			}

			if (!ret.replaceAll(" ", "").isEmpty()) {
				return candidate + ret;
			}
		}

		return "";
	}

	/**
	 * Checks if a connection already exists between source and sink; If that is not the case, create a new
	 * connection, else return null
	 *
	 * @param source The source port for the connection
	 * @param sink   The sink port for the connection
	 * @param sink   The sink port for the connection
	 * @return The created connection or null, if no connection was created
	 */
	private ElkEdge createEdgeIfNotExists(ElkPort source, ElkPort sink) {
		// Check if any edges outgoing from source connect to sink; If any such edge exists, return null
		for (ElkEdge edge : source.getOutgoingEdges()) {
			for (ElkConnectableShape target : edge.getTargets()) {
				if (target.equals(sink)) {
					return null;
				}
			}
		}

		if (source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
			logger.warn("Tried to create edge from sink to source (wrong direction)");
		}


		// create connecting edge
		ElkEdge newEdge = ElkElementCreator.createNewEdge(sink, source);

		return newEdge;
	}


	/**
	 * Links two signal nodes to create a graph containing the routes of the signal described by the containing signal
	 * tree
	 *
	 * @param child  The child node in the graph to be created -> The sink of this part of the route
	 * @param parent The parent node in the graph to be created -> The source of this part of the route
	 */
	private void linkSignalNodes(SignalNode child, SignalNode parent) {
		String key = "";

		// create in-tree connection
		child.setSParent(parent);

		// get key for child
		for (String candidate : child.getHParent().getHChildren().keySet()) {
			if (child.getHParent().getHChildren().get(candidate).equals(child)) {
				key = candidate;
				break;
			}
		}

		if (key.isEmpty()) {
			logger.error("hParent does not know its children. This should of course never happen");
		}

		parent.getSChildren().put(key, child);
	}

	/**
	 * Checks the layers containing the current layer for the source of the signal
	 *
	 * @param startNode Node where search is to start
	 * @return True if a source is in a parent layer, else false
	 */
	private boolean higherUse(SignalNode startNode) {
		SignalNode parent = startNode.getHParent();

		if (parent == null) {
			return false;
		} else if (parent.getHChildren().size() > 1 || parent.getIsSource()) {
			return true;
		} else {
			return higherUse(parent);
		}
	}
}
