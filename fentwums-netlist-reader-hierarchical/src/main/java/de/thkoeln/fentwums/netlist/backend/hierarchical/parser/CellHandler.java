package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.ModuleNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalOccurences;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class CellHandler {
    private static Logger logger = LoggerFactory.getLogger(CellHandler.class);

    public void createCells(HashMap<String, Object> netlist, ElkNode parentNode, ConcurrentHashMap<String, HashMap<Integer,
                SignalOccurences>> signalMaps, NetlistCreationSettings settings, HashMap<String, Object> blackboxes,
                            ModuleNode currentModuleNode, String moduleName, String instancePath) {
        HashMap<String, Object> module, cells, currentCell, currentCellAttributes, currentCellPortDirections, currentCellConnections;
        HashMap<String, ElkPort> constantDriverPortMap = new HashMap<>();
        int currentCellIndex = 0, maxSignals = 0, currentPortIndex = 0, currentBitIndex = 0;
        String cellType = "", srcLocation = "";
        PortSide newPortSide = PortSide.EAST, oppositeSide = PortSide.WEST;
        boolean isHidden = false, isDerived = false;

        module = (HashMap<String, Object>) netlist.get(moduleName);

        if (module == null) {
            logger.atError().setMessage("Could not find module {} in Netlist. Aborting...").addArgument(moduleName).log();
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

            cellType = ((String) currentCell.get("type")).replaceAll("\\$", "");

            // Check whether the module / cell is hidden or not
            isHidden = currentCell.get("hide_name").equals("0");

            if (currentCellAttributes.containsKey("src")) {
                srcLocation = currentCellAttributes.get("src").toString();
            }

            ElkNode newCellNode = ElkElementCreator.createNewNode(parentNode, cellName);
            newCellNode.setProperty(FEntwumSOptions.CELL_NAME, cellName);
            newCellNode.setProperty(FEntwumSOptions.CELL_TYPE, cellType);
            newCellNode.setProperty(FEntwumSOptions.SRC_LOCATION, srcLocation);

            ElkLabel newCellNodeLabel = ElkElementCreator.createNewCellLabel(cellType, newCellNode, settings);

            // Create label containing the cell's/module's name for non-hidden cells
            // The label is placed below the generated node
            // It contains the user-given name of the cell/module and therefor is likely to differ from the
            // cell's/module's type
            if (!isHidden) {
                ElkLabel newCellModuleLabel = ElkElementCreator.createNewModuleLabel(cellName, newCellNode, settings);
            }

            currentCellPortDirections = (HashMap<String, Object>) currentCell.get("port_directions");
            currentCellConnections = (HashMap<String, Object>) currentCell.get("connections");

            if (currentCellAttributes.containsKey("module_not_derived")) {
                isDerived = false;

                if (currentCellPortDirections != null) {
                    logger.atInfo().setMessage("Cell {} is a blackbox. Using yosys description to add input/output " +
                            "information").addArgument(cellName).log();
                } else {
                    if (blackboxes.containsKey(cellType)) {
                        logger.atInfo().setMessage("Using description from external file for cell {}").addArgument(cellName).log();
                        currentCellPortDirections = (HashMap<String, Object>) blackboxes.get(cellType);
                    } else {
                        logger.atError().setMessage("Cell {} is a blackbox cell. Aborting...").addArgument(cellName).log();
                        throw new RuntimeException("Cell " + cellName + " is a blackbox cell. Aborting...");
                    }
                }
            } else {
                isDerived = true;
            }

            // get max number of signals
            maxSignals = 0;

            for (String portName : currentCellPortDirections.keySet()) {
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


                    currentPortIndex++;
                }
            } else {
                PortHandler portHandler = new PortHandler();

                portHandler.createPorts(netlist, signalMaps, newCellNode, settings, cellType, instancePath + " " + cellName);

                // Make user modules available in hierarchy for later loading and expansion
                if (!isHidden) {
                    currentModuleNode.getChildNodes().put(cellName, new ModuleNode(newCellNode));
                }
            }
        }
    }
}
