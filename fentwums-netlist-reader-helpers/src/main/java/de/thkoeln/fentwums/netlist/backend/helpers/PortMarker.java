package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class PortMarker {
	private static Logger logger = LoggerFactory.getLogger(PortMarker.class);

	public void mark(ElkNode graph, NetlistCreationSettings settings) {
		for (ElkPort port : new ArrayList<ElkPort>(graph.getPorts())) {
			markPort(port, settings);
		}

		for (ElkNode node : graph.getChildren()) {
			mark(node, settings);
		}
	}

	private void markPort(ElkPort port, NetlistCreationSettings settings) {
		if (port.getOutgoingEdges().isEmpty() && port.getIncomingEdges().isEmpty()) {
			if (settings.doShowUnconnectedPorts()) {
				port.setProperty(FEntwumSOptions.NOT_CONNECTED, true);
			} else {
				port.getParent().getPorts().remove(port);
			}
		}
	}
}
