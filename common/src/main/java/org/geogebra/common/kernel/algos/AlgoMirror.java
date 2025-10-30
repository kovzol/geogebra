/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * AlgoMirrorPointPoint.java
 *
 * Created on 24. September 2001, 21:37
 */

package org.geogebra.common.kernel.algos;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.TreeMap;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.Region;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.ConicMirrorable;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoConicPart;
import org.geogebra.common.kernel.geos.GeoCurveCartesian;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoPoly;
import org.geogebra.common.kernel.geos.GeoRay;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.geos.GeoVec2D;
import org.geogebra.common.kernel.geos.Mirrorable;
import org.geogebra.common.kernel.implicit.GeoImplicit;
import org.geogebra.common.kernel.kernelND.GeoConicNDConstants;
import org.geogebra.common.kernel.kernelND.GeoConicPartND;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoLineND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.matrix.Coords;
import org.geogebra.common.kernel.prover.AbstractProverReciosMethod;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.adapters.MirrorAdapter;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.util.DoubleUtil;
import org.geogebra.common.util.MyMath;

/**
 *
 * @author Markus
 */
public class AlgoMirror extends AlgoTransformation implements
		SymbolicParametersBotanaAlgo, SymbolicParametersAlgo {

	protected Mirrorable out;
	private GeoLineND mirrorLine;
	protected GeoPointND mirrorPoint;
	private GeoConic mirrorConic;
	protected GeoElement mirror;

	private GeoPoint transformedPoint;
	private MirrorAdapter mirrorBotana;

	private PPolynomial[] polynomials;


	/**
	 * Creates new "mirror at point" algo
	 * 
	 * @param cons
	 *            construction
	 * @param label
	 *            output label
	 * @param in
	 *            source geo
	 * @param p
	 *            mirror point
	 */
	protected AlgoMirror(Construction cons, String label, GeoElement in,
			GeoPointND p) {

		this(cons, in, p);
		outGeo.setLabel(label);
	}

	/**
	 * Creates new "mirror at point" algo
	 * 
	 * @param cons
	 *            construction
	 * @param in
	 *            source geo
	 * @param p
	 *            mirror point
	 */
	public AlgoMirror(Construction cons, GeoElement in, GeoPointND p) {

		this(cons);
		mirrorPoint = p;
		endOfConstruction(cons, in, (GeoElement) p);
	}

	/**
	 * Creates new "mirror at conic" algo
	 * 
	 * @param cons
	 *            construction
	 * @param label
	 *            output label
	 * @param in
	 *            source geo
	 * @param c
	 *            mirror conic
	 */
	AlgoMirror(Construction cons, String label, GeoElement in, GeoConic c) {

		this(cons, in, c);
		outGeo.setLabel(label);
	}

	/**
	 * Creates new "mirror at conic" algo
	 * 
	 * @param cons
	 *            construction
	 * @param in
	 *            source geo
	 * @param c
	 *            mirror conic
	 */
	public AlgoMirror(Construction cons, GeoElement in, GeoConic c) {

		this(cons);
		mirrorConic = c;
		endOfConstruction(cons, in, c);
	}

	/**
	 * Creates new "mirror at line" algo
	 * 
	 * @param cons
	 *            construction
	 * @param label
	 *            output label
	 * @param in
	 *            source geo
	 * @param g
	 *            mirror line
	 */
	AlgoMirror(Construction cons, String label, GeoElement in, GeoLineND g) {

		this(cons, in, g);
		outGeo.setLabel(label);
	}

	/**
	 * Creates new "mirror at line" algo
	 * 
	 * @param cons
	 *            construction
	 * @param in
	 *            source geo
	 * @param g
	 *            mirror line
	 */
	public AlgoMirror(Construction cons, GeoElement in, GeoLineND g) {

		this(cons);
		mirrorLine = g;
		endOfConstruction(cons, in, (GeoElement) g);
	}

	/**
	 * used for 3D
	 * 
	 * @param cons
	 *            cons
	 */
	protected AlgoMirror(Construction cons) {
		super(cons);
	}

	/**
	 * end of construction
	 * 
	 * @param cons1
	 *            construction
	 * @param in
	 *            transformed geo
	 * @param setMirror
	 *            mirror
	 */
	public void endOfConstruction(Construction cons1, GeoElement in,
			GeoElement setMirror) {

		this.mirror = setMirror;

		inGeo = in;
		outGeo = getResultTemplate(inGeo);
		if (outGeo instanceof Mirrorable) {
			out = (Mirrorable) outGeo;
		}
		setInputOutput();

		transformedPoint = new GeoPoint(cons1);
		compute();
		if (inGeo.isGeoFunction()) {
			cons1.registerEuclidianViewCE(this);
		}
	}

	@Override
	public Commands getClassName() {
		return Commands.Mirror;
	}

	@Override
	public int getRelatedModeID() {
		if (mirror.isGeoLine()) {
			return EuclidianConstants.MODE_MIRROR_AT_LINE;
		} else if (mirror.isGeoPoint()) {
			return EuclidianConstants.MODE_MIRROR_AT_POINT;
		} else {
			return EuclidianConstants.MODE_MIRROR_AT_CIRCLE;
		}

	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		input = new GeoElement[2];
		input[0] = inGeo;
		input[1] = mirror;

		setOutputLength(1);
		setOutput(0, outGeo);
		setDependencies(); // done by AlgoElement
	}

	/**
	 * Returns the transformed geo
	 * 
	 * @return transformed geo
	 */
	@Override
	public GeoElement getResult() {
		return outGeo;
	}

	@Override
	public final void compute() {

		if (!mirror.isDefined()) {
			outGeo.setUndefined();
			return;
		}

		if (inGeo.isGeoList()) {
			transformList((GeoList) inGeo, (GeoList) outGeo);
			return;
		}

		setOutGeo();
		if (!outGeo.isDefined()) {
			return;
		}

		if (inGeo.isRegion() && mirror == mirrorConic) {
			GeoVec2D v = mirrorConic.getTranslationVector();
			outGeo.setInverseFill(
					((Region) inGeo).isInRegion(v.getX(), v.getY())
							^ inGeo.isInverseFill());
		}

		computeRegardingMirror();

		if (inGeo.isLimitedPath()) {
			this.transformLimitedPath(inGeo, outGeo);
		}
	}

	/**
	 * compute regarding which mirror type is used
	 */
	protected void computeRegardingMirror() {
		if (mirror == mirrorLine) {
			if (mirrorLine.getStartPoint() == null) {
				mirrorLine.setStandardStartPoint();
			}
			out.mirror(mirrorLine);
		} else if (mirror == mirrorPoint) {
			if (outGeo instanceof GeoFunction) {
				((GeoFunction) outGeo).mirror(getMirrorCoords());
			} else {
				out.mirror(getMirrorCoords());
			}
		} else {
			if (out instanceof ConicMirrorable) {
				((ConicMirrorable) out).mirror(mirrorConic);
			}
		}

	}

	/**
	 * set inGeo to outGeo
	 */
	@Override
	protected void setOutGeo() {
		if (mirror instanceof GeoConic && inGeo instanceof GeoLine) {
			((GeoLine) inGeo).toGeoConic((GeoConic) outGeo);
		}
		/*
		 * else if(mirror instanceof GeoConic && geoIn instanceof GeoConic &&
		 * geoOut instanceof GeoCurveCartesian){
		 * ((GeoConic)geoIn).toGeoCurveCartesian((GeoCurveCartesian)geoOut); }
		 */
		else if (mirror instanceof GeoConic && inGeo instanceof GeoConic
				&& outGeo instanceof GeoImplicit) {
			((GeoConic) inGeo).toGeoImplicitCurve((GeoImplicit) outGeo);
		} else if (inGeo instanceof GeoFunction && mirror != mirrorPoint) {
			((GeoFunction) inGeo)
					.toGeoCurveCartesian((GeoCurveCartesian) outGeo);
		} else if (inGeo instanceof GeoPoly && mirror == mirrorConic) {
			((GeoPoly) inGeo).toGeoCurveCartesian((GeoCurveCartesian) outGeo);
		} else {
			super.setOutGeo();
		}
	}

	/**
	 * 
	 * @return inhom coords for mirror point
	 */
	protected Coords getMirrorCoords() {
		return mirrorPoint.getInhomCoords();
	}

	@Override
	final public String toString(StringTemplate tpl) {
		// Michael Borcherds 2008-03-31
		// simplified to allow better translation
		return getLoc().getPlainDefault("AMirroredAtB", "%0 Mirrored at %1",
				inGeo.getLabel(tpl),
				mirror.getLabel(tpl));

	}

	@Override
	protected void setTransformedObject(GeoElement g, GeoElement g2) {
		inGeo = g;
		outGeo = g2;
		if (!(outGeo instanceof GeoList) && (outGeo instanceof Mirrorable)) {
			out = (Mirrorable) outGeo;
		}

	}

	@Override
	protected GeoElement getResultTemplate(GeoElement geo) {
		if ((geo instanceof GeoPoly) && mirror == mirrorConic) {
			return new GeoCurveCartesian(cons);
		}
		if ((geo instanceof GeoFunction) && mirror != mirrorPoint) {
			return new GeoCurveCartesian(cons);
		}
		if (geo.isLimitedPath() && mirror == mirrorConic) {
			return new GeoConicPart(cons, GeoConicNDConstants.CONIC_PART_ARC);
		}
		if (mirror instanceof GeoConic && geo instanceof GeoLine) {
			return new GeoConic(cons);
		}
		if (mirror instanceof GeoConic && geo instanceof GeoConic
				&& (!((GeoConic) geo).isCircle()
						|| !((GeoConic) geo).keepsType())) {
			return kernel.newImplicitPoly(cons).toGeoElement();
		}
		if (geo instanceof GeoPoly
				|| (geo.isLimitedPath() && mirror != mirrorConic)) {
			return copyInternal(cons, geo);
		}
		if (geo.isGeoList()) {
			return new GeoList(cons);
		}
		return copy(geo);
	}

	@Override
	protected void transformLimitedPath(GeoElement a, GeoElement b) {
		if (mirror != mirrorConic) {
			super.transformLimitedPath(a, b);
			return;
		}

		GeoConicPart arc = (GeoConicPart) b;
		arc.setParameters(0, 6.28, true);
		if (a instanceof GeoRay) {
			transformedPoint.removePath();
			setTransformedObject(((GeoRay) a).getStartPoint(),
					transformedPoint);
			compute();
			arc.pathChanged(transformedPoint);
			double d = transformedPoint.getPathParameter().getT();
			transformedPoint.removePath();
			transformedPoint.setCoords(mirrorConic.getTranslationVector());
			arc.pathChanged(transformedPoint);
			double e = transformedPoint.getPathParameter().getT();
			arc.setParameters(d * Kernel.PI_2, e * Kernel.PI_2, true);
			transformedPoint.removePath();
			setTransformedObject(arc.getPointParam(0.5), transformedPoint);
			compute();
			if (!((GeoRay) a).isOnPath(transformedPoint,
					Kernel.STANDARD_PRECISION)) {
				arc.setParameters(d * Kernel.PI_2, e * Kernel.PI_2, false);
			}

			setTransformedObject(a, b);
		} else if (a instanceof GeoSegment) {
			arc.setParameters(0, Kernel.PI_2, true);
			transformedPoint.removePath();
			setTransformedObject(((GeoSegment) a).getStartPoint(),
					transformedPoint);
			compute();
			if (arc.getType() == GeoConicNDConstants.CONIC_LINE) {
				arc.getLines()[0].setStartPoint(transformedPoint.copy());
			}
			// if start point itself is on path, transformed point may have
			// wrong path param #2306
			transformedPoint.removePath();
			arc.pathChanged(transformedPoint);
			double d = transformedPoint.getPathParameter().getT();

			arc.setParameters(0, Kernel.PI_2, true);
			transformedPoint.removePath();
			setTransformedObject(((GeoSegment) a).getEndPoint(),
					transformedPoint);
			compute();
			if (arc.getType() == GeoConicNDConstants.CONIC_LINE) {
				arc.getLines()[0].setEndPoint(transformedPoint.copy());
			}
			arc.pathChanged(transformedPoint);
			double e = transformedPoint.getPathParameter().getT();
			arc.setParameters(d * Kernel.PI_2, e * Kernel.PI_2, true);
			transformedPoint.removePath();
			transformedPoint.setCoords(mirrorConic.getTranslationVector());
			if (arc.isOnPath(transformedPoint, Kernel.STANDARD_PRECISION)) {
				arc.setParameters(d * Kernel.PI_2, e * Kernel.PI_2, false);
			}
			setTransformedObject(a, b);
		}
		if (a instanceof GeoConicPart) {
			transformLimitedConic(a, b);
		}
	}

	@Override
	public boolean swapOrientation(GeoConicPartND arc) {
		if (arc == null) {
			return true;
		} else if (mirror != mirrorConic || !(arc instanceof GeoConicPart)) {
			return arc.positiveOrientation();
		}
		GeoVec2D arcCentre = ((GeoConicPart) arc).getTranslationVector();
		GeoVec2D mirrorCentre = mirrorConic.getTranslationVector();
		double dist = MyMath.length(arcCentre.getX() - mirrorCentre.getX(),
				arcCentre.getY() - mirrorCentre.getY());
		return !DoubleUtil.isGreater(dist, ((GeoConicPart) arc).halfAxes[0]);
	}

	@Override
	public double getAreaScaleFactor() {
		return -1;
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) {
		if (mirrorBotana == null) {
			mirrorBotana = new MirrorAdapter();
		}
		return mirrorBotana.getBotanaVars();
	}

	@Override
	public String[] getBotanaVarsDescr(GeoElementND geo) throws NoSymbolicParametersException {
		return mirrorBotana.getBotanaVarsDescr();
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		if (mirrorBotana == null) {
			mirrorBotana = new MirrorAdapter();
		}
		return this.mirrorBotana.getBotanaPolynomials(geo, inGeo, mirrorLine,
				mirrorPoint, mirrorConic);
	}

	@Override
	public void reset() {
		if (mirrorBotana != null) {
			mirrorBotana.reset();
		}
	}

	@Override
	public SymbolicParameters getSymbolicParameters() {
		return new SymbolicParameters(this);
	}

	@Override
	public void getFreeVariables(HashSet<PVariable> variables)
			throws NoSymbolicParametersException {
		if (inGeo instanceof GeoPoint && mirror == mirrorPoint) {
			if (inGeo != null && mirrorPoint != null) {
				((GeoPoint) inGeo).getFreeVariables(variables);
				((GeoPoint) mirrorPoint).getFreeVariables(variables);
				return;
			}
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public int[] getDegrees(AbstractProverReciosMethod a)
			throws NoSymbolicParametersException {
		if (inGeo instanceof GeoPoint && mirror == mirrorPoint) {
			if (inGeo != null && mirrorPoint != null) {
				int[] degreeP = ((GeoPoint) inGeo).getDegrees(a);
				int[] degreeM = ((GeoPoint) mirrorPoint).getDegrees(a);
				int[] result = new int[3];
				result[0] = Math.max(degreeM[0] + degreeP[2],
						degreeM[2] + degreeP[0]);
				result[1] = Math.max(degreeM[1] + degreeP[2],
						degreeM[2] + degreeP[1]);
				result[2] = degreeM[2] + degreeP[2];
				return result;
			}
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public int getDegree(AbstractProverReciosMethod a) throws NoSymbolicParametersException {
		if (inGeo instanceof GeoPoint && mirror == mirrorPoint) {
			if (inGeo != null && mirrorPoint != null) {
				int degreeP = ((GeoPoint) inGeo).getDegree(a);
				int degreeM = ((GeoPoint) mirrorPoint).getDegree(a);
				int result = degreeP + degreeM;
				return result;
			}
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public BigInteger[] getExactCoordinates(
			TreeMap<PVariable, BigInteger> values)
			throws NoSymbolicParametersException {
		if (inGeo instanceof GeoPoint && mirror == mirrorPoint) {
			if (inGeo != null && mirrorPoint != null) {
				BigInteger[] pP = ((GeoPoint) inGeo).getExactCoordinates(values);
				BigInteger[] pM = ((GeoPoint) mirrorPoint).getExactCoordinates(values);
				BigInteger[] coords = new BigInteger[3];
				// We use the equations from AlgoMidpoint (the lazy way):
				// 1>> e1:=m0-p0*q2-q0*p2
				// 2>> e2:=m1-p1*q2-q1*p2
				// 3>> e3:=m2-2*p2*q2
				// 4>> l:=solve([e1,e2,e3],[q0,q1,q2])
				// list[[(2*m0*p2-m2*p0)/(2*p2^2),(2*m1*p2-m2*p1)/(2*p2^2),m2/(2*p2)]]
				// 5>> simplify(l*2*p2^2)
				// list[[2*m0*p2-m2*p0,2*m1*p2-m2*p1,m2*p2]]
				coords[0] = BigInteger.valueOf(2).multiply(pM[0]).multiply(pP[2]).subtract(pM[2].multiply(pP[0]));
				coords[1] = BigInteger.valueOf(2).multiply(pM[1]).multiply(pP[2]).subtract(pM[2].multiply(pP[1]));
				coords[2] = pM[2].multiply(pP[2]);
				return coords;
			}
		}
		throw new NoSymbolicParametersException();
	}

	@Override
	public PPolynomial[] getPolynomials() throws NoSymbolicParametersException {
		if (inGeo instanceof GeoPoint && mirror == mirrorPoint) {
			if (inGeo != null && mirrorPoint != null) {
				PPolynomial[] pP = ((GeoPoint) inGeo).getPolynomials();
				PPolynomial[] pM = ((GeoPoint) mirrorPoint).getPolynomials();
				polynomials = new PPolynomial[3];
				polynomials[0] = new PPolynomial(2).multiply(pM[0]).multiply(pP[2]).subtract(pM[2].multiply(pP[0]));
				polynomials[1] = new PPolynomial(2).multiply(pM[1]).multiply(pP[2]).subtract(pM[2].multiply(pP[1]));
				polynomials[2] = pM[2].multiply(pP[2]);
				return polynomials;
			}
		}
		throw new NoSymbolicParametersException();
	}
}
