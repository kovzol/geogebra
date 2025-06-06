/*
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by
the Free Software Foundation.

 */

/*
 * AlgoIncircle.java, dsun48 [6/26/2011]
 *
 */

package org.geogebra.common.kernel.advanced;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.SymbolicParametersBotanaAlgo;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoConicND;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.matrix.CoordSys;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.adapters.BotanaIncircle;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.util.MyMath;

public class AlgoIncircleCenter extends AlgoElement implements SymbolicParametersBotanaAlgo {

	// input
	private GeoPointND A;
	private GeoPointND B;
	private GeoPointND C;

	private GeoConicND circle;
	private GeoPointND incenter; // output

	private BotanaIncircle botanaParams;

	/**
	 * @param cons
	 *            construction
	 * @param A
	 *            vertex
	 * @param B
	 *            vertex
	 * @param C
	 *            vertex
	 */

	public AlgoIncircleCenter(Construction cons, GeoPointND A, GeoPointND B,
			GeoPointND C) {

		super(cons);

		this.A = A;
		this.B = B;
		this.C = C;

		int dim = MyMath.max(A.getDimension(), B.getDimension(),
				C.getDimension());
		circle = kernel.getGeoFactory().newConic(dim, cons);
		// output
		incenter = kernel.getGeoFactory().newPoint(dim, cons);
		// incenter.setLabel("inc");
		setInputOutput();

		compute();
	}

	@Override
	public Commands getClassName() {
		return Commands.IncircleCenter;
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		input = new GeoElement[3];
		input[0] = (GeoElement) A;
		input[1] = (GeoElement) B;
		input[2] = (GeoElement) C;

		super.setOutputLength(1);
		super.setOutput(0, incenter.toGeoElement());
		setDependencies(); // done by AlgoElement
	}

	public GeoPointND getIncenter() {
		return incenter;
	}

	// compute incircle of triangle A, B, C
	@Override
	public void compute() {
		if (!A.isDefined() || !B.isDefined() || !C.isDefined()) {
			circle.setUndefined();
			return;
		}
		double dAB = A.distance(B);
		double dAC = A.distance(C);
		double dBC = B.distance(C);
		double s = (dAB + dAC + dBC) / 2;
		double wA = dBC / s / 2;
		double wB = dAC / s / 2;
		double wC = dAB / s / 2;
		GeoPoint.setBarycentric(A, B, C, wA, wB, wC, 1, incenter);
		incenter.update();
		double radius = Math.sqrt((s - dBC) * (s - dAC) / s * (s - dAB));

		CoordSys sys = circle.getCoordSys();
		if (sys != CoordSys.Identity3D) {
			sys.resetCoordSys();
			sys.addPoint(A.getInhomCoordsInD3());
			sys.addPoint(B.getInhomCoordsInD3());
			sys.addPoint(C.getInhomCoordsInD3());
			sys.makeOrthoMatrix(false, false);
			circle.setSphereND(incenter.getCoordsInD2(sys), radius);
		} else {
			circle.setSphereND(incenter, radius);
		}

	}

	@Override
	public String toString(StringTemplate tpl) {
		// Michael Borcherds 2008-03-30
		// simplified to allow better Chinese translation
		return getLoc().getPlainDefault("IncircleCenterOfTriangleABC",
				"Incircle center of triangle %0%1%2", A.getLabel(tpl),
				B.getLabel(tpl), C.getLabel(tpl));
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) {
		if (botanaParams == null) {
			botanaParams = new BotanaIncircle();
		}
		return botanaParams.getBotanaVars();
	}

	@Override
	public String[] getBotanaVarsDescr(GeoElementND geo) throws NoSymbolicParametersException {
		return botanaParams.getBotanaVarsDescr();
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {

		if (botanaParams == null) {
			botanaParams = new BotanaIncircle();
		}
		return botanaParams.getPolynomials(getInput());
	}

	@Override
	public int getRelatedModeID() {
		return EuclidianConstants.MODE_INCIRCLE_CENTER;
	}


	@Override
	public void reset() {
		if (botanaParams != null) {
			botanaParams.reset();
		}
	}

}
