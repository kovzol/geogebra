package org.geogebra.common.export;

import java.util.HashSet;
import java.util.Map;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.SymbolicMode;
import org.geogebra.common.kernel.geos.GeoElement;

import edu.umd.cs.findbugs.annotations.Nullable;

final class MapleCommandTranslator {

	private MapleCommandTranslator() {
		// utility class
	}

	static String translateSolve(Command command) {
		int numOfArguments = command.getArgumentNumber();

		if (numOfArguments == 2) {
			String eqs = getArgumentOfCommand(command,0);
			String vars = String.valueOf(command.getArgument(1));
			vars = vars.replace("{", "[");
			vars = vars.replace("}", "]");
			return "solve(" + eqs + "," + vars + ")";
		}

		return null;
	}

	// need to handle the cases of factor polynom
	static String translateFactor(Command command) {
		int numOfArguments = command.getArgumentNumber();

		if (numOfArguments == 1) { // two numOfArguments are not implemented, TODO
			// Maple does not have an option to have a second argument
			String expr = command.getArgument(0).toString();
			if (expr.startsWith("$")) {
				// This is something like $1, so we convert it into something like !1:
				expr = "!" + expr.substring(1);
			} else {
				expr = getArgumentOfCommand(command,0);
			}
			return "ifactor(" + expr + ")";
		}

		return null;
	}

	static String translateDerivative(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();

		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// if there is only one argument then the command form is Derivative( <Function> )
		if (numOfArguments == 1) {
			return "diff(" + expression + ",x)";
		}

		// if there are two arguments then the command form is either
		// Derivative( <Function>, <Number> ) or Derivative( <Expression>, <Variable> )
		if (numOfArguments == 2) {
			String secondArg = getArgumentOfCommand(command, 1);

			// try to convert the second arg to number
			// in case the command form is Derivative( <Function>, <Number> )
			try {
				int order = Integer.parseInt(secondArg);
				return "diff(" + expression + ",x$" + order + ")";
			}
			// in case the command form is Derivative( <Expression>, <Variable> )
			catch (NumberFormatException e) {
				return "diff(" + expression + "," + secondArg + ")";
			}
		}

		// if there are three arguments then the command form is Derivative( <Expression>, <Variable>, <Number> )
		if (numOfArguments == 3) {
			String varName = getArgumentOfCommand(command, 1);
			String order = getArgumentOfCommand(command, 2);
			return "diff(" + expression + "," + varName + "$" + order + ")";
		}

		return null;
	}

	static String translateEliminate(Command command) {
		int numOfArguments = command.getArgumentNumber();
		String vars = "[";
		if (numOfArguments == 2) {
			String eqs = getArgumentOfCommand(command, 0);
			ExpressionNode equationsEN = command.getArgument(0);
			ExpressionNode variablesEN = command.getArgument(1);
			HashSet<GeoElement>
					variables = variablesEN.getVariables(SymbolicMode.SYMBOLIC);
			HashSet<String> variablesS = new HashSet<>();
			for (GeoElement ge : variables) {
				variablesS.add(ge.toString());
			}
			for (GeoElement eqVar : equationsEN.getVariables(SymbolicMode.SYMBOLIC)) {
				if (!variablesS.contains(eqVar.toString())) {
					if (vars.length() > 1) {
						vars += ",";
					}
					vars += eqVar;
				}
			}
			vars += "]";
			return "eliminate(" + eqs + "," + vars + ")";
		}

		return null;
	}

	static String translateIntegral(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();

		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// if there is only one argument then the command form is Integral( <Function> )
		if (numOfArguments == 1) {
			return "int(" + expression + ",x)";
		}

		// if there are two arguments then the command form is Integral( <Function>, <Variable> )
		if (numOfArguments == 2) {
			String varName = getArgumentOfCommand(command, 1);
			return "int(" + expression + "," + varName + ")";
		}

		// if there are three arguments then the command form is Integral( <Function>, <Start x-Value>, <End x-Value> )
		if (numOfArguments == 3) {
			String startValue = getArgumentOfCommand(command, 1);
			String endValue = getArgumentOfCommand(command, 2);
			return "int(" + expression + ",x=" + startValue + ".." + endValue + ")";
		}

		// if there are four arguments then the command form is Integral( <Function>, <Variable>, <Start Value>, <End Value> )
		if (numOfArguments == 4) {
			String varName = getArgumentOfCommand(command, 1);
			String startValue = getArgumentOfCommand(command, 2);
			String endValue = getArgumentOfCommand(command, 3);
			return "int(" + expression + "," + varName + "=" + startValue + ".." + endValue + ")";
		}

		return null;
	}

	static String translateIntegralBetween(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();

		String upperFunction = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);
		String lowerFunction = getMapleName(getArgumentOfCommand(command, 1), fullNameToShortName);
		String expression = "(" + upperFunction + ")-(" + lowerFunction + ")";

		// if there are four arguments then the command form is IntegralBetween( <Function>, <Function>, <Number>, <Number> )
		if (numOfArguments == 4) {
			String startValue = getArgumentOfCommand(command, 2);
			String endValue = getArgumentOfCommand(command, 3);
			return "int(" + expression + ",x=" + startValue + ".." + endValue + ")";
		}

		// if there are five arguments then the command form is IntegralBetween( <Function>, <Function>, <Variable>, <Number>, <Number> )
		if (numOfArguments == 5) {
			String varName = getArgumentOfCommand(command, 2);
			String startValue = getArgumentOfCommand(command, 3);
			String endValue = getArgumentOfCommand(command, 4);
			return "int(" + expression + "," + varName + "=" + startValue + ".." + endValue + ")";
		}

		return null;
	}

	static String translateLimit(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// if there are two arguments then the command form is Limit( <Expression>, <Value> )
		if (numOfArguments == 2) {
			String approachTo = fixPiAppear(getArgumentOfCommand(command, 1));
			return "limit(" + expression + ",x=" + approachTo + ")";
		}

		// if there are three arguments then the command form is Limit( <Expression>, <Variable>, <Value> )
		if (numOfArguments == 3) {
			String varName = getArgumentOfCommand(command, 1);
			String approachTo = fixPiAppear(getArgumentOfCommand(command, 2));
			return "limit(" + expression + "," + varName + "=" + approachTo + ")";
		}

		return null;
	}

	static String translateCurveCartesian(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();

		String xExpression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);
		String yExpression = getMapleName(getArgumentOfCommand(command, 1), fullNameToShortName);
		String varName = getArgumentOfCommand(command, 2);
		String startValue = fixPiAppear(getArgumentOfCommand(command, 3));
		String endValue = fixPiAppear(getArgumentOfCommand(command, 4));

		// if there are five arguments then the command form is Curve( <Expression>, <Expression>, <Parameter Variable>, <Start Value>, <End Value> )
		if (numOfArguments == 5) {
			return "plot([" + xExpression + "," + yExpression + ","
					+ varName + "=" + startValue + ".." + endValue + "])";
		}

		return null;
	}

	static String translateDegree(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// if there is only one argument then the command form is Degree( <Polynomial> )
		if (numOfArguments == 1) {
			return "degree(" + expression + ")";
		}

		// if there are two arguments then the command form is Degree( <Polynomial>, <Variable> )
		if (numOfArguments == 2) {
			String varName = getArgumentOfCommand(command, 1);
			return "degree(" + expression + "," + varName + ")";
		}

		return null;
	}

	static String translateDenominator(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// the command form is Denominator( <Expression> )
		return "denom(" + expression + ")";
	}

	static String translateInvert(Command command) {
		String expression = getArgumentOfCommand(command, 0).replace(" ", "");

		// the Invert command has two forms with one argument:
		// Invert( <Matrix> ) and Invert( <Function> )
		// if the expression has matrix syntax then the command form is Invert( <Matrix> )
		if (expression.startsWith("{{") && expression.endsWith("}}")) {
			expression = expression.replace("{", "[").replace("}", "]");
			return "LinearAlgebra:-MatrixInverse(Matrix(" + expression + "))";
		}
		// otherwise the command form is Invert( <Function> )
		return "subs(y=x, solve(y=" + expression + ", x))";
	}

	static String translateNumeric(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// if there is only one argument then the command form is Numeric( <Expression> )
		if (numOfArguments == 1) {
			// use 3 digits of precision as the default approximation
			return "evalf[3](" + expression + ")";
		}

		// if there are two arguments then the command form is Numeric( <Expression>, <Significant Figures> )
		if (numOfArguments == 2) {
			String numOfDigits = getArgumentOfCommand(command, 1);
			return "evalf[" + numOfDigits + "](" + expression + ")";
		}

		return null;
	}

	static String translateSubstitute(Command command, Map<String, String> fullNameToShortName) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

		// if there are two arguments then the command form is Substitute( <Expression>, <Substitution List> )
		if (numOfArguments == 2) {
			String substitutionList = getArgumentOfCommand(command, 1);
			return  "subs(" + substitutionList + "," + expression + ")";
		}

		// if there are three arguments and the second argument is not an equation,
		// then the command form is Substitute( <Expression>, <from>, <to> )
		if (numOfArguments == 3 && !getArgumentOfCommand(command, 1).contains("=")) {
			String from = getArgumentOfCommand(command, 1);
			String to = getArgumentOfCommand(command, 2);
			return "subs(" + from + "=" + to + "," + expression + ")";
		}

		// if there are three or more arguments and the second argument is an equation,
		// then the command form is Substitute( <Expression>, <Substitution>, <Substitution>, ... )
		if (numOfArguments >= 3 && getArgumentOfCommand(command, 1).contains("=")) {
			StringBuilder substitutionList = new StringBuilder("[");

			for (int j = 1; j < numOfArguments; j++) {
				if (j > 1) {
					substitutionList.append(",");
				}
				substitutionList.append(getArgumentOfCommand(command, j));
			}

			substitutionList.append("]");
			return "subs(" + substitutionList + "," + expression + ")";
		}

		return null;
	}

	static String translateIsPrime(Command command) {
		String expression = getArgumentOfCommand(command, 0);

		// the command form is IsPrime( <Number> )
		return  "isprime(" + expression + ")";
	}

	static String translateModularExponent(Command command) {
		String base = getArgumentOfCommand(command, 0);
		String exponent = getArgumentOfCommand(command, 1);
		String modulus = getArgumentOfCommand(command, 2);

		// the command form is ModularExponent( <Number>, <Number>, <Number> )
		return base + " &^ " + exponent + " mod " + modulus;
	}

	static String translatePrimeFactors(Command command) {
		String expression = getArgumentOfCommand(command, 0);

		// the command form is PrimeFactors( <Number> )
		return  "ifactor(" + expression + ")";
	}

	static String translateLaplace(Command command) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getArgumentOfCommand(command, 0);

		// if there is only one argument then the command form is Laplace( <Function> )
		if (numOfArguments == 1) {
			String varName = getSingleVariableName(command);

			// GeoGebra returns the result using the same variable name,
			// so Maple uses a temporary transform variable and then substitutes it back
			return "subs(_u=" + varName + ", inttrans:-laplace("
					+ expression + "," + varName + ",_u))";
		}

		// if there are two arguments then the command form is Laplace( <Function>, <Variable> )
		if (numOfArguments == 2) {
			String varName = getArgumentOfCommand(command, 1);

			// GeoGebra returns the result using the selected variable name,
			// so Maple uses a temporary transform variable and then substitutes it back
			return "subs(_u=" + varName + ", inttrans:-laplace("
					+ expression + "," + varName + ",_u))";
		}

		// if there are three arguments then the command form is Laplace( <Function>, <Variable>, <Variable> )
		if (numOfArguments == 3) {
			String varName = getArgumentOfCommand(command, 1);
			String newVarName = getArgumentOfCommand(command, 2);
			return "inttrans:-laplace(" + expression + "," + varName + "," + newVarName + ")";
		}

		return null;
	}

	static String translateInverseLaplace(Command command) {
		int numOfArguments = command.getArgumentNumber();
		String expression = getArgumentOfCommand(command, 0);

		// if there is only one argument then the command form is InverseLaplace( <Function> )
		if (numOfArguments == 1) {
			String varName = getSingleVariableName(command);

			// GeoGebra returns the result using the same variable name,
			// so Maple uses a temporary result variable and then substitutes it back
			return "subs(_u=" + varName + ", inttrans:-invlaplace("
					+ expression + "," + varName + ",_u))";
		}

		// if there are two arguments then the command form is InverseLaplace( <Function>, <Variable> )
		if (numOfArguments == 2) {
			String varName = getArgumentOfCommand(command, 1);

			// GeoGebra returns the result using the selected variable name,
			// so Maple uses a temporary result variable and then substitutes it back
			return "subs(_u=" + varName + ", inttrans:-invlaplace("
					+ expression + "," + varName + ",_u))";
		}

		// if there are three arguments then the command form is InverseLaplace( <Function>, <Variable>, <Variable> )
		if (numOfArguments == 3) {
			String varName = getArgumentOfCommand(command, 1);
			String newVarName = getArgumentOfCommand(command, 2);
			return "inttrans:-invlaplace(" + expression + "," + varName + "," + newVarName + ")";
		}

		return null;
	}

	private static String getSingleVariableName(Command command) {
		HashSet<GeoElement> variables = command.getArgument(0).getVariables(SymbolicMode.SYMBOLIC);

		if (variables.size() == 1) {
			return variables.iterator().next().toString();
		}

		return "t";
	}



	private static String getArgumentOfCommand(Command command, int argumentIndex) {
		return command.getArgument(argumentIndex)
				.getCASstring(StringTemplate.casCopyTemplate, false);
	}

	private static String getMapleName(String expression,
			Map<String, String> fullNameToShortName) {
		String shortName = fullNameToShortName.get(expression);
		return shortName == null ? expression : shortName;
	}

	static String fixPiAppear(String toFix) {
		if (toFix.contains("pi")) {
			toFix = toFix.replace("pi" , "Pi");
		}
		return toFix;
	}
}