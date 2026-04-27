package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.BundleRange;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalAgg;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalSplit;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import org.eclipse.elk.alg.layered.options.*;
import org.eclipse.elk.core.data.LayoutAlgorithmData;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.math.KVectorChain;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.impl.ElkGraphFactoryImpl;

import java.util.*;

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
		newNode.setProperty(CoreOptions.PARTITIONING_ACTIVATE, true);
		newNode.setProperty(CoreOptions.SEPARATE_CONNECTED_COMPONENTS, false);

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
		newAggNode.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

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
		newSplitNode.setProperty(CoreOptions.PARTITIONING_ACTIVATE, true);
		newSplitNode.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

		return newSplitNode;
	}

	public static ElkPort createNewAggSplitPort(ElkNode parent, PortSide side) {
		ElkPort newAggSplitPort = createPort(parent);
		newAggSplitPort.setProperty(CoreOptions.PORT_SIDE, side);
		newAggSplitPort.setDimensions(10, 10);
		newAggSplitPort.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

		return newAggSplitPort;
	}

	public static SignalSplit createSignalSplit(ElkNode parent, ElkPort sourcePort, List<String> labelList, NetlistCreationSettings settings) {
		ElkGraphFactory graphFactory = ElkGraphFactoryImpl.init();

		ElkPort priorSouthPort = null, southPort = null, northPort = null;

		int neededOutputs = labelList.size();
		int indexOfInPort = Math.floorDiv(neededOutputs - 1, 2);

		double maxLabelWidth = labelList.stream().map(String::length).max(Integer::compareTo).get() * settings.getPortLabelFontSize() * 0.7 + 10;
		double verticalLabelSpace = settings.getEdgeLabelFontSize() + 3.0;

		List<ElkPort> outPorts = new ArrayList<>();
		ElkPort inPort = null;

		ElkNode containerNode = createNode(parent);
		containerNode.setIdentifier("container");
		containerNode.setProperty(FEntwumSOptions.CELL_TYPE, "SPLIT_CONTAINER");
		containerNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.SEPARATE_CHILDREN);
		containerNode.setProperty(CoreOptions.ALGORITHM, "fixed");
		containerNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_POS);
		LayoutAlgorithmData algorithmData = LayoutMetaDataService.getInstance().getAlgorithmDataBySuffix("fixed");
		containerNode.setProperty(CoreOptions.RESOLVED_ALGORITHM, algorithmData);
		containerNode.setDimensions(10.0 + maxLabelWidth, verticalLabelSpace + 30 * (neededOutputs - 1) + 15);
		containerNode.setWidth(10.0 + maxLabelWidth);
		containerNode.setProperty(CoreOptions.NODE_SIZE_FIXED_GRAPH_SIZE, true);
		containerNode.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

		for (int i = 0; i < neededOutputs; i++) {
			ElkNode newNode = createNewSplitNode(containerNode);
			String currentLabelContent = labelList.get(i);

			double y = verticalLabelSpace + 30 * i;
			double y_p = verticalLabelSpace + 30 * (i - 1);
			double y_n = verticalLabelSpace + 30 * (i + 1);
			double x_i = 10;
			double x_r = containerNode.getWidth();
			double x_l = 0.0d;

			newNode.setLocation(x_i, y);
			newNode.setDimensions(10.0, 10.0);

			ElkPort outPort = createNewAggSplitPort(newNode, PortSide.EAST);
			outPort.setLocation(0, 0);

			ElkLabel splitLabel = createNewLabel(currentLabelContent, outPort, settings.getPortLabelFontSize());
			splitLabel.setLocation(3.0, -splitLabel.getHeight());

			ElkPort exOutPort = createNewAggSplitPort(containerNode, PortSide.EAST);
			exOutPort.setLocation(x_r, y - 5.0);
			exOutPort.setProperty(CoreOptions.PORT_ANCHOR, new KVector(0, 5));

			ElkEdge exOutEdge = createNewEdge(exOutPort, outPort);
			exOutEdge.setProperty(LayeredOptions.PRIORITY_STRAIGHTNESS, 1000000);
			exOutEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
			exOutEdge.setProperty(FEntwumSOptions.NO_TIP, true);

			ElkEdgeSection exOutEdgeSec = graphFactory.createElkEdgeSection();
			exOutEdgeSec.setStartLocation(x_i, y);
			exOutEdgeSec.setEndLocation(x_r, y);
			exOutEdge.getSections().add(exOutEdgeSec);

			outPort = exOutPort;

			outPorts.add(outPort);

			if (i == 0) {
				// Top splitter, only add south conn port
				southPort = createNewAggSplitPort(newNode, PortSide.SOUTH);
				southPort.setLocation(0, 0);
			} else if (i == neededOutputs - 1) {
				// Bottom splitter, only create north port
				northPort = createNewAggSplitPort(newNode, PortSide.NORTH);
				northPort.setLocation(0, 0);
			} else {
				// Between splitter, create both south and north ports
				northPort = createNewAggSplitPort(newNode, PortSide.NORTH);
				northPort.setLocation(0, 0);
				southPort = createNewAggSplitPort(newNode, PortSide.SOUTH);
				southPort.setLocation(0, 0);
			}

			if (i == indexOfInPort) {
				// Create input to splitter
				inPort = createNewAggSplitPort(newNode, PortSide.WEST);
				inPort.setLocation(0, 0);
				ElkPort exInPort = createNewAggSplitPort(containerNode, PortSide.WEST);
				exInPort.setLocation(x_l, y - 5.0);
				exInPort.setProperty(CoreOptions.PORT_ANCHOR, new KVector(10, 5));

				ElkEdge exInEdge = createNewEdge(inPort, exInPort);
				exInEdge.setProperty(LayeredOptions.PRIORITY_STRAIGHTNESS, 1000000);
				exInEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
				exInEdge.setProperty(FEntwumSOptions.NO_TIP, true);

				ElkEdgeSection exInSec = graphFactory.createElkEdgeSection();
				exInSec.setStartLocation(x_l, y);
				exInSec.setEndLocation(x_i, y);
				exInEdge.getSections().add(exInSec);

				inPort = exInPort;
				newNode.setProperty(LayeredOptions.PARTITIONING_PARTITION, 2);
			}

			// Create distributing connections
			if (i > 0) {
				if (i <= indexOfInPort) {
					// north to prior south
					ElkEdge distEdge = createNewEdge(priorSouthPort, northPort);
					distEdge.setProperty(CoreOptions.NO_LAYOUT, true);
					distEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
					distEdge.setProperty(FEntwumSOptions.NO_TIP, true);
					distEdge.setProperty(FEntwumSOptions.USE_SQUARE_JUNCTIONS, true);

					ElkEdgeSection distEdgeSec = graphFactory.createElkEdgeSection();
					distEdgeSec.setStartLocation(x_i, y);
					distEdgeSec.setEndLocation(x_i, y_p);
					distEdge.getSections().add(distEdgeSec);

					KVectorChain junctionPoints = new KVectorChain();
					junctionPoints.add(x_i, y);
					distEdge.setProperty(CoreOptions.JUNCTION_POINTS, junctionPoints);
				} else {
					// prior south to north
					ElkEdge distEdge = createNewEdge(northPort, priorSouthPort);
					distEdge.setProperty(CoreOptions.NO_LAYOUT, true);
					distEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
					distEdge.setProperty(FEntwumSOptions.NO_TIP, true);
					distEdge.setProperty(FEntwumSOptions.USE_SQUARE_JUNCTIONS, true);

					ElkEdgeSection distEdgeSec = graphFactory.createElkEdgeSection();
					distEdgeSec.setStartLocation(x_i, y_p);
					distEdgeSec.setEndLocation(x_i, y);
					distEdge.getSections().add(distEdgeSec);

					KVectorChain junctionPoints = new KVectorChain();
					junctionPoints.add(x_i, y_p);
					distEdge.setProperty(CoreOptions.JUNCTION_POINTS, junctionPoints);
				}
			}

			priorSouthPort = southPort;
		}

		return new SignalSplit(inPort, outPorts);
	}

	public static SignalAgg createSignalAgg(ElkNode parent, ElkPort sourcePort, List<String> labelList, NetlistCreationSettings settings) {
		ElkGraphFactory graphFactory = ElkGraphFactoryImpl.init();

		ElkPort priorSouthPort = null, southPort = null, northPort = null;

		int neededOutputs = labelList.size();
		int indexOfInPort = Math.floorDiv(neededOutputs - 1, 2);

		double maxLabelWidth = labelList.stream().map(String::length).max(Integer::compareTo).get() * settings.getPortLabelFontSize() * 0.7 + 10;
		double verticalLabelSpace = settings.getEdgeLabelFontSize() + 3.0;

		List<ElkPort> inPorts = new ArrayList<>();
		ElkPort outPort = null;

		ElkNode containerNode = createNode(parent);
		containerNode.setIdentifier("container");
		containerNode.setProperty(FEntwumSOptions.CELL_TYPE, "AGG_CONTAINER");
		containerNode.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.SEPARATE_CHILDREN);
		containerNode.setProperty(CoreOptions.ALGORITHM, "fixed");
		containerNode.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_POS);
		LayoutAlgorithmData algorithmData = LayoutMetaDataService.getInstance().getAlgorithmDataBySuffix("fixed");
		containerNode.setProperty(CoreOptions.RESOLVED_ALGORITHM, algorithmData);
		containerNode.setDimensions(10.0 + maxLabelWidth, verticalLabelSpace + 30 * (neededOutputs - 1) + 15);
		containerNode.setWidth(10.0 + maxLabelWidth);
		containerNode.setProperty(CoreOptions.NODE_SIZE_FIXED_GRAPH_SIZE, true);
		containerNode.setProperty(FEntwumSOptions.SCAFFOLDING_ELEMENT, true);

		for (int i = 0; i < neededOutputs; i++) {
			ElkNode newNode = createNewAggNode(containerNode);
			String currentLabelContent = labelList.get(i);

			double y = verticalLabelSpace + 30 * i;
			double y_p = verticalLabelSpace + 30 * (i - 1);
			double y_n = verticalLabelSpace + 30 * (i + 1);
			double x_r = containerNode.getWidth();
			double x_i = x_r - 10;
			double x_l = 0.0d;


			newNode.setLocation(x_i, y);
			newNode.setDimensions(10.0, 10.0);

			ElkPort inPort = createNewAggSplitPort(newNode, PortSide.WEST);
			inPort.setLocation(0, 0);

			ElkLabel splitLabel = createNewLabel(currentLabelContent, inPort, settings.getPortLabelFontSize());
			splitLabel.setLocation(-splitLabel.getWidth() - 3.0, -splitLabel.getHeight());

			ElkPort exInPort = createNewAggSplitPort(containerNode, PortSide.WEST);
			exInPort.setLocation(x_l, y - 5.0);
			exInPort.setProperty(CoreOptions.PORT_ANCHOR, new KVector(10, 5));

			ElkEdge exInEdge = createNewEdge(inPort, exInPort);
			exInEdge.setProperty(LayeredOptions.PRIORITY_STRAIGHTNESS, 1000000);
			exInEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
			exInEdge.setProperty(FEntwumSOptions.NO_TIP, true);

			ElkEdgeSection exInEdgeSec = graphFactory.createElkEdgeSection();
			exInEdgeSec.setStartLocation(x_l, y);
			exInEdgeSec.setEndLocation(x_i, y);
			exInEdge.getSections().add(exInEdgeSec);

			inPorts.add(exInPort);

			if (i == 0) {
				// Top splitter, only add south conn port
				southPort = createNewAggSplitPort(newNode, PortSide.SOUTH);
				southPort.setLocation(0, 0);
			} else if (i == neededOutputs - 1) {
				// Bottom splitter, only create north port
				northPort = createNewAggSplitPort(newNode, PortSide.NORTH);
				northPort.setLocation(0, 0);
			} else {
				// Between splitter, create both south and north ports
				northPort = createNewAggSplitPort(newNode, PortSide.NORTH);
				northPort.setLocation(0, 0);
				southPort = createNewAggSplitPort(newNode, PortSide.SOUTH);
				southPort.setLocation(0, 0);
			}

			if (i == indexOfInPort) {
				// Create input to splitter
				outPort = createNewAggSplitPort(newNode, PortSide.EAST);
				outPort.setLocation(0, 0);
				ElkPort exOutPort = createNewAggSplitPort(containerNode, PortSide.EAST);
				exOutPort.setLocation(x_r, y - 5.0);
				exOutPort.setProperty(CoreOptions.PORT_ANCHOR, new KVector(0, 5));

				ElkEdge exOutEdge = createNewEdge(exOutPort, outPort);
				exOutEdge.setProperty(LayeredOptions.PRIORITY_STRAIGHTNESS, 1000000);
				exOutEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
				exOutEdge.setProperty(FEntwumSOptions.NO_TIP, true);

				ElkEdgeSection exOutEdgeSec = graphFactory.createElkEdgeSection();
				exOutEdgeSec.setStartLocation(x_i, y);
				exOutEdgeSec.setEndLocation(x_r, y);
				exOutEdge.getSections().add(exOutEdgeSec);

				outPort = exOutPort;

				newNode.setProperty(LayeredOptions.PARTITIONING_PARTITION, 2);
			}

			// Create distributing connections
			if (i > 0) {
				if (i <= indexOfInPort) {
					// prior south to north
					ElkEdge distEdge = createNewEdge(northPort, priorSouthPort);
					distEdge.setProperty(CoreOptions.NO_LAYOUT, true);
					distEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
					distEdge.setProperty(FEntwumSOptions.NO_TIP, true);
					distEdge.setProperty(FEntwumSOptions.USE_SQUARE_JUNCTIONS, true);

					ElkEdgeSection distEdgeSec = graphFactory.createElkEdgeSection();
					distEdgeSec.setStartLocation(x_i, y_p);
					distEdgeSec.setEndLocation(x_i, y);
					distEdge.getSections().add(distEdgeSec);

					KVectorChain junctionPoints = new KVectorChain();
					junctionPoints.add(x_i, y);
					distEdge.setProperty(CoreOptions.JUNCTION_POINTS, junctionPoints);
				} else {
					// north to prior south
					ElkEdge distEdge = createNewEdge(priorSouthPort, northPort);
					distEdge.setProperty(CoreOptions.NO_LAYOUT, true);
					distEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.BUNDLED);
					distEdge.setProperty(FEntwumSOptions.NO_TIP, true);
					distEdge.setProperty(FEntwumSOptions.USE_SQUARE_JUNCTIONS, true);


					ElkEdgeSection distEdgeSec = graphFactory.createElkEdgeSection();
					distEdgeSec.setStartLocation(x_i, y);
					distEdgeSec.setEndLocation(x_i, y_p);
					distEdge.getSections().add(distEdgeSec);

					KVectorChain junctionPoints = new KVectorChain();
					junctionPoints.add(x_i, y_p);
					distEdge.setProperty(CoreOptions.JUNCTION_POINTS, junctionPoints);
				}
			}

			priorSouthPort = southPort;
		}

		return new SignalAgg(outPort, inPorts);
	}
}
