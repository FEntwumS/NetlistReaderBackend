package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
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

        System.out.println(sId);

        bundleRecursively(sRoot, sId);
    }

    public void bundleRecursively(SignalNode sNode, int sId) {
        SignalNode nextNode;
        SignalNode bundleNode;

        String path = sNode.getAbsolutePath();

        if (sId == 1161) {
            SignalTree toInspect = treeMap.get(sId);
            System.out.println(path);
        }

        if (path.trim().equals("addressing_top addr_master_1 adr")) {
            System.out.println("addr_master_1 adr");
        }

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
        HashMap<ElkNode, ElkPort> bundlePortMap = new HashMap<>();
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
                bundlePort = bundlePortMap.get(containingNode);

                // transfer edges to the bundle port

                for (ElkEdge incoming : currentPort.getIncomingEdges()) {
                    if (!bundlePort.getIncomingEdges().contains(incoming)) {
                        bundlePort.getIncomingEdges().add(incoming);
                    }
                }

                currentPort.getIncomingEdges().clear();

                for (ElkEdge outgoing : currentPort.getOutgoingEdges()) {
                    if (!bundlePort.getOutgoingEdges().contains(outgoing)) {
                        bundlePort.getOutgoingEdges().add(outgoing);
                    }
                }

                currentPort.getOutgoingEdges().clear();

                // Now remove the evaluated port from its parent element
                currentPort.getParent().getPorts().remove(currentPort);
            } else {
                // add new entry
                bundlePortMap.put(containingNode, currentPort);

                bundlePort = currentPort;
            }

            currentNode.setSPort(bundlePort);
        }

        // now update labels
        for (ElkNode key : indexRangeMap.keySet()) {
            currentPort = bundlePortMap.get(key);
            currentSignalRange = indexRangeMap.get(key);
            signalName = signalNameMap.get(key);
            signalRange = new StringBuilder("[");

            currentSignalRange.sort(Integer::compareTo);    // Very important

            int cRangeStart = currentSignalRange.getFirst(), cRangeEnd = currentSignalRange.getFirst(), cVal = 0;

            for (int value : currentSignalRange) {
                if (value - cRangeEnd > 1) {
                    // skip, therefore start new range

                    signalRange.append(cRangeStart);

                    if(cRangeStart != cRangeEnd) {
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

            currentPortLabel.setText(signalName);
            currentPortLabel.setDimensions(signalName.length() * 7 + 1, 10);
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
}
