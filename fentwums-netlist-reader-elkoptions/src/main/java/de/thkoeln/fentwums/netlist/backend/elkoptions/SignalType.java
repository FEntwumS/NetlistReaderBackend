package de.thkoeln.fentwums.netlist.backend.elkoptions;

/**
 * Provides values for a custom ELK option that specifies the type of the signal represented by the
 * <code>ElkEdge</code> it is attached to
 */
public enum SignalType {
	CONSTANT, BUNDLED_CONSTANT, BUNDLED, SINGLE, UNDEFINED;
}
