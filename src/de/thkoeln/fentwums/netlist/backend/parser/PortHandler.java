package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.SignalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
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

    @SuppressWarnings("unchecked")
    public HashMap<Integer, SignalTree> createPorts(HashMap<String, Object> ports, String modulename,
                                             ElkNode toplevel) {
        HashMap<Integer, SignalTree> signalMap = new HashMap<>(ports.keySet().size());
        HashMap<String, Object> currentPort;
        ArrayList<Object> currentPortDrivers;
        int currentPortDriverIndex;

        HashMap<String, ElkNode> constantNodes = new HashMap<>();
        ElkNode constTarget;
        ElkPort source, sink;

        for(String portname : ports.keySet()) {
            currentPort = (HashMap<String, Object>) ports.get(portname);
            currentPortDrivers = (ArrayList<Object>) currentPort.get("bits");

            // TODO: Update
            // Not always accurate, look at offset and MSB attributes
            currentPortDriverIndex = 0;

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
                } else {
                    portDirection = "output";
                }

                ElkPort toplevelPort = createPort(toplevel);
                toplevelPort.setProperty(CoreOptions.PORT_SIDE, side);
                toplevelPort.setDimensions(10d, 10d);
                toplevelPort.setIdentifier(modulename + driver);

                // Add label to port
                ElkLabel toplevelPortLabel =
                        createLabel(portname + (currentPortDrivers.size() == 1 ? "" : " [" + currentPortDriverIndex +
                                        "]"), toplevelPort);
                // HACK
                // TODO find better solution
                //
                // Why is this necessary?
                // This hack was introduced when no frontend for the backend existed and the live demo version of
                // elkjs was used to display the generated graph. For whatever reason the labels would be crossed by
                // edges (most likely because the labels weren't being layouted). THis essentially declares a worst
                // case layout to make the port labels readable
                toplevelPortLabel.setDimensions(toplevelPortLabel.getText().length() * 7 + 1, 10);

                // If the port has a constant driver (or is a constant driver), a source (or sink) node needs to be
                // created

                if (driver instanceof Integer) {
                    signalMap.put((int) driver, createSignalTree((int) driver, portname, modulename, toplevelPort));

                    toplevelPort.setIdentifier(driver.toString());
                } else {
                    if(constantNodes.containsKey(driver + portDirection)) {
                        constTarget = constantNodes.get(driver + portDirection);
                    } else {
                        side = side == PortSide.WEST ? PortSide.EAST : PortSide.WEST;

                        constTarget = createNode(toplevel.getParent());
                        constTarget.setDimensions(20d, 20d);
                        constTarget.setProperty(CoreOptions.NODE_LABELS_PLACEMENT,
                                EnumSet.of(NodeLabelPlacement.H_CENTER, NodeLabelPlacement.V_CENTER, NodeLabelPlacement.INSIDE));

                        ElkLabel constTargetLabel = createLabel((String) driver, constTarget);
                        constTargetLabel.setDimensions(constTargetLabel.getText().length() * 7 + 1, 10);

                        ElkPort constTargetPort = createPort(constTarget);
                        constTargetPort.setProperty(CoreOptions.PORT_SIDE, side);
                        constTargetPort.setDimensions(10d, 10d);

                        constantNodes.put(driver + portDirection, constTarget);
                    }

                    if (portDirection.equals("input")) {
                        source = constTarget.getPorts().getFirst();
                        sink = toplevelPort;
                    } else {
                        source = toplevelPort;
                        sink = constTarget.getPorts().getFirst();
                    }

                    ElkEdge constantEdge = createSimpleEdge(source, sink);
                    ElkLabel constantLabel =  createLabel((String) driver, constantEdge);
                    constantLabel.setDimensions(constantLabel.getText().length() * 7 + 1, 10);
                    constantLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.TAIL);
                }

                currentPortDriverIndex++;
            }
        }
        return signalMap;
    }

    public SignalTree createSignalTree(int port, String portname, String modulename, ElkPort sPort) {
        SignalTree tree = new SignalTree();
        tree.setSId(port);
        SignalNode rootNode = new SignalNode("root", null, new HashMap<String, SignalNode>(), null,
                new HashMap<String, SignalNode>(), new HashMap<String, SignalNode>(), false, null);

        tree.setHRoot(rootNode);
        rootNode.setSVisited(false);

        SignalNode toplevelNode = new SignalNode(modulename, rootNode, new HashMap<String, SignalNode>(), null,
                new HashMap<String, SignalNode>(), new HashMap<String, SignalNode>(), false, sPort);

        toplevelNode.setSVisited(true);

        if (sPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.WEST) {
            toplevelNode.setIsSource(true);
            tree.setSRoot(toplevelNode);
        }

        return tree;
    }
}
