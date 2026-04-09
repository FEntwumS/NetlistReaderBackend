package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkPort;

import java.util.List;

public record AggSet(List<Integer> sigbits, ElkPort port) {
}
