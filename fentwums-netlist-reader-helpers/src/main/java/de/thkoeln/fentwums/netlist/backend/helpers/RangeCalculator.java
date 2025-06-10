package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalElement;

import java.util.ArrayList;
import java.util.List;

public class RangeCalculator {
    public static List<Range> calculateRanges(List<SignalElement> values) {
        int cRangeStart, cRangeEnd, currentElem;

        List<Range> ret = new ArrayList<>();
        List<Object> signalIndices = new ArrayList<>();

        if (values.isEmpty()) {
            return ret;
        }
        values.sort(SignalElement::compareTo);

        cRangeStart = values.getFirst().canonicalIndex();
        cRangeEnd = cRangeStart;
        signalIndices.add(values.getFirst().driver());

        if (values.size() == 1) {
                        ret.add(new Range(cRangeStart, cRangeEnd, signalIndices));

            return ret;
        }

        for (int i = 1; i < values.size(); i++) {
            currentElem = values.get(i).canonicalIndex();

            if (cRangeEnd + 1 != currentElem) {
                ret.add(new Range(cRangeStart, cRangeEnd, signalIndices));

                cRangeStart = currentElem;
                cRangeEnd = currentElem;
                signalIndices.clear();
                signalIndices.add(values.get(i).driver());
            } else {
                cRangeEnd = currentElem;
                signalIndices.add(values.get(i).driver());
            }
        }

        ret.add(new Range(cRangeStart, cRangeEnd, signalIndices));

        return ret;
    }
}
