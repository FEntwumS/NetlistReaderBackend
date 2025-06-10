package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.List;

public record Range(int lower, int upper, List<Object> drivers) {
    public boolean singleElement() {
        return lower == upper;
    }
}
