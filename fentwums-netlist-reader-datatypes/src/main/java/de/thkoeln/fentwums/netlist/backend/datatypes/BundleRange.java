package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.List;

public record BundleRange(Range containedRange, List<Object> actualDrivers, List<Object> internalDrivers) {
}
