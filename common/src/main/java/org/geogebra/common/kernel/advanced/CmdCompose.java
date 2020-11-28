package org.geogebra.common.kernel.advanced;

import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.algos.AlgoCompose;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.commands.CommandProcessor;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.main.MyError;

public class CmdCompose extends CommandProcessor {
    /**
     * Create new command processor
     *
     * @param kernel kernel
     */
    public CmdCompose(Kernel kernel) {
        super(kernel);
    }

    @Override
    public GeoElement[] process(Command c)
            throws MyError, CircularDefinitionException {
        int n = c.getArgumentNumber();
        GeoElement[] arg;
        arg = resArgs(c);
        if (n == 2) {
            if (arg[1] instanceof GeoList) {
                AlgoCompose algo = new AlgoCompose(cons, c.getLabel(), arg[0],
                        (GeoList) arg[1]);

                GeoElement[] ret = {algo.getResult()};
                return ret;

            }
        }
        throw argNumErr(c);

    }

}