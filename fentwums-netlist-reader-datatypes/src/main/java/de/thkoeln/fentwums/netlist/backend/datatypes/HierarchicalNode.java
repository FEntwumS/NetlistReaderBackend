package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class HierarchicalNode {
	private String hName;
	private int lId;
	private HierarchicalNode parent;
	private HashMap<String, HierarchicalNode> children;
	private boolean isLeaf;
	private ArrayList<Vector> vectors;
	private HashMap<Integer, Bundle> possibleBundles;
	private ElkNode node;
	private HashMap<String, ElkNode> constantDrivers;
	private ArrayList<ElkNode> childList;
	private ArrayList<ElkEdge> edgeList;
	private ArrayList<Integer> currentlyBundledSignals;
	private String path;

	private static Logger logger = LoggerFactory.getLogger(HierarchicalNode.class);

	public HierarchicalNode() {
		hName = "";
		lId = 0;
		parent = null;
		children = new HashMap<String, HierarchicalNode>(8);
		isLeaf = true;
		vectors = new ArrayList<>(8);
		possibleBundles = new HashMap<>(8);
		node = null;
		constantDrivers = new HashMap<>();
		currentlyBundledSignals = new ArrayList<>(8);
	}

	public HierarchicalNode(String hName, HierarchicalNode parent, HashMap<String, HierarchicalNode> children,
							ArrayList<Vector> vectors, HashMap<Integer, Bundle> possibleBundles, ElkNode node) {
		this.hName = hName;
		this.parent = parent;
		this.children = children;
		this.vectors = vectors;
		this.possibleBundles = possibleBundles;
		this.node = node;
		constantDrivers = new HashMap<>();

		this.isLeaf = children == null || children.isEmpty();

		if(parent != null) {
			parent.getChildren().put(hName, this);
		}

		currentlyBundledSignals = new ArrayList<>(8);
	}

	public String getHName() {
		return hName;
	}

	public void setHName(String hName) {
		this.hName = hName;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public void setIsLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}

	public int getLId() {
		return lId;
	}

	public void setLId(int lId) {
		this.lId = lId;
	}

	public HierarchicalNode getParent() {
		return parent;
	}

	public void setParent(HierarchicalNode parent) {
		this.parent = parent;
	}

	public HashMap<String, HierarchicalNode> getChildren() {
		return children;
	}

	public void setChildren(HashMap<String, HierarchicalNode> children) {
		this.children = children;
	}

	public ArrayList<Vector> getVectors() {
		return vectors;
	}

	public void setVectors(ArrayList<Vector> vectors) {
		this.vectors = vectors;
	}

	public HashMap<Integer, Bundle> getPossibleBundles() {
		return possibleBundles;
	}

	public void setPossibleBundles(HashMap<Integer, Bundle> possibleBundles) {
		this.possibleBundles = possibleBundles;
	}

	public ElkNode getNode() {
		return node;
	}

	public void setNode(ElkNode node) {
		this.node = node;
	}

	public HashMap<String, ElkNode> getConstantDrivers() {
		return constantDrivers;
	}

	public void setConstantDrivers(HashMap<String, ElkNode> constantDrivers) {
		this.constantDrivers = constantDrivers;
	}

	public ArrayList<ElkNode> getChildList() {
		return childList;
	}

	public void setChildList(ArrayList<ElkNode> childList) {
		this.childList = childList;
	}

	public ArrayList<ElkEdge> getEdgeList() {
		return edgeList;
	}

	public void setEdgeList(ArrayList<ElkEdge> edgeList) {
		this.edgeList = edgeList;
	}

	public ArrayList<Integer> getCurrentlyBundledSignals() {
		return currentlyBundledSignals;
	}

	public void setCurrentlyBundledSignals(ArrayList<Integer> currentlyBundledSignals) {
		this.currentlyBundledSignals = currentlyBundledSignals;
	}

	public String getAbsolutePath() {
		if (path != null) {
			return path;
		}

		if (this.getParent() != null) {
			for (String candidate : this.getParent().getChildren().keySet()) {
				if (this.getParent().getChildren().get(candidate).equals(this)) {
					String ret = this.getParent().getAbsolutePath() + " " + candidate;
					path = ret;

					return ret;
				}
			}

			logger.atError().setMessage("Parent {} does not know its child {}").addArgument(this.getParent().getHName()).addArgument(this.getHName()).log();
			return "";
		}
		return this.getHName();
	}
}
