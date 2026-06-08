package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.List;

public record Range(int lower, int upper) {
    public boolean singleElement() {
        return lower == upper;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;

        Range comparisonRange = (Range) obj;

        return this.lower == comparisonRange.lower
                && this.upper == comparisonRange.upper;
    }
}
