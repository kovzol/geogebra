package org.geogebra.common.kernel.advanced;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.commands.CommandProcessor;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.MyError;

/**
 * ToolImage
 */
public class CmdDiscover extends CommandProcessor {
    /**
     * Create new command processor
     *
     * @param kernel
     *            kernel
     */
    public CmdDiscover(Kernel kernel) {
        super(kernel);
    }

    @Override
    public GeoElement[] process(Command c) throws MyError {

        int n = c.getArgumentNumber();
        GeoElement[] arg;
        arg = resArgs(c);

        switch (n) {
            case 1:
                if (arg[0] instanceof GeoElement) {

                    app.showDiscover(arg[0]);
                    return arg;
                }
                throw argErr(c, arg[0]);

            default:
                throw argNumErr(c);

        }
    }
}