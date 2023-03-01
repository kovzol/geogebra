/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * AlgoRotatePoint.java
 *
 * Created on 24. September 2001, 21:37
 */

package org.geogebra.common.kernel.algos;

import java.math.BigInteger;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.NumberValue;
import org.geogebra.common.kernel.arithmetic.Polynomial;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.Dilateable;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoConicPart;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoNumberValue;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.matrix.Coords;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.util.debug.Log;

/**
 * 
 * @author Markus
 */
public class AlgoDilate extends AlgoTransformation
	implements  SymbolicParametersBotanaAlgo {

	protected GeoPointND S;
	private Dilateable out;
	private NumberValue r;
	private GeoElement rgeo;

	private PVariable[] botanaVars;
	private PPolynomial[] botanaPolynomials;

	/**
	 * Creates new labeled enlarge geo
	 * 
	 * @param cons
	 *            construction
	 * @param label
	 *            output label
	 * @param A
	 *            dilated geo
	 * @param r
	 *            coefficient
	 * @param S
	 *            dilation center
	 */
	AlgoDilate(Construction cons, String label, GeoElement A, GeoNumberValue r,
			GeoPointND S) {
		this(cons, A, r, S);
		outGeo.setLabel(label);
	}

	/**
	 * Creates new unlabeled enlarge geo
	 * 
	 * @param cons
	 *            construction
	 * @param A
	 *            dilated geo
	 * @param r
	 *            coefficient
	 * @param S
	 *            dilation center
	 */
	public AlgoDilate(Construction cons, GeoElement A, GeoNumberValue r,
			GeoPointND S) {
		super(cons);
		this.r = r;
		this.S = S;

		inGeo = A;
		rgeo = r.toGeoElement();

		outGeo = getResultTemplate(inGeo);
		if (outGeo instanceof Dilateable) {
			out = (Dilateable) outGeo;
		}

		setInputOutput();
		compute();
	}

	@Override
	public Commands getClassName() {
		return Commands.Dilate;
	}

	@Override
	public int getRelatedModeID() {
		return EuclidianConstants.MODE_DILATE_FROM_POINT;
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		input = new GeoElement[S == null ? 2 : 3];
		input[0] = inGeo;
		input[1] = rgeo;
		if (S != null) {
			input[2] = (GeoElement) S;
		}

		setOutputLength(1);
		setOutput(0, outGeo);
		setDependencies(); // done by AlgoElement
	}

	/**
	 * Returns the resulting GeoElement
	 * 
	 * @return the resulting GeoElement
	 */
	@Override
	public GeoElement getResult() {
		return outGeo;
	}

	@Override
	protected void setTransformedObject(GeoElement g, GeoElement g2) {
		inGeo = g;
		outGeo = g2;
		if (!(outGeo instanceof GeoList) && (outGeo instanceof Dilateable)) {
			out = (Dilateable) outGeo;
		}
	}

	// calc dilated point
	@Override
	public final void compute() {
		if (inGeo.isGeoList()) {
			transformList((GeoList) inGeo, (GeoList) outGeo);
			return;
		}

		setOutGeo();
		if (!out.isDefined()) {
			return;
		}

		out.dilate(r, getPointCoords());

		if (inGeo.isLimitedPath()) {
			this.transformLimitedPath(inGeo, outGeo);
		}
	}

	@Override
	final public String toString(StringTemplate tpl) {
		// Michael Borcherds 2008-03-30
		// simplified to allow better Chinese translation
		String sLabel = S == null ? cons.getOrigin().toValueString(tpl)
				: S.getLabel(tpl);
		return getLoc().getPlainDefault("ADilatedByFactorBfromC",
				"%0 dilated by factor %1 from %2", inGeo.getLabel(tpl),
				rgeo.getLabel(tpl), sLabel);

	}

	/**
	 * 
	 * @return point coords for dilate
	 */
	protected Coords getPointCoords() {
		if (S == null) {
			return Coords.O;
		}

		return S.getInhomCoords();
	}

	@Override
	protected void transformLimitedPath(GeoElement a, GeoElement b) {
		if (!(a instanceof GeoConicPart)) {
			super.transformLimitedPath(a, b);
		} else {
			super.transformLimitedConic(a, b);
		}

	}

	@Override
	public double getAreaScaleFactor() {
		return r.getDouble() * r.getDouble();
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) throws NoSymbolicParametersException {
		return botanaVars;
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		GeoNumeric num = null;
		boolean cachable = true;
		if (this.r instanceof GeoNumeric) {
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

		PVariable[] vS = ((GeoPoint) S.toGeoElement()).getBotanaVars(S);

		BigInteger[] q = new BigInteger[2]; // borrowed from ProverBotanasMethod
		double x = num.getValue();
		/*
		 * Use the fraction P/Q according to the current kernel
		 * setting. We use the P/Q=x <=> P-Q*x=0 equation.
		 */
		q = kernel.doubleToRational(x);

		PPolynomial sx = new PPolynomial(vS[0]);
		PPolynomial sy = new PPolynomial(vS[1]);

		if (inGeo.isGeoPoint()) {
			GeoPoint A = (GeoPoint) inGeo;
			PVariable[] vA = A.getBotanaVars(A);

			if (botanaVars == null) {
				botanaVars = new PVariable[2];
				// outGeo
				botanaVars[0] = new PVariable(geo.getKernel());
				botanaVars[1] = new PVariable(geo.getKernel());
			}

			botanaPolynomials = new PPolynomial[2];

			PPolynomial ax = new PPolynomial(vA[0]);
			PPolynomial ay = new PPolynomial(vA[1]);
			PPolynomial outX = new PPolynomial(botanaVars[0]);
			PPolynomial outY = new PPolynomial(botanaVars[1]);

			botanaPolynomials[0] = ((ax.subtract(sx)).multiply(new PPolynomial(q[0])))
					.subtract((outX.subtract(sx)).multiply(new PPolynomial(q[1])));
			botanaPolynomials[1] = ((ay.subtract(sy)).multiply(new PPolynomial(q[0])))
					.subtract((outY.subtract(sy)).multiply(new PPolynomial(q[1])));

			return botanaPolynomials;
		}

		if (inGeo.isGeoConic() && ((GeoConic) inGeo).isCircle()) {
			// Do the same as for points, but dilate both the center and the circumpoint.
			GeoConic A = (GeoConic) inGeo;
			PVariable[] vA = A.getBotanaVars(A);

			if (botanaVars == null) {
				botanaVars = new PVariable[4];
				// outGeo
				botanaVars[0] = new PVariable(geo.getKernel());
				botanaVars[1] = new PVariable(geo.getKernel());
				botanaVars[2] = new PVariable(geo.getKernel());
				botanaVars[3] = new PVariable(geo.getKernel());
			}

			botanaPolynomials = new PPolynomial[4];

			PPolynomial aox = new PPolynomial(vA[0]);
			PPolynomial aoy = new PPolynomial(vA[1]);
			PPolynomial apx = new PPolynomial(vA[2]);
			PPolynomial apy = new PPolynomial(vA[3]);
			PPolynomial outoX = new PPolynomial(botanaVars[0]);
			PPolynomial outoY = new PPolynomial(botanaVars[1]);
			PPolynomial outpX = new PPolynomial(botanaVars[2]);
			PPolynomial outpY = new PPolynomial(botanaVars[3]);

			botanaPolynomials[0] = ((aox.subtract(sx)).multiply(new PPolynomial(q[0])))
					.subtract((outoX.subtract(sx)).multiply(new PPolynomial(q[1])));
			botanaPolynomials[1] = ((aoy.subtract(sy)).multiply(new PPolynomial(q[0])))
					.subtract((outoY.subtract(sy)).multiply(new PPolynomial(q[1])));
			botanaPolynomials[2] = ((apx.subtract(sx)).multiply(new PPolynomial(q[0])))
					.subtract((outpX.subtract(sx)).multiply(new PPolynomial(q[1])));
			botanaPolynomials[3] = ((apy.subtract(sy)).multiply(new PPolynomial(q[0])))
					.subtract((outpY.subtract(sy)).multiply(new PPolynomial(q[1])));

			return botanaPolynomials;
		}

		Log.debug("unimplemented");
		// TODO: Implement missing cases (parabola, ellipse/hyperbola, algebraic curves)
		throw new NoSymbolicParametersException();

	}
}
