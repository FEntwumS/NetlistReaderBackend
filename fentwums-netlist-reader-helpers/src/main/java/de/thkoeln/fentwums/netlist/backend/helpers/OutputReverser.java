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

/**
 * Utility class that reverses the output ports to improve the displayed layout by significantly reducing edge crossings
 */
public class OutputReverser {
	public OutputReverser() {}

	/**
	 * Reverses the output ports of <code>node</code> and assigns all of <code>node</code>'s ports the <code>PORT_INDEX</code> property
	 * @param node The node whose outputs are to be reversed
	 */
	public void reversePorts(ElkNode node) {
		int index = node.getPorts().size() - 1;

		for (ElkPort port : node.getPorts()) {
			if (port.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
				port.setProperty(CoreOptions.PORT_INDEX, index);
			} else {
				// count the other way to preserve the order
				port.setProperty(CoreOptions.PORT_INDEX, node.getPorts().size() + (node.getPorts().size() - index));
			}
			index--;
		}

		for (ElkNode child : node.getChildren()) {
			reversePorts(child);
		}
	}
}
