package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;
import org.eclipse.elk.alg.layered.options.PortType;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortLabelPlacement;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.HashMap;

import static org.eclipse.elk.graph.util.ElkGraphUtil.*;


public class PortHandler {
    private enum portDirection {
        IN,
        OUT,
        INOUT
    };

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

                if (driver instanceof Integer) {
                    treeList.add(createSignalTree(currentPort, portname, modulename));



                    if (portDirection.equals("input")) {
                        side = PortSide.WEST;
                    }

                    ElkPort p = createPort(toplevel);
                    p.setProperty(CoreOptions.PORT_SIDE, side);
                    createLabel(portname + currentPortDriverIndex, p);
                    p.setDimensions(10d, 10d);
                    p.setParent(toplevel);

                    System.out.println("Created port " + portname + "-" + currentPortDriverIndex);

                    currentPortDriverIndex++;

                } else {
                    // constant driver

                    //TODO:
                    // Create driver node
                    // Connect node to Port

                    ElkNode constDriverNode = createNode(toplevel.getParent());
                    createLabel((String) driver, constDriverNode);

                    if (!portDirection.equals("input")) {
                        side = PortSide.WEST;
                    }

                    ElkPort cDriverOutputPort = createPort(constDriverNode);
                    cDriverOutputPort.setProperty(CoreOptions.PORT_SIDE, side);
                    cDriverOutputPort.setDimensions(10d, 10d);
                    cDriverOutputPort.setParent(constDriverNode);

                    if (portDirection.equals("input")) {
                        side = PortSide.WEST;
                    }

                    ElkPort constInputPort = createPort(toplevel);
                    constInputPort.setProperty(CoreOptions.PORT_SIDE, side);
                    createLabel(portname + currentPortDriverIndex, constInputPort);
                    constInputPort.setDimensions(10d, 10d);
                    constInputPort.setParent(toplevel);

                    ElkEdge constDriverEdge = createSimpleEdge(cDriverOutputPort, constInputPort);
                    createLabel((String) driver, constDriverEdge);
                }
            }
        }
        return treeList;
    }

    public SignalTree createSignalTree(HashMap<String, Object> port, String portname, String modulename) {
        SignalTree tree = new SignalTree();

        ArrayList portDrivers = (ArrayList) port.get("bits");

        tree.setSId((Integer) portDrivers.getFirst());
        return null;
    }
}
