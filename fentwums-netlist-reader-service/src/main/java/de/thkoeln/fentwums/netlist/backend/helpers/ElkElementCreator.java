package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.options.SignalType;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.*;

import java.util.EnumSet;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

/**
 * Utility class for creating elk elements. Provides creation methods for all currently used elk elements and sets
 * the relevant options to reduce bloat
 */
public class ElkElementCreator {
	public ElkElementCreator() {
	}

	/**
	 * Creates a new child node with standardised options
	 *
	 * @param parent     The parent node of the node to be created
	 * @param identifier The identifier of the node to be created
	 * @return The created child node
	 */
	public static ElkNode createNewNode(ElkNode parent, String identifier) {
		ElkNode newNode = createNode(parent);
		newNode.setIdentifier(identifier);
		newNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
		newNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
		newNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
				NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));
		newNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT,
				EnumSet.of(PortLabelPlacement.INSIDE));
		newNode.setProperty(CoreOptions.SPACING_LABEL_PORT_HORIZONTAL, 3.0d);
		newNode.setProperty(CoreOptions.SPACING_EDGE_LABEL, 3.0d);
		newNode.setProperty(CoreOptions.SPACING_LABEL_NODE, 4.0d);

		return newNode;
	}

	/**
	 * Creates a new ElkEdge from source to sink
	 *
	 * @param sink   The sink of the connection
	 * @param source The source of the connection
	 * @return The created edge
	 */
	public static ElkEdge createNewEdge(ElkConnectableShape sink, ElkConnectableShape source) {
		ElkEdge newEdge = createSimpleEdge(source, sink);

		// Every signal is single by default
		newEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);

		return newEdge;
	}

	/**
	 * Creates a new ElkPort
	 *
	 * @param parent The ElkNode to which the port is to be attached
	 * @param side   The side on which the port is to be attached
	 * @return the creted port
	 */
	public static ElkPort createNewPort(ElkNode parent, PortSide side) {
		ElkPort newPort = createPort(parent);
		newPort.setDimensions(10, 10);
		newPort.setProperty(CoreOptions.PORT_SIDE, side);
		newPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, "A");

		return newPort;
	}

	/**
	 * Creates a new ElkLabel with the given font size and automatically sets its dimensions.
	 *
	 * @param content	The content to be displayed in the label
	 * @param parent	The element to which the element is to be attached
	 * @param fontsize	The font size to be used
	 * @return	The created label
	 */
	public static ElkLabel createNewLabel(String content, ElkGraphElement parent, double fontsize) {
		if (parent.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)) {
			content += " ";
		}

		ElkLabel newLabel = createLabel(content, parent);

		// Since ELK does not know about the font size (and the font itself for that matter), the computed dimensions
		// are dependent on this hardcoded formula
		newLabel.setDimensions(content.length() * 5.75 + 1, fontsize);

		newLabel.setProperty(FEntwumSOptions.FONT_SIZE, fontsize);

		return newLabel;
	}

	/**
	 * Creates a new ElkLabel and automatically sets its dimensions.
	 *
	 * @param content The content to be displayed in the label
	 * @param parent  The element to which the label is to be attached
	 * @return The created label
	 */
	public static ElkLabel createNewLabel(String content, ElkGraphElement parent) {
		return createNewLabel(content, parent, 10.0d);
	}

	/**
	 * Creates an ElkNode that can be used as a constant driver
	 *
	 * @param parent The parent node
	 * @return The created node
	 */
	public static ElkNode createNewConstantDriver(ElkNode parent) {
		ElkNode newNode = createNode(parent);

		newNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
		newNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
		newNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT,
				EnumSet.of(NodeLabelPlacement.H_CENTER, NodeLabelPlacement.V_CENTER, NodeLabelPlacement.INSIDE));
		newNode.setProperty(CoreOptions.SPACING_LABEL_PORT_HORIZONTAL, 3.0d);
		newNode.setProperty(FEntwumSOptions.CELL_TYPE, "Constant driver");

		return newNode;
	}
}
