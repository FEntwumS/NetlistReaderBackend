package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
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

    public SignalBundler() {}

    public void bundleSignalWithId(int sId, String path) {
        SignalNode root = treeMap.get(sId).getHRoot();

        HierarchicalNode hNode = findHNode(path);
        Bundle toBundle = hierarchy.getRoot().getPossibleBundles().get(sId);

        ArrayList<SignalNode> nodesToBundle = new ArrayList<>(toBundle.getBundleSignalMap().size());

        for (int isId : toBundle.getBundleSignalMap().keySet()) {
            nodesToBundle.add(treeMap.get(isId).getHRoot());
        }

        if (nodesToBundle.size() <= 1) {
            return;
        }

        bundleSignalRecursively(nodesToBundle, findHNode(path));
    }

    public void bundleSignalRecursively(ArrayList<SignalNode> nodesToBundle, HierarchicalNode root) {
//        ArrayList<ElkPort> currentSinkList = new ArrayList<>(), currentSourceList = new ArrayList<>();

        // First bundle the incoming edges
        bundleLayer(nodesToBundle, true);

        // Then the outgoing edges
        bundleLayer(nodesToBundle, false);

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

            bundleSignalRecursively(childBundleList, findHNode(childKey));
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

    private void bundleLayer(ArrayList<SignalNode> nodesToBundle, boolean incomingEdges) {
        HashMap<ElkNode, ElkPort> sourcePortMap = new HashMap<>(), sinkPortMap = new HashMap<>();
        ElkPort currentPort, currentSink, currentSource, newSink, newSource;
        ElkNode currentSourceNode, currentSinkNode;
        boolean newPortCreated = false, needNewEdge = true;
        ArrayList<ElkEdge> edgeList;

        for (SignalNode node : nodesToBundle) {
            currentPort = node.getSPort();

            if(currentPort != null) {
                edgeList = new ArrayList<ElkEdge>(incomingEdges ? currentPort.getIncomingEdges() :
                        currentPort.getOutgoingEdges());

                for (ElkEdge edge : edgeList) {
                    newPortCreated = false;
                    needNewEdge = false;

                    currentSource = (ElkPort) edge.getSources().getFirst();
                    currentSink = (ElkPort) edge.getTargets().getFirst();

                    currentSinkNode = currentSink.getParent();
                    currentSourceNode = currentSource.getParent();

                    if (currentSinkNode == null || currentSourceNode == null) {
                        continue;
                    }

                    // check if nodes exist in respective hashmaps

                    // source first

                    if (sourcePortMap.containsKey(currentSourceNode)) {
                        newSource = sourcePortMap.get(currentSourceNode);
                    } else {
                        newPortCreated = true;

                        newSource = createPort(currentSourceNode);
                        newSource.setDimensions(10, 10);
                        newSource.setProperty(CoreOptions.PORT_SIDE, currentSource.getProperty(CoreOptions.PORT_SIDE));

                        sourcePortMap.put(currentSourceNode, newSource);
                    }

                    // then sink

                    if (sinkPortMap.containsKey(currentSinkNode)) {
                        newSink = sinkPortMap.get(currentSinkNode);
                    } else {
                        newPortCreated = true;

                        newSink = createPort(currentSinkNode);
                        newSink.setDimensions(10, 10);
                        newSink.setProperty(CoreOptions.PORT_SIDE, currentSink.getProperty(CoreOptions.PORT_SIDE));

                        sinkPortMap.put(currentSinkNode, newSink);
                    }

                    if (newSource == null || newSink == null) {
                        System.out.println("How did this happen?");
                        continue;
                    }

                    // if a port has been created, an edge needs to be constructed EVERY time, else need to check

                    if (newPortCreated) {
                        ElkEdge newEdge = createSimpleEdge(newSource, newSink);
                    } else {
                        for (ElkEdge candidate : newSource.getOutgoingEdges()) {
                            if (candidate.getTargets().getFirst().equals(currentSink)) {
                                needNewEdge = false;
                                break;
                            }
                        }

                        if (needNewEdge) {
                            ElkEdge newEdge = createSimpleEdge(newSource, newSink);
                        }
                    }

                    // now remove original ports and edge (-s???; i sure hope not)

//                    currentSourceNode.getPorts().remove(currentSource);
//                    currentSinkNode.getPorts().remove(currentSink);

                    // First remove edge
                    edge.getContainingNode().getContainedEdges().remove(edge);
                    currentSource.getOutgoingEdges().remove(edge);
                    currentSink.getIncomingEdges().remove(edge);

                    // Then check if the source or sink have any more connections
                    // Remove them, if possible

                    // Source first
                    if (currentSource.getIncomingEdges().isEmpty() && currentSource.getOutgoingEdges().isEmpty()) {
                        currentSourceNode.getPorts().remove(currentSource);
                    }

                    // Then sink

                    if (currentSink.getIncomingEdges().isEmpty() && currentSink.getOutgoingEdges().isEmpty()) {
                        currentSinkNode.getPorts().remove(currentSink);
                    }

                    // What needs to be removed and when? BIG question
                    // Dont remove ports too early. Most ports have at least one incoming AND at least one outgoing
                    // edge. If a port is removed while it still contains edges, the layouter will crash
                }
            }
        }
    }

    public void debundleSignalAt(String path) {}

    public void debundleSignalRecursively(SignalNode sNode, HierarchicalNode hNode) {}

    private SignalNode findSNode(String path) { return null;}

    private HierarchicalNode findHNode(String path) { return null; }

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
