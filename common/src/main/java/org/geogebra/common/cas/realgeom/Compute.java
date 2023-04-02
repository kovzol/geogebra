package org.geogebra.common.cas.realgeom;

/*
 * It computes the real geometry problem via Tarski.
 * Most of this piece of code is taken from the RealGeom system.
 */

import static org.geogebra.common.cas.giac.CASgiac.ggbGiac;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.TreeSet;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.util.debug.Log;

public class Compute {

	private static String formulas;
	private static String response;
	private static Kernel k;

	private static String executeGiac(String command) {
		GeoGebraCAS cas = (GeoGebraCAS) k.getGeoGebraCAS();
		try {
			return cas.evaluateRaw(command);
		} catch (Throwable e) {
			Log.error("Error in RealGeom/Compute/executeGiac: input=" + command);
			return "ERROR";
		}
	}

	private static String triangleInequality(String a, String b, String c) {
		return a + "+" + b + ">" + c;
	}

	private static void appendIneqs(String ineq) {
		if (ineq.equals("")) {
			return;
		}

		// Rewrites first:
		ineq = ineq.replaceAll("\\*", " ").replace("and", "/\\").replace("or", "\\/");
		ineq = ineq.replace("&&", "/\\").replace("||", "\\/");

		if (!"".equals(formulas)) {
			formulas += " /\\ ";
		}
		formulas += "(" + ineq + ")";

	}

	private static String eq(String lhs, String rhs) {
		return lhs + "=" + rhs;
	}

	private static String product(String a, String b) {
		return "(" + a + ") (" + b + ")";
	}

	private static void appendResponse(String message) {
		String data = new Timestamp(System.currentTimeMillis()) + " " + message;
		System.out.println(data);
		if (!"".equals(response)) {
			response += "\n";
		}
		response += message;
	}

	private static String rewriteGiac(String formula) {
		// A typical example:
		// m^2 + m - 1 >= 0 /\ m^2 - m - 1 <= 0 /\ [ m^2 - m - 1 = 0 \/ m^2 + m - 1 = 0 ]

		// appendResponse("LOG: formula=" + formula, Log.VERBOSE);
		String[] conjunctions = formula.split(" && ");

		StringBuilder rewritten = new StringBuilder();
		for (String c : conjunctions) {
			if (c.startsWith("[ ") &&
					c.endsWith(" ]")) {
				c = removeHeadTail(c, 2); // trim [ ... ]
			}
			if (c.startsWith("[") &&
					c.endsWith("]")) {
				c = removeHeadTail(c, 1); // trim [...] (for Tarski)
			}
			// appendResponse("LOG: c=" + c, Log.VERBOSE);
			String[] disjunctions = c.split(" \\\\/ ");
			StringBuilder product = new StringBuilder();
			for (String d : disjunctions) {
				// appendResponse("LOG: d=" + d, Log.VERBOSE);
				if (d.endsWith(" = 0")) {
					d = d.substring(0, d.length() - 4); // remove = 0
				}
				if (d.contains("/=")) { // !=
					// Giac currently cannot handle inequalities.
					// So we remove this part and hope for the best.
					d = "";
				} else {
					d = "(" + d + ")";
				}
				product.append(d).append("*");
				// appendResponse("LOG: product=" + product, Log.VERBOSE);
			}
			product = new StringBuilder(product.substring(0, product.length() - 1)); // remove last *
			// appendResponse("LOG: product=" + product, Log.VERBOSE);
			rewritten.append(product).append(",");
			// appendResponse("LOG: rewritten=" + rewritten, Log.VERBOSE);
		}
		rewritten = new StringBuilder(rewritten.substring(0, rewritten.length() - 1)); // remove last ,

		String mathcode = "solve([" + rewritten + "],m)";
		appendResponse("LOG: mathcode=" + mathcode);
		String giacOutput = executeGiac(mathcode);
		// keep only the middle of "list[...]":
		if (giacOutput.contains("list")) {
			giacOutput = giacOutput.replaceAll("list", "");
			giacOutput = removeHeadTail(giacOutput, 1);
		}
		if (giacOutput.contains("rootof")) {
			mathcode = "evalf(" + giacOutput + ")";
			giacOutput = executeGiac(mathcode) + "..."; // this is just an approximation
		}

		// NOT REQUIRED IN REALGEOM (WE ARE IN GEOGEBRA MODE HERE):
		if (giacOutput.startsWith("{") && giacOutput.endsWith("}")) {
			giacOutput = removeHeadTail(giacOutput, 1);
		}
		giacOutput = giacOutput.replaceAll(" && ", " and ");
		if (giacOutput.contains(" and ")) {
			giacOutput = "(" + giacOutput + ")";
		}

		giacOutput = giacOutput.replaceAll("√", "sqrt");
		return giacOutput;
	}

	static String ggInit = "caseval(\"init geogebra\")";

	static String ilsDef() {
		return ggbGiac("isLinearSum" +
				" (poly)->" +
				"{ local degs,vars,ii,ss;" +
				"  vars:=lvar(poly);" +
				"  ii:=1;" +
				"  ss:=size(poly);" +
				"  while(ii<ss){" +
				"      degs:=degree(poly[ii],vars);" +
				"      if ((sum(degs))>1) {" +
				"          return(false);" +
				"        };" +
				"      ii:=ii+1;" +
				"    };" +
				"  return(true);" +
				"}");
	}

	static String dlDef(boolean keep) {
		return ggbGiac(
				"delinearize" +
						" (polys,excludevars)->" +
						"{ local ii,degs,pos,vars,linvar,p,qvar,pos2,keep,cc,substval,substs;" +
						"  keep:=[];" +
						"  substs:=\"\";" +
						"  vars:=lvar(polys);" +
						"  print(\"Input: \"+size(polys)+\" eqs in \"+size(vars)+\" vars\");" +
						"  cc:=1;" +
						"  while(cc<(size(lvar(polys)))){" +
						"      ii:=0;" +
						"      while(ii<(size(polys)-1)){" +
						"          degs:=degree(polys[ii],vars);" +
						"          if ((sum(degs)=cc) && (isLinear(polys[ii]))) {" +
						"              pos:=find(1,degs);" +
						"              if (((size(pos))=cc)) {" +
						"                  p:=0;" +
						"                  linvar:=vars[pos[p]];" +
						"                  while(((is_element(linvar,excludevars)) && (cc>1)) && (p<(size(pos)-1))){" +
						"                      p:=p+1;" +
						"                      linvar:=vars[pos[p]];" +
						"                    };" +
						"                  if ((not(is_element(linvar,excludevars))) || (cc<2)) {" +
						// "                      if (is_element(linvar,excludevars) && (cc>1)) {" +
						"                      if (is_element(linvar,excludevars)) {" +

						(keep ?
								"                      keep:=append(keep,polys[ii]);" +
										"                      print(\"Keeping \" + polys[ii]); "
								:
								"                  print(\"Keeping disabled\");  "
						)
						+
						"                         };" +
						"                      substval:=(op((solve(polys[ii]=0,linvar))[0]))[1];" +
						"                      print(\"Removing \" + polys[ii] + \", substituting \" + linvar + \" by \" + substval);" +
						"                      substs:=substs + linvar + \"=\" + substval + \",\";" +
						"                      polys:=simplify(remove(0,expand(expand(subs(polys,[linvar],[substval])))));" +
						"                      print(\"New set: \" + polys);" +
						"                      vars:=lvar(polys);" +
						"                      ii:=-1;" +
						"                    };" +
						"                };" +
						"            }" +
						// Quadratic check (FIXME: do that only for rational roots):
						"          else {" +
						//"              print(\"ii=\"+ii + \" size=\" + size(polys));" +
						"              if ((sum(degs)=2) && (not(isLinear(polys[ii])))) {" +
						"                  pos2:=find(2,degs);" +
						"                  if (size(pos2)>0) {" +
						"                      qvar:=vars[pos2[0]];" +
						"                      if (is_element(qvar,excludevars)) {" +
						"                          print(\"Considering positive roots of \"+(polys[ii]=0)+\" in variable \"+qvar);" +
						"                          print(solve(polys[ii]=0,qvar));" +
						"                          substval:=rhs((op(solve(polys[ii]=0,qvar)))[1]);" +
						"                          print(\"Positive root is \"+substval);" +
						"                          if (type(substval)==integer || type(substval)==rational) {" +
						"                              polys:=simplify(remove(0,expand(subs(polys,[qvar],[substval]))));" +
						"                              print(\"New set: \" + polys);" +
						(keep ?
								"                      keep:=append(keep,substval-qvar);" +
										"                      print(\"Keeping \" + (substval-qvar)); "
								:
								"                  print(\"Keeping disabled\");  "
						)
						+
						"                              substs:=substs + qvar + \"=\" + substval + \",\";" +
						"                              vars:=lvar(polys);" +
						"                              ii:=-1;" +
						"                            };" +
						"                        };" +
						"                    };" +
						//"                  print(ii);" +
						"                };" +
						"            };" +
						// End of quadratic check.
						"          ii:=ii+1;" +
						"        };" +
						"      cc:=cc+1;" +
						//"      print(cc);" +
						"    };" +
						"  polys:=flatten(append(polys,keep));" +
						"  print(\"Set after delinearization: \" + polys);" +
						"  vars:=lvar(polys);" +
						"  print(\"Delinearization output: \"+size(polys)+\" eqs in \"+size(vars)+\" vars\");" +
						"  return([polys,substs]);" +
						"}");
	}

	static String rdDef() {
		return ggbGiac("removeDivisions" +
				" (polys)->{ local ii;" +
				"             ii:=0;" +
				"             while(ii<(size(polys))) {" +
				//"                 polys[ii]:=expand(lcm(denom(coeff(polys[ii])))*(polys[ii]));" +
				"                 polys[ii]:=numer(simplify(polys[ii]));" +
				"                 ii:=ii+1;" +
				"                 };" +
				"             return(polys);" +
				"        }");
	}

	static String ilDef() {
		return ggbGiac("isLinear" +
				" (poly)->{if (((sommet(poly))=\"+\")) {" +
				"              return(isLinearSum(poly));" +
				"            };" +
				"          return(isLinearSum(poly+1234567));" + // FIXME, this is a dirty hack
				"        }");
	}

	static String rmwDef() {
		return ggbGiac("removeW12" +
				" (polys, m, w1, w2)->{ local ii, vars, w1e, w2e, neweq;" +
				"                 ii:=0;" +
				"                 neweq:=0;" +
				"                 while(ii<(size(polys))) {" +
				"                     vars:=lvar(polys[ii]);" +
				"                     if (vars intersect [w1] != set[] && vars intersect [m] == set[]) {" +
				"                         w1e:=rhs((solve(polys[ii]=0,w1))[0]);" +
				"                         print(\"Remove \" + polys[ii]);" +
				"                         polys:=suppress(polys,ii);" +
				"                         ii:=ii-1;" +
				"                         vars:=[];" +
				"                       };" +
				"                     if (vars intersect [w2] != set[] && vars intersect [m] == set[]) {" +
				"                         w2e:=rhs((solve(polys[ii]=0,w2))[0]);" +
				"                         print(\"Remove \" + polys[ii]);" +
				"                         polys:=suppress(polys,ii);" +
				"                         ii:=ii-1;" +
				"                         vars:=[];" +
				"                       }" +
				"                     ii:=ii+1;" +
				"                   }" +
				"                 ii:=0;" +
				"                 while(ii<(size(polys))) {" +
				"                     vars:=lvar(polys[ii]);" +
				"                     if (vars intersect [m] == set[m]) {" +
				"                         print(\"Remove \" + polys[ii]);" +
				"                         polys:=suppress(polys,ii);" +
				"                         neweq:=(w1e)-m*(w2e);" +
				"                         ii:=ii-1;" +
				"                         vars:=[];" +
				"                       }" +
				"                     ii:=ii+1;" +
				"                   };" +
				"                 if (neweq != 0) {" +
				"                     print(\"Add \" + neweq);" +
				"                     polys:=flatten(append(polys,neweq));" +
				"                   }" +
				"                 return(polys);" +
				"               }");
	}

	/**
	 * Solve a problem with coordinates.
	 */

	public static String euclideanSolverExplore(Kernel kernel, String lhs, String rhs, String ineqs, String polys,
			String triangles, String vars, String posvariables) {
		k = kernel;
		String m = "m"; // TODO: Use a different dummy variable
		String code;
		formulas = "";
		response = "";

		if (!"".equals(triangles)) {
			String[] trianglesArray = triangles.split(";");
			for (String s : trianglesArray) {
				String[] variables = s.split(",");
				appendIneqs(triangleInequality(variables[0], variables[1], variables[2]));
				appendIneqs(triangleInequality(variables[1], variables[2], variables[0]));
				appendIneqs(triangleInequality(variables[2], variables[0], variables[1]));
			}
		}

		String eq = "(" + lhs + ")-m*(" + rhs + ")";
		appendResponse("LOG: ineqs=" + formulas);

		String[] varsArray = vars.split(",");
		StringBuilder varsubst = new StringBuilder();
		for (int i = 0; i < Math.min(varsArray.length, 4); ++i) {
			int value = 0;
			if (i == 2)
				value = 1;
			// 0,0,1,0 according to (0,0) and (1,0)
			if (i > 0)
				varsubst.append(",");
			varsubst.append(varsArray[i]).append("=").append(value);
		} // in varsubst we have something like a1=0, a2=0, b1=1, b2=0

		String ineqs2 = "";
		if (!ineqs.equals("")) {
			// quote is a workaround here, because gwt-giac may crash on simple inequalities
			// like x+x*y>0 -- see also some other instances below
			ineqs2 = executeGiac("subst(quote([" + ineqs + "]),[" + varsubst + "])");
			if (ineqs2.equals("ERROR")) {
				return "GIAC ERROR";
			}
			ineqs2 = removeHeadTail(ineqs2, 1);
		}

		String ineqVars = "";
		String[] ineqs2Array = ineqs2.split(",");
		if (!ineqs2.equals("")) {
			for (String ie : ineqs2Array) {
				String[] disjunctionsArray = ie.split(" \\|\\| ");
				for (String d : disjunctionsArray) {
					String[] conjunctionsArray = d.split(" \\&\\& ");
					for (String c : conjunctionsArray) {
						// This is very hacky, and in some cases, maybe incorrect...
						String ieRewriteEq = c.replace(">", "=").replace("<", "=")
								.replace("==", "=").replace("(", ""). replace(")", "");
						String ieVarsCode = "lvar(lhs(" + ieRewriteEq + "),rhs(" + ieRewriteEq + "))";
						String ieVars = executeGiac(ieVarsCode);
						ieVars = removeHeadTail(ieVars, 1);
						ineqVars += "," + ieVars;
					}
				}
			}
		}

		appendResponse("LOG: before substitution, polys=" + polys + ", ineqs=" + ineqs2);
		String polys2 = executeGiac("subst([" + polys + "],[" + varsubst + "])");

		polys2 = removeHeadTail(polys2, 1); // removing { and } in Mathematica (or [ and ] in Giac)

		// Add main equation:
		polys2 += "," + eq;
		appendResponse("LOG: before delinearization, polys=" + polys2);
		String linCode = "[[" + ggInit + "],[" + ilsDef() + "],[" + ilDef() + "],[" + dlDef(true) + "],[" + rmwDef() +
				"],[" + rdDef() + "],";
		if (lhs.equals("w1") && rhs.equals("w2")) {
			linCode += "removeDivisions(removeW12(delinearize([" + polys2 + "],[" + posvariables + ineqVars + ",w1,w2])[0],m,w1,w2))][6]";
		} else {
			linCode += "removeDivisions(delinearize([" + polys2 + "],[" + posvariables + ineqVars + "," + lhs + "," + rhs + "])[0])][6]";
		}
		appendResponse("LOG: delinearization code=" + linCode);
		polys2 = executeGiac(linCode);
		if (polys2.equals("0")) {
			appendResponse("ERROR: Giac returned 0");
			appendResponse("GIAC ERROR");
			return response;
		}
		appendResponse("LOG: after delinearization, polys=" + polys2);
		appendResponse("LOG: before removing unnecessary variables, vars=" + vars);
		polys2 = removeHeadTail(polys2, 1); // removing { and } in Mathematica (or [ and ] in Giac)
		String minimVarsCode = "lvar([" + polys2 + "])"; // remove unnecessary variables
		vars = executeGiac(minimVarsCode);
		appendResponse("LOG: after removing unnecessary variables, vars=" + vars);
		vars = removeHeadTail(vars, 1); // removing { and } in Mathematica (or [ and ] in Giac)
		// Remove m from vars (but keep it only if there is no other variable):
		vars = vars.replace(",m", "").replace("m,", "");
		appendResponse("LOG: after removing m, vars=" + vars);
		varsArray = vars.split(",");

		String[] posvariablesArray = posvariables.split(",");
		for (String item : posvariablesArray) {
			if (Arrays.asList(varsArray).contains(item)) appendIneqs(item + ">0");
		}

		String[] polys2Array = polys2.split(",");

		if (!ineqVars.equals("")) {
			vars += ineqVars;
		}

		// Remove duplicated vars.
		// Even this can be improved by rechecking all polys/ineqs/ineq:
		TreeSet<String> varsSet = new TreeSet<>();
		varsArray = vars.split(",");
		for (String v : varsArray) {
			varsSet.add(v);
		}
		vars = "";
		for (String v : varsSet) {
				vars += v + ",";
			}
		if (!vars.equals("")) {
			vars = vars.substring(0, vars.length() - 1); // remove last , if exists
		}

		// FINAL COMPUTATION.


		// Remove m completely:
		vars = vars.replace("m", "");

		for (String s : polys2Array) appendIneqs(s + "=0");
		for (String s : ineqs2Array) appendIneqs(s);

		String result;
		int expectedLines;
		code = epcDef() + "(epc [ ex " + vars + " [" + formulas + "]])";;
		expectedLines = 4;

		appendResponse("LOG: code=" + code);

		result = k.getApplication().tarski.evalCached(code);

		if (result.contains("\n")) {
			String [] resultlines = result.split("\n");
			result = resultlines[resultlines.length - 1]; // IMPORTANT: THIS IS " - 2 " IN REALGEOM!
			// IMPORTANT: THIS IS NOT REQUIRED IN REALGEOM
			result = getTarskiOutput(result);
		}
		if (result.equals("")) {
			appendResponse("ERROR: empty output");
			appendResponse("TARSKI ERROR");
			return "TARSKI ERROR";
		}
		appendResponse("LOG: result=" + result);
		if (result.contains("error") || result.contains("failure")) { // TODO: do it properly
			appendResponse("TARSKI ERROR");
			return "TARSKI ERROR";
		}
		if (result.equals("TRUE")) {
			// No usable answer is received (m is arbitrary)
			appendResponse("m>0");
			return "m>0";
		}
		// hacky way to convert QEPCAD formula to Mathematica formula FIXME
		String rewrite = result.replace("/\\", "&&").
				replace(">==", ">=").replace("<==", "<=").
				replace("TRUE", "1=1");

		// add missing condition to output
		rewrite += " && m>0";

		String real = rewriteGiac(rewrite);
		appendResponse(real);

		return real;
	}

	/* Prove an inequality with coordinates. */
	// Consider unifying this with euclideanSoverExplore.
	public static String euclideanSolverProve(Kernel kernel, int maxfixcoords, String ineq, String ineqs, String polys,
			String triangles, String vars, String posvariables, String freevars) {

		k = kernel;
		String code;
		formulas = "";
		response = "";

		if (!"".equals(triangles)) {
			String[] trianglesArray = triangles.split(";");
			for (String s : trianglesArray) {
				String[] variables = s.split(",");
				appendIneqs(triangleInequality(variables[0], variables[1], variables[2]));
				appendIneqs(triangleInequality(variables[1], variables[2], variables[0]));
				appendIneqs(triangleInequality(variables[2], variables[0], variables[1]));
			}
		}

		String[] varsArray = vars.split(",");
		StringBuilder varsubst = new StringBuilder();
		appendResponse("LOG: maxfixcoords=" + maxfixcoords);
		for (int i = 0; i < Math.min(varsArray.length, maxfixcoords); ++i) {
			int value = 0;
			if (i == 2)
				value = 1;
			// 0,0,1,0 according to (0,0) and (1,0)
			if (i > 0)
				varsubst.append(",");
			varsubst.append(varsArray[i]).append("=").append(value);
		} // in varsubst we have something like a1=0, a2=0, b1=1, b2=0
		appendResponse("LOG: before substitution, polys=" + polys + ", ineqs=" + ineqs + ", ineq=" + ineq);
		String polys2 = executeGiac("subst([" + polys + "],[" + varsubst + "])");
		polys2 = removeHeadTail(polys2, 1); // removing [ and ]

		String ineqs2 = "";
		if (!ineqs.equals("")) {
			ineqs2 = executeGiac("subst(quote([" + ineqs + "]),[" + varsubst + "])");
			if (ineqs2.equals("ERROR")) {
				return "GIAC ERROR";
			}
			ineqs2 = removeHeadTail(ineqs2, 1);
		}

		String ineq2 = executeGiac("subst(quote([" + ineq + "]),[" + varsubst + "])");
		if (ineq2.equals("ERROR")) {
			return "GIAC ERROR";
		}
		ineq2 = removeHeadTail(ineq2, 1);

		appendResponse("LOG: after substitution, polys=" + polys2+ ", ineqs=" + ineqs2 + ", ineq=" + ineq2);
		boolean keep = false;

		String linCode = "[[" + ggInit + "],[" + ilsDef() + "],[" + ilDef() + "],[" + dlDef(keep) + "],[" + rdDef() + "],";

		// Collect variables from inequalities.

		String ineqRewriteEq = ineq2.replace(">", "=").replace("<", "=")
				.replace("==", "=");
		String ineqVarsCode = "lvar(lhs(" + ineqRewriteEq + "),rhs(" + ineqRewriteEq + "))";
		String ineqVars = executeGiac(ineqVarsCode);
		ineqVars = removeHeadTail(ineqVars, 1);

		String[] ineqs2Array = ineqs2.split(",");
		if (!ineqs2.equals("")) {
			for (String ie : ineqs2Array) {
				String[] disjunctionsArray = ie.split(" \\|\\| ");
				for (String d : disjunctionsArray) {
					String[] conjunctionsArray = d.split(" \\&\\& ");
					for (String c : conjunctionsArray) {
						// This is very hacky, and in some cases, maybe incorrect...
						String ieRewriteEq = c.replace(">", "=").replace("<", "=")
								.replace("==", "=").replace("(", ""). replace(")", "");
						String ieVarsCode = "lvar(lhs(" + ieRewriteEq + "),rhs(" + ieRewriteEq + "))";
						String ieVars = executeGiac(ieVarsCode);
						ieVars = removeHeadTail(ieVars, 1);
						ineqVars += "," + ieVars;
					}
				}
			}
		}

		// End collecting variables.

		// linCode += "removeDivisions(delinearize([" + polys2 + "],[" + posvariables + "]))][5]";
		// linCode += "removeDivisions(delinearize([" + polys2 + "],[" + posvariables + ineqVars + "])[0])][5]";
		linCode += "[dl:=delinearize([" + polys2 + "],[" + posvariables + ineqVars + "])],removeDivisions(dl[0]),dl[1]][6..7]";

		// linCode += "removeDivisions([" + polys2 + "],[" + posvariables + "])][5]";
		appendResponse("LOG: delinearization code=" + linCode);
		String polys_substs = executeGiac(linCode);
		if (polys_substs.equals("0")) {
			appendResponse("ERROR: Giac returned 0");
			appendResponse("GIAC ERROR");
			return "GIAC ERROR";
		}
		appendResponse("LOG: after delinearization, {polys,substs}=" + polys_substs);
		appendResponse("LOG: before removing unnecessary poly variables, vars=" + vars);
		// {{-v11^2+2*v11*v9-v9^2+v15^2-1,v16^2-1,-v5+1,-v6+1,v7,v8-1,-v12+1,v10},"v5=1,v6=1,v7=0,v8=1,v12=1,v10=0,"}
		polys_substs = removeHeadTail(polys_substs, 1); // removing { and } in Mathematica (or [ and ] in Giac)
		// {-v11^2+2*v11*v9-v9^2+v15^2-1,v16^2-1,-v5+1,-v6+1,v7,v8-1,-v12+1,v10},"v5=1,v6=1,v7=0,v8=1,v12=1,v10=0,"
		int split = polys_substs.indexOf("}");
		polys2 = polys_substs.substring(1, split);
		appendResponse("LOG: polys after split=" + polys2);
		String substs = polys_substs.substring(split + 3, polys_substs.length() - 1);
		if (!substs.equals("")) {
			substs = substs.substring(0, substs.length() - 1); // remove last , if exists
		}
		appendResponse("LOG: substs after split=" + substs);
		String minimVarsCode = "lvar([" + polys2 + "])"; // remove unnecessary variables
		vars = executeGiac(minimVarsCode);
		appendResponse("LOG: after removing unnecessary poly variables, vars=" + vars);
		vars = removeHeadTail(vars, 1); // removing { and } in Mathematica (or [ and ] in Giac)
		varsArray = vars.split(",");

		String[] posvariablesArray = posvariables.split(",");
		for (String item : posvariablesArray) {
			// FIXME: Make a distinction between variables like sqrt2 and the other ones that can be eliminated.
			// if (Arrays.asList(varsArray).contains(item))
			appendIneqs(item + ">=0");
			if (!Arrays.asList(varsArray).contains(item)) {
				vars += "," + item;
			}
		}
		vars += "," + ineqVars;

		String[] polys2Array = polys2.split(",");

		if (!substs.equals("")) {
			String[] substsArray = substs.split(",");
			for (String s : substsArray) {
				appendIneqs(s);
				String[] substitution = s.split("=");
				vars += "," + substitution[0];
			}
		}

		if (!ineqs2.equals("")) {
			for (String s : ineqs2Array) {
				if (!substs.equals("")) {
					s = executeGiac("subst([" + s + "],[" + substs + "])");
					s = removeHeadTail(s, 1);
				}
				appendIneqs(s);
			}
		}
		if (!substs.equals("")) {
			ineq = executeGiac("subst(quote([" + ineq + "]),[" + substs + "])");
			if (ineq.equals("ERROR")) {
				return "GIAC ERROR";
			}
			ineq = removeHeadTail(ineq, 1);
		}
		// If ineq contains "true", an internal issue in Tarski's code prevents getting
		// the required answer. Here we use a workaround, and it actually speeds up
		// computation for such a case.
		if ("true".equals(ineq)) {
			return "false"; // negation required
		}
		appendIneqs("~(" + ineq + ")");

		// Remove duplicated vars.
		// Even this can be improved by rechecking all polys/ineqs/ineq:
		TreeSet<String> varsSet = new TreeSet<>();
		varsArray = vars.split(",");
		for (String v : varsArray) {
			varsSet.add(v);
		}

		// Remove freevars:
		String[] freevarsArray = freevars.split(",");
		for (String v : freevarsArray) {
			varsSet.remove(v);
		}

		vars = "";
		for (String v : varsSet) {
			vars += v + ",";
		}
		if (!vars.equals("")) {
			vars = vars.substring(0, vars.length() - 1); // remove last , if exists
		}

		for (String s : polys2Array) appendIneqs(s + "=0");
		String result;

		code = epcDef() + " (epc [ex " + vars + " [" + formulas + "]])";
		int expectedLines = 4;

		appendResponse("LOG: code=" + code);
		result = k.getApplication().tarski.evalCached(code);

		if (result.contains("\n")) {
			String [] resultlines = result.split("\n");
			result = resultlines[resultlines.length - 1]; // IMPORTANT: THIS IS " - 2 " IN REALGEOM!
			// IMPORTANT: THIS IS NOT REQUIRED IN REALGEOM
			result = getTarskiOutput(result);
		}
		appendResponse("LOG: result=" + result);
		appendResponse(result);

		return result;
	}

	static String removeHeadTail(String input, int length) {
		if (input.length() >= 2 * length) {
			return input.substring(length, input.length() - length);
		}
		return input;
	}

	private static String epcDef() {
		String qc;
		qc = "qepcad-api-call";
		/*
		return // "; (process F) - assumes F is prenex conjunction, variable m is free, all others existentially quantified\n" +
				// "; returns quantifier-free equivalent to F\n" +
				"(def process (lambda (F) (def L (getargs F)) (def V (get L 0 0 1)) (def B (bbwb (get L 1)))" +
						" (if (equal? (get B 0) 'UNSAT) [false] ((lambda () (def G (qfr (t-ex V (get B 1))))" +
						" (if (equal? (t-type G) 1) G (if (equal? (t-type G) 6) (" + qc + " G 'T)" +
						" (if (equal? (t-type G) 5) (" + qc + " (bin-reduce t-or (map (lambda (H) (" + qc +
						" (exclose H '(m)) 'T)) (getargs G))) 'T) (" + qc + " G 'T))))))))) " +
						// "\n" +
						// "; (expand F) - assumes F is prenex, variable m is free, all others existentially quantified (ors may appear!)\n" +
						// "; returns list L of conjunctions s.t. the or of elts of L is equivalent to F\n" +
						"(def expand (lambda (F)" +
						"      (def A (getargs F))" +
						"      (def V (get A 0 0 1))" +
						"      (def G (get A 1))" +
						"      (def X (dnf G))" +
						"      (def L (if (equal? (t-type X) 5) (getargs X) (list X)))" +
						"      (map (lambda (f) (exclose f '(m))) L) ))" +
						// "\n" +
						// "; (epc F) - assumes F is prenex, variable m is free, all others existentially quantified (ors may appear!)\n" +
						// "; returns quantifier-free equivalent to F.  NOTE: epc stands for \"expand - process - combine\", which is what this does\n" +
						"(def epc (lambda (F) (normalize (bin-reduce t-or (map (lambda (G) (if (equal? (t-type G) 6) (process G) G)) (expand F))))))";
		// "\n" +
		// "; NOTE: epc doesn't handle edge cases like F := [true] / [false] / [ex x[true]] / [ex x[false]]";
		 */

		/*
		return "; (process F) - F is an existentially quantified formula\n"
				+ "; 1. Let F' be the quantifier-free part of F and do BBWB simplification on F'\n"
				+ "; 2. if result is FALSE, return\n"
				+ "; 3. do \"quantified formula rewrite\", which is repeated linear substitutions for quantified variables followed by simplifications\n"
				+ ";    which results in a disjunction of quantified formulas\n"
				+ "; 4. call qepcad on each formula independently to eliminate quanfied variables and return a list of the resulting formulas\n"
				+ "(def process\n"
				+ "     (lambda (F)\n"
				+ "       (def L (getargs F))\n"
				+ "       (def V (get L 0 0 1))\n"
				+ "       (def B (bbwb (get L 1)))\n"
				+ "       (if (equal? (get B 0) 'UNSAT)\n"
				+ "    [false]\n"
				+ "    (\n"
				+ "      (lambda ()\n"
				+ "        (def G (qfr (t-ex V (get B 1))))\n"
				+ "        (if (equal? (t-type G) 1)\n"
				+ "        G\n"
				+ "        (if (equal? (t-type G) 6)\n"
				+ "              (qepcad-api-call G 'T)\n"
				+ "              (if (equal? (t-type G) 5)\n"
				+ "                (qepcad-api-call (bin-reduce t-or (map (lambda (H) (qepcad-api-call (t-ex V H) 'T)) (getargs G))) 'T) (qepcad-api-call G 'T))))\n"
				+ "            )))))\n"
				+ "\n"
				+ "; (expand F) - F is an existentially quantified formula\n"
				+ "; 1. Take the quantifier-free part of F and expand into DNF F1 \\/ F2 \\/ ... \\/ Fk\n"
				+ "; 2. For each Fi reintroduce the quantified variables from F and call the result Gi\n"
				+ "; 3. Return (G1 G2 ... Gk)\n"
				+ "(def expand\n"
				+ "     (lambda (F)\n"
				+ "       (def A (getargs F))\n"
				+ "       (def V (get A 0 0 1))\n"
				+ "       (def G (get A 1))\n"
				+ "       (def X (dnf G))\n"
				+ "       (def L (if (equal? (t-type X) 5) (getargs X) (list X)))\n"
				+ "       (map (lambda (f) (t-ex V f)) L) ))\n"
				+ "\n"
				+ "; (epc F) - F is an existentially quantified formula\n"
				+ "; 1. L = (expand F)\n"
				+ "; 2. Lp = list resulting from applying process to each of the elements of L and collecting the result\n"
				+ "; 3. \"or\" the elements of Lp all together and simplify the result\n"
				+ "(def epc (lambda (F) (normalize (bin-reduce t-or (map (lambda (G) (if (equal? (t-type G) 6) (process G) G)) (expand F))))))\n";
		 */

		return "; epcx - version 4.0 2023-03-28 - Chris Brown\n"
				+ "; This script defines a new Tarski command \"epc\" that provides a better \"black box\"\n"
				+ "; command for existential quantifier elimination.\n"
				+ "; Use like: (epc [ex x[x^2 + b x + c = 0]])\n"
				+ "; IMPORTANT! Only call epc on a prenex, existentially quantified formula.\n"
				+ ";            I.e. when you call (epc F), the formula F should look like\n"
				+ ";                 [ex x1,...,xn[ F ] ], where F is quantifier-free.\n"
				+ ";            \n"
				+ "\n"
				+ "; (qepcad-ex-elim F t)  Qepcad existential quantifier elimination\n"
				+ "; Inputs  F: a existentially quantified prenex conjunction (or constant or quantifier-free conjunction)\n"
				+ ";         t: either 'T or 'E, the formula output flag for qepcad-api-call\n"
				+ "; This will test satisfiability first, and only try to do qe if satisfiability fails.\n"
				+ "; The rationale for this is that there is a lot more variable order flexibility for\n"
				+ "; qepcad, and also rewriting options for qfr for a fully quantified formula, so the\n"
				+ "; computing time is likely to be substantially less for checking satisfiability than\n"
				+ "; for doing quantifier elimination\n"
				+ "(def qepcad-ex-elim\n"
				+ "  (lambda (F t)\n"
				+ "    (if (equal? (t-type F) 1) ; if F is constant\n"
				+ "      F\n"
				+ "      (if (equal? (length (get-free-vars F)) 0)\n"
				+ "        (qepcad-api-call F t)\n"
				+ "        ((lambda () ; let hack\n"
				+ "           (def S (process (exclose F)))\n"
				+ "           (if (equal? S [false])\n"
				+ "             [false]\n"
				+ "             (qepcad-api-call F t)\n"
				+ "\t   )))))))\n"
				+ "\n"
				+ "; (qepcad-simplify F t) Qepcad simplification\n"
				+ "(def qepcad-simplify (lambda (F t) (qepcad-api-call F t)))\n"
				+ "\n"
				+ "; (process F) - F is an existentially quantified formula\n"
				+ "; 1. Let F' be the quantifier-free part of F and do fast simplification on F'\n"
				+ "; 2. if result is FALSE, return \n"
				+ "; 3. do \"quantified formula rewrite\", which is repeated linear substitutions for quantified variables followed by simplifications\n"
				+ ";    which results in a disjunction of quantified formulas\n"
				+ "; 4. call qepcad on each formula independently to eliminate quanfied variables and return a list of the resulting formulas\n"
				+ "(def process\n"
				+ "     (lambda (F)\n"
				+ "       (def L (getargs F))\n"
				+ "       (def V (get L 0 0 1))\n"
				+ "       (def B (normalize (get L 1)))\n"
				+ "       (if (equal? (t-type B) 1) ; is B a T/F constant?\n"
				+ "          B\n"
				+ "          (\n"
				+ "            (lambda () ; This is a funky way to simulate a block of statements\n"
				+ "              (def G (qfr (t-ex V B)))\n"
				+ "              (if (equal? (t-type G) 1) ; is G a T/F constant ?\n"
				+ "                G\n"
				+ "                (if (equal? (t-type G) 6) ; is G of the form [ex x1,...,xn [ Conjunction ]]?\n"
				+ "                   (qepcad-simplify (qepcad-ex-elim G 'T) 'T) ; inner call does QE, outer call simplifies result (e.g. if there are assumptions)\n"
				+ "                   (if (equal? (t-type G) 5) ; is G a disjunction of formulas?\n"
				+ "                       (qepcad-simplify (bin-reduce t-or (map (lambda (H) (qepcad-ex-elim H 'T)) (getargs G))) 'T)\n"
				+ "                        ; quantifier-free atom or conjunction\n"
				+ "                       (qepcad-simplify G 'T)\n"
				+ "    ))))))))\n"
				+ "\n"
				+ "; (expand F) - F is an existentially quantified formula\n"
				+ "; 1. Take the quantifier-free part of F and expand into DNF F1 \\/ F2 \\/ ... \\/ Fk\n"
				+ "; 2. For each Fi reintroduce the quantified variables from F and call the result Gi\n"
				+ "; 3. Return (G1 G2 ... Gk)\n"
				+ "(def expand\n"
				+ "     (lambda (F)\n"
				+ "       (def A (getargs F))\n"
				+ "       (def V (get A 0 0 1))\n"
				+ "       (def G (get A 1))\n"
				+ "       (def X (dnf G))\n"
				+ "       (def L (if (equal? (t-type X) 5) (getargs X) (list X)))\n"
				+ "       (map (lambda (f) (t-ex V f)) L) ))\n"
				+ "\n"
				+ "; (epc F) - F is an existentially quantified formula\n"
				+ "; 1. L = (expand F)\n"
				+ "; 2. Lp = list resulting from applying process to each of the elements of L and collecting the result\n"
				+ "; 3. \"or\" the elements of Lp all together and simplify the result\n"
				+ "(def epc (lambda (F) (normalize (bin-reduce t-or (map (lambda (G) (if (equal? (t-type G) 6) (process G) G)) (expand F))))))";
	}

	// Taken from RealGeom's ExternalCAS:
	public static String getTarskiOutput(String line) {
		if (line.endsWith(":err")) {
			return "";
		}
		int semicolon = line.indexOf(":");
		if (semicolon > -1) {
			String content = line.substring(0, semicolon);
			if (content.startsWith("[") || content.startsWith("\"")) {
				content = content.substring(1, content.length() - 1); // trim [ and ], and "s
			}
			return content;
		}
		return "";
	}

}