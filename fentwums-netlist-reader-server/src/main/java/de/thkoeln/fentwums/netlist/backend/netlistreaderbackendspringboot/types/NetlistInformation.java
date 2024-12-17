package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot.types;

import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.helpers.SignalBundler;
import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;

import java.util.concurrent.locks.ReentrantLock;

public class NetlistInformation {
	private GraphCreator creator;
	private SignalBundler bundler;
	private CellCollapser collapser;
	public ReentrantLock lock = new ReentrantLock(true);

	public NetlistInformation(GraphCreator creator, SignalBundler bundler, CellCollapser collapser) {
		this.creator = creator;
		this.bundler = bundler;
		this.collapser = collapser;
	}

	public GraphCreator getCreator() {
		return creator;
	}

	public void setCreator(GraphCreator creator) {
		this.creator = creator;
	}

	public SignalBundler getBundler() {
		return bundler;
	}

	public void setBundler(SignalBundler bundler) {
		this.bundler = bundler;
	}

	public CellCollapser getCollapser() {
		return collapser;
	}

	public void setCollapser(CellCollapser collapser) {
		this.collapser = collapser;
	}
}
