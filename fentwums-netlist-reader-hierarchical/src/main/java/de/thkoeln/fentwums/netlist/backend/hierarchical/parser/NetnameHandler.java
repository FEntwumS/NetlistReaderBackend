package de.thkoeln.fentwums.netlist.backend.hierarchical.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.ModuleNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalOccurences;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NetnameHandler {
    private static Logger logger = LoggerFactory.getLogger(NetnameHandler.class);

    public void handleNetnames(HashMap<String, Object> netlist,
                               ConcurrentHashMap<String, HashMap<Integer, SignalOccurences>> signalMaps,
                               NetlistCreationSettings settings, ModuleNode currentModuleNode, String moduleName,
                               String instancePath) {
        HashMap<String, Object> module, netnames, currentNet, currentNetAttributes;
        HashMap<Integer, SignalOccurences> signalMap = signalMaps.get(instancePath);
        boolean hideName, isReversed;
        List<Object> currentNetBits;
        String currentNetSrc;
        int currentIndexInNet;
        ElkPort sourcePort;
        SignalOccurences currentSignalOccurences;

        if (signalMap == null) {
            logger.atError().setMessage("signalMap is null for module {} at path {}. Aborting...").addArgument(
                    moduleName).addArgument(instancePath).log();
            return;
        }

        module = (HashMap<String, Object>) netlist.get(moduleName);

        if (module == null) {
            logger.atError().setMessage("Could not find module {} in Netlist. Aborting...").addArgument(moduleName)
                    .log();
            return;
        }

        netnames = (HashMap<String, Object>) module.get("netnames");

        for (String currentNetName : netnames.keySet()) {
            currentNet = (HashMap<String, Object>) netnames.get(currentNetName);

            // TODO filter better
            // hideName = currentNet.containsKey("hide_name") && !currentNet.get("hide_name").equals(0);
            hideName = true;

            currentNetBits = (ArrayList<Object>) currentNet.get("bits");

            currentNetAttributes = (HashMap<String, Object>) currentNet.get("attributes");

            if (currentNetAttributes.containsKey("src")) {
                currentNetSrc = (String) currentNetAttributes.get("src");
            } else {
                currentNetSrc = "";
            }

            if (currentNet.containsKey("offset")) {
                currentIndexInNet = (int) currentNet.get("offset");
            } else {
                currentIndexInNet = 0;
            }

            isReversed = currentNet.containsKey("upto");

            if (isReversed) {
                currentNetBits = (List<Object>) currentNetBits.reversed();
                currentIndexInNet += currentNetBits.size() - 1;
            }

            for (Object bit : currentNetBits) {
                if (bit instanceof Integer) {
                    currentSignalOccurences = signalMap.get(bit);
                    if (currentSignalOccurences != null) {
                        sourcePort = currentSignalOccurences.getSourcePort();

                        if (sourcePort != null) {
                            sourcePort.setProperty(FEntwumSOptions.MSB_FIRST, isReversed);

                            for (ElkPort sink : currentSignalOccurences.getSinkPorts()) {
                                sink.setProperty(FEntwumSOptions.MSB_FIRST, isReversed);

                                ElkEdge newEdge = createNewEdge(sink, sourcePort);

                                newEdge.setProperty(FEntwumSOptions.SRC_LOCATION, currentNetSrc);
                                newEdge.setProperty(FEntwumSOptions.INDEX_IN_SIGNAL, currentIndexInNet);
                                newEdge.setProperty(FEntwumSOptions.SIGNAL_NAME, currentNetName);
                                newEdge.setProperty(FEntwumSOptions.MSB_FIRST, isReversed);

                                if (!hideName) {
                                    ElkLabel newEdgeLabel = ElkElementCreator.createNewEdgeLabel(currentNetName +
                                                                                                         (currentNetBits.size() ==
                                                                                                                 1 ?
                                                                                                                 "" :
                                                                                                                 " [" +
                                                                                                                         currentIndexInNet +
                                                                                                                         "]"),
                                                                                                 newEdge, settings);
                                }
                            }
                        }
                    }

                } else {
                    logger.atDebug().setMessage("Net {} of module {} contains constant value. Skipping this bit...")
                            .addArgument(currentNetName).addArgument(moduleName).log();
                }

                if (isReversed) {
                    currentIndexInNet--;
                } else {
                    currentIndexInNet++;
                }
            }
        }

        // Now check for inside self loops
        // All matches are marked appropriately
        ElkNode currentNode = currentModuleNode.getNode();

        for (int index : signalMap.keySet()) {
            SignalOccurences signalOccurences = signalMap.get(index);

            ElkPort source = signalOccurences.getSourcePort();

            if (source == null) {
                continue;
            }

            if (source.getProperty(CoreOptions.PORT_SIDE) != PortSide.WEST || source.getParent() != currentNode) {
                continue;
            }

            for (ElkEdge candidate : source.getOutgoingEdges()) {
                ElkPort sink = (ElkPort) candidate.getTargets().getFirst();

                if (sink.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST && sink.getParent() == currentNode) {
                    candidate.setProperty(CoreOptions.INSIDE_SELF_LOOPS_YO, true);
                }
            }
        }
    }

    private ElkEdge createNewEdge(ElkPort sink, ElkPort source) {
        for (ElkEdge edge : source.getOutgoingEdges()) {
            if (edge.getTargets().getFirst().equals(sink)) {
                return edge;
            }
        }

        return ElkElementCreator.createNewEdge(sink, source);
    }
}
