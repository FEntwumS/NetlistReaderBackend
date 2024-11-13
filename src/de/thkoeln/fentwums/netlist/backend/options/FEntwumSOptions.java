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

    public final static IProperty<Integer> SIGNAL_INDEX = new Property<Integer>(
            "de.thkoeln.fentwums.netlist.backend.signalindex",
            0
    );

    public final static IProperty<String> VECTOR_SIGNALS = new Property<String>(
            "de.thkoeln.fentwums.netlist.backend.vector-signals",
            ""
    );

    public final static IProperty<String> CELL_NAME = new Property<String>(
            "de.thkoeln.fentwums.netlist.backend.cellname",
            ""
    );

    public final static IProperty<String> CELL_TYPE = new Property<String>(
            "de.thkoeln.fentwums.netlist.backend.celltype",
            ""
    );

    public final static IProperty<String> LOCATION_PATH = new Property<String>(
            "de.thkoeln.fentwums.netlist.backend.location-path",
            ""
    );

    public static final IProperty<String> SRC_LOCATION = new Property<String>(
            "de.thkoeln.fentwums.netlist.backend.src-location",
            ""
    );

    public static final IProperty<String> SIGNAL_VALUE = new Property<String >(
            "de.thkoeln.fentwums.netlist.backend.signalvalue",
            ""
    );

    @Override
    public void apply(Registry registry) {
        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.signalname")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.signalindex")
                .defaultValue(0)
                .type(LayoutOptionData.Type.INT)
                .optionClass(Integer.class)
                .targets(EnumSet.of(LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.vector-signals")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.cellname")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.NODES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.celltype")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.NODES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.location-path")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.NODES, LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.src-location")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.NODES, LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );

        registry.register(new LayoutOptionData.Builder()
                .id("de.thkoeln.fentwums.netlist.backend.signalvalue")
                .defaultValue("")
                .type(LayoutOptionData.Type.STRING)
                .optionClass(String.class)
                .targets(EnumSet.of(LayoutOptionData.Target.EDGES))
                .visibility(LayoutOptionData.Visibility.VISIBLE)
                .create()
        );
    }
}
