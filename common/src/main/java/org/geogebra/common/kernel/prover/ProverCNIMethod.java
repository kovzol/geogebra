package org.geogebra.common.kernel.prover;

import java.util.ArrayList;
import java.util.TreeSet;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoAnglePoints;
import org.geogebra.common.kernel.algos.AlgoAngularBisectorPoints;
import org.geogebra.common.kernel.algos.AlgoCircleThreePoints;
import org.geogebra.common.kernel.algos.AlgoCircleTwoPoints;
import org.geogebra.common.kernel.algos.AlgoDependentBoolean;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectLineConic;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.algos.AlgoJoinPoints;
import org.geogebra.common.kernel.algos.AlgoJoinPointsRay;
import org.geogebra.common.kernel.algos.AlgoJoinPointsSegment;
import org.geogebra.common.kernel.algos.AlgoLineBisector;
import org.geogebra.common.kernel.algos.AlgoLineBisectorSegment;
import org.geogebra.common.kernel.algos.AlgoLinePointLine;
import org.geogebra.common.kernel.algos.AlgoMidpoint;
import org.geogebra.common.kernel.algos.AlgoMidpointSegment;
import org.geogebra.common.kernel.algos.AlgoOrthoLinePointLine;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.algos.AlgoPolygon;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.scripting.CmdShowProof;
import org.geogebra.common.main.Localization;
import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.Prover.ProofResult;
import org.geogebra.common.util.debug.Log;

import static org.geogebra.common.cas.giac.CASgiac.ggbGiac;

import com.himamis.retex.editor.share.util.Unicode;

public class ProverCNIMethod {

	private static Kernel kernel;

	public static int WARNING_PERPENDICULAR_OR_PARALLEL = 1;
	public static int WARNING_EQUALITY_OR_COLLINEAR = 2;

	public static class CNIDefinition {
		// TODO: Consider adding more refinements here, add extra infos related to the Strings.
		String declaration; // \n-separated Strings of declarations in Giac format
		String realRelation; // \n-separated Strings of rhs of real relations in Giac format
		boolean ignore; // empty definition?
		boolean rMustBe0 = false; // if r is required to be 0
		int warning = 0; // different interpretation than usual?
		int specRestriction = 0; // number of disallowed fixed points
	}

	public static ProofResult prove(Prover prover) {

		GeoElement statement = prover.getStatement();
		kernel = statement.getKernel();
		Localization loc = kernel.getLocalization();

		String declarations = "";
		String realRelations = "";
		int realRelationsNo = 0;
		boolean declarative = true;
		boolean rMustBeZero = false;
		int maxSpecRestriction = 0;

		String VARIABLE_R_STRING = "r__"; // This must be a kind of unique string.
		String VARIABLE_I_STRING = "I_"; // This must be a kind of unique string.

		// We try to avoid divisions by X-Y if X or Y are generated points,
		// to not introduce extra degeneracy than required. That is,
		// all formulas assume that arguments are ordered where the free points appear first.
		String[] predefinitions = {"coll(A_,B_,C_):=(A_-C_)/(A_-B_)",
				"par(A_,B_,C_,D_):=(C_-D_)/(A_-B_)",
				"perppar(A_,B_,C_,D_):=((C_-D_)/(A_-B_))^2",
				"conc(A_,B_,C_,D_):=((C_-D_)/(C_-A_))/((B_-D_)/(B_-A_))",
				// They are not considered yet:
				"eqangle(A_,B_,C_,D_,E_,F_):=((B_-A_)/(B_-C_))/((E_-D_)/(E_-F_))",
				"isosc(A_,B_,C_):=eqangle(C_,B_,A_,A_,C_,B_)" // |AB|=|AC|
		};
		String predefs = "";
		for (String predefinition : predefinitions) {
			predefs += "[" + predefinition + "],";
		}

		// All predecessors:
		TreeSet<GeoElement> allPredecessors = statement.getAllPredecessors();

		// Free points. We need them to eliminate the variables according to them.
		TreeSet<GeoElement> freePoints = new TreeSet<>();
		// Real-relational points. We need them to eliminate the variables according to them too.
		TreeSet<GeoElement> realRelationalPoints = new TreeSet<>();

		if (prover.getShowproof()) {
			prover.addProofLine(loc.getMenuDefault("TheHypotheses", "The hypotheses:"));
		}
		for (GeoElement ge : allPredecessors) {
			if (ge.getParentAlgorithm() == null) {
				freePoints.add(ge);
			} else {
				// We also collect declarative and real-relational definitions.
				CNIDefinition def = null;
				try {
					def = getCNIHypothesisDefinition(ge);
				} catch (Exception ex) {
					Log.debug("The CNI method does not yet fully implement " + ge.getParentAlgorithm().toString()
							+ " which is required for " + ge.getLabelSimple());
					return ProofResult.UNKNOWN;
				}
				if (def == null) {
					Log.debug("The CNI method does not yet implement " + ge.getParentAlgorithm().toString()
						+ " which is required for " + ge.getLabelSimple());
					return ProofResult.UNKNOWN;
				}
				if (def.ignore) {
					continue; // for
				}
				if (def.declaration != null) {
					declarations += def.declaration + "\n";
					if (prover.getShowproof()) {
						prover.addProofLine(CmdShowProof.TEXT_EQUATION, def.declaration);
					}
				}
				if (def.realRelation != null) {
					String[] CASrealRelations = def.realRelation.split("\n");
					if (prover.getShowproof()) {
						prover.addProofLine(loc.getPlain("ConsideringDefinitionA",
								ge.getLabelSimple() + " = "
										+ ge.getDefinition(
										StringTemplate.defaultTemplate)));
					}
					for (String CASrealRelation : CASrealRelations) {
						realRelationsNo++;
						String expression = CASrealRelation + "=" + VARIABLE_R_STRING + realRelationsNo;
						realRelations += expression + ",";
						if (prover.getShowproof()) {
							String rewriteProgram = "[" + predefs + expression + "][" + predefinitions.length + "]";
							String expression2 = executeGiac(rewriteProgram);
							prover.addProofLine(CmdShowProof.TEXT_EQUATION, expression2
									+ Unicode.IS_ELEMENT_OF + "\u211D");
						}
					}
					if (def.warning == WARNING_PERPENDICULAR_OR_PARALLEL) {
						prover.addProofLine(CmdShowProof.PROBLEM, loc.getMenuDefault("PerpendicularityParallelism",
								"Perpendicularity means perpendicularity or parallelism simultaneously."));
					}
					if (def.warning == WARNING_EQUALITY_OR_COLLINEAR) {
						prover.addProofLine(CmdShowProof.PROBLEM, loc.getMenuDefault("EqualityCollinearity",
								"Equality of lengths means equality or collinearity simultaneously."));
					}
					realRelationalPoints.add(ge);
					declarative = false;
				}
			}
		}

		// Adding the thesis. This is very similar to the code above:
		CNIDefinition def = null;
		try {
			def = getCNIThesisDefinition(statement);
		} catch (Exception e) {
			Log.debug("The CNI method does not yet fully implement " + statement.getParentAlgorithm().toString());
			return ProofResult.UNKNOWN;
		}
		if (def == null) {
			Log.debug("The CNI method does not yet implement " + statement.getParentAlgorithm().toString());
			return ProofResult.UNKNOWN;
		}
		if (prover.getShowproof()) {
			prover.addProofLine(loc.getMenuDefault("TheThesis", "The thesis:"));
			prover.addProofLine(statement.getParentAlgorithm().getDefinition(StringTemplate.defaultTemplate));
		}
		if (def.declaration != null) {
			declarations += def.declaration;
			if (prover.getShowproof()) {
				prover.addProofLine(CmdShowProof.TEXT_EQUATION, def.declaration);
			}
		}
		if (def.realRelation != null) {
			String thesis = def.realRelation + "=" + VARIABLE_R_STRING;
			realRelations += thesis;
			if (prover.getShowproof()) {
				String rewriteProgram = "[" + predefs + thesis + "][" + predefinitions.length + "]";
				String thesis2 = executeGiac(rewriteProgram);
				prover.addProofLine(CmdShowProof.TEXT_EQUATION, thesis2);
				if (def.warning == WARNING_PERPENDICULAR_OR_PARALLEL) {
					prover.addProofLine(CmdShowProof.PROBLEM, loc.getMenuDefault("PerpendicularityParallelism",
							"Perpendicularity means perpendicularity or parallelism simultaneously"));
				}
				if (def.warning == WARNING_EQUALITY_OR_COLLINEAR) {
					prover.addProofLine(CmdShowProof.PROBLEM,
							loc.getMenuDefault("EqualityCollinearity",
									"Equality of lengths means equality or collinearity simultaneously."));
				}
				if (def.specRestriction > 0 && def.specRestriction > maxSpecRestriction) {
					maxSpecRestriction = def.specRestriction;
				}
			}
		}
		if (def.rMustBe0) {
			rMustBeZero = true;
		}
		// Specialization.
		if (prover.getShowproof()) {
			prover.addProofLine(CmdShowProof.SPECIALIZATION, loc.getMenuDefault("WlogCoordinates",
					"Without loss of generality, some coordinates can be fixed:"));
		}
		// Put the first two points into 0 and 1:
		int i = 0;
		TreeSet<GeoElement> specialized = new TreeSet<>();
		String specCode = "";
		for (GeoElement ge : freePoints) {
			if (i == 0 && maxSpecRestriction < 2) {
				String spec1 = getUniqueLabel(ge) + ":=0";
				specCode += spec1 + "\n";
				if (prover.getShowproof()) {
					prover.addProofLine(CmdShowProof.TEXT_EQUATION, spec1);
				}
				specialized.add(ge);
			}
			if (i == 1 && maxSpecRestriction < 1) {
				String spec2 = getUniqueLabel(ge) + ":=1";
				specCode += spec2 + "\n";
				if (prover.getShowproof()) {
					prover.addProofLine(CmdShowProof.TEXT_EQUATION, spec2);
				}
				specialized.add(ge);
			}
			i++;
		}
		declarations = specCode + declarations; // Prepend specializations before declarations.
		freePoints.removeAll(specialized);
		// These will be no longer free points.

		// Putting the code together...
		String program = "";
		program = "[";
		program += predefs;
		String[] declarationsA = declarations.split("\n");
		for (String declaration : declarationsA) {
			program += "[" + declaration + "],";
		}
		program += "[" + VARIABLE_I_STRING + ":=eliminate([" + realRelations;
		String program1 = program; // first program stored, later it may be required with an edit
		String rest = "";
		rest += "],";
		String toEliminate = "";
		for (GeoElement ge : freePoints) {
			toEliminate += getUniqueLabel(ge) + ",";
		}
		for (GeoElement ge : realRelationalPoints) {
			toEliminate += getUniqueLabel(ge) + ",";
		}
		toEliminate = removeTail(toEliminate, 1);
		rest += "[" + toEliminate + "])]";
		int codeLengthLines = predefinitions.length + declarationsA.length + 1;
		rest += "][" + (codeLengthLines - 1) + "]";
		program += rest;
		String elimIdeal = executeGiac(program);
		// This is in form {{4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}}
		// or there may be multiple polynomials in the form {{...,...,...}}

		if (prover.getShowproof()) {
			prover.addProofLine(loc.getMenuDefault("EliminateAllComplexVariables",
					"We eliminate all variables that correspond to complex points."));
		}

		if (elimIdeal.equals("{{}}")) {
			// There is no direct correspondence between r1, r2, ..., and r.
			// The statement is quite probably false, but we cannot explicitly state this.
			if (prover.getShowproof()) {
				prover.addProofLine(CmdShowProof.PROBLEM,
						loc.getMenuDefault("NoCorrespondenceBetweenHypothesesThesis",
								"There is no correspondence between the hypotheses and the thesis."));
			}
			Log.debug("The elimination ideal is <0>, no conclusion.");
			return ProofResult.UNKNOWN;
		}

		// There is a direct correspondence.
		String elimIdealL = removeHeadTail(elimIdeal, 1).
				replace("{", "[").replace("}", "]"); // remove { and }
		// Now we choose the minimal degree polynomial (in r) of this list.
		program = "[[" + VARIABLE_I_STRING + ":= " + elimIdealL + "],[deg:=inf],[degi:=0],"
				+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { d:=degree(" + VARIABLE_I_STRING
				+ "[k]," + VARIABLE_R_STRING + ");"
				+ "if (d>0 && d<deg) { deg:=d; degi:=k; } }],"
				+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
		program = ggbGiac(program);
		String minDegree = executeGiac(program);
		// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

		String minDegreeC = removeHeadTail(minDegree,1); // remove { and }
		String[] minDegreeA = minDegreeC.split(","); // Separate items
		if (minDegreeA[0].equals("+infinity")) {
			// r cannot be expressed, the statement is probably false...
			if (prover.getShowproof()) {
				prover.addProofLine(CmdShowProof.PROBLEM,
						loc.getMenuDefault("ThesisCannotBeExpressed",
								"The thesis cannot be expressed with the hypotheses."));
			}
			Log.debug("The elimination ideal does not contain r_.");
			return ProofResult.UNKNOWN;
		}
		int minDegreeI = Integer.valueOf(minDegreeA[0]);
		if (minDegreeI == 1) {
			// r can be expressed by using r1, r2, ..., here r is linear.
			if (prover.getShowproof()) {
				prover.addProofLine(loc.getPlain(
						"The thesis (%0) can be expressed as a rational expression of the hypotheses, because %0 is linear in an obtained polynomial equation:",
						VARIABLE_R_STRING));
				prover.addProofLine(minDegreeA[1] + "=0");
			}
			Log.debug("The elimination ideal contains " + minDegreeA[1] + ", it is linear in r_.");
			// Check if r can be expressed without a division:
			// lvar(coeff(2*r_+1,r_)[0])
			program = "lvar(coeff(" + minDegreeA[1] + "," + VARIABLE_R_STRING + ")[0])";
			String divVars = executeGiac(program);
			if (divVars.equals("{}")) {
				if (rMustBeZero) {
					if (minDegreeA[1].equals(VARIABLE_R_STRING)) {
						if (prover.getShowproof()) {
							prover.addProofLine(CmdShowProof.CONCLUSION,
									loc.getMenuDefault("ThesisZeroStatementTrue",
											"Since the thesis is zero, the statement is true."));
						}
						Log.debug("r_ is zero.");
						return ProofResult.TRUE;
					}
					if (prover.getShowproof()) {
						prover.addProofLine(CmdShowProof.PROBLEM,
								loc.getMenuDefault("ThesisShouldBeZero",
										"Since the thesis is not zero, the statement cannot be proven."));
					}
					Log.debug("r_ should be zero.");
					return ProofResult.UNKNOWN; // maybe here we can result FALSE?
				}
				if (prover.getShowproof()) {
					prover.addProofLine(loc.getMenuDefault("ThesisCanBeExpressedPolynomial",
							"The thesis can be expressed as a polynomial expression of the hypotheses."));
					prover.addProofLine(CmdShowProof.CONCLUSION,
							loc.getMenuDefault("HypothesesRealThesisReal",
									"Since all hypotheses are real expressions, the thesis must also be real."));
				}
				return ProofResult.TRUE;
			}
			// Read off the divisor when expressing r:
			program = "coeff(" + minDegreeA[1] + "," + VARIABLE_R_STRING + ")[0])";
			String divisor = executeGiac(program);
			if (prover.getShowproof()) {
				prover.addProofLine(
						loc.getPlain("Expressing the thesis requires a division by %0.", divisor));
				prover.addProofLine(loc.getMenuDefault("AssumeDivisorZero",
						"Let us assume that that divisor is 0 and restart the elimination."));
			}
			// Insert the divisor in the first program and check what happens:
			program = program1 + "," + divisor + rest;
			String elimIdeal2 = executeGiac(program);

			if (elimIdeal2.equals("{{1}}")) {
				// The case divisor == 0 is contradictory. This means that division by zero
				// is not a relevant issue, so we can be sure that the statement is true.
				if (prover.getShowproof()) {
					prover.addProofLine(loc.getMenuDefault("DivisorCannotBeZero",
							"The elimination verifies that that divisor cannot be zero."));
				}
				Log.debug("Division by zero is irrelevant.");
				if (rMustBeZero) {
					if (prover.getShowproof()) {
						prover.addProofLine(CmdShowProof.PROBLEM,
								loc.getMenuDefault("ThesisShouldBeZero",
										"Since the thesis is not zero, the statement cannot be proven."));
					}
					Log.debug("r_ should be zero.");
					return ProofResult.UNKNOWN; // maybe here we can result FALSE?
				}
				if (prover.getShowproof()) {
					prover.addProofLine(CmdShowProof.CONCLUSION,
							loc.getMenuDefault("HypothesesRealThesisReal",
									"Since all hypotheses are real expressions, the thesis must also be real."));
				}
				return ProofResult.TRUE;
			}

			// There is direct correspondence between r1, r2, ..., and r.
			String elimIdeal2L = removeHeadTail(elimIdeal2, 1).
					replace("{", "[").replace("}", "]"); // remove { and }
			// Now we choose the minimal degree polynomial (in r) of this list.
			program = "[[" + VARIABLE_I_STRING + ":= " + elimIdeal2L + "],[deg:=inf],[degi:=0],"
					+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { d:=degree(" + VARIABLE_I_STRING
					+ "[k]," + VARIABLE_R_STRING + ");"
					+ "if (d>0 && d<deg) { deg:=d; degi:=k; } }],"
					+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
			program = ggbGiac(program);
			String minDegree2 = executeGiac(program);
			// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

			String minDegree2C = removeHeadTail(minDegree2,1); // remove { and }
			String[] minDegree2A = minDegree2C.split(","); // Separate items
			if (minDegree2A[0].equals("+infinity")) {
				// r cannot be expressed, the statement is probably false...
				if (prover.getShowproof()) {
					prover.addProofLine(CmdShowProof.PROBLEM,
							loc.getMenuDefault("AssumingZeroThesisCannotBeExpressed",
									"Assuming that this is zero, the thesis cannot be expressed with the hypotheses."));
				}
				Log.debug("The second elimination ideal does not contain r_.");
				return ProofResult.UNKNOWN;
			}
			int minDegree2I = Integer.valueOf(minDegree2A[0]);
			if (minDegree2I == 1) {
				// The secondly computed ideal is linear.
				if (prover.getShowproof()) {
					prover.addProofLine(loc.getPlain(
							"The thesis (%0) can now be expressed as a rational expression of the hypotheses, because %0 is linear in an obtained polynomial equation:",
							VARIABLE_R_STRING));
					prover.addProofLine(minDegree2A[1] + "=0");
				}
				Log.debug("The second elimination ideal contains " + minDegree2A[1] + ", it is linear in r_.");
				// Check if r can be expressed without a division:
				// lvar(coeff(2*r_+1,r_)[0])
				program = "lvar(coeff(" + minDegree2A[1] + ","+ VARIABLE_R_STRING + ")[0])";
				String divVars2 = executeGiac(program);
				if (divVars2.equals("{}")) {
					if (rMustBeZero) {
						if (minDegree2A[1].equals(VARIABLE_R_STRING)) {
							if (prover.getShowproof()) {
								prover.addProofLine(CmdShowProof.CONCLUSION,
										loc.getMenuDefault("ThesisZeroStatementTrue",
												"Since the thesis is zero, the statement is true."));
							}
							Log.debug("r_ is zero.");
							return ProofResult.TRUE;
						}
						if (prover.getShowproof()) {
							prover.addProofLine(CmdShowProof.PROBLEM,
									loc.getMenuDefault("ThesisShouldBeZeroNow",
											"Since the thesis is not zero now, the statement cannot be proven."));
						}
						Log.debug("r_ should be zero.");
						return ProofResult.UNKNOWN; // maybe here we can result FALSE?
						}
					if (prover.getShowproof()) {
						prover.addProofLine(loc.getMenuDefault("NowThesisCanBeExpressedPolynomial",
								"Now the thesis can be expressed as a polynomial expression of the hypotheses."));
						prover.addProofLine(CmdShowProof.CONCLUSION,
								loc.getMenuDefault("HypothesesRealThesisReal",
										"Since all hypotheses are real expressions, the thesis must also be real."));
					}
					return ProofResult.TRUE;
				}
				// Cannot decide, maybe we need another round? TODO
				if (prover.getShowproof()) {
					prover.addProofLine(CmdShowProof.PROBLEM,
							loc.getMenuDefault("ThesisStillContainsDivision",
									"The thesis still contains a division, no conclusion can be found."));
				}
				Log.debug("Another division occurred, a third elimination is needed.");
				return ProofResult.UNKNOWN;
			}
			// The division does not result in an unambiguous case.
			if (prover.getShowproof()) {
				prover.addProofLine(CmdShowProof.PROBLEM,
						loc.getMenuDefault("ThesisCannotBeExpressedDivision",
								"The thesis cannot be expressed as a division now."));
			}
			Log.debug("The division does not result in an unambiguous case.");
			return ProofResult.UNKNOWN;
		}
		// The case is not linear.
		if (prover.getShowproof()) {
			prover.addProofLine(CmdShowProof.PROBLEM,
					loc.getMenuDefault("ThesisCannotBeExpressedDivision",
							"The thesis cannot be expressed as a division."));
		}
		Log.debug("r_ is not linear, further check is needed.");
		return ProofResult.UNKNOWN;
	}

	/** Create the CNI definition for a GeoElement (for a hypothesis).
	 * Compute the full declaration String, but only the lhs of the real relation String,
	 * and return them to the caller. This method should cover all algos sooner or later.
	 * Now it is just a prototype that implements the CNI method for some frequently used algos.
	 *
	 * @param ge the input GeoElement
	 * @return all required information for the CNI definition for the input
	 */
	static CNIDefinition getCNIHypothesisDefinition(GeoElement ge) {
		CNIDefinition c = new CNIDefinition();
		AlgoElement ae = ge.getParentAlgorithm();
		String gel = getUniqueLabel(ge);
		// Declarations:
		if (ae instanceof AlgoMidpoint) {
			AlgoMidpoint am = (AlgoMidpoint) ae;
			GeoElement P = am.getP();
			GeoElement Q = am.getQ();
			String Pl = getUniqueLabel(P);
			String Ql = getUniqueLabel(Q);
			c.declaration = gel + ":=(" + Pl + "+" + Ql + ")/2";
			return c;
		}
		if (ae instanceof AlgoMidpointSegment) {
			AlgoMidpointSegment ams = (AlgoMidpointSegment) ae;
			GeoElement P = ams.getP();
			GeoElement Q = ams.getQ();
			String Pl = getUniqueLabel(P);
			String Ql = getUniqueLabel(Q);
			c.declaration = gel + ":=(" + Pl + "+" + Ql + ")/2";
			return c;
		}
		// Real relations:
		if (ae instanceof AlgoIntersectLines) {
			AlgoIntersectLines ail = (AlgoIntersectLines) ae;
			GeoLine g = ail.getg();
			GeoLine h = ail.geth();
			String rel1 = "", rel2 = "";
			rel1 = online((GeoPoint) ge, g);
			rel2 = online((GeoPoint) ge, h);
			if (rel1 == null || rel2 == null) {
				return null; // Not implemented.
			}
			if (rel1.startsWith("perppar") || rel2.startsWith("perppar")) {
				c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
			}
			if (rel1.startsWith("isosc") || rel2.startsWith("isosc")) {
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
			}
			c.realRelation = rel1 + "\n" + rel2;
			return c;
		}
		if (ae instanceof AlgoIntersectLineConic) {
			AlgoIntersectLineConic ailc = (AlgoIntersectLineConic) ae;
			GeoLine l = ailc.getLine();
			GeoConic co = ailc.getConic();
			String rel1 = "", rel2 = "";
			rel1 = online((GeoPoint) ge, l);
			rel2 = oncircle((GeoPoint) ge, co);
			if (rel1 == null || rel2 == null) {
				return null; // Not implemented.
			}
			if (rel1.startsWith("perppar")) {
				c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
			}
			if (rel1.startsWith("isosc")) {
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
			}
			c.realRelation = rel1 + "\n" + rel2;
			return c;
		}
		if (ae instanceof AlgoPointOnPath) {
			AlgoPointOnPath apop = (AlgoPointOnPath) ae;
			GeoElement[] input = apop.getInput();
			GeoElement p = input[0];
			if (p instanceof GeoLine) {
				GeoPoint gS = ((GeoLine) p).getStartPoint();
				GeoPoint gE = ((GeoLine) p).getEndPoint();
				c.realRelation = online((GeoPoint) ge, (GeoLine) p);
				if (c.realRelation.startsWith("perppar")) {
					c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
				}
				if (c.realRelation.startsWith("isosc")) {
					c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				}
				return c;
			}
			if (p instanceof GeoConic) {
				if (((GeoConic) p).isCircle()) {
					ArrayList<GeoPointND> points = ((GeoConic) p).getPointsOnConic();
					if (points.size() >= 4) {
						GeoPoint A = (GeoPoint) points.get(0).toGeoElement();
						GeoPoint B = (GeoPoint) points.get(1).toGeoElement();
						GeoPoint C = (GeoPoint) points.get(2).toGeoElement();
						GeoPoint D = (GeoPoint) points.get(3).toGeoElement();
						c.realRelation = concyclic(A, B, C, D);
						return c;
					}
					return null; // Not implemented.
				}
				return null; // Not implemented.
			}
			return null; // Not implemented.
		}
		if (ae instanceof AlgoPolygon || ae instanceof AlgoJoinPointsSegment ||
				ae instanceof AlgoJoinPoints || ae instanceof AlgoCircleThreePoints ||
				ae instanceof AlgoAngularBisectorPoints || ae instanceof AlgoLineBisector ||
				ae instanceof AlgoLineBisectorSegment || ae instanceof AlgoJoinPointsRay ||
				ae instanceof AlgoLinePointLine || ae instanceof AlgoOrthoLinePointLine ||
				ae instanceof AlgoCircleTwoPoints || ae instanceof AlgoAnglePoints) {
			c.ignore = true;
			return c;
		}
		// Unimplemented, but it should be handled...
		return null;
	}

	/** Create the CNI definition for a GeoElement (for a thesis).
	 * Compute the rhs of the declaration String, the lhs of the real relation String,
	 * and return them to the caller. This method should cover all algos sooner or later.
	 * Now it is just a prototype that implements the CNI method some frequently used algos.
	 *
	 * @param ge the input GeoElement
	 * @return all required information for the CNI definition for the input
	 */
	static CNIDefinition getCNIThesisDefinition(GeoElement ge) {
		CNIDefinition c = new CNIDefinition();
		AlgoElement ae = ge.getParentAlgorithm();
		if (ae instanceof AlgoAreCollinear) {
			AlgoAreCollinear aac = (AlgoAreCollinear) ae;
			GeoElement[] input = aac.getInput();
			GeoElement A = input[0];
			GeoElement B = input[1];
			GeoElement C = input[2];
			c.realRelation = collinear(A, B, C);
			return c;
		}
		if (ae instanceof AlgoAreConcyclic) {
			AlgoAreConcyclic aac = (AlgoAreConcyclic) ae;
			GeoElement[] input = aac.getInput();
			GeoElement A = input[0];
			GeoElement B = input[1];
			GeoElement C = input[2];
			GeoElement D = input[3];
			c.realRelation = concyclic(A, B, C, D);
			return c;
		}
		if (ae instanceof AlgoAreParallel) {
			AlgoAreParallel aap = (AlgoAreParallel) ae;
			GeoElement[] input = aap.getInput();
			GeoLine g = (GeoLine) input[0];
			GeoLine h = (GeoLine) input[1];
			c.realRelation = parallel(g, h);
			return c;
		}
		if (ae instanceof AlgoArePerpendicular) { // in fact, perpendicular or parallel
			Log.debug("Warning: Testing perpendicularity AND parallelism simultaneously");
			AlgoArePerpendicular aap = (AlgoArePerpendicular) ae;
			GeoElement[] input = aap.getInput();
			GeoLine g = (GeoLine) input[0];
			GeoLine h = (GeoLine) input[1];
			c.realRelation = perppar(g, h);
			c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
			return c;
		}
		if (ae instanceof AlgoAreEqual) {
			AlgoAreEqual aae = (AlgoAreEqual) ae;
			GeoElement[] input = aae.getInput();
			return equal(input[0], input[1]);
		}
		if (ae instanceof AlgoAreCongruent) {
			AlgoAreCongruent aac = (AlgoAreCongruent) ae;
			GeoElement[] input = aac.getInput();
			return equal(input[0], input[1]);
		}
		if (ae instanceof AlgoDependentBoolean) {
			ExpressionNode en = ((AlgoDependentBoolean) ae).getExpression();
			if (!en.getLeft().isGeoElement() || !en.getRight().isGeoElement()) {
				return null; // Unimplemented (maybe a sum).
			}
			GeoElement ge1 = en.getLeftTree().getSingleGeoElement();
			GeoElement ge2 = en.getRightTree().getSingleGeoElement();
			Operation o = en.getOperation();
			if (o == Operation.PARALLEL) {
				c.realRelation = parallel((GeoLine) ge1, (GeoLine) ge2);
				return c;
			} else if (o == Operation.PERPENDICULAR) {
				Log.debug("Warning: Testing perpendicularity AND parallelism simultaneously");
				c.realRelation = perppar((GeoLine) ge1, (GeoLine) ge2);
				c.warning = WARNING_PERPENDICULAR_OR_PARALLEL;
				return c;
			} else if (o == Operation.EQUAL_BOOLEAN) {
				return equal(ge1, ge2);
			}
		}
		// Unimplemented, but it should be handled...
		return null;
	}

	/**
	 * Return a label that is unique and can be inserted in a Giac code.
	 * @param ge the input GeoElement
	 * @return the label as String
	 */
	static String getUniqueLabel(GeoElement ge) {
		return ge.getLabelSimple();
	}

	static String removeTail(String input, int length) {
		if (input.length() >= length) {
			return input.substring(0, input.length() - length);
		}
		return input;
	}

	private static String executeGiac(String command) {
		GeoGebraCAS cas = (GeoGebraCAS) kernel.getGeoGebraCAS();
		try {
			return cas.evaluateRaw(command);
		} catch (Throwable e) {
			Log.error("Error in ProverCNIMethod/executeGiac: input=" + command);
			return "ERROR";
		}
	}

	// This is already present in the class Compute. TODO: Unify the code.
	static String removeHeadTail(String input, int length) {
		if (input.length() >= 2 * length) {
			return input.substring(length, input.length() - length);
		}
		return input;
	}

	static String collinear(GeoElement ge1, GeoElement ge2, GeoElement ge3) {
		TreeSet<GeoElement> collPoints = new TreeSet<>();
		collPoints.add(ge1);
		collPoints.add(ge2);
		collPoints.add(ge3);
		String ret = "coll(";
		for (GeoElement cp : collPoints) {
			ret += getUniqueLabel(cp) + ",";
		}
		ret = removeTail(ret, 1);
		ret += ")";
		return ret;
	}

	static String concyclic(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4) {
		TreeSet<GeoElement> concPoints = new TreeSet<>();
		concPoints.add(ge1);
		concPoints.add(ge2);
		concPoints.add(ge3);
		concPoints.add(ge4);
		String ret = "conc(";
		for (GeoElement cp : concPoints) {
			ret += getUniqueLabel(cp) + ",";
		}
		ret = removeTail(ret, 1);
		ret += ")";
		return ret;
	}

	static String parallel(GeoPoint ge1, GeoPoint ge2, GeoPoint ge3, GeoPoint ge4) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);

		int i1 = ge1.getConstructionIndex();
		int i2 = ge2.getConstructionIndex();
		int i3 = ge3.getConstructionIndex();
		int i4 = ge4.getConstructionIndex();

		// In a natural order we return the same ordered quadruple:
		if (i1 < i2 && i2 < i3 && i3 < i4)
			return "par(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";

		// In some reversed orders we return the same ordered quadruple:
		if ((i1 > i3 && i2 > i4) || (i1 > i4 && i2 > i3))
			return "par(" + ge3l + "," + ge4l + "," + ge1l + "," + ge2l + ")";

		// Otherwise we return the default order:
		return "par(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";
	}

	static String parallel(GeoLine g, GeoLine h) {
		GeoPoint gS = g.getStartPoint();
		GeoPoint gE = g.getEndPoint();
		GeoPoint hS = h.getStartPoint();
		GeoPoint hE = h.getEndPoint();
		return parallel(gS, gE, hS, hE);
	}

	static String perppar(GeoPoint ge1, GeoPoint ge2, GeoPoint ge3, GeoPoint ge4) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);

		int i1 = ge1.getConstructionIndex();
		int i2 = ge2.getConstructionIndex();
		int i3 = ge3.getConstructionIndex();
		int i4 = ge4.getConstructionIndex();

		// In a natural order we return the same ordered quadruple:
		if (i1 < i2 && i2 < i3 && i3 < i4)
			return "perppar(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";

		// In some reversed orders we return the same ordered quadruple:
		if ((i1 > i3 && i2 > i4) || (i1 > i4 && i2 > i3))
			return "perppar(" + ge3l + "," + ge4l + "," + ge1l + "," + ge2l + ")";

		// Otherwise we return the default order:
		return "perppar(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";
	}

	static String perppar(GeoLine g, GeoLine h) {
		GeoPoint gS = g.getStartPoint();
		GeoPoint gE = g.getEndPoint();
		GeoPoint hS = h.getStartPoint();
		GeoPoint hE = h.getEndPoint();
		return perppar(gS, gE, hS, hE);
	}

	static String isosc(GeoPoint ge1, GeoPoint ge2, GeoPoint ge3) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		return "isosc(" + ge1l + "," + ge2l + "," + ge3l + ")";
	}

	static String eqangle(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4,
			GeoElement ge5, GeoElement ge6) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);
		String ge5l = getUniqueLabel(ge5);
		String ge6l = getUniqueLabel(ge6);
		return "eqangle(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + "," + ge5l + "," + ge6l + ")";
	}

	static String online(GeoPoint ge, GeoLine g) {
		GeoPoint gS = g.getStartPoint();
		GeoPoint gE = g.getEndPoint();
		if (gS != null && gE != null) {
			return collinear(gS, gE, ge);
		} else {
			if (gS != null) {
				AlgoElement gAe = g.getParentAlgorithm();
				if (gAe instanceof AlgoAngularBisectorPoints) {
					GeoPoint A = ((AlgoAngularBisectorPoints) gAe).getA();
					GeoPoint B = ((AlgoAngularBisectorPoints) gAe).getB();
					GeoPoint C = ((AlgoAngularBisectorPoints) gAe).getC();
					return eqangle(A, B, ge, ge, B, C);
				} else if (gAe instanceof AlgoLineBisector) {
					GeoPoint A = ((AlgoLineBisector) gAe).getA();
					GeoPoint B = ((AlgoLineBisector) gAe).getB();
					return isosc(ge, A, B);
				} else if (gAe instanceof AlgoLineBisectorSegment) {
					GeoSegment f = ((AlgoLineBisectorSegment) gAe).getSegment();
					GeoPoint A = f.getStartPoint();
					GeoPoint B = f.getEndPoint();
					return isosc(ge, A, B);
				} else if (gAe instanceof AlgoLinePointLine) {
					AlgoLinePointLine alpl = (AlgoLinePointLine) gAe;
					GeoElement[] input = alpl.getInput();
					GeoPoint P = (GeoPoint) input[0];
					GeoLine h = (GeoLine) input[1];
					GeoPoint hS = h.getStartPoint();
					GeoPoint hE = h.getEndPoint();
					return parallel(P, (GeoPoint) ge, hS, hE);
				} else if (gAe instanceof AlgoOrthoLinePointLine) {
					AlgoOrthoLinePointLine alpl = (AlgoOrthoLinePointLine) gAe;
					GeoElement[] input = alpl.getInput();
					GeoPoint P = (GeoPoint) input[0];
					GeoLine h = (GeoLine) input[1];
					GeoPoint hS = h.getStartPoint();
					GeoPoint hE = h.getEndPoint();
					return perppar(P, (GeoPoint) ge, hS, hE);
				} else {
					// Not yet implemented.
					return null;
				}
			}
		}
		return null; // Unimplemented.
	}

	static String oncircle(GeoPoint ge, GeoConic co) {
		AlgoElement coAe = co.getParentAlgorithm();
		if (coAe instanceof AlgoCircleTwoPoints) {
			AlgoCircleTwoPoints actp = (AlgoCircleTwoPoints) coAe;
			GeoPoint ce = (GeoPoint) actp.getInput(0);
			GeoPoint p = (GeoPoint) actp.getInput(1);
			return isosc(ce, p, ge);
		}
		return null; // Unimplemented.
	}

	static CNIDefinition equal(GeoElement ge1, GeoElement ge2) {
		CNIDefinition c = new CNIDefinition();;
		if (ge1 instanceof GeoPoint && ge2 instanceof GeoPoint) {
			GeoPoint P = (GeoPoint) ge1;
			GeoPoint Q = (GeoPoint) ge2;
			String Pl = getUniqueLabel(P);
			String Ql = getUniqueLabel(Q);
			c.realRelation = Pl + "-" + Ql;
			c.rMustBe0 = true;
			c.specRestriction = 1; // the second free point cannot be fixed
			return c;
		}
		if (ge1 instanceof GeoSegment && ge2 instanceof GeoSegment) {
			GeoSegment s1 = (GeoSegment) ge1;
			GeoSegment s2 = (GeoSegment) ge2;
			GeoPoint A = (GeoPoint) s1.getStartPoint();
			GeoPoint B = (GeoPoint) s1.getEndPoint();
			GeoPoint C = (GeoPoint) s2.getStartPoint();
			GeoPoint D = (GeoPoint) s2.getEndPoint();
			if (A.equals(C)) {
				c.realRelation = isosc(A,B,D);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			if (A.equals(D)) {
				c.realRelation = isosc(A,B,C);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			if (B.equals(C)) {
				c.realRelation = isosc(B,A,D);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			if (B.equals(D)) {
				c.realRelation = isosc(B,A,C);
				c.warning = WARNING_EQUALITY_OR_COLLINEAR;
				return c;
			}
			return null; // In general, checking |AB|=|CD| is not supported by the method.
		}
		if (ge1 instanceof GeoAngle && ge2 instanceof GeoAngle) {
			GeoAngle a1 = (GeoAngle) ge1;
			GeoAngle a2 = (GeoAngle) ge2;
			AlgoElement ae1 = a1.getParentAlgorithm();
			AlgoElement ae2 = a2.getParentAlgorithm();
			if (ae1 instanceof AlgoAnglePoints && ae2 instanceof AlgoAnglePoints) {
				GeoPoint A = (GeoPoint) ((AlgoAnglePoints) ae1).getA();
				GeoPoint B = (GeoPoint) ((AlgoAnglePoints) ae1).getB();
				GeoPoint C = (GeoPoint) ((AlgoAnglePoints) ae1).getC();
				GeoPoint D = (GeoPoint) ((AlgoAnglePoints) ae2).getA();
				GeoPoint E = (GeoPoint) ((AlgoAnglePoints) ae2).getB();
				GeoPoint F = (GeoPoint) ((AlgoAnglePoints) ae2).getC();
				c.realRelation = eqangle(A,B,C,D,E,F);
				return c;
			}
			return null; // Not yet implemented;
		}
		return null; // Missing implementation for equality of other objects.
	}
}
