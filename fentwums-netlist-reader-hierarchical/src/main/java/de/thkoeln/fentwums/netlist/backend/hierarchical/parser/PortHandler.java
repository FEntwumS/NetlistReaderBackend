package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;


import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalOccurences;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PortHandler {
private static Logger logger = LoggerFactory.getLogger(PortHandler.class);

    /**
     * Creates the input and output ports of the current entity
     *
     * @param currentNode The ElkNode representing the current entity
     */
    public void createPorts(HashMap<String, Object> netlist, ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps, ElkNode currentNode,
                            NetlistCreationSettings settings, String moduleType, String instancePath) {
        PortSide createdPortSide, oppositePortSide;
        HashMap<String, Object> currentPort, module, ports;
        int currentIndexInPort;
        ArrayList<Object> portDrivers;
        int signalIndex;
        String currentPortDirection;
        HashMap<String, ElkPort> constantDriverPortMap = new HashMap<>();
        HashMap<Integer, SignalOccurences> signalMap;

        if (signalMaps.containsKey(instancePath)) {
            signalMap = signalMaps.get(instancePath);
        } else {
            signalMap = new HashMap<>();
            signalMaps.put(instancePath, signalMap);
        }

        module = (HashMap<String, Object>) netlist.get(moduleType);

        if (module == null) {
            logger.atError().setMessage("Module {} not found in Netlist. Aborting...").addArgument(moduleType).log();
        }

        ports = (HashMap<String, Object>) netlist.get("ports");

        boolean isTopLevel = currentNode.getParent() != null && currentNode.getParent().getIdentifier().equals("root");

        for (String portname : ports.keySet()) {
            currentPort = (HashMap<String, Object>) ports.get(portname);

            currentPortDirection = (String) currentPort.get("direction");
            if (!currentPortDirection.equals("output")) {
                createdPortSide = PortSide.WEST;
                oppositePortSide = PortSide.EAST;
            } else {
                createdPortSide = PortSide.EAST;
                oppositePortSide = PortSide.WEST;
            }

            // set offset in port index
            if (currentPort.containsKey("offset")) {
                currentIndexInPort = Integer.parseInt(currentPort.get("offset").toString());
            } else {
                currentIndexInPort = 0;
            }

            portDrivers = (ArrayList<Object>) currentPort.get("bits");

            // reverse order for MSB-first ports
            if (currentPort.containsKey("upto")) {
                portDrivers = (ArrayList<Object>) portDrivers.reversed();
            }

            for (Object driver : portDrivers) {
                ElkPort newPort = ElkElementCreator.createNewPort(currentNode, createdPortSide);
                newPort.setProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP, currentIndexInPort);
                newPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);

                ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(portname + (portDrivers.size() == 1
                        ? "" : " [" + currentIndexInPort + "]"), newPort, settings);

                if (driver instanceof Integer) {
                    signalIndex = (Integer) driver;

                    if (signalMap.containsKey(signalIndex)) {
                        insertPortIntoMap(signalMap, newPort, signalIndex);
                    } else {
                        signalMap.put(signalIndex, new SignalOccurences());

                        insertPortIntoMap(signalMap, newPort, signalIndex);
                    }
                } else {
                    // Only create constant drivers for the toplevel entity
                    // Otherwise the cell creator will take care of creating the constant drivers
                    // to simplify the entity insertion process
                    // TODO add constant drivers

                    if (isTopLevel) {
                        ElkPort constPort;
                        ElkPort source, sink;

                        if (constantDriverPortMap.containsKey(driver + currentPortDirection)) {
                            constPort = constantDriverPortMap.get(driver + currentPortDirection);
                        } else {
                            ElkNode constNode = ElkElementCreator.createNewConstantDriver(currentNode.getParent());
                            constNode.setDimensions(20d, 20d);

                            ElkLabel constLabel = ElkElementCreator.createNewConstantDriverLabel((String) driver, constNode, settings);

                            constPort = ElkElementCreator.createNewPort(constNode, oppositePortSide);

                            constantDriverPortMap.put(driver + currentPortDirection, constPort);
                        }

                        if (currentPortDirection.equals("input")) {
                            sink = newPort;
                            source = constPort;
                        } else {
                            sink = constPort;
                            source = newPort;
                        }

                        ElkEdge constEdge = ElkElementCreator.createNewEdge(sink, source);
                        constEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);

                        ElkLabel constEdgeLabel = ElkElementCreator.createNewEdgeLabel((String) driver, constEdge, settings);
                        constEdgeLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.TAIL);
                    }
                }

                currentIndexInPort++;
            }
        }
    }

    private void insertPortIntoMap(HashMap<Integer, SignalOccurences> signalMap, ElkPort port, int signalIndex) {
        SignalOccurences signalOccurences = signalMap.get(signalIndex);

        if (port.getProperty(CoreOptions.PORT_SIDE) == PortSide.WEST) {
            signalOccurences.setInPort(port);
        } else {
            signalOccurences.getOutPorts().add(port);
        }
    }
}
