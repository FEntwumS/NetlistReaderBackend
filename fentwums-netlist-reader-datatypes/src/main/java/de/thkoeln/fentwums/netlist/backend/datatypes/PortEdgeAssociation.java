package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkPort;

public record PortEdgeAssociation(ElkPort port, ElkEdge edge) {
}
