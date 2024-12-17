package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.Set;

public class NetInformation {
	public String scope;
	public Set<Integer> bits;

	public NetInformation(String scope, Set<Integer> bits) {
		this.scope = scope;
		this.bits = bits;
	}
}
