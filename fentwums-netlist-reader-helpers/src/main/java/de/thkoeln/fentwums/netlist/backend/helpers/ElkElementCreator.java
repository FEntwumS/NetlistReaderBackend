package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.BundleRange;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import org.eclipse.elk.alg.layered.options.FixedAlignment;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.math.KVector;
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
				EnumSet.of(PortLabelPlacement.INSIDE, PortLabelPlacement.NEXT_TO_PORT_IF_POSSIBLE));
		newNode.setProperty(CoreOptions.SPACING_LABEL_PORT_HORIZONTAL, 1.0d);
		newNode.setProperty(CoreOptions.SPACING_LABEL_PORT_VERTICAL, 1.0d);
		newNode.setProperty(CoreOptions.SPACING_EDGE_LABEL, 3.0d);
		newNode.setProperty(CoreOptions.SPACING_LABEL_NODE, 3.0d);
		newNode.setProperty(CoreOptions.SPACING_EDGE_EDGE, 10.0d);
		newNode.setProperty(CoreOptions.RANDOM_SEED, 1);
		newNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
		newNode.setProperty(LayeredOptions.NODE_PLACEMENT_BK_FIXED_ALIGNMENT, FixedAlignment.BALANCED);

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
		newEdge.setProperty(CoreOptions.EDGE_THICKNESS, 0.0d);

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
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the element is to be attached
	 * @param fontsize The font size to be used
	 * @return The created label
	 */
	public static ElkLabel createNewLabel(String content, ElkGraphElement parent, double fontsize) {
		content = content.trim();

		ElkLabel newLabel = createLabel(content, parent);

		// Since ELK does not know about the font size (and the font itself for that matter), the computed dimensions
		// are dependent on this hardcoded formula
		newLabel.setDimensions(content.length() * 0.7d * fontsize + 3, fontsize + 5);

		newLabel.setProperty(FEntwumSOptions.FONT_SIZE, fontsize);

		return newLabel;
	}

	/**
	 * Creates a new normal-sized ElkLabel and automatically sets its dimensions.
	 *
	 * @param content The content to be displayed in the label
	 * @param parent  The element to which the label is to be attached
	 * @return The created label
	 */
	@Deprecated
	public static ElkLabel createNewLabel(String content, ElkGraphElement parent) {
		return createNewLabel(content, parent, 10.0d);
	}

	/**
	 * Creates a label for elk nodes and sets the spacing to an appropriate value
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param fontsize The font size to be used
	 * @return The created label
	 */
	private static ElkLabel createNodeLabel(String content, ElkGraphElement parent, double fontsize) {
		parent.setProperty(CoreOptions.SPACING_LABEL_NODE, (10 / (-(fontsize * 0.0005 + 1))) + 12);

		return createNewLabel(content, parent, fontsize);
	}

	/**
	 * Creates a new title-sized ElkLabel and automatically sets its dimensions
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param settings The settings that should be used during the labels creation
	 * @return The created label
	 */
	public static ElkLabel createNewEntityLabel(String content, ElkGraphElement parent,
												NetlistCreationSettings settings) {
		if (settings == null) {
			return createNodeLabel(content, parent, 25.0d);
		} else {
			return createNodeLabel(content, parent, settings.getEntityLabelFontSize());
		}
	}

	/**
	 * Creates a new subtitle-sized ElkLabel and automatically sets its dimensions
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param settings The settings that should be used during the labels creation
	 * @return The created label
	 */
	public static ElkLabel createNewCellLabel(String content, ElkGraphElement parent,
											  NetlistCreationSettings settings) {
		if (settings == null) {
			return createNodeLabel(content, parent, 15.0d);
		} else {
			return createNodeLabel(content, parent, settings.getCellLabelFontSize());
		}
	}

	/**
	 * Creates a new subtitle-sized ElkLabel and automatically sets its dimensions
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param settings The settings that should be used during the labels creation
	 * @return The created label
	 */
	public static ElkLabel createNewModuleLabel(String content, ElkGraphElement parent,
												NetlistCreationSettings settings) {
		ElkLabel newLabel;

		if (settings == null) {
			newLabel = createNodeLabel(content, parent, 15.0d);
		} else {
			newLabel = createNodeLabel(content, parent, settings.getCellLabelFontSize());
		}

		newLabel.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_CENTER,
				NodeLabelPlacement.V_BOTTOM, NodeLabelPlacement.OUTSIDE));

		return newLabel;
	}

	/**
	 * Creates a new normal-sized ElkLabel and automatically sets its dimensions
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param settings The settings that should be used during the labels creation
	 * @return The created label
	 */
	public static ElkLabel createNewEdgeLabel(String content, ElkGraphElement parent,
											  NetlistCreationSettings settings) {
		if (settings == null) {
			return createNewLabel(content, parent, 10.0d);
		} else {
			return createNewLabel(content, parent, settings.getEdgeLabelFontSize());
		}
	}

	/**
	 * Creates a new normal-sized ElkLabel and automatically sets its dimensions
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param settings The settings that should be used during the labels creation
	 * @return The created label
	 */
	public static ElkLabel createNewPortLabel(String content, ElkGraphElement parent,
											  NetlistCreationSettings settings) {
		if (settings == null) {
			return createNewLabel(content, parent, 10.0d);
		} else {
			return createNewLabel(content, parent, settings.getPortLabelFontSize());
		}
	}

	/**
	 * Creates a new normal-sized ElkLabel and automatically sets its dimensions
	 *
	 * @param content  The content to be displayed in the label
	 * @param parent   The element to which the label is to be attached
	 * @param settings The settings that should be used during the labels creation
	 * @return The created label
	 */
	public static ElkLabel createNewConstantDriverLabel(String content, ElkGraphElement parent,
														NetlistCreationSettings settings) {
		if (settings == null) {
			return createNewLabel(content, parent, 10.0d);
		} else {
			return createNewLabel(content, parent, settings.getPortLabelFontSize());
		}
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

	public static ElkNode createNewHierarchyContainer(ElkNode parent) {
		ElkNode newNode = createNode(parent);

		newNode.setProperty(CoreOptions.ALGORITHM, "layered");
		newNode.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
		newNode.setProperty(CoreOptions.PARTITIONING_ACTIVATE, true);
		newNode.setProperty(CoreOptions.EXPAND_NODES, true);
		newNode.setProperty(CoreOptions.SPACING_NODE_NODE, 0.0d);
		newNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
		newNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.of(SizeConstraint.NODE_LABELS,
				SizeConstraint.PORTS, SizeConstraint.PORT_LABELS));

		return newNode;
	}

	public static ElkNode createNewSimpleHierarchyNode(ElkNode parent) {
		ElkNode newNode = createNode(parent);
		newNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.of(SizeConstraint.NODE_LABELS,
				SizeConstraint.PORTS, SizeConstraint.PORT_LABELS));
		newNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE,
				PortLabelPlacement.NEXT_TO_PORT_IF_POSSIBLE));
		newNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
		newNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
		newNode.setProperty(CoreOptions.SPACING_PORT_PORT, 0.0d);
		newNode.setProperty(CoreOptions.SPACING_LABEL_NODE, 0.0d);
		newNode.setProperty(CoreOptions.SPACING_LABEL_LABEL, 2.0d);
		newNode.setProperty(CoreOptions.SPACING_LABEL_PORT_VERTICAL, 0.0d);
		newNode.setProperty(CoreOptions.SPACING_COMPONENT_COMPONENT, 0.0d);

		return newNode;
	}

	public static ElkLabel createNewSimpleHierarchyLabel(ElkConnectableShape parent, String content) {
		ElkLabel newLabel = createNewLabel(content, parent, 10.0d);

		newLabel.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_LEFT,
				NodeLabelPlacement.V_CENTER, NodeLabelPlacement.INSIDE));

		return newLabel;
	}

	public static ElkLabel createNewTitleHierarchyLabel(ElkConnectableShape parent, String content) {
		ElkLabel newLabel = createNewLabel(content, parent, 15.0d);

		newLabel.setProperty(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(NodeLabelPlacement.H_LEFT,
				NodeLabelPlacement.V_TOP, NodeLabelPlacement.OUTSIDE));

		return newLabel;
	}

	public static ElkPort createNewSimpleHierarchyPort(ElkNode parent, double width, double height) {
		ElkPort newPort = createPort(parent);
		newPort.setDimensions(width, height);
		newPort.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);
		newPort.setProperty(CoreOptions.PORT_BORDER_OFFSET, -width - 2);
		newPort.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE,
				PortLabelPlacement.NEXT_TO_PORT_IF_POSSIBLE));

		return newPort;
	}

	public static ElkEdge createNewSimpleHierarchyEdge(ElkConnectableShape sink, ElkConnectableShape source) {
		ElkEdge newEdge = createSimpleEdge(source, sink);

		return newEdge;
	}

	// Insertions for splitting/aggregating nodes

	public static ElkNode createNewAggNode(ElkNode parent) {
		ElkNode newAggNode = createNode(parent);
		newAggNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
		newAggNode.setProperty(CoreOptions.NODE_SIZE_MINIMUM, new KVector(4, 4));
		newAggNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.OUTSIDE,
				PortLabelPlacement.ALWAYS_SAME_SIDE));
		newAggNode.setProperty(CoreOptions.SPACING_LABEL_PORT_HORIZONTAL, 1.0d);
		newAggNode.setProperty(CoreOptions.SPACING_LABEL_PORT_VERTICAL, 1.0d);
		newAggNode.setProperty(CoreOptions.SPACING_EDGE_LABEL, 3.0d);
		newAggNode.setProperty(CoreOptions.SPACING_LABEL_NODE, 3.0d);
		newAggNode.setProperty(CoreOptions.SPACING_EDGE_EDGE, 10.0d);
		newAggNode.setProperty(CoreOptions.RANDOM_SEED, 1);
		newAggNode.setProperty(FEntwumSOptions.CELL_TYPE, "AGG_NODE");

		return newAggNode;
	}

	public static ElkNode createNewSplitNode(ElkNode parent) {
		ElkNode newSplitNode = createNode(parent);
		newSplitNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
		newSplitNode.setProperty(CoreOptions.NODE_SIZE_MINIMUM, new KVector(4, 4));
		newSplitNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.OUTSIDE,
				PortLabelPlacement.ALWAYS_OTHER_SAME_SIDE));
		newSplitNode.setProperty(CoreOptions.SPACING_LABEL_PORT_HORIZONTAL, 1.0d);
		newSplitNode.setProperty(CoreOptions.SPACING_LABEL_PORT_VERTICAL, 1.0d);
		newSplitNode.setProperty(CoreOptions.SPACING_EDGE_LABEL, 3.0d);
		newSplitNode.setProperty(CoreOptions.SPACING_LABEL_NODE, 3.0d);
		newSplitNode.setProperty(CoreOptions.SPACING_EDGE_EDGE, 10.0d);
		newSplitNode.setProperty(CoreOptions.RANDOM_SEED, 1);
		newSplitNode.setProperty(FEntwumSOptions.CELL_TYPE, "SPLIT_NODE");

		return newSplitNode;
	}

	public static ElkPort createNewAggSplitPort(ElkNode parent, PortSide side) {
		ElkPort newAggSplitPort = createPort(parent);
		newAggSplitPort.setProperty(CoreOptions.PORT_SIDE, side);
		newAggSplitPort.setDimensions(0, 0);

		return newAggSplitPort;
	}

	public static ElkEdge createNewAggSplitEdge(ElkPort source, ElkPort sink, BundleRange indexes) {
		ElkEdge newAggSplitEdge = createNewEdge(sink, source);

		newAggSplitEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);

		return newAggSplitEdge;
	}

	public static ElkLabel createNewAggSplitLabel(ElkGraphElement parent, BundleRange indexes, String netname,
												  NetlistCreationSettings settings, boolean msbFirst) {
		double fontsize;

		if (settings == null) {
			fontsize = 10.0d;
		} else {
			fontsize = settings.getPortLabelFontSize();
		}

		StringBuilder builder = new StringBuilder();

		if (netname != null && !netname.isEmpty()) {
			builder.append(netname);
			builder.append(" ");
		}

		builder.append("[");

		if (msbFirst) {
			builder.append(indexes.containedRange().lower());
			builder.append(":");
			builder.append(indexes.containedRange().upper());
		} else {
			builder.append(indexes.containedRange().upper());
			builder.append(":");
			builder.append(indexes.containedRange().lower());
		}

		builder.append("]");

		String content = builder.toString();

		ElkLabel newAggSplitLabel = createLabel(content, parent);
		newAggSplitLabel.setDimensions(content.length() * 0.7d * fontsize + 3, fontsize + 5);
		newAggSplitLabel.setProperty(FEntwumSOptions.FONT_SIZE, fontsize);

		return newAggSplitLabel;
	}

	public static ElkLabel createNewSingleLabel(ElkGraphElement parent, BundleRange singleIndexes, String netname,
												NetlistCreationSettings settings, boolean msbFirst) {
		double fontsize;

		if (settings == null) {
			fontsize = 10.0d;
		} else {
			fontsize = settings.getPortLabelFontSize();
		}

		StringBuilder builder = new StringBuilder();

		if (netname != null && !netname.isEmpty()) {
			builder.append(netname);
			builder.append(" ");
		}

		builder.append("[");
		builder.append(singleIndexes.containedRange().upper());
		builder.append("]");

		String content = builder.toString();

		ElkLabel newSingleLabel = createLabel(content, parent);
		newSingleLabel.setDimensions(content.length() * 0.7d * fontsize + 3, fontsize + 5);
		newSingleLabel.setProperty(FEntwumSOptions.FONT_SIZE, fontsize);

		return newSingleLabel;
	}

	public static ElkNode insertVectorSplitNode(ElkNode parent, ElkPort sink1, ElkPort sink2, BundleRange bundle1,
												BundleRange bundle2, String netname, boolean msbFirst,
												NetlistCreationSettings settings) {
		ElkNode newSplitNode = createNewSplitNode(parent);

		// Create ports
		ElkPort inPort = createNewAggSplitPort(newSplitNode, PortSide.WEST);

		ElkPort outPort1 = createNewAggSplitPort(newSplitNode, PortSide.NORTH);
		ElkPort outPort2 = createNewAggSplitPort(newSplitNode, PortSide.SOUTH);

		// Create edges
		ElkEdge outEdge1 = createNewAggSplitEdge(outPort1, sink1, bundle1);
		ElkEdge outEdge2 = createNewAggSplitEdge(outPort2, sink2, bundle2);

		// Create labels for split of edges
		ElkLabel outLabel1 = createNewAggSplitLabel(outPort1, bundle1, netname, settings, msbFirst);
		ElkLabel outLabel2 = createNewAggSplitLabel(outPort2, bundle2, netname, settings, msbFirst);

		return newSplitNode;
	}

	public static ElkNode insertVectorAggNode(ElkNode parent, ElkPort source1, ElkPort source2, BundleRange bundle1,
											  BundleRange bundle2, String netname, boolean msbFirst,
											  NetlistCreationSettings settings) {
		ElkNode newAggNode = createNewAggNode(parent);

		// Create ports
		ElkPort outPort = createNewAggSplitPort(newAggNode, PortSide.EAST);

		ElkPort inPort1 = createNewAggSplitPort(newAggNode, PortSide.NORTH);
		ElkPort inPort2 = createNewAggSplitPort(newAggNode, PortSide.SOUTH);

		// Create edges
		ElkEdge inEdge1 = createNewAggSplitEdge(source1, inPort1, bundle1);
		ElkEdge inEdge2 = createNewAggSplitEdge(source2, inPort2, bundle2);

		// Create labels for pre-aggregated edges
		ElkLabel inLabel1 = createNewAggSplitLabel(inPort1, bundle1, netname, settings, msbFirst);
		ElkLabel inLabel2 = createNewAggSplitLabel(inPort2, bundle2, netname, settings, msbFirst);

		return newAggNode;
	}

	public static ElkNode insertSingleSplitNode(ElkNode parent, ElkPort vectorSink, ElkPort singleSink,
												BundleRange vectorBundle, BundleRange singleBundle, String netname,
												boolean msbFirst, NetlistCreationSettings settings) {
		ElkNode newSplitNode = createNewSplitNode(parent);

		// Create ports
		ElkPort inPort = createNewAggSplitPort(newSplitNode, PortSide.WEST);

		ElkPort vectorOutPort = createNewAggSplitPort(newSplitNode, PortSide.EAST);
		ElkPort singleOutPort = createNewAggSplitPort(newSplitNode, PortSide.SOUTH);

		// Create edges
		ElkEdge vectorEdge = createNewAggSplitEdge(vectorOutPort, vectorSink, vectorBundle);
		ElkEdge singleEdge = createNewEdge(singleSink, singleOutPort);

		// Create labels
		ElkLabel vectorLabel = createNewAggSplitLabel(vectorOutPort, vectorBundle, netname, settings, msbFirst);
		ElkLabel singleLabel = createNewSingleLabel(singleOutPort, singleBundle, netname, settings, msbFirst);

		return newSplitNode;
	}

	public static ElkNode insertSingleAggNode(ElkNode parent, ElkPort vectorSource, ElkPort singleSource,
											  BundleRange vectorBundle, BundleRange singleBundle, String netname,
											  boolean msbFirst, NetlistCreationSettings settings) {
		ElkNode newAggNode = createNewAggNode(parent);

		// Create ports
		ElkPort vectorInPort = createNewAggSplitPort(newAggNode, PortSide.WEST);
		ElkPort singleInPort = createNewAggSplitPort(newAggNode, PortSide.SOUTH);

		// Create edges
		ElkEdge vectorEdge = createNewAggSplitEdge(vectorSource, vectorInPort, vectorBundle);
		ElkEdge singleEdge = createNewEdge(singleInPort, singleSource);

		// Create labels
		ElkLabel vectorLabel = createNewAggSplitLabel(vectorInPort, vectorBundle, netname, settings, msbFirst);
		ElkLabel singleLabel = createNewSingleLabel(singleInPort, singleBundle, netname, settings, msbFirst);

		return newAggNode;
	}

	public static ElkNode insertDoubleSingleSplitNode(ElkNode parent, ElkPort sink1, ElkPort sink2,
													  BundleRange singleBundle1, BundleRange singleBundle2,
													  String netname, boolean msbFirst,
													  NetlistCreationSettings settings) {
		ElkNode newSplitNode = createNewSplitNode(parent);

		// Create ports
		ElkPort inPort = createNewAggSplitPort(newSplitNode, PortSide.WEST);

		ElkPort outPort1 = createNewAggSplitPort(newSplitNode, PortSide.NORTH);
		ElkPort outPort2 = createNewAggSplitPort(newSplitNode, PortSide.SOUTH);

		// Create edges
		ElkEdge outEdge1 = createNewEdge(sink1, outPort1);
		ElkEdge outEdge2 = createNewEdge(sink2, outPort2);

		// Create labels
		ElkLabel label1 = createNewSingleLabel(outPort1, singleBundle1, netname, settings, msbFirst);
		ElkLabel label2 = createNewSingleLabel(outPort2, singleBundle2, netname, settings, msbFirst);

		return newSplitNode;
	}

	public static ElkNode insertDoubleSingleAggNode(ElkNode parent, ElkPort source1, ElkPort source2,
													BundleRange singleBundle1, BundleRange singleBundle2,
													String netname, boolean msbFirst,
													NetlistCreationSettings settings) {
		ElkNode newAggNode = createNewAggNode(parent);

		// Create ports
		ElkPort outPort = createNewAggSplitPort(newAggNode, PortSide.EAST);

		ElkPort inPort1 = createNewAggSplitPort(newAggNode, PortSide.NORTH);
		ElkPort inPort2 = createNewAggSplitPort(newAggNode, PortSide.SOUTH);

		// Create edges
		ElkEdge inEdge1 = createNewEdge(inPort1, source1);
		ElkEdge inEdge2 = createNewEdge(inPort2, source2);

		// Create labels
		ElkLabel label1 = createNewSingleLabel(inPort1, singleBundle1, netname, settings, msbFirst);
		ElkLabel label2 = createNewSingleLabel(inPort2, singleBundle2, netname, settings, msbFirst);

		return newAggNode;
	}

	public static ElkNode insertSplitNode(ElkNode parent, ElkPort sourcePort) {
		ElkNode newSplitNode = createNewSplitNode(parent);

		// Create ports
		ElkPort inPort = createNewAggSplitPort(newSplitNode, PortSide.WEST);

		ElkPort outPort1 = createNewAggSplitPort(newSplitNode, PortSide.NORTH);
		ElkPort outPort2 = createNewAggSplitPort(newSplitNode, PortSide.SOUTH);

		return newSplitNode;
	}
}
