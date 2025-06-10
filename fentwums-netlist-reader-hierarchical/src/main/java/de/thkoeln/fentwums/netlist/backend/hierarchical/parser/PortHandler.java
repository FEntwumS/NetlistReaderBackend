package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;


import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalElement;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalOccurences;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
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
                            String instancePath) {
        PortSide createdPortSide, oppositePortSide;
        HashMap<String, Object> currentPort, module, ports;
        int currentIndexInPort;
        List<Object> portDrivers;
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

            reversedPort = currentPort.containsKey("upto");

            // reverse order for MSB-first ports
            if (reversedPort) {
                portDrivers = portDrivers.reversed();
                canonicalIndex = portDrivers.size() - 1;
                currentIndexInPort += portDrivers.size() - 1;
            } else {
                canonicalIndex = 0;
            }

            for (Object driver : portDrivers) {
                if (driver instanceof Integer) {
                    signalIndexList.add(new SignalElement(currentIndexInPort, driver));
                } else {
                    constantSignalIndexList.add(new SignalElement(currentIndexInPort, driver));
                }

                if (reversedPort) {
                    canonicalIndex--;
                    currentIndexInPort--;
                } else {
                    canonicalIndex++;
                    currentIndexInPort++;
                }
            }

            List<Range> signalRanges = RangeCalculator.calculateRanges(signalIndexList);
            List<Range> constRanges = RangeCalculator.calculateRanges(constantSignalIndexList);

            // First create ports for "normal" signals
            for (Range signalRange : signalRanges) {
                ElkPort newPort = ElkElementCreator.createNewPort(currentNode, createdPortSide);
                newPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);

                if (signalRange.singleElement()) {
                    newPort.setProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP, (int) signalRange.drivers().getFirst());
                    newPort.setProperty(FEntwumSOptions.CANONICAL_INDEX_IN_PORT_GROUP, signalRange.lower());

                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + (portDrivers.size() == 1 ? "" : " [" + (signalRange.lower()) + "]"), newPort,
                            settings);
                } else {
                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + " [" + (signalRange.upper()) + ":" + (signalRange.lower()) + "]", newPort,
                            settings);
                }

                for (Object driver : signalRange.drivers()) {
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
            for (Range constRange : constRanges) {
                ElkPort constPort;
                ElkPort source, sink;
                ElkNode constNode;

                ElkPort newPort = ElkElementCreator.createNewPort(currentNode, createdPortSide);
                newPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portname);

                if (constRange.singleElement()) {
                    newPort.setProperty(FEntwumSOptions.CANONICAL_INDEX_IN_PORT_GROUP, constRange.lower());

                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + (portDrivers.size() == 1 ? "" : " [" + (constRange.lower()) + "]"), newPort,
                            settings);
                } else {
                    ElkLabel newPortLabel = ElkElementCreator.createNewPortLabel(
                            portname + " [" + (constRange.upper()) + ":" + (constRange.lower()) + "]", newPort,
                            settings);
                }

                StringBuilder constantValues = new StringBuilder();

                for (Object driver : constRange.drivers()) {
                    if (driver instanceof String) {
                        constantValues.append((String) driver);
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

                } else {
                    // Const input
                    constNode = ElkElementCreator.createNewConstantDriver(currentNode.getParent());
                    constNode.setDimensions(20d, 20d);

                    ElkLabel constLabel = ElkElementCreator.createNewConstantDriverLabel(constantValues.toString(),
                                                                                         constNode, settings);

                    constPort = ElkElementCreator.createNewPort(constNode, oppositePortSide);

                }

                source = constPort;
                sink = newPort;

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
