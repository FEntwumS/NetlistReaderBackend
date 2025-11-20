package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
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
        newNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.of(SizeConstraint.NODE_LABELS, SizeConstraint.PORTS, SizeConstraint.PORT_LABELS));

        return newNode;
    }

    public static ElkNode createNewSimpleHierarchyNode(ElkNode parent) {
        ElkNode newNode = createNode(parent);
        newNode.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.of(SizeConstraint.NODE_LABELS, SizeConstraint.PORTS, SizeConstraint.PORT_LABELS));
        newNode.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE, PortLabelPlacement.NEXT_TO_PORT_IF_POSSIBLE));
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
        newPort.setProperty(CoreOptions.PORT_LABELS_PLACEMENT, EnumSet.of(PortLabelPlacement.INSIDE, PortLabelPlacement.NEXT_TO_PORT_IF_POSSIBLE));

        return newPort;
    }

    public static ElkEdge createNewSimpleHierarchyEdge(ElkConnectableShape sink, ElkConnectableShape source) {
        ElkEdge newEdge = createSimpleEdge(source, sink);

        return newEdge;
    }
}
