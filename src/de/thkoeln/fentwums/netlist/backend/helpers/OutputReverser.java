package de.thkoeln.fentwums.netlist.backend.helpers;

import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutputReverser {
    public OutputReverser() {}

    public void reversePorts(ElkNode node) {
        ArrayList<ElkPort> temp = new ArrayList<>(node.getPorts());

        Collections.reverse(temp);

        node.getPorts().clear();
        node.getPorts().addAll(temp);

        for (ElkNode child : node.getChildren()) {
            reversePorts(child);
        }
    }
}
