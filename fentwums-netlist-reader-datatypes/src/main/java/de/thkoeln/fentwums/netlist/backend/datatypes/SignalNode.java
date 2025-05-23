package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SignalNode {
	private String sName;
	private SignalNode hParent;
	private HashMap<String, SignalNode> hChildren;
	private SignalNode sParent;
	private HashMap<String, SignalNode> sChildren;
	private boolean sVisited;
	private boolean isSource;
	private List<ElkPort> inPorts;
	private ElkPort outPort;
	private int indexInSignal;
	private String srcLocation;

	private static Logger logger = LoggerFactory.getLogger(SignalNode.class);

	public SignalNode() {
		sName = "";
		hParent = null;
		hChildren = new HashMap<String, SignalNode>(8);
		sParent = null;
		sChildren = new HashMap<String, SignalNode>(8);
		sVisited = false;
	}

	public SignalNode(String sName, SignalNode hParent, HashMap<String, SignalNode> hChildren, SignalNode sParent,
					  HashMap<String, SignalNode> sChildren,
					  boolean isSource, List<ElkPort> inPorts, ElkPort outPort) {
		this.sName = sName;
		this.hParent = hParent;
		this.hChildren = hChildren;
		this.sParent = sParent;
		this.sChildren = sChildren;
		this.setInPorts(inPorts);
		this.setOutPort(outPort);

		if (hParent != null && hParent.getHChildren() != null) {
			hParent.getHChildren().put(this.sName, this);
		}

		this.isSource = isSource;

		this.sVisited = false;
	}

	public String getSName() {
		return sName;
	}

	public void setSName(String sName) {
		this.sName = sName;
	}

	public SignalNode getHParent() {
		return hParent;
	}

	public void setHParent(SignalNode hParent) {
		this.hParent = hParent;
	}

	public HashMap<String, SignalNode> getHChildren() {
		return hChildren;
	}

	public void setHChildren(HashMap<String, SignalNode> hChildren) {
		this.hChildren = hChildren;
	}

	public SignalNode getSParent() {
		return sParent;
	}

	public void setSParent(SignalNode sParent) {
		this.sParent = sParent;
	}

	public HashMap<String, SignalNode> getSChildren() {
		return sChildren;
	}

	public void setSChildren(HashMap<String, SignalNode> sChildren) {
		this.sChildren = sChildren;
	}

	public boolean getSVisited() {
		return sVisited;
	}

	public void setSVisited(boolean sVisited) {
		this.sVisited = sVisited;
	}

	public boolean getIsSource() {
		return isSource;
	}

	public void setIsSource(boolean isSource) {
		this.isSource = isSource;
	}

	public List<ElkPort> getInPorts() {
		return inPorts;
	}

	public void setInPorts(List<ElkPort> inPorts) {
		if (this.inPorts != null) {
			this.inPorts.clear();
		} else if (inPorts != null) {
			this.inPorts = new ArrayList<>(inPorts.size());
		} else {
			this.inPorts = new ArrayList<>(1);
			return;
		}

		for (ElkPort p : inPorts) {
			if (p == null) {
				continue;
			}

			if (p.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
				logger.error("Trying to set inPort as outPort");

				continue;
			}

			this.inPorts.add(p);
		}
	}

	public ElkPort getOutPort() {
		return outPort;
	}

	public void addInPort(ElkPort inPort) {
		if (inPort == null) {
			return;
		}

		if (this.inPorts == null) {
			this.inPorts = new ArrayList<ElkPort>();
		}

		if (inPort.getProperty(CoreOptions.PORT_SIDE) == PortSide.EAST) {
			logger.error("Trying to add inPort as outPort");
		}

		this.inPorts.add(inPort);
	}

	public void setOutPort(ElkPort outPort) {
		if(outPort != null && outPort.getProperty(CoreOptions.PORT_SIDE) != PortSide.EAST) {
			logger.error("Trying to set outPort as inPort");

			return;
		}

		this.outPort = outPort;
	}

	public int getIndexInSignal() {
		return indexInSignal;
	}

	public void setIndexInSignal(int indexInSignal) {
		this.indexInSignal = indexInSignal;
	}

	public String getAbsolutePath() {

		if (this.getHParent() != null) {
			for (String candidate : this.getHParent().getHChildren().keySet()) {
				if (this.getHParent().getHChildren().get(candidate).equals(this)) {
					String ret = this.getHParent().getAbsolutePath() + " " + candidate;

					return ret;
				}
			}

			logger.atError().setMessage("hParent {} does not know its child {}").addArgument(this.getHParent().getSName()).addArgument(this.getSName()).log();
		}
		return "";
	}

	public void setSrcLocation(String srcLocation) {
		this.srcLocation = srcLocation;
	}

	public String getSrcLocation() {
		return srcLocation;
	}
}
