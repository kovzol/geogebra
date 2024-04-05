package org.geogebra.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.RelationNumerical;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoDependentBoolean;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoJoinPoints;
import org.geogebra.common.kernel.algos.AlgoJoinPointsSegment;
import org.geogebra.common.kernel.algos.AlgoPointInRegion;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.algos.AlgoPolygonRegular;
import org.geogebra.common.kernel.geos.GProperty;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.prover.AbstractProverReciosMethod;
import org.geogebra.common.kernel.prover.ProverBotanasMethod;
import org.geogebra.common.kernel.prover.ProverPureSymbolicMethod;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.scripting.CmdShowProof;
import org.geogebra.common.main.Localization;
import org.geogebra.common.plugin.EuclidianStyleConstants;
import org.geogebra.common.util.debug.Log;

import com.himamis.retex.editor.share.util.Unicode;

/**
 * Prover package for GeoGebra. Allows using multiple backends for theorem
 * proving.
 */

public abstract class Prover {

	/**
	 * Enum list of supported prover backends for GeoGebra
	 *
	 * @author Zoltan Kovacs
	 *
	 */
	public enum ProverEngine {
		/**
		 * Tomas Recio's method
		 */
		RECIOS_PROVER,
		/**
		 * Francisco Botana's method
		 */
		BOTANAS_PROVER,
		/**
		 * OpenGeoProver (http://code.google.com/p/open-geo-prover/), Wu's
		 * method
		 */
		OPENGEOPROVER_WU,
		/**
		 * OpenGeoProver, Area method
		 */
		OPENGEOPROVER_AREA,
		/**
		 * pure symbolic prover (every object is calculated symbolically, also
		 * the statements)
		 */
		PURE_SYMBOLIC_PROVER,
		/**
		 * default prover (GeoGebra decides internally)
		 */
		AUTO,
		/**
		 * not a theorem prover, but an implicit locus calculator
		 */
		LOCUS_IMPLICIT,
		/**
		 * not a theorem prover, but an explicit locus calculator
		 */
		LOCUS_EXPLICIT,
		/* not a theorem prover, but an envelope calculator */
		ENVELOPE
	}

	/**
	 * Possible results of an attempted proof
	 *
	 * @author Zoltan Kovacs
	 *
	 */
	public enum ProofResult {
		/**
		 * The proof is completed, the statement is generally true (maybe with
		 * some NDG conditions).
		 */
		TRUE,
		/**
		 * The proof is completed, the statement is generally true (with some
		 * NDG conditions) but no readable NDGs were found.
		 */
		TRUE_NDG_UNREADABLE,
		/**
		 * The proof is completed. The statement is neither generally true, nor
		 * generally false. That is, it is true on a component, but not true on
		 * all components in the algebraic geometry context.
		 */
		TRUE_ON_COMPONENTS,
		/**
		 * The proof is completed, the statement is generally false.
		 */
		FALSE,
		/**
		 * The statement cannot be proved by using the current backend within
		 * the given timeout.
		 */
		UNKNOWN,
		/**
		 * "?", usually from giac.js --- processing in progress
		 */
		PROCESSING
	}

	/**
	 * Maximal time to be spent in the prover subsystem
	 */
	/* input */
	private int timeout = 5;
	private ProverEngine engine = ProverEngine.AUTO;

	private Construction construction;
	/**
	 * The statement to be prove
	 */
	protected GeoElement statement;

	/**
	 * Recio's prover.
	 */
	protected AbstractProverReciosMethod reciosProver;

	/**
	 * The result of the proof
	 */
	protected ProofResult result;

	/**
	 * Should the prover return extra NDG conditions? If not, some computation
	 * time may be saved.
	 */
	private boolean returnExtraNDGs;
	private List<ProverEngine> proveAutoOrder;
	private List<ProverEngine> proveDetailsAutoOrder;
	private boolean showproof = false;
	private String proof = "";

	public boolean getShowproof() {
		return showproof;
	}

	public void setShowproof(boolean set) {
		showproof = set;
	}

	public String getProof() {
		return proof;
	}

	public void addProofLine(char type, String line) {
		proof += type + line + "\n";
	}

	public void addProofLine(String line) {
		proof += CmdShowProof.TEXT + line + "\n";
	}

	/**
	 * An object which contains a condition description (e.g. "AreCollinear")
	 * and an ordered list of GeoElement's (e.g. A, B, C)
	 *
	 * @author Zoltan Kovacs
	 */
	public static class NDGCondition implements Comparable {
		/**
		 * The condition String
		 */
		String condition;

		/**
		 * How human readable is this condition? The lower the better. This
		 * number is always >= 0;
		 */
		double readability = 1.0;

		public PPolynomial[] getPolys() {
			return polys;
		}

		public void setPolys(PPolynomial[] polys) {
			this.polys = polys;
		}

		/**
		 * The polynomials that explain this condition (optional).
		 */
		PPolynomial[] polys;

		/**
		 * Array of GeoElements (parameters of the condition)
		 */
		GeoElement[] geos;

		/**
		 * Gets readability score for this NDG condition.
		 *
		 * @return score
		 */
		public double getReadability() {
			return readability;
		}

		/**
		 * Sets readability score for this NDG condition.
		 *
		 * @param readability
		 *            score
		 */
		public void setReadability(double readability) {
			this.readability = readability;
		}

		/**
		 * A short textual description of the condition
		 *
		 * @return the condition
		 */
		public String getCondition() {
			return condition;
		}

		/**
		 * Sets a condition text
		 *
		 * @param condition
		 *            the text, e.g. "AreCollinear"
		 */
		public void setCondition(String condition) {
			this.condition = condition;
		}

		/**
		 * Returns the GeoElements for a given condition
		 *
		 * @return the array of GeoElements
		 */
		public GeoElement[] getGeos() {
			return geos;
		}

		/**
		 * Sets the GeoElements for a given condition
		 *
		 * @param object
		 *            the array of GeoElements
		 */
		public void setGeos(GeoElement[] object) {
			this.geos = object;
		}

		public StringBuilder explain(Localization loc) {
			String cond = getCondition();
			StringBuilder s = new StringBuilder();
			if ("AreParallel".equals(cond)) {
				// non-parallism in 2D means intersecting
				// FIXME: this is not true for 3D
				s = sb(RelationNumerical.intersectString(
						getGeos()[0], getGeos()[1],
						true, loc));
			} else if ("AreCollinear".equals(cond)) {
				s = sb(RelationNumerical
						.triangleNonDegenerateString(
								(GeoPoint) getGeos()[0],
								(GeoPoint) getGeos()[1],
								(GeoPoint) getGeos()[2],
								loc));
			} else if ("AreEqual".equals(cond)) {
				s = sb(RelationNumerical.equalityString(
						getGeos()[0], getGeos()[1],
						false, loc));
			} else if ("ArePerpendicular".equals(cond)) {
				s = sb(RelationNumerical.perpendicularString(
						(GeoLine) getGeos()[0],
						(GeoLine) getGeos()[1], false,
						loc));
			} else if ("AreCongruent".equals(cond)) {
				s = sb(RelationNumerical.congruentSegmentString(
						getGeos()[0], getGeos()[1],
						false, loc));
			}
			return s;
		}


		/**
		 * Should this condition be used in the Discover command?
		 */
		static boolean discovery = false;

		private static GeoLine line(GeoPoint P1, GeoPoint P2,
				Construction cons) {
			TreeSet<GeoElement> ges = cons.getGeoSetConstructionOrder();
			Iterator<GeoElement> it = ges.iterator();
			// TODO: Maybe there is a better way here to lookup the appropriate
			// line
			// if it already exists (by using kernel).
			while (it.hasNext()) {
				GeoElement ge = it.next();
				if (ge instanceof GeoLine) {
					GeoPoint Q1 = ((GeoLine) ge).getStartPoint();
					GeoPoint Q2 = ((GeoLine) ge).getEndPoint();
					if ((Q1 != null && Q2 != null)
							&& ((Q1.equals(P1) && Q2.equals(P2))
							|| (Q1.equals(P2) && Q2.equals(P1)))) {
						return (GeoLine) ge;
					}
				}
			}
			// If there is no such line, we simply create one.
			boolean oldMacroMode = cons.isSuppressLabelsActive();
			cons.setSuppressLabelCreation(false);
			AlgoJoinPoints ajp = new AlgoJoinPoints(cons, null, P1, P2);
			GeoLine line = ajp.getLine();
			if (!discovery) {
				line.setEuclidianVisible(true);
				line.setLineType(EuclidianStyleConstants.LINE_TYPE_DASHED_LONG);
				line.setLabelVisible(true);
				line.updateVisualStyle(GProperty.COMBINED); // visibility and style
			} else {
				line.setEuclidianVisible(false);
			}

			cons.setSuppressLabelCreation(oldMacroMode);
			return line;
		}

		/* TODO: Unify this code with line(). */
		private static GeoSegment segment(GeoPoint P1, GeoPoint P2,
				Construction cons) {
			TreeSet<GeoElement> ges = cons.getGeoSetConstructionOrder();
			Iterator<GeoElement> it = ges.iterator();
			// TODO: Maybe there is a better way here to lookup the appropriate
			// line
			// if it already exists (by using kernel).
			while (it.hasNext()) {
				GeoElement ge = it.next();
				if (ge instanceof GeoSegment) {
					GeoPoint Q1 = ((GeoSegment) ge).getStartPoint();
					GeoPoint Q2 = ((GeoSegment) ge).getEndPoint();
					if ((Q1 != null && Q2 != null)
							&& ((Q1.equals(P1) && Q2.equals(P2))
							|| (Q1.equals(P2) && Q2.equals(P1)))) {
						return (GeoSegment) ge;
					}
				}
			}
			// If there is no such line, we simply create one.
			boolean oldMacroMode = cons.isSuppressLabelsActive();
			cons.setSuppressLabelCreation(false);
			AlgoJoinPointsSegment ajp = new AlgoJoinPointsSegment(cons, null,
					P1, P2);
			GeoSegment segment = ajp.getSegment();
			if (!discovery) {
				segment.setEuclidianVisible(true);
				segment.setLineType(EuclidianStyleConstants.LINE_TYPE_DASHED_LONG);
				segment.setLabelVisible(true);
				segment.updateVisualStyle(GProperty.COMBINED);
			} else {
				segment.setEuclidianVisible(false);
				segment.updateVisualStyle(GProperty.COMBINED);
			}
			cons.setSuppressLabelCreation(oldMacroMode);
			return segment;
		}

		private void sortGeos() {
			// We need this because geos are sorted in the order of creation.
			Arrays.sort(geos, new Comparator<GeoElement>() {
				@Override
				public int compare(GeoElement g1, GeoElement g2) {
					return g1.getLabelSimple().compareTo(g2.getLabelSimple());
				}
			});
		}

		/**
		 * Rewrites the NDG to a simpler form.
		 *
		 * @param cons
		 *            the current construction
		 */
		public void rewrite(Construction cons, boolean d) {
			discovery = d;
			String cond = this.getCondition();
			if ("AreCollinear".equals(cond)) {
				sortGeos();
			} else if ("ArePerpendicular".equals(cond)
					&& this.geos.length == 3) {
				// ArePerpendicular[Line[P1,P3],Line[P3,P2]].
				GeoPoint P1 = (GeoPoint) this.geos[0];
				GeoPoint P2 = (GeoPoint) this.geos[1];
				GeoPoint P3 = (GeoPoint) this.geos[2];
				GeoLine l1 = line(P1, P3, cons);
				GeoLine l2 = line(P3, P2, cons);
				if (l1 != null && l2 != null) {
					geos = new GeoElement[2];
					geos[0] = l1;
					geos[1] = l2;
					sortGeos();
				}
			} else if ("AreEqual".equals(cond)
					|| "ArePerpendicular".equals(cond)
					|| "AreParallel".equals(cond)
					|| "AreCongruent".equals(cond)) {
				if (this.geos.length == 4) {
					// This is an AreEqual[P1,P2,P3,P4]-like condition.
					// We should try to rewrite it to
					// AreEqual[Line[P1,P2],Line[P3,P4]].
					GeoPoint P1 = (GeoPoint) this.geos[0];
					GeoPoint P2 = (GeoPoint) this.geos[1];
					GeoPoint P3 = (GeoPoint) this.geos[2];
					GeoPoint P4 = (GeoPoint) this.geos[3];
					// Maybe AreEqual should be here, too. TODO: Check...
					if ("AreCongruent".equals(cond)) {
						GeoSegment s1 = segment(P1, P2, cons);
						GeoSegment s2 = segment(P3, P4, cons);
						if (s1 != null && s2 != null) {
							geos = new GeoElement[2];
							geos[0] = s1;
							geos[1] = s2;
							sortGeos();
						}
					} else {
						GeoLine l1 = line(P1, P2, cons);
						GeoLine l2 = line(P3, P4, cons);
						if (l1 != null && l2 != null) {
							geos = new GeoElement[2];
							geos[0] = l1;
							geos[1] = l2;
							sortGeos();
						}
					}
				} else if (this.geos.length == 2) {
					// This is an AreEqual[l1,l2]-like condition.
					// We should sort l1 and l2.
					sortGeos();
					// Unsure if this is called at all.
				}
			}
		}

		private static StringBuilder sb(String content) {
			return content == null ? null : new StringBuilder(content);
		}

		public String toString(Localization loc) {
			StringBuilder s;
			if (geos == null) { // formula with quantities
				s = sb(condition);
			} else {
				if (loc == null) {
					s = sb(condition);
				} else {
					s = sb(loc.getCommand(condition));
				}
				s.append("(");
				for (int i = 0; i < geos.length; ++i) {
					if (i > 0) {
						s.append(',');
					}
					/*
					 * There can be a case when the underlying
					 * prover sends such objects which cannot be
					 * understood by GeoGebra. In this case we
					 * use the "Objects" word. In this case we
					 * normally return ProveResult.UNKNOWN to
					 * not confuse the student, but for sure, we
					 * still do the check here as well.
					 */
					GeoElement geo = geos[i];
					if (geo != null) {
						s.append(geos[i]
								.getLabelSimple());
					} else {
						s.append(Unicode.ELLIPSIS);
					}
				}
				s.append(")");
			}
			return s.toString();
		}

		@Override
		public String toString() {
			return toString(null);
		}

		@Override
		public int compareTo(Object o) {
			double r = this.readability - ((NDGCondition) o).readability;
			if (r != 0) {
				return (int) Math.signum(r);
			}
			int c = this.condition.compareTo(((NDGCondition) o).condition);
			if (c != 0) {
				return c;
			}
			int s = this.toString().compareTo(((NDGCondition) o).toString());
			if (s != 0) {
				return s;
			}
			return 0; // they should be equal...
		}
	}

	/* output */
	private TreeSet<NDGCondition> ndgConditions = new TreeSet<>();

	/**
	 * Constructor for the package.
	 */
	public Prover() {
		proveAutoOrder = new ArrayList<>();
		// Order of Prove[] for the AUTO prover:
		// Recio is the fastest.
		proveAutoOrder.add(ProverEngine.RECIOS_PROVER);
		// Botana's prover is also fast for general problems.
		proveAutoOrder.add(ProverEngine.BOTANAS_PROVER);
		// Wu may be a bit slower.
		// proveAutoOrder.add(ProverEngine.OPENGEOPROVER_WU);
		// There are some problems with the communication between GeoGebra and OGP,
		// so this is disabled for now.
		// Area method is not polished yet, thus it's disabled:
		// proveAutoOrder.add(ProverEngine.OPENGEOPROVER_AREA);

		// Order of ProveDetails[] for the AUTO prover:
		proveDetailsAutoOrder = new ArrayList<>();
		// Botana's prover based on elimination (with no presumed NDGs) gives
		// the shortest conditions, best for educational use.
		proveDetailsAutoOrder.add(ProverEngine.BOTANAS_PROVER);
		// Wu's method does the most general good job.
		// proveDetailsAutoOrder.add(ProverEngine.OPENGEOPROVER_WU);
		// There are some problems with the communication between GeoGebra and OGP,
		// so this is disabled for now.
		// Recio does not give NDGs:
		// proveDetailsAutoOrder.add(ProverEngine.RECIOS_PROVER);
		// Area method is buggy at the moment, needs Damien's fixes.
		// It returns {true} always at the moment, not useful.
		// proveDetailsAutoOrder.add(ProverEngine.OPENGEOPROVER_AREA);
	}

	/**
	 * Gives the current statement to prove
	 *
	 * @return the statement (usually a GeoBoolean)
	 */
	public GeoElement getStatement() {
		return statement;
	}

	/**
	 * Sets the maximal time spent in the Prover for the given proof.
	 *
	 * @param timeout
	 *            The timeout in seconds
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Sets the maximal time spent in the Prover for the given proof.
	 *
	 * @return The timeout in seconds
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sets the prover engine.
	 *
	 * @param engine
	 *            The engine subsystem
	 */
	public void setProverEngine(ProverEngine engine) {
		this.engine = engine;
	}

	/**
	 * Gets the prover engine.
	 *
	 * @return the engine subsystem
	 */
	public ProverEngine getProverEngine() {
		return this.engine;
	}

	/**
	 * Sets the GeoGebra construction as the set of the used objects in the
	 * proof.
	 *
	 * @param construction
	 *            The GeoGebra construction
	 */
	public void setConstruction(Construction construction) {
		this.construction = construction;
	}

	/**
	 * Sets the statement to be proven.
	 *
	 * @param root
	 *            The statement to be proven
	 */
	public void setStatement(GeoElement root) {
		this.statement = root;
	}

	/**
	 * The real computation of decision of a statement. The statement is
	 * forwarded to an engine (or more engines).
	 */
	public void decideStatement() {
		// Step 1: Checking if the statement is null.
		if (statement == null) {
			Log.error("No statement to prove");
			result = ProofResult.UNKNOWN;
			return;
		}

		// Step 2:
		// Maybe an already computed value is asked to be proven, e.g.
		// Prove[1==1], i.e. Prove[true]
		AlgoElement algoParent = statement.getParentAlgorithm();
		if (algoParent == null) {
			if (statement.getValueForInputBar().equals("true")) {
				result = ProofResult.TRUE; // Trust in kernel's wisdom
			} else if (statement.getValueForInputBar().equals("false")) {
				result = ProofResult.FALSE; // Trust in kernel's wisdom
			}
			else {
				result = ProofResult.UNKNOWN; // Not sure if this is executed at
			}
			// all, but for sure.
			return;
		}

		StatementFeatures.init(statement);

		// Step 3: Non-AUTO provers
		if (engine != ProverEngine.AUTO) {
			callEngine(engine);
			return;
		}

		// Step 4: AUTO prover
		Log.debug("Using " + engine);
		Iterator<ProverEngine> it;
		if (isReturnExtraNDGs()) {
			it = proveDetailsAutoOrder.iterator();
		} else {
			it = proveAutoOrder.iterator();
		}
		result = ProofResult.UNKNOWN;
		while ((result == ProofResult.UNKNOWN
				|| result == ProofResult.TRUE_NDG_UNREADABLE)
				&& it.hasNext()) {
			ProverEngine pe = it.next();
			if (pe == ProverEngine.OPENGEOPROVER_WU
					|| pe == ProverEngine.OPENGEOPROVER_AREA) {
				/*
				 * Checking if OGP is capable of working on this statement
				 * properly or not.
				 */
				AlgoElement ae = statement.getParentAlgorithm();
				if (ae instanceof AlgoDependentBoolean) {
					/* see triangle-midsegment6 */
					Log.debug(
							"OGP cannot safely check expressions, OGP will be ignored");
					continue; /* try the next prover */
				}
			}
			callEngine(pe);
		}
	}

	/**
	 * A helper method to override the last found proof result with the new one,
	 * if the new one is not unknown, or if the result is null yet, then we
	 * prefer the unknown result.
	 *
	 * @param pr
	 *            the new result
	 * @return decision which result is better
	 */
	private ProofResult override(ProofResult pr) {
		if (result == null || pr != ProofResult.UNKNOWN) {
			return pr;
		}
		return result;
	}

	private void callEngine(ProverEngine currentEngine) {
		Log.debug("Using " + currentEngine);
		ndgConditions = new TreeSet<>(); // reset
		if (currentEngine == ProverEngine.BOTANAS_PROVER) {
			ProverBotanasMethod pbm = new ProverBotanasMethod();
			result = override(pbm.prove(this));
			return;
		} else if (currentEngine == ProverEngine.RECIOS_PROVER) {
			result = override(getReciosProver().prove(this));
			return;
		} else if (currentEngine == ProverEngine.PURE_SYMBOLIC_PROVER) {
			result = override(ProverPureSymbolicMethod.prove(this));
			return;
		} else if (currentEngine == ProverEngine.OPENGEOPROVER_WU
				|| currentEngine == ProverEngine.OPENGEOPROVER_AREA) {
			result = override(openGeoProver(currentEngine));
			return;
		}

	}

	/**
	 * Gets non-degeneracy conditions of the current proof.
	 *
	 * @return The XML output string of the NDG condition
	 */
	public TreeSet<NDGCondition> getNDGConditions() {
		return ndgConditions;
	}

	/**
	 * Gets the proof result
	 *
	 * @return The result (TRUE, FALSE or UNKNOWN)
	 */
	public ProofResult getProofResult() {
		return result;
	}

	/**
	 * If the result of the proof can be expressed by a boolean value, then it
	 * returns that value.
	 *
	 * @return The result of the proof (true, false or null)
	 */
	public ExtendedBoolean getYesNoAnswer() {
		if (result != null) {
			if (result == Prover.ProofResult.TRUE
					|| result == Prover.ProofResult.TRUE_NDG_UNREADABLE
					|| result == Prover.ProofResult.TRUE_ON_COMPONENTS) {
				return ExtendedBoolean.TRUE;
			}
			if (result == Prover.ProofResult.FALSE) {
				return ExtendedBoolean.FALSE;
			}
		}
		return ExtendedBoolean.UNKNOWN;
	}

	/**
	 * A minimal version of the construction XML. Only elements/commands are
	 * preserved, the rest is deleted.
	 *
	 * @param cons
	 *            The construction
	 * @param statement
	 *            The statement to prove
	 * @return The simplified XML
	 */
	// TODO: Cut even more unneeded parts to reduce unneeded traffic between OGP
	// and GeoGebra.
	protected static String simplifiedXML(Construction cons,
			GeoElement statement) {
		StringBuilder sb = new StringBuilder();
		cons.getConstructionElementsXML_OGP(sb, statement);

		// /* FIXME: EXTREMELY DIRTY HACK. This should be handled in OGP instead
		// here.
		// * In GeoGebra3D some objects get a 3D parameter, e.g. Circle. OGP is
		// not
		// * yet prepared for handling this, so we simply remove the
		// a2="xOyPlane" texts
		// * from the XML. Hopefully this works for most cases...
		// */
		// return "<construction>\n" + sb.toString().replace(" a2=\"xOyPlane\"",
		// "") + "</construction>";

		return "<construction>\n" + sb.toString() + "</construction>";
	}

	/**
	 * Does the real computation for the proof
	 */
	public void compute() {
		// Will be overridden by web and desktop
	}

	/**
	 * Calls OpenGeoProver
	 *
	 * @param pe
	 *            Prover Engine
	 * @return the proof result
	 */
	protected abstract ProofResult openGeoProver(ProverEngine pe);

	/**
	 * Will the prover return extra NDGs?
	 *
	 * @return yes or no
	 */
	public boolean isReturnExtraNDGs() {
		return returnExtraNDGs;
	}

	/**
	 * The prover may return extra NDGs
	 *
	 * @param returnExtraNDGs
	 *            setting for the prover
	 */
	public void setReturnExtraNDGs(boolean returnExtraNDGs) {
		this.returnExtraNDGs = returnExtraNDGs;
	}

	/**
	 * Formulate figure in readable format: create a mathematically readable
	 * statement. TODO: create translation keys.
	 *
	 * @param statement
	 *            the input statement
	 * @return a localized statement in readable format
	 */
	public static String getTextFormat(GeoElement statement, boolean showStatement, String separator) {
		// FIXME: This is now English specific. For a more general approach we need
		// more work on the localization (with additional database entries).
		Localization loc = statement.getKernel().getLocalization();
		ArrayList<String> freePoints = new ArrayList<>();
		Iterator<GeoElement> it = statement.getAllPredecessors().iterator();
		StringBuilder hypotheses = new StringBuilder();
		ArrayList<AlgoElement> ael = new ArrayList<>(); // processed algos
		while (it.hasNext()) {
			GeoElement geo = it.next();
			if (geo.isGeoPoint() && geo.getParentAlgorithm() == null) {
				freePoints.add(getLabel(loc, geo));
			} else if (!(geo instanceof GeoNumeric)) {
				if (!ael.contains(geo.getParentAlgorithm())) {
					String definition =
							geo.getDefinitionDescription(StringTemplate.defaultTemplate);
					if (definition == null || definition.equals("")) {
						definition = geo.toString(); // handle e.g. xAxis: y = 0, TODO: improve this.
					}
					// Make the first letter lowercase. TODO: Check if this is OK for all locales.
					if (!loc.isReverseNameDescriptionLanguage() && !loc.getLocaleStr().startsWith("de")) {
						definition = (definition.substring(0, 1)).toLowerCase(Locale.ROOT)
								+ definition.substring(1);
					}
					definition = definition.replace("Bisector", "bisector");
					String textLocalized = null;
					AlgoElement ae = geo.getParentAlgorithm();
					if (ae != null && (ae instanceof AlgoPointOnPath
							|| ae instanceof AlgoPointInRegion)) {
						textLocalized = loc.getPlain("LetABeAB",
								getLabel(loc, geo), definition);
					} else {
						if (ae != null && ae instanceof AlgoPolygonRegular && !ael.contains(ae)) {
							ael.add(ae);
							StringBuilder points = new StringBuilder();
							points.append(ae.getInput(0).getLabelSimple()).append(", ");
							points.append(ae.getInput(1).getLabelSimple()).append(", ");
							int n = ae.getOutputLength();
							for (int i = n / 2 + 2; i < n; ++i) {
								points.append(ae.getOutput(i).getLabelSimple());
								points.append(", ");
							}
							int l = points.length();
							points.deleteCharAt(l - 1);
							points.deleteCharAt(l - 2);
							textLocalized = loc.getPlain("LetABeTheRegularBGonVerticesC",
									getLabel(loc, geo), ae.getInput(2).toString(),
									points.toString());
						} else {
							textLocalized = loc.getPlain("LetABeTheB",
									getLabel(loc, geo), definition);
						}
					}
					if (textLocalized != null) {
						hypotheses.append(textLocalized).append(separator);
					}
				}
			} else { // geo is a GeoNumeric
				if (geo.isLabelSet()) {
					String definition =
							geo.getDefinitionDescription(StringTemplate.defaultTemplate);
					String textLocalized = loc.getPlain("DenoteTheExpressionAByB",
							definition, getLabel(loc, geo));
					if (textLocalized != null) {
						hypotheses.append(textLocalized).append(separator);
					}
				}
			}
		}
		StringBuilder theoremText = new StringBuilder();
		StringBuilder freePointsText = new StringBuilder();

		for (String str : freePoints) {
			freePointsText.append(str);
			freePointsText.append(", ");
		}
		int l = freePointsText.length();
		if (l > 0) {
			// remove last two chars:
			freePointsText.deleteCharAt(l - 1);
			freePointsText.deleteCharAt(l - 2);
			if (freePoints.size() > 1) {
				theoremText.append(loc.getPlain("LetABeArbitraryPoints",
						freePointsText.toString())).append(separator);
			} else {
				theoremText.append(loc.getPlainDefault("LetABeAnArbitraryPoint",
						"Let %0 be an arbitrary point.",
						freePointsText.toString())).append(separator);
			}
		}

		theoremText.append(hypotheses);

		if (showStatement) {
			String toProveStr = "true"; // localize
			if (statement.getParentAlgorithm() != null) {
				toProveStr = String.valueOf(statement.getParentAlgorithm());
			}
			theoremText.append(loc.getPlain("ProveThatA", toProveStr));
		}
		return theoremText.toString();
	}

	static String getLabel(Localization loc, GeoElement geo) {
		String label = geo.getLabelSimple();
		if (label == null) {
			label = loc.getMenuDefault("ANamelessObject", "a nameless object");
		}
		return label;
	}

	/**
	 * @return Recio's prover
	 */
	AbstractProverReciosMethod getReciosProver() {

		if (reciosProver == null) {
			reciosProver = getNewReciosProver();
		}

		return reciosProver;
	}

	/**
	 * @return new Recio's prover
	 */
	protected abstract AbstractProverReciosMethod getNewReciosProver();

	/**
	 * @return The full GeoGebra construction, containing all geos and algos.
	 */
	public Construction getConstruction() {
		return construction;
	}

	/* Adds a non-degeneracy condition to the prover object
	 *
	 * @param ndgc
	 *            the condition itself
	 */
	public void addNDGcondition(NDGCondition ndgc) {
		// Add only a new condition, not an existing one.
		// Since ndgConditions is a TreeSet with ordering, this should be automatically working.
		ndgConditions.add(ndgc);
	}


}
