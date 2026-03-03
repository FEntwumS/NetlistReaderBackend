package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.List;

public record BundleRange(Range containedRange, List<Object> actualDrivers, List<Object> internalDrivers) implements Comparable<BundleRange> {
	
	@Override
	public int compareTo(BundleRange o) {
		return Integer.compare(this.containedRange().upper(), o.containedRange.upper());
	}
}
