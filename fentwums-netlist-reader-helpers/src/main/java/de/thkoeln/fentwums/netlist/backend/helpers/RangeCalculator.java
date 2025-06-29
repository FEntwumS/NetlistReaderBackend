package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.BundleRange;
import de.thkoeln.fentwums.netlist.backend.datatypes.Range;
import de.thkoeln.fentwums.netlist.backend.datatypes.SignalElement;

import java.util.ArrayList;
import java.util.List;

public class RangeCalculator {
    public static List<BundleRange> calculateRanges(List<SignalElement> values) {
        int cRangeStart, cRangeEnd, currentElem;

        List<BundleRange> ret = new ArrayList<>();
        List<Object> internalSignalIndices = new ArrayList<>(), actualDrivers = new ArrayList<>();

        if (values.isEmpty()) {
            return ret;
        }
        values.sort(SignalElement::compareTo);

        cRangeStart = values.getFirst().canonicalIndex();
        cRangeEnd = cRangeStart;
        actualDrivers.add(values.getFirst().actualDriver());
        internalSignalIndices.add(values.getFirst().internalSignalIndex());

        if (values.size() == 1) {
                        ret.add(new BundleRange(new Range(cRangeStart, cRangeEnd), actualDrivers,
                                                internalSignalIndices));

            return ret;
        }

        for (int i = 1; i < values.size(); i++) {
            currentElem = values.get(i).canonicalIndex();

            if (cRangeEnd + 1 != currentElem) {
                ret.add(new BundleRange(new Range(cRangeStart, cRangeEnd), actualDrivers, internalSignalIndices));

                cRangeStart = currentElem;
                cRangeEnd = currentElem;
                actualDrivers.clear();
                actualDrivers.add(values.get(i).actualDriver());
                internalSignalIndices.clear();
                internalSignalIndices.add(values.get(i).internalSignalIndex());
            } else {
                cRangeEnd = currentElem;
                actualDrivers.add(values.get(i).actualDriver());
                internalSignalIndices.add(values.get(i).internalSignalIndex());
            }
        }

        ret.add(new BundleRange(new Range(cRangeStart, cRangeEnd), actualDrivers, internalSignalIndices));

        return ret;
    }
}
