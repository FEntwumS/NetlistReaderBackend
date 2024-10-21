package de.thkoeln.fentwums.netlist.backend.parser;

import de.thkoeln.fentwums.netlist.backend.datatypes.SignalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalTree;

import java.util.ArrayList;
import java.util.HashMap;

public class NetnameHandler {
    public NetnameHandler() {}

    public void handleNetnames (HashMap<String, Object> netnames, String modulename,
                                HashMap<Integer, SignalTree> signalMap) {
        HashMap<String, Object> currentNet;
        HashMap<String, Object> currentNetAttributes;
        String currentNetPath;
        String[] currentNetPathSplit;
        ArrayList<Object> bitList;

        SignalTree currentSignalTree;
        SignalNode currentSignalNode;

        for (String currentNetName : netnames.keySet()) {
            currentNet = (HashMap<String, Object>) netnames.get(currentNetName);
            currentNetAttributes = (HashMap<String, Object>) currentNet.get("attributes");

            if (currentNetAttributes.containsKey("hdlname")) {
                currentNetPath = (String) currentNetAttributes.get("hdlname");
            } else {
                currentNetPath =
                        currentNetName.replaceFirst("\\$flatten", "").replaceAll("\\$auto\\$ghdl\\.cc:","").replaceAll("\\\\", "").replaceAll(
                        "\\" +
                        ".", " ");;
            }

            currentNetPathSplit = currentNetPath.split(" ");

            // Get relevant signal tree

            bitList = (ArrayList<Object>) currentNet.get("bits");

            for (Object bit : bitList) {
                currentSignalTree = signalMap.get((Integer) bit);

                currentSignalNode = currentSignalTree.getRoot().getHChildren().get(modulename);

                for(int i = 0; i < currentNetPathSplit.length - 1; i++) {
                    currentSignalNode = currentSignalNode.getHChildren().get(currentNetPathSplit[i]);

                    if (currentSignalNode == null) {
                        break;
                    }
                }

                if (currentSignalNode == null) {
                    System.out.println("Unknown cell; Bit " + (int) bit);

                    continue;
                }

                currentSignalNode.setSVisited(true);
            }
        }
    }
}
