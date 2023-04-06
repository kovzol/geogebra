package org.geogebra.common.kernel.prover;

import static org.geogebra.common.plugin.Operation.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.cas.realgeom.Compute;
import org.geogebra.common.cas.realgeom.RealGeomWebService;
import org.geogebra.common.cas.singularws.SingularWebService;
import org.geogebra.common.factories.UtilFactory;
import org.geogebra.common.javax.swing.RelationPane;
import org.geogebra.common.kernel.CASGenericInterface;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.advanced.AlgoDynamicCoordinates;
import org.geogebra.common.kernel.advanced.AlgoIncircleCenter;
import org.geogebra.common.kernel.algos.AlgoAngularBisectorLines;
import org.geogebra.common.kernel.algos.AlgoAngularBisectorPoints;
import org.geogebra.common.kernel.algos.AlgoCirclePointRadius;
import org.geogebra.common.kernel.algos.AlgoDependentBoolean;
import org.geogebra.common.kernel.algos.AlgoDependentNumber;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoEllipseHyperbolaFociPoint;
import org.geogebra.common.kernel.algos.AlgoFractionText;
import org.geogebra.common.kernel.algos.AlgoIntersectConics;
import org.geogebra.common.kernel.algos.AlgoIntersectLineConic;
import org.geogebra.common.kernel.algos.AlgoParabolaPointLine;
import org.geogebra.common.kernel.algos.AlgoPointInRegion;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.algos.AlgoPolygonRegular;
import org.geogebra.common.kernel.algos.SymbolicParametersBotanaAlgo;
import org.geogebra.common.kernel.algos.SymbolicParametersBotanaAlgoAre;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.MyList;
import org.geogebra.common.kernel.arithmetic.ValidExpression;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoConicPart;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoPolygon;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.matrix.Coords;
import org.geogebra.common.kernel.prover.adapters.DependentNumberAdapter;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.main.Localization;
import org.geogebra.common.main.ProverSettings;
import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.DoubleUtil;
import org.geogebra.common.util.ExtendedBoolean;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.Prover.NDGCondition;
import org.geogebra.common.util.Prover.ProofResult;
import org.geogebra.common.util.Prover.ProverEngine;
import org.geogebra.common.util.debug.Log;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.himamis.retex.editor.share.util.Unicode;

import jdk.nashorn.internal.runtime.Debug;

/**
 * A prover which uses Francisco Botana's method to prove geometric theorems.
 * 
 * @author Zoltan Kovacs
 * @author Csilla Solyom-Gecse
 *
 */

public class ProverBotanasMethod {

	private static Map<List<PVariable>, GeoElement> botanaVarsInv;

	/**
	 * Inverse mapping of botanaVars for a given statement.
	 *
	 * @param statement the input statement
	 * @throws NoSymbolicParametersException if implementation is missing
	 */
	static void updateBotanaVarsInv(GeoElement statement)
			throws NoSymbolicParametersException {
		if (botanaVarsInv == null) {
			botanaVarsInv = new HashMap<>();
		}
		for (GeoElement geo : statement.getAllPredecessors()) {
			if (!(geo instanceof GeoNumeric)) {
				PVariable[] vars = ((SymbolicParametersBotanaAlgo) geo)
						.getBotanaVars(geo);
				if (vars != null) {
					List<PVariable> varsList = Arrays.asList(vars);
					botanaVarsInv.put(varsList, geo);
				}
			}
		}
	}

	/**
	 * Compute free points in a statement.
	 *
	 * @param statement the input statement
	 * @return list of free points
	 */
	public static List<GeoElement> getFreePoints(GeoElement statement) {
		List<GeoElement> freePoints = new ArrayList<>();
		for (GeoElement geo : statement.getAllPredecessors()) {
			if (geo.isGeoPoint() && geo.getParentAlgorithm() == null) {
				/* this is a free point */
				freePoints.add(geo);
			}
		}
		return freePoints;
	}

	/**
	 * Compute predecessor free points of an element, used for locus/envelope.
	 * Works similarly like getFreePoints(), but geos whose parent is
	 * AlgoDynamicCoordinates will be used as free points and their predecessors
	 * can be ignored.
	 *
	 * @param geo input geo
	 * @return list of free points
	 */
	private static TreeSet<GeoElement> getLocusFreePoints(GeoElement geo) {
		TreeSet<GeoElement> freePoints = new TreeSet<>();
		AlgoElement algo = geo.getParentAlgorithm();
		if (algo != null) {
			for (GeoElement g : algo.getInput()) {
				AlgoElement a = g.getParentAlgorithm();
				if (g.isGeoPoint() && a == null) {
					/* this is a free point */
					freePoints.add(g);
				} else if (g.isGeoPoint()
						&& a instanceof AlgoDynamicCoordinates) {
					/* this will be considered as a free point */
					freePoints.add(g);
				} else {
					/* find recursively the parents */
					freePoints.addAll(getLocusFreePoints(g));
				}
			}
		}
		return freePoints;
	}

	/**
	 * Creates those polynomials which describe that none of 3 free points can
	 * lie on the same line.
	 *
	 * @param prover the underlying prover
	 * @return the NDG polynomials (in denial form)
	 * @throws NoSymbolicParametersException if implementation is missing
	 */
	static PPolynomial[] create3FreePointsNeverCollinearNDG(
			Prover prover) throws NoSymbolicParametersException {
		/* Creating the set of free points first: */
		List<GeoElement> freePoints = getFreePoints(prover.getStatement());
		int setSize = freePoints.size();
		if (setSize < 3) {
			return new PPolynomial[0];
		}

		/* Creating NDGs: */
		NDGCondition ndgc = new NDGCondition();
		if (setSize > 3) {
			ndgc.setCondition("DegeneratePolygon");
		} else {
			ndgc.setCondition("AreCollinear");
		}
		GeoElement[] geos = new GeoElement[setSize];
		int i = 0;
		for (GeoElement freePoint : freePoints) {
			geos[i++] = freePoint;
		}
		ndgc.setGeos(geos);
		Arrays.sort(ndgc.getGeos());
		prover.addNDGcondition(ndgc);

		/* The output will contain $\binom{n}{3}$ elements: */
		PPolynomial[] ret = new PPolynomial[setSize * (setSize - 1)
				* (setSize - 2) / 6];
		i = 0;
		/* Creating the set of triplets: */
		HashSet<HashSet<GeoElement>> triplets = new HashSet<>();
		for (GeoElement geo1 : freePoints) {
			for (GeoElement geo2 : freePoints) {
				if (!isEqual(geo1, geo2)) {
					for (GeoElement geo3 : freePoints) {
						if (!isEqual(geo1, geo3) && !isEqual(geo2, geo3)) {
							HashSet<GeoElement> triplet = new HashSet<>();
							triplet.add(geo1);
							triplet.add(geo2);
							triplet.add(geo3);
							/*
							 * Only the significantly new triplets will be
							 * processed:
							 */
							if (!triplets.contains(triplet)) {
								triplets.add(triplet);
								PVariable[] fv1 = ((SymbolicParametersBotanaAlgo) geo1)
										.getBotanaVars(geo1);
								PVariable[] fv2 = ((SymbolicParametersBotanaAlgo) geo2)
										.getBotanaVars(geo2);
								PVariable[] fv3 = ((SymbolicParametersBotanaAlgo) geo3)
										.getBotanaVars(geo3);
								/* Creating the polynomial for collinearity: */
								PPolynomial p = PPolynomial.collinear(fv1[0],
										fv1[1], fv2[0], fv2[1], fv3[0], fv3[1]);
								Log.info("Forcing non-collinearity for points "
										+ geo1 + ":" + geo1.hashCode() + ", "
										+ geo2 + ":" + geo2.hashCode() + " and "
										+ geo3);
								/*
								 * Rabinowitsch trick for prohibiting
								 * collinearity:
								 */
								ret[i] = p
										.multiply(
												new PPolynomial(new PVariable(
														geo1.getKernel())))
										.subtract(
												new PPolynomial(BigInteger.ONE));
								/*
								 * FIXME: this always introduces an extra
								 * variable, shouldn't do
								 */
								i++;
							}
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Comparisan that gives true when comparing undefined object with self
	 *
	 * @param geo1 comparison argument
	 * @param geo3 comparison argument
	 * @return whether objects have equal value or are identical
	 */
	private static boolean isEqual(GeoElement geo1, GeoElement geo3) {
		return geo1 == geo3 || geo1.isEqual(geo3);
	}

	/**
	 * Uses a minimal heuristics to fix the first four variables to certain
	 * "easy" numbers. The first two variables (usually the coordinates of the
	 * first point) are set to 0, and the second two variables (usually the
	 * coordinates of the second point) are set to 0 and 1.
	 *
	 * @param prover the input prover
	 * @param coords number of fixed coordinates
	 * @return a TreeMap, containing the substitutions
	 * @throws NoSymbolicParametersException
	 */
	private static TreeMap<PVariable, BigInteger> fixValues(Prover prover,
															int coords) throws NoSymbolicParametersException {

		BigInteger[] fixCoords = {BigInteger.ZERO, BigInteger.ZERO,
				BigInteger.ZERO, BigInteger.ONE};

		GeoElement statement = prover.getStatement();
		List<GeoElement> freePoints = getFreePoints(statement);
		List<GeoElement> fixedPoints = new ArrayList<>();
		/* Adding free points: */
		for (GeoElement ge : freePoints) {
			fixedPoints.add(ge);
		}

		TreeMap<PVariable, BigInteger> ret = new TreeMap<>();

		Iterator<GeoElement> it = fixedPoints.iterator();
		GeoElement[] geos = new GeoElement[2];
		int i = 0, j = 0;
		while (it.hasNext() && i < 2 && j < coords) {
			GeoElement geo = it.next();
			PVariable[] fv = ((SymbolicParametersBotanaAlgo) geo)
					.getBotanaVars(geo);
			geos[i] = geo;
			ret.put(fv[0], fixCoords[j]);
			++j;
			if (j < coords) {
				ret.put(fv[1], fixCoords[j]);
				++i;
				++j;
			}
		}

		/* We implicitly assumed that the first two points are different: */
		if (i == 2 && prover.isReturnExtraNDGs()) {
			NDGCondition ndgc = new NDGCondition();
			ndgc.setCondition("AreEqual");
			ndgc.setGeos(geos);
			Arrays.sort(ndgc.getGeos());
			prover.addNDGcondition(ndgc);
		}
		return ret;
	}

	/**
	 * Translation of a geometric statement into an algebraic one. We use
	 * polynomials and integer coefficients. The computations assume that a
	 * complex algebraic geometry approach will be used based on the Groebner
	 * basis method (or Wu's characteristic method, but that's not yet
	 * implemented).
	 */
	public static class AlgebraicStatement {
		/**
		 * The statement in geometric form, e.g. AreCollinear[D,E,F].
		 */
		public GeoElement geoStatement;
		/**
		 * The prover which uses this class.
		 */
		Prover geoProver;
		/**
		 * The set of polynomials which are the translations of the geometric
		 * hypotheses and the thesis. The thesis is stored reductio ad absurdum.
		 */
		private Set<PPolynomial> polynomials;
		/**
		 * The set of free variables. By default each variable is non-free, thus
		 * this list is empty. Some algos and geo types should create an entry
		 * for some of the Botana variables.
		 */
		Set<PVariable> freeVariables = new TreeSet<>();
		/**
		 * The set of "almost free" variables. They are free in most cases, but we
		 * should avoid the assumption that they are free. So, if other variables
		 * can be chosen for substitution, then they are preferred. E.g. a coordinate
		 * of a point on a path is almost free, because in most cases we can
		 * set one of the coordinates freely. But in some degenerate cases, it is not so,
		 * e.g. for vertical or horizontal lines.
		 */
		Set<PVariable> almostFreeVariables = new TreeSet<>();
		/**
		 * Should the "false" result be interpreted as undefined?
		 */
		boolean interpretFalseAsUndefined = false;
		/**
		 * Should the "true" result be interpreted as undefined?
		 */
		boolean interpretTrueAsUndefined = false;

		private boolean disallowFixSecondPoint = false;

		private String polys, elimVars, freeVars, freeVarsWithAlmostFree, freeVarsWithoutAlmostFree, elimVarsWithAlmostFree;
		private String rgVars; // all variables that are required for real geometry
		private PPolynomial[] thesisFactors;
		private TreeMap<GeoElement, PPolynomial[]> geoPolys = new TreeMap<>();
		// ineqs cannot be eliminated:
		public Set<String> ineqs = new TreeSet<>(); // TODO: Write getter/setter instead.
		private boolean dryRun = false; // If set, do not compute any heavy detail.
		private Set<String> extVars = new TreeSet<>(); // external variables (may be substitued)
		private Set<String> extPolys = new TreeSet<>(); // external polynomials (may be eliminated)
		private Set<String> posVars = new TreeSet<>(); // positive variables (RG)
		// Positive variables should not be eliminated via GB, so we maintain a list of them.
		// Such variables are: sqrt2, a, b, c (lengths of sides).
		private String thesisIneq = null;

		/**
		 * Number of maximal fix coordinates. -1 if no limit. Sometimes we need
		 * to limit the maximum if the construction contains constrained point
		 * on a path.
		 */
		int maxFixcoords = -1;

		/**
		 * A map of substitutions, used only in locus equations and envelopes.
		 */
		public TreeMap<PVariable, BigInteger> substitutions;
		/**
		 * The variables for x and y, used only in locus equations and
		 * envelopes.
		 */
		public PVariable[] curveVars = new PVariable[2];

		/**
		 * The result of the proof (even if no computation was done). Sometimes
		 * it can be predicted without any further computations.
		 */
		ProofResult result = null;

		/**
		 * Return the polynomials of the algebraic structure as a String. Use
		 * computeStrings() before using this method.
		 *
		 * @return polynomials in String format
		 */
		public String getPolys() {
			return polys;
		}

		/**
		 * Retrieve polynomial belonging to a given GeoElement.
		 *
		 * @param geo input geometric object
		 * @return algebraic representation of the geometric object
		 */
		public PPolynomial[] getGeoPolys(GeoElement geo) {
			return geoPolys.get(geo);
		}

		public ArrayList<String> getExtPolys() {
			ArrayList<String> al = new ArrayList<>();
			for (String p : extPolys) {
				al.add(p);
			}
			return al;
		}

		/**
		 * Retrieve free variables.
		 *
		 * @return the set of free variables
		 */
		public Set<PVariable> getFreeVariables() {
			return freeVariables;
		}

		/**
		 * Retrieve "almost" free variables.
		 *
		 * @return the set of "almost" free variables
		 */
		public Set<PVariable> getAlmostFreeVariables() {
			return almostFreeVariables;
		}

		/**
		 * Add algebraic representation of a geometric object to the polynomial
		 * system. It may contain one or more polynomials.
		 *
		 * @param geo geometric object
		 * @param ps  algebraic representation of the geometric object
		 */
		public void addGeoPolys(GeoElement geo, PPolynomial[] ps) {
			geoPolys.put(geo, ps);
			for (PPolynomial p : ps) {
				if (addPolynomial(p)) {
					if (ProverSettings.get().captionAlgebra) {
						geo.addCaptionBotanaPolynomial(p.toTeX());
					}
				}
			}
		}

		/**
		 * Remove all polynomials from the system which describe constraints of
		 * the given input geometric object
		 *
		 * @param geo geometric object
		 */
		public void removeGeoPolys(GeoElement geo) {
			if (geoPolys.isEmpty()) {
				return; // do nothing
			}
			PPolynomial[] ps = geoPolys.get(geo);
			if (ps == null) {
				return;
			}
			for (PPolynomial p : ps) {
				removePolynomial(p);
			}
			geoPolys.remove(geo);
		}

		/**
		 * Return the elimination variables of the algebraic structure as a
		 * String. Use computeStrings() before using this method.
		 *
		 * @return elimination variables in String format
		 */
		public String getElimVars() {
			return elimVars;
		}

		/**
		 * Return the elimination variables plus the almost free variables
		 * of the algebraic structure as a String.
		 * Use computeStrings() before using this method.
		 *
		 * @return elimination variables in String format
		 */
		public String getElimVarsWithAlmostFree() {
			return elimVarsWithAlmostFree;
		}

		/**
		 * Return the free variables of the algebraic structure as a String. Use
		 * computeStrings() before using this method.
		 *
		 * @return free variables in String format
		 */
		public String getFreeVars() {
			return freeVars;
		}

		/**
		 * Return the free variables of the algebraic structure as a String. Use
		 * computeStrings() before using this method. This method does not return
		 * those variables that are almost free.
		 *
		 * @return free variables in String format
		 */
		public String getFreeVarsWithoutAlmostFree() {
			return freeVarsWithoutAlmostFree;
		}

		public String getFreeVarsWithAlmostFree() {
			return freeVarsWithAlmostFree;
		}

		/**
		 * Return all variables for the RG subsystem. Use
		 * computeStrings() before using this method.
		 *
		 * @return free variables in String format
		 */
		public String getRGVars() {
			return rgVars;
		}

		/**
		 * @return the polynomials
		 */
		public Set<PPolynomial> getPolynomials() {
			return polynomials;
		}

		/**
		 * Remove the thesis (eventually the negated one if it is already
		 * negated).
		 */
		public void removeThesis() {
			removeGeoPolys(geoStatement);
		}

		/**
		 * Add the negated thesis. Note that this can be called only once and
		 * cannot be reverted. (?)
		 */
		public void addNegatedThesis() {
			addGeoPolys(geoStatement, thesisFactors);
		}

		/**
		 * Add a polynomial to the system manually.
		 *
		 * @param p the polynomial to be added
		 * @return if the polynomial was added (true) or ignored (false)
		 */
		public boolean addPolynomial(PPolynomial p) {
			if (polynomials.contains(p)) {
				Log.debug("Ignoring existing poly " + p);
				return false;
			}
			polynomials.add(p);
			int size = polynomials.size();
			Log.debug("Adding poly #" + (size) + ": " + p.toTeX());
			return true;
		}

		public boolean addIneq(String ie) {
			if (ineqs.contains(ie)) {
				Log.debug("Ignoring existing ineq " + ie);
				return false;
			}
			ineqs.add(ie);
			int size = ineqs.size();
			Log.debug("Adding ineq #" + (size) + ": " + ie);
			return true;
		}

		public boolean addPosVar(String v) {
			if (posVars.contains(v)) {
				Log.debug("Ignoring existing posVar " + v);
				return false;
			}
			posVars.add(v);
			int size = posVars.size();
			Log.debug("Adding posVar #" + (size) + ": " + v);
			return true;
		}

		public boolean addExtVar(String v) {
			if (extVars.contains(v)) {
				Log.debug("Ignoring existing extVar " + v);
				return false;
			}
			extVars.add(v);
			int size = extVars.size();
			Log.debug("Adding extVar #" + (size) + ": " + v);
			return true;
		}

		public boolean addExtPoly(String p) {
			if (extPolys.contains(p)) {
				Log.debug("Ignoring existing external poly " + p);
				return false;
			}
			extPolys.add(p);
			int size = extPolys.size();
			Log.debug("Adding external poly #" + (size) + ": " + p);
			return true;
		}

		/**
		 * Remove a polynomial from the system manually.
		 *
		 * @param p the polynomial to be removed
		 */
		public void removePolynomial(PPolynomial p) {
			polynomials.remove(p);
		}

		/**
		 * The result of the computation.
		 *
		 * @return result
		 */
		public ProofResult getResult() {
			return result;
		}

		/**
		 * Create an algebraic equation system of the statement given in the
		 * construction, by using the underlying prover settings.
		 *
		 * @param statement   the statement to be proven
		 * @param movingPoint use numerical approximation for this element instead of
		 *                    computing it symbolically. This can be useful if an
		 *                    element cannot be precisely described symbolically: only
		 *                    its generalized formula can be described symbolically.
		 * @param prover      the underlying prover
		 */

		public AlgebraicStatement(GeoElement statement, GeoElement movingPoint,
								  Prover prover, boolean dry) {
			dryRun = dry;
			CASGenericInterface c = statement.kernel.getGeoGebraCAS().getCurrentCAS();
			if (c.isLoaded()) {
				Log.debug("GeoGebra thinks Giac is loaded.");
				if (c.evaluateCAS("1+1").equals("2")) {
					algebraicTranslation(statement, movingPoint, prover);
					return;
				}
				Log.debug("But 1+1=2 seems to be problematic.");
				result = ProofResult.PROCESSING;
			} else {
				Log.debug("GeoGebra thinks Giac is not loaded yet.");
				result = ProofResult.PROCESSING;
			}
		}

		/**
		 * Convert Java datatypes into String datatypes.
		 */
		public void computeStrings() {
			TreeSet<PVariable> dependentVariables = new TreeSet<>();
			TreeSet<PVariable> dependentVariablesWithAlmostFree = new TreeSet<>();
			TreeSet<PVariable> freeVariablesWithAlmostFree = new TreeSet<>();
			TreeSet<PVariable> freeVariablesWithoutAlmostFree = new TreeSet<>();
			freeVariablesWithAlmostFree.addAll(freeVariables);

			PPolynomial[] eqSystem = this.getPolynomials()
					.toArray(new PPolynomial[this.getPolynomials().size()]);
			TreeSet<PVariable> variables = new TreeSet<>();
			variables.addAll(freeVariables);
			variables.addAll(PPolynomial.getVars(eqSystem));

			Iterator<PVariable> variablesIterator = variables.iterator();
			while (variablesIterator.hasNext()) {
				PVariable variable = variablesIterator.next();
				if (!freeVariables.contains(variable)) {
					dependentVariablesWithAlmostFree.add(variable);
					dependentVariables.add(variable);
				}
				if (almostFreeVariables.contains(variable)) {
					dependentVariablesWithAlmostFree.add(variable);
				}
				if (freeVariables.contains(variable) && !almostFreeVariables.contains(variable)) {
					freeVariablesWithoutAlmostFree.add(variable);
				}
				if (freeVariables.contains(variable) && almostFreeVariables.contains(variable)) {
					freeVariablesWithAlmostFree.add(variable);
				}
			}

			PPolynomial[] eqSystemSubstituted;
			if (substitutions != null) {
				eqSystemSubstituted = new PPolynomial[eqSystem.length];
				for (int i = 0; i < eqSystem.length; i++) {
					eqSystemSubstituted[i] = eqSystem[i]
							.substitute(substitutions);
				}
				variables.removeAll(substitutions.keySet());
			} else {
				eqSystemSubstituted = eqSystem;
			}

			Log.debug(
					"Eliminating system in " + variables.size() + " variables ("
							+ dependentVariables.size() + " dependent)");

			this.polys = PPolynomial
					.getPolysAsCommaSeparatedString(eqSystemSubstituted);
			this.elimVars = PPolynomial.getVarsAsCommaSeparatedString(
					eqSystemSubstituted, null, false, freeVariables);
			this.freeVars = PPolynomial.getVarsAsCommaSeparatedString(
					eqSystemSubstituted, null, true, freeVariables);
			this.elimVarsWithAlmostFree = PPolynomial.getVarsAsCommaSeparatedString(
					eqSystemSubstituted, null, true, dependentVariablesWithAlmostFree);
			// this.freeVarsWithoutAlmostFree = PPolynomial.getVarsAsCommaSeparatedString(
			// 		eqSystemSubstituted, null, false, dependentVariablesWithAlmostFree);
			this.freeVarsWithoutAlmostFree = PPolynomial.getVarsAsCommaSeparatedString(
					eqSystemSubstituted, null, true, freeVariablesWithoutAlmostFree);
			this.freeVarsWithAlmostFree = PPolynomial.getVarsAsCommaSeparatedString(
					eqSystemSubstituted, null, true, freeVariablesWithAlmostFree);

			/* Construct the needed variables for the real algebraic geometry subsystem. */
			this.rgVars = freeVarsWithoutAlmostFree;
			if (!this.rgVars.equals("")) {
				if (!elimVars.equals("")) {
					this.rgVars += "," + elimVarsWithAlmostFree;
				}
				if (!this.extVars.isEmpty()) {
					for (String v : extVars) {
						this.rgVars += "," + v;
					}
				}
			}

		}

		/*
		 * Do a breadth first search from the statement (s)
		 * back towards its predecessors. Do not continue
		 * traversing when the numerical object (n) is reached.
		 * The visited objects will be kept. This does not
		 * include the numerical object.
		 */
		private TreeSet<GeoElement> keptElements (GeoElement n, GeoElement s) {
			TreeSet<GeoElement> keptElements = new TreeSet<>();
			TreeSet<GeoElement> toProcess = new TreeSet<>();
			toProcess.add(s);

			while (!toProcess.isEmpty()) {
				keptElements.addAll(toProcess);
				Iterator<GeoElement> it = toProcess.iterator();
				TreeSet<GeoElement> toFurtherProcess = new TreeSet<>();
				while (it.hasNext()) {
					GeoElement processed = it.next();
					AlgoElement aeProcessed = processed.getParentAlgorithm();
					if (aeProcessed != null) {
						GeoElement[] processedInputs = aeProcessed.getInput();
						for (GeoElement input : processedInputs) {
							if (!input.equals(n)) {
								toFurtherProcess.add(input);
							}
						}
					}
				}
				toProcess = toFurtherProcess;
			}
			return keptElements;
		}

		private PPolynomial tripletSign (GeoPoint p1, GeoPoint p2, GeoPoint p3) {
			try {
				PVariable[] geoVariables1 = ((SymbolicParametersBotanaAlgo) p1)
						.getBotanaVars(p1);
				PVariable[] geoVariables2 = ((SymbolicParametersBotanaAlgo) p2)
						.getBotanaVars(p2);
				PVariable[] geoVariables3 = ((SymbolicParametersBotanaAlgo) p3)
						.getBotanaVars(p3);
				PPolynomial p = tripletSign(geoVariables1, geoVariables2, geoVariables3);
				return p;
			} catch (NoSymbolicParametersException e) {
				return null;
			}
		}

		private PPolynomial tripletSign (GeoPoint p1, GeoPoint p2, PVariable[] p3) {
			try {
				PVariable[] geoVariables1 = ((SymbolicParametersBotanaAlgo) p1)
						.getBotanaVars(p1);
				PVariable[] geoVariables2 = ((SymbolicParametersBotanaAlgo) p2)
						.getBotanaVars(p2);
				PPolynomial p = tripletSign(geoVariables1, geoVariables2, p3);
				return p;
			} catch (NoSymbolicParametersException e) {
				return null;
			}
		}

		private PPolynomial tripletSign (PVariable[] p1, PVariable[] p2, PVariable[] p3) {
			// Idea taken from https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
			// (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
			PPolynomial p = (((new PPolynomial(p1[0]).subtract(new PPolynomial(p3[0])))
						.multiply(new PPolynomial(p2[1]).subtract(new PPolynomial(p3[1])))))
						.subtract((new PPolynomial(p2[0]).subtract(new PPolynomial(p3[0])))
								.multiply(new PPolynomial(p1[1]).subtract(new PPolynomial(p3[1]))));
			return p;
		}


		private PPolynomial tripletSignRotated (GeoPoint p1, GeoPoint p2, GeoPoint p3) {
			// Instead of using p1, we use its rotation by 90 about p2.

			// Idea taken from https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
			// (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
			try {
				PVariable[] geoVariables1 = ((SymbolicParametersBotanaAlgo) p1)
						.getBotanaVars(p1);
				PVariable[] geoVariables2 = ((SymbolicParametersBotanaAlgo) p2)
						.getBotanaVars(p2);
				PVariable[] geoVariables3 = ((SymbolicParametersBotanaAlgo) p3)
						.getBotanaVars(p3);
				PPolynomial[] rotated = new PPolynomial[2];
				rotated[0] = (new PPolynomial(geoVariables2[0]).add(new PPolynomial(geoVariables1[1]))).
						subtract(new PPolynomial(geoVariables2[1]));
				rotated[1] = (new PPolynomial(geoVariables2[1]).add(new PPolynomial(geoVariables2[0]))).
						subtract(new PPolynomial(geoVariables1[0]));
				PPolynomial p = ((rotated[0].subtract(new PPolynomial(geoVariables3[0])))
						.multiply(new PPolynomial(geoVariables2[1]).subtract(new PPolynomial(geoVariables3[1]))))
						.subtract((new PPolynomial(geoVariables2[0]).subtract(new PPolynomial(geoVariables3[0])))
								.multiply(rotated[1].subtract(new PPolynomial(geoVariables3[1]))));
				return p;
			} catch (NoSymbolicParametersException e) {
				return null;
			}
		}

		private void setHypotheses(GeoElement movingPoint) {
			polynomials = new TreeSet<>();

			TreeSet<GeoElement> predecessors = new TreeSet<>();
			TreeSet<GeoElement> allPredecessors = geoStatement
					.getAllPredecessors();
			if (geoProver.getProverEngine() == ProverEngine.LOCUS_EXPLICIT) {
				allPredecessors.add(geoStatement);
			}

			Iterator<GeoElement> it;
			/*
			 * The algo of the moving point will be computed numerically after
			 * the end of the symbolic computations. This is not ideal since
			 * some symbolic algos e.g. AlgoIntersectLineConic want to use
			 * existing equations and those will be symbolic in all cases, that
			 * is, it would be better to compute all numerical equations first
			 * or (even better) during the symbolic process.
			 *
			 * We don't use numerical formula for implicit locus to avoid
			 * contradiction.
			 */
			GeoElement numerical = null;
			AlgoElement numAlgo;
			if (movingPoint != null
					&& (numAlgo = movingPoint.getParentAlgorithm()) != null
					&& (geoProver
					.getProverEngine() != ProverEngine.LOCUS_IMPLICIT)) {
				numerical = (GeoElement) numAlgo.getInput(0);

				/*
				 * Formerly we did not want to use the numerical formula for
				 * linear objects. Now we still compute the numerical formula
				 * for most of the cases to avoid contradictions between
				 * approximated values and exact symbolic values.
				 */
				if (numerical instanceof GeoSegment
						|| numerical instanceof GeoConicPart) {
					// we don't want the equation of the length
					numerical = null;
				}
			}

			/*
			 * Remove geos directly related with AlgoDependentNumber algos since
			 * we don't want to add them twice (they will be invoked during
			 * their occurrence on a higher level in their geos). Hopefully this
			 * is OK in general, that is, we never need using
			 * AlgoDependentNumber's polynomials directly. If this is still the
			 * case, our idea here must be redesigned. Also remove geos which
			 * should be computed numerically. Also remove numbers which have no
			 * role, e.g. in Intersect(c,d,2).
			 */
			it = allPredecessors.iterator();
			while (it.hasNext()) {
				GeoElement geo = it.next();
				AlgoElement algo = geo.getParentAlgorithm();
				if (!(geo instanceof GeoNumeric
						&& (algo instanceof AlgoDependentNumber
						|| algo == null))) {
					predecessors.add(geo);
				}

				/*
				 * No object may be numerical if it is later referenced
				 * in another object by a non-point-on-path way.
				 */
				if (algo != null) {
					GeoElement[] inputs = algo.getInput();
					for (GeoElement input : inputs) {
						if (numerical != null &&
								numerical.equals(input) && !(algo instanceof AlgoPointOnPath)) {
							// Forbid numerical computation in this case:
							numerical = null;
						}
					}
				}
			}

			/*
			 * All such predecessors of the statement object that are predecessors
			 * of only the numerical object can be safely ignored.
			 */
			if (numerical != null) {
				predecessors.retainAll(keptElements(numerical, geoStatement));
			}

			/*
			// Re-add axes and points on the axes:
			it = allPredecessors.iterator();
			while (it.hasNext()) {
				GeoElement geo = it.next();
				// add axis
				if (geo instanceof GeoLine && ((GeoLine) geo).hasFixedSlope()) {
					predecessors.add(geo);
				} else {
					// add point on axis
					AlgoElement algo = geo.getParentAlgorithm();
					if (algo instanceof AlgoPointOnPath) {
						Path path = ((AlgoPointOnPath) algo).getPath();
						if (path instanceof GeoAxis) {
							predecessors.add(geo);
						}
					}
				}
			}
			*/

			ProverSettings proverSettings = ProverSettings.get();
			it = predecessors.iterator();
			while (it.hasNext()) {
				GeoElement geo = it.next();
				if (geo.equals(numerical)) {
					Log.debug("Using " + geo + " as a numerical object, not considering its symbolic counterpart");
				} else if (geo instanceof SymbolicParametersBotanaAlgo) {
					try {
						if (geo instanceof GeoLine
								&& ((GeoLine) geo).hasFixedSlope()
								&& !(geoProver
								.getProverEngine() == ProverEngine.LOCUS_EXPLICIT
								|| geoProver
								.getProverEngine() == ProverEngine.LOCUS_IMPLICIT)) {
							Log.info(
									"Statements containing axes or fixed slope lines are unsupported");
							result = ProofResult.UNKNOWN;
							return;
						}
						if (proverSettings.captionAlgebra) {
							geo.setCaption(null);
						}
						String command = geo
								.getDefinition(StringTemplate.noLocalDefault);
						if (!("".equals(command))) {
							Log.debug(geo.getLabelSimple() + " = "
									+ geo.getDefinition(
									StringTemplate.noLocalDefault)
									+ " /* "
									+ geo.getDefinitionDescription(
									StringTemplate.noLocalDefault)
									+ " */");
						} else {
							String description = geo
									.getAlgebraDescriptionDefault();
							if ((geo instanceof GeoLine
									&& ((GeoLine) geo).hasFixedSlope())
									|| (geo instanceof GeoNumeric)) {
								Log.debug(description);
							} else if (!description.startsWith("xOyPlane")) {
								/*
								 * handling GeoGebra3D's definition for xy-plane
								 */
								Log.debug(description + " /* free point */");
								PVariable[] v;
								v = ((SymbolicParametersBotanaAlgo) geo)
										.getBotanaVars(geo);
								if (proverSettings.captionAlgebra) {
									geo.setCaptionBotanaVars("(" + v[0].toTeX()
											+ "," + v[1].toTeX() + ")", "yellow");
								}
								if (v != null) {
									Log.debug("// Free point "
											+ geo.getLabelSimple() + "(" + v[0]
											+ "," + v[1] + ")");
								}
							}
						}
						PPolynomial[] geoPolynomials = ((SymbolicParametersBotanaAlgo) geo)
								.getBotanaPolynomials(geo);

						AlgoElement algo = geo.getParentAlgorithm();
						/*
						 * We used to check if the construction step could be
						 * reliably translated to an algebraic representation.
						 * This was the case for linear constructions (parallel,
						 * perpendicular etc.) but not for quadratic ones
						 * (intersection of conics etc.). In the latter case the
						 * equation system might have been solvable even if
						 * geometrically, "seemingly" the statement was true. To
						 * avoid such confusing cases, it was better to report
						 * undefined instead of false.
						 *
						 * Now we check the statement's negation as well and it
						 * is enough to handle those cases.
						 *
						 * TODO: This piece of code can be removed on a cleanup.
						 */
						if (algo instanceof AlgoAngularBisectorPoints
								|| algo instanceof AlgoEllipseHyperbolaFociPoint
								|| (algo instanceof AlgoIntersectConics
								&& ((AlgoIntersectConics) algo)
								.existingIntersections() != 1)
								|| (algo instanceof AlgoIntersectLineConic
								&& ((AlgoIntersectLineConic) algo)
								.existingIntersections() != 1)) {
							// interpretFalseAsUndefined = true;
							Log.info(algo
									+ " is not 1-1 algebraic mapping, but FALSE will not be interpreted as UNKNOWN");
						}
						/*
						 * End of reliability check.
						 */

						/*
						 * Declare free variables. Now this is done here, not in
						 * the getBotanaVars() and getBotanaPolynomials()
						 * methods in order to allow different sets of free
						 * variables for the same objects which may appear in
						 * different ART commands.
						 */
						PVariable[] geoVariables = ((SymbolicParametersBotanaAlgo) geo)
								.getBotanaVars(geo);
						if (geoVariables != null) {
							if (algo instanceof AlgoPointOnPath
									|| geo instanceof GeoNumeric) {
								freeVariables.add(geoVariables[0]);
								almostFreeVariables.add(geoVariables[0]);
							} else if (algo instanceof AlgoDynamicCoordinates
									|| (geo instanceof GeoLine
									&& ((GeoLine) geo).hasFixedSlope())
									|| (geo instanceof GeoPoint
									&& algo == null)) {
								for (PVariable geoVariable : geoVariables) {
									freeVariables.add(geoVariable);
									Log.debug(geoVariable + " is free");
								}
							}
						}

						if (algo instanceof AlgoCirclePointRadius) {
							disallowFixSecondPoint = true;
						}

						/*
						 * Consider the following case: Let AB a segment and C a
						 * point on it. Move C to A. Now let's check if
						 * Prove[A==C] returns false. Since C is on a line and
						 * normally A=(0,0) and B=(0,1), thus x(C)=0 follows.
						 * But we set x(C) to be a free variable in the
						 * AlgoPointOnPath equation and y(C) to be dependent
						 * which is a bad idea for Cox's method: this scenario
						 * cannot be constructed (the converse scenario: x(C) is
						 * dependent and y(C) is free would be fine), so Cox's
						 * method will return true (because a non-constructible
						 * setting is always contradictory)---even if the
						 * statement is false. So we avoid setting B=(0,1) for
						 * Cox's method when there is a point on a path,
						 * otherwise we will get true for a false statement! See
						 * Example 52 in Zoltan's diss on page 176---here we
						 * need to generalize B to avoid getting true. This will
						 * slow down some things, but that's the price for the
						 * correct behavior. Note that non-linear paths are not
						 * affected.
						 */
						if (algo instanceof AlgoPointOnPath
								&& algo.input[0] instanceof GeoLine) {
							maxFixcoords = 2;
						}

						/* START OF REAL ALGEBRAIC GEOMETRY SUPPORT. */

						if (algo instanceof AlgoPointOnPath
								&& algo.input[0] instanceof GeoSegment) {
							// Let P be on segment AB. Then (A-P)*(P-B)>=0 is assumed.
							GeoSegment s = (GeoSegment) algo.input[0];
							GeoPoint A = s.getStartPoint();
							GeoPoint B = s.getEndPoint();
							PVariable[] geoVariablesA = ((SymbolicParametersBotanaAlgo) A)
									.getBotanaVars(A);
							PVariable[] geoVariablesB = ((SymbolicParametersBotanaAlgo) B)
									.getBotanaVars(B);
							PPolynomial a1mp1 = new PPolynomial(geoVariablesA[0])
									.subtract(new PPolynomial(geoVariables[0]));
							PPolynomial a2mp2 = new PPolynomial(geoVariablesA[1])
									.subtract(new PPolynomial(geoVariables[1]));
							PPolynomial p1mb1 = new PPolynomial(geoVariables[0])
									.subtract(new PPolynomial(geoVariablesB[0]));
							PPolynomial p2mb2 = new PPolynomial(geoVariables[1])
									.subtract(new PPolynomial(geoVariablesB[1]));
							PPolynomial lhs = a1mp1.multiply(p1mb1).add(a2mp2.multiply(p2mb2));
							if (addIneq(lhs.toString() + ">=0")) {
								if (ProverSettings.get().captionAlgebra) {
									geo.addCaptionBotanaPolynomial(
											lhs.toTeX().replace("=", "\\geq"));
								}
							}
						}

						if (algo instanceof AlgoIncircleCenter) {
							// Idea taken from https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
							GeoPoint A = (GeoPoint) algo.input[0];
							GeoPoint B = (GeoPoint) algo.input[1];
							GeoPoint C = (GeoPoint) algo.input[2];
							GeoPoint P = (GeoPoint) geo;
							PPolynomial p1 = tripletSign(P, A, B);
							PPolynomial p2 = tripletSign(P, B, C);
							PPolynomial p3 = tripletSign(P, C, A);
							String d1 = p1.toString();
							String d2 = p2.toString();
							String d3 = p3.toString();
							String all_pos = "((" + d1 + ">0)AND" + "(" + d2 + ">0)AND" + "(" + d3 + ">0))";
							String all_neg = "((" + d1 + "<0)AND" + "(" + d2 + "<0)AND" + "(" + d3 + "<0))";
							if (addIneq(all_pos + "OR" + all_neg)) {
								if (ProverSettings.get().captionAlgebra) {
									geo.addCaptionBotanaPolynomial(p1.toTeX().replace("=", ">") +
										"\\land " + p2.toTeX().replace("=", ">") +
										"\\land " + p3.toTeX().replace("=", ">") +
										"\\\\" +
										"\\lor " + p1.toTeX().replace("=", "<") +
										"\\land " + p2.toTeX().replace("=", "<") +
										"\\land " + p3.toTeX().replace("=", "<"));
								}
							}
						}

						if (algo instanceof AlgoPointInRegion) {
							// Idea taken from https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
							GeoPolygon p = (GeoPolygon) algo.input[0];
							if (p.getPointsLength() == 3) {
								GeoPoint A = p.getPoint(0);
								GeoPoint B = p.getPoint(1);
								GeoPoint C = p.getPoint(2);
								GeoPoint P = (GeoPoint) geo;
								PPolynomial p1 = tripletSign(P, A, B);
								PPolynomial p2 = tripletSign(P, B, C);
								PPolynomial p3 = tripletSign(P, C, A);
								String d1 = p1.toString();
								String d2 = p2.toString();
								String d3 = p3.toString();
								String all_pos =
										"((" + d1 + ">0)AND" + "(" + d2 + ">0)AND" + "(" + d3
												+ ">0))";
								String all_neg =
										"((" + d1 + "<0)AND" + "(" + d2 + "<0)AND" + "(" + d3
												+ "<0))";
								if (addIneq(all_pos + "OR" + all_neg)) {
									if (ProverSettings.get().captionAlgebra) {
										geo.addCaptionBotanaPolynomial(
												p1.toTeX().replace("=", ">") +
														"\\land " + p2.toTeX().replace("=", ">") +
														"\\land " + p3.toTeX().replace("=", ">") +
														"\\\\" +
														"\\lor " + p1.toTeX().replace("=", "<") +
														"\\land " + p2.toTeX().replace("=", "<") +
														"\\land " + p3.toTeX().replace("=", "<"));
									}
								}
							}
						}

						if (algo instanceof AlgoAngularBisectorPoints) {
							// Idea taken from https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
							GeoElement[] input = algo.getInput();
							GeoPoint A = (GeoPoint) input[0];
							GeoPoint B = (GeoPoint) input[1];
							GeoPoint C = (GeoPoint) input[2];
							PVariable[] vM = new PVariable[2];
							vM[0] = geoVariables[0];
							vM[1] = geoVariables[1];
							PPolynomial p1 = tripletSign(A, B, vM);
							PPolynomial p2 = tripletSign(B, C, vM);
							PPolynomial p = p1.multiply(p2);
							String d = p.toString() + ">0";
							if (addIneq(d)) {
								if (ProverSettings.get().captionAlgebra) {
									geo.addCaptionBotanaPolynomial(p.toTeX()
											.replace("=", ">"));
								}
							}
						}

						/* GeoGebra shows two bisector lines if the angular bisectors are
						 * defined by two segments/lines (and not vertices).
						 * So it is not needed here to designate one of them.
						 */
						// Note: THIS IS DISABLED FOR NOW.
						if (algo instanceof AlgoAngularBisectorLines && false) {
							// Idea taken from https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
							GeoElement[] input = algo.getInput();
							PVariable[] vA = new PVariable[2];
							PVariable[] vB = new PVariable[2];
							PVariable[] vC = new PVariable[2];
							PVariable[] vM = new PVariable[2];
							vA[0] = geoVariables[6];
							vA[1] = geoVariables[7];
							vB[0] = geoVariables[8];
							vB[1] = geoVariables[9];
							vC[0] = geoVariables[2];
							vC[1] = geoVariables[3];
							vM[0] = geoVariables[0];
							vM[1] = geoVariables[1];
							PPolynomial p1 = tripletSign(vA, vB, vM);
							PPolynomial p2 = tripletSign(vB, vC, vM);
							PPolynomial p = p1.multiply(p2);
							String d = p.toString() + ">0";
							if (addIneq(d)) {
								if (ProverSettings.get().captionAlgebra) {
									geo.addCaptionBotanaPolynomial(p.toTeX()
											.replace("=", ">"));
								}
							}
						}

						if (algo instanceof AlgoPolygonRegular) {
							// see https://geogebra-prover.myjetbrains.com/youtrack/issue/TP-69
							GeoPoint A = (GeoPoint) algo.input[0];
							GeoPoint B = (GeoPoint) algo.input[1];
							int num = (int) ((GeoNumeric) algo.input[2]).getValue();
							AlgoPolygonRegular apr = (AlgoPolygonRegular) algo;
							GeoPoint C = (GeoPoint) apr.getOutput(num + 1);
							// int outputLength = apr.getOutputLength();
							// GeoPoint C = (GeoPoint) apr.getOutput(outputLength - 1); // use the last vertex
							if (num != 4) { // unnecessary condition for n=4 (it is unambiguous)
								PPolynomial p = tripletSign(A, B, C);
								String d = p.toString() + ">0";
								if (addIneq(d)) {
									if (ProverSettings.get().captionAlgebra) {
										geo.addCaptionBotanaPolynomial(p.toTeX().replace("=", ">"));
									}
								}
							}
							if (num > 4) {
								if (num < 9) {
									PPolynomial p = tripletSignRotated(A, B, C);
									String e = p.toString() + ">0";
									if (addIneq(e)) {
										if (ProverSettings.get().captionAlgebra) {
											geo.addCaptionBotanaPolynomial(
													p.toTeX().replace("=", ">"));
										}
									}
								} else {
									// General method, contributed by Keiichi Tsujimoto.
									// https://geogebra-prover.myjetbrains.com/youtrack/issue/TP-69
									// Take the rotation polynomial:
									PPolynomial p = geoPolynomials[0];
									// Compute its two biggest roots and take their arithmetical mean.
									// The mean will separate all other roots from the biggest one.
									String strForGiac = "[[p:=" + p.toString() + "],[s:=solve(p=0)]," +
											"[s1:=s[-2][2]],[s2:=s[-1][2]]," +
											"[lvar(p)[0]>simplify(floor((s1+s2)/2*1000)/1000)]][4][0]";
									// Here we assume that the roots are isolated enough to use
									// rounding by 3 digits. For small n values this should not be a problem.
									// For large n values the computation is probably infeasible,
									// so we ignore that issue...
									CASGenericInterface c = algo.getKernel().getGeoGebraCAS().getCurrentCAS();
									try {
										String e = c.evaluateRaw(strForGiac);
										if (addIneq(e)) {
											if (ProverSettings.get().captionAlgebra) {
												geo.addCaptionBotanaPolynomial(e.replaceAll("v(\\d*)", "v_{$1}").
														replaceAll("\\(", "").
														replaceAll("\\)",""));
											}
										}
									} catch (Throwable t) {
										Log.debug("Error computing bound for vertex coordinate");
									}
								}
							}
						}

						/* END OF REAL GEOMETRY SUPPORT. */

						if (geoPolynomials != null) {
							if (geo instanceof GeoPoint) {
								PVariable[] v;
								v = ((SymbolicParametersBotanaAlgo) geo)
										.getBotanaVars(geo);
								Log.debug("// Constrained point "
										+ geo.getLabelSimple() + "(" + v[0]
										+ "," + v[1] + ")");
								if (proverSettings.captionAlgebra) {
									String color = "cyan";
									if (algo instanceof AlgoPointOnPath) {
										color = "greenyellow";
										// Seems to be given lowercased to avoid NPE. FIXME.
									}
									geo.setCaptionBotanaVars("(" + v[0].toTeX()
											+ "," + v[1].toTeX() + ")", color);
								}
							}
							boolean useThisPoly = true;
							if (algo != null && algo instanceof AlgoPointOnPath
									&& geoProver
									.getProverEngine() == ProverEngine.LOCUS_EXPLICIT) {
								/*
								 * Is this an Envelope command with geo on the
								 * virtual path? In this case we should not
								 * change to the numerical approach.
								 */
								if (!algo.equals(
										geoStatement.getParentAlgorithm())) {
									/*
									 * Skip this object for now: it is a point
									 * on a path. Its coordinates will be used
									 * directly (with substitution) or---for the
									 * moving point---the numerical poly will be
									 * used.
									 */
									useThisPoly = false;
								}
							}
							if (numerical == null) {
								// there is no numerical object,
								// so we still use this poly
								useThisPoly = true;
								/* Do not use objects that are not necessary for the proof and may make computation slow.
								 * This is especially important for envelope computations.
								 * In some cases we define a path (e.g. a parabola), but the path definition
								 * is unnecessary, even if we use the definition when setting up further
								 * equations (like a point on that path). So we keep the later equations and
								 * drop the path definition.
								 */
								if (algo instanceof AlgoParabolaPointLine ||
									algo instanceof  AlgoEllipseHyperbolaFociPoint) {
									useThisPoly = false;
								}
							}
							if (useThisPoly) {
								Log.debug("Hypotheses:");
								addGeoPolys(geo, geoPolynomials);
								/*
								for (PPolynomial p : geoPolynomials) {
									if (proverSettings.captionAlgebra) {
										geo.addCaptionBotanaPolynomial(
												p.toTeX());
									}
								}
								*/
							} else {
								Log.debug(
										"This object will be computed numerically");
							}
						}
					} catch (NoSymbolicParametersException e) {
						Log.info(geo.getParentAlgorithm()
								+ " is not fully implemented");
						result = ProofResult.UNKNOWN;
						if (!(geo.getKernel().isSilentMode()) &&
								(geoProver.getProverEngine() == ProverEngine.LOCUS_EXPLICIT ||
										geoProver.getProverEngine()
												== ProverEngine.LOCUS_IMPLICIT)) {
							Construction cons = geo.getConstruction();
							RelationPane tablePane =
									cons.getApplication().getFactory().newRelationPane();
							final RelationPane.RelationRow[] rr = new RelationPane.RelationRow[1];
							rr[0] = new RelationPane.RelationRow();
							StringBuilder html = new StringBuilder("<html>");
							rr[0].setInfo(html.toString());
							Localization loc = cons.getApplication().getLocalization();
							String problematicCommand = geo.getDefinitionForInputBar();

							String warning = loc.getPlainDefault("AnimatedGIF.ErrorA",
									"Error: " + problematicCommand,
									problematicCommand);

							tablePane.showDialog(warning, rr,
									cons.getApplication());
							String msg1 = loc.getMenuDefault("UnsupportedSteps",
									"The construction contains unsupported steps.");
							String msg2 = loc.getMenuDefault("RedrawDifferently",
									"Please redraw the figure in a different way.");
							tablePane.changeRowLeftColumn(0, "<html>" + msg1 + "<br>" +
									msg2 + "</html>");
						}
						return;
					}
				} else {
					Log.info(geo.getParentAlgorithm() + " unimplemented");
					result = ProofResult.UNKNOWN;
					return;
				}
			}
			/*
			 * Processing numerical object. The equation computed by GeoGebra
			 * will be used.
			 */
			Log.debug("Processing numerical object");
			if (numerical != null) {
				try {
					PVariable[] vars = ((SymbolicParametersBotanaAlgo) movingPoint)
							.getBotanaVars(movingPoint);
					Kernel kernel = geoStatement.kernel;
					// int decimals = kernel.getPrintDecimals();
					// kernel.setPrintDecimals(decimals * 2);
					String strForGiac = getFormulaString(numerical);
					// kernel.setPrintDecimals(decimals);
					GeoGebraCAS cas = (GeoGebraCAS) kernel.getGeoGebraCAS();
					String giacOutput4 = "";
					try {
						String giacOutput = cas.getCurrentCAS()
								.evaluateRaw(strForGiac);
						// create a poly instead of equation
						String strForGiac2 = "lhs(" + giacOutput + ")-rhs("
								+ giacOutput + ")";
						String giacOutput2 = cas.getCurrentCAS()
								.evaluateRaw(strForGiac2);
						// create a poly with integer coeffs: lcm of
						// denominators
						String strForGiac3 = "lcm(denom(coeff(" + giacOutput2
								+ ")))";
						String giacOutput3 = cas.getCurrentCAS()
								.evaluateRaw(strForGiac3);
						// multiply with the lcm
						String strForGiac4 = "expand((" + giacOutput2 + ") * "
								+ giacOutput3 + ")";
						giacOutput4 = cas.getCurrentCAS()
								.evaluateRaw(strForGiac4);
					} catch (Throwable t) {
						Log.debug("Problem on running Giac");
						result = ProofResult.UNKNOWN;
						return;
					}
					String outputSubst = giacOutput4
							.replaceAll("x", vars[0].toString())
							.replaceAll("y", vars[1].toString());
					/*
					 * Now we have the equation in terms of the Botana
					 * variables. Next, we have to convert the equation to a
					 * Botana polynomial. This piece of code is borrowed from
					 * AlgoDependentNumber.
					 */
					ValidExpression resultVE = cas.getCASparser()
							.parseGeoGebraCASInputAndResolveDummyVars(
									outputSubst, kernel, null);
					PolynomialNode polyNode = new PolynomialNode();
					ExpressionNode en = new ExpressionNode(kernel, resultVE);
					AlgoDependentNumber adn = new AlgoDependentNumber(
							geoStatement.getConstruction(), en, false, null,
							false);
					DependentNumberAdapter proverAdapter = adn.getProverAdapter();
					proverAdapter.setBotanaVars(vars);
					proverAdapter.buildPolynomialTree(en, polyNode);
					proverAdapter.expressionNodeToPolynomial(en, polyNode);
					while (polyNode.getPoly() == null) {
						proverAdapter.expressionNodeToPolynomial(en, polyNode);
					}
					/* Finally we obtain the Botana polynomial. */
					PPolynomial botanaPolynomial = polyNode.getPoly();
					/* Don't use this algo any longer. */
					movingPoint.getConstruction().removeFromAlgorithmList(adn);
					movingPoint.getConstruction()
							.removeFromConstructionList(adn);
					Log.debug("Hypothesis:");
					PPolynomial[] botanaPolynomials = new PPolynomial[1];
					botanaPolynomials[0] = botanaPolynomial;
					addGeoPolys(movingPoint, botanaPolynomials);
					// Maybe to be removed:
					if (proverSettings.captionAlgebra) {
						numerical.addCaptionBotanaPolynomial(
								botanaPolynomial.toTeX());
					}
				} catch (NoSymbolicParametersException e) {
					Log.info("Unhandled case on processing numerical objects");
					result = ProofResult.UNKNOWN;
					return;
				}
			}
			Log.debug("Hypotheses have been processed.");
		}

		/**
		 * @param numerical numerical object
		 * @return equation as string
		 */
		String getFormulaString(GeoElement numerical) {

			return numerical.getFormulaString(
					StringTemplate.giacTemplateInternal, true);
		}

		private void setThesis() {
			try {
				interpretTrueAsUndefined = false;
				/*
				 * The sets of statement polynomials. The last equation of each
				 * set will be negated.
				 */

				PPolynomial[][] statements;
				AlgoElement ae = geoStatement.getParentAlgorithm();
				if (ae != null) {
					statements = ((SymbolicParametersBotanaAlgoAre) ae)
							.getBotanaPolynomials();
				} else {
					statements = new PPolynomial[1][1];
					if (geoStatement instanceof GeoBoolean) {
						if (((GeoBoolean) geoStatement).getBoolean()) {
							statements[0][0] = new PPolynomial(0);
						} else {
							statements[0][0] = new PPolynomial(BigInteger.ONE);
						}
					} else {
						Log.debug(
								"Unhandled case, statement is UNKNOWN at the moment");
						result = ProofResult.UNKNOWN;
						return;
					}
				}

				/* case input was an expression */
				if (statements == null) {
					statements = getExpressionStatements(geoStatement);
					if (result != null) {
						return;
					}
					if (dryRun) {
						return; // Do not compute anything. It will be done in AlgoCompare.
					}
					if (thesisIneq != null) {
						Log.debug("Thesis inequality = " + thesisIneq);
						proveInequality();
						return;
					}
					if (statements == null) {
						// TODO: If this is a locus computation and the thesisIneq is not empty,
						// try to use some workaround...
						return; // no success
					}
				}

				if (disallowFixSecondPoint) {
					maxFixcoords = 2;
				}

				AlgoElement algo = geoStatement.getParentAlgorithm();
				if (algo instanceof AlgoAreCongruent) {
					if (algo.input[0] instanceof GeoAngle
							&& algo.input[1] instanceof GeoAngle) {
						interpretTrueAsUndefined = true;
						// FIXME: this should be removed, and an essential
						// condition added
					}
				}
				if (algo instanceof AlgoDependentBoolean) {
					Operation operation = ((AlgoDependentBoolean) algo)
							.getOperation();
					if (operation == IS_ELEMENT_OF) {
						if (algo.input[0] instanceof GeoConic
								&& (((GeoConic) algo.input[0]).isEllipse()
								|| ((GeoConic) algo.input[0])
								.isHyperbola())) {
							interpretTrueAsUndefined = true;
						} else if (algo.input[1] instanceof GeoConic
								&& (((GeoConic) algo.input[1]).isEllipse()
								|| ((GeoConic) algo.input[1])
								.isHyperbola())) {
							interpretTrueAsUndefined = true;
						}
					} else if (operation == EQUAL_BOOLEAN) {
						if ((algo.input[0] instanceof GeoAngle
								&& algo.input[1] instanceof GeoAngle)) {
							interpretTrueAsUndefined = true;
							// FIXME: this should be removed, and an essential
							// condition added
						}
					}
				}

				int k = polynomials.size();

				int minus = 1;
				if (geoProver
						.getProverEngine() == ProverEngine.LOCUS_IMPLICIT) {
					minus = 0;
				}
				ProverSettings proverSettings = ProverSettings.get();
				Log.debug("Thesis equations (non-denied ones):");
				for (PPolynomial[] statement : statements) {
					for (int j = 0; j < statement.length - minus; ++j) {
						/* Note: the geo is not stored */
						if (addPolynomial(statement[j])) {
							if (proverSettings.captionAlgebra) {
								geoStatement.addCaptionBotanaPolynomial(
										statement[j].toTeX());
							}
							Log.debug((k + 1) + ". " + statement[j]);
						}
						k++;
					}
				}

				if (geoProver
						.getProverEngine() == ProverEngine.LOCUS_IMPLICIT) {
					Log.debug("Not using refutation");
					return;
				}

				/*
				 * Rabinowitsch trick for the last polynomials of the theses of
				 * the statement. Here we use that NOT (A and B and C) == (NOT
				 * A) or (NOT b) or (NOT c), and disjunctions can be algebraized
				 * by using products.
				 */
				Log.debug(
						"Thesis reductio ad absurdum (denied statement), product of factors:");
				PPolynomial spoly = new PPolynomial(BigInteger.ONE);
				PVariable z = new PVariable(geoStatement.getKernel());
				/*
				 * It is OK to use the same variable for each factor since it is
				 * enough to find one counterexample only for one of the theses.
				 * See
				 * http://link.springer.com/article/10.1007%2Fs10817-009-9133-x
				 * Appendix, Proposition 6 and Corollary 2 to read more on this.
				 * FIXME: this always introduces an extra variable, shouldn't
				 * do.
				 */
				thesisFactors = new PPolynomial[statements.length];
				int i = 0;
				for (PPolynomial[] statement : statements) {
					PPolynomial factor = (statement[statement.length - 1]);
					thesisFactors[i] = factor;
					Log.debug("(" + factor + ")*" + z + "-1");
					factor = factor.multiply(new PPolynomial(z))
							.subtract(new PPolynomial(BigInteger.ONE));
					spoly = spoly.multiply(factor);
					i++;
				}
				/*
				 * We store the geoStatement -> product mapping. Later we should
				 * be able to remove this and use the last polys for checking
				 * the negated statement.
				 */
				PPolynomial[] spolys = new PPolynomial[1];
				spolys[0] = spoly;
				addGeoPolys(geoStatement, spolys);
				/* if (proverSettings.captionAlgebra) {
					geoStatement.addCaptionBotanaPolynomial(spoly.toTeX());
				}
				 */

			} catch (NoSymbolicParametersException e) {
				Log.debug(
						"Unsuccessful run, statement is UNKNOWN at the moment");
				result = ProofResult.UNKNOWN;
			}

		}

		private StringBuilder removeLastChar(StringBuilder sb) {
			sb.deleteCharAt(sb.length() - 1);
			return sb;
		}

		public static String trimBraces(String s) {
			if (s.startsWith("{") && s.endsWith("}")) {
				s = s.substring(1, s.length() - 1);
			}
			return s;
		}

		public StringBuilder getRGParameters() {
			RealGeomWebService realgeomWS = geoStatement.getConstruction().getApplication().getRealGeomWS();

			/* Force some non-degeneracies. */
			PPolynomial[] nonDegPolys;
			try {
				nonDegPolys = create3FreePointsNeverCollinearNDG(geoProver);
			} catch (Exception e) {
				return null;
			}
			for (PPolynomial ndp : nonDegPolys) {
				addPolynomial(ndp);
			}

			StringBuilder rgParameters = new StringBuilder();

			/* get distance polynomials */
			AlgoElement algo = geoStatement.getParentAlgorithm();
			ArrayList<PPolynomial> segPolys =
					((AlgoDependentBoolean) algo).getProverAdapter().getExtraPolys();
			PVariable[] extraVars =
					((AlgoDependentBoolean) algo).getProverAdapter().getBotanaVars();
			if (ProverSettings.get().captionAlgebra) {
				ArrayList<Map.Entry<GeoSegment, PPolynomial>> segmentBotanaPolys =
						((AlgoDependentBoolean) algo).getProverAdapter().getSegmentBotanaPolys();
				Iterator<Map.Entry<GeoSegment, PPolynomial>> it = segmentBotanaPolys.iterator();
				while (it.hasNext()) {
					Map.Entry<GeoSegment, PPolynomial> entry = it.next();
					GeoSegment s = entry.getKey();
					PPolynomial p = entry.getValue();
					if (ProverSettings.get().captionAlgebra) {
						s.addCaptionBotanaPolynomial(s.getLabelTextOrHTML() + ":" + p.toTeX());
					}
				}
			}
			for (PPolynomial sp : segPolys) {
				addExtPoly(sp.toString()); // add segment polynomials as external polynomials
			}

			computeStrings();

			// The first ones of these variables will be most certainly substituted later.
			// So their order is important. We should not add here any variables that are
			// not to be eliminated.
			String vars = getRGVars();
			rgParameters.append("vars=").append(vars);

			StringBuilder posvariables = new StringBuilder();
			for (PVariable v : extraVars) {
				posvariables.append(v.getName()).append(",");
			}
			for (String v : posVars) {
				posvariables.append(v).append(",");
			}
			if (!posVars.isEmpty()) {
				removeLastChar(posvariables); // remove last ,
			}
			rgParameters.append("&posvariables=").append(posvariables);

			// This should be the last one, because some additional polys may be added.
			rgParameters.append("&polys=");
			String polys = getPolys();
			rgParameters.append(polys);
			// Add all external polynomials
			if (!polys.equals("") && !extPolys.isEmpty()) {
				rgParameters.append(",");
			}
			for (String p : extPolys) {
				rgParameters.append(p).append(",");
			}
			if (!extPolys.isEmpty()) {
				removeLastChar(rgParameters); // remove last ,
			}

			// Inequalities
			StringBuilder ies = new StringBuilder();
			for (String ie : ineqs) {
				ie = ie.replace("=", "E");
				ies.append(ie).append(",");
			}
			if (!ineqs.isEmpty()) {
				rgParameters.append("&ineqs=");
				removeLastChar(ies); // remove last ,
				rgParameters.append(ies);
			}

			if (realgeomWS != null && realgeomWS.isAvailable()) {
				String rgwsCas = realgeomWS.getCAS();
				rgParameters.append("&cas=" + rgwsCas);
			}
			// This should be set but QEPCAD has problems with this:
			// rgParameters.append("&maxfixcoords=" + maxFixcoords);

			Log.debug(rgParameters);
			return rgParameters;
		}

		public String rewriteResult(String rgResult) {
			if ("$Aborted".equals(rgResult)) {
				Log.debug("Timeout in RealGeom");
				rgResult = "";
			}
			if ("$Failed".equals(rgResult)) {
				Log.debug("Computation issue in RealGeom");
				rgResult = "";
			}
			if ("GIAC ERROR".equals(rgResult)) {
				Log.debug("Giac error in RealGeom");
				rgResult = "";
			}
			if ("QEPCAD ERROR".equals(rgResult)) {
				Log.debug("Qepcad error in RealGeom");
				rgResult = "";
			}
			if ("TARSKI ERROR".equals(rgResult)) {
				Log.debug("Tarski error in RealGeom");
				rgResult = "";
			}
			return rgResult;
		}

		private void proveInequality() {
			/* Handle some special cases. It is possible that DependentBooleanAdapter.exprCode
			 * already computed this accidentally, if the input was some simple algebraic
			 * formula. We acknowledge this and simply return that value.
			 * FIXME in DependentBooleanAdapter.exprCode.
			 */
			if (thesisIneq.equals("true")) {
				result = ProofResult.TRUE;
				return;
			}
			if (thesisIneq.equals("false")) {
				result = ProofResult.FALSE;
				return;
			}

			String rgCommand = "euclideansolver";
			StringBuilder rgParameters = getRGParameters();
			if (rgParameters == null) {
				result = ProofResult.UNKNOWN;
				return;
			}
			// Do not send = via http, it is reserved:
			thesisIneq = thesisIneq.replace(">=", "≥")
					.replace("<=", "≤").replace("=", "E");

			rgParameters.append("&ineq=").append(thesisIneq);

			RealGeomWebService realgeomWS = geoStatement.getConstruction().getApplication().getRealGeomWS();

			rgParameters.append("&mode=prove");
			Log.debug(rgParameters);
			String rgResult = "";

			if (realgeomWS != null && realgeomWS.isAvailable()) {
				rgResult = realgeomWS.directCommand(rgCommand, rgParameters.toString());
			} else {
				String[] rgs = rgParameters.toString().split("&");
				Log.debug("maxFixcoords = " + maxFixcoords);
				if (maxFixcoords == -1) {
					maxFixcoords = 4;
				}
				if (maxFixcoords == 4) {
					NDGCondition ndgc = new NDGCondition();
					ndgc.setCondition("AreEqual");
					GeoElement[] geos = new GeoElement[2];
					int i = 0;
					List<GeoElement> fp = getFreePoints(geoStatement);
					geos[0] = fp.get(0);
					geos[1] = fp.get(1);
					ndgc.setGeos(geos);
					Arrays.sort(ndgc.getGeos());
					geoProver.addNDGcondition(ndgc);
				}
				rgResult = Compute.euclideanSolverProve(geoStatement.kernel, maxFixcoords, paramLookup(rgs, "ineq"),
								paramLookup(rgs, "ineqs"), paramLookup(rgs, "polys"),
								paramLookup(rgs, "triangles"), paramLookup(rgs, "vars"),
						paramLookup(rgs, "posvariables"), freeVars);

			}

			rgResult = rewriteResult(rgResult);

			if (rgResult == null || rgResult.equals("")) {
				result = ProofResult.UNKNOWN;
				return;
			}

			if (rgResult.equals("false")) {
				result = ProofResult.TRUE;
				return;
			}

			if (rgResult.equals("true")) {
				result = ProofResult.FALSE;
				return;
			}

			if (!(rgResult.contains(" = ") || rgResult.contains(" > ") || rgResult.contains(" < ")
				|| rgResult.contains(" >= ") || rgResult.contains(" <= ") || rgResult.contains(" \\/ "))) {
				// If the result is a conjunction of NOT-EQUAL operations, return false.
				// In this case the statement is true under some condition which is a disjunction
				// of EQUAL operations which has a lower dimension as the variable space.
				result = ProofResult.FALSE;
				return;
			}

			if (!rgResult.contains(" /\\ ") && rgResult.contains(" /= ")) {
				// If the result is a disjunction of certain operations, and at least a NOT-EQUAL appears, return false.
				// In this case the statement is true under some condition which contains a conjunction
				// of an EQUAL operation which has a lower dimension as the variable space.
				result = ProofResult.FALSE;
				return;
			}

			// We could process the output here, maybe the conditions can be read off somehow... TODO

			// In general it is difficult to tell if the negation of the result is "small enough".
			// Here we handle some special cases and leave the decision of the general case
			// for future work.

			if (ineqs.size() > 0) {
				// There may be some assumptions that disallow a simple negation.
				// For example, we assume that a point is on a segment (not just on a line),
				// and this should be added as a conjunctive part to the negation. TODO.
				// Now we return with UNKNOWN.
				Log.debug("There are hypotheses that are of form inequality.");
				result = ProofResult.UNKNOWN;
				return;
			}

			// Here we use the fact that there are no inequality assumptions.
			// 1. round, only negation.
			String rgResultP = geoStatement.getKernel().getApplication().tarski.evalCached("(t-neg [" + rgResult + "])");
			rgResultP = Compute.getTarskiOutput(rgResultP);
			Log.debug("resultP=" + rgResultP);
			// A special case: If there is no conjunction and there is at least one inequality (<, <=, >, >=, /=)
			// in the "positive" result, then the statement holds in a "large enough" set:
			if (!(rgResultP.contains(" /\\ ")) && (rgResultP.contains(" > ") || rgResultP.contains(" < ")
					|| rgResultP.contains(" >= ") || rgResultP.contains(" <= ") || rgResultP.contains(" /= "))) {
				// e.g. 2 v5 - 3 >= 0 \/ 2 v5 + 1 <= 0
				result = ProofResult.TRUE_ON_COMPONENTS;
				return;
			}
			// Another special case: If there is no disjunction and there is at least one inequality (<, <=, >, >=, /=)
			// in the "positive" result, then the statement holds in a "large enough" set:
			if (!(rgResultP.contains(" \\/ ")) && (rgResultP.contains(" > ") || rgResultP.contains(" < ")
					|| rgResultP.contains(" >= ") || rgResultP.contains(" <= ") || rgResultP.contains(" /= "))) {
				// e.g. v5 >= 0 /\ v5 - 1 <= 0
				result = ProofResult.TRUE_ON_COMPONENTS;
				return;
			}
			// 2. round, simplifying the negation.
			// Maybe we can simplify the formula... In fact, in some cases this will not simplify
			// things, either... :-( TODO in Tarski/QEPCAD.
			// Note that qepcad-api-call can be expensive when there are more variables.
			String rgResultP2 = geoStatement.getKernel().getApplication().tarski.evalCached("(qepcad-api-call [" + rgResultP + "])");
			Log.debug("resultP2=" + rgResultP2);
			if (!(rgResultP2.contains(" /\\ ")) && (rgResultP2.contains(" > ") || rgResultP2.contains(" < ")
					|| rgResultP2.contains(" >= ") || rgResultP2.contains(" <= ") || rgResultP2.contains(" /= "))) {
				result = ProofResult.TRUE_ON_COMPONENTS;
				return;
			}
			if (!(rgResultP2.contains(" \\/ ")) && (rgResultP2.contains(" > ") || rgResultP2.contains(" < ")
					|| rgResultP2.contains(" >= ") || rgResultP2.contains(" <= ") || rgResultP2.contains(" /= "))) {
				result = ProofResult.TRUE_ON_COMPONENTS;
				return;
			}

			// No idea what this formula means. Stay on the safe side and say nothing...
			result = ProofResult.UNKNOWN;
		}


		private String convertSqrtToQepcad(String text) {
			char s = Unicode.SQUARE_ROOT;
			RegExp regExp = RegExp.compile(s + "\\d+", "g");
			MatchResult matcher = regExp.exec(text);
			while (matcher != null) {
				String number = matcher.getGroup(0).substring(1);
				String qexpr = "(sqrt" + number + ")";
				addIneq("sqrt" + number + "^2=" + number);
				addPosVar("sqrt" + number);
				matcher = regExp.exec(text);
			}
			return text.replace(s + "", "sqrt");
		}

		private PPolynomial[][] getExpressionStatements(GeoElement geoStatement) {
			PPolynomial[][] statements;

			AlgoElement algo = geoStatement.getParentAlgorithm();
			/*
			 * First the MEP code must be computed. It implicitly computes the Botana variables
			 * as well. Even if we do not use the MEP code (for inequalities), the Botana
			 * variables are necessary.
			 */
			String mepCode = null;
			try {
				mepCode = ((AlgoDependentBoolean) algo).minimalExtendedPolyGiacCode();
			} catch (NoSymbolicParametersException e) {
				Log.debug("Error during creating MEP code");
				return null;
			}
			String pCode = ((AlgoDependentBoolean) algo).exprGiacCode();

			Kernel k = geoStatement.getKernel();
			GeoGebraCAS cas = (GeoGebraCAS) k.getGeoGebraCAS();
			CASGenericInterface c = cas.getCurrentCAS();

			// In some cases we force running computations via real geometry.
			boolean forceRG = false;
			if (pCode.contains("sqrt") && geoProver.getProverEngine() != ProverEngine.LOCUS_IMPLICIT ) {
				forceRG = true;
			}
			if (!ineqs.isEmpty() && geoProver.getProverEngine() != ProverEngine.LOCUS_IMPLICIT) {
				forceRG = true;
			}

			Operation operation = ((AlgoDependentBoolean) algo).getOperation();
			if (forceRG || operation == LESS || operation == LESS_EQUAL ||
					operation == GREATER || operation == GREATER_EQUAL) {
				Log.debug("Inequality");
				try {
					thesisIneq = c.evaluateRaw(pCode);
					thesisIneq = thesisIneq.substring(1, thesisIneq.length() - 1); // trim { }
					thesisIneq = convertSqrtToQepcad(thesisIneq);
					Log.debug("thesisIneq1 = " + thesisIneq);
					// Simplify (to avoid division by expressions, if possible):
					String giacCode = "simplify(" + thesisIneq + ")";
					// If division cannot be avoided, we use multiplications instead...
					thesisIneq = cas.evaluateRaw(giacCode);
					Log.debug("thesisIneq2 = " + thesisIneq);
					thesisIneq = thesisIneq.replaceAll("\\)/\\(", ")*(");
					Log.debug("thesisIneq3 = " + thesisIneq);
					// This workaround is very dirty and should be solved in a different way.

					// get degree
					String degExp = thesisIneq.replace(">=", "-")
							.replace("<=", "-").replace(">", "-").replace("<", "-");
					int deg = getDegree(degExp);
					if (deg == -1) {
						result = ProofResult.FALSE;
						return null;
					}

					if (operation == EQUAL_BOOLEAN) {
						thesisIneq += "=0";
					}
					return null;
				} catch (Throwable e) {
					Log.debug(
							"Unsuccessful run on evaluating the expression, statement is UNKNOWN at the moment");
					result = ProofResult.UNKNOWN;
					return null;
				}
			}

			/*
			 * Disallow fixing the second point. This is crucial, otherwise false theorems
			 * like Segment[A,B]==1 will be proven.
			 */
			maxFixcoords = 2;
			// We set this here and not before.
			// Otherwise the inequalities will be too difficult to solve.

			try {
				/* K: extended polynomial */
				String output = c.evaluateRaw(mepCode);
				/* F: user's polynomial formula */
				String userOutput = c.evaluateRaw(pCode);
				/* T = K/F: the factor between user's formula and the extended one */
				String casResult = c.evaluateRaw(
						"simplify(" + output + "/" + userOutput + ")");
				/* unhandled input expression */
				if (output.contains("?") || userOutput.contains("?")
						|| casResult.contains("?")) {
					this.result = ProofResult.UNKNOWN;
					return null;
				}
				/* T is not empty */
				/*
				 * Put possible extended factors into the NDG list. Here we simply parse
				 * the Giac output. This code is ugly, TODO: use a more elegant way.
				 */
				if (geoProver.getProverEngine() != ProverEngine.LOCUS_IMPLICIT
						&& !("{}".equals(casResult))) {
					// skip { and }
					casResult = casResult.substring(1, casResult.length() - 1);
					// factorization of the result
					String factResult = cas.getCurrentCAS()
							.evaluateRaw("factor(" + casResult + ")");
					// removing leading - from a product (if any)
					if (factResult.length() > 1 && factResult.substring(0, 2).equals("-(")) {
						factResult = factResult.substring(1);
					}

					// If the result does not start with a "(", but there is one in it later,
					// it means that this is a product like -v28*v27*(4*v25-v26)*(v23+v24).
					// So we change it to (-v28)*(v27)*(4*v25-v26)*(v23+v24).
					int firstMultParen = factResult.indexOf("*(");
					if (!factResult.startsWith("(") && firstMultParen >= 0) {
						String firstPart = factResult.substring(0, firstMultParen);
						String secondPart = factResult.substring(firstMultParen);
						firstPart = "(" + firstPart + ")";
						firstPart = firstPart.replace("*", ")*(");
						factResult = firstPart + secondPart;
					}

					// split regarding to )*(
					String[] factors = factResult.split("\\)\\*\\(");
					// if there are more factors, the first and last
					// still contain ( and ), trim them
					if (factors.length > 1) {
						factors[0] = factors[0].substring(1);
						factors[factors.length - 1] = factors[factors.length - 1]
								.substring(0, factors[factors.length - 1].length() - 1);
					}
					boolean polyIsConst = false;
					if (factors.length == 1 && factors[0].matches("[-+]?\\d*\\.?\\d+")) {
						polyIsConst = true; // poly is a number
					}
					// list of polynomial factors
					ArrayList<PPolynomial> polyListOfFactors = new ArrayList<>();
					if (!polyIsConst) {
						for (String factor : factors) {
							// parse factors into expression
							// if (factor.startsWith("-")) {
							//	factor = "(" + factor + ")"; // work around leading minus
							// }
							// Log.debug("factor " + factor);

							// For some strange reason the parser sometimes crashes.
							// This can be worked around by adding extra parentheses:
							factor = "(" + factor + ")";

							ValidExpression resultVE = cas.getCASparser().
									parseGeoGebraCASInputAndResolveDummyVars(factor, k, null);
							PolynomialNode polyRoot = new PolynomialNode();
							// build polynomial to parsed expression
							((AlgoDependentBoolean) algo).getProverAdapter().buildPolynomialTree(
									(ExpressionNode) resultVE, polyRoot);
							((AlgoDependentBoolean) algo).getProverAdapter().
									expressionNodeToPolynomial((ExpressionNode) resultVE, polyRoot);
							while (polyRoot.getPoly() == null) {
								((AlgoDependentBoolean) algo).getProverAdapter()
										.expressionNodeToPolynomial((ExpressionNode) resultVE,
												polyRoot);
							}
							// add polynomial to list of polys
							PPolynomial poly = polyRoot.getPoly();
							if (poly != null) {
								polyListOfFactors.add(poly);
							}
						}
					}

					for (PPolynomial p : polyListOfFactors) {
						NDGCondition ndgc =
								new NDGDetector(geoProver, null, freeVariables).detect(p);
						if (ndgc != null) {
							geoProver.addNDGcondition(ndgc);
						}
					}
					/* Put possible extended factors into the NDG list, end. */
				}
				/* giac output is not empty */
				if (!("{}".equals(output))) {
					ValidExpression validExpression = cas.getCASparser()
							.parseGeoGebraCASInputAndResolveDummyVars(output, k, null);
					PolynomialNode polyRoot = new PolynomialNode();
					ExpressionNode expNode =
							new ExpressionNode(k, ((ExpressionNode) validExpression).getLeft());
					MyList list = new MyList(k);
					ExpressionNode root = null;
					if (expNode.getLeft() instanceof MyList) {
						list = ((MyList) expNode.getLeft()).getMyList();
					}
					if (list.getListElement(0).isExpressionNode()) {
						root = (ExpressionNode) list.getListElement(0);
					}

					((AlgoDependentBoolean) algo).getProverAdapter()
							.buildPolynomialTree(root, polyRoot);
					((AlgoDependentBoolean) algo).getProverAdapter()
							.expressionNodeToPolynomial(root, polyRoot);
					while (polyRoot.getPoly() == null) {
						((AlgoDependentBoolean) algo).getProverAdapter()
								.expressionNodeToPolynomial(root, polyRoot);
					}
					/* get distance polynomials */
					ArrayList<PPolynomial> extraPolys =
							((AlgoDependentBoolean) algo).getProverAdapter().getExtraPolys();
					statements = new PPolynomial[1][extraPolys.size() + 1];
					int index = 0;
					for (PPolynomial p : extraPolys) {
						statements[0][index] = p;
						index++;
					}
					if (ProverSettings.get().captionAlgebra) {
						ArrayList<Map.Entry<GeoSegment, PPolynomial>> segmentBotanaPolys =
								((AlgoDependentBoolean) algo).getProverAdapter().getSegmentBotanaPolys();
						Iterator<Map.Entry<GeoSegment, PPolynomial>> it = segmentBotanaPolys.iterator();
						while (it.hasNext()) {
							Map.Entry<GeoSegment, PPolynomial> entry = it.next();
							GeoSegment s = entry.getKey();
							PPolynomial p = entry.getValue();
							if (ProverSettings.get().captionAlgebra) {
								s.addCaptionBotanaPolynomial(
										s.getLabelTextOrHTML() + ":" + p.toTeX());
							}
						}
					}

					/* add input polynomial */
					statements[0][index] = polyRoot.getPoly();
				}
				/* case giac result was empty */
				else {
					statements = new PPolynomial[1][1];
					statements[0][0] = new PPolynomial(0);
				}
			} catch (Throwable e) {
				Log.debug(
						"Unsuccessful run on evaluating the expression, statement is UNKNOWN at the moment");
				result = ProofResult.UNKNOWN;
				return null;
			}

			return statements;
		}

		/**
		 * Get the degree of a polynomial, unless it is non-homogeneous. In this latter case return -1.
		 * We assume that all variables are linear quantities, that is, segments/distances.
		 * @param expr
		 * @return the degree of the polynomial (or -1 in the non-homogeneous case)
		 */
		int getDegree(String expr) {
			// See TP-39 for an explanation:
			// a(x,y,z):=x^2+x*y+2z^2; d:=degree(a(x,y,z)); expand(a(t*x,t*y,t*z))==expand(t^d*a(x,y,z))
			// For some strange reason Giac needs to handle this in the following way:
			// [a(v9,v10):=begin return v9+2+v10; end, d():=begin return total_degree(a(v9,v10),[v9,v10]) end,expand(a(t*v9,t*v10))==expand(t^d()*a(v9,v10)),d()]][2]
			if ("true".equals(expr)) {
				return 0; // handle this special case differently
			}
			String list = "";
			String list_t = "";
			String code_a = "a(";
			String code_at = "a(";

			String varlist;
			Kernel kernel = geoStatement.getKernel();
			GeoGebraCAS cas = (GeoGebraCAS) kernel.getGeoGebraCAS();
			try {
				varlist = trimBraces(cas.evaluateRaw("lvar(" + expr + ")"));
			} catch (Throwable t) {
				return -1; // in fact this is an error
			}
			String[] vars = varlist.split(",");
			// Add these to the extraVars!

			for (String v : vars) {
				if (!extVars.contains(v)) {
					extVars.add(v);
				}
				if (!v.startsWith("sqrt")) {
					list += v + ",";
					list_t += "t*" + v + ",";
				}
			}
			list = list.substring(0, list.length()-1); // remove last ","
			list_t = list_t.substring(0, list_t.length()-1); // remove last ","
			code_a += list + ")"; // add closing ")"
			code_at += list_t + ")"; // add closing ")"

			String code = "[" + code_a + ":=begin return " + expr + "; end, ";
			code += "d():=begin return total_degree(" + code_a + ",[" + list + "]) end,";
			code += "[simplify(" + code_at + "-t^d()*(" + code_a + "))==0,d()]][2]";

			String hominfo = "";
			try {
				hominfo = cas.getCurrentCAS().evaluateRaw(code); // expected e.g.: {true,1} or {false,1}
			} catch (Throwable t) {
				return -1;
			}
			if (hominfo.indexOf("false") > 0) {
				// Not homogeneous.
				return -1;
			}
			int commapos = hominfo.indexOf(',');
			String deg = hominfo.substring(commapos + 1);
			deg = deg.substring(0, deg.length() - 1); // trim "}"
			int degree = Integer.parseInt(deg);
			return degree;
		}

		private void algebraicTranslation(GeoElement statement,
										  GeoElement movingPoint, Prover prover) {
			ProverSettings proverSettings = ProverSettings.get();
			geoStatement = statement;
			geoProver = prover;

			/*
			 * Make sure that the prover has the same statement. FIXME: this is
			 * redundant, it would be enough to set the prover here.
			 */
			prover.setStatement(statement);
			setHypotheses(movingPoint);
			if (result != null) {
				return;
			}
			if (prover.getProverEngine() == ProverEngine.LOCUS_EXPLICIT) {
				return;
			}
			try {
				updateBotanaVarsInv(statement);
			} catch (NoSymbolicParametersException e) {
				Log.debug("Botana vars cannot be inverted");
				result = ProofResult.UNKNOWN;
				return;
			}
			setThesis();
			if (result != null) {
				return;
			}

			/*
			 * Only for the Prove command makes sense to set up extra NDG
			 * conditions
			 */
			if (prover.getProverEngine() != ProverEngine.RECIOS_PROVER
					&& proverSettings.freePointsNeverCollinear != null
					&& proverSettings.freePointsNeverCollinear
					&& !(prover.isReturnExtraNDGs())) {
				try {
					Collections.addAll(polynomials,
							create3FreePointsNeverCollinearNDG(prover));
				} catch (NoSymbolicParametersException e) {
					Log.debug("Extra NDG conditions cannot be added");
					result = ProofResult.UNKNOWN;
				}
			}
		}

	}

	/**
	 * Proves the statement by using Botana's method
	 *
	 * @param prover the prover input object
	 * @return if the statement is true
	 */
	public ProofResult prove(Prover prover) {

		boolean investigateNonGeometricMaximalIndependentSet = false;

		GeoElement statement = prover.getStatement();
		ProverSettings proverSettings = ProverSettings.get();
		Kernel k = statement.getKernel();
		/*
		 * Decide quickly if proving this kind of statement is already
		 * implemented at all:
		 */
		if (!(statement
				.getParentAlgorithm() instanceof SymbolicParametersBotanaAlgoAre)) {
			Log.info(statement.getParentAlgorithm() + " unimplemented");
			return ProofResult.UNKNOWN;
			/*
			 * If not, let's not spend any time here, but give up immediately.
			 */
		}

		/* If Singular is not available, let's try Giac (mainly on web) */
		SingularWebService singularWS = prover.getConstruction()
				.getApplication().getSingularWS();
		if (singularWS == null || (!singularWS.isAvailable())) {
			proverSettings.transcext = false;
		}

		/* The NDG conditions (automatically created): */
		if (proverSettings.freePointsNeverCollinear == null) {
			proverSettings.freePointsNeverCollinear = false;
		}

		AlgebraicStatement as = new AlgebraicStatement(statement, null, prover, false);

		/*
		 * It's possible that we already know the answer without computing
		 * anything on the polynomials. If yes, we quit here and return the
		 * known result.
		 */
		if (as.result != null) {
			return as.result;
		}

		/* Set substitutions. */
		TreeMap<PVariable, BigInteger> substitutions = null;
		int fixcoords;
		if (prover.isReturnExtraNDGs()) {
			fixcoords = proverSettings.useFixCoordinatesProveDetails;
		} else {
			fixcoords = proverSettings.useFixCoordinatesProve;
		}
		if (as.maxFixcoords >= 0 && as.maxFixcoords < fixcoords) {
			fixcoords = as.maxFixcoords;
		}
		if (fixcoords > 0) {
			try {
				substitutions = fixValues(prover, fixcoords);
			} catch (NoSymbolicParametersException e) {
				as.result = ProofResult.UNKNOWN;
				Log.debug("Cannot add fix values");
				return as.result;
			}
			Log.debug("substitutions: " + substitutions);
		}

		if (prover.isReturnExtraNDGs() ||
				!prover.getConstruction().getApplication()
						.singularWSisAvailable()) {
			/* START OF PROVEDETAILS. */
			Set<Set<PPolynomial>> eliminationIdeal;
			NDGDetector ndgd = new NDGDetector(prover, substitutions,
					as.freeVariables);

			boolean found = false;
			int permutation = 0;
			int MAX_PERMUTATIONS = 1; /*
			 * Giac cannot permute the variables at
			 * the moment.
			 */
			if (prover.getConstruction().getApplication()
					.singularWSisAvailable()) {
				/*
				 * TODO: Limit MAX_PERMUTATIONS to (#freevars-#substitutes)! to
				 * prevent unneeded computations:
				 */
				MAX_PERMUTATIONS = 8; /*
				 * intuitively set, see Polynomial.java
				 * for more on info (Pappus6 will work
				 * with 7, too)
				 */
				/* Pappus6 is at https://www.geogebra.org/m/TEQGgRKe */
			}
			while (!found && permutation < MAX_PERMUTATIONS) {

				eliminationIdeal = PPolynomial.eliminate(
						as.getPolynomials()
								.toArray(new PPolynomial[as.getPolynomials()
										.size()]),
						substitutions, k, permutation++, true, false,
						as.freeVariables);
				if (eliminationIdeal == null) {
					return ProofResult.UNKNOWN;
				}

				Iterator<Set<PPolynomial>> ndgSet = eliminationIdeal.iterator();

				List<TreeSet<GeoPoint>> xEqualSet = new ArrayList<>();
				// xEqualSet.add(new TreeSet<GeoPoint>());
				List<TreeSet<GeoPoint>> yEqualSet = new ArrayList<>();
				// yEqualSet.add(new TreeSet<GeoPoint>());
				boolean xyRewrite = (eliminationIdeal.size() == 2);

				List<NDGCondition> bestNdgSet = new ArrayList<>();
				double bestScore = Double.POSITIVE_INFINITY;
				int ndgI = 0;
				while (ndgSet.hasNext()) {
					ndgI++;
					Log.debug("Considering NDG " + ndgI + "...");
					List<NDGCondition> ndgcl = new ArrayList<>();
					double score = 0.0;
					/*
					 * All NDGs must be translatable into human readable form.
					 */
					boolean readable = true;
					Set<PPolynomial> thisNdgSet = ndgSet.next();
					Iterator<PPolynomial> ndg = thisNdgSet.iterator();
					while (ndg.hasNext() && readable) {
						PPolynomial poly = ndg.next();
						if (poly.isZero()) {

							/*
							 * Here we know that the statement is reported to be
							 * not generally true.
							 */
							Log.debug("Statement is NOT GENERALLY TRUE");

							/*
							 * It is possible that the statement is not
							 * generally false, either.
							 *
							 */
							as.removeThesis();
							as.addNegatedThesis();
							eliminationIdeal = PPolynomial.eliminate(
									as.getPolynomials()
											.toArray(new PPolynomial[as
													.getPolynomials().size()]),
									substitutions, k, permutation++, true,
									false, as.freeVariables);
							if (eliminationIdeal == null) {
								Log.debug("eliminationIdeal is null");
								return ProofResult.UNKNOWN;
							}
							ndgSet = eliminationIdeal.iterator();
							while (ndgSet.hasNext()) {
								thisNdgSet = ndgSet.next();
								ndg = thisNdgSet.iterator();
								while (ndg.hasNext()) {
									poly = ndg.next();
									if (poly.isZero()) {
										/*
										 * Here we know that the statement is
										 * may be not generally false if we
										 * are working with a maximal independent
										 * set of variables.
										 */
										as.removeThesis();
										int naivDim = as.getFreeVariables()
												.size()
												- substitutions.keySet().size();
										Log.debug(
												"Naive dimension = " + naivDim);
										if (!HilbertDimension.isDimGreaterThan2(
												as, substitutions, naivDim)) {
											Log.debug(
													"Statement is NOT GENERALLY FALSE");
											return ProofResult.TRUE_ON_COMPONENTS;
										}
										if (!investigateNonGeometricMaximalIndependentSet) {
											return ProofResult.UNKNOWN;
										}
										/* Check again if the statement is generally
										 * false by using a maximum independent set
										 * of variables.
										 */
										as.addNegatedThesis();
										eliminationIdeal = PPolynomial.eliminate(
												as.getPolynomials()
														.toArray(new PPolynomial[as
																.getPolynomials().size()]),
												substitutions, k, permutation++, true,
												false, HilbertDimension.getAMaximalSet());
										ndgSet = eliminationIdeal.iterator();
										while (ndgSet.hasNext()) {
											thisNdgSet = ndgSet.next();
											ndg = thisNdgSet.iterator();
											while (ndg.hasNext()) {
												poly = ndg.next();
												if (poly.isZero()) {
													Log.debug(
															"Statement is NOT GENERALLY FALSE");
													return ProofResult.TRUE_ON_COMPONENTS;
												}
											}
										}
										return ProofResult.FALSE;
									}
								}
							}
							/*
							 * End of checking if the statement is not generally
							 * false.
							 */

							if (as.interpretFalseAsUndefined) {
								Log.debug("Interpreting FALSE as UNKNOWN");
								return ProofResult.UNKNOWN;
							}
							return ProofResult.FALSE;
						}

						/*
						 * Here we know that the statement is reported to be
						 * generally true with some NDGs.
						 */
						if (!poly.isConstant()) {
							if (as.interpretTrueAsUndefined) {
								Log.debug("Interpreting TRUE as UNKNOWN");
								return ProofResult.UNKNOWN;
							}
							NDGCondition ndgc = ndgd.detect(poly);
							if (ndgc == null) {
								readable = false;
							} else {
								/*
								 * Check if this elimination ideal equals to
								 * {xM-xN,yM-yN}:
								 */
								xyRewrite = (xyRewrite
										&& thisNdgSet.size() == 1);
								/*
								 * Note that in some cases the CAS may return
								 * (xM-xN)*(-1) which consists of two factors,
								 * so thisNdgSet.size() == 1 will fail. Until
								 * now there is no experience of such behavior
								 * for such simple ideals, so maybe this check
								 * is OK.
								 */
								if (xyRewrite) {
									if (ndgc.getCondition()
											.equals("xAreEqual")) {
										TreeSet<GeoPoint> points = new TreeSet<>();
										points.add(
												(GeoPoint) ndgc.getGeos()[0]);
										points.add(
												(GeoPoint) ndgc.getGeos()[1]);
										xEqualSet.add(points);
									}
									if (ndgc.getCondition()
											.equals("yAreEqual")) {
										TreeSet<GeoPoint> points = new TreeSet<>();
										points.add(
												(GeoPoint) ndgc.getGeos()[0]);
										points.add(
												(GeoPoint) ndgc.getGeos()[1]);
										yEqualSet.add(points);
									}
									if (xEqualSet.size() == 1
											&& xEqualSet.equals(yEqualSet)) {
										/*
										 * If yes, set the condition to
										 * AreEqual(M,N) and readable enough:
										 */
										ndgc.setCondition("AreEqual");
										ndgc.setReadability(0.5);
									}
								}

								ndgcl.add(ndgc);
								score += ndgc.getReadability();
							}
						}
					}
					/*
					 * Now we take the set if the conditions are readable and
					 * the set is the current best. TODO: Here we should
					 * simplify the NDGs, i.e. if one of them is a logical
					 * consequence of others, then it should be eliminated.
					 */
					if (readable && score < bestScore) {
						Log.debug("Found a better NDG score (" + score
								+ ") than " + bestScore);
						bestScore = score;
						bestNdgSet = ndgcl;
						found = true;
					} else {
						if (readable) {
							Log.debug("Not better than previous NDG score ("
									+ bestScore + "), this is " + score);
						} else {
							Log.debug("...unreadable");
						}
					}
				}
				if (found) {
					for (NDGCondition aBestNdgSet : bestNdgSet) {
						prover.addNDGcondition(aBestNdgSet);
					}
				}
			}
			/*
			 * No readable NDGs was found, search for another prover to make a
			 * better job:
			 */
			if (!found) {
				Log.debug("Statement is TRUE but NDGs are UNREADABLE");
				return ProofResult.TRUE_NDG_UNREADABLE;
			}
			/* END OF PROVEDETAILS. */

			/* START OF PROVE. */
		} else {
			ExtendedBoolean solvable = PPolynomial.solvable(
					as.getPolynomials()
							.toArray(new PPolynomial[as.getPolynomials()
									.size()]),
					substitutions, statement.getKernel(),
					proverSettings.transcext, as.freeVariables);
			if (ExtendedBoolean.UNKNOWN.equals(solvable)) {
				/*
				 * Prover returned with no success, search for another prover:
				 */
				Log.debug(
						"Unsuccessful run, statement is UNKNOWN at the moment");
				return ProofResult.UNKNOWN;
			}
			if (solvable.boolVal()) {
				if (!proverSettings.transcext) {
					/*
					 * We cannot reliably tell if the statement is really false:
					 */
					Log.debug(
							"No transcext support, system is solvable, statement is UNKNOWN");
					return ProofResult.UNKNOWN;
				}
				/* Here we know that the statement is not generally true. */
				Log.debug("Statement is NOT GENERALLY TRUE");

				/*
				 * It is possible that the statement is not generally false,
				 * either.
				 */
				as.removeThesis();
				as.addNegatedThesis();
				solvable = PPolynomial
						.solvable(
								as.getPolynomials()
										.toArray(new PPolynomial[as
												.getPolynomials().size()]),
								substitutions, statement.getKernel(),
								proverSettings.transcext, as.freeVariables);
				if (ExtendedBoolean.UNKNOWN.equals(solvable)) {
					/*
					 * Prover returned with no success, search for another
					 * prover:
					 */
					Log.debug(
							"Unsuccessful run on negated statement, statement is UNKNOWN at the moment");
					return ProofResult.UNKNOWN;
				}
				if (solvable.boolVal()) {
					/*
					 * Here we know that the statement is not generally false.
					 */
					as.removeThesis();
					int naivDim = as.getFreeVariables().size()
							- substitutions.keySet().size();
					Log.debug("Naive dimension = " + naivDim);
					if (!HilbertDimension.isDimGreaterThan2(as, substitutions,
							naivDim)) {
						Log.debug("Statement is NOT GENERALLY FALSE");
						return ProofResult.TRUE_ON_COMPONENTS;
					}
					if (!investigateNonGeometricMaximalIndependentSet) {
						return ProofResult.UNKNOWN;
					}
					/*
					 * Check again if the statement is generally false by using
					 * a maximum independent set of variables.
					 */
					as.addNegatedThesis();
					solvable = PPolynomial.solvable(
							as.getPolynomials()
									.toArray(new PPolynomial[as.getPolynomials()
											.size()]),
							substitutions, statement.getKernel(),
							proverSettings.transcext,
							HilbertDimension.getAMaximalSet());
					if (solvable.boolVal()) {
						Log.debug("Statement is NOT GENERALLY FALSE");
						return ProofResult.TRUE_ON_COMPONENTS;
					}
					return ProofResult.FALSE;
				}
				/* End of checking if the statement is not generally false. */

				if (as.interpretFalseAsUndefined
						&& !as.interpretTrueAsUndefined) {
					Log.debug("Interpreting FALSE as UNKNOWN");
					return ProofResult.UNKNOWN;
				}
				return ProofResult.FALSE;
			}
		}
		if (as.interpretTrueAsUndefined) {
			Log.debug("Interpreting TRUE as UNKNOWN");
			return ProofResult.UNKNOWN;
		}
		Log.debug("Statement is GENERALLY TRUE");
		return ProofResult.TRUE;
	}

	/**
	 * Create algebraic equations of the construction to prepare computing a
	 * locus or envelope equation.
	 *
	 * @param tracer     the locus point
	 * @param mover      the moving point
	 * @param implicit   if the locus equation is implicit
	 * @param callerAlgo the caller Algo
	 * @return the object which describes the construction algebraically
	 */
	public static AlgebraicStatement translateConstructionAlgebraically(
			GeoElement tracer, GeoElement mover, boolean implicit,
			AlgoElement callerAlgo) {
		Prover p = UtilFactory.getPrototype().newProver();
		p.setProverEngine(implicit ? ProverEngine.LOCUS_IMPLICIT
				: ProverEngine.LOCUS_EXPLICIT);
		AlgebraicStatement as = new AlgebraicStatement(tracer, mover, p, false);
		ProofResult proofresult = as.getResult();
		if (proofresult == ProofResult.PROCESSING
				|| proofresult == ProofResult.UNKNOWN) {
			/*
			 * Don't do further computations until CAS is ready or there were
			 * unimplemented algos or some other issues:
			 */
			Log.debug("Cannot compute implicit curve: " + proofresult);
			return null;
		}

		as.substitutions = new TreeMap<>();
		TreeSet<GeoElement> freePoints = ProverBotanasMethod
				.getLocusFreePoints(tracer);
		if (!implicit) {
			freePoints.add(tracer);
		}
		if (!freePoints.contains(mover)) {
			freePoints.add(mover);
		}

		/* axis and fixed slope line support */
		Kernel k = mover.getKernel();
		for (GeoElement geo : (tracer).getAllPredecessors()) {
			if (geo instanceof GeoLine && ((GeoLine) geo).hasFixedSlope()) {

				PVariable[] vars;
				try {
					vars = ((SymbolicParametersBotanaAlgo) geo)
							.getBotanaVars(geo);
				} catch (NoSymbolicParametersException e) {
					Log.debug("Cannot get Botana variables for " + geo);
					return null;
				}

				GeoLine l = (GeoLine) geo;

				/*
				 * a0/a1*x+b0/b1*y+c0/c1=0, that is:
				 * a0*b1*c1*x+a1*b0*c1*y+a1*b1*c0=0
				 */
				Coords P = l.getCoords();
				BigInteger[] a = k.doubleToRational(P.get(1));
				BigInteger[] b = k.doubleToRational(P.get(2));
				BigInteger[] c = k.doubleToRational(P.get(3));

				// Setting up two equations for the two points:
				PPolynomial a0 = new PPolynomial(a[0]);
				PPolynomial a1 = new PPolynomial(a[1]);
				PPolynomial b0 = new PPolynomial(b[0]);
				PPolynomial b1 = new PPolynomial(b[1]);
				PPolynomial c0 = new PPolynomial(c[0]);
				PPolynomial c1 = new PPolynomial(c[1]);
				PPolynomial xp = new PPolynomial(vars[0]);
				PPolynomial yp = new PPolynomial(vars[1]);
				PPolynomial xq = new PPolynomial(vars[2]);
				PPolynomial yq = new PPolynomial(vars[3]);

				PPolynomial ph = a0.multiply(b1).multiply(c1).multiply(xp)
						.add(a1.multiply(b0).multiply(c1).multiply(yp))
						.add(a1.multiply(b1).multiply(c0));
				as.addPolynomial(ph);
				Log.debug("Extra poly 1 for " + l.getLabelSimple() + ": " + ph);
				ph = a0.multiply(b1).multiply(c1).multiply(xq)
						.add(a1.multiply(b0).multiply(c1).multiply(yq))
						.add(a1.multiply(b1).multiply(c0));
				as.addPolynomial(ph);
				Log.debug("Extra poly 2 for " + l.getLabelSimple() + ": " + ph);

				if (!(a[0].equals(BigInteger.ZERO))) {
					/*
					 * This equation is not horizontal, so y can be arbitrarily
					 * chosen. Let's choose y=0 and y=1 for the 2 points.
					 */
					ph = yp;
					as.addPolynomial(ph);
					Log.debug("Extra poly 3 for " + l.getLabelSimple() + ": "
							+ ph);
					ph = yq.subtract(new PPolynomial(BigInteger.ONE));
					Log.debug("Extra poly 4 for " + l.getLabelSimple() + ": "
							+ ph);
					as.addPolynomial(ph);
				} else {
					/*
					 * This equation is horizontal, so x can be arbitrarily
					 * chosen. Let's choose x=0 and x=1 for the 2 points.
					 */
					ph = xp;
					as.addPolynomial(ph);
					Log.debug("Extra poly 3 for " + l.getLabelSimple() + ": "
							+ ph);
					ph = xq.subtract(new PPolynomial(BigInteger.ONE));
					as.addPolynomial(ph);
					Log.debug("Extra poly 4 for " + l.getLabelSimple() + ": "
							+ ph);
				}
				// These coordinates are no longer free.
				for (int i = 0; i < 4; i++) {
					as.freeVariables.remove(vars[i]);
				}
			}
			AlgoElement algo = geo.getParentAlgorithm();
			boolean condition;
			condition = implicit || geo != tracer;
			if (condition && algo instanceof AlgoPointOnPath) {
				/*
				 * We need to add handle all points which are on a path like
				 * free points (that is, substitution of their coordinates will
				 * be performed later), unless this point is the locus point.
				 */
				if (!freePoints.contains(geo)) {
					freePoints.add(geo);
				}
			}
		}

		PVariable[] moverVars;
		try {
			moverVars = ((SymbolicParametersBotanaAlgo) mover).getBotanaVars(mover);
		} catch (NoSymbolicParametersException e) {
			Log.debug("Cannot get Botana variables for " + mover);
			return null;
		}

		boolean autoNdg = false;
		SingularWebService singularWS = mover.getConstruction()
				.getApplication().getSingularWS();
		if (singularWS == null || (!singularWS.isAvailable())) {
			autoNdg = true;
		}

		/* Create mover direct dependencies for Pech's idea (see below).
		 * This set contains all points that should be avoided to
		 * coincide with the mover.
		 */
		TreeSet<GeoElementND> moverDirectDependencies = new TreeSet<>();
		if (autoNdg && !implicit) {
			AlgoPointOnPath apop = (AlgoPointOnPath) mover.getParentAlgorithm();
			GeoElement i0 = apop.input[0];
			if (i0 instanceof GeoLine) {
				GeoLine gl = (GeoLine) i0;
				// End points of the line path of the mover should be avoided.
				if (gl.startPoint != null) {
					moverDirectDependencies.add(gl.startPoint);
				}
				if (gl.endPoint != null) {
					moverDirectDependencies.add(gl.endPoint);
				}
			} else if (i0 instanceof GeoConic && ((GeoConic) i0).isCircle()) {
				GeoConic gc = (GeoConic) i0;
				if (gc.isCircle()) {
					// Circumpoints of the circular path may be considered to avoid.
					for (GeoElementND ge : gc.getPointsOnConic()) {
						if (!ge.isEqual(mover)) {
							// Consider only those points that play role in
							// building a tangent to the circle.
							// TODO: This reads all geos, we need only the related ones:
							for (GeoElement ge2 : tracer.getConstruction().getGeoSetLabelOrder()) {
								if (ge2 instanceof GeoLine) {
									GeoElement[] input = ge2.getParentAlgorithm().input;
									GeoElement sp = input[0];
									GeoElement ep = input[1];
									if ((sp.equals(ge) && ep.equals(mover)) ||
											(ep.equals(ge) && sp.equals(mover))) {
										moverDirectDependencies.add(ge);
									}
								}
							}
						}
					}
				}
			}
			Log.debug("Direct dependencies of the mover = " + moverDirectDependencies);
		}

		/* free point support */
		/*
		 * Note that sometimes free points can be on a path, but they are
		 * considered free if they are not changed while the mover moves.
		 */
		for (GeoElement freePoint : freePoints) {
			freePoint.addToUpdateSetOnly(callerAlgo);
			PVariable[] vars;
			try {
				vars = ((SymbolicParametersBotanaAlgo) freePoint)
						.getBotanaVars(freePoint);
			} catch (NoSymbolicParametersException e1) {
				Log.debug("Cannot get Botana variables for " + freePoint);
				return null;
			}

			boolean condition = !mover.equals(freePoint);

			if (!implicit) {
				condition &= !tracer.equals(freePoint);
			}

			if (autoNdg && condition
					&& moverDirectDependencies.contains(freePoint)
					&& vars != null) {
				/* add non-degeneracy condition for the points to be avoided (Pech's idea) */
				PPolynomial v = new PPolynomial(new PVariable(k));
				PPolynomial ndg = PPolynomial.sqrDistance(moverVars[0], moverVars[1], vars[0], vars[1]).multiply(v).
						subtract(new PPolynomial(1));
				as.addPolynomial(ndg);
			}

			if (condition) {
				boolean createX = true;
				boolean createY = true;
				AlgoElement ae = freePoint.getParentAlgorithm();
				/*
				 * If this "free" point is on a path, then its path may be
				 * important to be kept as a symbolic object for consistency.
				 * Let's do that if the path is linear.
				 */
				if (ae instanceof AlgoPointOnPath) {
					if (ae.input[0] instanceof GeoLine) {
						PPolynomial[] symPolys;
						try {
							symPolys = ((SymbolicParametersBotanaAlgo) freePoint)
									.getBotanaPolynomials(freePoint);
						} catch (NoSymbolicParametersException e) {
							Log.debug(
									"An error occured during obtaining symbolic parameters");
							return null;
						}
						int i = 1;
						for (PPolynomial symPoly : symPolys) {
							as.addPolynomial(symPoly);
							Log.debug("Extra symbolic poly " + i + " for "
									+ freePoint.getLabelSimple() + ": " + symPoly);
						}
						double[] dir = new double[2];
						((GeoLine) ae.input[0]).getDirection(dir);
						if (dir[0] == 0.0) {
							/* vertical */
							if (vars != null) {
								as.freeVariables.remove(vars[0]);
								as.freeVariables.add(vars[1]);
							} else {
								// If vars == null, don't try to create
								// any other objects that depend on it:
								createY = false;
							}
							createX = false;
						} else {
							/* horizontal */
							if (vars != null) {
								as.freeVariables.add(vars[0]);
								as.freeVariables.remove(vars[1]);
							} else {
								// If vars == null, don't try to create
								// any other objects that depend on it:
								createX = false;
							}
							createY = false;
						}
					} else {
						// non-linear path
						if (implicit) {
							/*
							 * If the path is not linear, but we are computing
							 * implicit locus then the condition may need to
							 * have the symbolic object for consistency.
							 */
							GeoElement input = ae.input[0];
							/*
							 * In some cases it is useful do something similar
							 * like for linear path for the more complicated
							 * curves also, for example, for circles. (Other
							 * cases might also be implemented, e.g. other types
							 * of conics.) That is, we keep the symbolic
							 * equation, but fix one of the coordinates of the
							 * point on the path. Sometimes it is useful to
							 * carefully decide which coordinate should be
							 * fixed.
							 */
							if (input instanceof GeoConic
									&& ((GeoConic) input).isCircle()) {
								GeoConic gc = (GeoConic) input;
								Coords co = gc.getMidpoint();
								Coords cp = ((GeoPoint) freePoint).getCoords();
								if (co.get(3) == 1.0 && cp.get(3) == 1.0
										&& DoubleUtil.isEqual(co.get(1),
										cp.get(1))) {
									/*
									 * first coordinates are equal, so the
									 * radius is vertical
									 */
									as.freeVariables.remove(vars[0]);
									as.freeVariables.add(vars[1]);
									createX = false;
								} else {
									/*
									 * use the other coordinate for other
									 * circles
									 */
									as.freeVariables.remove(vars[1]);
									as.freeVariables.add(vars[0]);
									createY = false;
								}
								// Log.debug(co + " " + cp);
							} else {
								/* ad hoc selection for non-circle conics */
								as.freeVariables.remove(vars[0]);
								as.freeVariables.add(vars[1]);
								createX = false;
							}
						}
					}
				}
				if (createX && createY) {
					/*
					 * Remove any other polynomials which define other
					 * constraints for this point. This is necessary because we
					 * can obtain contradiction if the other (symbolic)
					 * constraints are slightly different than this inaccurate
					 * numerical one.
					 */
					if (as.getGeoPolys(freePoint) != null) {
						Log.debug("Removing other constraints for "
								+ freePoint.getLabelSimple());
						as.removeGeoPolys(freePoint);
					}
				}
				BigInteger[] q = new BigInteger[2]; // P and Q for P/Q
				if (createX) {
					double x = ((GeoPoint) freePoint).getInhomX();
					/*
					 * Use the fraction P/Q according to the current kernel
					 * setting. We use the P/Q=x <=> P-Q*x=0 equation.
					 */
					q = k.doubleToRational(x);

					as.freeVariables.remove(vars[0]);
					PPolynomial ph = new PPolynomial(q[0])
							.subtract(new PPolynomial(vars[0])
									.multiply(new PPolynomial(q[1])));
					as.addPolynomial(ph);
					Log.debug("Extra poly for x of "
							+ freePoint.getLabelSimple() + ": " + ph);
				}
				if (createY) {
					double y = ((GeoPoint) freePoint).getInhomY();
					/*
					 * Use the fraction P/Q according to the current kernel
					 * setting. We use the P/Q=x <=> P-Q*x=0 equation.
					 */
					q = k.doubleToRational(y);

					as.freeVariables.remove(vars[1]);
					PPolynomial ph = new PPolynomial(q[0])
							.subtract(new PPolynomial(vars[1])
									.multiply(new PPolynomial(q[1])));
					as.addPolynomial(ph);
					Log.debug("Extra poly for y of "
							+ freePoint.getLabelSimple() + ": " + ph);
				}
			} else {
				condition = true;
				if (!implicit) {
					condition = tracer.equals(freePoint);
				}
				if (condition) {
					as.freeVariables.add(vars[0]);
					as.freeVariables.add(vars[1]);
					as.curveVars = vars;
				} else {
					as.freeVariables.remove(vars[0]);
					as.freeVariables.remove(vars[1]);
				}
			}
		}
		as.computeStrings();
		return as;
	}

	public static String paramLookup(String[] haystack, String needle) {
		for (String h : haystack) {
			if (h.startsWith(needle + "=")) {
				String s = h.substring(needle.length() + 1);
				// Convert the data back.
				s = s.replace("≥", ">=").replace("≤", "<=")
						.replace("E", "=");
				return s;
			}
		}
		return "";
	}

}