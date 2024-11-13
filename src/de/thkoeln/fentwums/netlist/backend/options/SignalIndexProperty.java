package de.thkoeln.fentwums.netlist.backend.options;

import org.eclipse.elk.graph.properties.IProperty;

public class SignalIndexProperty implements IProperty {
    @Override
    public Object getDefault() {
        return null;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public Comparable getLowerBound() {
        return null;
    }

    @Override
    public Comparable getUpperBound() {
        return null;
    }
}
