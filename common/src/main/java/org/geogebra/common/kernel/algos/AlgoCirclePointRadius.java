/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * AlgoCirclePointRadius.java
 *
 * Created on 15. November 2001, 21:37
 */

package org.geogebra.common.kernel.algos;

import java.math.BigInteger;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumberValue;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoQuadricND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.main.Localization;

/**
 * 
 * @author Markus added TYPE_SEGMENT Michael Borcherds 2008-03-14
 */
public class AlgoCirclePointRadius extends AlgoSphereNDPointRadius implements
		AlgoCirclePointRadiusInterface, SymbolicParametersBotanaAlgo {

	private PVariable[] botanaVars;
	private String[] botanaVarsDescr;
	private PPolynomial[] botanaPolynomials;

	/**
	 * @param cons
	 *            construction
	 * @param M
	 *            center
	 * @param r
	 *            radius
	 */
	public AlgoCirclePointRadius(Construction cons, GeoPoint M,
			GeoNumberValue r) {
		super(cons, M, r);
	}

	AlgoCirclePointRadius(Construction cons, GeoPoint M, GeoSegment rgeo) {

		super(cons, M, rgeo);
	}

	@Override
	protected GeoQuadricND createSphereND(Construction cons1) {
		return new GeoConic(cons1);
	}

	@Override
	public Commands getClassName() {
		return Commands.Circle;
	}

	@Override
	public int getRelatedModeID() {
		switch (super.getType()) {
		case AlgoSphereNDPointRadius.TYPE_RADIUS:
			return EuclidianConstants.MODE_CIRCLE_POINT_RADIUS;
		default:
			return EuclidianConstants.MODE_COMPASSES;
		}
	}

	/**
	 * @return resulting conic
	 */
	public GeoConic getCircle() {
		return (GeoConic) getSphereND();
	}

	@Override
	final public String toString(StringTemplate tpl) {
		// Michael Borcherds 2008-03-30
		// simplified to allow better Chinese translation
		return getLoc().getPlainDefault("CircleWithCenterAandRadiusB",
				"Circle with center %0 and radius %1",
				getM().getLabel(tpl), getRGeo().getLabel(tpl));
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) {
		return botanaVars;
	}
	@Override
	public String[] getBotanaVarsDescr(GeoElementND geo) {
		return botanaVarsDescr;
	}
	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		boolean cachable = false; // This must be disabled. :-(
		GeoNumeric num = null;

		if (!(this.getInput(1) instanceof GeoSegment) && this.getInput(1) instanceof GeoNumeric) {
			num = (GeoNumeric) this.getInput(1);
			if (num != null && num.isNumberValue() && num.isDrawable()) {
				cachable = false; // this may be a slider or a non-constant value, so don't cache
			}
			AlgoElement ae = num.getParentAlgorithm();
			if (ae instanceof AlgoDependentNumber) {
				cachable = false; // this is a dependent expression, so don't cache
			}

		}
		if (botanaPolynomials != null && cachable) {
			return botanaPolynomials;
		}

		GeoPoint P = (GeoPoint) this.getInput(0);

		/* SPECIAL CASE 1: radius is a segment */
		if (this.getInput(1) instanceof GeoSegment) {
			/*
			 * Here we do the full work for this segment. It would be nicer to
			 * put this code into GeoSegment but we need to use the square of
			 * the length of the segment in this special case.
			 */
			GeoSegment s = (GeoSegment) this.getInput(1);
			if (botanaVars == null) {
				PVariable[] centerBotanaVars = P.getBotanaVars(P);
				botanaVars = new PVariable[4];
				botanaVarsDescr = new String[4];
				// center P
				botanaVars[0] = centerBotanaVars[0];
				botanaVars[1] = centerBotanaVars[1];
				// point C on the circle
				botanaVars[2] = new PVariable(kernel);
				botanaVars[3] = new PVariable(kernel);
				// botanaVarsDescr[2] = "The x value of a point of " + geo.getLabelSimple();
				// botanaVarsDescr[3] = "The y value of a point of " + geo.getLabelSimple();
				setBotanaVarsDescr1(2, "x", geo);
				setBotanaVarsDescr1(3, "y", geo);
			}
			GeoPoint A = s.getStartPoint();
			GeoPoint B = s.getEndPoint();
			PVariable[] ABotanaVars = A.getBotanaVars(A);
			PVariable[] BBotanaVars = B.getBotanaVars(B);

			botanaPolynomials = new PPolynomial[2];
			// C-P == B-A <=> C-P-B+A == 0
			botanaPolynomials[0] = new PPolynomial(botanaVars[2])
					.subtract(new PPolynomial(botanaVars[0]))
					.subtract(new PPolynomial(BBotanaVars[0]))
					.add(new PPolynomial(ABotanaVars[0]));
			botanaPolynomials[1] = new PPolynomial(botanaVars[3])
					.subtract(new PPolynomial(botanaVars[1]))
					.subtract(new PPolynomial(BBotanaVars[1]))
					.add(new PPolynomial(ABotanaVars[1]));
			// done for both coordinates!
			return botanaPolynomials;
		}

		/* SPECIAL CASE 2: radius is an expression */

		if (P == null || num == null) {
			throw new NoSymbolicParametersException();
		}

		if (botanaVars == null) {
			PVariable[] centerBotanaVars = P.getBotanaVars(P);
			botanaVars = new PVariable[4];
			botanaVarsDescr = new String[4];
			// center
			botanaVars[0] = centerBotanaVars[0];
			botanaVars[1] = centerBotanaVars[1];
			// point on circle
			botanaVars[2] = new PVariable(kernel);
			botanaVars[3] = new PVariable(kernel);
			botanaVarsDescr[2] = "The x value of a helper point of the circle such that "
					+ P.getLabelSimple() + " is moved right by " + num.getDefinitionDescription(StringTemplate.defaultTemplate) + " units";
			botanaVarsDescr[3] = "The y value of " + P.getLabelSimple();
			// radius
			// botanaVars[4] = new PVariable(kernel);
		}

		PPolynomial[] extraPolys = null;
		// FIXME: This does not work if the expression is based on a slider.
		if (num.getParentAlgorithm() instanceof AlgoDependentNumber) {
			extraPolys = num.getBotanaPolynomials(num);
			botanaPolynomials = new PPolynomial[extraPolys.length + 2];
		} else {
			if (num.isNumberValue()) { // the fix radius or slider value will be another equation
				botanaPolynomials = new PPolynomial[3];
			} else {
				botanaPolynomials = new PPolynomial[2];
			}
		}

		/*
		 * Note that we read the Botana variables just after reading the Botana
		 * polynomials since the variables are set after the polys are set.
		 */
		PVariable[] radiusBotanaVars = num.getBotanaVars(num);
		int k = 0;
		// r^2
		// PPolynomial sqrR = PPolynomial.sqr(new PPolynomial(radiusBotanaVars[0]));
		PPolynomial R = new PPolynomial(radiusBotanaVars[0]);
		// define radius
		if (extraPolys != null) {
			botanaPolynomials = new PPolynomial[extraPolys.length + 2];
			for (k = 0; k < extraPolys.length; k++) {
				botanaPolynomials[k] = extraPolys[k];
			}
		}

		// define circle
		// botanaPolynomials[k] = PPolynomial.sqrDistance(botanaVars[0],
		//				botanaVars[1], botanaVars[2], botanaVars[3]).subtract(sqrR);

		// Put the circumpoint to East, to distance R:
		botanaPolynomials[k] = new PPolynomial(botanaVars[2])
				.subtract(new PPolynomial(botanaVars[0]))
				.subtract(R);
		botanaPolynomials[k+1] = new PPolynomial(botanaVars[3])
				.subtract(new PPolynomial(botanaVars[1]));
		// done for both coordinates!

		// Solving TP-9:
		if (!(num.getParentAlgorithm() instanceof AlgoDependentNumber) &&
				num.isNumberValue()) {
			k+=2;
			BigInteger[] q = new BigInteger[2]; // borrowed from ProverBotanasMethod
			double x = num.getValue();
			/*
			 * Use the fraction P/Q according to the current kernel
			 * setting. We use the P/Q=x <=> P-Q*x=0 equation.
			 */
			q = kernel.doubleToRational(x);
			botanaPolynomials[k] = new PPolynomial(q[0])
					.subtract(new PPolynomial(radiusBotanaVars[0])
							.multiply(new PPolynomial(q[1])));
		}

		return botanaPolynomials;

	}

	@Override
	public void reset() {
		botanaVars = null;
		botanaVarsDescr = null;
		botanaPolynomials = null;
	}

	void setBotanaVarsDescr1(int pos, String coord, GeoElementND geo) {
		Localization loc = geo.getKernel().getLocalization();
		botanaVarsDescr[pos] = getLoc().getPlainDefault("AValueOfAPointOfB",
				"%0 value of a point of %1",coord, geo.getLabelSimple());
	}

}
