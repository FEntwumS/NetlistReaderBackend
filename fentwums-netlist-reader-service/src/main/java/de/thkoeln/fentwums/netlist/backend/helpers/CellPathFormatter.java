package de.thkoeln.fentwums.netlist.backend.helpers;

public class CellPathFormatter {
    public CellPathFormatter() {}

    public String format(String input) {
        String output;

        input = input.replaceAll("\\.\\$auto\\$ghdl\\.cc", " ghdl")
                .replaceAll("\\$auto\\$ghdl\\.cc","ghdl")
                .replaceAll("\\$auto\\$opt_dff\\.cc", "opt_dff")
                .replaceAll("\\$auto\\$ff\\.cc", "auto_ff")
                .replaceAll("\\$auto\\$opt_reduce\\.cc", "opt_reduce");

        if (input.startsWith("$flatten")) {
            output = input.replaceAll("\\$flatten\\\\", "").replaceAll("\\.\\\\", " ");
        } else {
            output = input;
            //output = input.replaceAll("\\.", " ");
        }

        return output.replaceAll("\\\\", "");
    }
}
