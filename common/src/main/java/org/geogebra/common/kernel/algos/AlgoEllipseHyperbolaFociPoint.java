/*
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by
the Free Software Foundation.

 */

/*
 * AlgoEllipseFociPoint.java
 *
 * Ellipse with Foci A and B passing through point C
 *
 * Michael Borcherds
 * 2008-04-06
 * adapted from EllipseFociLength
 */

package org.geogebra.common.kernel.algos;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoConicND;
import org.geogebra.common.kernel.kernelND.GeoConicNDConstants;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

/**
 *
 * @author Markus
 */
public class AlgoEllipseHyperbolaFociPoint
		extends AlgoEllipseHyperbolaFociPointND
		implements SymbolicParametersBotanaAlgo {

	private PPolynomial[] botanaPolynomials;
	private PVariable[] botanaVars;

	/**
	 * @param cons
	 *            construction
	 * @param label
	 *            output label
	 * @param A
	 *            focus
	 * @param B
	 *            focus
	 * @param C
	 *            point on conic
	 * @param type
	 *            conic type
	 */
	public AlgoEllipseHyperbolaFociPoint(Construction cons, String label,
			GeoPointND A, GeoPointND B, GeoPointND C, final int type) {
		super(cons, label, A, B, C, null, type);
	}

	/**
	 * @param cons
	 *            construction
	 * @param A
	 *            focus
	 * @param B
	 *            focus
	 * @param C
	 *            point on conic
	 * @param type
	 *            conic type
	 */
	public AlgoEllipseHyperbolaFociPoint(Construction cons, GeoPointND A,
			GeoPointND B, GeoPointND C, final int type) {

		super(cons, A, B, C, null, type);

	}

	@Override
	protected GeoConicND newGeoConic(Construction cons1) {
		return new GeoConic(cons1);
	}

	@Override
	protected GeoPoint getA2d() {
		return (GeoPoint) getFocus1();
	}

	@Override
	protected GeoPoint getB2d() {
		return (GeoPoint) getFocus2();
	}

	@Override
	protected GeoPoint getC2d() {
		return (GeoPoint) getConicPoint();
	}

	// ///////////////////////////////
	// TRICKS FOR XOY PLANE
	// ///////////////////////////////

	@Override
	protected int getInputLengthForXML() {
		return getInputLengthForXMLMayNeedXOYPlane();
	}

	@Override
	protected int getInputLengthForCommandDescription() {
		return getInputLengthForCommandDescriptionMayNeedXOYPlane();
	}

	@Override
	public GeoElementND getInput(int i) {
		return getInputMaybeXOYPlane(i);
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) {
		return botanaVars;
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		if (botanaPolynomials != null) {
			return botanaPolynomials;
		}

		if (type == GeoConicNDConstants.CONIC_ELLIPSE) {

			GeoPoint F1 = getA2d();
			GeoPoint F2 = getB2d();
			GeoPoint Q = getC2d();

			if (F1 != null && F2 != null && Q != null) {
				PVariable[] vA = F1.getBotanaVars(F1);
				PVariable[] vB = F2.getBotanaVars(F2);
				PVariable[] vC = Q.getBotanaVars(Q);

				// if the 2 focus points are equal
				// handle the ellipse as a circle
				if (vA[0] == vB[0] && vA[1] == vB[1]) {
					if (botanaVars == null) {
						botanaVars = new PVariable[4];
						// A - center
						botanaVars[0] = vA[0];
						botanaVars[1] = vA[1];
						// C - point on the circle
						botanaVars[2] = vC[0];
						botanaVars[3] = vC[1];
					}
					return botanaPolynomials;
				}
				if (botanaVars == null) {
					botanaVars = new PVariable[8];
					// P - point of ellipse
					botanaVars[0] = new PVariable(kernel);
					botanaVars[1] = new PVariable(kernel);

					// A - focus point
					botanaVars[2] = vA[0];
					botanaVars[3] = vA[1];
					// B - focus point
					botanaVars[4] = vB[0];
					botanaVars[5] = vB[1];
					// C - point on ellipse
					botanaVars[6] = vC[0];
					botanaVars[7] = vC[1];
				}

				/*
				 * It seems faster to not store the parts of the sums ac+bc=ap+bp as extra variables, but
				 * compute the quartic polynomial in advance. It is a polynomial of px and py
				 * where P=(px,py) and A=(ax,ay), B=(bx,by), C=(cx,cy).
				 */
				// Giac provides these computations:
				// ee:=eliminate([ac^2=(ax-cx)^2+(ay-cy)^2,bc^2=(bx-cx)^2+(b_y-cy)^2,ap^2=(ax-px)^2+(ay-py)^2,bp^2=(bx-px)^2+(b_y-py)^2,ap+bp=ac+bc],[ac,bc,ap,bp])
				// print(ee):
				// ee:[cx^4*ay^4-4*ax*cx^3*ay^3*cy+6*ax^2*cx^2*ay^2*cy^2-4*ax^3*cx*ay*cy^3+ax^4*cy^4-4*ax^2*cx^3*ay^2*bx+4*ax*cx^4*ay^2*bx-4*cx^3*ay^4*bx+8*ax^3*cx^2*ay*cy*bx-8*ax^2*cx^3*ay*cy*bx+8*ax*cx^2*ay^3*cy*bx+4*cx^3*ay^3*cy*bx-4*ax^4*cx*cy^2*bx+4*ax^3*cx^2*cy^2*bx-4*ax^2*cx*ay^2*cy^2*bx-8*ax*cx^2*ay^2*cy^2*bx+4*ax^2*cx*ay*cy^3*bx+4*ax^2*cx^2*ay^2*bx^2-4*ax*cx^3*ay^2*bx^2-4*cx^4*ay^2*bx^2+4*cx^2*ay^4*bx^2-8*ax^3*cx*ay*cy*bx^2+8*ax^2*cx^2*ay*cy*bx^2+8*ax*cx^3*ay*cy*bx^2-8*ax*cx*ay^3*cy*bx^2-4*cx^2*ay^3*cy*bx^2+4*ax^3*cx*cy^2*bx^2-8*ax^2*cx^2*cy^2*bx^2-4*ax^2*ay^2*cy^2*bx^2+16*ax*cx*ay^2*cy^2*bx^2-2*cx^2*ay^2*cy^2*bx^2-4*ay^4*cy^2*bx^2+4*ax^2*ay*cy^3*bx^2-4*ax*cx*ay*cy^3*bx^2+8*ay^3*cy^3*bx^2-2*ax^2*cy^4*bx^2-4*ay^2*cy^4*bx^2+8*cx^3*ay^2*bx^3-16*ax*cx^2*ay*cy*bx^3+4*ax^2*cx*cy^2*bx^3+4*ax*cx^2*cy^2*bx^3-4*cx*ay^2*cy^2*bx^3+4*cx*ay*cy^3*bx^3-4*cx^2*ay^2*bx^4+8*ax*cx*ay*cy*bx^4-4*ax*cx*cy^2*bx^4+4*ay^2*cy^2*bx^4-4*ay*cy^3*bx^4+cy^4*bx^4-4*ax^2*cx^2*ay^2*cy*b_y+4*ax*cx^3*ay^2*cy*b_y-4*cx^2*ay^4*cy*b_y+8*ax^3*cx*ay*cy^2*b_y-8*ax^2*cx^2*ay*cy^2*b_y+8*ax*cx*ay^3*cy^2*b_y+4*cx^2*ay^3*cy^2*b_y-4*ax^4*cy^3*b_y+4*ax^3*cx*cy^3*b_y-4*ax^2*ay^2*cy^3*b_y-8*ax*cx*ay^2*cy^3*b_y+4*ax^2*ay*cy^4*b_y+8*ax^4*cx*cy*bx*b_y-16*ax^3*cx^2*cy*bx*b_y+8*ax^2*cx^3*cy*bx*b_y+16*ax^2*cx*ay^2*cy*bx*b_y-16*ax*cx^2*ay^2*cy*bx*b_y-4*cx^3*ay^2*cy*bx*b_y+8*cx*ay^4*cy*bx*b_y-16*ax^2*cx*ay*cy^2*bx*b_y+24*ax*cx^2*ay*cy^2*bx*b_y-16*cx*ay^3*cy^2*bx*b_y-4*ax^2*cx*cy^3*bx*b_y+8*cx*ay^2*cy^3*bx*b_y+8*ax^2*cx^2*cy*bx^2*b_y-8*ax*cx^3*cy*bx^2*b_y+16*cx^2*ay^2*cy*bx^2*b_y-16*ax*cx*ay*cy^2*bx^2*b_y-8*cx^2*ay*cy^2*bx^2*b_y+4*ax^2*cy^3*bx^2*b_y+4*ax*cx*cy^3*bx^2*b_y-4*ay^2*cy^3*bx^2*b_y+4*ay*cy^4*bx^2*b_y-8*ax^2*cx*cy*bx^3*b_y+8*ax*cx^2*cy*bx^3*b_y-8*cx*ay^2*cy*bx^3*b_y+8*cx*ay*cy^2*bx^3*b_y-4*cx*cy^3*bx^3*b_y-4*ax^4*cx^2*b_y^2+8*ax^3*cx^3*b_y^2-4*ax^2*cx^4*b_y^2-4*ax^2*cx^2*ay^2*b_y^2+4*ax*cx^3*ay^2*b_y^2-2*cx^4*ay^2*b_y^2-8*ax^3*cx*ay*cy*b_y^2+16*ax^2*cx^2*ay*cy*b_y^2-4*ax*cx^3*ay*cy*b_y^2-8*ax*cx*ay^3*cy*b_y^2+4*cx^2*ay^3*cy*b_y^2+4*ax^4*cy^2*b_y^2-4*ax^3*cx*cy^2*b_y^2-2*ax^2*cx^2*cy^2*b_y^2+4*ax^2*ay^2*cy^2*b_y^2+8*ax*cx*ay^2*cy^2*b_y^2-8*cx^2*ay^2*cy^2*b_y^2-4*ax^2*ay*cy^3*b_y^2+8*ax*cx*ay*cy^3*b_y^2-4*ax^2*cy^4*b_y^2-4*ax^2*cx^3*bx*b_y^2+4*ax*cx^4*bx*b_y^2+4*cx^3*ay^2*bx*b_y^2-16*ax*cx^2*ay*cy*bx*b_y^2+4*cx^3*ay*cy*bx*b_y^2+16*ax^2*cx*cy^2*bx*b_y^2-8*ax*cx^2*cy^2*bx*b_y^2+8*cx*ay^2*cy^2*bx*b_y^2-8*cx*ay*cy^3*bx*b_y^2+4*ax^2*cx^2*bx^2*b_y^2-4*ax*cx^3*bx^2*b_y^2-4*cx^2*ay^2*bx^2*b_y^2+16*ax*cx*ay*cy*bx^2*b_y^2-4*cx^2*ay*cy*bx^2*b_y^2-4*ax^2*cy^2*bx^2*b_y^2-4*ax*cx*cy^2*bx^2*b_y^2+6*cx^2*cy^2*bx^2*b_y^2+4*ay^2*cy^2*bx^2*b_y^2-4*ay*cy^3*bx^2*b_y^2-4*ax^2*cx^2*cy*b_y^3+4*ax*cx^3*cy*b_y^3+4*cx^2*ay^2*cy*b_y^3-16*ax*cx*ay*cy^2*b_y^3+4*cx^2*ay*cy^2*b_y^3+8*ax^2*cy^3*b_y^3-8*ax^2*cx*cy*bx*b_y^3+8*ax*cx^2*cy*bx*b_y^3-4*cx^3*cy*bx*b_y^3-8*cx*ay^2*cy*bx*b_y^3+8*cx*ay*cy^2*bx*b_y^3+4*ax^2*cx^2*b_y^4-4*ax*cx^3*b_y^4+cx^4*b_y^4+8*ax*cx*ay*cy*b_y^4-4*cx^2*ay*cy*b_y^4-4*ax^2*cy^2*b_y^4+4*ax^2*cx^3*ay^2*px-4*ax*cx^4*ay^2*px-8*ax^3*cx^2*ay*cy*px+8*ax^2*cx^3*ay*cy*px+4*ax*cx^2*ay^3*cy*px+4*ax^4*cx*cy^2*px-4*ax^3*cx^2*cy^2*px-8*ax^2*cx*ay^2*cy^2*px-4*ax*cx^2*ay^2*cy^2*px+4*ax^3*ay*cy^3*px+8*ax^2*cx*ay*cy^3*px-4*ax^3*cy^4*px+4*ax^2*cx^2*ay^2*bx*px+4*cx^4*ay^2*bx*px+4*cx^2*ay^4*bx*px-16*ax^2*cx^2*ay*cy*bx*px-12*cx^2*ay^3*cy*bx*px+4*ax^4*cy^2*bx*px+4*ax^2*cx^2*cy^2*bx*px+12*ax^2*ay^2*cy^2*bx*px-8*ax*cx*ay^2*cy^2*bx*px+12*cx^2*ay^2*cy^2*bx*px+8*ay^4*cy^2*bx*px-12*ax^2*ay*cy^3*bx*px-16*ay^3*cy^3*bx*px+4*ax^2*cy^4*bx*px+8*ay^2*cy^4*bx*px-8*ax^2*cx*ay^2*bx^2*px+4*ax*cx^2*ay^2*bx^2*px-4*cx^3*ay^2*bx^2*px-8*cx*ay^4*bx^2*px+8*ax^3*ay*cy*bx^2*px+8*ax^2*cx*ay*cy*bx^2*px+8*ax*cx^2*ay*cy*bx^2*px-8*cx^3*ay*cy*bx^2*px+8*ax*ay^3*cy*bx^2*px+16*cx*ay^3*cy*bx^2*px-4*ax^3*cy^2*bx^2*px-8*ax^2*cx*cy^2*bx^2*px+4*ax*cx^2*cy^2*bx^2*px-8*ax*ay^2*cy^2*bx^2*px-4*ax*ay*cy^3*bx^2*px-8*cx*ay*cy^3*bx^2*px+4*ax*cy^4*bx^2*px-8*cx^2*ay^2*bx^3*px+16*cx^2*ay*cy*bx^3*px-4*ax^2*cy^2*bx^3*px-4*cx^2*cy^2*bx^3*px-12*ay^2*cy^2*bx^3*px+12*ay*cy^3*bx^3*px-4*cy^4*bx^3*px+8*cx*ay^2*bx^4*px-8*ax*ay*cy*bx^4*px-8*cx*ay*cy*bx^4*px+4*ax*cy^2*bx^4*px+4*cx*cy^2*bx^4*px-8*ax^4*cx*cy*b_y*px+16*ax^3*cx^2*cy*b_y*px-8*ax^2*cx^3*cy*b_y*px-8*ax^2*cx*ay^2*cy*b_y*px+12*ax*cx^2*ay^2*cy*b_y*px-8*ax^3*ay*cy^2*b_y*px+8*ax^2*cx*ay*cy^2*b_y*px-8*ax*cx^2*ay*cy^2*b_y*px-8*ax*ay^3*cy^2*b_y*px+12*ax^3*cy^3*b_y*px-8*ax^2*cx*cy^3*b_y*px+16*ax*ay^2*cy^3*b_y*px-8*ax*ay*cy^4*b_y*px-8*ax^4*cy*bx*b_y*px+8*ax^2*cx^2*cy*bx*b_y*px-16*ax^2*ay^2*cy*bx*b_y*px-4*cx^2*ay^2*cy*bx*b_y*px-8*ay^4*cy*bx*b_y*px+16*ax^2*ay*cy^2*bx*b_y*px+16*ax*cx*ay*cy^2*bx*b_y*px-8*cx^2*ay*cy^2*bx*b_y*px+16*ay^3*cy^2*bx*b_y*px-4*ax^2*cy^3*bx*b_y*px-8*ay*cy^4*bx*b_y*px+8*ax^2*cx*cy*bx^2*b_y*px-16*ax*cx^2*cy*bx^2*b_y*px+8*cx^3*cy*bx^2*b_y*px-8*cx*ay^2*cy*bx^2*b_y*px+16*ax*ay*cy^2*bx^2*b_y*px+8*cx*ay*cy^2*bx^2*b_y*px-12*ax*cy^3*bx^2*b_y*px+8*cx*cy^3*bx^2*b_y*px+8*ax^2*cy*bx^3*b_y*px-8*cx^2*cy*bx^3*b_y*px+8*ay^2*cy*bx^3*b_y*px-8*ay*cy^2*bx^3*b_y*px+4*cy^3*bx^3*b_y*px+8*ax^4*cx*b_y^2*px-8*ax^3*cx^2*b_y^2*px-4*ax^2*cx^3*b_y^2*px+4*ax*cx^4*b_y^2*px+8*ax^2*cx*ay^2*b_y^2*px-4*ax*cx^2*ay^2*b_y^2*px+8*ax^3*ay*cy*b_y^2*px-8*ax^2*cx*ay*cy*b_y^2*px-4*ax*cx^2*ay*cy*b_y^2*px+8*ax*ay^3*cy*b_y^2*px-12*ax^3*cy^2*b_y^2*px+12*ax*cx^2*cy^2*b_y^2*px-16*ax*ay^2*cy^2*b_y^2*px+8*ax*cy^4*b_y^2*px+4*ax^2*cx^2*bx*b_y^2*px-4*cx^4*bx*b_y^2*px-4*cx^2*ay^2*bx*b_y^2*px+12*cx^2*ay*cy*bx*b_y^2*px-8*ax^2*cy^2*bx*b_y^2*px-8*ax*cx*cy^2*bx*b_y^2*px-4*cx^2*cy^2*bx*b_y^2*px-16*ay^2*cy^2*bx*b_y^2*px+16*ay*cy^3*bx*b_y^2*px-8*ax^2*cx*bx^2*b_y^2*px+4*ax*cx^2*bx^2*b_y^2*px+4*cx^3*bx^2*b_y^2*px+8*cx*ay^2*bx^2*b_y^2*px-16*ax*ay*cy*bx^2*b_y^2*px-8*cx*ay*cy*bx^2*b_y^2*px+12*ax*cy^2*bx^2*b_y^2*px-8*cx*cy^2*bx^2*b_y^2*px+16*ax^2*cx*cy*b_y^3*px-12*ax*cx^2*cy*b_y^3*px+16*ax*ay*cy^2*b_y^3*px-16*ax*cy^3*b_y^3*px+8*ax^2*cy*bx*b_y^3*px+4*cx^2*cy*bx*b_y^3*px+8*ay^2*cy*bx*b_y^3*px-8*ay*cy^2*bx*b_y^3*px-8*ax^2*cx*b_y^4*px+4*ax*cx^2*b_y^4*px-8*ax*ay*cy*b_y^4*px+8*ax*cy^2*b_y^4*px-8*ax^2*cx^2*ay^2*px^2+4*ax*cx^3*ay^2*px^2-2*cx^2*ay^4*px^2+8*ax^3*cx*ay*cy*px^2+8*ax^2*cx^2*ay*cy*px^2-8*ax*cx^3*ay*cy*px^2-4*ax*cx*ay^3*cy*px^2+4*cx^2*ay^3*cy*px^2-4*ax^4*cy^2*px^2-4*ax^3*cx*cy^2*px^2+4*ax^2*cx^2*cy^2*px^2-2*ax^2*ay^2*cy^2*px^2+16*ax*cx*ay^2*cy^2*px^2-4*cx^2*ay^2*cy^2*px^2-4*ay^4*cy^2*px^2-4*ax^2*ay*cy^3*px^2-8*ax*cx*ay*cy^3*px^2+8*ay^3*cy^3*px^2+4*ax^2*cy^4*px^2-4*ay^2*cy^4*px^2+4*ax^2*cx*ay^2*bx*px^2-8*ax*cx^2*ay^2*bx*px^2-4*cx^3*ay^2*bx*px^2+4*cx*ay^4*bx*px^2-8*ax^3*ay*cy*bx*px^2+8*ax^2*cx*ay*cy*bx*px^2+8*ax*cx^2*ay*cy*bx*px^2+8*cx^3*ay*cy*bx*px^2-8*ax*ay^3*cy*bx*px^2-4*cx*ay^3*cy*bx*px^2-4*ax^3*cy^2*bx*px^2+4*ax^2*cx*cy^2*bx*px^2-8*ax*cx^2*cy^2*bx*px^2-8*cx*ay^2*cy^2*bx*px^2+16*ax*ay*cy^3*bx*px^2+8*cx*ay*cy^3*bx*px^2-8*ax*cy^4*bx*px^2+4*ax^2*ay^2*bx^2*px^2+4*ax*cx*ay^2*bx^2*px^2+16*cx^2*ay^2*bx^2*px^2+4*ay^4*bx^2*px^2-16*ax^2*ay*cy*bx^2*px^2-16*ax*cx*ay*cy*bx^2*px^2-16*cx^2*ay*cy*bx^2*px^2-12*ay^3*cy*bx^2*px^2+16*ax^2*cy^2*bx^2*px^2+4*ax*cx*cy^2*bx^2*px^2+4*cx^2*cy^2*bx^2*px^2+22*ay^2*cy^2*bx^2*px^2-12*ay*cy^3*bx^2*px^2+4*cy^4*bx^2*px^2-8*cx*ay^2*bx^3*px^2+16*ax*ay*cy*bx^3*px^2-4*ax*cy^2*bx^3*px^2-4*cx*cy^2*bx^3*px^2-4*ay^2*bx^4*px^2+8*ay*cy*bx^4*px^2-4*cy^2*bx^4*px^2+8*ax^4*cy*b_y*px^2-16*ax^2*cx^2*cy*b_y*px^2+8*ax*cx^3*cy*b_y*px^2+12*ax^2*ay^2*cy*b_y*px^2-4*ax*cx*ay^2*cy*b_y*px^2-4*cx^2*ay^2*cy*b_y*px^2+4*ay^4*cy*b_y*px^2-8*ax*cx*ay*cy^2*b_y*px^2+8*cx^2*ay*cy^2*b_y*px^2-4*ay^3*cy^2*b_y*px^2-12*ax^2*cy^3*b_y*px^2+8*ax*cx*cy^3*b_y*px^2-8*ay^2*cy^3*b_y*px^2+8*ay*cy^4*b_y*px^2+16*ax^3*cy*bx*b_y*px^2-16*ax^2*cx*cy*bx*b_y*px^2+8*ax*cx^2*cy*bx*b_y*px^2-8*cx^3*cy*bx*b_y*px^2+16*ax*ay^2*cy*bx*b_y*px^2+12*cx*ay^2*cy*bx*b_y*px^2-40*ax*ay*cy^2*bx*b_y*px^2-8*cx*ay*cy^2*bx*b_y*px^2+16*ax*cy^3*bx*b_y*px^2-8*cx*cy^3*bx*b_y*px^2-16*ax^2*cy*bx^2*b_y*px^2+8*ax*cx*cy*bx^2*b_y*px^2+8*cx^2*cy*bx^2*b_y*px^2-8*ay^2*cy*bx^2*b_y*px^2-4*cy^3*bx^2*b_y*px^2-8*ax*cy*bx^3*b_y*px^2+8*cx*cy*bx^3*b_y*px^2-4*ax^4*b_y^2*px^2-8*ax^3*cx*b_y^2*px^2+16*ax^2*cx^2*b_y^2*px^2-4*ax*cx^3*b_y^2*px^2-4*ax^2*ay^2*b_y^2*px^2-4*ax*cx*ay^2*b_y^2*px^2+4*cx^2*ay^2*b_y^2*px^2-8*ax^2*ay*cy*b_y^2*px^2+12*ax*cx*ay*cy*b_y^2*px^2-4*cx^2*ay*cy*b_y^2*px^2-4*ay^3*cy*b_y^2*px^2+22*ax^2*cy^2*b_y^2*px^2-8*ax*cx*cy^2*b_y^2*px^2-4*cx^2*cy^2*b_y^2*px^2+16*ay^2*cy^2*b_y^2*px^2-8*ay*cy^3*b_y^2*px^2-4*cy^4*b_y^2*px^2+4*ax^2*cx*bx*b_y^2*px^2-8*ax*cx^2*bx*b_y^2*px^2+4*cx^3*bx*b_y^2*px^2-4*cx*ay^2*bx*b_y^2*px^2+16*ax*ay*cy*bx*b_y^2*px^2-4*cx*ay*cy*bx*b_y^2*px^2+16*cx*cy^2*bx*b_y^2*px^2+4*ax^2*bx^2*b_y^2*px^2+4*ax*cx*bx^2*b_y^2*px^2-8*cx^2*bx^2*b_y^2*px^2-4*ay^2*bx^2*b_y^2*px^2+12*ay*cy*bx^2*b_y^2*px^2-2*cy^2*bx^2*b_y^2*px^2-12*ax^2*cy*b_y^3*px^2-4*ax*cx*cy*b_y^3*px^2+4*cx^2*cy*b_y^3*px^2-4*ay^2*cy*b_y^3*px^2-4*ay*cy^2*b_y^3*px^2+8*cy^3*b_y^3*px^2-8*ax*cy*bx*b_y^3*px^2-4*cx*cy*bx*b_y^3*px^2+4*ax^2*b_y^4*px^2+4*ax*cx*b_y^4*px^2-2*cx^2*b_y^4*px^2+4*ay*cy*b_y^4*px^2-4*cy^2*b_y^4*px^2+4*ax^2*cx*ay^2*px^3+4*ax*cx^2*ay^2*px^3-16*ax^2*cx*ay*cy*px^3+4*ax*ay^3*cy*px^3+8*ax^3*cy^2*px^3-4*ax*ay^2*cy^2*px^3-4*ax^2*ay^2*bx*px^3-4*cx^2*ay^2*bx*px^3-4*ay^4*bx*px^3+16*ax^2*ay*cy*bx*px^3+12*ay^3*cy*bx*px^3-8*ax^2*cy^2*bx*px^3-12*ay^2*cy^2*bx*px^3-4*ax*ay^2*bx^2*px^3-4*cx*ay^2*bx^2*px^3+16*cx*ay*cy*bx^2*px^3-8*ax*cy^2*bx^2*px^3+8*ay^2*bx^3*px^3-16*ay*cy*bx^3*px^3+8*cy^2*bx^3*px^3-16*ax^3*cy*b_y*px^3+16*ax^2*cx*cy*b_y*px^3-12*ax*ay^2*cy*b_y*px^3+16*ax*ay*cy^2*b_y*px^3-4*ay^2*cy*bx*b_y*px^3+16*ay*cy^2*bx*b_y*px^3+16*ax*cy*bx^2*b_y*px^3-16*cx*cy*bx^2*b_y*px^3+8*ax^3*b_y^2*px^3-4*ax^2*cx*b_y^2*px^3-4*ax*cx^2*b_y^2*px^3+4*ax*ay^2*b_y^2*px^3-4*ax*ay*cy*b_y^2*px^3-12*ax*cy^2*b_y^2*px^3-4*ax^2*bx*b_y^2*px^3+4*cx^2*bx*b_y^2*px^3+4*ay^2*bx*b_y^2*px^3-12*ay*cy*bx*b_y^2*px^3-4*cy^2*bx*b_y^2*px^3-4*ax*bx^2*b_y^2*px^3+4*cx*bx^2*b_y^2*px^3+12*ax*cy*b_y^3*px^3+4*cy*bx*b_y^3*px^3-4*ax*b_y^4*px^3-4*ax*cx*ay^2*px^4+ay^4*px^4+8*ax*cx*ay*cy*px^4-4*ay^3*cy*px^4-4*ax^2*cy^2*px^4+4*ay^2*cy^2*px^4+4*ax*ay^2*bx*px^4+4*cx*ay^2*bx*px^4-8*ax*ay*cy*bx*px^4-8*cx*ay*cy*bx*px^4+8*ax*cy^2*bx*px^4-4*ay^2*bx^2*px^4+8*ay*cy*bx^2*px^4-4*cy^2*bx^2*px^4+8*ax^2*cy*b_y*px^4-8*ax*cx*cy*b_y*px^4+4*ay^2*cy*b_y*px^4-8*ay*cy^2*b_y*px^4-8*ax*cy*bx*b_y*px^4+8*cx*cy*bx*b_y*px^4-4*ax^2*b_y^2*px^4+4*ax*cx*b_y^2*px^4-2*ay^2*b_y^2*px^4+4*ay*cy*b_y^2*px^4+4*cy^2*b_y^2*px^4+4*ax*bx*b_y^2*px^4-4*cx*bx*b_y^2*px^4-4*cy*b_y^3*px^4+b_y^4*px^4+4*ax*cx^3*ay^3*py-4*cx^4*ay^3*py-8*ax^2*cx^2*ay^2*cy*py+8*ax*cx^3*ay^2*cy*py+4*cx^2*ay^4*cy*py+4*ax^3*cx*ay*cy^2*py-4*ax^2*cx^2*ay*cy^2*py-8*ax*cx*ay^3*cy^2*py-4*cx^2*ay^3*cy^2*py+4*ax^2*ay^2*cy^3*py+8*ax*cx*ay^2*cy^3*py-4*ax^2*ay*cy^4*py-8*ax^3*cx^2*ay*bx*py+16*ax^2*cx^3*ay*bx*py-8*ax*cx^4*ay*bx*py-8*ax*cx^2*ay^3*bx*py+12*cx^3*ay^3*bx*py-8*ax^2*cx*ay^2*cy*bx*py+8*ax*cx^2*ay^2*cy*bx*py-8*cx^3*ay^2*cy*bx*py-8*cx*ay^4*cy*bx*py+12*ax^2*cx*ay*cy^2*bx*py-8*ax*cx^2*ay*cy^2*bx*py+16*cx*ay^3*cy^2*bx*py-8*cx*ay^2*cy^3*bx*py+8*ax^3*cx*ay*bx^2*py-16*ax^2*cx^2*ay*bx^2*py+8*cx^4*ay*bx^2*py+8*ax*cx*ay^3*bx^2*py-12*cx^2*ay^3*bx^2*py+8*ax^2*ay^2*cy*bx^2*py-8*ax*cx*ay^2*cy*bx^2*py+8*ay^4*cy*bx^2*py-4*ax^2*ay*cy^2*bx^2*py-4*ax*cx*ay*cy^2*bx^2*py+12*cx^2*ay*cy^2*bx^2*py-8*ay^3*cy^2*bx^2*py-4*ay^2*cy^3*bx^2*py+4*ay*cy^4*bx^2*py+16*ax*cx^2*ay*bx^3*py-16*cx^3*ay*bx^3*py+16*cx*ay^2*cy*bx^3*py-12*cx*ay*cy^2*bx^3*py-8*ax*cx*ay*bx^4*py+8*cx^2*ay*bx^4*py-8*ay^2*cy*bx^4*py+4*ay*cy^2*bx^4*py+8*ax^4*cx^2*b_y*py-16*ax^3*cx^3*b_y*py+8*ax^2*cx^4*b_y*py+12*ax^2*cx^2*ay^2*b_y*py-12*ax*cx^3*ay^2*b_y*py+4*cx^4*ay^2*b_y*py+4*cx^2*ay^4*b_y*py-8*ax^2*cx^2*ay*cy*b_y*py+4*ax^4*cy^2*b_y*py-12*ax^3*cx*cy^2*b_y*py+12*ax^2*cx^2*cy^2*b_y*py+4*ax^2*ay^2*cy^2*b_y*py-16*ax*cx*ay^2*cy^2*b_y*py+4*cx^2*ay^2*cy^2*b_y*py+4*ax^2*cy^4*b_y*py-8*ax^4*cx*bx*b_y*py+16*ax^3*cx^2*bx*b_y*py-8*ax*cx^4*bx*b_y*py-16*ax^2*cx*ay^2*bx*b_y*py+16*ax*cx^2*ay^2*bx*b_y*py-4*cx^3*ay^2*bx*b_y*py-8*cx*ay^4*bx*b_y*py+16*ax*cx^2*ay*cy*bx*b_y*py-4*ax^2*cx*cy^2*bx*b_y*py-8*ax*cx^2*cy^2*bx*b_y*py+8*cx*ay^2*cy^2*bx*b_y*py-16*ax^2*cx^2*bx^2*b_y*py+16*ax*cx^3*bx^2*b_y*py-8*cx^2*ay^2*bx^2*b_y*py-8*cx^2*ay*cy*bx^2*b_y*py-4*ax^2*cy^2*bx^2*b_y*py+12*ax*cx*cy^2*bx^2*b_y*py-4*cx^2*cy^2*bx^2*b_y*py+4*ay^2*cy^2*bx^2*b_y*py-4*cy^4*bx^2*b_y*py+8*ax^2*cx*bx^3*b_y*py-8*ax*cx^2*bx^3*b_y*py+8*cx*ay^2*bx^3*b_y*py+4*cx*cy^2*bx^3*b_y*py+8*ax^3*cx*ay*b_y^2*py-8*ax^2*cx^2*ay*b_y^2*py-4*ax*cx^3*ay*b_y^2*py+4*cx^4*ay*b_y^2*py+8*ax*cx*ay^3*b_y^2*py-4*cx^2*ay^3*b_y^2*py-8*ax^4*cy*b_y^2*py+16*ax^3*cx*cy*b_y^2*py-8*ax*cx^3*cy*b_y^2*py-8*ax^2*ay^2*cy*b_y^2*py+8*ax*cx*ay^2*cy*b_y^2*py-8*cx^2*ay^2*cy*b_y^2*py+4*ax^2*ay*cy^2*b_y^2*py+8*ax*cx*ay*cy^2*b_y^2*py+4*cx^2*ay*cy^2*b_y^2*py-4*ax^2*cy^3*b_y^2*py-8*ax*cx*cy^3*b_y^2*py+16*ax*cx^2*ay*bx*b_y^2*py-12*cx^3*ay*bx*b_y^2*py-8*ax^2*cx*cy*bx*b_y^2*py+8*ax*cx^2*cy*bx*b_y^2*py+8*cx^3*cy*bx*b_y^2*py+8*cx*ay^2*cy*bx*b_y^2*py-16*cx*ay*cy^2*bx*b_y^2*py+8*cx*cy^3*bx*b_y^2*py-16*ax*cx*ay*bx^2*b_y^2*py+12*cx^2*ay*bx^2*b_y^2*py+8*ax^2*cy*bx^2*b_y^2*py-8*ax*cx*cy*bx^2*b_y^2*py-8*cx^2*cy*bx^2*b_y^2*py-8*ay^2*cy*bx^2*b_y^2*py+4*ay*cy^2*bx^2*b_y^2*py+4*cy^3*bx^2*b_y^2*py-12*ax^2*cx^2*b_y^3*py+12*ax*cx^3*b_y^3*py-4*cx^4*b_y^3*py-4*cx^2*ay^2*b_y^3*py-8*ax^2*cy^2*b_y^3*py+16*ax*cx*cy^2*b_y^3*py-4*cx^2*cy^2*b_y^3*py+8*ax^2*cx*bx*b_y^3*py-8*ax*cx^2*bx*b_y^3*py+4*cx^3*bx*b_y^3*py+8*cx*ay^2*bx*b_y^3*py-8*cx*cy^2*bx*b_y^3*py-8*ax*cx*ay*b_y^4*py+4*cx^2*ay*b_y^4*py+8*ax^2*cy*b_y^4*py-8*ax*cx*cy*b_y^4*py+4*cx^2*cy*b_y^4*py+8*ax^3*cx^2*ay*px*py-16*ax^2*cx^3*ay*px*py+8*ax*cx^4*ay*px*py-4*ax*cx^2*ay^3*px*py+24*ax^2*cx*ay^2*cy*px*py-16*ax*cx^2*ay^2*cy*px*py-4*ax^3*ay*cy^2*px*py-16*ax^2*cx*ay*cy^2*px*py+16*ax*cx^2*ay*cy^2*px*py+8*ax*ay^3*cy^2*px*py-16*ax*ay^2*cy^3*px*py+8*ax*ay*cy^4*px*py+8*ax^2*cx^2*ay*bx*px*py-8*cx^4*ay*bx*px*py-4*cx^2*ay^3*bx*px*py-8*ax^2*ay^2*cy*bx*px*py+16*ax*cx*ay^2*cy*bx*px*py+16*cx^2*ay^2*cy*bx*px*py-8*ay^4*cy*bx*px*py-4*ax^2*ay*cy^2*bx*px*py-16*cx^2*ay*cy^2*bx*px*py+16*ay^2*cy^3*bx*px*py-8*ay*cy^4*bx*px*py-8*ax^3*ay*bx^2*px*py+8*ax^2*cx*ay*bx^2*px*py-16*ax*cx^2*ay*bx^2*px*py+16*cx^3*ay*bx^2*px*py-8*ax*ay^3*bx^2*px*py+16*cx*ay^3*bx^2*px*py-8*ax*ay^2*cy*bx^2*px*py-40*cx*ay^2*cy*bx^2*px*py+12*ax*ay*cy^2*bx^2*px*py+16*cx*ay*cy^2*bx^2*px*py+16*ay^2*cy*bx^3*px*py-4*ay*cy^2*bx^3*px*py+8*ax*ay*bx^4*px*py-8*cx*ay*bx^4*px*py-8*ax^4*cx*b_y*px*py+16*ax^2*cx^3*b_y*px*py-8*ax*cx^4*b_y*px*py-8*ax^2*cx*ay^2*b_y*px*py-4*ax*cx^2*ay^2*b_y*px*py+16*ax^2*cx*ay*cy*b_y*px*py-4*ax^3*cy^2*b_y*px*py+16*ax^2*cx*cy^2*b_y*px*py-16*ax*cx^2*cy^2*b_y*px*py+8*ax*ay^2*cy^2*b_y*px*py-8*ax*cy^4*b_y*px*py+8*ax^4*bx*b_y*px*py-16*ax^2*cx^2*bx*b_y*px*py+8*cx^4*bx*b_y*px*py+16*ax^2*ay^2*bx*b_y*px*py+12*cx^2*ay^2*bx*b_y*px*py+8*ay^4*bx*b_y*px*py-32*ax*cx*ay*cy*bx*b_y*px*py+12*ax^2*cy^2*bx*b_y*px*py+16*cx^2*cy^2*bx*b_y*px*py-16*ay^2*cy^2*bx*b_y*px*py+8*cy^4*bx*b_y*px*py+8*ax^2*cx*bx^2*b_y*px*py+8*ax*cx^2*bx^2*b_y*px*py-16*cx^3*bx^2*b_y*px*py-8*cx*ay^2*bx^2*b_y*px*py+16*cx*ay*cy*bx^2*b_y*px*py-4*ax*cy^2*bx^2*b_y*px*py-16*cx*cy^2*bx^2*b_y*px*py-8*ax^2*bx^3*b_y*px*py+8*cx^2*bx^3*b_y*px*py-8*ay^2*bx^3*b_y*px*py-4*cy^2*bx^3*b_y*px*py-8*ax^3*ay*b_y^2*px*py-8*ax^2*cx*ay*b_y^2*px*py+12*ax*cx^2*ay*b_y^2*px*py-8*ax*ay^3*b_y^2*px*py+16*ax^3*cy*b_y^2*px*py-40*ax^2*cx*cy*b_y^2*px*py+16*ax*cx^2*cy*b_y^2*px*py+8*ax*ay^2*cy*b_y^2*px*py-16*ax*ay*cy^2*b_y^2*px*py+16*ax*cy^3*b_y^2*px*py-4*cx^2*ay*bx*b_y^2*px*py-8*ax^2*cy*bx*b_y^2*px*py+16*ax*cx*cy*bx*b_y^2*px*py-16*cx^2*cy*bx*b_y^2*px*py+8*ay^2*cy*bx*b_y^2*px*py+8*ay*cy^2*bx*b_y^2*px*py-16*cy^3*bx*b_y^2*px*py+16*ax*ay*bx^2*b_y^2*px*py-8*cx*ay*bx^2*b_y^2*px*py-8*ax*cy*bx^2*b_y^2*px*py+24*cx*cy*bx^2*b_y^2*px*py+16*ax^2*cx*b_y^3*px*py-4*ax*cx^2*b_y^3*px*py-8*ax^2*bx*b_y^3*px*py-4*cx^2*bx*b_y^3*px*py-8*ay^2*bx*b_y^3*px*py+8*cy^2*bx*b_y^3*px*py+8*ax*ay*b_y^4*px*py-8*ax*cy*b_y^4*px*py-8*ax^3*cx*ay*px^2*py+8*ax^2*cx^2*ay*px^2*py+4*ax*cx*ay^3*px^2*py+4*cx^2*ay^3*px^2*py-8*ax^2*ay^2*cy*px^2*py-16*ax*cx*ay^2*cy*px^2*py+4*ay^4*cy*px^2*py+16*ax^2*ay*cy^2*px^2*py-4*ay^3*cy^2*px^2*py+8*ax^3*ay*bx*px^2*py-16*ax^2*cx*ay*bx*px^2*py+8*ax*cx^2*ay*bx*px^2*py+8*ax*ay^3*bx*px^2*py-12*cx*ay^3*bx*px^2*py+8*ax*ay^2*cy*bx*px^2*py+16*cx*ay^2*cy*bx*px^2*py-8*ax*ay*cy^2*bx*px^2*py+8*ax^2*ay*bx^2*px^2*py+8*ax*cx*ay*bx^2*px^2*py-16*cx^2*ay*bx^2*px^2*py-4*ay^3*bx^2*px^2*py-8*ay*cy^2*bx^2*px^2*py-16*ax*ay*bx^3*px^2*py+16*cx*ay*bx^3*px^2*py+16*ax^3*cx*b_y*px^2*py-16*ax^2*cx^2*b_y*px^2*py-4*ax^2*ay^2*b_y*px^2*py+12*ax*cx*ay^2*b_y*px^2*py-4*cx^2*ay^2*b_y*px^2*py-4*ay^4*b_y*px^2*py-8*ax^2*ay*cy*b_y*px^2*py-8*ax^2*cy^2*b_y*px^2*py+4*ay^2*cy^2*b_y*px^2*py-16*ax^3*bx*b_y*px^2*py+8*ax^2*cx*bx*b_y*px^2*py+8*ax*cx^2*bx*b_y*px^2*py-16*ax*ay^2*bx*b_y*px^2*py-4*cx*ay^2*bx*b_y*px^2*py+16*ax*ay*cy*bx*b_y*px^2*py-8*ax*cy^2*bx*b_y*px^2*py+8*ax^2*bx^2*b_y*px^2*py-16*ax*cx*bx^2*b_y*px^2*py+8*cx^2*bx^2*b_y*px^2*py+16*ay^2*bx^2*b_y*px^2*py-8*ay*cy*bx^2*b_y*px^2*py+16*cy^2*bx^2*b_y*px^2*py+8*ax*bx^3*b_y*px^2*py-8*cx*bx^3*b_y*px^2*py+16*ax^2*ay*b_y^2*px^2*py-4*ax*cx*ay*b_y^2*px^2*py-4*cx^2*ay*b_y^2*px^2*py+4*ay^3*b_y^2*px^2*py+16*ax*cx*cy*b_y^2*px^2*py-8*ay^2*cy*b_y^2*px^2*py+4*ay*cy^2*b_y^2*px^2*py-16*ax*ay*bx*b_y^2*px^2*py+12*cx*ay*bx*b_y^2*px^2*py+8*ax*cy*bx*b_y^2*px^2*py-16*cx*cy*bx*b_y^2*px^2*py-4*ay*bx^2*b_y^2*px^2*py-8*cy*bx^2*b_y^2*px^2*py-4*ax^2*b_y^3*px^2*py-12*ax*cx*b_y^3*px^2*py+4*cx^2*b_y^3*px^2*py+4*ay^2*b_y^3*px^2*py-4*cy^2*b_y^3*px^2*py+8*ax*bx*b_y^3*px^2*py+4*cx*bx*b_y^3*px^2*py-4*ay*b_y^4*px^2*py+4*cy*b_y^4*px^2*py+8*ax^2*cx*ay*px^3*py-8*ax*cx^2*ay*px^3*py-4*ax*ay^3*px^3*py+8*ax*ay^2*cy*px^3*py-8*ax*ay*cy^2*px^3*py-8*ax^2*ay*bx*px^3*py+8*cx^2*ay*bx*px^3*py+4*ay^3*bx*px^3*py-8*ay^2*cy*bx*px^3*py+8*ay*cy^2*bx*px^3*py+8*ax*ay*bx^2*px^3*py-8*cx*ay*bx^2*px^3*py-8*ax^2*cx*b_y*px^3*py+8*ax*cx^2*b_y*px^3*py+4*ax*ay^2*b_y*px^3*py+8*ax*cy^2*b_y*px^3*py+8*ax^2*bx*b_y*px^3*py-8*cx^2*bx*b_y*px^3*py-4*ay^2*bx*b_y*px^3*py-8*cy^2*bx*b_y*px^3*py-8*ax*bx^2*b_y*px^3*py+8*cx*bx^2*b_y*px^3*py-4*ax*ay*b_y^2*px^3*py-8*ax*cy*b_y^2*px^3*py+4*ay*bx*b_y^2*px^3*py+8*cy*bx*b_y^2*px^3*py+4*ax*b_y^3*px^3*py-4*bx*b_y^3*px^3*py-4*ax^4*cx^2*py^2+8*ax^3*cx^3*py^2-4*ax^2*cx^4*py^2-2*ax^2*cx^2*ay^2*py^2-4*ax*cx^3*ay^2*py^2+4*cx^4*ay^2*py^2-4*cx^2*ay^4*py^2-4*ax^3*cx*ay*cy*py^2+16*ax^2*cx^2*ay*cy*py^2-8*ax*cx^3*ay*cy*py^2+8*ax*cx*ay^3*cy*py^2-4*cx^2*ay^3*cy*py^2-2*ax^4*cy^2*py^2+4*ax^3*cx*cy^2*py^2-4*ax^2*cx^2*cy^2*py^2-8*ax^2*ay^2*cy^2*py^2+8*ax*cx*ay^2*cy^2*py^2+4*cx^2*ay^2*cy^2*py^2+4*ax^2*ay*cy^3*py^2-8*ax*cx*ay*cy^3*py^2+4*ax^4*cx*bx*py^2-4*ax^3*cx^2*bx*py^2-8*ax^2*cx^3*bx*py^2+8*ax*cx^4*bx*py^2+12*ax^2*cx*ay^2*bx*py^2-12*cx^3*ay^2*bx*py^2+8*cx*ay^4*bx*py^2-4*ax^2*cx*ay*cy*bx*py^2-8*ax*cx^2*ay*cy*bx*py^2+8*cx^3*ay*cy*bx*py^2-4*ax^2*cx*cy^2*bx*py^2+8*ax*cx^2*cy^2*bx*py^2-16*cx*ay^2*cy^2*bx*py^2+8*cx*ay*cy^3*bx*py^2-4*ax^3*cx*bx^2*py^2+16*ax^2*cx^2*bx^2*py^2-8*ax*cx^3*bx^2*py^2-4*cx^4*bx^2*py^2-4*ax^2*ay^2*bx^2*py^2-8*ax*cx*ay^2*bx^2*py^2+22*cx^2*ay^2*bx^2*py^2-4*ay^4*bx^2*py^2-4*ax^2*ay*cy*bx^2*py^2+12*ax*cx*ay*cy*bx^2*py^2-8*cx^2*ay*cy*bx^2*py^2-8*ay^3*cy*bx^2*py^2+4*ax^2*cy^2*bx^2*py^2-4*ax*cx*cy^2*bx^2*py^2-4*cx^2*cy^2*bx^2*py^2+16*ay^2*cy^2*bx^2*py^2-4*ay*cy^3*bx^2*py^2-4*ax^2*cx*bx^3*py^2-4*ax*cx^2*bx^3*py^2+8*cx^3*bx^3*py^2-12*cx*ay^2*bx^3*py^2-4*cx*ay*cy*bx^3*py^2+4*cx*cy^2*bx^3*py^2+4*ax*cx*bx^4*py^2-4*cx^2*bx^4*py^2+4*ay^2*bx^4*py^2+4*ay*cy*bx^4*py^2-2*cy^2*bx^4*py^2-8*ax^3*cx*ay*b_y*py^2+16*ax*cx^3*ay*b_y*py^2-8*cx^4*ay*b_y*py^2-8*ax*cx*ay^3*b_y*py^2-4*cx^2*ay^3*b_y*py^2+4*ax^4*cy*b_y*py^2-4*ax^3*cx*cy*b_y*py^2-8*ax^2*cx^2*cy*b_y*py^2+8*ax*cx^3*cy*b_y*py^2+4*ax^2*ay^2*cy*b_y*py^2+8*ax*cx*ay^2*cy*b_y*py^2+4*cx^2*ay^2*cy*b_y*py^2-8*ax^2*ay*cy^2*b_y*py^2+8*ax*cx*ay*cy^2*b_y*py^2-8*cx^2*ay*cy^2*b_y*py^2-4*ax^2*cy^3*b_y*py^2+8*ax*cx*cy^3*b_y*py^2+16*ax^2*cx*ay*bx*b_y*py^2-40*ax*cx^2*ay*bx*b_y*py^2+16*cx^3*ay*bx*b_y*py^2+16*cx*ay^3*bx*b_y*py^2+12*ax^2*cx*cy*bx*b_y*py^2-8*ax*cx^2*cy*bx*b_y*py^2-8*cx^3*cy*bx*b_y*py^2-16*cx*ay^2*cy*bx*b_y*py^2+8*cx*ay*cy^2*bx*b_y*py^2-8*cx*cy^3*bx*b_y*py^2+16*ax*cx*ay*bx^2*b_y*py^2-4*ax^2*cy*bx^2*b_y*py^2-4*ax*cx*cy*bx^2*b_y*py^2+16*cx^2*cy*bx^2*b_y*py^2+4*ay^2*cy*bx^2*b_y*py^2-8*ay*cy^2*bx^2*b_y*py^2+4*cy^3*bx^2*b_y*py^2-8*cx*ay*bx^3*b_y*py^2-4*cx*cy*bx^3*b_y*py^2+4*ax^4*b_y^2*py^2-12*ax^3*cx*b_y^2*py^2+22*ax^2*cx^2*b_y^2*py^2-12*ax*cx^3*b_y^2*py^2+4*cx^4*b_y^2*py^2+4*ax^2*ay^2*b_y^2*py^2-16*ax*cx*ay^2*b_y^2*py^2+16*cx^2*ay^2*b_y^2*py^2+4*ax^2*ay*cy*b_y^2*py^2-16*ax*cx*ay*cy*b_y^2*py^2+4*cx^2*ay*cy*b_y^2*py^2+16*ax^2*cy^2*b_y^2*py^2-16*ax*cx*cy^2*b_y^2*py^2+4*cx^2*cy^2*b_y^2*py^2-8*ax^2*cx*bx*b_y^2*py^2-4*cx^3*bx*b_y^2*py^2-16*cx*ay^2*bx*b_y^2*py^2+8*cx*ay*cy*bx*b_y^2*py^2+8*cx*cy^2*bx*b_y^2*py^2-4*ax^2*bx^2*b_y^2*py^2+12*ax*cx*bx^2*b_y^2*py^2-2*cx^2*bx^2*b_y^2*py^2+4*ay^2*bx^2*b_y^2*py^2+4*ay*cy*bx^2*b_y^2*py^2-8*cy^2*bx^2*b_y^2*py^2+16*ax*cx*ay*b_y^3*py^2-4*cx^2*ay*b_y^3*py^2-8*ax^2*cy*b_y^3*py^2-4*cx^2*cy*b_y^3*py^2-8*cx*ay*bx*b_y^3*py^2+8*cx*cy*bx*b_y^3*py^2-4*ax^2*b_y^4*py^2+8*ax*cx*b_y^4*py^2-4*cx^2*b_y^4*py^2+4*ax^4*cx*px*py^2-4*ax^3*cx^2*px*py^2-8*ax^2*cx*ay^2*px*py^2+16*ax*cx^2*ay^2*px*py^2+4*ax^3*ay*cy*px*py^2-16*ax^2*cx*ay*cy*px*py^2-8*ax*ay^3*cy*px*py^2+4*ax^3*cy^2*px*py^2+8*ax*ay^2*cy^2*px*py^2-4*ax^4*bx*px*py^2+4*ax^2*cx^2*bx*px*py^2-4*ax^2*ay^2*bx*px*py^2-8*ax*cx*ay^2*bx*px*py^2-8*cx^2*ay^2*bx*px*py^2+12*ax^2*ay*cy*bx*px*py^2+16*ay^3*cy*bx*px*py^2-4*ax^2*cy^2*bx*px*py^2-16*ay^2*cy^2*bx*px*py^2+4*ax^3*bx^2*px*py^2-8*ax^2*cx*bx^2*px*py^2+4*ax*cx^2*bx^2*px*py^2+16*ax*ay^2*bx^2*px*py^2-4*ax*ay*cy*bx^2*px*py^2+16*cx*ay*cy*bx^2*px*py^2-4*ax*cy^2*bx^2*px*py^2+4*ax^2*bx^3*px*py^2-4*cx^2*bx^3*px*py^2-4*ay^2*bx^3*px*py^2-12*ay*cy*bx^3*px*py^2+4*cy^2*bx^3*px*py^2-4*ax*bx^4*px*py^2+4*cx*bx^4*px*py^2+8*ax^3*ay*b_y*px*py^2+8*ax^2*cx*ay*b_y*px*py^2-8*ax*cx^2*ay*b_y*px*py^2+8*ax*ay^3*b_y*px*py^2-12*ax^3*cy*b_y*px*py^2+16*ax^2*cx*cy*b_y*px*py^2-16*ax*ay^2*cy*b_y*px*py^2+8*ax*ay*cy^2*b_y*px*py^2-16*ax^2*ay*bx*b_y*px*py^2+16*ax*cx*ay*bx*b_y*px*py^2-8*cx^2*ay*bx*b_y*px*py^2-16*ay^3*bx*b_y*px*py^2-4*ax^2*cy*bx*b_y*px*py^2+8*ay^2*cy*bx*b_y*px*py^2+8*ay*cy^2*bx*b_y*px*py^2-16*ax*ay*bx^2*b_y*px*py^2+8*cx*ay*bx^2*b_y*px*py^2+12*ax*cy*bx^2*b_y*px*py^2-16*cx*cy*bx^2*b_y*px*py^2+8*ay*bx^3*b_y*px*py^2+4*cy*bx^3*b_y*px*py^2-4*ax^3*b_y^2*px*py^2-8*ax*cx^2*b_y^2*px*py^2+8*ax*ay^2*b_y^2*px*py^2+8*ax*ay*cy*b_y^2*px*py^2-16*ax*cy^2*b_y^2*px*py^2+16*ax^2*bx*b_y^2*px*py^2-8*ax*cx*bx*b_y^2*px*py^2+16*cx^2*bx*b_y^2*px*py^2+8*ay^2*bx*b_y^2*px*py^2-16*ay*cy*bx*b_y^2*px*py^2+8*cy^2*bx*b_y^2*px*py^2-4*ax*bx^2*b_y^2*px*py^2-8*cx*bx^2*b_y^2*px*py^2-16*ax*ay*b_y^3*px*py^2+16*ax*cy*b_y^3*px*py^2+8*ay*bx*b_y^3*px*py^2-8*cy*bx*b_y^3*px*py^2-4*ax^3*cx*px^2*py^2+4*ax^2*cx^2*px^2*py^2+6*ax^2*ay^2*px^2*py^2-4*ax*cx*ay^2*px^2*py^2-4*cx^2*ay^2*px^2*py^2-4*ax^2*ay*cy*px^2*py^2+16*ax*cx*ay*cy*px^2*py^2-4*ay^3*cy*px^2*py^2-4*ax^2*cy^2*px^2*py^2+4*ay^2*cy^2*px^2*py^2+4*ax^3*bx*px^2*py^2+4*ax^2*cx*bx*px^2*py^2-8*ax*cx^2*bx*px^2*py^2-8*ax*ay^2*bx*px^2*py^2+12*cx*ay^2*bx*px^2*py^2-8*ax*ay*cy*bx*px^2*py^2-16*cx*ay*cy*bx*px^2*py^2+8*ax*cy^2*bx*px^2*py^2-8*ax^2*bx^2*px^2*py^2+4*ax*cx*bx^2*px^2*py^2+4*cx^2*bx^2*px^2*py^2-2*ay^2*bx^2*px^2*py^2+12*ay*cy*bx^2*px^2*py^2-4*cy^2*bx^2*px^2*py^2+4*ax*bx^3*px^2*py^2-4*cx*bx^3*px^2*py^2-8*ax^2*ay*b_y*px^2*py^2-8*ax*cx*ay*b_y*px^2*py^2+8*cx^2*ay*b_y*px^2*py^2+4*ay^3*b_y*px^2*py^2+12*ax^2*cy*b_y*px^2*py^2-16*ax*cx*cy*b_y*px^2*py^2+4*ay^2*cy*b_y*px^2*py^2-8*ay*cy^2*b_y*px^2*py^2+24*ax*ay*bx*b_y*px^2*py^2-8*cx*ay*bx*b_y*px^2*py^2-8*ax*cy*bx*b_y*px^2*py^2+16*cx*cy*bx*b_y*px^2*py^2-8*ay*bx^2*b_y*px^2*py^2-4*cy*bx^2*b_y*px^2*py^2-2*ax^2*b_y^2*px^2*py^2+12*ax*cx*b_y^2*px^2*py^2-4*cx^2*b_y^2*px^2*py^2-8*ay^2*b_y^2*px^2*py^2+4*ay*cy*b_y^2*px^2*py^2+4*cy^2*b_y^2*px^2*py^2-8*ax*bx*b_y^2*px^2*py^2-4*cx*bx*b_y^2*px^2*py^2+6*bx^2*b_y^2*px^2*py^2+4*ay*b_y^3*px^2*py^2-4*cy*b_y^3*px^2*py^2+4*ax^3*cx*ay*py^3-4*ax^2*cx^2*ay*py^3+8*cx^2*ay^3*py^3+4*ax^2*ay^2*cy*py^3-16*ax*cx*ay^2*cy*py^3+4*ax^2*ay*cy^2*py^3-12*ax^2*cx*ay*bx*py^3+16*ax*cx^2*ay*bx*py^3-16*cx*ay^3*bx*py^3+16*cx*ay^2*cy*bx*py^3+4*ax^2*ay*bx^2*py^3-4*ax*cx*ay*bx^2*py^3-12*cx^2*ay*bx^2*py^3+8*ay^3*bx^2*py^3-4*ay^2*cy*bx^2*py^3-4*ay*cy^2*bx^2*py^3+12*cx*ay*bx^3*py^3-4*ay*bx^4*py^3-4*ax^4*b_y*py^3+12*ax^3*cx*b_y*py^3-12*ax^2*cx^2*b_y*py^3-4*ax^2*ay^2*b_y*py^3+16*ax*cx*ay^2*b_y*py^3-8*cx^2*ay^2*b_y*py^3-4*ax^2*cy^2*b_y*py^3-4*ax^2*cx*bx*b_y*py^3+16*ax*cx^2*bx*b_y*py^3+4*ax^2*bx^2*b_y*py^3-12*ax*cx*bx^2*b_y*py^3-4*cx^2*bx^2*b_y*py^3-4*ay^2*bx^2*b_y*py^3+4*cy^2*bx^2*b_y*py^3+4*cx*bx^3*b_y*py^3-4*ax^2*ay*b_y^2*py^3-8*cx^2*ay*b_y^2*py^3-4*ax^2*cy*b_y^2*py^3+16*ax*cx*cy*b_y^2*py^3+16*cx*ay*bx*b_y^2*py^3-16*cx*cy*bx*b_y^2*py^3-4*ay*bx^2*b_y^2*py^3+4*cy*bx^2*b_y^2*py^3+8*ax^2*b_y^3*py^3-16*ax*cx*b_y^3*py^3+8*cx^2*b_y^3*py^3-4*ax^3*ay*px*py^3+8*ax^2*cx*ay*px*py^3-8*ax*cx^2*ay*px*py^3+8*ax*ay^2*cy*px*py^3-8*ax*ay*cy^2*px*py^3+4*ax^2*ay*bx*px*py^3+8*cx^2*ay*bx*px*py^3-8*ay^2*cy*bx*px*py^3+8*ay*cy^2*bx*px*py^3-4*ax*ay*bx^2*px*py^3-8*cx*ay*bx^2*px*py^3+4*ay*bx^3*px*py^3+4*ax^3*b_y*px*py^3-8*ax^2*cx*b_y*px*py^3+8*ax*cx^2*b_y*px*py^3-8*ax*ay^2*b_y*px*py^3+8*ax*cy^2*b_y*px*py^3-4*ax^2*bx*b_y*px*py^3-8*cx^2*bx*b_y*px*py^3+8*ay^2*bx*b_y*px*py^3-8*cy^2*bx*b_y*px*py^3+4*ax*bx^2*b_y*px*py^3+8*cx*bx^2*b_y*px*py^3-4*bx^3*b_y*px*py^3+8*ax*ay*b_y^2*px*py^3-8*ax*cy*b_y^2*px*py^3-8*ay*bx*b_y^2*px*py^3+8*cy*bx*b_y^2*px*py^3+ax^4*py^4-4*ax^3*cx*py^4+4*ax^2*cx^2*py^4-4*cx^2*ay^2*py^4-4*ax^2*ay*cy*py^4+8*ax*cx*ay*cy*py^4+4*ax^2*cx*bx*py^4-8*ax*cx^2*bx*py^4+8*cx*ay^2*bx*py^4-8*cx*ay*cy*bx*py^4-2*ax^2*bx^2*py^4+4*ax*cx*bx^2*py^4+4*cx^2*bx^2*py^4-4*ay^2*bx^2*py^4+4*ay*cy*bx^2*py^4-4*cx*bx^3*py^4+bx^4*py^4+4*ax^2*ay*b_y*py^4-8*ax*cx*ay*b_y*py^4+8*cx^2*ay*b_y*py^4+4*ax^2*cy*b_y*py^4-8*ax*cx*cy*b_y*py^4-8*cx*ay*bx*b_y*py^4+8*cx*cy*bx*b_y*py^4+4*ay*bx^2*b_y*py^4-4*cy*bx^2*b_y*py^4-4*ax^2*b_y^2*py^4+8*ax*cx*b_y^2*py^4-4*cx^2*b_y^2*py^4]

				PPolynomial ax = new PPolynomial(vA[0]);
				PPolynomial ay = new PPolynomial(vA[1]);
				PPolynomial bx = new PPolynomial(vB[0]);
				PPolynomial by = new PPolynomial(vB[1]);
				PPolynomial cx = new PPolynomial(vC[0]);
				PPolynomial cy = new PPolynomial(vC[1]);
				PPolynomial x = new PPolynomial(botanaVars[0]);
				PPolynomial y = new PPolynomial(botanaVars[1]);

				PPolynomial p =
						((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((cx.pow(
								4)).multiply((ay.pow(4)))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(3)))).multiply(cy)))).add(
								(((((new PPolynomial(6)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((cy.pow(3)))))).add(
								((ax.pow(4)).multiply((cy.pow(4)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply((ay.pow(2)))).multiply(bx)))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply(bx)))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(4)))).multiply(bx)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										bx)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(ay)).multiply(cy)).multiply(
										bx)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(bx)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(bx)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply(bx)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										bx)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										bx)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(bx)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										ay)).multiply((cy.pow(3)))).multiply(bx)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply((bx.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(
										(bx.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply((bx.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(3)))).multiply((bx.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((ay.pow(3)))).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))))).subtract(
								((((new PPolynomial(2)).multiply((ax.pow(2)))).multiply(
										(cy.pow(4)))).multiply((bx.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(4)))).multiply((bx.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(3)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply((bx.pow(3)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(4)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((bx.pow(4)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(4)))))).add(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(4)))))).add(
								((cy.pow(4)).multiply((bx.pow(4)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(cy)).multiply(
										by)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply(cy)).multiply(by)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply(by)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((cy.pow(2)))).multiply(
										by)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((cy.pow(2)))).multiply(by)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply((cy.pow(2)))).multiply(
										by)))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cy.pow(3)))).multiply(by)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(cy.pow(3)))).multiply(by)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(3)))).multiply(
										by)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(3)))).multiply(by)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(4)))).multiply(by)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										cy)).multiply(bx)).multiply(by)))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(cy)).multiply(bx)).multiply(
										by)))).add(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(cy)).multiply(
										bx)).multiply(by)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(cy)).multiply(
										bx)).multiply(by)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(4)))).multiply(
										cy)).multiply(bx)).multiply(by)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(by)))).add(
								(((((((new PPolynomial(24)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(by)))).subtract(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(by)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(by)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										by)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										cy)).multiply((bx.pow(2)))).multiply(by)))).add(
								((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										by)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										by)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(by)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(
										by)))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(by)))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(
										(bx.pow(2)))).multiply(by)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										cy)).multiply((bx.pow(3)))).multiply(by)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										cy)).multiply((bx.pow(3)))).multiply(by)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										cy)).multiply((bx.pow(3)))).multiply(by)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))).multiply(
										by)))).subtract(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(3)))).multiply(by)))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cx.pow(2)))).multiply((by.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(3)))).multiply((by.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(4)))).multiply((by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(
										(by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))))).subtract(
								((((new PPolynomial(2)).multiply((cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply(cy)).multiply((by.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(by.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										ay)).multiply(cy)).multiply((by.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(cy)).multiply((by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply((by.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(3)))).multiply((by.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(3)))).multiply(
										(by.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(4)))).multiply((by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(bx)).multiply((by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										bx)).multiply((by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(
										(by.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply((by.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										(by.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(
										(by.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(bx)).multiply((by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).add(
								(((((new PPolynomial(6)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply((by.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										cy)).multiply((by.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(
										(by.pow(3)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply((by.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply((by.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cy.pow(3)))).multiply((by.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										cy)).multiply(bx)).multiply((by.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										cy)).multiply(bx)).multiply((by.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										cy)).multiply(
										bx)).multiply((by.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										cy)).multiply(bx)).multiply((by.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(3)))))).add(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((by.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(by.pow(4)))))).add(
								((cx.pow(4)).multiply((by.pow(4)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((by.pow(4)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((by.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply((ay.pow(2)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(ay)).multiply(cy)).multiply(
										x)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply(x)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										(cy.pow(3)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										ay)).multiply((cy.pow(3)))).multiply(x)))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cy.pow(4)))).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(bx)).multiply(
										x)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply(bx)).multiply(x)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply(x)))).subtract(
								((((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(bx)).multiply(
										x)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										x)))).add(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										x)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										x)))).add(
								((((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										x)))).add(
								(((((new PPolynomial(8)).multiply((ay.pow(4)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(x)))).subtract(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										ay)).multiply((cy.pow(3)))).multiply(bx)).multiply(
										x)))).subtract(
								(((((new PPolynomial(16)).multiply((ay.pow(3)))).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(4)))).multiply(bx)).multiply(x)))).add(
								(((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										(cy.pow(4)))).multiply(bx)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(4)))).multiply(
										(bx.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(x)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(x)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										cy)).multiply((bx.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cy.pow(4)))).multiply(
										(bx.pow(2)))).multiply(x)))).subtract(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(3)))).multiply(x)))).add(
								((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(cy)).multiply((bx.pow(3)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(12)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))).multiply(x)))).add(
								(((((new PPolynomial(12)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(3)))).multiply(x)))).subtract(
								((((new PPolynomial(4)).multiply((cy.pow(4)))).multiply(
										(bx.pow(3)))).multiply(x)))).add(
								(((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(4)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(4)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(4)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(4)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(4)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										cy)).multiply(by)).multiply(x)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(by)).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(cy)).multiply(by)).multiply(
										x)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(cy)).multiply(
										by)).multiply(x)))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(cy)).multiply(
										by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply(by)).multiply(x)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply((cy.pow(2)))).multiply(
										by)).multiply(x)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((cy.pow(2)))).multiply(
										by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply(x)))).add(
								(((((new PPolynomial(12)).multiply((ax.pow(3)))).multiply(
										(cy.pow(3)))).multiply(by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(cy.pow(3)))).multiply(by)).multiply(x)))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply((cy.pow(3)))).multiply(by)).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cy)).multiply(
										bx)).multiply(by)).multiply(x)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply(x)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply(x)))).subtract(
								(((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(4)))).multiply(
										cy)).multiply(
										bx)).multiply(by)).multiply(x)))).add(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										ay)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)).multiply(x)))).add(
								((((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)).multiply(x)))).subtract(
								(((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)).multiply(x)))).add(
								((((((new PPolynomial(16)).multiply((ay.pow(3)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(by)).multiply(
										x)))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(
										bx)).multiply(by)).multiply(x)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										by)).multiply(x)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										by)).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)))).subtract(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										by)).multiply(x)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)))).add(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)))).subtract(
								((((((new PPolynomial(12)).multiply(ax)).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply((cy.pow(3)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)))).add(
								(((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										(by.pow(2)))).multiply(x)))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply((by.pow(2)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										(by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply(x)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(
										(by.pow(2)))).multiply(x)))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										cy)).multiply((by.pow(2)))).multiply(x)))).subtract(
								(((((new PPolynomial(12)).multiply((ax.pow(3)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))).multiply(x)))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))).multiply(x)))).add(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(4)))).multiply(
										(by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply(x)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)))).add(
								(((((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(cy)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)))).add(((((((new PPolynomial(16)).multiply(ay)).multiply(
								(cy.pow(3)))).multiply(bx)).multiply((by.pow(2)))).multiply(
								x)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply((by.pow(2)))).multiply(
										x)))).subtract(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply((by.pow(2)))).multiply(
										x)))).add(((((((new PPolynomial(12)).multiply(ax)).multiply(
								(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
								(by.pow(2)))).multiply(
								x)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply((by.pow(3)))).multiply(
										x)))).subtract(
								((((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(cy)).multiply((by.pow(3)))).multiply(
										x)))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((by.pow(3)))).multiply(
										x)))).subtract(
								(((((new PPolynomial(16)).multiply(ax)).multiply(
										(cy.pow(3)))).multiply(
										(by.pow(3)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										bx)).multiply((by.pow(3)))).multiply(x)))).add(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										bx)).multiply((by.pow(3)))).multiply(x)))).add(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										bx)).multiply((by.pow(3)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply((by.pow(3)))).multiply(x)))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(by.pow(4)))).multiply(x)))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(by.pow(4)))).multiply(x)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((by.pow(4)))).multiply(x)))).add(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										(by.pow(4)))).multiply(x)))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(2)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply(cy)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										ay)).multiply(cy)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(cy)).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(
										(x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cy.pow(2)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										(cy.pow(2)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(3)))).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(3)))).multiply((x.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((ay.pow(3)))).multiply(
										(cy.pow(3)))).multiply((x.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(4)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(4)))).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(4)))).multiply(
										bx)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply(bx)).multiply((x.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(bx)).multiply(
										(x.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply(bx)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										cy)).multiply(bx)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(
										cy)).multiply(bx)).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(bx)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(4)))).multiply(
										bx)).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(22)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((cy.pow(4)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(3)))).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(3)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(4)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(8)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(4)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(bx.pow(4)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cy)).multiply(
										by)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										cy)).multiply(by)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										cy)).multiply(
										by)).multiply((x.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(cy.pow(3)))).multiply(by)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										(cy.pow(3)))).multiply(by)).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(
										by)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										cy)).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										cy)).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply((x.pow(2)))))).add(
								(((((((new PPolynomial(12)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply((x.pow(2)))))).subtract(
								(((((((new PPolynomial(40)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cy)).multiply((bx.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										(bx.pow(3)))).multiply(by)).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(cy)).multiply(
										(bx.pow(3)))).multiply(by)).multiply(
										(x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply((x.pow(2)))))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(22)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cy.pow(4)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										bx)).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										bx)).multiply((by.pow(2)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										bx)).multiply((by.pow(2)))).multiply((x.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((((new PPolynomial(4)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(x.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(3)))).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((by.pow(3)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(3)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(3)))).multiply((x.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(by.pow(3)))).multiply((x.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((cy.pow(3)))).multiply(
										(by.pow(3)))).multiply((x.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										bx)).multiply((by.pow(3)))).multiply(
										(x.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply((by.pow(3)))).multiply((x.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(by.pow(4)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(4)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(2)).multiply((cx.pow(2)))).multiply(
										(by.pow(4)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(cy)).multiply(
										(by.pow(4)))).multiply((x.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(by.pow(4)))).multiply((x.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((x.pow(3)))))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(
										(x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										cy)).multiply((x.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cy.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(
										(x.pow(3)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										bx)).multiply(
										(x.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(cy)).multiply(bx)).multiply(
										(x.pow(3)))))).add(
								(((((new PPolynomial(12)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(
										bx)).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(12)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										(bx.pow(3)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(16)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(3)))).multiply((x.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										cy)).multiply(
										by)).multiply((x.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply(by)).multiply(
										(x.pow(3)))))).subtract(
								((((((new PPolynomial(12)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(x.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(by)).multiply(
										(x.pow(3)))))).subtract(
								((((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										bx)).multiply(by)).multiply((x.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(x.pow(3)))))).subtract(
								((((((new PPolynomial(16)).multiply(cx)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(12)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).subtract(
								((((((new PPolynomial(12)).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply((by.pow(2)))).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(12)).multiply(ax)).multiply(cy)).multiply(
										(by.pow(3)))).multiply((x.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(cy)).multiply(bx)).multiply(
										(by.pow(3)))).multiply((x.pow(3)))))).subtract(
								((((new PPolynomial(4)).multiply(ax)).multiply(
										(by.pow(4)))).multiply(
										(x.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((x.pow(4)))))).add(
								((ay.pow(4)).multiply((x.pow(4)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((x.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(
										(x.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((x.pow(4)))))).add(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										bx)).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										bx)).multiply((x.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((x.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply((x.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(8)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((x.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										by)).multiply((x.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply(by)).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										by)).multiply((x.pow(4)))))).subtract(
								(((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										by)).multiply((x.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										bx)).multiply(by)).multiply((x.pow(4)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply(by)).multiply((x.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).subtract(
								((((new PPolynomial(2)).multiply((ay.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(cy)).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).add(
								((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(bx)).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).subtract(
								(((((new PPolynomial(4)).multiply(cx)).multiply(bx)).multiply(
										(by.pow(2)))).multiply((x.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply(cy)).multiply(
										(by.pow(3)))).multiply(
										(x.pow(4)))))).add(((by.pow(4)).multiply((x.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(3)))).multiply(y)))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(ay.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(cy)).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply(cy)).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((cy.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((cy.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply((cy.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(3)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(4)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(bx)).multiply(
										y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(ay)).multiply(bx)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										ay)).multiply(bx)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(bx)).multiply(y)))).add(
								(((((new PPolynomial(12)).multiply((cx.pow(3)))).multiply(
										(ay.pow(3)))).multiply(bx)).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(cy)).multiply(
										bx)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(cy)).multiply(
										bx)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(4)))).multiply(
										cy)).multiply(bx)).multiply(y)))).add(
								(((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((bx.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((bx.pow(2)))).multiply(
										y)))).add(
								(((((new PPolynomial(8)).multiply((cx.pow(4)))).multiply(
										ay)).multiply(
										(bx.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((bx.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply((bx.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										y)))).add(
								(((((new PPolynomial(8)).multiply((ay.pow(4)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										ay)).multiply((cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(8)).multiply((ay.pow(3)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(3)))).multiply((bx.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(
										(bx.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((bx.pow(3)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(16)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(
										(bx.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(3)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(12)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(3)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((bx.pow(4)))).multiply(y)))).add(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(bx.pow(4)))).multiply(y)))).subtract(
								(((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(4)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(4)))).multiply(y)))).add(
								(((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										(cx.pow(2)))).multiply(by)).multiply(y)))).subtract(
								(((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										(cx.pow(3)))).multiply(by)).multiply(y)))).add(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(4)))).multiply(by)).multiply(y)))).add(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(by)).multiply(
										y)))).subtract(
								((((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply((ay.pow(2)))).multiply(by)).multiply(
										y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply(by)).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply(by)).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										by)).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply(y)))).subtract(
								((((((new PPolynomial(12)).multiply((ax.pow(3)))).multiply(
										cx)).multiply((cy.pow(2)))).multiply(by)).multiply(
										y)))).add(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(by)).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(by)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(by)).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(by)).multiply(
										y)))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(4)))).multiply(by)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										bx)).multiply(by)).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(bx)).multiply(by)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										bx)).multiply(by)).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(bx)).multiply(
										by)).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(bx)).multiply(
										by)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(by)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(4)))).multiply(
										bx)).multiply(by)).multiply(y)))).add(
								((((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply(by)).multiply(y)))).subtract(
								(((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										by)).multiply(y)))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										y)))).add(((((((new PPolynomial(16)).multiply(ax)).multiply(
								(cx.pow(3)))).multiply((bx.pow(2)))).multiply(by)).multiply(
								y)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										by)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										y)))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((cy.pow(4)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(bx.pow(3)))).multiply(by)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((by.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										ay)).multiply((by.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										ay)).multiply(
										(by.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cy)).multiply(
										(by.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(cy)).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										cy)).multiply((by.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((by.pow(2)))).multiply(
										y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((by.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(3)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply((by.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(12)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(bx)).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										cy)).multiply(
										bx)).multiply((by.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(
										bx)).multiply((by.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((bx.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										ay)).multiply((bx.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((bx.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((by.pow(3)))).multiply(y)))).add(
								(((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(by.pow(3)))).multiply(y)))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(by.pow(3)))).multiply(y)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((by.pow(3)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((by.pow(3)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										bx)).multiply((by.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										bx)).multiply((by.pow(3)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										bx)).multiply(
										(by.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										bx)).multiply((by.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply((by.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((by.pow(4)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(by.pow(4)))).multiply(y)))).add(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(4)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((by.pow(4)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(4)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(ay)).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										ay)).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(24)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(cy)).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(cy)).multiply(
										x)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply(x)).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply((cy.pow(2)))).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((cy.pow(2)))).multiply(
										x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										(cy.pow(2)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply((cy.pow(3)))).multiply(x)).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(bx)).multiply(
										x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(4)))).multiply(
										ay)).multiply(
										bx)).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(bx)).multiply(x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										x)).multiply(
										y)))).add(
								((((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(4)))).multiply(
										cy)).multiply(
										bx)).multiply(x)).multiply(y)))).subtract(
								(((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										ay)).multiply((cy.pow(2)))).multiply(bx)).multiply(
										x)).multiply(
										y)))).add(
								((((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(4)))).multiply(
										bx)).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										(bx.pow(2)))).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply((bx.pow(2)))).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((bx.pow(2)))).multiply(
										x)).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((cx.pow(3)))).multiply(
										ay)).multiply((bx.pow(2)))).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										(bx.pow(2)))).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((bx.pow(2)))).multiply(x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										x)).multiply(y)))).subtract(
								(((((((new PPolynomial(40)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((bx.pow(2)))).multiply(
										x)).multiply(y)))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(x)).multiply(
										y)))).add(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(x)).multiply(
										y)))).add(
								((((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										cy)).multiply((bx.pow(3)))).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(bx.pow(4)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(4)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										by)).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(by)).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										by)).multiply(x)).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(by)).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(by)).multiply(
										x)).multiply(y)))).add(
								((((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(by)).multiply(
										x)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((cy.pow(2)))).multiply(by)).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(by)).multiply(
										x)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(by)).multiply(
										x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(4)))).multiply(
										by)).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(4)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(4)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply((ay.pow(4)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply(y)))).subtract(
								(((((((((new PPolynomial(32)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply(bx)).multiply(by)).multiply(
										x)).multiply(y)))).add(
								(((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply((cy.pow(4)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)).multiply(
										y)))).add((((((((new PPolynomial(8)).multiply(ax)).multiply(
								(cx.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
								x)).multiply(y)))).subtract(
								((((((new PPolynomial(16)).multiply((cx.pow(3)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)).multiply(y)))).add(
								((((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(by)).multiply(
										x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply(by)).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										(by.pow(2)))).multiply(x)).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((by.pow(2)))).multiply(
										x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										(by.pow(2)))).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										cy)).multiply((by.pow(2)))).multiply(x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(40)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(cy)).multiply((by.pow(2)))).multiply(
										x)).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((by.pow(2)))).multiply(
										x)).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(x)).multiply(
										y)))).add(((((((new PPolynomial(16)).multiply(ax)).multiply(
								(cy.pow(3)))).multiply((by.pow(2)))).multiply(x)).multiply(
								y)))).subtract(
								(((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).add(
								((((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).add(
								(((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										x)).multiply(
										y)))).add((((((((new PPolynomial(8)).multiply(ay)).multiply(
								(cy.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
								x)).multiply(y)))).subtract(
								((((((new PPolynomial(16)).multiply((cy.pow(3)))).multiply(
										bx)).multiply((by.pow(2)))).multiply(x)).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)).multiply(
										y)))).add(
								(((((((new PPolynomial(24)).multiply(cx)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(x)).multiply(
										y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((by.pow(3)))).multiply(x)).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(by.pow(3)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(3)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(3)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(3)))).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(3)))).multiply(x)).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(by.pow(4)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										(by.pow(4)))).multiply(x)).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((x.pow(2)))).multiply(
										y)))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										cy)).multiply(
										(x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										ay)).multiply((cy.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										(cy.pow(2)))).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										bx)).multiply((x.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(bx)).multiply(
										(x.pow(2)))).multiply(
										y)))).add((((((((new PPolynomial(8)).multiply(ax)).multiply(
								(cx.pow(2)))).multiply(ay)).multiply(bx)).multiply(
								(x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										bx)).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(12)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(bx)).multiply((x.pow(2)))).multiply(
										y)))).add((((((((new PPolynomial(8)).multiply(ax)).multiply(
								(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
								(x.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										(x.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((bx.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										ay)).multiply((bx.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(bx.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										by)).multiply(
										(x.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(cy)).multiply(by)).multiply(
										(x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(16)).multiply((ax.pow(3)))).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))).multiply(
										y)))).add((((((((new PPolynomial(8)).multiply(ax)).multiply(
								(cx.pow(2)))).multiply(bx)).multiply(by)).multiply(
								(x.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(4)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))).multiply(y)))).add(
								((((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(8)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(16)).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((x.pow(2)))).multiply(
										y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(bx.pow(3)))).multiply(
										by)).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(bx.pow(3)))).multiply(
										by)).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										ay)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(y)))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										bx)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).add(
								(((((((new PPolynomial(12)).multiply(cx)).multiply(ay)).multiply(
										bx)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										bx)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply((by.pow(2)))).multiply((x.pow(2)))).multiply(
										y)))).subtract(
								((((((new PPolynomial(4)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								(((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(bx)).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply(cx)).multiply(bx)).multiply(
										(by.pow(3)))).multiply((x.pow(2)))).multiply(y)))).subtract(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(by.pow(4)))).multiply(
										(x.pow(2)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply(cy)).multiply(
										(by.pow(4)))).multiply(
										(x.pow(2)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										ay)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										ay)).multiply((x.pow(3)))).multiply(y)))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										(x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										cy)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										bx)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										bx)).multiply((x.pow(3)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										bx)).multiply(
										(x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										bx)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(ay)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))).multiply(y)))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(4)).multiply(ay)).multiply(bx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))).multiply(y)))).add(
								((((((new PPolynomial(8)).multiply(cy)).multiply(bx)).multiply(
										(by.pow(2)))).multiply((x.pow(3)))).multiply(y)))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(by.pow(3)))).multiply(
										(x.pow(3)))).multiply(y)))).subtract(
								(((((new PPolynomial(4)).multiply(bx)).multiply(
										(by.pow(3)))).multiply(
										(x.pow(3)))).multiply(y)))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(cx.pow(2)))).multiply((y.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										(cx.pow(3)))).multiply((y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(4)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply((y.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(ay.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(4)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply(cy)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										ay)).multiply(cy)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(cy)).multiply(
										(y.pow(2)))))).subtract(
								((((new PPolynomial(2)).multiply((ax.pow(4)))).multiply(
										(cy.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((cy.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(3)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(3)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										bx)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(bx)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(3)))).multiply(bx)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(4)))).multiply(
										bx)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										cx)).multiply((ay.pow(2)))).multiply(bx)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply((cx.pow(3)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(4)))).multiply(
										bx)).multiply((y.pow(2)))))).subtract(
								(((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(bx)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply(bx)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((cy.pow(2)))).multiply(bx)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(bx)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(bx.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(bx.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(bx.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(22)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(4)))).multiply(
										(bx.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((bx.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(bx.pow(3)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(bx.pow(3)))).multiply((y.pow(2)))))).add(
								((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										(bx.pow(3)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										(bx.pow(3)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(3)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(4)))).multiply((y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(bx.pow(4)))).multiply((y.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(4)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(4)))).multiply((y.pow(2)))))).subtract(
								((((new PPolynomial(2)).multiply((cy.pow(2)))).multiply(
										(bx.pow(4)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply(by)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(ay)).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((cx.pow(4)))).multiply(
										ay)).multiply(
										by)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply(by)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										cy)).multiply(
										by)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										cy)).multiply(by)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										cy)).multiply(by)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply(by)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((cy.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(3)))).multiply(by)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(by)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(bx)).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(40)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(bx)).multiply(
										by)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((cx.pow(3)))).multiply(
										ay)).multiply(bx)).multiply(by)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(bx)).multiply(by)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply(bx)).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(3)))).multiply(
										cy)).multiply(
										bx)).multiply(by)).multiply((y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										by)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(3)))).multiply(
										bx)).multiply(by)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((bx.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										cy)).multiply((bx.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cy.pow(3)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(3)))).multiply(by)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cx)).multiply(cy)).multiply(
										(bx.pow(3)))).multiply(by)).multiply((y.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										(by.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(by.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(22)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(12)).multiply(ax)).multiply(
										(cx.pow(3)))).multiply(
										(by.pow(2)))).multiply((y.pow(2)))))).add(
								((((new PPolynomial(4)).multiply((cx.pow(4)))).multiply(
										(by.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										bx)).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(3)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(bx)).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((by.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((by.pow(3)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(by.pow(3)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(3)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(3)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										bx)).multiply((by.pow(3)))).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply((by.pow(3)))).multiply(
										(y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(by.pow(4)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(4)))).multiply((y.pow(2)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(by.pow(4)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										cx)).multiply(
										x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cx.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(ay.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply((ay.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										cy)).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(cy)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										cy)).multiply(x)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(cy.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										bx)).multiply(
										x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(cy)).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(bx.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										(bx.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply((bx.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(4)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply((bx.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(bx.pow(3)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(bx.pow(3)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(3)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(12)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(3)))).multiply(x)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(bx.pow(3)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(bx.pow(4)))).multiply(
										x)).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(bx.pow(4)))).multiply(
										x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										by)).multiply(x)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(by)).multiply(
										x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(3)))).multiply(
										by)).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(12)).multiply((ax.pow(3)))).multiply(
										cy)).multiply(by)).multiply(x)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(cy)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(by)).multiply(
										x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(bx)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(bx)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(bx)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply((ay.pow(3)))).multiply(
										bx)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(bx)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(bx)).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(bx)).multiply(by)).multiply(
										x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(12)).multiply(ax)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(bx.pow(3)))).multiply(
										by)).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(cy)).multiply(
										(bx.pow(3)))).multiply(
										by)).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply((by.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply((by.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((ax.pow(2)))).multiply(
										bx)).multiply((by.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										bx)).multiply((by.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply((cx.pow(2)))).multiply(
										bx)).multiply((by.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ay)).multiply(cy)).multiply(
										bx)).multiply((by.pow(2)))).multiply(x)).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(ay)).multiply(
										(by.pow(3)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cy)).multiply(
										(by.pow(3)))).multiply(x)).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ay)).multiply(bx)).multiply(
										(by.pow(3)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(cy)).multiply(bx)).multiply(
										(by.pow(3)))).multiply(x)).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(6)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((x.pow(2)))).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										cy)).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(cy.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										bx)).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										bx)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										bx)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										bx)).multiply((x.pow(2)))).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(bx)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(
										(bx.pow(3)))).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(bx.pow(3)))).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										by)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										by)).multiply((x.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ay.pow(3)))).multiply(
										by)).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										by)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										by)).multiply((x.pow(2)))).multiply((y.pow(2)))))).add(
								(((((((new PPolynomial(24)).multiply(ax)).multiply(ay)).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((((new PPolynomial(16)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply(by)).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply((x.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(2)).multiply((ax.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								(((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								((((((new PPolynomial(4)).multiply(ay)).multiply(cy)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(bx)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).subtract(
								((((((new PPolynomial(4)).multiply(cx)).multiply(bx)).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(6)).multiply((bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((x.pow(2)))).multiply(
										(y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(by.pow(3)))).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).subtract(
								(((((new PPolynomial(4)).multiply(cy)).multiply(
										(by.pow(3)))).multiply(
										(x.pow(2)))).multiply((y.pow(2)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										ay)).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(ay)).multiply((y.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(3)))).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(
										(y.pow(3)))))).subtract(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(cy.pow(2)))).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(ay)).multiply(bx)).multiply(
										(y.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(ay)).multiply(bx)).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(3)))).multiply(
										bx)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(cy)).multiply(bx)).multiply(
										(y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(bx.pow(2)))).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply((bx.pow(2)))).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(12)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(bx.pow(2)))).multiply((y.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((ay.pow(3)))).multiply(
										(bx.pow(2)))).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										(bx.pow(2)))).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(12)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(3)))).multiply((y.pow(3)))))).subtract(
								((((new PPolynomial(4)).multiply(ay)).multiply(
										(bx.pow(4)))).multiply(
										(y.pow(3)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(4)))).multiply(
										by)).multiply(
										(y.pow(3)))))).add(
								(((((new PPolynomial(12)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										by)).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(12)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(ay.pow(2)))).multiply(by)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cy.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										bx)).multiply(by)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(bx)).multiply(by)).multiply(
										(y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								((((((new PPolynomial(12)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((cy.pow(2)))).multiply(
										(bx.pow(2)))).multiply(by)).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(cx)).multiply(
										(bx.pow(3)))).multiply(
										by)).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										(by.pow(2)))).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										(by.pow(2)))).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										(by.pow(2)))).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply((by.pow(2)))).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(16)).multiply(cx)).multiply(ay)).multiply(
										bx)).multiply((by.pow(2)))).multiply(
										(y.pow(3)))))).subtract(
								((((((new PPolynomial(16)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply((by.pow(2)))).multiply(
										(y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(
										(by.pow(2)))).multiply((y.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										(by.pow(3)))).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(16)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(3)))).multiply((y.pow(3)))))).add(
								((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										(by.pow(3)))).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										ay)).multiply(
										x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										ay)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										ay)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										cy)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										bx)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										bx)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										cy)).multiply(
										bx)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(ay)).multiply(
										(cy.pow(2)))).multiply(
										bx)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(4)).multiply(ax)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(x)).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(bx.pow(3)))).multiply(
										x)).multiply((y.pow(3)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										by)).multiply(
										x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(ay.pow(2)))).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(
										(cy.pow(2)))).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply((ay.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply((cy.pow(2)))).multiply(
										bx)).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(4)).multiply(ax)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply(x)).multiply((y.pow(3)))))).subtract(
								(((((new PPolynomial(4)).multiply((bx.pow(3)))).multiply(
										by)).multiply(
										x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(ay)).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cy)).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(3)))))).subtract(
								((((((new PPolynomial(8)).multiply(ay)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(3)))))).add(
								((((((new PPolynomial(8)).multiply(cy)).multiply(bx)).multiply(
										(by.pow(2)))).multiply(x)).multiply((y.pow(3)))))).add(
								((ax.pow(4)).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(3)))).multiply(
										cx)).multiply(
										(y.pow(4)))))).add(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(cx.pow(2)))).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(ay.pow(2)))).multiply((y.pow(4)))))).subtract(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										cy)).multiply((y.pow(4)))))).add(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(cy)).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cx)).multiply(
										bx)).multiply((y.pow(4)))))).subtract(
								(((((new PPolynomial(8)).multiply(ax)).multiply(
										(cx.pow(2)))).multiply(
										bx)).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(8)).multiply(cx)).multiply(
										(ay.pow(2)))).multiply(
										bx)).multiply((y.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										cy)).multiply(bx)).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(2)).multiply((ax.pow(2)))).multiply(
										(bx.pow(2)))).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ax)).multiply(cx)).multiply(
										(bx.pow(2)))).multiply((y.pow(4)))))).add(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(bx.pow(2)))).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ay.pow(2)))).multiply(
										(bx.pow(2)))).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply(cx)).multiply(
										(bx.pow(3)))).multiply(
										(y.pow(4)))))).add(((bx.pow(4)).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										ay)).multiply(
										by)).multiply((y.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										ay)).multiply(by)).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(8)).multiply((cx.pow(2)))).multiply(
										ay)).multiply(
										by)).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										cy)).multiply(
										by)).multiply((y.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										cy)).multiply(by)).multiply((y.pow(4)))))).subtract(
								((((((new PPolynomial(8)).multiply(cx)).multiply(ay)).multiply(
										bx)).multiply(by)).multiply((y.pow(4)))))).add(
								((((((new PPolynomial(8)).multiply(cx)).multiply(cy)).multiply(
										bx)).multiply(by)).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(4)).multiply(ay)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply((y.pow(4)))))).subtract(
								(((((new PPolynomial(4)).multiply(cy)).multiply(
										(bx.pow(2)))).multiply(
										by)).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((ax.pow(2)))).multiply(
										(by.pow(2)))).multiply((y.pow(4)))))).add(
								(((((new PPolynomial(8)).multiply(ax)).multiply(cx)).multiply(
										(by.pow(2)))).multiply((y.pow(4)))))).subtract(
								((((new PPolynomial(4)).multiply((cx.pow(2)))).multiply(
										(by.pow(2)))).multiply((y.pow(4))))));

				botanaPolynomials = new PPolynomial[1];

				botanaPolynomials[0] = p;
				return botanaPolynomials;

			}
			throw new NoSymbolicParametersException();

		} else if (type == GeoConicNDConstants.CONIC_HYPERBOLA) {
			GeoPoint F1 = getA2d();
			GeoPoint F2 = getB2d();
			GeoPoint Q = getC2d();

			if (F1 != null && F2 != null && Q != null) {
				PVariable[] vA = F1.getBotanaVars(F1);
				PVariable[] vB = F2.getBotanaVars(F2);
				PVariable[] vC = Q.getBotanaVars(Q);

				if (botanaVars == null) {
					botanaVars = new PVariable[12];
					// P - point of hyperbola
					botanaVars[0] = new PVariable(kernel);
					botanaVars[1] = new PVariable(kernel);
					// auxiliary variables
					botanaVars[2] = new PVariable(kernel);
					botanaVars[3] = new PVariable(kernel);
					botanaVars[4] = new PVariable(kernel);
					botanaVars[5] = new PVariable(kernel);
					// A
					botanaVars[6] = vA[0];
					botanaVars[7] = vA[1];
					// B
					botanaVars[8] = vB[0];
					botanaVars[9] = vB[1];
					// C
					botanaVars[10] = vC[0];
					botanaVars[11] = vC[1];
				}

				botanaPolynomials = new PPolynomial[5];

				PPolynomial d1 = new PPolynomial(botanaVars[2]);
				PPolynomial d2 = new PPolynomial(botanaVars[3]);
				PPolynomial e1 = new PPolynomial(botanaVars[4]);
				PPolynomial e2 = new PPolynomial(botanaVars[5]);

				// d1-d2 = e1-e2
				botanaPolynomials[0] = d1.subtract(d2).subtract(e1).add(e2);

				// d1^2=Polynomial.sqrDistance(a1,a2,c1,c2)
				botanaPolynomials[1] = PPolynomial
						.sqrDistance(vA[0], vA[1], vC[0], vC[1])
						.subtract(d1.multiply(d1));

				// d2^2=Polynomial.sqrDistance(b1,b2,c1,c2)
				botanaPolynomials[2] = PPolynomial
						.sqrDistance(vB[0], vB[1], vC[0], vC[1])
						.subtract(d2.multiply(d2));

				// e1^2=Polynomial.sqrDistance(a1,a2,p1,p2)
				botanaPolynomials[3] = PPolynomial
						.sqrDistance(vA[0], vA[1], botanaVars[0], botanaVars[1])
						.subtract(e1.multiply(e1));

				// e2^2=Polynomial.sqrDistance(b1,b2,p1,p2)
				botanaPolynomials[4] = PPolynomial
						.sqrDistance(vB[0], vB[1], botanaVars[0], botanaVars[1])
						.subtract(e2.multiply(e2));
				return botanaPolynomials;
			}
			throw new NoSymbolicParametersException();

		} else {
			throw new NoSymbolicParametersException();
		}
	}

}
