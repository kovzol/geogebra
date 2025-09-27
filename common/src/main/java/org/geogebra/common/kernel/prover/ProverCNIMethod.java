package org.geogebra.common.kernel.prover;

import java.util.TreeSet;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.algos.AlgoJoinPointsSegment;
import org.geogebra.common.kernel.algos.AlgoMidpoint;
import org.geogebra.common.kernel.algos.AlgoMidpointSegment;
import org.geogebra.common.kernel.algos.AlgoPolygon;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
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
				CNIDefinition def = getCNIHypothesisDefinition(ge);
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

		String[] predefinitions = {"coll(A_,B_,C_):=(A_-B_)/(A_-C_)"};

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
		program += "[" + VARIABLE_I_STRING + ":=eliminate([" + realRelations + "],";
		String toEliminate = "";
		for (GeoElement ge : freePoints) {
			toEliminate += getUniqueLabel(ge) + ",";
		}
		for (GeoElement ge : realRelationalPoints) {
			toEliminate += getUniqueLabel(ge) + ",";
		}
		toEliminate = removeTail(toEliminate, 1);
		program += "[" + toEliminate + "])]";
		int codeLengthLines = predefinitions.length + declarationsA.length + 1;
		program += "][" + (codeLengthLines - 1) + "]";
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
				+ "[for (k:=0;k<size(" + VARIABLE_I_STRING + ");k++) { if (degree("
					+ VARIABLE_I_STRING + "[k],r_)<deg) { deg:=degree("
					+ VARIABLE_I_STRING + "[k],r_); degi:=k; } }],"
				+ "[deg," + VARIABLE_I_STRING + "[degi]]][4]";
		program = ggbGiac(program);
		String minDegree = executeGiac(program);
		// The result is in form: {1,4*r_1*r_2*r_-4*r_1*r_2-4*r_1*r_-4*r_2*r_+3*r_1+3*r_2+3*r_}

		String minDegreeC = removeHeadTail(minDegree,1); // remove { and }
		String[] minDegreeA = minDegreeC.split(","); // Separate items
		int minDegreeI = Integer.valueOf(minDegreeA[0]);
		if (minDegreeI == 1) {
			// r can be expressed by using r1, r2, ..., here r is linear.
			// The statement is true.
			Log.debug("The elimination ideal contains " + minDegreeA[1] + ", it is linear in r_.");
			return ProofResult.TRUE;
		}

		return ProofResult.UNKNOWN;

	}

	/** Create the CNI definition for a GeoElement (for a hypothesis).
	 * Compute the rhs of the declaration String, the lhs of the real relation String,
	 * and return them to the caller. This method should cover all algos sooner or later.
	 * Now it is just a prototype that implements the CNI method some frequently used algos.
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
			GeoPoint gS = g.getStartPoint();
			GeoPoint gE = g.getEndPoint();
			GeoPoint hS = h.getStartPoint();
			GeoPoint hE = h.getEndPoint();
			String gSl = getUniqueLabel(gS);
			String gEl = getUniqueLabel(gE);
			String hSl = getUniqueLabel(hS);
			String hEl = getUniqueLabel(hE);
			c.realRelation = "coll(" + gSl + "," + gEl + "," + gel + ")\n" +
					"coll(" + hSl + "," + hEl + "," + gel + ")";
			return c;
		}
		if (ae instanceof AlgoPolygon || ae instanceof AlgoJoinPointsSegment) {
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
			String Al = getUniqueLabel(A);
			String Bl = getUniqueLabel(B);
			String Cl = getUniqueLabel(C);
			c.realRelation = "coll(" + Al + "," + Bl + "," + Cl + ")";
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

}
