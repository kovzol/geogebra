package org.geogebra.common.export;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.SymbolicMode;
import org.geogebra.common.kernel.geos.GeoElement;

final class MapleCommandTranslator {

	private MapleCommandTranslator() {
		// utility class
	}

	static String translateAreEqual(Command command,
			Function<ExpressionNode, String> argumentTranslator) {

		int numOfArguments = command.getArgumentNumber();

		// if there are two arguments then the command form is AreEqual( <Object>, <Object> )
		if (numOfArguments == 2) {
			String firstExpression = argumentTranslator.apply(command.getArgument(0));
			String secondExpression = argumentTranslator.apply(command.getArgument(1));

			return "evalb(simplify((" + firstExpression + ")-("
					+ secondExpression + ")) = 0)";
		}

		return null;
	}

	static String translateSimplify(Command command,
			Function<ExpressionNode, String> argumentTranslator) {

		// the command form is Simplify( <Function> )
		String expression = argumentTranslator.apply(command.getArgument(0));
		return "simplify(" + expression + ")";
	}

	static String translateCompleteSquare(Command command,
			Function<ExpressionNode, String> argumentTranslator) {

		// The command form is CompleteSquare( <Quadratic Function> )
		String expression = argumentTranslator.apply(command.getArgument(0));
		String varName = getSingleVariableName(command,0);
		return "Student:-Precalculus:-CompleteSquare( " +  expression + "," + varName + " )";
	}

	static String translateCFactor(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		// if there is one argument then the command form is CFactor( <Expression> )
		if (numOfArguments == 1) {
			String expression = argumentTranslator.apply(command.getArgument(0));
			return "factor(" + expression + ", I)";
		}

		// if there are two arguments then the command form is CFactor( <Expression>, <Variable> )
		if (numOfArguments == 2) {
			String expression = argumentTranslator.apply(command.getArgument(0));
			String varName = argumentTranslator.apply(command.getArgument(1));
			return "factor(" + expression + ", " + varName + ", I)";
		}

		return null;
	}

	static String translateCommonDenominator(Command command,
			Function<ExpressionNode, String> argumentTranslator) {

		// The command form is CommonDenominator( <Expression>, <Expression> )
		String firstExpression = fixSyntax(argumentTranslator.apply(command.getArgument(0)));
		String secondExpression = fixSyntax(argumentTranslator.apply(command.getArgument(1)));
		return "lcm(denom(normal(" + firstExpression + "))"
				+ ",denom(normal(" + secondExpression + ")))";
	}

	static String translateDivisors(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		// The command form is Divisors( <Number> )
		String argNum = argumentTranslator.apply(command.getArgument(0));
		return "NumberTheory:-tau(" + argNum + ")";
	}

	static String translateDivisorsList(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		// The command form is DivisorsList( <Number> )
		String argNum = argumentTranslator.apply(command.getArgument(0));
		return "NumberTheory:-Divisors(" + argNum + ")";
	}

	static String translateDivisorsSum(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		// The command form is DivisorsSum( <Number> )
		String argNum = argumentTranslator.apply(command.getArgument(0));
		return "NumberTheory:-SumOfDivisors(" + argNum + ")";
	}

	static String translateExpand(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));
		return "expand(" +  expression + ")";
	}

	static String translateAssume(Command command,
			Function<ExpressionNode, String> argumentTranslator) {

		// the command form is Assume( <Condition>, <Expression> )
		String conditions = fixSyntax(argumentTranslator.apply(command.getArgument(0)));
		String expression = argumentTranslator.apply(command.getArgument(1));
		return "(" + expression + ") assuming  " +  conditions;
	}

	static String translateSolve(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		if (numOfArguments == 2) {
			String eqs = argumentTranslator.apply(command.getArgument(0));
			String vars = String.valueOf(command.getArgument(1));
			vars = vars.replace("{", "[");
			vars = vars.replace("}", "]");
			return "solve(" + eqs + "," + vars + ")";
		}

		return null;
	}

	static String translateSolveCubic(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));
		return "solve(" + expression + ")";
	}

	static String translateSolveQuartic(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));
		return "solve(" + expression + ")";
	}

	static String translateFactor(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		// if there is one argument then the command form is Factor( <Polynomial> )
		// or Factor( <Number> )
		if (numOfArguments == 1) {
			String expr = command.getArgument(0).toString();
			if (expr.startsWith("$")) {
				// This is something like $1, so we convert it into something like !1:
				expr = "!" + expr.substring(1);
			} else {
				expr = argumentTranslator.apply(command.getArgument(0));
			}
			if (isIntegerExpression(expr)) {
				return "ifactor(" + expr + ")";
			}
			return "factor(" + expr + ")";
		}
		// if there are two arguments then the command form is Factor( <Expression>, <Variable> )
		if (numOfArguments == 2) {
			String expr = argumentTranslator.apply(command.getArgument(0));
			String varName = argumentTranslator.apply(command.getArgument(1));
			return "factor(" + expr + "," + varName + ")";
		}
		return null;

	}

	static String translateLCM(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		// in case of one argument, the command form is LCM( <List of Polynomials> ) or LCM( <List of Numbers> )
		if (numOfArguments == 1) {
			String expression = argumentTranslator.apply(command.getArgument(0));
			return "lcm(op(" +  expression + "))";
		}

		// in case of two arguments, the command form is LCM( <Number>, <Number> ) or LCM( <Polynomial>, <Polynomial> )
		if (numOfArguments == 2) {
			String firstArg = argumentTranslator.apply(command.getArgument(0));
			String secondArg = argumentTranslator.apply(command.getArgument(1));

			// if both of the arguments are integers then the command form is LCM( <Number>, <Number> )
			if (isIntegerExpression(firstArg) && isIntegerExpression(secondArg)) {
				return "ilcm(" + firstArg + "," + secondArg + ")";
			}
			// otherwise, treat the arguments as polynomials and use Maple's polynomial LCM
			else {
				return "lcm(" + firstArg + "," + secondArg + ")";
			}

		}

		return null;
	}

	static String translateGCD(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		// in case of one argument, the command form is GCD( <List of Polynomials> ) or GCD( <List of Numbers> )
		if (numOfArguments == 1) {
			String expression = argumentTranslator.apply(command.getArgument(0));
			return "foldl(gcd,op(" + expression + "))";
		}

		// in case of two arguments, the command form is GCD( <Number>, <Number> ) or GCD( <Polynomial>, <Polynomial> )
		if (numOfArguments == 2) {
			String firstArg = argumentTranslator.apply(command.getArgument(0));
			String secondArg = argumentTranslator.apply(command.getArgument(1));

			// if both of the arguments are integers then the command form is GCD( <Number>, <Number> )
			if (isIntegerExpression(firstArg) && isIntegerExpression(secondArg)) {
				return "igcd(" + firstArg + "," + secondArg + ")";
			}
			// otherwise, treat the arguments as polynomials and use Maple's polynomial GCD
			else {
				return "gcd(" + firstArg + "," + secondArg + ")";
			}

		}

		return null;
	}

	static String translateGeometricMean(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));
		expression = expression.replace("{", "[").replace("}", "]");
		return "evalf(mul(x, x in " + expression + ")^(1/nops(" + expression + ")))";
	}

	static String translateHarmonicMean(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));
		expression = expression.replace("{", "[").replace("}", "]");
		return "evalf(nops(" + expression + ") / add(1/x, x in " + expression + "))";
	}

//	static String translatenPr(Command command,
//			Function<ExpressionNode, String> argumentTranslator) {
//		String firstArg = argumentTranslator.apply(command.getArgument(0));
//		String secondArg = argumentTranslator.apply(command.getArgument(1));
//		return firstArg + "!/(" + firstArg + "! - " + secondArg + "!)";
//	}

	static String translatenCr(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String firstArg = argumentTranslator.apply(command.getArgument(0));
		String secondArg = argumentTranslator.apply(command.getArgument(1));
		return firstArg + "!/((" + firstArg + " - " + secondArg + ")! * " + secondArg + "!)";
	}

	static String translateDerivative(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there is only one argument then the command form is Derivative( <Function> )
		if (numOfArguments == 1) {
			return "diff(" + expression + ",x)";
		}

		// if there are two arguments then the command form is either
		// Derivative( <Function>, <Number> ) or Derivative( <Expression>, <Variable> )
		if (numOfArguments == 2) {
			String secondArg = argumentTranslator.apply(command.getArgument(1));

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
			String varName = argumentTranslator.apply(command.getArgument(1));
			String order = argumentTranslator.apply(command.getArgument(2));
			return "diff(" + expression + "," + varName + "$" + order + ")";
		}

		return null;
	}

	static String translateEliminate(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String vars = "[";
		if (numOfArguments == 2) {
			String eqs = argumentTranslator.apply(command.getArgument(0));
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

	// TODO: GeoGebra may add an integration constant for indefinite integrals,
	// while Maple int(...) usually does not. This can affect nested commands,
	// especially when the result is later used inside IntegralBetween.
	static String translateIntegral(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there is only one argument then the command form is Integral( <Function> )
		if (numOfArguments == 1) {
			return "int(" + expression + ",x)";
		}

		// if there are two arguments then the command form is Integral( <Function>, <Variable> )
		if (numOfArguments == 2) {
			String varName = argumentTranslator.apply(command.getArgument(1));
			return "int(" + expression + "," + varName + ")";
		}

		// if there are three arguments then the command form is Integral( <Function>, <Start x-Value>, <End x-Value> )
		if (numOfArguments == 3) {
			String startValue = fixSyntax(argumentTranslator.apply(command.getArgument(1)));
			String endValue = fixSyntax(argumentTranslator.apply(command.getArgument(2)));
			return "int(" + expression + ",x=" + startValue + ".." + endValue + ")";
		}

		// if there are four arguments then the command form is Integral( <Function>, <Variable>, <Start Value>, <End Value> )
		if (numOfArguments == 4) {
			String varName = argumentTranslator.apply(command.getArgument(1));
			String startValue = fixSyntax(argumentTranslator.apply(command.getArgument(2)));
			String endValue = fixSyntax(argumentTranslator.apply(command.getArgument(3)));
			return "int(" + expression + "," + varName + "=" + startValue + ".." + endValue + ")";
		}

		return null;
	}

	static String translateIntegralBetween(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		String upperFunction = argumentTranslator.apply(command.getArgument(0));
		String lowerFunction = argumentTranslator.apply(command.getArgument(1));
		String expression = "(" + upperFunction + ")-(" + lowerFunction + ")";

		// if there are four arguments then the command form is IntegralBetween( <Function>, <Function>, <Number>, <Number> )
		if (numOfArguments == 4) {
			String startValue = fixSyntax(argumentTranslator.apply(command.getArgument(2)));
			String endValue = fixSyntax(argumentTranslator.apply(command.getArgument(3)));
			return "int(" + expression + ",x=" + startValue + ".." + endValue + ")";
		}

		// if there are five arguments then the command form is IntegralBetween( <Function>, <Function>, <Variable>, <Number>, <Number> )
		if (numOfArguments == 5) {
			String varName = argumentTranslator.apply(command.getArgument(2));
			String startValue = fixSyntax(argumentTranslator.apply(command.getArgument(3)));
			String endValue = fixSyntax(argumentTranslator.apply(command.getArgument(4)));
			return "int(" + expression + "," + varName + "=" + startValue + ".." + endValue + ")";
		}

		return null;
	}

	static String translateLimit(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there are two arguments then the command form is Limit( <Expression>, <Value> )
		if (numOfArguments == 2) {
			String approachTo = fixSyntax(argumentTranslator.apply(command.getArgument(1)));
			return "limit(" + expression + ",x=" + approachTo + ")";
		}

		// if there are three arguments then the command form is Limit( <Expression>, <Variable>, <Value> )
		if (numOfArguments == 3) {
			String varName = argumentTranslator.apply(command.getArgument(1));
			String approachTo = fixSyntax(argumentTranslator.apply(command.getArgument(2)));
			return "limit(" + expression + "," + varName + "=" + approachTo + ")";
		}

		return null;
	}

	static String translateDiv(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		// The command form is either Div( <Dividend Number>, <Divisor Number> ) or Div( <Dividend Polynomial>, <Divisor Polynomial> )
		String Dividend = argumentTranslator.apply(command.getArgument(0));
		String Divisor = argumentTranslator.apply(command.getArgument(1));
		if (isIntegerExpression(Divisor)) {
			return "iquo(" + Dividend + "," + Divisor + ")";
		} else {
			String varName = getSingleVariableName(command, 1);
			return "quo(" +  Dividend + "," + Divisor + "," + varName +")";
		}
	}

	static String translateCurveCartesian(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		String xExpression = argumentTranslator.apply(command.getArgument(0));
		String yExpression = argumentTranslator.apply(command.getArgument(1));
		String varName = argumentTranslator.apply(command.getArgument(2));
		String startValue = fixSyntax(argumentTranslator.apply(command.getArgument(3)));
		String endValue = fixSyntax(argumentTranslator.apply(command.getArgument(4)));

		// if there are five arguments then the command form is Curve( <Expression>, <Expression>, <Parameter Variable>, <Start Value>, <End Value> )
		if (numOfArguments == 5) {
			return "plot([" + xExpression + "," + yExpression + ","
					+ varName + "=" + startValue + ".." + endValue + "])";
		}

		return null;
	}

	static String translateDegree(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there is only one argument then the command form is Degree( <Polynomial> )
		if (numOfArguments == 1) {
			return "degree(" + expression + ")";
		}

		// if there are two arguments then the command form is Degree( <Polynomial>, <Variable> )
		if (numOfArguments == 2) {
			String varName = argumentTranslator.apply(command.getArgument(1));
			return "degree(" + expression + "," + varName + ")";
		}

		return null;
	}

	static String translateDenominator(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));

		// the command form is Denominator( <Expression> )
		return "denom(" + expression + ")";
	}

	static String translateInvert(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();

		if (numOfArguments == 1) {
			String expression = argumentTranslator.apply(command.getArgument(0)).replace(" ", "");

			// the Invert command has two forms with one argument:
			// Invert( <Matrix> ) and Invert( <Function> )
			// if the expression has matrix syntax then the command form is Invert( <Matrix> )
			if (expression.startsWith("{{") && expression.endsWith("}}")) {
				expression = expression.replace("{", "[").replace("}", "]");
				return "LinearAlgebra:-MatrixInverse(Matrix(" + expression + "))";
			}

			// otherwise the command form is Invert( <Function> )
			// wrap solve(...) in a list because solve may return several inverse branches
			return "subs(y=x, [solve(y=" + expression + ", x)])";
		}

		return null;
	}

	// TODO: Maple may print integer numeric results as 1. after evalf.
	// Handling this fully would require post-processing Maple output, not only export-time translation.
	// maybe we can handle it with wrapping the whole with the maple's command round()
	static String translateNumeric(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String expression = argumentTranslator.apply(command.getArgument(0));

		// avoid evalf for integers, since Maple may print them with a trailing dot
		if (isIntegerExpression(expression)) {
			return "round(evalf[3](" + expression + "))";
		}

		// if there is only one argument then the command form is Numeric( <Expression> )
		if (numOfArguments == 1) {
			// use 3 digits of precision as the default approximation
			return "evalf[3](" + expression + ")";
		}

		// if there are two arguments then the command form is Numeric( <Expression>, <Significant Figures> )
		if (numOfArguments == 2) {
			String significantFigures = argumentTranslator.apply(command.getArgument(1));
			return "evalf[" + significantFigures + "](" + expression + ")";
		}

		return null;
	}

	static String translateSubstitute(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there are two arguments then the command form is Substitute( <Expression>, <Substitution List> )
		if (numOfArguments == 2) {
			String substitutionList = argumentTranslator.apply(command.getArgument(1));
			return  "subs(" + substitutionList + "," + expression + ")";
		}

		// if there are three arguments and the second argument is not an equation,
		// then the command form is Substitute( <Expression>, <from>, <to> )
		String secondArgument = argumentTranslator.apply(command.getArgument(1));

		if (numOfArguments == 3 && !secondArgument.contains("=")) {
			String to = argumentTranslator.apply(command.getArgument(2));
			return "subs(" + secondArgument + "=" + to + "," + expression + ")";
		}

		// if there are three or more arguments and the second argument is an equation,
		// then the command form is Substitute( <Expression>, <Substitution>, <Substitution>, ... )
		if (numOfArguments >= 3 && secondArgument.contains("=")) {
			StringBuilder substitutionList = new StringBuilder("[");

			for (int j = 1; j < numOfArguments; j++) {
				if (j > 1) {
					substitutionList.append(",");
				}
				substitutionList.append(argumentTranslator.apply(command.getArgument(j)));
			}

			substitutionList.append("]");
			return "subs(" + substitutionList + "," + expression + ")";
		}

		return null;
	}

	static String translateIsPrime(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));

		// the command form is IsPrime( <Number> )
		return  "isprime(" + expression + ")";
	}

	static String translateModularExponent(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String base = argumentTranslator.apply(command.getArgument(0));
		String exponent = argumentTranslator.apply(command.getArgument(1));
		String modulus = argumentTranslator.apply(command.getArgument(2));

		// the command form is ModularExponent( <Number>, <Number>, <Number> )
		return base + " &^ " + exponent + " mod " + modulus;
	}

	static String translatePrimeFactors(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		String expression = argumentTranslator.apply(command.getArgument(0));

		// the command form is PrimeFactors( <Number> )
		return  "ifactor(" + expression + ")";
	}

	static String translateLaplace(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there is only one argument then the command form is Laplace( <Function> )
		if (numOfArguments == 1) {
			String varName = getSingleVariableName(command,0);

			if (varName.equals("t")) {
				return "subs(u=s, inttrans:-laplace("
						+ expression + ",t,u))";
			}

			// GeoGebra returns the result using the same variable name (except when the var name is t),
			// so Maple uses a temporary transform variable and then substitutes it back
			return "subs(u=" + varName + ", inttrans:-laplace("
					+ expression + "," + varName + ",u))";
		}

		// if there are two arguments then the command form is Laplace( <Function>, <Variable> )
		if (numOfArguments == 2) {
			String varName = argumentTranslator.apply(command.getArgument(1));

			// GeoGebra returns the result using the selected variable name,
			// so Maple uses a temporary transform variable and then substitutes it back
			return "subs(u=" + varName + ", inttrans:-laplace("
					+ expression + "," + varName + ",u))";
		}

		// if there are three arguments then the command form is Laplace( <Function>, <Variable>, <Variable> )
		if (numOfArguments == 3) {
			String varName = argumentTranslator.apply(command.getArgument(1));
			String newVarName = argumentTranslator.apply(command.getArgument(2));

			if (varName.equals(newVarName)) {
				return "subs(u=" + varName + ", inttrans:-laplace("
						+ expression + "," + varName + ",u))";
			}
			return "inttrans:-laplace(" + expression + "," + varName + "," + newVarName + ")";
		}

		return null;
	}

	static String translateInverseLaplace(Command command,
			Function<ExpressionNode, String> argumentTranslator) {
		int numOfArguments = command.getArgumentNumber();
		String expression = argumentTranslator.apply(command.getArgument(0));

		// if there is only one argument then the command form is InverseLaplace( <Function> )
		if (numOfArguments == 1) {
			String varName = getSingleVariableName(command,0);

			if (varName.equals("t")) {
				return "subs(u=s, inttrans:-invlaplace("
						+ expression + ",t,u))";
			}

			// GeoGebra returns the result using the same variable name (except when the var name is t),
			// so Maple uses a temporary result variable and then substitutes it back
			return "subs(u=" + varName + ", inttrans:-invlaplace("
					+ expression + "," + varName + ",u))";
		}

		// if there are two arguments then the command form is InverseLaplace( <Function>, <Variable> )
		if (numOfArguments == 2) {
			String varName = argumentTranslator.apply(command.getArgument(1));

			// GeoGebra returns the result using the selected variable name,
			// so Maple uses a temporary result variable and then substitutes it back
			return "subs(u=" + varName + ", inttrans:-invlaplace("
					+ expression + "," + varName + ",u))";
		}

		// if there are three arguments then the command form is InverseLaplace( <Function>, <Variable>, <Variable> )
		if (numOfArguments == 3) {
			String varName = argumentTranslator.apply(command.getArgument(1));
			String newVarName = argumentTranslator.apply(command.getArgument(2));

			if (varName.equals(newVarName)) {
				return "subs(u=" + varName + ", inttrans:-invlaplace("
						+ expression + "," + varName + ",u))";
			}
			return "inttrans:-invlaplace(" + expression + "," + varName + "," + newVarName + ")";
		}

		return null;
	}

	private static String getSingleVariableName(Command command,int index) {
		HashSet<GeoElement> variables = command.getArgument(index).getVariables(SymbolicMode.SYMBOLIC);

		if (!variables.isEmpty()) {
			return variables.iterator().next().toString();
		}

		return "x";
	}

	public static String getMapleName(String expression,
			Map<String, String> fullNameToShortName) {
		String shortName = fullNameToShortName.get(expression);
		return shortName == null ? expression : shortName;
	}

	static String fixSyntax(String toFix) {
		String fixedSyntax = fixPiAppear(toFix);
		fixedSyntax = fixInfinityAppear(fixedSyntax);
		fixedSyntax = fixAndOperatorAppear(fixedSyntax);
		fixedSyntax = fixOrOperatorAppear(fixedSyntax);
		return fixedSyntax;
	}

	static String fixPiAppear(String toFix) {
		return toFix.replace("pi" , "Pi");
	}

	static String fixInfinityAppear(String toFix) {
		return toFix.replace("Infinity" , "infinity");
	}

	static String fixAndOperatorAppear(String toFix) {
		return toFix.replace("&&", " and ")
				.replace("∧", " and ")
				.replace("And", " and ");
	}

	static String fixOrOperatorAppear(String toFix) {
		return toFix.replace("||", " or ")
				.replace("∨", " or ")
				.replace("Or", " or ");
	}

	private static boolean isIntegerExpression(String expression) {
		try {
			Long.parseLong(expression);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}