package de.thkoeln.fentwums.netlist.backend.datatypes;

public class NetlistCreationSettings {
	private double entityLabelFontSize;
	private double cellLabelFontSize;
	private double portLabelFontSize;
	private double edgeLabelFontSize;

	/**
	 * Creates a new settings store for the associated netlist
	 *
	 * @param entityLabelFontSize	Fontsize for entities
	 * @param cellLabelFontSize	Fontsize for cells
	 * @param portLabelFontSize	Fontsize for ports
	 * @param edgeLabelFontSize	Fontsize for edges
	 */
	public NetlistCreationSettings(double entityLabelFontSize, double cellLabelFontSize, double portLabelFontSize, double edgeLabelFontSize) {
		this.entityLabelFontSize = entityLabelFontSize;
		this.cellLabelFontSize = cellLabelFontSize;
		this.portLabelFontSize = portLabelFontSize;
		this.edgeLabelFontSize = edgeLabelFontSize;
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
}
