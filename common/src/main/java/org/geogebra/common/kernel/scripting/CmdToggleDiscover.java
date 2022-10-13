/*
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by
the Free Software Foundation.

 */

package org.geogebra.common.kernel.scripting;

import org.geogebra.common.euclidian.EuclidianViewInterfaceCommon;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.BooleanValue;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.arithmetic.NumberValue;
import org.geogebra.common.kernel.commands.CmdScripting;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.MyError;

/**
 * ShowAxex[]
 *
 * ShowAxes[&lt;Boolean>]
 *
 * ShowAxes[&lt;View ID>, &lt;Boolean]
 */
public class CmdToggleDiscover extends CmdScripting {

	/**
	 * Create new command processor
	 *
	 * @param kernel
	 *            kernel
	 */
	public CmdToggleDiscover(Kernel kernel) {
		super(kernel);
	}

	@Override
	protected final GeoElement[] perform(Command c) throws MyError {
		int n = c.getArgumentNumber();

		EuclidianViewInterfaceCommon ev = null;

		GeoElement[] arg = resArgs(c);
		switch (n) {
		case 0:
			app.getSettings().getEuclidian(1).stepwiseDiscovery(true);
			break;
		case 1:
			if (!(arg[0] instanceof BooleanValue)) {
				throw argErr(c, arg[0]);
			}

			boolean show = ((BooleanValue) arg[0]).getBoolean();
			app.getSettings().getEuclidian(1).stepwiseDiscovery(show);
			break;

		default:
			throw argNumErr(c);
		}
		return arg;
	}


}
