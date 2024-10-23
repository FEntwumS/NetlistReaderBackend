package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;

import java.util.HashMap;

public class SignalBundler {
    private HashMap<Integer, SignalTree> treeMap;
    private HierarchyTree hierarchy;

    public SignalBundler() {}

    public void bundleSignalAt(String path) {}
    public void bundleSignalRecursively(SignalNode sNode, HierarchicalNode hNode) {}
    public void debundleSignalAt(String path) {}
    public void debundleSignalRecursively(SignalNode sNode, HierarchicalNode hNode) {}

    private SignalNode findSNode(String path) { return null;}
    private HierarchicalNode findHNode(String path) { return null; }
}
