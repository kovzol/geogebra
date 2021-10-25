package org.geogebra.common.util;

/**
 * Tarski package for GeoGebra.
 */

public abstract class Tarski {

	public Tarski() {
	}

	public boolean init(int numcells, int timeout) {
		// Will be overridden by web and desktop
		return false;
	}

	public String eval(String command) {
		// Will be overridden by web and desktop
		return "?";
	}

}
