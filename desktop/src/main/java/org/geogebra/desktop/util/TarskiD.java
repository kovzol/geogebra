package org.geogebra.desktop.util;

import tarski.*;
import org.geogebra.common.util.Tarski;

/**
 * Implements desktop dependent parts of Tarski.
 *
 * @author Zoltan Kovacs
 *
 */
public class TarskiD extends Tarski {

	@Override
	public String eval(String command) {
		String response = "";
		response = tarski.TARSKIEVAL(command);
		return response;
	}

	@Override
	public boolean init(int timeout) {
		System.loadLibrary("tarski");
		tarski.TARSKIINIT(50000000, timeout);
		return true;
	}

}