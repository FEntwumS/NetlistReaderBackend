package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.createPort;
import static org.eclipse.elk.graph.util.ElkGraphUtil.createSimpleEdge;

public class SignalBundler {
    private HashMap<Integer, SignalTree> treeMap;
    private HierarchyTree hierarchy;

    public SignalBundler() {}

    public void bundleSignalWithId(int sId, String path) {
        SignalNode root = treeMap.get(sId).getHRoot();

        HierarchicalNode hNode = findHNode(path);
        Bundle toBundle = hNode.getPossibleBundles().get(sId);

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
        HashMap<ElkNode, ElkPort> bundleSourcePortMap = new HashMap<>(), bundleSinkPortMap = new HashMap<>();
        ElkPort currentPort, currentSink, currentSource, newSink, newSource;
        ElkNode currentSourceNode, currentSinkNode;
        boolean newPortCreated = false, needNewEdge = true;

        for (SignalNode node : nodesToBundle) {
            currentPort = node.getSPort();

            if(currentPort != null) {
                for (ElkEdge edge : currentPort.getIncomingEdges()) {
                    currentSource = (ElkPort) edge.getSources().getFirst();
                    currentSink = (ElkPort) edge.getTargets().getFirst();

                    currentSinkNode = currentSink.getParent();
                    currentSourceNode = currentSource.getParent();

                    // check if nodes exist in respective hashmaps

                    // source first

                    if (bundleSourcePortMap.containsKey(currentSourceNode)) {
                        newSource = bundleSourcePortMap.get(currentSourceNode);
                    } else {
                        newPortCreated = true;

                        newSource = createPort(currentSourceNode);
                        newSource.setDimensions(10, 10);
                        newSource.setProperty(CoreOptions.PORT_SIDE, currentSource.getProperty(CoreOptions.PORT_SIDE));

                        bundleSourcePortMap.put(currentSourceNode, newSource);
                    }

                    // then sink

                    if (bundleSinkPortMap.containsKey(currentSinkNode)) {
                        newSink = bundleSinkPortMap.get(currentSinkNode);
                    } else {
                        newPortCreated = true;

                        newSink = createPort(currentSinkNode);
                        newSink.setDimensions(10, 10);
                        newSink.setProperty(CoreOptions.PORT_SIDE, currentSink.getProperty(CoreOptions.PORT_SIDE));

                        bundleSinkPortMap.put(currentSinkNode, newSink);
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

                    currentSourceNode.getPorts().remove(currentSource);
                    currentSinkNode.getPorts().remove(currentSink);
                    edge.getContainingNode().getContainedEdges().remove(edge);

                    // What needs to be removed and when? BIG question

//                    currentSinkList.add((ElkPort) edge.getTargets().getFirst());
//                    currentSourceList.add((ElkPort) edge.getSources().getFirst());
                }

                for (ElkEdge edge : currentPort.getOutgoingEdges()) {
//                    currentSinkList.add((ElkPort) edge.getTargets().getFirst());
//                    currentSourceList.add((ElkPort) edge.getSources().getFirst());
                }
            }
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
