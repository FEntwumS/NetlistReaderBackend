package de.thkoeln.fentwums.netlist.backend.datatypes;

import java.util.HashMap;

public class Bundle {
	/**
	 * Index of the signal the bundle has been created around.
	 */
	private int bundleSignalIndex;

	/**
	 * List of all the signal indices contained in this bundle.
	 */
	private HashMap<Integer, Integer> bundleSignalMap;

	public Bundle() {
		bundleSignalIndex = 0;
		bundleSignalMap = new HashMap<Integer, Integer>(32);
	}

	public Bundle(int bundleSignalIndex) {
		this.bundleSignalIndex = bundleSignalIndex;
		bundleSignalMap = new HashMap<Integer, Integer>(32);
	}

	public Bundle(int bundleSignalIndex, HashMap<Integer, Integer> bundleSignalMap) {
		this.bundleSignalIndex = bundleSignalIndex;
		this.bundleSignalMap = bundleSignalMap;
	}

	public Bundle(Bundle bundle) {
		if (bundle == null) {
			throw new NullPointerException();
		}

		this.bundleSignalIndex = bundle.getBundleSignalIndex();
		this.bundleSignalMap = new HashMap<Integer, Integer>(bundle.getBundleSignalMap());
	}

	public int getBundleSignalIndex() {
		return bundleSignalIndex;
	}

	public void setBundleSignalIndex(int bundleSignalIndex) {
		this.bundleSignalIndex = bundleSignalIndex;
	}

	public HashMap<Integer, Integer> getBundleSignalMap() {
		return bundleSignalMap;
	}

	public void setBundleSignalMap(HashMap<Integer, Integer> bundleSignalMap) {
		this.bundleSignalMap = bundleSignalMap;
	}
}
