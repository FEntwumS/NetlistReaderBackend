package de.thkoeln.fentwums.netlist.backend.datatypes;

public record SignalElement(int canonicalIndex, Object actualDriver, Object internalSignalIndex) implements Comparable<SignalElement> {
    @Override
    public int compareTo(SignalElement o) {
        return Integer.compare(canonicalIndex, o.canonicalIndex);
    }
}
