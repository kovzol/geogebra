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

	private PPolynomial[] botanaPolynomials;
	private PVariable[] botanaVars;

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
	public PVariable[] getBotanaVars(GeoElementND geo) throws NoSymbolicParametersException {
		return botanaVars;
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		if (botanaPolynomials != null) {
			return botanaPolynomials;
		}

		if (A != null && B != null && C != null) {
			GeoPoint A1 = ((GeoPoint) A.toGeoElement());
			GeoPoint B1 = ((GeoPoint) B.toGeoElement());
			GeoPoint C1 = ((GeoPoint) C.toGeoElement());
			PVariable[] vA = A1.getBotanaVars(A1);
			PVariable[] vB = B1.getBotanaVars(B1);
			PVariable[] vC = C1.getBotanaVars(C1);

			if (botanaVars == null) {
				botanaVars = new PVariable[8];
				// I, the incenter
				botanaVars[0] = new PVariable(kernel);
				botanaVars[1] = new PVariable(kernel);
				// Fa, the feet of I projected on BC
				botanaVars[2] = new PVariable(kernel);
				botanaVars[3] = new PVariable(kernel);
				// Fb, the feet of I projected on AC
				botanaVars[4] = new PVariable(kernel);
				botanaVars[5] = new PVariable(kernel);
				// Fc, the feet of I projected on AB
				botanaVars[6] = new PVariable(kernel);
				botanaVars[7] = new PVariable(kernel);
			}

			botanaPolynomials = new PPolynomial[8];

			// IFa=IFb
			botanaPolynomials[0] = PPolynomial.equidistant(botanaVars[2], botanaVars[3],
					botanaVars[0], botanaVars[1], botanaVars[4], botanaVars[5]);
			// IFb=IFc
			botanaPolynomials[1] = PPolynomial.equidistant(botanaVars[4], botanaVars[5],
					botanaVars[0], botanaVars[1], botanaVars[6], botanaVars[7]);
			// A,Fb,C are collinear
			botanaPolynomials[2] = PPolynomial.collinear(vA[0], vA[1], botanaVars[4], botanaVars[5],
					vC[0], vC[1]);
			// A,Fc,B are collinear
			botanaPolynomials[3] = PPolynomial.collinear(vA[0], vA[1], botanaVars[6], botanaVars[7],
					vB[0], vB[1]);
			// B,Fa,C are collinear
			botanaPolynomials[4] = PPolynomial.collinear(vB[0], vB[1], botanaVars[2], botanaVars[3],
					vC[0], vC[1]);
			// AC is perpendicular to IFb
			botanaPolynomials[5] = PPolynomial.perpendicular(vA[0], vA[1], vC[0], vC[1],
					botanaVars[0], botanaVars[1], botanaVars[4], botanaVars[5]);
			// AB is perpendicular to IFc
			botanaPolynomials[6] = PPolynomial.perpendicular(vA[0], vA[1], vB[0], vB[1],
					botanaVars[0], botanaVars[1], botanaVars[6], botanaVars[7]);
			// BC is perpendicular to IFa
			botanaPolynomials[7] = PPolynomial.perpendicular(vB[0], vB[1], vC[0], vC[1],
					botanaVars[0], botanaVars[1], botanaVars[2], botanaVars[3]);
			return botanaPolynomials;

		}
		throw new NoSymbolicParametersException();

	}
}
