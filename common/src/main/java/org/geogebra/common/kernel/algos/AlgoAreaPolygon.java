/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * Area of polygon P[0], ..., P[n]
 *
 */

package org.geogebra.common.kernel.algos;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoPolygon;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

/**
 * Computes the area of a polygon
 * 
 * @author mathieu
 */
public class AlgoAreaPolygon extends AlgoElement
		implements SymbolicParametersBotanaAlgo {

	private GeoPolygon polygon; // input
	private GeoNumeric area; // output

	private PVariable[] botanaVars;
	private String[] botanaVarsDescr;

	/**
	 * @param cons
	 *            construction
	 * @param polygon
	 *            polygon
	 */
	public AlgoAreaPolygon(Construction cons, 
			GeoPolygon polygon) {
		super(cons);
		this.polygon = polygon;
		area = new GeoNumeric(cons);
		setInputOutput(); // for AlgoElement

		// compute angle
		compute();
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
		input = new GeoElement[1];
		input[0] = polygon;

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

	// calc area of conic c
	@Override
	public final void compute() {
		/*
		 * if (!polygon.isDefined()) { area.setUndefined(); return; }
		 */

		area.setValue(polygon.getArea());
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo)
			throws NoSymbolicParametersException {
		GeoPointND[] pointsOfPolygon = polygon.getPoints();
		if (botanaVars == null) {
			botanaVars = new PVariable[pointsOfPolygon.length * 2];
			botanaVarsDescr = new String[pointsOfPolygon.length * 2];
			for (int i = 0; i < pointsOfPolygon.length; i++) {
				PVariable[] currentPointBotanavars = ((GeoPoint) pointsOfPolygon[i])
						.getBotanaVars(pointsOfPolygon[i]);
				botanaVars[2 * i] = currentPointBotanavars[0];
				botanaVarsDescr[2 * i] = "The x value of point " + i + 1 + " of the polygon";
				botanaVars[2 * i + 1] = currentPointBotanavars[1];
				botanaVarsDescr[2 * i + 1] = "The y value of point " + i + 1 + " of the polygon";
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
		return null;
	}

	@Override
	public void reset() {
		botanaVars = null;
		botanaVarsDescr = null;
	}

}
