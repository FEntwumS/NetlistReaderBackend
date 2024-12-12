package de.thkoeln.fentwums.netlist.backend.datatypes;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.options.SignalType;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkPort;

public class SignalTree {
	private int sId;
	private char sValue;
	private SignalNode hRoot;
	private SignalNode sRoot;   // ASSUMPTION: Every signal only has ONE source

	public SignalTree() {
		sId = 0;
		sValue = 'x';
		hRoot = null;
	}

	public SignalTree(int sId, char sValue, SignalNode hRoot) {
		this.sId = sId;
		this.sValue = sValue;
		this.hRoot = hRoot;
	}

	public int getSId() {
		return sId;
	}

	public void setSId(int sId) {
		this.sId = sId;
	}

	public char getSValue() {
		return sValue;
	}

	public void setSValue(char sValue) {
		this.sValue = sValue;

		this.updateSignalValue(this.hRoot);
	}

	public SignalNode getHRoot() {
		return hRoot;
	}

	public void setHRoot(SignalNode hRoot) {
		this.hRoot = hRoot;
	}

	public SignalNode getSRoot() {
		return sRoot;
	}

	public void setSRoot(SignalNode sRoot) {
		this.sRoot = sRoot;
	}

	public SignalNode getNodeAt(String path) {
		String[] pathSplit = path.trim().split(" ");
		SignalNode currentNode = hRoot;

		if (path.isEmpty()) {
			return currentNode;
		}

		for (String fragment : pathSplit) {
			currentNode = currentNode.getHChildren().get(fragment);

			if (currentNode == null) {
				return null;
			}
		}

		return currentNode;
	}

	private void updateSignalValue(SignalNode currentNode) {
		ElkPort currentPort = currentNode.getSPort();

		if (currentPort != null) {
			for (ElkEdge edge : currentPort.getOutgoingEdges()) {
				if (edge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.BUNDLED)) {
					continue;
				} else if (edge.getProperty(FEntwumSOptions.SIGNAL_TYPE).equals(SignalType.SINGLE)) {
					edge.setProperty(FEntwumSOptions.SIGNAL_VALUE, String.valueOf(this.sValue));
				}
			}
		}

		for (String child : currentNode.getHChildren().keySet()) {
			updateSignalValue(currentNode.getHChildren().get(child));
		}
	}
}
