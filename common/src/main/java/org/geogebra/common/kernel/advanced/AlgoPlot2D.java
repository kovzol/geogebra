/*
 GeoGebra - Dynamic Mathematics for Everyone
 http://www.geogebra.org

 This file is part of GeoGebra.

 This program is free software; you can redistribute it and/or modify it
 under the terms of the GNU General Public License as published by
 the Free Software Foundation.

 */
package org.geogebra.common.kernel.advanced;

import org.geogebra.common.euclidian.draw.DrawInequalityExternal;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Path;
import org.geogebra.common.kernel.PathNormalizer;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.ExpressionValue;
import org.geogebra.common.kernel.arithmetic.FunctionNVar;
import org.geogebra.common.kernel.arithmetic.FunctionVariable;
import org.geogebra.common.kernel.arithmetic.FunctionalNVar;
import org.geogebra.common.kernel.arithmetic.Inequality;
import org.geogebra.common.kernel.cas.UsesCAS;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.geos.GeoFunctionNVar;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.implicit.GeoImplicitCurve;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;

import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.debug.Log;

/**
 * Adapted from AlgoPerimeterPoly
 */

public class AlgoPlot2D extends AlgoElement implements UsesCAS {

	private GeoElement inp;
	private GeoElement outp;

	private FunctionNVar fnv;

	/**
	 * @param cons
	 *            construction
	 * @param function
	 *            the function to be plotted (via an external plotter)
	 */

	public AlgoPlot2D(Construction cons, String label, GeoElement function) {
		super(cons);

		this.inp = function;

		compute_init();
		setInputOutput();
		outp.setLabel(label); // don't forget this -- in that case no GeoElement will be saved

	}

	@Override
	public Commands getClassName() {
		return Commands.Plot2D;
	}

	@Override
	protected void setInputOutput() {
		input = new GeoElement[1];
		input[0] = inp;
		super.setOutputLength(1);
		super.setOutput(0, outp);
		setDependencies();
	}

	private void compute_init() {
		if (inp instanceof GeoFunctionNVar) {
			this.outp = inp.copy();
			// Force this object as an (external) polynomial one.
			((GeoFunctionNVar) outp).getFunction().setPolynomial(true);
			// Otherwise it will be still visualized via the built-in method in EuclidianDraw.
			// Instead, we use DrawInequalityExternal.
		} else {
			// Build the output accordingly. We store it as a proper input.
			fnv = new FunctionNVar(kernel, inp.getDefinition());
			FunctionVariable[] fv = null;
			if (inp instanceof GeoImplicitCurve) {
				fv = ((GeoImplicitCurve) inp).getFunctionDefinition().getFunctionVariables();
			}
			if (inp instanceof GeoLine) {
				fv = ((GeoLine) inp).getFunction().getFunctionVariables();
			}
			if (inp instanceof GeoConic) {
				fv = ((GeoConic) inp).getFunction().getFunctionVariables();
			}
			if (inp instanceof GeoFunction) {
				fv = ((GeoFunction) inp).getFunction().getFunctionVariables();
			}
			// Hopefully we handled all possible cases.
			fnv.setExpression(inp.getDefinition(), fv);
			fnv.setForceInequality(true);
			fnv.initFunction();
			outp = new GeoFunctionNVar(cons, fnv, true);
			// Force this object as an (external) polynomial one.
			((GeoFunctionNVar) outp).getFunction().setPolynomial(true);
		}

	}

	@Override
	public final void compute() {
		compute_init();
		super.setOutput(0, outp);
		cons.getApplication().getActiveEuclidianView().updateAllDrawables(true);

		// The trick is that we hacked the GeoFunctionNVar object. It will be
		// drawn by the kernel, so here is nothing to do!
		// DrawInequalityExternal die = new DrawInequalityExternal(kernel.getApplication().getActiveEuclidianView(), outp);
	}

}