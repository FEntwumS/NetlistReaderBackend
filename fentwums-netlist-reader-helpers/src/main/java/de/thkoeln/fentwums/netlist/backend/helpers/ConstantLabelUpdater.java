package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.PortType;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConstantLabelUpdater {
	public static final Logger logger = LoggerFactory.getLogger(ConstantLabelUpdater.class);

	public static void updateLabels (ElkNode entityInstance, NetlistCreationSettings settings) {
		for (ElkNode childNode : List.of(entityInstance.getChildren().toArray(new ElkNode[0]))) {
			if (childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("HDL_ENTITY")
					|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("Constant driver")) {
				// Skip entity instances and constant drivers/sinks

				continue;
			}

			// Now go through the ports
			// Remove any attached constant drivers
			// And create the appropriate label

			for (int i = 0; i < childNode.getPorts().size(); i++) {
				ElkPort p = childNode.getPorts().get(i);

				// Skip all non-constant and non-western ports
				if (!(p.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT_SINGLE)
					|| p.getProperty(FEntwumSOptions.PORT_TYPE).equals(PortType.CONSTANT_MULTIPLE))
					|| !p.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)) {
					continue;
				}

				if (p.getIncomingEdges().size() > 1) {
					logger.atError().setMessage("Cell {} port {} is marked as constant but has {} incoming edges. Skipping...")
							.addArgument(childNode.getIdentifier())
							.addArgument(p.getLabels().getFirst().getText())
							.addArgument(p.getIncomingEdges().size())
							.log();

					continue;
				}

				if (p.getIncomingEdges().isEmpty()) {
					// TODO investigate if this is a structural error in the graph

					continue;
				}

				ElkEdge toRemoveEdge = p.getIncomingEdges().getFirst();
				ElkNode toRemoveNode = ((ElkPort) toRemoveEdge.getSources().getFirst()).getParent();
				String labelContents = toRemoveNode.getLabels().getFirst().getText();

				// Remove the edge
				entityInstance.getContainedEdges().remove(toRemoveEdge);
				p.getIncomingEdges().clear();

				// Remove the node
				entityInstance.getChildren().remove(toRemoveNode);

				// Add the new port
				ElkPort dummyEdgeSink = ElkElementCreator.createNewPort(childNode, PortSide.WEST);
				dummyEdgeSink.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

				// Move the port to the correct position so that model-based port ordering works as expected
				childNode.getPorts().move(i, dummyEdgeSink);

				// Add the dummy edge
				ElkEdge dummyEdge = ElkElementCreator.createNewEdge(dummyEdgeSink, p);
				dummyEdge.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

				// Add the label
				ElkLabel constLabel = ElkElementCreator.createNewEdgeLabel(labelContents, dummyEdge, settings);
				constLabel.setDimensions(constLabel.getWidth() - 10.0d, 0.1d);

				// Modify the ports
				p.setDimensions(10.0d, 0.2d);
				dummyEdgeSink.setDimensions(10.0d, 0.2d);
				p.setProperty(CoreOptions.PORT_ANCHOR, new KVector(10.0, 0.2));
				dummyEdgeSink.setProperty(CoreOptions.PORT_ANCHOR, new KVector(10.0, 0.2));

				if (childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("SPLIT_CONTAINER")
						|| childNode.getProperty(FEntwumSOptions.CELL_TYPE).equals("AGG_CONTAINER")) {
					dummyEdgeSink.setY(p.getY() + 10.0d);
				} else {
					// Update label height for port label of cell
					// This ensures consistent alignment of all port labels, both on constant and non-constant ports
					p.getLabels().getFirst().setHeight(0.2d);
				}
			}

		}
	}
}
