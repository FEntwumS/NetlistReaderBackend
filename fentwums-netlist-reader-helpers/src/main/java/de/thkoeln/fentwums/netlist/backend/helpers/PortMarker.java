package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMarker {
	private static Logger logger = LoggerFactory.getLogger(PortMarker.class);

	public void mark(ElkNode graph) {
		for (ElkPort port : graph.getPorts()) {
			markPort(port);
		}

		for (ElkNode node : graph.getChildren()) {
			mark(node);
		}
	}

	private void markPort(ElkPort port) {
		if (port.getOutgoingEdges().isEmpty() && port.getIncomingEdges().isEmpty()) {
			port.setProperty(FEntwumSOptions.NOT_CONNECTED, true);
		}
	}
}
