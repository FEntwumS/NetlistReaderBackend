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
import org.eclipse.emf.common.util.EList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createPort;
import static org.eclipse.elk.graph.util.ElkGraphUtil.createSimpleEdge;

public class SignalBundler {
    private HashMap<Integer, SignalTree> treeMap;
    private HierarchyTree hierarchy;

    public SignalBundler() {
    }

    public void bundleSignalWithId(int sId) {
        SignalNode sRoot = treeMap.get(sId).getSRoot();

        bundleRecursively(sRoot, sId);
    }

    public void bundleRecursively(SignalNode sNode, int sId) {
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
        bundleLayer(nodesToBundle, toBundle, hNode, sId);

        // bundle the children
        for (String child : sNode.getSChildren().keySet()) {
            nextNode = sNode.getSChildren().get(child);

            if (nextNode.getIsSource() == false) {
                bundleRecursively(nextNode, sId);
            }
        }
    }

    private void bundlePorts(ArrayList<SignalNode> nodesToBundle) {
        HashMap<ElkNode, ElkPort> incomingBundlePortMap = new HashMap<>(), outgoingBundlePortMap = new HashMap<>();
        HashMap<ElkNode, ArrayList<Integer>> indexRangeMap = new HashMap<>();
        HashMap<ElkNode, String> signalNameMap = new HashMap<>();
        ArrayList<Integer> currentSignalRange;
        ElkPort currentPort, bundlePort;
        ElkNode containingNode;
        int currentIndexInSignal;
        String signalName;
        StringBuilder signalRange;
        final char separator = ';';
        ElkLabel currentPortLabel;
        boolean needEdge;
        ArrayList<ElkEdge> reworkEdgeList = new ArrayList<>(), removeEdgeList = new ArrayList<>();
        HashMap<ElkNode, HashMap<String, ElkPort>> bundlePortMap = new HashMap<>();

        ElkElementCreator creator = new ElkElementCreator();

        for (SignalNode currentNode : nodesToBundle) {
            currentPort = currentNode.getSPort();

            if (currentPort == null) {
                continue;
            }

            currentIndexInSignal = currentNode.getIndexInSignal();
            signalName = currentNode.getSName();

            // check if the node this port is attached to already has a bundle port
            containingNode = currentPort.getParent();

            // add in-vector index
            if (indexRangeMap.containsKey(containingNode)) {
                currentSignalRange = indexRangeMap.get(containingNode);

                currentSignalRange.add(currentIndexInSignal);
            } else {
                currentSignalRange = new ArrayList<>();

                currentSignalRange.add(currentIndexInSignal);

                indexRangeMap.put(containingNode, currentSignalRange);
            }

            if (!signalNameMap.containsKey(containingNode)) {
                signalNameMap.put(containingNode, signalName);
            }

            if (bundlePortMap.containsKey(containingNode)) {
                if (bundlePortMap.get(containingNode).containsKey(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME))) {
                    bundlePort = bundlePortMap.get(containingNode).get(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME));
                } else {
                    bundlePort = currentPort;
                    bundlePortMap.get(containingNode).put(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), currentPort);
                }

                reworkEdgeList.clear();
                removeEdgeList.clear();


                // transfer edges to the bundle port

                for (ElkEdge incoming : currentPort.getIncomingEdges()) {
                    needEdge = true;

                    if (incoming.getSources().isEmpty()) {
                        continue;
                    }

                    for (ElkEdge edge : bundlePort.getIncomingEdges()) {
                        if (((ElkPort) edge.getSources().getFirst()).getParent().equals(((ElkPort) incoming.getSources().getFirst()).getParent())) {
                            needEdge = false;

                            incoming.getContainingNode().getContainedEdges().remove(incoming);
                            removeEdgeList.add(incoming);

                            edge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);

                            //break;
                        }
                    }

                    if (needEdge) {
                        bundlePort.getIncomingEdges().add(incoming);
                        reworkEdgeList.add(incoming);
                    }
                }

                for (ElkEdge edge : reworkEdgeList) {
                    edge.getTargets().clear();
                    edge.getTargets().add(bundlePort);
                }

                for (ElkEdge edge : removeEdgeList) {
                    edge.getTargets().getFirst().getIncomingEdges().remove(edge);
                    edge.getSources().getFirst().getOutgoingEdges().remove(edge);
                    edge.getTargets().clear();
                    edge.getSources().clear();
                }

                reworkEdgeList.clear();
                removeEdgeList.clear();

                currentPort.getIncomingEdges().clear();

                if (!currentPort.getProperty(CoreOptions.PORT_SIDE).equals(bundlePort.getProperty(CoreOptions.PORT_SIDE))) {
                    if (outgoingBundlePortMap.containsKey(containingNode)) {
                        bundlePort = outgoingBundlePortMap.get(containingNode);
                    } else {
                        bundlePort = currentPort;
                        outgoingBundlePortMap.put(containingNode, currentPort);
                    }
                }

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

                bundlePortMap.get(containingNode).put(currentPort.getProperty(FEntwumSOptions.PORT_GROUP_NAME), currentPort);

                bundlePort = currentPort;
            }

            // TODO rework
            currentNode.setSPort(bundlePort);
        }

        // now update labels
        for (ElkNode key : incomingBundlePortMap.keySet()) {
            currentPort = incomingBundlePortMap.get(key);
            currentSignalRange = indexRangeMap.get(key);
            signalName = signalNameMap.get(key);
            signalRange = new StringBuilder("[");

            currentSignalRange.sort(Integer::compareTo);    // Very important

            int cRangeStart = currentSignalRange.getFirst(), cRangeEnd = currentSignalRange.getFirst(), cVal = 0;

            for (int value : currentSignalRange) {
                if (value - cRangeEnd > 1) {
                    // skip, therefore start new range

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

            currentPortLabel = creator.createNewLabel(signalName, currentPort);
        }
    }

    private void bundleLayer(ArrayList<SignalNode> toBundle, Bundle bundle, HierarchicalNode currentHNode, int sId) {
        // return early if the signal is already bundled
        for (int bId : bundle.getBundleSignalMap().keySet()) {
            if (currentHNode.getCurrentlyBundledSignals().contains(bId)) {
                return;
            }
        }

        // bundle them
        bundlePorts(toBundle);

        // store the indexes of the newly bundled signals
        currentHNode.getCurrentlyBundledSignals().addAll(bundle.getBundleSignalMap().keySet());
    }

    public void debundleSignalAt(String path) {
    }

    public void debundleSignalRecursively(SignalNode sNode, HierarchicalNode hNode) {
    }

    public HashMap<Integer, SignalTree> getTreeMap() {
        return treeMap;
    }

    public void setTreeMap(HashMap<Integer, SignalTree> treeMap) {
        this.treeMap = treeMap;
    }

    public HierarchyTree getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(HierarchyTree hierarchy) {
        this.hierarchy = hierarchy;
    }

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
