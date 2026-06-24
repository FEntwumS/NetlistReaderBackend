package de.thkoeln.fentwums.netlist.backend.elkoptions;

public record NetAssociation(String netName, int indexInNet, SignalNameValidityLevel validityLevel) implements Comparable<NetAssociation> {
	@Override
	public int compareTo(NetAssociation o) {
		return Integer.compare(this.indexInNet, o.indexInNet);
	}
}
