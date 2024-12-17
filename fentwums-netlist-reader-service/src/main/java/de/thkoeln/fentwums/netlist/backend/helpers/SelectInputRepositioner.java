package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

/**
 * Helper class that repositions the Select Inputs of various cells
 */
public class SelectInputRepositioner {

	/**
	 * Reposition the select inputs on currentGraphNode and its children. The ports will be moved to the south side
	 *
	 * @param currentGraphNode The starting node
	 */
	public void repositionSelect(ElkNode currentGraphNode) {
		for (ElkPort port : currentGraphNode.getPorts()) {
			if (port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && port.getProperty(FEntwumSOptions.PORT_GROUP_NAME).equals("S")
					&& port.getParent().getProperty(FEntwumSOptions.CELL_TYPE).matches("pmux|mux")) {
				port.setProperty(CoreOptions.PORT_SIDE, PortSide.SOUTH);
			}
		}

		for (ElkNode node : currentGraphNode.getChildren()) {
			repositionSelect(node);
		}
	}
}
