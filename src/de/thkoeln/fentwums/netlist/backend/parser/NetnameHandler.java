package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.SignalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class NetnameHandler {
    public NetnameHandler() {}

    public void handleNetnames (HashMap<String, Object> netnames, String modulename,
                                HashMap<Integer, SignalTree> signalMap) {
        HashMap<String, Object> currentNet;
        HashMap<String, Object> currentNetAttributes;
        String currentNetPath;
        String[] currentNetPathSplit;
        ArrayList<Object> bitList;
        String unusedBits;
        String[] unusedBitsSplit = new String[0];
        int currentBitIndex = 0;

        SignalTree currentSignalTree;
        SignalNode currentSignalNode;

        for (String currentNetName : netnames.keySet()) {
            currentNet = (HashMap<String, Object>) netnames.get(currentNetName);
            currentNetAttributes = (HashMap<String, Object>) currentNet.get("attributes");
            currentBitIndex = 0;

            if (currentNetAttributes.containsKey("hdlname")) {
                currentNetPath = (String) currentNetAttributes.get("hdlname");
            } else {
                currentNetPath =
                        currentNetName.replaceFirst("\\$flatten", "").replaceAll("\\$auto\\$ghdl\\.cc","ghdl")
                                .replaceAll("\\\\", "").replaceAll("\\.", " ");
            }

            currentNetPathSplit = currentNetPath.split(" ");

            if (currentNetAttributes.containsKey("unused_bits")) {
                unusedBits = ((String) currentNetAttributes.get("unused_bits"));
                unusedBitsSplit = unusedBits.split(" ");
            }

            // Get relevant signal tree

            bitList = (ArrayList<Object>) currentNet.get("bits");

            for (Object bit : bitList) {
                if (bit instanceof String) {
                    continue;   // Constant drivers cant be routed using the signal tree
                }

                int finalCurrentBitIndex = currentBitIndex;
                if (Arrays.stream(unusedBitsSplit).anyMatch(x -> x.equals(String.valueOf(finalCurrentBitIndex)))) {
                    continue;
                }

                currentBitIndex++;


                currentSignalTree = signalMap.get((Integer) bit);

                currentSignalNode = currentSignalTree.getHRoot().getHChildren().get(modulename);

                for(int i = 0; i < currentNetPathSplit.length - 1; i++) {
                    currentSignalNode = currentSignalNode.getHChildren().get(currentNetPathSplit[i]);

                    if (currentSignalNode == null) {
                        break;
                    }
                }

                if (currentSignalNode == null) {
                    System.out.println("Unknown cell; Bit " + (int) bit);

                    continue;
                }

                currentSignalNode.setSVisited(true);
                currentSignalNode.setSName(currentNetPathSplit[currentNetPathSplit.length - 1]);

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

            currentGraphNode = currentSignalNode.getSPort().getParent();

            checkParent(currentSignalTree, currentSignalNode, modulename, currentGraphNode);
        }
    }

    private void checkParent(SignalTree currentSignalTree, SignalNode currentSignalNode, String modulename,
                             ElkNode currentGraphNode) {
        // TODO add handling for "u-turn signals" -> signals which enter a node and leave it without any changes
        // requires updates to signalnode datastructure and its handling
        // do they even exist?


        // ===================
        // ALles müll


        SignalNode parent = currentSignalNode.getHParent();
        ElkPort source, sink;

        if (currentSignalNode.getSPort() == null) {
            ElkPort newOutPort = createPort(currentGraphNode);
            newOutPort.setDimensions(10, 10);
            newOutPort.setProperty(CoreOptions.PORT_SIDE, PortSide.EAST);

            ElkLabel newOutPortLabel = createLabel(currentSignalNode.getSName(), newOutPort);
            newOutPortLabel.setDimensions(newOutPortLabel.getText().length() * 7 + 1, 10);

            source = newOutPort;
        } else {
            source = currentSignalNode.getSPort();
        }

        if (parent.getSVisited() == true && !parent.getSName().equals("root")) {
            if (!parent.getHParent().getSName().equals("root")) {
                if (parent.getSPort() == null) {

                    ElkPort newInPort = createPort(currentGraphNode.getParent());
                    newInPort.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);
                    newInPort.setDimensions(10, 10);

                    ElkLabel newInPortLabel = createLabel(parent.getSName(), newInPort);
                    newInPortLabel.setDimensions(newInPortLabel.getText().length() * 7 + 1, 10);

                    sink = newInPort;
                } else {
                    sink = parent.getSPort();
                }
                ElkEdge newEdge = createSimpleEdge(source, sink);

                if (!source.equals(sink)) {
                    System.out.println("wtf that wasnt supposed to happen");
                }
            }

            checkParent(currentSignalTree, parent, modulename, currentGraphNode.getParent());
        }

        //checkLateral(parent, currentSignalNode, currentGraphNode);


    }

    private void checkLateral(SignalNode currentNode, SignalNode precursor, ElkNode precursorGraphNode) {
        SignalNode currentLateral;
        ElkPort sink, source;
        ElkNode lateralGraphNode;

        for (String lateral : currentNode.getHChildren().keySet()) {
            currentLateral = currentNode.getHChildren().get(lateral);

            if (currentLateral.getSVisited() && !currentLateral.getSName().equals(precursor.getSName())) {
                // Find node in elk graph

                for (ElkNode candidate : precursorGraphNode.getParent().getChildren()) {
                    candidate.getIdentifier();
                }

                if (precursor.getSPort() == null) {

                } else {
                    source = precursor.getSPort();
                }

                if (currentLateral.getSPort() == null) {

                } else {
                    sink = precursor.getSPort();
                }

                //ElkEdge newEdge = createSimpleEdge(sink, source);

                //checkChildren(currentLateral, lateralGraphNode);
            }
        }
    }

    private void checkChildren(SignalNode parent) {
        SignalNode currentChild;

        for (String child : parent.getHChildren().keySet()) {
            currentChild = parent.getHChildren().get(child);

            if (currentChild.getHChildren().isEmpty()) {
                return;
            }

            if (currentChild.getSVisited() == true) {
                ElkEdge newEdge = createSimpleEdge(parent.getSPort(), currentChild.getSPort());

                checkChildren(currentChild);
            }
        }
    }
}
