package de.thkoeln.fentwums.netlist.backend.elkoptions;

import org.eclipse.elk.core.data.ILayoutMetaDataProvider;
import org.eclipse.elk.core.data.LayoutOptionData;
import org.eclipse.elk.graph.properties.IProperty;
import org.eclipse.elk.graph.properties.Property;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Custom options provider
 */
public class FEntwumSOptions implements ILayoutMetaDataProvider {

	/**
	 * The name of the signal
	 */
	public static final IProperty<String> SIGNAL_NAME = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.signalname",
			"");

	/**
	 * The bitindex of the signal
	 */
	public final static IProperty<Integer> SIGNAL_INDEX = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.signalindex",
			0
	);

	/**
	 * The name of the cell
	 */
	public final static IProperty<String> CELL_NAME = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.cellname",
			""
	);

	/**
	 * The type of the cell. Constant drivers get assigned the type "Constant driver"
	 */
	public final static IProperty<String> CELL_TYPE = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.celltype",
			""
	);

	/**
	 * The location of the cell inside the hierarchy
	 */
	public final static IProperty<String> LOCATION_PATH = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.location-path",
			""
	);

	/**
	 * The source file and line of code that led to the creation of this cell
	 */
	public static final IProperty<String> SRC_LOCATION = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.src-location",
			""
	);

	/**
	 * The value of the associated signal
	 */
	public static final IProperty<String> SIGNAL_VALUE = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.signalvalue",
			""
	);

	/**
	 * The bit's index in the containing signal
	 */
	public static final IProperty<Integer> INDEX_IN_SIGNAL = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.index-in-signal",
			0
	);

	/**
	 * The signal type
	 */
	public static final IProperty<SignalType> SIGNAL_TYPE = new Property<SignalType>(
			"de.thkoeln.fentwums.netlist.backend.signaltype",
			SignalType.UNDEFINED
	);

	/**
	 * The name of the port's containing port group
	 */
	public static final IProperty<String> PORT_GROUP_NAME = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.port-group-name",
			""
	);

	/**
	 * The bitindices of bundled signals contained in the containing edge
	 */
	public static final IProperty<ArrayList<Integer>> BUNDLED_SIGNALS = new Property<ArrayList<Integer>>(
			"de.thkoeln.fentwums.netlist.backend.bundled-signals",
			null
	);

	public static final IProperty<Double> FONT_SIZE = new Property<Double>(
			"de.thkoeln.fentwums.netlist.backend.font-size",
			0.0d
	);

	public static final IProperty<Boolean> NOT_CONNECTED = new Property<Boolean>(
			"de.thkoeln.fentwums.netlist.backend.not-connected",
			false
	);

	public static final IProperty<Integer> INDEX_IN_PORT_GROUP = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.index-in-port-group",
			0
	);

	public static final IProperty<Integer> CANONICAL_INDEX_IN_PORT_GROUP = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.canonical-index-in-port-group",
			0
	);

	public static final IProperty<Integer> CANONICAL_BUNDLE_LOWER_INDEX_IN_PORT_GROUP = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.canonical-bundle-lower-index-in-port-group",
			0
	);

	public static final IProperty<Integer> CANONICAL_BUNDLE_UPPER_INDEX_IN_PORT_GROUP = new Property<Integer>(
			"de.thkoeln.fentwums.netlist.backend.canonical-bundle-upper-index-in-port-group",
			0
	);

	public static final IProperty<String> PORT_DIRECTION = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.hierarchy-view.port-direction",
			"UNKNOWN"
	);

	public static final IProperty<String> HIERARCHY_ANCESTOR_NAME = new Property<String>(
			"de.thkoeln.fentwums.netlist.backend.hierarchy-ancestor-name",
			"root"
	);

	public static final IProperty<HierarchyContainerSubNodeType> HIERARCHY_CONTAINER_SUB_NODE_TYPE = new Property<HierarchyContainerSubNodeType>(
			"de.thkoeln.fentwums.netlist.backend.hierarchy-container-sub-node-type",
			HierarchyContainerSubNodeType.TYPE
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

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.font-size")
				.defaultValue(10.0d)
				.type(LayoutOptionData.Type.DOUBLE)
				.optionClass(Double.class)
				.targets(EnumSet.of(LayoutOptionData.Target.LABELS))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.not-connected")
				.defaultValue(false)
				.type(LayoutOptionData.Type.BOOLEAN)
				.optionClass(Boolean.class)
				.targets(EnumSet.of(LayoutOptionData.Target.PORTS))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.index-in-port-group")
				.defaultValue(0)
				.type(LayoutOptionData.Type.INT)
				.optionClass(Integer.class)
				.targets(EnumSet.of(LayoutOptionData.Target.PORTS))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.canonical-index-in-port-group")
				.defaultValue(0)
				.type(LayoutOptionData.Type.INT)
				.optionClass(Integer.class)
				.targets(EnumSet.of(LayoutOptionData.Target.PORTS))
				.visibility(LayoutOptionData.Visibility.HIDDEN)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
              .id("de.thkoeln.fentwums.netlist.backend.canonical-bundle-lower-index-in-port-group")
              .defaultValue(0)
              .type(LayoutOptionData.Type.INT)
              .optionClass(Integer.class)
              .targets(EnumSet.of(LayoutOptionData.Target.PORTS))
              .visibility(LayoutOptionData.Visibility.HIDDEN)
              .create()
		);

		registry.register(new LayoutOptionData.Builder()
              .id("de.thkoeln.fentwums.netlist.backend.canonical-bundle-upper-index-in-port-group")
              .defaultValue(0)
              .type(LayoutOptionData.Type.INT)
              .optionClass(Integer.class)
              .targets(EnumSet.of(LayoutOptionData.Target.PORTS))
              .visibility(LayoutOptionData.Visibility.HIDDEN)
              .create()
		);

		registry.register(new LayoutOptionData.Builder()
				.id("de.thkoeln.fentwums.netlist.backend.hierarchy-view.port-direction")
				.defaultValue("UNKNOWN")
				.type(LayoutOptionData.Type.STRING)
				.optionClass(String.class)
				.targets(EnumSet.of(LayoutOptionData.Target.PORTS))
				.visibility(LayoutOptionData.Visibility.VISIBLE)
				.create()
		);

		registry.register(new LayoutOptionData.Builder()
			  .id("de.thkoeln.fentwums.netlist.backend.hierarchy-ancestor-name")
			  .defaultValue("root")
			  .type(LayoutOptionData.Type.STRING)
			  .optionClass(String.class)
			  .targets(EnumSet.of(LayoutOptionData.Target.NODES))
			  .visibility(LayoutOptionData.Visibility.VISIBLE)
			  .create()
		);

		registry.register(new LayoutOptionData.Builder()
			  .id("de.thkoeln.fentwums.netlist.backend.hierarchy-container-sub-node-type")
			  .defaultValue(HierarchyContainerSubNodeType.TYPE)
			  .type(LayoutOptionData.Type.ENUM)
			  .optionClass(HierarchyContainerSubNodeType.class)
			  .targets(EnumSet.of(LayoutOptionData.Target.NODES))
			  .visibility(LayoutOptionData.Visibility.VISIBLE)
			  .create()
		);
	}
}
