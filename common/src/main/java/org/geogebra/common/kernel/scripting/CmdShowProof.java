package org.geogebra.common.kernel.scripting;

import static org.geogebra.common.main.App.VIEW_CAS;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GFont;
import org.geogebra.common.cas.view.CASTable;
import org.geogebra.common.cas.view.CASView;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.View;
import org.geogebra.common.kernel.arithmetic.BooleanValue;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.commands.CmdScripting;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoCasCell;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.kernel.prover.AlgoProve;
import org.geogebra.common.kernel.prover.AlgoProveDetails;
import org.geogebra.common.main.App;
import org.geogebra.common.main.MyError;
import org.geogebra.common.util.debug.Log;

import com.himamis.retex.editor.share.util.Unicode;


/**
 * ToolImage
 */
public class CmdShowProof extends CmdScripting {
	/**
	 * Create new command processor
	 *
	 * @param kernel
	 *            kernel
	 */
	public CmdShowProof(Kernel kernel) {
		super(kernel);
	}

	public GeoElement[] perform(Command c) throws MyError {

		int n = c.getArgumentNumber();
		GeoElement[] arg;
		arg = resArgs(c);

		switch (n) {
		case 1:
			if (arg[0] instanceof BooleanValue) {
				CASView cv = (CASView) cons.getApplication().getView(VIEW_CAS);

				if (!app.showView(App.VIEW_CAS)) {
					// ...getRowCount gives 1 if the CAS View is closed. FIXME.
					throw new MyError(loc, "Please open the CAS View first.");
				}

				kernel.storeUndoInfo();

				int rows;
				rows = cv.getConsoleTable().getRowCount();

				AlgoProveDetails algo = new AlgoProveDetails(cons, arg[0], false, false, true);
				String statementText = algo.statementText(arg[0]);

				String[] statementTexts = statementText.split("\n");
				for (String s : statementTexts) {
					GeoCasCell gcc1 = new GeoCasCell(cons);
					gcc1.setInput(s);
					gcc1.setUseAsText(true);
					gcc1.setFontColor(GColor.BLUE);
					gcc1.computeOutput();
					gcc1.update();
					cons.setCasCellRow(gcc1, rows++);
				}

				boolean statementTrue = false;
				GeoList output = algo.getGeoList();
				GeoCasCell gcc2 = new GeoCasCell(cons);
				gcc2.setUseAsText(true);
				if (output.size() == 0 || (output.size() == 2 && (!((GeoBoolean) output.get(0)).isDefined()))) {
					if (output.size() == 2) { // There was some info given on the reason why the proof was unsuccessful...
						String proofs = ((GeoText) output.get(1)).toString();
						proofs = proofs.substring(1, proofs.length() - 1);
						String[] proof = proofs.split("\n");
						String hint = proof[proof.length - 1]; // use only the last piece of information
						gcc2.setInput(hint); // show hint
					} else {
						gcc2.setInput("The statement could not be proven nor disproven.");
					}
				} else {
					boolean proofResult = ((GeoBoolean) output.get(0)).getBoolean();
					if (proofResult) {
						statementTrue = true;
						if (output.size() == 2) {
							gcc2.setInput("The statement is always true.");
						} else {
							if (output.get(2).toString().equals("\"c\"")) {
								gcc2.setInput("The statement is true on parts, false on parts.");
								statementTrue = false; // Unimplemented. TODO.
							} else {
								gcc2.setInput(
										"The statement is true under some non-degeneracy conditions (see below).");
							}
						}
					} else {
						gcc2.setInput("The statement is false.");
					}
				}
				gcc2.setFontColor(GColor.RED);
				gcc2.computeOutput();
				gcc2.update();
				cons.setCasCellRow(gcc2, rows++);

				if (statementTrue) {
					String proofs = ((GeoText) output.get(output.size() - 1)).toString();
					proofs = proofs.substring(1, proofs.length() - 1);
					if (proofs.length() == 0) {
						proofs = "The statement is trivial.";
					}
					else if (proofs.contains("proves") && proofs.contains("Contradiction!")) {
						proofs = "We prove this by contradiction.\n" + proofs;
					} else {
						proofs = "(Currently no full proof can be provided, but just some steps.)\n" +
								"(In the background, all steps are checked, but a full presentation is not yet implemented.)\n" +
								"(Please try a newer version of GeoGebra Discovery if possible.)\n" + proofs;
					}
					String[] proof = proofs.split("\n");
					int steps = proof.length;
					for (int s = 0; s < steps; s++) {
						String step = proof[s];
						boolean showstep = true;
						if (s < steps - 1) {
							String nextstep = proof[s + 1];
							if (step.endsWith(":") && nextstep.endsWith(":")) {
								showstep = false; // don't show this step,
								// because it contains empty substeps
							}
						}
						if (showstep) {
							GeoCasCell gcc3 = new GeoCasCell(cons);
							if (step.endsWith("0") || step.endsWith("}") || step.startsWith("s")) {
								gcc3.setUseAsText(false); // this is a formula or a list or a syzygy
								if (step.contains(":")) {
									int index = step.indexOf(":");
									String var = step.substring(0, index);
									if (cons.getAllLabels().contains(var)) {
										kernel.undo();
										throw new MyError(loc, "Variable " + var + " is already defined. Please remove it first.");
									}
								}
							} else {
								gcc3.setUseAsText(true);
							}
							gcc3.setInput(step);
							if (step.contains("Let free point")) {
								gcc3.setFontColor(GColor.ORANGE);
							}
							if (step.contains("dependent point") || step.startsWith("v")) {
								gcc3.setFontColor(GColor.DARK_CYAN);
							}
							if (step.contains("degener") || step.startsWith(Unicode.BULLET + "")) {
								gcc3.setFontColor(GColor.MAGENTA);
							}
							if (step.contains("proves")) {
								gcc3.setFontColor(GColor.RED);
								gcc3.setFontStyle(GFont.BOLD);
							}
							if (step.startsWith("(") && step.endsWith(")")) {
								gcc3.setFontStyle(GFont.ITALIC);
								gcc3.setInput(step.substring(1, step.length()-1));
							}
							gcc3.computeOutput();
							gcc3.update();
							cons.setCasCellRow(gcc3, rows++);
						}
					}
				}

				cons.updateConstruction(false);

				algo.remove();
				return null;
			}
			throw argErr(c, arg[0]);

		default:
			throw argNumErr(c);

		}
	}
}