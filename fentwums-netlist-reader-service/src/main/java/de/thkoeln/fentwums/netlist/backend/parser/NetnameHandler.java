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

public class NetnameHandler {
    ElkElementCreator creator;
    private static Logger logger = LoggerFactory.getLogger(NetnameHandler.class);

    public NetnameHandler() {
        creator = new ElkElementCreator();
    }

    public void handleNetnames(HashMap<String, Object> netnames, String modulename,
                               HashMap<Integer, SignalTree> signalMap, HierarchyTree hierarchyTree) {
        HashMap<String, Object> currentNet;
        HashMap<String, Object> currentNetAttributes;
        String currentNetPath;
        String[] currentNetPathSplit;
        ArrayList<Object> bitList;
        String unusedBits;
        String[] unusedBitsSplit = new String[0];
        int currentBitIndex = 0;
        CellPathFormatter formatter = new CellPathFormatter();

        SignalTree currentSignalTree;
        SignalNode currentSignalNode;
        HierarchicalNode currentHNode;
        Bundle newBundle;
        HashMap<Integer, Integer> cleanedBitMap = new HashMap<>(1000);
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

            if (currentNetAttributes.containsKey("hdlname")) {
                currentNetPath = (String) currentNetAttributes.get("hdlname");
            } else if (currentNetAttributes.containsKey("scopename")) {
              currentNetPath = (String) currentNetAttributes.get("scopename");
            } else {
                currentNetPath = "";
                //throw new RuntimeException("Net contains neither hdlname nor scopename attribute. Aborting");
                //currentNetPath = formatter.format(currentNetName);
            }

            // TODO find better solution
            //
            // Ignore hdlname attribute for now until better mechanism to distinguish its validity is found
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

            cleanedBitMap.clear();

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

                currentSignalNode = currentSignalTree.getHRoot().getHChildren().get(modulename);
                currentHNode = hierarchyTree.getRoot();

                for (int i = 0; i < currentNetPathSplit.length - 1; i++) {
                    currentSignalNode = currentSignalNode.getHChildren().get(currentNetPathSplit[i]);
                    currentHNode = currentHNode.getChildren().get(currentNetPathSplit[i]);

                    if (currentSignalNode == null || currentHNode == null) {
                        break;
                    }
                }

                if (currentSignalNode == null) {
                    // TODO check if this is true when a nonsensical user construct exists

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
        }
    }

    public void recreateHierarchy(HashMap<Integer, SignalTree> signalMap, String modulename) {
        SignalTree currentSignalTree;
        SignalNode currentSignalNode;
        ElkNode currentGraphNode;
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

    // Two cases exist:
    // 1. sRoot is leaf
    // 2. sRoot is not leaf
    //
    // if case 1:
    // start at sroot, create signal up to common ancestor
    // then join up all other leaves
    // then check if hierarchy extends even higher (output signal)
    //
    // if case 2:
    // just join up leaves
    //
    // TODO check for loops or other nonsensical user-generated constructs

    private void routeSource(SignalTree currentSignalTree, SignalNode precursor) {
        SignalNode currentNode = precursor.getHParent();
        ElkNode currentGraphNode;
        ElkPort sink, source;
        int currentSignalIndex;
        boolean needEdge = true;
        String key = "";

        // dont create port, if currentnode is toplevel and no port exists
        if (currentSignalTree.getHRoot().getHChildren().containsValue(currentNode) && currentNode.getSPort() == null) {
            return;
        }

        if (currentNode.getSVisited()) {
            // TODO remove if necessary
            // currentNode.setIsSource(true);
            if (currentNode.getHParent() == null || currentNode.getHParent().getSVisited() == false) {
                return;
            }
            source = precursor.getSPort();
            // create port (if doesnt exist yet)
            // should be the default (except for output signals)
            if (currentNode.getSPort() == null) {
                currentGraphNode = source.getParent().getParent();

                if (currentGraphNode.getIdentifier().equals("root")) {
                    logger.error("Routing somehow reached root node");
                }

                sink = createPort(currentGraphNode);
                sink.setDimensions(10, 10);
                sink.setProperty(CoreOptions.PORT_SIDE, PortSide.EAST);

                // Propagate port group indication to created ports
                sink.setProperty(FEntwumSOptions.PORT_GROUP_NAME, source.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

                currentNode.setSPort(sink);

                currentSignalIndex = currentNode.getIndexInSignal();

                ElkLabel sinkLabel = ElkElementCreator.createNewLabel(currentNode.getSName() + (currentSignalIndex != -1 ? " [" + currentSignalIndex + "]" : ""),
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

    private void findSinks(SignalTree currentSignalTree, SignalNode currentSignalNode) {
        SignalNode nextNode;
        if (currentSignalNode == null) {
            currentSignalNode = currentSignalTree.getHRoot();
        }

        for (String candidate : currentSignalNode.getHChildren().keySet()) {
            nextNode = currentSignalNode.getHChildren().get(candidate);

            if (nextNode.getIsSource() && !currentSignalNode.getSName().equals("root")) {
                continue;
            }

            if (nextNode.getHChildren().isEmpty() && !nextNode.getHParent().getSName().equals("root")) {
                routeSink(currentSignalTree, nextNode);
            } else {
                findSinks(currentSignalTree, nextNode);
            }
        }
    }

    private void routeSink(SignalTree currentSignalTree, SignalNode currentSignalNode) {
        SignalNode precursor = currentSignalNode.getHParent();
        ElkPort source, sink;
        SignalNode sourceNode;
        int currentSignalIndex;


        sink = currentSignalNode.getSPort();

        // else source should be in same layer; search there for signal source (check port side)
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

        // check if signal came from parent, construct port as necessary
        if (precursor.getHParent().getSVisited() && sink.getIncomingEdges().isEmpty()) {
            // check if precursor source port exists
            if (precursor.getSPort() == null) {
                if (sink.getParent().getParent().getIdentifier().equals("root")) {
                    // TODO fixme
                    logger.error("The root node seems to contain ports");

                    return;
                }
                source = createPort(sink.getParent().getParent());
                source.setDimensions(10, 10);
                source.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);
                source.setProperty(FEntwumSOptions.PORT_GROUP_NAME, sink.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

                currentSignalIndex = precursor.getIndexInSignal();

                ElkLabel sourceLabel =
                        ElkElementCreator.createNewLabel(precursor.getSName() + (currentSignalIndex != -1 ? " [" + currentSignalIndex +
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

        // Source could be somewhere in an unmarked parent
        // So continue upwards
        //
        // TODO find better names for these signals
        // as they are not named, maybe use the portname in conjunction with the port descriptor from the netlist?
        // Then add each layer as the signals traverses boundaries?

        if (sink == null) {
            return;
        }

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
                    source = createPort(sink.getParent().getParent());
                    source.setDimensions(10, 10);
                    source.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);
                    source.setProperty(FEntwumSOptions.PORT_GROUP_NAME, sink.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

                    currentSignalIndex = precursor.getIndexInSignal();

                    ElkLabel sourceLabel;

                    if (precursor.getSVisited()) {
                        sourceLabel = ElkElementCreator.createNewLabel(precursor.getSName() + (currentSignalIndex != -1 ? " [" + currentSignalIndex +
                                "]" : ""), source);
                    } else {
                        sourceLabel = ElkElementCreator.createNewLabel(String.valueOf(currentSignalTree.getSId()), source);
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

        // Check if source is located in unmarked child
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

        if (!precursor.getHParent().getSName().equals("root")) {
            routeSink(currentSignalTree, precursor);
        }
    }

    private void routeSourceBelow(SignalTree currentSignalTree, SignalNode precursor, String[] pathSplit,
                                  int depth) {
        SignalNode child;
        ElkPort source, sink;


        child = precursor.getHChildren().get(pathSplit[depth]);

        if (depth < pathSplit.length - 1) {
            routeSourceBelow(currentSignalTree, child, pathSplit, depth + 1);
        }

        if (depth >= 1) {
            source = child.getSPort();
            sink = precursor.getSPort();

            if (sink == null) {
                sink = createPort(source.getParent().getParent());
                sink.setDimensions(10, 10);
                sink.setProperty(CoreOptions.PORT_SIDE, PortSide.EAST);
                sink.setProperty(FEntwumSOptions.PORT_GROUP_NAME, source.getProperty(FEntwumSOptions.PORT_GROUP_NAME));

                precursor.setSPort(sink);

                ElkLabel sinkLabel = ElkElementCreator.createNewLabel(String.valueOf(currentSignalTree.getSId()), sink);
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

    private String getSourceBelow(SignalNode precursor) {
        String ret = "";
        SignalNode child;

        for (String candidate : precursor.getHChildren().keySet()) {
            child = precursor.getHChildren().get(candidate);

            if (child.getIsSource()) {
                return candidate;
            } else {
                ret = " " + getSourceBelow(child);
            }

            if (!ret.replaceAll(" ", "").isEmpty()) {
                return candidate + ret;
            }
        }

        return "";
    }

    private ElkEdge createEdgeIfNotExists(ElkPort source, ElkPort sink) {
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

    private boolean higherUse(SignalNode startNode) {
        SignalNode parent = startNode.getHParent();

        if (parent == null) {
            return false;
        } else if (parent.getHChildren().keySet().size() > 1 || parent.getIsSource()) {
            return true;
        } else {
            return higherUse(parent);
        }
    }

    private int highestUse(SignalNode currentSignalNode) {
        SignalNode nextNode = currentSignalNode.getHParent();

        int layersAbove = 0;
        int currentLayer = 0;

        while (nextNode != null) {
            currentLayer++;
            if (nextNode.getSVisited()) {
                layersAbove = currentLayer;
            }

            nextNode = nextNode.getHParent();
        }

        return layersAbove;
    }
}
