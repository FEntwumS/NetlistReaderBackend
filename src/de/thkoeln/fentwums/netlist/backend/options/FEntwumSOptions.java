package de.thkoeln.fentwums.netlist.backend.options;

import org.eclipse.elk.core.data.ILayoutMetaDataProvider;
import org.eclipse.elk.core.data.LayoutOptionData;
import org.eclipse.elk.graph.properties.IProperty;
import org.eclipse.elk.graph.properties.Property;

import java.util.EnumSet;

public class FEntwumSOptions implements ILayoutMetaDataProvider {

    public static final IProperty<String> SIGNAL_NAME = new Property<String>(
            "de.thkoeln.fentwums.netlist.backend.signalname",
            "");

    @Override
    public void apply(Registry registry) {
        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.signalname")
                .defaultValue("")
                .type(LayoutOptionData.Type.BOOLEAN)
                .optionClass(Boolean.class)
                .targets(EnumSet.of(LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );
    }
}
