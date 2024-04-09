/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.algos;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.prover.AbstractProverReciosMethod;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.main.Localization;

public class AlgoLineBisector extends AlgoElement
		implements SymbolicParametersAlgo, SymbolicParametersBotanaAlgo {

	private GeoPoint A; // input
	private GeoPoint B; // input
	private GeoLine g; // output

	// temp
	private GeoPoint midPoint;
	private PPolynomial[] polynomials;
	private PPolynomial[] botanaPolynomials;
	private PVariable[] botanaVars;
	private String[] botanaVarsDescr;

	/** Creates new AlgoLineBisector */
	public AlgoLineBisector(Construction cons, String label, GeoPoint A,
			GeoPoint B) {
		super(cons);
		this.A = A;
		this.B = B;
		g = new GeoLine(cons);
		midPoint = new GeoPoint(cons);
		g.setStartPoint(midPoint);
		setInputOutput(); // for AlgoElement

		// compute bisector of A, B
		compute();
		g.setLabel(label);
	}

	@Override
	public Commands getClassName() {
		return Commands.LineBisector;
	}

	@Override
	public int getRelatedModeID() {
		return EuclidianConstants.MODE_LINE_BISECTOR;
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		input = new GeoElement[2];
		input[0] = A;
		input[1] = B;

		super.setOutputLength(1);
		super.setOutput(0, g);
		setDependencies(); // done by AlgoElement
	}

	public GeoLine getLine() {
		return g;
	}

	// Made public for LocusEqu
	public GeoPoint getA() {
		return A;
	}

	// Made public for LocusEqu
	public GeoPoint getB() {
		return B;
	}

	// Made public for LocusEqu
	public GeoPoint getMidPoint() {
		return midPoint;
	}

	// line through P normal to v
	@Override
	public final void compute() {
		// get inhomogenous coords
		double ax = A.inhomX;
		double ay = A.inhomY;
		double bx = B.inhomX;
		double by = B.inhomY;

		// comput line
		g.x = ax - bx;
		g.y = ay - by;
		midPoint.setCoords((ax + bx), (ay + by), 2.0);
		g.z = -(midPoint.x * g.x + midPoint.y * g.y) / 2.0;
	}

	@Override
	public SymbolicParameters getSymbolicParameters() {
		return new SymbolicParameters(this);
	}

	@Override
	public void getFreeVariables(HashSet<PVariable> variables)
			throws NoSymbolicParametersException {
		if (A != null && B != null) {
			A.getFreeVariables(variables);
			B.getFreeVariables(variables);
			return;
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public int[] getDegrees(AbstractProverReciosMethod a)
			throws NoSymbolicParametersException {
		if (A != null && B != null) {
			int[] degree1 = A.getDegrees(a);
			int[] degree2 = B.getDegrees(a);
			int[] result = new int[3];
			result[0] = Math.max(degree1[0] + degree1[2] + 2 * degree2[2],
					2 * degree1[2] + degree2[0] + degree2[2]);
			result[1] = Math.max(degree1[1] + degree1[2] + 2 * degree2[2],
					2 * degree1[2] + degree2[1] + degree2[2]);
			result[2] = 2 * Math.max(
					Math.max(degree1[2] + degree2[0], degree1[2] + degree2[1]),
					Math.max(degree1[0] + degree2[2], degree1[1] + degree2[2]));
			return result;
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public BigInteger[] getExactCoordinates(
			TreeMap<PVariable, BigInteger> values)
			throws NoSymbolicParametersException {
		if (A != null && B != null) {
			BigInteger[] coords1 = A.getExactCoordinates(values);
			BigInteger[] coords2 = B.getExactCoordinates(values);

			BigInteger[] result = new BigInteger[3];
			// 2 az bz (-az bx + ax bz)
			result[0] = BigInteger.valueOf(2).multiply(coords1[2])
					.multiply(coords2[2])
					.multiply(coords1[0].multiply(coords2[2])
							.subtract(coords2[0].multiply(coords1[2])));
			// 2 az bz (-az by + ay bz)
			result[1] = BigInteger.valueOf(2).multiply(coords1[2])
					.multiply(coords2[2])
					.multiply(coords1[1].multiply(coords2[2])
							.subtract(coords2[1].multiply(coords1[2])));
			// (az bx - ax bz) (az bx + ax bz) - (-az by + ay bz) (az by + ay
			// bz)
			result[2] = coords1[2].multiply(coords2[0])
					.subtract(coords1[0].multiply(coords2[2]))
					.multiply(coords1[2].multiply(coords2[0])
							.add(coords1[0].multiply(coords2[2])))
					.subtract(coords1[1].multiply(coords2[2])
							.subtract(coords1[2].multiply(coords2[1]))
							.multiply(coords1[1].multiply(coords2[2])
									.add(coords1[2].multiply(coords2[1]))));
			return result;
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public PPolynomial[] getPolynomials() throws NoSymbolicParametersException {
		if (polynomials != null) {
			return polynomials;
		}
		if (A != null && B != null) {
			PPolynomial[] coords1 = A.getPolynomials();
			PPolynomial[] coords2 = B.getPolynomials();

			polynomials = new PPolynomial[3];
			// 2 az bz (-az bx + ax bz)
			polynomials[0] = (new PPolynomial(2)).multiply(coords1[2])
					.multiply(coords2[2])
					.multiply(coords1[0].multiply(coords2[2])
							.subtract(coords2[0].multiply(coords1[2])));
			// 2 az bz (-az by + ay bz)
			polynomials[1] = (new PPolynomial(2)).multiply(coords1[2])
					.multiply(coords2[2])
					.multiply(coords1[1].multiply(coords2[2])
							.subtract(coords2[1].multiply(coords1[2])));
			// (az bx - ax bz) (az bx + ax bz) - (-az by + ay bz) (az by + ay
			// bz)
			polynomials[2] = coords1[2].multiply(coords2[0])
					.subtract(coords1[0].multiply(coords2[2]))
					.multiply(coords1[2].multiply(coords2[0])
							.add(coords1[0].multiply(coords2[2])))
					.subtract(coords1[1].multiply(coords2[2])
							.subtract(coords1[2].multiply(coords2[1]))
							.multiply(coords1[1].multiply(coords2[2])
									.add(coords1[2].multiply(coords2[1]))));
			return polynomials;
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) {
		return botanaVars;
	}

	@Override
	public String[] getBotanaVarsDescr(GeoElementND geo) throws NoSymbolicParametersException {
		return botanaVarsDescr;
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {

		if (botanaPolynomials != null) {
			return botanaPolynomials;
		}
		if (A != null && B != null) {
			PVariable[] vA = A.getBotanaVars(A);
			PVariable[] vB = B.getBotanaVars(B);

			if (botanaVars == null) {
				botanaVars = new PVariable[4]; // storing 4 new variables (C, D)
				botanaVars[0] = new PVariable(kernel);
				botanaVars[1] = new PVariable(kernel);
				botanaVars[2] = new PVariable(kernel);
				botanaVars[3] = new PVariable(kernel);
				botanaVarsDescr = new String[4];
				// botanaVarsDescr[0] = "The x value of midpoint of " + A.getLabelSimple() + B.getLabelSimple();
				// botanaVarsDescr[1] = "The y value of midpoint of " + A.getLabelSimple() + B.getLabelSimple();
				setBotanaVarsDescr1(0, "x");
				setBotanaVarsDescr1(1, "y");

				// botanaVarsDescr[2] = "The x value of rotation of point " + A.getLabelSimple()
				//		+ " around the midpoint of " + A.getLabelSimple() + B.getLabelSimple()
				//		+ " by 90 degrees";
				// botanaVarsDescr[3] = "The y value of rotation of point " + A.getLabelSimple()
				//		+ " around the midpoint of " + A.getLabelSimple() + B.getLabelSimple()
				//		+ " by 90 degrees";
				setBotanaVarsDescr2(2, "x");
				setBotanaVarsDescr2(3, "y");
			}

			botanaPolynomials = SymbolicParameters
					.botanaPolynomialsLineBisector(vA[0], vA[1], vB[0], vB[1],
							botanaVars);

			return botanaPolynomials;
		}
		throw new NoSymbolicParametersException();
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
	final public String toString(StringTemplate tpl) {

		return getLoc().getPlainDefault("LineBisectorOfA",
				"Perpendicular Bisector of %0",
				A.getLabel(tpl) + B.getLabel(tpl));
	}

	@Override
	public void reset() {
		botanaVars = null;
		botanaVarsDescr = null;
		botanaPolynomials = null;
	}

	void setBotanaVarsDescr1(int pos, String coord) {
		Localization loc = A.kernel.getLocalization();
		botanaVarsDescr[pos] = loc.getPlainDefault("AValueOfMidpointOfB",
				"%0 value of midpoint of %1",
				coord, A.getLabelSimple() + B.getLabelSimple());
	}
	void setBotanaVarsDescr2(int pos, String coord) {
		Localization loc = A.kernel.getLocalization();
		botanaVarsDescr[pos] = loc.getPlainDefault("AValueOfRotationOfPointBAroundTheMidpointOfCByDDegrees",
				"%0 value of rotation of point %1 around the midpoint of %2 by %3 degrees",
				coord, A.getLabelSimple(), A.getLabelSimple() + B.getLabelSimple(), "90");
	}

}
