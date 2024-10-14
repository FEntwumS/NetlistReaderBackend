package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchyTree;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.eclipse.elk.graph.ElkNode;

import java.util.ArrayList;
import java.util.HashMap;

public class CellHandler {
    public CellHandler() {}

    public void createCells(HashMap<String, Object> cells, String modulename, ElkNode toplevel,
                                                ArrayList<SignalTree> signalTreeList, HierarchyTree hierarchyTree) {
        System.out.println(hierarchyTree.getRoot().getNode());
    }
}
