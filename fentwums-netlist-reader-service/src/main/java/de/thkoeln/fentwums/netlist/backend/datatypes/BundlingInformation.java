package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;

public record BundlingInformation(ElkPort port, String signalName, ArrayList<Integer> containedSignals) {

}
