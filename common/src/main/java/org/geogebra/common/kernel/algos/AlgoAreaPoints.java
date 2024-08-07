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
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

/**
 * Computes area of polygon P[0], ..., P[n]
 *
 */
public class AlgoAreaPoints extends AlgoElement
		implements SymbolicParametersBotanaAlgo {

	protected GeoPointND[] P; // input
	protected GeoNumeric area; // output

	private PVariable[] botanaVars;
	private String[] botanaVarsDescr;

	/**
	 * @param cons
	 *            construction
	 * @param P
	 *            points
	 */
	public AlgoAreaPoints(Construction cons, GeoPointND[] P) {
		super(cons);
		this.P = P;
		createOutput(cons);
		setInputOutput(); // for AlgoElement

		// compute
		initCoords();
		compute();
	}

	/**
	 * init Coords values
	 */
	protected void initCoords() {
		// none here
	}

	protected void createOutput(Construction cons1) {
		area = new GeoNumeric(cons1);
	}

	@Override
	public Commands getClassName() {
		return Commands.Area;
	}

	@Override
	public int getRelatedModeID() {
		return EuclidianConstants.MODE_AREA;
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		input = new GeoElement[P.length];
		for (int i = 0; i < P.length; i++) {
			input[i] = (GeoElement) P[i];
		}

		setOutputLength(1);
		setOutput(0, area);
		setDependencies(); // done by AlgoElement
	}

	/**
	 * @return output area
	 */
	public GeoNumeric getArea() {
		return area;
	}

	GeoPointND[] getPoints() {
		return P;
	}

	// calc area of polygon P[0], ..., P[n]
	// angle in range [0, pi]
	@Override
	public void compute() {
		area.setValue(Math.abs(AlgoPolygon.calcAreaWithSign(P)));
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo)
			throws NoSymbolicParametersException {
		GeoPointND[] points = getPoints();
		if (botanaVars == null) {
			botanaVars = new PVariable[points.length * 2];
			botanaVarsDescr = new String[points.length * 2];
			for (int i = 0; i < points.length; i++) {
				PVariable[] currentPointBotanavars = ((GeoPoint) points[i])
						.getBotanaVars(points[i]);
				botanaVars[2 * i] = currentPointBotanavars[0];
				botanaVarsDescr[2 * i] = "The x value of point " + i + 1 + " of the polygon";
				botanaVars[2 * i + 1] = currentPointBotanavars[1];
				botanaVarsDescr[2 * i + 1] = "The y value of point " + i + 1 + " of the polygon";
				// TODO: The descriptions seem unused. Check.
			}
		}
		return botanaVars;
	}
	public String[] getBotanaVarsDescr(GeoElementND geo) {
		return botanaVarsDescr;
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void reset() {
		botanaVars = null;
		botanaVarsDescr = null;
	}

}
