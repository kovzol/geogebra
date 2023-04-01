package org.geogebra.common.util;

/**
 * Tarski package for GeoGebra.
 */

public abstract class Tarski {

	public Tarski() {
	}

	public boolean init(int timeout) {
		// Will be overridden by web and desktop
		return false;
	}

	public String eval(String command) {
		// Will be overridden by web and desktop
		return "?";
	}

	public MaxSizeHashMap<String, String> tarskiResultCache = new MaxSizeHashMap<>(5000);

	public String evalCached (String command) {
		if (tarskiResultCache.containsKey(command)) {
			return tarskiResultCache.get(command);
		}
		String ret = eval(command);

		// Handle erroneous results:
		if (ret.contains("Exception")) {
			ret = ""; // TODO: consider calling reinit()
		}

		tarskiResultCache.put(command, ret);
		return ret;
	}

	public boolean reinit(int timeout) {
		// Will be overridden by web and desktop
		return false;
	}

	public boolean end() {
		// Will be overridden by web and desktop
		return false;
	}

}
