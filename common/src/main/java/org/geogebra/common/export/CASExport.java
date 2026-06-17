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
							def = translateMapleCommand(command, fullNameToShortName);
						}
					}

					if (def == null) {
						def = cell.getDefinitionDescription(StringTemplate.casCopyTemplate);
					}

					txt += def + ";\n";
				}
			}
		}

		// TODO: Consider putting this to somewhere else
		txt = txt.replace("'", "_"); // always use _ instead of '

		return txt;
	}

	private String translateMapleCommand(Command command,
			Map<String, String> fullNameToShortName) {

		String def = null;
		String name = command.getName();

		switch (name) {
		case "AreEqual":
			def = MapleCommandTranslator.translateAreEqual(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Simplify":
			def = MapleCommandTranslator.translateSimplify(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "CompleteSquare":
			def = MapleCommandTranslator.translateCompleteSquare(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "CFactor":
			def = MapleCommandTranslator.translateCFactor(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "CommonDenominator":
			def = MapleCommandTranslator.translateCommonDenominator(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Divisors":
			def = MapleCommandTranslator.translateDivisors(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "DivisorsList":
			def = MapleCommandTranslator.translateDivisorsList(command,
					arguments -> translateMapleArgument(arguments, fullNameToShortName));
			break;

		case "DivisorsSum":
			def = MapleCommandTranslator.translateDivisorsSum(command,
					arguments -> translateMapleArgument(arguments, fullNameToShortName));
			break;

		case "Expand":
			def = MapleCommandTranslator.translateExpand(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Assume":
			def = MapleCommandTranslator.translateAssume(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Solve":
			def = MapleCommandTranslator.translateSolve(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "SolveCubic":
			def = MapleCommandTranslator.translateSolveCubic(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "SolveQuartic":
			def = MapleCommandTranslator.translateSolveQuartic(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Factor":
			def = MapleCommandTranslator.translateFactor(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "LCM":
			def = MapleCommandTranslator.translateLCM(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "GCD":
			def = MapleCommandTranslator.translateGCD(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "GeometricMean":
			def = MapleCommandTranslator.translateGeometricMean(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
		break;

		case "HarmonicMean":
			def = MapleCommandTranslator.translateHarmonicMean(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Derivative":
			def = MapleCommandTranslator.translateDerivative(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Eliminate":
			def = MapleCommandTranslator.translateEliminate(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Integral":
			def = MapleCommandTranslator.translateIntegral(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "IntegralBetween":
			def = MapleCommandTranslator.translateIntegralBetween(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Limit":
			def = MapleCommandTranslator.translateLimit(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Div":
			def = MapleCommandTranslator.translateDiv(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "CurveCartesian":
			def = MapleCommandTranslator.translateCurveCartesian(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Degree":
			def = MapleCommandTranslator.translateDegree(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Denominator":
			def = MapleCommandTranslator.translateDenominator(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Invert":
			def = MapleCommandTranslator.translateInvert(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Numeric":
			def = MapleCommandTranslator.translateNumeric(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Substitute":
			def = MapleCommandTranslator.translateSubstitute(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "IsPrime":
			def = MapleCommandTranslator.translateIsPrime(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "ModularExponent":
			def = MapleCommandTranslator.translateModularExponent(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "PrimeFactors":
			def = MapleCommandTranslator.translatePrimeFactors(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "Laplace":
			def = MapleCommandTranslator.translateLaplace(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;

		case "InverseLaplace":
			def = MapleCommandTranslator.translateInverseLaplace(command,
					argument -> translateMapleArgument(argument, fullNameToShortName));
			break;
		}

		return def;
	}


	private String translateMapleArgument(ExpressionNode argument,
			Map<String, String> fullNameToShortName) {

		Command nestedCommand = argument.getTopLevelCommand();

		if (nestedCommand != null) {
			return translateMapleCommand(nestedCommand, fullNameToShortName);
		}

		// TODO: handle equations whose left/right sides may contain nested commands
		// Example: Solve(Derivative(x^2) = 0, x)
		// The whole argument is not a command, but the left side is.

		String expression = argument.getCASstring(StringTemplate.casCopyTemplate, false);
		return MapleCommandTranslator.getMapleName(expression, fullNameToShortName);
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
