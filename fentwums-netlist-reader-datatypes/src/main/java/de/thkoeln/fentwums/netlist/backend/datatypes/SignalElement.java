package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkEdge;

public record SignalElement(int canonicalIndex, Object actualDriver, Object internalSignalIndex, ElkEdge associatedEdge) implements Comparable<SignalElement> {
    @Override
    public int compareTo(SignalElement o) {
        return Integer.compare(canonicalIndex, o.canonicalIndex);
    }
}
