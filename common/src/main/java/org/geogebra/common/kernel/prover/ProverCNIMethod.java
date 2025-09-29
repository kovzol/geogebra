package org.geogebra.common.kernel.prover;

import java.util.ArrayList;
import java.util.TreeSet;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.algos.AlgoAngularBisectorPoints;
import org.geogebra.common.kernel.algos.AlgoCircleThreePoints;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.algos.AlgoJoinPoints;
import org.geogebra.common.kernel.algos.AlgoJoinPointsSegment;
import org.geogebra.common.kernel.algos.AlgoLineBisector;
import org.geogebra.common.kernel.algos.AlgoMidpoint;
import org.geogebra.common.kernel.algos.AlgoMidpointSegment;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.algos.AlgoPolygon;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.Prover.ProofResult;
import org.geogebra.common.util.debug.Log;

import static org.geogebra.common.cas.giac.CASgiac.ggbGiac;

public class ProverCNIMethod {

	private static Kernel kernel;

	public static class CNIDefinition {
		String declaration;
		String realRelation;
		boolean ignore;
	}

	public static ProofResult prove(Prover prover) {

		GeoElement statement = prover.getStatement();
		kernel = statement.getKernel();

		String declarations = "";
		String realRelations = "";
		int realRelationsNo = 0;
		boolean declarative = true;

		String VARIABLE_R_STRING = "r_"; // This must be a kind of unique string.
		String VARIABLE_I_STRING = "I_"; // This must be a kind of unique string.

		// All predecessors:
		TreeSet<GeoElement> allPredecessors = statement.getAllPredecessors();

		// Free points. We need them to eliminate the variables according to them.
		TreeSet<GeoElement> freePoints = new TreeSet<>();
		// Real-relational points. We need them to eliminate the variables according to them too.
		TreeSet<GeoElement> realRelationalPoints = new TreeSet<>();

		for (GeoElement ge : allPredecessors) {
			if (ge.getParentAlgorithm() == null) {
				freePoints.add(ge);
			} else {
				// We also collect declarative and real-relational definitions.
				CNIDefinition def = null;
				try {
					def = getCNIHypothesisDefinition(ge);
				} catch (NullPointerException ex) {
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
				}
				if (def.realRelation != null) {
					String[] CASrealRelations = def.realRelation.split("\n");
					for (String CASrealRelation : CASrealRelations) {
						realRelationsNo++;
						realRelations += CASrealRelation + "=" + VARIABLE_R_STRING + realRelationsNo + ",";
					}
					realRelationalPoints.add(ge);
					declarative = false;
				}
			}
		}

		// Specialization.
		// Put the first two points into 0 and 1:
		int i = 0;
		TreeSet<GeoElement> specialized = new TreeSet<>();
		String specCode = "";
		for (GeoElement ge : freePoints) {
			if (i == 0) {
				specCode += getUniqueLabel(ge) + ":=0\n";
				specialized.add(ge);
			}
			if (i == 1) {
				specCode += getUniqueLabel(ge) + ":=1\n";
				specialized.add(ge);
			}
			i++;
		}
		declarations = specCode + declarations; // Prepend specializations before declarations.
		freePoints.removeAll(specialized);
		// These will be no longer free points.

		// Adding the thesis. This is very similar to the code above:
		CNIDefinition def = getCNIThesisDefinition(statement);
		if (def == null) {
			Log.debug("The CNI method does not yet implement " + statement.getParentAlgorithm().toString()
					+ " which is required for " + statement.getLabelSimple());
			return ProofResult.UNKNOWN;
		}
		if (def.declaration != null) {
			declarations += def.declaration;
		}
		if (def.realRelation != null) {
			realRelations += def.realRelation + "=" + VARIABLE_R_STRING;
		}

		String[] predefinitions = {"coll(A_,B_,C_):=(A_-B_)/(A_-C_)",
				"par(A_,B_,C_,D_):=(A_-B_)/(C_-D_)",
				"conc(A_,B_,C_,D_):=((C_-A_)/(C_-B_))/((D_-A_)/(D_-B_))",
				"eqangle(A_,B_,C_,D_,E_,F_):=((B_-A_)/(B_-C_))/((E_-D_)/(E_-F_))"
		};

		// Putting the code together...
		String program = "";
		program = "[";
		for (String predefinition : predefinitions) {
			program += "[" + predefinition + "],";
		}
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

		if (elimIdeal.equals("{{}}")) {
			// There is no direct correspondence between r1, r2, ..., and r.
			// The statement is quite probably false, but we cannot explicitly state this.
			Log.debug("The elimination ideal is <0>, no conclusion.");
			return ProofResult.UNKNOWN;
		}

		// There is direct correspondence.
		String elimIdealL = removeHeadTail(elimIdeal, 1).
				replace("{", "[").replace("}", "]"); // remove { and }
		// Now we choose the minimal degree polynomial (in r) of this list.
		program = "[[" + VARIABLE_I_STRING + ":= " + elimIdealL + "],[deg:=inf],[degi:=0],"
				+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { d:=degree(" + VARIABLE_I_STRING + "[k],r_);"
				+ "if (d>0 && d<deg) { deg:=d; degi:=k; } }],"
				+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
		program = ggbGiac(program);
		String minDegree = executeGiac(program);
		// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

		String minDegreeC = removeHeadTail(minDegree,1); // remove { and }
		String[] minDegreeA = minDegreeC.split(","); // Separate items
		if (minDegreeA[0].equals("+infinity")) {
			// r cannot be expressed, the statement is probably false...
			Log.debug("The elimination ideal does not contain r_.");
			return ProofResult.UNKNOWN;
		}
		int minDegreeI = Integer.valueOf(minDegreeA[0]);
		if (minDegreeI == 1) {
			// r can be expressed by using r1, r2, ..., here r is linear.
			Log.debug("The elimination ideal contains " + minDegreeA[1] + ", it is linear in r_.");
			// Check if r can be expressed without a division:
			// lvar(coeff(2*r_+1,r_)[0])
			program = "lvar(coeff(" + minDegreeA[1] + ",r_)[0])";
			String divVars = executeGiac(program);
			if (divVars.equals("{}")) {
				return ProofResult.TRUE;
			}
			// Read off the divisor when expressing r:
			program = "coeff(" + minDegreeA[1] + ",r_)[0])";
			String divisor = executeGiac(program);
			// Insert the divisor in the first program and check what happens:
			program = program1 + "," + divisor + rest;
			String elimIdeal2 = executeGiac(program);

			if (elimIdeal2.equals("{{1}}")) {
				// The case divisor == 0 is contradictory. This means that division by zero
				// is not a relevant issue, so we can be sure that the statement is true.
				Log.debug("Division by zero is irrelevant.");
				return ProofResult.TRUE;
			}

			// There is direct correspondence between r1, r2, ..., and r.
			String elimIdeal2L = removeHeadTail(elimIdeal2, 1).
					replace("{", "[").replace("}", "]"); // remove { and }
			// Now we choose the minimal degree polynomial (in r) of this list.
			program = "[[" + VARIABLE_I_STRING + ":= " + elimIdeal2L + "],[deg:=inf],[degi:=0],"
					+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { d:=degree(" + VARIABLE_I_STRING + "[k],r_);"
					+ "if (d>0 && d<deg) { deg:=d; degi:=k; } }],"
					+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
			program = ggbGiac(program);
			String minDegree2 = executeGiac(program);
			// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

			String minDegree2C = removeHeadTail(minDegree2,1); // remove { and }
			String[] minDegree2A = minDegree2C.split(","); // Separate items
			int minDegree2I = Integer.valueOf(minDegree2A[0]);
			if (minDegree2I == 1) {
				// The secondly computed ideal is linear.
				Log.debug("The second elimination ideal contains " + minDegree2A[1] + ", it is linear in r_.");
				// Check if r can be expressed without a division:
				// lvar(coeff(2*r_+1,r_)[0])
				program = "lvar(coeff(" + minDegree2A[1] + ",r_)[0])";
				String divVars2 = executeGiac(program);
				if (divVars2.equals("{}")) {
					return ProofResult.TRUE;
				}
				// Cannot decide, maybe we need another round? TODO
				return ProofResult.UNKNOWN;
			}
			// The division does not result in an unambiguous case.
			return ProofResult.UNKNOWN;
		}
		// The case is not linear.
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
			GeoPoint gS = g.getStartPoint();
			GeoPoint gE = g.getEndPoint();
			GeoPoint hS = h.getStartPoint();
			GeoPoint hE = h.getEndPoint();
			if (gS != null && gE != null) {
				rel1 = collinear(gS, gE, ge);
			} else {
				if (gS != null) {
					AlgoElement gAe = g.getParentAlgorithm();
					if (gAe instanceof AlgoAngularBisectorPoints) {
						GeoPoint A = ((AlgoAngularBisectorPoints) gAe).getA();
						GeoPoint B = ((AlgoAngularBisectorPoints) gAe).getB();
						GeoPoint C = ((AlgoAngularBisectorPoints) gAe).getC();
						rel1 = eqangle(A, B, ge, ge, B, C);
					} else if (gAe instanceof AlgoLineBisector) {
						GeoPoint A = ((AlgoLineBisector) gAe).getA();
						GeoPoint B = ((AlgoLineBisector) gAe).getB();
						rel2 = eqangle(ge, A, B, A, B, ge);
					} else {
						// Not yet implemented.
						return null;
					}
				}
			}
			if (hS != null && hE != null) {
				rel2 = collinear(hS, hE, ge);
			} else {
				if (hS != null) {
					AlgoElement hAe = h.getParentAlgorithm();
					if (hAe instanceof AlgoAngularBisectorPoints) {
						GeoPoint A = ((AlgoAngularBisectorPoints) hAe).getA();
						GeoPoint B = ((AlgoAngularBisectorPoints) hAe).getB();
						GeoPoint C = ((AlgoAngularBisectorPoints) hAe).getC();
						rel2 = eqangle(A, B, ge, ge, B, C);
					} else if (hAe instanceof AlgoLineBisector) {
						GeoPoint A = ((AlgoLineBisector) hAe).getA();
						GeoPoint B = ((AlgoLineBisector) hAe).getB();
						rel2 = eqangle(ge, A, B, A, B, ge);
					} else {
						// Not yet implemented.
						return null;
					}
				}
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
				c.realRelation = collinear(gS, gE, ge);
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
						c.realRelation = concyclyc(A, B, C, D);
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
				ae instanceof AlgoAngularBisectorPoints || ae instanceof AlgoLineBisector) {
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
			c.realRelation = concyclyc(A, B, C, D);
			return c;
		}
		if (ae instanceof AlgoAreParallel) {
			AlgoAreParallel aap = (AlgoAreParallel) ae;
			GeoElement[] input = aap.getInput();
			GeoLine g = (GeoLine) input[0];
			GeoLine h = (GeoLine) input[1];
			GeoPoint gS = g.getStartPoint();
			GeoPoint gE = g.getEndPoint();
			GeoPoint hS = h.getStartPoint();
			GeoPoint hE = h.getEndPoint();
			String gSl = getUniqueLabel(gS);
			String gEl = getUniqueLabel(gE);
			String hSl = getUniqueLabel(hS);
			String hEl = getUniqueLabel(hE);
			c.realRelation = "par(" + gSl + "," + gEl + "," + hSl + "," + hEl + ")";
			return c;
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
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		return "coll(" + ge1l + "," + ge2l + "," + ge3l + ")";
	}

	static String concyclyc(GeoElement ge1, GeoElement ge2, GeoElement ge3, GeoElement ge4) {
		String ge1l = getUniqueLabel(ge1);
		String ge2l = getUniqueLabel(ge2);
		String ge3l = getUniqueLabel(ge3);
		String ge4l = getUniqueLabel(ge4);
		return "conc(" + ge1l + "," + ge2l + "," + ge3l + "," + ge4l + ")";
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
}
