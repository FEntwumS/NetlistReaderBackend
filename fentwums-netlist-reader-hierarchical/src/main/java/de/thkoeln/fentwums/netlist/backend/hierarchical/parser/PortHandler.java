package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;


import de.thkoeln.fentwums.netlist.backend.datatypes.*;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.elkoptions.PortType;
import de.thkoeln.fentwums.netlist.backend.elkoptions.SignalType;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import de.thkoeln.fentwums.netlist.backend.helpers.RangeCalculator;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PortHandler {
    private static Logger logger = LoggerFactory.getLogger(PortHandler.class);

    /**
     * Creates the input and output ports of the current entity
     *
     * @param currentNode The ElkNode representing the current entity
     */
    public void createPorts(HashMap<String, Object> netlist,
                            ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps,
                            ElkNode currentNode, NetlistCreationSettings settings, String moduleType,
                            String instancePath, HashMap<String, Object> instanceConnections) {
        PortSide createdPortSide, oppositePortSide;
        HashMap<String, Object> currentPort, module, ports;
        int currentIndexInPort;
        List<Object> portDrivers, instanceConnection;
        int signalIndex, canonicalIndex;
        String currentPortDirection;
        HashMap<String, ElkPort> constantDriverPortMap = new HashMap<>();
        HashMap<Integer, SignalOccurences> signalMap;
        boolean reversedPort;
        ArrayList<SignalElement> signalIndexList = new ArrayList<>(), constantSignalIndexList = new ArrayList<>();

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

        ports = (HashMap<String, Object>) module.get("ports");

        boolean isTopLevel = currentNode.getParent() != null && currentNode.getParent().getIdentifier().equals("root");

        for (String portname : ports.keySet()) {
            signalIndexList.clear();
            constantSignalIndexList.clear();

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

            portDrivers = (List<Object>) currentPort.get("bits");

            // Use constant value from parent, if the port is an input of a non-toplevel entity
            if (!currentPortDirection.equals("output") && instanceConnections != null && instanceConnections.containsKey(portname)) {
                instanceConnection = (List<Object>) instanceConnections.get(portname);
            } else {
                instanceConnection = (List<Object>) currentPort.get("bits");
            }

            reversedPort = currentPort.containsKey("upto");

            // reverse order for MSB-first ports
            if (reversedPort) {
                portDrivers = portDrivers.reversed();
                instanceConnection = instanceConnection.reversed();
                canonicalIndex = portDrivers.size() - 1;
                currentIndexInPort += portDrivers.size() - 1;
            } else {
                canonicalIndex = 0;
            }

            if (instanceConnection.isEmpty()) {
                logger.atWarn().setMessage("Module type {} port {} has no connections. Skipping...").addArgument(moduleType).addArgument(portname).log();
                continue;
            }

            for (int i = 0; i < portDrivers.size(); i++) {
                if (instanceConnection.get(i) instanceof Integer) {
                    signalIndexList.add(new SignalElement(currentIndexInPort, instanceConnection.get(i),
                                                          portDrivers.get(i)));
                } else {
                    constantSignalIndexList.add(new SignalElement(currentIndexInPort, instanceConnection.get(i),
                                                                  portDrivers.get(i)));
                }

                if (reversedPort) {
                    canonicalIndex--;
                    currentIndexInPort--;
                } else {
                    canonicalIndex++;
                    currentIndexInPort++;
                }
            }

            List<BundleRange> signalRanges = RangeCalculator.calculateRanges(signalIndexList);
            List<BundleRange> constRanges = RangeCalculator.calculateRanges(constantSignalIndexList);

            // First create ports for "normal" signals
            for (BundleRange signalRange : signalRanges) {
                ElkPort newPort = ElkElementCreator.createNewPort(currentNode, createdPortSide);
                newPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);

                if (signalRange.containedRange().singleElement()) {
                    // newPort.setProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP, (int) signalRange.drivers().getFirst());
                    newPort.setProperty(FEntwumSOptions.CANONICAL_INDEX_IN_PORT_GROUP, signalRange.containedRange().lower());

                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + (portDrivers.size() == 1 ? "" : " [" + (signalRange.containedRange().lower()) + "]"), newPort,
                            settings);
                } else {
                    // Add contained range data to port
                    newPort.setProperty(FEntwumSOptions.CANONICAL_BUNDLE_LOWER_INDEX_IN_PORT_GROUP,
                                        signalRange.containedRange().lower());
                    newPort.setProperty(FEntwumSOptions.CANONICAL_BUNDLE_UPPER_INDEX_IN_PORT_GROUP, signalRange.containedRange().upper());

                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + (reversedPort ? " [" + (signalRange.containedRange().upper()) + ":" + (signalRange.containedRange().lower()) : " [" + (signalRange.containedRange().lower()) + ":" + (signalRange.containedRange().upper())) + "]", newPort,
                            settings);
                }

                for (Object driver : signalRange.internalDrivers()) {
                    if (driver instanceof Integer) {
                        signalIndex = (Integer) driver;

                        if (signalMap.containsKey(signalIndex)) {
                            insertPortIntoMap(signalMap, newPort, signalIndex);
                        } else {
                            signalMap.put(signalIndex, new SignalOccurences());

                            insertPortIntoMap(signalMap, newPort, signalIndex);
                        }
                    } else {
                        logger.atError().setMessage("Cell {} Signal {} is constant but part of \"normal\" range")
                                .addArgument(moduleType).addArgument(portname).log();
                    }
                }
            }

            // Then create ports for constant signals
            // Constant inputs are generated as nodes on the same layer as the current entities representation, whereas
            // constant outputs are generated as children of the current entities representation
            for (BundleRange constRange : constRanges) {
                ElkPort constPort;
                ElkPort source, sink;
                ElkNode constNode;

                ElkPort newPort = ElkElementCreator.createNewPort(currentNode, createdPortSide);
                newPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);

                if (constRange.containedRange().singleElement()) {
                    newPort.setProperty(FEntwumSOptions.CANONICAL_INDEX_IN_PORT_GROUP, constRange.containedRange().lower());

                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + (portDrivers.size() == 1 ? "" : " [" + (constRange.containedRange().lower()) + "]"), newPort,
                            settings);
                } else {
                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + (reversedPort ? " [" + (constRange.containedRange().upper()) + ":" + (constRange.containedRange().lower()) : " [" + (constRange.containedRange().lower()) + ":" + (constRange.containedRange().upper())) + "]", newPort,
                            settings);
                }

                StringBuilder constantValues = new StringBuilder();

                for (Object driver : constRange.actualDrivers()) {
                    if (driver instanceof String) {
                        constantValues.append((String) driver);
                    } else {
                        logger.error("Non-String const driver");
                    }
                }

                // Also generate the driver node
                if (createdPortSide == PortSide.EAST) {
                    // Const outputs are generated as a child
                    constNode = ElkElementCreator.createNewConstantDriver(currentNode);
                    constNode.setDimensions(20d, 20d);

                    ElkLabel constLabel = ElkElementCreator.createNewConstantDriverLabel(constantValues.toString(),
                                                                                         constNode, settings);

                    constPort = ElkElementCreator.createNewPort(constNode, createdPortSide);

                    source = constPort;
                    sink = newPort;
                } else {
                    // Const input
                    constNode = ElkElementCreator.createNewConstantDriver(currentNode.getParent());
                    constNode.setDimensions(20d, 20d);

                    ElkLabel constLabel = ElkElementCreator.createNewConstantDriverLabel(constantValues.toString(),
                                                                                         constNode, settings);

                    constPort = ElkElementCreator.createNewPort(constNode, oppositePortSide);

                    source = constPort;
                    sink = newPort;
                }

                if (portDrivers.size() == 1) {
                    newPort.setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_SINGLE);
                    constNode.setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_SINGLE);
                } else {
                    newPort.setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_MULTIPLE);
                    constNode.setProperty(FEntwumSOptions.PORT_TYPE, PortType.CONSTANT_MULTIPLE);
                }

                // Add connection
                ElkEdge constEdge = ElkElementCreator.createNewEdge(sink, source);
                constEdge.setProperty(FEntwumSOptions.SIGNAL_TYPE, SignalType.CONSTANT);

                ElkLabel constEdgeLabel = ElkElementCreator.createNewEdgeLabel(constantValues.toString(), constEdge,
                                                                               settings);
                constEdgeLabel.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.TAIL);
            }
        }
    }

    private void insertPortIntoMap(HashMap<Integer, SignalOccurences> signalMap, ElkPort port, int signalIndex) {
        SignalOccurences signalOccurences = signalMap.get(signalIndex);

        if (port.getProperty(CoreOptions.PORT_SIDE) == PortSide.WEST) {
            signalOccurences.setSourcePort(port);
        } else {
            signalOccurences.getSinkPorts().add(port);
        }
    }
}
