package org.geogebra.common.kernel.advanced;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.ExpressionValue;
import org.geogebra.common.kernel.arithmetic.FunctionVariable;
import org.geogebra.common.kernel.arithmetic.Inequality;
import org.geogebra.common.kernel.arithmetic.ValidExpression;
import org.geogebra.common.kernel.commands.CommandProcessor;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.geos.GeoFunctionNVar;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.implicit.GeoImplicitCurve;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.main.MyError;

/**
 * ToolImage
 */
public class CmdPlot2D extends CommandProcessor {
	/**
	 * Create new command processor
	 *
	 * @param kernel
	 *            kernel
	 */
	public CmdPlot2D(Kernel kernel) {
		super(kernel);
	}

	@Override
	public GeoElement[] process(Command c) throws MyError {

		int n = c.getArgumentNumber();
		GeoElement[] arg;
		arg = resArgs(c);

		switch (n) {
		case 1:
			if (arg[0] instanceof GeoFunctionNVar) {
				GeoFunctionNVar geoFunctionNVar = (GeoFunctionNVar) arg[0];
				FunctionVariable[] fvars = geoFunctionNVar.getFunctionVariables();
				if (fvars.length > 2) {
					throw new MyError(app.getLocalization(), "InvalidEquation");
				}
				AlgoPlot2D algo = new AlgoPlot2D(cons, c.getLabel(), (GeoFunctionNVar) arg[0]);
				return algo.getOutput();
			} else if (arg[0] instanceof GeoImplicitCurve || arg[0] instanceof GeoLine ||
					arg[0] instanceof GeoFunction || arg[0] instanceof GeoConic) {
				AlgoPlot2D algo = new AlgoPlot2D(cons, c.getLabel(), arg[0]);
				return algo.getOutput();
			}
			throw argErr(c, arg[0]);

		default:
			throw argNumErr(c);

		}
	}
}