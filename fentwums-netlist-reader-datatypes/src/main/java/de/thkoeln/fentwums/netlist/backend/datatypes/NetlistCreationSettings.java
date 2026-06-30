package de.thkoeln.fentwums.netlist.backend.datatypes;

import de.thkoeln.fentwums.netlist.backend.elkoptions.JunctionShape;

public class NetlistCreationSettings {
	private double entityLabelFontSize;
	private double cellLabelFontSize;
	private double portLabelFontSize;
	private double edgeLabelFontSize;
	private PerformanceTarget performanceTarget;
	private RequestedJunctionShape junctionShape;
	private int layoutEffort;
	private boolean showUnconnectedPorts;
	private boolean onlyShowUserCreatedSignalNames;

	/**
	 * Creates a new settings store for the associated netlist
	 *
	 * @param entityLabelFontSize	Fontsize for entities
	 * @param cellLabelFontSize	Fontsize for cells
	 * @param portLabelFontSize	Fontsize for ports
	 * @param edgeLabelFontSize	Fontsize for edges
	 */
	public NetlistCreationSettings(double entityLabelFontSize, double cellLabelFontSize, double portLabelFontSize, double edgeLabelFontSize, PerformanceTarget performanceTarget, RequestedJunctionShape junctionShape, int layoutEffort, boolean showUnconnectedPorts, boolean onlyShowUserCreatedSignalNames) {
		this.entityLabelFontSize = entityLabelFontSize;
		this.cellLabelFontSize = cellLabelFontSize;
		this.portLabelFontSize = portLabelFontSize;
		this.edgeLabelFontSize = edgeLabelFontSize;
		this.performanceTarget = performanceTarget;
		this.junctionShape = junctionShape;
		this.layoutEffort = layoutEffort;
		this.showUnconnectedPorts = showUnconnectedPorts;
		this.onlyShowUserCreatedSignalNames = onlyShowUserCreatedSignalNames;
	}

	/**
	 * Get the font size for entities
	 *
	 * @return	The font size
	 */
	public double getEntityLabelFontSize() {
		return entityLabelFontSize;
	}

	/**
	 * Get the font size for cells
	 *
	 * @return	The font size
	 */
	public double getCellLabelFontSize() {
		return cellLabelFontSize;
	}

	/**
	 * Get the font size for ports
	 *
	 * @return 	The font size
	 */
	public double getPortLabelFontSize() {
		return portLabelFontSize;
	}

	/**
	 * Get the font size for edges
	 *
	 * @return	The font size
	 */
	public double getEdgeLabelFontSize() {
		return edgeLabelFontSize;
	}

	/**
	 * Get the requested performance target
	 *
	 * @return The performance target
	 */
	public PerformanceTarget getPerformanceTarget() {
		return performanceTarget;
	}

	public RequestedJunctionShape getJunctionShape() {
		return junctionShape;
	}

	public int getLayoutEffort() {
		return layoutEffort;
	}

	public boolean doShowUnconnectedPorts() {
		return showUnconnectedPorts;
	}

	public boolean doOnlyShowUserCreatedSignalNames() {
		return onlyShowUserCreatedSignalNames;
	}
}
