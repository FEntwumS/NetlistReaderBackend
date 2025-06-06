package de.thkoeln.fentwums.netlist.backend.interfaces;

import de.thkoeln.fentwums.netlist.backend.datatypes.NetInformation;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import org.eclipse.elk.graph.ElkNode;

import java.util.HashMap;

public interface IGraphCreator {
	public ElkNode createGraphFromNetlist(HashMap<String, Object> netlist, String moduleName, HashMap<String, Object> blackboxMap, NetlistCreationSettings settings);
	public String layoutGraph();
	public ICollapsableNode getRoot();
	public ElkNode getGraphRoot();

	HashMap<String, NetInformation> getNetInformationMap();
}
