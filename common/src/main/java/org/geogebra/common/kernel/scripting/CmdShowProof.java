package org.geogebra.common.kernel.scripting;

import static org.geogebra.common.main.App.VIEW_CAS;

import org.geogebra.common.awt.GColor;
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

				int rows;
				rows = cv.getConsoleTable().getRowCount();

				AlgoProveDetails algo = new AlgoProveDetails(cons, arg[0], false, false, true);
				String statementText = algo.statementText(arg[0]);

				GeoCasCell gcc1 = new GeoCasCell(cons);
				gcc1.setInput(statementText);
				gcc1.setUseAsText(true);
				gcc1.setFontColor(GColor.BLUE);
				gcc1.computeOutput();
				gcc1.update();
				cons.setCasCellRow(gcc1, rows++);

				GeoList output = algo.getGeoList();
				GeoCasCell gcc2 = new GeoCasCell(cons);
				gcc2.setUseAsText(true);
				if (output.size() == 0) {
					gcc2.setInput("The statement could not be proven nor disproven.");
				} else {
					boolean proofResult = ((GeoBoolean) output.get(0)).getBoolean();
					if (proofResult) {
						gcc2.setInput("The statement is true.");
					} else {
						gcc2.setInput("The statement is false.");
					}
				}
				gcc2.setFontColor(GColor.RED);
				gcc2.computeOutput();
				gcc2.update();
				cons.setCasCellRow(gcc2, rows++);

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