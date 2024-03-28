/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.algos;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Region;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

/**
 * Point in region algorithm
 * 
 * @author mathieu
 *
 */
public class AlgoPointInRegion extends AlgoElement implements SymbolicParametersBotanaAlgo {

	protected Region region; // input
	protected GeoPoint P; // output

	private PPolynomial[] polynomials;
    private PVariable[] botanaVars;
	private String [] botanaVarsDescr;

	/**
	 * @param cons
	 *            construction
	 * @param region
	 *            region
	 */
	public AlgoPointInRegion(Construction cons, Region region) {
		super(cons);
		this.region = region;
	}

	/**
	 * @param cons
	 *            construction
	 * @param label
	 *            output label
	 * @param region
	 *            region
	 * @param x
	 *            estimated x-coord
	 * @param y
	 *            estimated y-coord
	 */
	public AlgoPointInRegion(Construction cons, String label, Region region,
			double x, double y) {

		this(cons, region);

		P = new GeoPoint(cons, region);
		P.setCoords(x, y, 1.0);

		setInputOutput(); // for AlgoElement

		compute();
		P.setLabel(label);
	}

	@Override
	public Commands getClassName() {
		return Commands.PointIn;
	}

	@Override
	public int getRelatedModeID() {
		return EuclidianConstants.MODE_POINT_ON_OBJECT;
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {

		input = new GeoElement[1];
		input[0] = region.toGeoElement();

		setOutputLength(1);
		setOutput(0, P);
		setDependencies(); // done by AlgoElement
	}

	/**
	 * returns the point
	 * 
	 * @return resulting point
	 */
	public GeoPoint getP() {
		return P;
	}

	/**
	 * Returns the region
	 * 
	 * @return region
	 */
	Region getRegion() {
		return region;
	}

	@Override
	public void compute() {

		if (region.isDefined()) {
			region.regionChanged(P);
			P.updateCoords();
		} else {
			P.setUndefined();
		}
	}

	@Override
	final public String toString(StringTemplate tpl) {
		// Michael Borcherds 2008-03-30
		// simplified to allow better Chinese translation
		return getLoc().getPlainDefault("PointInA", "Point in %0",
				input[0].getLabel(tpl));

	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) throws NoSymbolicParametersException {
		if (botanaVars != null) {
			return botanaVars;
		}
		botanaVars = new PVariable[2];
		botanaVars[0] = new PVariable(kernel);
		botanaVars[1] = new PVariable(kernel);
		botanaVarsDescr[0] = "x value of the point";
		botanaVarsDescr[1] = "y value of the point";
		return botanaVars;
	}

	@Override
	public String[] getBotanaVarsDescr(GeoElementND geo) throws NoSymbolicParametersException {
		return botanaVarsDescr;
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		return null;
	}
}
