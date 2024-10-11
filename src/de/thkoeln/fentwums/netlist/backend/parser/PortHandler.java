package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.checkerframework.checker.units.qual.N;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.NodeLabelPlacement;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;


public class PortHandler {
    public PortHandler() {}

    public ArrayList<SignalTree> createPorts(HashMap<String, Object> ports, String modulename,
                                             ElkNode toplevel) {
        ArrayList<SignalTree> treeList = new ArrayList<>(ports.keySet().size());
        HashMap<String, Object> currentPort;
        ArrayList currentPortDrivers;
        int currentPortDriverIndex;

        for(String portname : ports.keySet()) {
            currentPort = (HashMap<String, Object>) ports.get(portname);
            currentPortDrivers = (ArrayList) currentPort.get("bits");

            // Not always accurate, look at offset and MSB attributes
            currentPortDriverIndex = 1;

            for(Object driver: currentPortDrivers) {
                String portDirection = (String) currentPort.get("direction");
                PortSide side = PortSide.EAST;

                // Create port for toplevel entity

                /*
                 * Input ports on the left side (WEST), output ports on the right side (EAST)
                 * Buffer ports (inout) are treated as outputs
                 */
                if (portDirection.equals("input")) {
                    side = PortSide.WEST;
                }

                ElkPort toplevelPort = createPort(toplevel);
                toplevelPort.setProperty(CoreOptions.PORT_SIDE, side);
                toplevelPort.setDimensions(10d, 10d);

                // Add label to port
                ElkLabel toplevelPortLabel = createLabel(portname + " [" + currentPortDriverIndex + "]", toplevelPort);
                // HACK
                // TODO find better solution
                //
                // Why is this necessary?
                // This hack was introduced when no frontend for the backend existed and the live demo version of
                // elkjs was used to display the generated graph. For whatever reason the labels would be crossed by
                // edges (most likely because the labels weren't being layouted). THis essentially declares a worst
                // case layout to make the port labels readable
                toplevelPortLabel.setDimensions(toplevelPortLabel.getText().length() * 7, 15);

                // If the port has a constant driver (or is a constant driver), a source (or sink) node needs to be
                // created

                if (!(driver instanceof Integer)) {
                    ElkNode outsideNode = createNode(toplevel.getParent());
                    outsideNode.setDimensions(20d, 20d);
                    ElkLabel outsideNodeLabel = createLabel((String) driver, outsideNode);
                    outsideNode.setProperty(CoreOptions.NODE_LABELS_PLACEMENT,
                            EnumSet.of(NodeLabelPlacement.H_CENTER, NodeLabelPlacement.V_CENTER, NodeLabelPlacement.INSIDE));
                    outsideNodeLabel.setDimensions(7,15);

                    side = side == PortSide.WEST ? PortSide.EAST : PortSide.WEST;

                    ElkPort outsideNodePort = createPort(outsideNode);
                    outsideNodePort.setProperty(CoreOptions.PORT_SIDE, side);
                    outsideNodePort.setDimensions(10d, 10d);

                    ElkPort source, sink;

                    if (portDirection.equals("input")) {
                        source = outsideNodePort;
                        sink = toplevelPort;
                    } else {
                        source = toplevelPort;
                        sink = outsideNodePort;
                    }

                    ElkEdge constantEdge = createSimpleEdge(source, sink);
                    ElkLabel constantLabel =  createLabel((String) driver, constantEdge);
                    constantLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.TAIL);
                }

                currentPortDriverIndex++;
            }
        }
        return treeList;
    }

    public SignalTree createSignalTree(HashMap<String, Object> port, String portname, String modulename) {
        SignalTree tree = new SignalTree();

        ArrayList portDrivers = (ArrayList) port.get("bits");

        tree.setSId((Integer) portDrivers.getFirst());

        // TODO Implement function lol

        return null;
    }
}
