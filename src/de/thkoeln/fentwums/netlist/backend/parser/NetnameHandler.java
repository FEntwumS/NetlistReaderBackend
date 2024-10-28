package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.helpers.CellPathFormatter;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class NetnameHandler {
    public NetnameHandler() {
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
        HashMap<Integer, Integer> cleanedBitMap;

        for (String currentNetName : netnames.keySet()) {
            currentNet = (HashMap<String, Object>) netnames.get(currentNetName);
            currentNetAttributes = (HashMap<String, Object>) currentNet.get("attributes");
            currentBitIndex = 0;

//            if (currentNetAttributes.containsKey("hdlname")) {
//                currentNetPath = (String) currentNetAttributes.get("hdlname");
//            } else {
//                currentNetPath = formatter.format(currentNetName);
//            }

            // TODO find better solution
            //
            // Ignore hdlname attribute for now until better mechanism to distiguish its validity is found
            currentNetPath = formatter.format(currentNetName);

            currentNetPathSplit = currentNetPath.split(" ");

            if (currentNetAttributes.containsKey("unused_bits")) {
                unusedBits = ((String) currentNetAttributes.get("unused_bits"));
                unusedBitsSplit = unusedBits.split(" ");
            } else {
                unusedBitsSplit = new String[0];
            }

            // Get relevant signal tree

            bitList = (ArrayList<Object>) currentNet.get("bits");

            cleanedBitMap = new HashMap<Integer, Integer>(bitList.size());

            for (Object bit : bitList) {
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

                    System.out.println("Unknown cell; Bit " + (int) bit);
                    System.out.println("This error may be caused by unused signals left in the netlist file");

                    continue;
                }

                if (currentHNode == null) {
                    System.out.print("Missing layer in hierarchy: ");
                    for (String s : currentNetPathSplit) {
                        System.out.print(s + " ");
                    }

                    System.out.println();
                    continue;
                }

                currentSignalNode.setSVisited(true);
                currentSignalNode.setSName(currentNetPathSplit[currentNetPathSplit.length - 1]);

                if (bitList.size() - unusedBitsSplit.length > 1) {
                    newBundle = new Bundle((int) bit, cleanedBitMap);

                    currentHNode.getPossibleBundles().add(newBundle);
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

        // For each signal, first find its source, then work backwards towards all sinks
        for (int signalIndex : signalMap.keySet()) {
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

    private void routeSource(SignalTree currentTree, SignalNode precursor) {
        SignalNode currentNode = precursor.getHParent();
        ElkNode currentGraphNode;
        ElkPort sink, source;
        int currentSignalIndex;
        boolean needEdge = true;

        // dont create port, if currentnode is toplevel and no port exists
        if (currentTree.getHRoot().getHChildren().containsValue(currentNode) && currentNode.getSPort() == null) {
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
                    System.out.println("wtf! that wasnt supposed to happen :(");
                }

                sink = createPort(currentGraphNode);
                sink.setDimensions(10, 10);
                sink.setProperty(CoreOptions.PORT_SIDE, PortSide.EAST);

                currentNode.setSPort(sink);

                currentSignalIndex = currentNode.getIndexInSignal();

                ElkLabel sinkLabel = createLabel(currentNode.getSName() + (currentSignalIndex != -1 ? " [" + currentSignalIndex + "]" : ""),
                        sink);
                sinkLabel.setDimensions(sinkLabel.getText().length() * 7 + 1, 10);
            } else {
                sink = currentNode.getSPort();
            }

            // create the connecting edge
            createEdgeIfNotExists(source, sink);

            // go up one layer
            routeSource(currentTree, currentNode);
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

        // check if signal came from parent, construct port as necessary
        if (precursor.getHParent().getSVisited()) {
            // check if precursor source port exists
            if (precursor.getSPort() == null) {
                if (sink.getParent().getParent().getIdentifier().equals("root")) {
                    // TODO fixme
                    System.out.println("wtf! that wasnt supposed to happen :/");

                    return;
                }
                source = createPort(sink.getParent().getParent());
                source.setDimensions(10, 10);
                source.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);

                currentSignalIndex = precursor.getIndexInSignal();

                ElkLabel sourceLabel =
                        createLabel(precursor.getSName() + (currentSignalIndex != -1 ? " [" + currentSignalIndex +
                                "]" : ""), source);
                sourceLabel.setDimensions(sourceLabel.getText().length() * 7 + 1, 10);

                precursor.setSPort(source);
            } else {
                source = precursor.getSPort();
            }

            if (source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)
                    && !sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                createEdgeIfNotExists(source, sink);
            }
        }

        // else source should be in same layer; search there for signal source (check port side)
        for (String candidate : precursor.getHChildren().keySet()) {
            sourceNode = precursor.getHChildren().get(candidate);

            source = sourceNode.getSPort();

            if (source != null && source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST) && !sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST) && sink.getIncomingEdges().isEmpty()) {
                createEdgeIfNotExists(source, sink);
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

        // TODO remove commented part of condition
        if (!sink.getParent().getParent().getParent().getIdentifier().equals("root")/* && sink.getIncomingEdges().isEmpty()*/) {
            // Create new port on western side of precursor (input)
            source = precursor.getSPort();

            if (source == null) {
                source = createPort(sink.getParent().getParent());
                source.setDimensions(10, 10);
                source.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);

                currentSignalIndex = precursor.getIndexInSignal();

                ElkLabel sourceLabel;

                if (precursor.getSVisited()) {
                    sourceLabel = createLabel(precursor.getSName() + (currentSignalIndex != -1 ? " [" + currentSignalIndex +
                            "]" : ""), source);
                } else {
                    sourceLabel = createLabel(String.valueOf(currentSignalTree.getSId()), source);
                }
                sourceLabel.setDimensions(sourceLabel.getText().length() * 7 + 1, 10);

                precursor.setSPort(source);
            }

            if (sink.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
                return;
            }

            createEdgeIfNotExists(source, sink);
        }

        // Check if source is located in unmarked child
        String possibleSourceBelow = getSourceBelow(precursor);
        if (!possibleSourceBelow.isEmpty()) {
            String[] possibleSourceBelowSplit = possibleSourceBelow.split(" ");
            routeSourceBelow(currentSignalTree, precursor, possibleSourceBelowSplit, 0);

            // Add final link

            source = precursor.getHChildren().get(possibleSourceBelowSplit[0]).getSPort();

            if (source.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST) && !sink.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
                createEdgeIfNotExists(source, sink);
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

                precursor.setSPort(sink);

                ElkLabel sinkLabel = createLabel(String.valueOf(currentSignalTree.getSId()), sink);
                sinkLabel.setDimensions(sinkLabel.getText().length() * 7 + 1, 10);
            }

            if (sink.getProperty(CoreOptions.PORT_SIDE) == PortSide.WEST) {
                return;
            }
            createEdgeIfNotExists(source, sink);
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

    private void createEdgeIfNotExists(ElkPort source, ElkPort sink) {
        boolean needEdge = true;

        for (ElkEdge edge : source.getOutgoingEdges()) {
            for (ElkConnectableShape target : edge.getTargets()) {
                if (target.equals(sink)) {
                    needEdge = false;
                    break;
                }
            }
        }

        if (needEdge) {
            // create connecting edge
            ElkEdge newEdge = createSimpleEdge(source, sink);
        }
    }
}
