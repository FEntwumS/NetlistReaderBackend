package de.thkoeln.fentwums.netlist.backend.helpers;

public class CellPathFormatter {
    public CellPathFormatter() {}

    public String format(String input) {
        String output;

        input = input.replaceAll("\\$auto\\$ghdl\\.cc","ghdl");

        if (input.startsWith("$flatten")) {
            output = input.replaceAll("\\$flatten\\\\", "").replaceAll("\\.\\\\", " ");
        } else {
            output = input.replaceAll("\\.", " ");
        }

        return output;
    }
}
