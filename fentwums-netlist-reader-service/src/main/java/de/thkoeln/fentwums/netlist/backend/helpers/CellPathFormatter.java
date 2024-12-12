package de.thkoeln.fentwums.netlist.backend.helpers;

/**
 * Provides unified formatting and cleaning of locations of netlist elements. Since the passes that are applied
 * during netlist generation affect these paths and add potentially confusing patterns, a standardised formatter is
 * necessary
 */
public class CellPathFormatter {
	public CellPathFormatter() {
	}

	/**
	 * Formats and cleans the input string
	 * @param input The string that is to be formatted and cleaned
	 * @return The formatted and cleaned string
	 */
	public String format(String input) {
		String output;

		input = input.replaceAll("\\.\\$auto\\$ghdl\\.cc", " ghdl")
				.replaceAll("\\$auto\\$ghdl\\.cc", "ghdl")
				.replaceAll("\\$auto\\$opt_dff\\.cc", "opt_dff")
				.replaceAll("\\$auto\\$ff\\.cc", "auto_ff")
				.replaceAll("\\$auto\\$opt_reduce\\.cc", "opt_reduce");

		if (input.startsWith("$flatten")) {
			output = input.replaceAll("\\$flatten\\\\", "").replaceAll("\\.\\\\", " ");
		} else {
			output = input;
		}

		return output.replaceAll("\\\\", "");
	}
}
