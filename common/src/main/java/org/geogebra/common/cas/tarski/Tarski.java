package org.geogebra.common.cas.tarski;

import org.geogebra.common.util.debug.Log;

/**
 * Maintains an embedded Tarski system.
 * @author Zoltan Kovacs <zoltan@geogebra.org>
 */
public class Tarski {

	public static String eval(String command) {
		String response = "";
		response = tarski.TARSKIEVAL(command);
		return response;
	}

	public static boolean init(int numcells, int timeout) {
		System.loadLibrary("tarski");
		tarski.TARSKIINIT(numcells, timeout);
		return true;
	}

}
