package org.geogebra.common.export;

import static org.geogebra.common.main.App.VIEW_CAS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GFont;
import org.geogebra.common.cas.view.CASView;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.SymbolicMode;
import org.geogebra.common.kernel.arithmetic.ValidExpression;
import org.geogebra.common.kernel.geos.GeoCasCell;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Localization;

import com.himamis.retex.editor.share.util.Unicode;

public class CASExport {

	/**
	 * Application instance.
	 */
	public App app;
	private Localization loc;

	private boolean useColors = true, useMathJax = true;
	private String inputBackgroundColor = "#f0f0ff";
	private String outputBackgroundColor = "#fff0f0";
	private String inputFontFamily = "sans-serif";
	private String outputFontFamily = "sans-serif";
	private String padding = "2px 2px 2px 2px";
	private boolean numbered = true;
	private String numberingBackgroundColor = "#e8eef7";

	public CASExport(App app) {
		this.app = app;
		this.setLoc(app.getLocalization());
	}

	public void setLoc(Localization loc) {
		this.loc = loc;
	}

	public String createHtml() {
		String html = "<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n<meta charset=\"UTF-8\">\n";

		html += "<title>" + loc.getMenuDefault("CASView", "CAS View") + "</title>\n";

		// Set style:
		html += "<style>\n";
		html += ".input {background-color: " + inputBackgroundColor + "; "
				+ "font-family: " + inputFontFamily + ";"
				+ "padding: " + padding + "; }\n";
		html += ".output {background-color: " + outputBackgroundColor + "; "
				+ "font-family: " + outputFontFamily + ";"
				+ "padding: " + padding + " }\n";
		// Remove dot after number (as in GeoGebra):
		html += "@counter-style empty-style {\n"
				+ "  system: extends decimal;\n"
				+ "  suffix: ' ';\n"
				+ "}\n"
				+ "ol {\n"
				+ "  list-style: empty-style;\n"
				+ "  font-family: sans-serif;\n"
				+ "  background-color: " + numberingBackgroundColor + ";";
		html += "</style>\n";

		// In case LaTeX (MathJax) is used, load it:
		if (useMathJax) {
			html += "<script type=\"text/x-mathjax-config\">\n"
					+ "    MathJax.Hub.Config({\n"
					+ "        \"CommonHTML\": { linebreaks: { automatic: true } },\n"
					+ "        \"HTML-CSS\": { linebreaks: { automatic: true } },\n"
					+ "        \"SVG\": { linebreaks: { automatic: true } }\n"
					+ "    });\n"
					+ "</script>\n";
			html += "<script type=\"text/javascript\" async\n"
					+ "  src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML\">\n"
					+ "</script>\n";
		}

		html += "</head>\n<body style=\"background-color: " + numberingBackgroundColor + "\">\n";

		Construction cons = app.kernel.getConstruction();
		CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);
		int rows = cv.getRowCount();

		if (numbered) {
			html += "<ol>\n";
		}

		// Iterate on all cells:
		for (int i = 0; i < rows; i++) {
			if (numbered) {
				html += "<li>";
			}

			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);
			String input;
			String fullInput = cell.getFullInput();
			if (fullInput != null) {
				input = fullInput;
			} else {
				input = cell.getInput(StringTemplate.defaultTemplate);
				input = input.replace("<", "&gt;");
			}
			GColor color = cell.getFontColor();
			String colorString = color.toString();

			html += "<div class=\"input\">";
			// Set colors and text styles:
			if (useColors) {
				html += "<div style=\"color:" + colorString + "\">";
				if ((cell.getFontStyle() & GFont.BOLD) == GFont.BOLD) {
					html += "<b>";
				}
				if ((cell.getFontStyle() & GFont.ITALIC) == GFont.ITALIC && fullInput == null) {
					html += "<i>";
				}
				if ((cell.getFontStyle() & GFont.UNDERLINE) == GFont.UNDERLINE) {
					html += "<u>";
				}
			}
			html += input;
			if (useColors) {
				if ((cell.getFontStyle() & GFont.UNDERLINE) == GFont.UNDERLINE) {
					html += "</u>";
				}
				if ((cell.getFontStyle() & GFont.ITALIC) == GFont.ITALIC) {
					html += "</i>";
				}
				if ((cell.getFontStyle() & GFont.BOLD) == GFont.BOLD) {
					html += "</b>";
				}
				html += "</div>";
			}
			html += "</div>\n";

			// If there is an output formula, put it in the output:
			if (!cell.isUseAsText() || fullInput != null) {
				String output = "";
				if (useMathJax) {
					String latexOutput = cell.getLaTeXOutput(false);
					if (latexOutput != null) {
						output = "\\[" + latexOutput + "\\]";
					}
				} else {
					output = cv.getRowOutputValue(i);
				}
				html += "<div class=\"output\">" + output + "</div>\n";
			}

			// End of cell:
			html += "\n";
		}
		if (numbered) {
			html += "</ol>\n";
		}

		html += "</body>\n";
		html += "</html>\n";
		return html;
	}

	public String createLatex() {
		String latex = "\\documentclass[12pt]{article}\n"
				+ "\\usepackage{xcolor}\n"
				+ "\\usepackage{breqn}\n"
				+ "\\usepackage{amssymb}\n" // for \mathbb{...}
				+ "\\sloppy\n"
				+ "\\title{" + loc.getMenuDefault("CASView", "CAS View") + "}\n"
				+ "\\begin{document}\n";

		Construction cons = app.kernel.getConstruction();
		CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);
		int rows = cv.getRowCount();

		if (numbered) {
			latex += "\\begin{enumerate}\n";
		} else {
			latex += "\\begin{itemize}\n";
		}

		// Iterate on all cells:
		for (int i = 0; i < rows; i++) {
			if (numbered) {
				latex += "\\item\n";
			}

			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);
			String input;
			String fullInput = cell.getFullInput();
			if (fullInput != null) {
				input = fullInput;
			} else {
				input = cell.getInput(StringTemplate.defaultTemplate);
			}
			GColor color = cell.getFontColor();
			int red = color.getRed();
			int green = color.getGreen();
			int blue = color.getBlue();

			// Set colors and text styles:
			if (useColors) {
				latex += "\\definecolor{mycolor" + i + "}{rgb}{" + red / 256.0 + ", "
					+ green / 256.0 + ", " + blue / 256.0 + "}\n";
				if ((cell.getFontStyle() & GFont.BOLD) == GFont.BOLD) {
					latex += "\\textbf{";
				}
				if ((cell.getFontStyle() & GFont.ITALIC) == GFont.ITALIC && fullInput == null) {
					latex += "\\textit{";
				}
				if ((cell.getFontStyle() & GFont.UNDERLINE) == GFont.UNDERLINE) {
					latex += "\\underline{";
				}
			}

			input = input.replace("{", "\\{");
			input = input.replace("}", "\\}");
			input = input.replace("_", "\\_");
			input = input.replace("^", "\\^{}");

			input = input.replace(Unicode.BULLET + "", "$\\bullet$");
			input = input.replace(Unicode.IS_ELEMENT_OF + "", " $\\in$ ");
			input = input.replace(Unicode.PARALLEL + "", " $\\parallel$ ");
			input = input.replace(Unicode.PERPENDICULAR + "", " $\\perp$ ");
			input = input.replace(Unicode.QUESTEQ + "", " $\\stackrel{?}{=}$ ");
			input = input.replace(Unicode.NOTEQUAL + "", " $\\neq$ ");
			input = input.replace("\u211D", "$\\mathbb{R}$");

			latex += "\\textcolor{mycolor" + i + "}{" + input + "}";
			if (useColors) {
				if ((cell.getFontStyle() & GFont.UNDERLINE) == GFont.UNDERLINE) {
					latex += "}";
				}
				if ((cell.getFontStyle() & GFont.ITALIC) == GFont.ITALIC) {
					latex += "}";
				}
				if ((cell.getFontStyle() & GFont.BOLD) == GFont.BOLD) {
					latex += "}";
				}
			}
			latex += "\n";

			// If there is an output formula, put it in the output:
			if (!cell.isUseAsText() || fullInput != null) {
				String output = "";
				String latexOutput = cell.getLaTeXOutput(false);
				if (latexOutput != null) {
					output = "\\begin{dmath*}" + latexOutput + "\\end{dmath*}\n";
				} else {
					output = "\\begin{center}" + cv.getRowOutputValue(i) + "\\end{center}\n";
				}
				latex += output + "\n";
			}

		}
		if (numbered) {
			latex += "\\end{enumerate}\n";
		} else {
			latex += "\\end{itemize}\n";
		}

		latex += "\\end{document}\n";
		return latex;
	}


	public String createMapleTxt(boolean showPrompt) {
		String txt = "";

		Construction cons = app.kernel.getConstruction();
		CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);
		int rows = cv.getRowCount();
		Map<String , String> shortNameToFullName = new HashMap<>(); // Maps a short assignment name to its full assignment form
		Map<String , String> fullNameToShortName = new HashMap<>(); // Maps a full assignment name to its short assignment form
		// Iterate on all cells:
		for (int i = 0; i < rows; i++) {
			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);
			String input;
			String fullInput = cell.getFullInput();
			String var = cell.getAssignmentVariable();
			if (var != null && !var.isEmpty() && !var.equals("NONE")) {
				String FullAssignment = cell.getInput(StringTemplate.defaultTemplate);
				FullAssignment = FullAssignment.substring(0 , FullAssignment.indexOf(":=")).trim();
				shortNameToFullName.put(var , FullAssignment);
				fullNameToShortName.put(FullAssignment, var);
			}
			if (fullInput != null) {
				input = fullInput;
			} else {
				input = cell.getInput(StringTemplate.defaultTemplate);
			}

			if (cell.isUseAsText() && fullInput == null) {
				txt += "# " + input + "\n";
			} else {
				if (fullInput != null) {
					if (showPrompt) {
						txt += "> ";
					}
					txt += input + "\n";
				} else if (!cell.isEmpty()) {
					if (showPrompt) {
						txt += "> ";
					}
					if (var != null) {
						txt += var + ":=";
					}

					String def = null;

					ValidExpression ve = cell.getInputVE();
					if (ve != null) {
						Command command = ve.getTopLevelCommand();
						if (command != null) {
							String name = command.getName();
							int numOfArguments = command.getArgumentNumber();
							if (name.equals("Solve")) {
								if (numOfArguments == 2) {
									String eqs = getArgumentOfCommand(command,0);
									String vars = String.valueOf(command.getArgument(1));
									vars = vars.replace("{", "[");
									vars = vars.replace("}", "]");
									def = "solve(" + eqs + "," + vars + ")";
								}
							}
							// need to handle the cases of factor polynom
							if (name.equals("Factor")) {
								if (numOfArguments == 1) { // two numOfArguments are not implemented, TODO
									// Maple does not have an option to have a second argument
									String expr = command.getArgument(0).toString();
									if (expr.startsWith("$")) {
										// This is something like $1, so we convert it into something like !1:
										expr = "!" + expr.substring(1);
									} else {
										expr = getArgumentOfCommand(command,0);
									}
									def = "ifactor(" + expr + ")";
								}
							}

							if (name.equals("Derivative")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// if there is only one argument then the command form is Derivative( <Function> )
								if (numOfArguments == 1) {
									def = "diff(" + expression + ",x)";
								}

								// if there are two arguments then the command form is either
								// Derivative( <Function>, <Number> ) or Derivative( <Expression>, <Variable> )
								if (numOfArguments == 2) {
									String secondArg = getArgumentOfCommand(command, 1);

									// try to convert the second arg to number
									// in case the command form is Derivative( <Function>, <Number> )
									try {
										int order = Integer.parseInt(secondArg);
										def = "diff(" + expression + ",x$" + order + ")";
									}
									// in case the command form is Derivative( <Expression>, <Variable> )
									catch (NumberFormatException e) {
										def = "diff(" + expression + "," + secondArg + ")";
									}
								}

								// if there are three arguments then the command form is Derivative( <Expression>, <Variable>, <Number> )
								if (numOfArguments == 3) {
									String varName = getArgumentOfCommand(command, 1);
									String order = getArgumentOfCommand(command, 2);
									def = "diff(" + expression + "," + varName + "$" + order + ")";
								}
							}

							if (name.equals("Eliminate")) {
								String vars = "[";
								if (numOfArguments == 2) {
									String eqs = getArgumentOfCommand(command,0);
									ExpressionNode equationsEN = command.getArgument(0);
									ExpressionNode variablesEN = command.getArgument(1);
									HashSet<GeoElement> variables = variablesEN.getVariables(SymbolicMode.SYMBOLIC);
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
									def = "eliminate(" + eqs + "," + vars + ")";
								}
							}

							if (name.equals("Integral")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// if there is only one argument then the command form is Integral( <Function> )
								if (numOfArguments == 1) {
									def = "int(" + expression + ",x)";
								}

								// if there are two arguments then the command form is Integral( <Function>, <Variable> )
								if (numOfArguments == 2) {
									String varName = getArgumentOfCommand(command, 1);
									def = "int(" + expression + "," + varName + ")";
								}

								// if there are three arguments then the command form is Integral( <Function>, <Start x-Value>, <End x-Value> )
								if (numOfArguments == 3) {
									String startValue = getArgumentOfCommand(command, 1);
									String endValue = getArgumentOfCommand(command, 2);
									def = "int(" + expression + ",x=" + startValue + ".." + endValue + ")";
								}

								// if there are four arguments then the command form is Integral( <Function>, <Variable>, <Start Value>, <End Value> )
								if (numOfArguments == 4) {
									String varName = getArgumentOfCommand(command, 1);
									String startValue = getArgumentOfCommand(command, 2);
									String endValue = getArgumentOfCommand(command, 3);
									def = "int(" + expression + "," + varName + "=" + startValue + ".." + endValue + ")";
								}
							}

							if (name.equals("IntegralBetween")) {
								String upperFunction = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);
								String lowerFunction = getMapleName(getArgumentOfCommand(command, 1), fullNameToShortName);
								String expression = "(" + upperFunction + ")-(" + lowerFunction + ")";

								// if there are four arguments then the command form is IntegralBetween( <Function>, <Function>, <Number>, <Number> )
								if (numOfArguments == 4) {
									String startValue = getArgumentOfCommand(command, 2);
									String endValue = getArgumentOfCommand(command, 3);
									def = "int(" + expression + ",x=" + startValue + ".." + endValue + ")";
								}

								// if there are five arguments then the command form is IntegralBetween( <Function>, <Function>, <Variable>, <Number>, <Number> )
								if (numOfArguments == 5) {
									String varName = getArgumentOfCommand(command, 2);
									String startValue = getArgumentOfCommand(command, 3);
									String endValue = getArgumentOfCommand(command, 4);
									def = "int(" + expression + "," + varName + "=" + startValue + ".." + endValue + ")";
								}
							}

							if (name.equals("Limit")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// if there are two arguments then the command form is Limit( <Expression>, <Value> )
								if (numOfArguments == 2) {
									String approachTo = fixPiAppear(getArgumentOfCommand(command, 1));
									def = "limit(" + expression + ",x=" + approachTo + ")";
								}

								// if there are three arguments then the command form is Limit( <Expression>, <Variable>, <Value> )
								if (numOfArguments == 3) {
									String varName = getArgumentOfCommand(command, 1);
									String approachTo = fixPiAppear(getArgumentOfCommand(command, 2));
									def = "limit(" + expression + "," + varName + "=" + approachTo + ")";
								}
							}

							if (name.equals("CurveCartesian")) {
								String xExpression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);
								String yExpression = getMapleName(getArgumentOfCommand(command, 1), fullNameToShortName);
								String varName = getArgumentOfCommand(command, 2);
								String startValue = fixPiAppear(getArgumentOfCommand(command, 3));
								String endValue = fixPiAppear(getArgumentOfCommand(command, 4));

								// if there are five arguments then the command form is Curve( <Expression>, <Expression>, <Parameter Variable>, <Start Value>, <End Value> )
								if (numOfArguments == 5) {
									def = "plot([" + xExpression + "," + yExpression + ","
											+ varName + "=" + startValue + ".." + endValue + "])";
								}
							}

							if (name.equals("Degree")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// if there is only one argument then the command form is Degree( <Polynomial> )
								if (numOfArguments == 1) {
									def = "degree(" + expression + ")";
								}

								// if there are two arguments then the command form is Degree( <Polynomial>, <Variable> )
								if (numOfArguments == 2) {
									String varName = getArgumentOfCommand(command, 1);
									def = "degree(" + expression + "," + varName + ")";
								}
							}

							if (name.equals("Denominator")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// the command form is Denominator( <Expression> )
								def = "denom(" + expression + ")";
							}

							if (name.equals("Invert")) {
								String expression = getArgumentOfCommand(command, 0).replace(" ", "");

								// the Invert command has two forms with one argument:
								// Invert( <Matrix> ) and Invert( <Function> )

								// if the expression has matrix syntax then the command form is Invert( <Matrix> )
								if (expression.startsWith("{{") && expression.endsWith("}}")) {
									expression = expression.replace("{", "[").replace("}", "]");
									def = !txt.contains("with(LinearAlgebra):") ? "with(LinearAlgebra):" : "";
									def += "MatrixInverse(Matrix(" + expression + "))";
								}
								// otherwise the command form is Invert( <Function> )
								else {
									def = "subs(y=x, solve(y=" + expression + ", x))";
								}
							}

							if (name.equals("Numeric")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// if there is only one argument then the command form is Numeric( <Expression> )
								if (numOfArguments == 1) {
									// use 3 digits of precision as the default approximation
									def = "evalf[3](" + expression + ")";
								}

								// if there are two arguments then the command form is Numeric( <Expression>, <Significant Figures> )
								if (numOfArguments == 2) {
									String numOfDigits = getArgumentOfCommand(command, 1);
									def = "evalf[" + numOfDigits + "](" + expression + ")";
								}
							}

							if (name.equals("Substitute")) {
								String expression = getMapleName(getArgumentOfCommand(command, 0), fullNameToShortName);

								// if there are two arguments then the command form is Substitute( <Expression>, <Substitution List> )
								if (numOfArguments == 2) {
									String substitutionList = getArgumentOfCommand(command, 1);
									def = "subs(" + substitutionList + "," + expression + ")";
								}

								// if there are three arguments and the second argument is not an equation,
								// then the command form is Substitute( <Expression>, <from>, <to> )
								if (numOfArguments == 3 && !getArgumentOfCommand(command, 1).contains("=")) {
									String from = getArgumentOfCommand(command, 1);
									String to = getArgumentOfCommand(command, 2);
									def = "subs(" + from + "=" + to + "," + expression + ")";
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
									def = "subs(" + substitutionList + "," + expression + ")";
								}
							}

							if (name.equals("IsPrime")) {
								String expression = getArgumentOfCommand(command, 0);

								// the command form is IsPrime( <Number> )
								def = "isprime(" + expression + ")";
							}

							if (name.equals("ModularExponent")) {
								String base = getArgumentOfCommand(command, 0);
								String exponent = getArgumentOfCommand(command, 1);
								String modulus = getArgumentOfCommand(command, 2);

								// the command form is ModularExponent( <Number>, <Number>, <Number> )
								def = base + " &^ " + exponent + " mod " + modulus;
							}

							if (name.equals("PrimeFactors")) {
								String expression = getArgumentOfCommand(command, 0);

								// the command form is PrimeFactors( <Number> )
								def = "ifactor(" + expression + ")";
							}

							// need to complete
							// their many cases to handle
							if (name.equals("Laplace")) {
								String Expression = getArgumentOfCommand(command , 0);
								def = !txt.contains("with(inttrans): ") ? "with(inttrans): " : "";
								// if there is only one argument than the command form is Laplace( <Function> )
								if (numOfArguments == 1) {
									String varName = (String.valueOf(command.getArgument(0).getVariables(SymbolicMode.SYMBOLIC))).replaceAll("[\\[\\]]" , "");
									 if (varName.contains("t") && varName.contains("*")) {
										 def = "laplace(" + Expression + ", t, s)";
									 } else {
//										varName = varName.replaceAll("[\\[\\]]" , "");
										def = "subs(s=" + varName + ", laplace(" + Expression + ", " + varName + ", s))";
									 }
								}
								// if there are two arguments than the command form is Laplace( <Function>, <Variable> )
								if (numOfArguments == 2) {
									String varName = getArgumentOfCommand(command , 1);
									if (!varName.equals("s")) {
										def = "subs(s=" +  varName + ", laplace(" + Expression + ", " + varName + ", s))";
									} else {
										def = "subs(t=s ,laplace(" + Expression + ",s,t))";
									}

								}
								// if there are three arguments than the command form is Laplace( <Function>, <Variable>, <Variable> )
								if (numOfArguments == 3) {
									String varName = getArgumentOfCommand(command , 1);
									String newVarName = getArgumentOfCommand(command , 2);
									if (varName.equals(newVarName)) {
										def = "subs(s=" + newVarName +  ",laplace(" + Expression + "," + varName + ", s))";
									} else {
										def = "laplace(" + Expression + ", " + varName + ", " + newVarName + ")";
									}
								}
							}

							// need to complete
							// their many cases to handle
							if (name.equals("InverseLaplace")) {
								String Expression = getArgumentOfCommand(command , 0);
								def = !txt.contains("with(inttrans):") ? "with(inttrans):" : "";
								def += "InverseLaplace(" + Expression + ",";
								if (numOfArguments == 1) {
									def += " s , t)";
								}
								String varName = getArgumentOfCommand(command , 1);
								if (numOfArguments == 2) {
									def += varName + ", t)";
								}
								if (numOfArguments == 3) {
									String newVarName = getArgumentOfCommand(command , 2);
									def += varName + ", " +  newVarName + ")";
								}

							}

						}
					}

					if (def == null) {
						def = cell.getDefinitionDescription(StringTemplate.casCopyTemplate);
					}

					txt += def + ";\n";
				}
			}
		}

		return txt;
	}

	private String getArgumentOfCommand(Command command,int indexOfCommand) {
		return command.getArgument(indexOfCommand).getCASstring(StringTemplate.casCopyTemplate, false);
	}

	private String getMapleName(String expression, Map<String, String> fullNameToShortName) {
		String shortName = fullNameToShortName.get(expression);
		return shortName == null ? expression : shortName;
	}


	private String fixPiAppear(String toFix) {
		if (toFix.contains("pi")) {
			toFix = toFix.replace("pi" , "Pi");
		}
		return toFix;
	}

	public String createGiacTxt() {
		// ";" are not compulsory in Giac code, so they could be omitted.

		String txt = "caseval(\"init geogebra\");\n";

		Construction cons = app.kernel.getConstruction();
		CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);
		int rows = cv.getRowCount();

		// Iterate on all cells:
		for (int i = 0; i < rows; i++) {

			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);
			String input;
			String fullInput = cell.getFullInput();
			if (fullInput != null) {
				input = fullInput;
			} else {
				input = cell.getInput(StringTemplate.defaultTemplate);
			}

			if (cell.isUseAsText() && fullInput == null) {
				txt += "comment(\"" + input + "\");\n";
			} else {
				if (fullInput != null) {
					txt += input + ";\n";
				} else if (!cell.isEmpty()) {
					String var = cell.getAssignmentVariable();
					if (var != null) {
						txt += var + ":=";
					}
					String def = cell.getDefinitionDescription(StringTemplate.casCopyTemplate);
					txt += def + ";\n";
				}
			}
		}

		return txt;
	}

	public String createMathematicaTxt() {
		String txt = "";

		Construction cons = app.kernel.getConstruction();
		CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);
		int rows = cv.getRowCount();

		// Iterate on all cells:
		for (int i = 0; i < rows; i++) {

			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);
			String input;
			String fullInput = cell.getFullInput();
			if (fullInput != null) {
				input = fullInput;
			} else {
				input = cell.getInput(StringTemplate.defaultTemplate);
			}

			if (cell.isUseAsText() && fullInput == null) {
				input = input.replace(Unicode.BULLET + "", "-");
				input = input.replace(Unicode.IS_ELEMENT_OF + "", " element of ");
				input = input.replace(Unicode.PARALLEL + "", " || ");
				input = input.replace(Unicode.PERPENDICULAR + "", " _|_ ");
				input = input.replace(Unicode.QUESTEQ + "", " == ");
				input = input.replace(Unicode.NOTEQUAL + "", " != ");
				// TODO: There may be other characters to replace, maybe another method can
				// simplify this...
				txt += "(* " + input + " *)\n";
			} else {
				if (fullInput != null) {
					// TODO: There should be a full StringTemplate written, instead of this:
					input = input.replace("=", "==");
					txt += input + ";\n";
				} else if (!cell.isEmpty()) {
					String var = cell.getAssignmentVariable();
					if (var != null) {
						txt += var + ":=";
					}
					String def = cell.getDefinitionDescription(StringTemplate.casCopyTemplate);
					// TODO: There should be a full StringTemplate written, instead of this:
					def = def.replace("=", "==");
					txt += def + ";\n";
				}
			}
		}

		return txt;
	}

}
