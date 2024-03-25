package org.geogebra.common.export;

import static org.geogebra.common.main.App.VIEW_CAS;

import java.io.IOException;
import java.util.ArrayList;

import org.geogebra.common.GeoGebraConstants;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GFont;
import org.geogebra.common.cas.view.CASView;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Macro;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoCasCell;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Feature;
import org.geogebra.common.main.Localization;

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
				+ "<head>\n";

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

		html += "</head>\n<body>\n";

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

			String input = cv.getCellInput(i);

			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);
			GColor color = cell.getFontColor();
			String colorString = color.toString();

			html += "<div class=\"input\">";
			// Set colors and text styles:
			if (useColors) {
				html += "<div style=\"color:" + colorString + "\">";
				if ((cell.getFontStyle() & GFont.BOLD) == GFont.BOLD) {
					html += "<b>";
				}
				if ((cell.getFontStyle() & GFont.ITALIC) == GFont.ITALIC) {
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
			if (!cell.isUseAsText()) {
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

	public String createMapleTxt() {
		String txt = "";

		Construction cons = app.kernel.getConstruction();
		CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);
		int rows = cv.getRowCount();

		// Iterate on all cells:
		for (int i = 0; i < rows; i++) {

			String input = cv.getCellInput(i);

			GeoCasCell cell = cv.getConsoleTable().getGeoCasCell(i);

			if (cell.isUseAsText()) {
				txt += "# " + input + "\n";
			} else {
				if (!cell.isEmpty()) {
					txt += "> ";
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
}
