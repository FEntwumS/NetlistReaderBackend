package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.ModuleNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalOccurences;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CellHandler {
    private static Logger logger = LoggerFactory.getLogger(CellHandler.class);

    public void createCells(HashMap<String, Object> netlist, ElkNode parentNode,
                            ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps,
                            NetlistCreationSettings settings, HashMap<String, Object> blackboxes,
                            ModuleNode currentModuleNode, String moduleName, String instancePath) {
        HashMap<String, Object> module, cells, currentCell, currentCellAttributes, currentCellPortDirections, currentCellConnections;
        int currentCellIndex = 0, maxSignals, currentPortIndex, currentBitIndex;
        String cellType, srcLocation = "", newSubModulePath;
        PortSide newPortSide, oppositeSide = PortSide.WEST;
        boolean isHidden, isDerived;
        HashMap<Integer, String> constantValues;
        ArrayList<Object> currentCellPortDrivers;
        HashMap<Integer, SignalOccurences> signalMap = signalMaps.get(instancePath);

        if (signalMap == null) {
            logger.atError().setMessage("signalMap is null for module {} at path {}. Aborting...").addArgument(
                    moduleName).addArgument(instancePath).log();
            return;
        }

        module = (HashMap<String, Object>) netlist.get(moduleName);

        if (module == null) {
            logger.atError().setMessage("Could not find module {} in Netlist. Aborting...").addArgument(
                    moduleName).log();
            return;
        }

        cells = (HashMap<String, Object>) module.get("cells");

        for (String cellName : cells.keySet()) {
            if (currentCellIndex % 512 == 0) {
                logger.atInfo().setMessage("Cell {} of {}")
                        .addArgument(currentCellIndex).addArgument(cells.size()).log();
            }

            currentCellIndex++;

            currentCell = (HashMap<String, Object>) cells.get(cellName);
            currentCellAttributes = (HashMap<String, Object>) currentCell.get("attributes");

            cellType = (String) currentCell.get("type");

            // Check whether the module / cell is hidden or not
            isHidden = currentCell.get("hide_name").equals("0");

            if (currentCellAttributes.containsKey("src")) {
                srcLocation = currentCellAttributes.get("src").toString();
            }

            currentCellPortDirections = (HashMap<String, Object>) currentCell.get("port_directions");
            currentCellConnections = (HashMap<String, Object>) currentCell.get("connections");

            if (currentCellAttributes.containsKey("module_not_derived") || cellType.contains("paramod")) {
                isDerived = false;

                if (currentCellPortDirections != null) {
                    logger.atInfo().setMessage("Cell {} is a blackbox. Using yosys description to add input/output " +
                            "information").addArgument(cellName).log();
                } else {
                    if (blackboxes.containsKey(cellType)) {
                        logger.atInfo().setMessage("Using description from external file for cell {}").addArgument(
                                cellName).log();
                        currentCellPortDirections = (HashMap<String, Object>) blackboxes.get(cellType);
                    } else {
                        logger.atError().setMessage("Cell {} is a blackbox cell. Aborting...").addArgument(
                                cellName).log();
                        throw new RuntimeException("Cell " + cellName + " is a blackbox cell. Aborting...");
                    }
                }
            } else {
                isDerived = true;
            }

            ElkNode newCellNode = ElkElementCreator.createNewNode(parentNode, cellName);
            newCellNode.setProperty(FEntwumSOptions.CELL_NAME, cellName);

            newCellNode.setProperty(FEntwumSOptions.SRC_LOCATION, srcLocation);
            if (!isHidden && !isDerived) {
                newCellNode.setProperty(FEntwumSOptions.LOCATION_PATH, instancePath + " " + cellName);
                newCellNode.setProperty(FEntwumSOptions.CELL_TYPE, "HDL_ENTITY");
            } else {
                newCellNode.setProperty(FEntwumSOptions.CELL_TYPE, cellType);
            }

            ElkLabel newCellNodeLabel = ElkElementCreator.createNewCellLabel(cellType, newCellNode, settings);

            // Create label containing the cell's/module's name for non-hidden cells
            // The label is placed below the generated node
            // It contains the user-given name of the cell/module and therefor is likely to differ from the
            // cell's/module's type
            if (!isHidden) {
                ElkLabel newCellModuleLabel = ElkElementCreator.createNewModuleLabel(cellName, newCellNode, settings);
            }

            // get max number of signals
            maxSignals = 0;

            for (String portName : currentCellConnections.keySet()) {
                if (((ArrayList<Object>) currentCellConnections.get(portName)).size() > maxSignals) {
                    maxSignals = ((ArrayList<Object>) currentCellConnections.get(portName)).size();
                }
            }

            currentPortIndex = 0;

            // Now create the cell's ports
            // For non-derived modules, the PortCreator is used, as there is additional information in the module's port
            // section
            // Hidden, non-derived cells are assumed to be blackboxes and therefore are not made available for expansion
            // Non-hidden, non-derived modules are assumed to be user-created HDL entity instantiations and made
            // available for later expansion by the user
            if (isDerived) {
                for (String portName : currentCellPortDirections.keySet()) {
                    currentBitIndex = 0;
                    constantValues = new HashMap<>();

                    if (currentCellConnections.size() != currentCellPortDirections.size() || !currentCellConnections.containsKey(
                            portName)) {
                        throw new RuntimeException(
                                "Mismatch between number of ports in port_directions and connections");
                    }

                    if (currentCellPortDirections.get(portName).equals("input")) {
                        newPortSide = PortSide.WEST;
                        oppositeSide = PortSide.EAST;
                    } else {
                        newPortSide = PortSide.EAST;
                        oppositeSide = PortSide.WEST;
                    }

                    currentCellPortDrivers = (ArrayList<Object>) currentCellConnections.get(portName);

                    for (Object driver : currentCellPortDrivers) {
                        if (driver instanceof Integer) {
                            // Create port
                            ElkPort newCellPort = ElkElementCreator.createNewPort(newCellNode, newPortSide);
                            newCellPort.setProperty(CoreOptions.PORT_INDEX,
                                    currentPortIndex * maxSignals + currentBitIndex);
                            newCellPort.setProperty(FEntwumSOptions.PORT_GROUP_NAME, portName);
                            newCellPort.setProperty(FEntwumSOptions.INDEX_IN_PORT_GROUP, currentBitIndex);

                            ElkLabel newCellPortLabel = ElkElementCreator.createNewPortLabel(
                                    portName + (currentCellPortDrivers.size() == 1 ? "" : " [" + currentBitIndex + "]"),
                                    newCellPort, settings);

                            SignalOccurences signalOccurences = signalMap.get(driver);

                            if (signalOccurences == null) {
                                signalOccurences = new SignalOccurences();

                                signalMap.put((Integer) driver, signalOccurences);
                            }

                            if (newPortSide == PortSide.EAST) {
                                signalOccurences.setSourcePort(newCellPort);
                            } else {
                                signalOccurences.getSinkPorts().add(newCellPort);
                            }
                        } else {
                            // Defer Port construction until all constant values have been gathered
                            constantValues.put(currentBitIndex, (String) driver);
                        }

                        currentBitIndex++;
                    }

                    // TODO create constant drivers

                    currentPortIndex++;
                }
            } else {
                PortHandler portHandler = new PortHandler();
                newSubModulePath = instancePath + " " + cellName;

                portHandler.createPorts(netlist, signalMaps, newCellNode, settings, cellType, newSubModulePath);

                // Make user modules available in hierarchy for later loading and expansion
                if (!isHidden) {
                    ModuleNode childModuleNode = new ModuleNode(newCellNode);
                    childModuleNode.setCellType(cellType);
                    childModuleNode.setCellName(cellName);

                    currentModuleNode.getChildren().put(cellName, childModuleNode);
                }

                // Backport port association for signals crossing boundary to new module
                for (String portName : currentCellPortDirections.keySet()) {
                    currentBitIndex = 0;

                    if (currentCellConnections.size() != currentCellPortDirections.size() || !currentCellConnections.containsKey(
                            portName)) {
                        throw new RuntimeException(
                                "Mismatch between number of ports in port_directions and connections");
                    }

                    currentCellPortDrivers = (ArrayList<Object>) currentCellConnections.get(portName);

                    for (Object driver : currentCellPortDrivers) {
                        if (driver instanceof Integer) {
                            int finalCurrentBitIndex = currentBitIndex;
                            Set<ElkPort> matchingPorts = newCellNode.getPorts().stream().filter(
                                    port -> port.getProperty(FEntwumSOptions.PORT_GROUP_NAME).equals(
                                            portName) && port.getProperty(
                                            FEntwumSOptions.CANONICAL_INDEX_IN_PORT_GROUP) == finalCurrentBitIndex).collect(
                                    Collectors.toSet());

                            if (matchingPorts.isEmpty()) {
                                logger.atError().setMessage("Module {} portgroup {} index {} not found")
                                        .addArgument(cellName).addArgument(portName).addArgument(currentBitIndex).log();
                            } else {
                                if (matchingPorts.size() > 1) {
                                    logger.atWarn().setMessage(
                                                    "Module {} portgroup {} index {} found more than one matching port")
                                            .addArgument(cellName).addArgument(portName).addArgument(
                                                    currentBitIndex).log();
                                }

                                if (!signalMap.containsKey(driver)) {
                                    signalMap.put((Integer) driver, new SignalOccurences());
                                }

                                if (currentCellPortDirections.get(portName).equals("input")) {
                                    signalMap.get(driver).getSinkPorts().add(matchingPorts.iterator().next());
                                } else {
                                    signalMap.get(driver).setSourcePort(matchingPorts.iterator().next());
                                }
                            }
                        } else {
                            // Constant drivers can be ignored
                            logger.atDebug().setMessage("Module {} portgroup {} index {} is constant. Skipping...")
                                    .addArgument(cellName).addArgument(portName).addArgument(currentBitIndex).log();
                        }

                        currentBitIndex++;
                    }

                    currentPortIndex++;
                }
            }
        }
    }
}
