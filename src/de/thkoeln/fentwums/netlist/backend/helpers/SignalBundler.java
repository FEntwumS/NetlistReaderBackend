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

    // need to rework bundleSignalWithId to be recursive and the main bundling function (it will supersede
    // bundleSignalRecursively)
    // Using an actual hierarchy- and signal tree-based approach, taking into account the already existing bundles
    // (vectors inside the hierarchy), a more complete and bundler with superior scaling will be created
    //
    // but how? :57
    //
    // prerequisite: fully built signaltree
    //
    // dont want to reimplement tree traversal
    // but how to connect signalnode with hierarchicalnode? especially if child of snode is parent of respective hnode?
    // maybe add function that gives absolute path of snode, which could then be used to find the corresponding hnode?
    // seems like a lot of work (both code and for processor)
    //
    // then use hnode to find the bundle, also crosscheck it with the stored vectors
    // also need some centralised list where all currently collapsed signals are stored (just use index)
    // -> it is not necessary to store the layer
    //
    // bundlePorts()-method can stay as it is now
    //
    // If vector signal uses each bit two times (once, to get vector up a layer, and another time to use each bit to
    // drive a cell), the source shall not be simplified. easier said than done
    //
    // how to deal with additional signals?
    //

    public void bundleSignalWithId(int sId, String path) {
        SignalNode root = treeMap.get(sId).getHRoot();

        HierarchicalNode hNode = hierarchy.getNodeAt(path);
        Bundle toBundle = hierarchy.getRoot().getPossibleBundles().get(sId);

        if (toBundle == null) {
            return;
        }

        ArrayList<SignalNode> nodesToBundle = new ArrayList<>(toBundle.getBundleSignalMap().size());

        for (int isId : toBundle.getBundleSignalMap().keySet()) {
            nodesToBundle.add(treeMap.get(isId).getHRoot());
        }

        if (nodesToBundle.size() <= 1) {
            return;
        }

        bundleSignalRecursively(nodesToBundle, hierarchy.getNodeAt(path));
    }

    public void bundleSignalRecursively(ArrayList<SignalNode> nodesToBundle, HierarchicalNode root) {

        bundlePorts(nodesToBundle);

        // Then move the whole list through each of the child nodes

        // How do i work with splits?
        // what if the desired depth is different on a signal-index basis??

        // get all possible child keys

        HashSet<String> childKeySet = new HashSet<>();
        for (SignalNode node : nodesToBundle) {
            childKeySet.addAll(node.getHChildren().keySet());
        }

        ArrayList<SignalNode> childBundleList = new ArrayList<SignalNode>();

        for (String childKey : childKeySet) {
            childBundleList.clear();

            for (SignalNode node : nodesToBundle) {
                if (node.getHChildren().containsKey(childKey)) {
                    childBundleList.add(node.getHChildren().get(childKey));
                }
            }

            bundleSignalRecursively(childBundleList, null);
        }

        // Use hashmaps with elknodes to create link single port -> bundle port

        // Just wrong, should first use hierarchy to get relevant signals (layer necessary?)

        // how to do this stuff?
        // find all edges belonging to a certain signal
        // then check every edge for common sources and sinks
        // if source and sink are identical, combine them

        // store edges for a given layer in the signal tree (should also give access to source and sink ports)
        // then use bundles from hierarchy to find the relevant edges in the treemap
        // combine them
        // store existing source and destination ports and nodes
        // iterate through trees to check for duplicates sources/sinks
        // then follow the relevant signal trees to do the same bundling there

        // how to deal with vectors being split?
        // detection via portside and incoming/outgoing edges?
        // then call bundling in relevant direction?

        // the number of signals needing to be bundled will be the biggest problem
        //
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

    private void bundleLayer(HierarchicalNode currentHNode, int sId) {
        Bundle bundle;
        ArrayList<SignalNode> toBundle;

        // first get the bundle in this layer
        bundle = currentHNode.getPossibleBundles().get(sId);

        // return early if no bundle exists
        if (bundle == null) {
            return;
        }

        // return early if the signal is already bundled
        if (currentHNode.getCurrentlyBundledSignals().contains(sId)) {
            return;
        }

        toBundle = new ArrayList<>(bundle.getBundleSignalMap().size());

        // then get the necessary signal nodes
        for (int id : bundle.getBundleSignalMap().keySet()) {
            toBundle.add(treeMap.get(id).getNodeAt(currentHNode.getAbsolutePath()));
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
