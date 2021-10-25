package org.geogebra.web.html5.util;

import org.geogebra.common.util.Tarski;

/**
 * @author Zoltan Kovacs
 *
 *         Implements web dependent parts of Tarski.
 */
public class TarskiW extends Tarski {

	@Override
	public String eval(String command) {
		String response = "";
		// response = tarski.TARSKIEVAL(command);
		return response;
	}

	@Override
	public boolean init(int numcells, int timeout) {
		// System.loadLibrary("tarski");
		// tarski.TARSKIINIT(numcells, timeout);
		return false;
	}


}