package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;

/**
 * A datastructure to store information regarding the bundling process
 *
 * @param port             The port which is to be used by the bundled signals
 * @param signalName       The name of the signal to be bundled
 * @param containedSignals The indices of the bits contained in the signal that are to be bundled
 */
public record BundlingInformation(ElkPort port, String signalName, ArrayList<Integer> containedSignals) {

}
