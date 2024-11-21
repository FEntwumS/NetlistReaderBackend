package de.thkoeln.fentwums.netlist.backend.helpers;

import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutputReverser {
    public OutputReverser() {}

    public void reversePorts(ElkNode node) {
        int index = node.getPorts().size() - 1;

        for (ElkPort port : node.getPorts()) {
            if (port.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
                port.setProperty(CoreOptions.PORT_INDEX, index);
            }
            index--;
        }

        for (ElkNode child : node.getChildren()) {
            reversePorts(child);
        }
    }
}
