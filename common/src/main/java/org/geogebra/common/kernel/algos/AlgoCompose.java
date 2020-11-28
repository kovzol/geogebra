package org.geogebra.common.kernel.algos;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoLineND;

public class AlgoCompose extends AlgoElement {

    // input
    private GeoElement oo;
    private GeoElement[] il;

    private GeoElement outGeo;

    public AlgoCompose(Construction cons, String label, GeoElement outputObject,
                       GeoList inputList) {
        this(cons, outputObject, inputList);
        outGeo.setLabel(label);
    }

    public AlgoCompose(Construction cons, GeoElement outputObject,
                         GeoList inputList) {
        super(cons);
        il = new GeoElement[inputList.size()];

        input = new GeoElement[2];
        input[0] = outputObject;
        input[1] = inputList;

        for (int i=0; i < il.length; ++i) {
            il[i] = inputList.get(i);
        }
        oo = outputObject;
        outGeo = oo.deepCopyGeo();

        setInputOutput();
        compute();
    }

    @Override
    public Commands getClassName() {
        return Commands.Compose;
    }

    // for AlgoElement
    @Override
    protected void setInputOutput() {
        setOnlyOutput(outGeo);
        setDependencies(); // done by AlgoElement
    }

    public GeoElement getResult() {
        return outGeo;
    }

    @Override
    public final void compute() {
        // get the transformation and the parameter position

        // A is a free point, mirrored about line BC.
        // D is another free point.
        // Compose({D}, {A, A'})
        // should give {{D'}} where D' is the mirror image of D about BC.

        if (il.length != 2) {
            return;
        }

        GeoElement start = il[0]; // A
        GeoElement end = il[1]; // A'

        if (end == null) {
            return;
        }
        AlgoElement ae = end.getParentAlgorithm(); // the map
        if (ae == null) {
            return;
        }

        GeoElement[] inputs = ae.getInput();
        int inputsLength = inputs.length;
        int param = 0; // the index of parameter to be used for the new object in the mapping
        boolean found = false;
        while (!found && param < inputsLength) {
            GeoElement ge = inputs[param];
            if (ge.equals(start)) {
                found = true;
            } else {
                param++;
            }
        }
        if (!found) {
            return;
        }

        AlgoElement nae = null;
        if (ae instanceof AlgoMirror) {
            if (param == 0) {
                GeoElementND about = ae.getInput(1);
                if (about instanceof GeoLineND) {
                    nae = new AlgoMirror(cons, oo, (GeoLineND) about);
                }
                if (about instanceof GeoConic) {
                    nae = new AlgoMirror(cons, oo, (GeoConic) about);
                }
                if (about instanceof GeoPoint) {
                    nae = new AlgoMirror(cons, oo, (GeoPoint) about);
                }
            }
            if (param == 1) {
                GeoElement about = ae.getInput(0).toGeoElement();
                if (oo instanceof GeoLineND) {
                    nae = new AlgoMirror(cons, about, (GeoLineND) oo);
                }
                if (oo instanceof GeoConic) {
                    nae = new AlgoMirror(cons, about, (GeoConic) oo);
                }
                if (oo instanceof GeoPoint) {
                    nae = new AlgoMirror(cons, about, (GeoPoint) oo);
                }
            }
        }
        if (nae == null) {
            return;
        }
        outGeo = nae.getOutput(0);
        outGeo.setParentAlgorithm(this);

    }
}
