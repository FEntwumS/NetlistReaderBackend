package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.options.SignalType;
import org.eclipse.elk.core.options.*;
import org.eclipse.elk.graph.*;

import java.util.EnumSet;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;

public class ElkElementCreator {
    public ElkElementCreator() {}

    public ElkNode createNewNode(ElkNode parent, String identifier) {
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

    public ElkEdge createNewEdge(ElkConnectableShape sink, ElkConnectableShape source) {
        ElkEdge newEdge = createSimpleEdge(source, sink);

        // Every signal is single by default
        newEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.SINGLE);

        return newEdge;
    }

    public ElkPort createNewPort(ElkNode parent, PortSide side) {
        ElkPort newPort = createPort(parent);
        newPort.setDimensions(10, 10);
        newPort.setProperty(CoreOptions.PORT_SIDE, side);

        return newPort;
    }

    public ElkLabel createNewLabel(String content, ElkGraphElement parent) {
        if (parent.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)) {
            content += " ";
        }

        ElkLabel newLabel = createLabel(content, parent);
        newLabel.setDimensions(content.length() * 7 + 1, 10);

        return newLabel;
    }

    public ElkNode createNewConstantDriver(ElkNode parent) {
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
