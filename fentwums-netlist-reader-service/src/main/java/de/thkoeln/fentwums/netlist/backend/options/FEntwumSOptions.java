package de.thkoeln.fentwums.netlist.backend.options;

import org.eclipse.elk.core.data.ILayoutMetaDataProvider;
import org.eclipse.elk.core.data.LayoutOptionData;
import org.eclipse.elk.graph.properties.IProperty;
import org.eclipse.elk.graph.properties.Property;

import java.util.ArrayList;
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

	public static final IProperty<Integer> INDEX_IN_SIGNAL = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.index-in-signal",
			0
	);

	public static final IProperty<SignalType> SIGNAL_TYPE = new Property<SignalType>(
			"de.thkoeln.fentwums.netlist.backend.signaltype",
			SignalType.UNDEFINED
	);

	public static final IProperty<String> PORT_GROUP_NAME = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.port-group-name",
			""
	);

	public static final IProperty<ArrayList<Integer>> BUNDLED_SIGNALS = new Property<ArrayList<Integer>>(
			"de.thkoeln.fentwums.netlist.backend.bundled-signals",
			null
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

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.index-in-signal")
				.defaultValue(0)
				.type(LayoutOptionData.Type.INT)
				.optionClass(Integer.class)
				.targets(EnumSet.of(LayoutOptionData.Target.EDGES))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.signaltype")
				.defaultValue(SignalType.UNDEFINED)
				.type(LayoutOptionData.Type.ENUM)
				.optionClass(SignalType.class)
				.targets(EnumSet.of(LayoutOptionData.Target.EDGES))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.port-group-name")
				.defaultValue("")
				.type(LayoutOptionData.Type.STRING)
				.optionClass(String.class)
				.targets(EnumSet.of(LayoutOptionData.Target.PORTS))
				.visibility(LayoutOptionData.Visibility.HIDDEN)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.bundled-signals")
				.defaultValue(null)
				.type(LayoutOptionData.Type.OBJECT)
				.optionClass(ArrayList.class)
				.targets(EnumSet.of(LayoutOptionData.Target.PORTS))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);
	}
}
