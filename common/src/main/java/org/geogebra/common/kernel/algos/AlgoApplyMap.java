package org.geogebra.common.kernel.algos;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoLineND;

public class AlgoApplyMap extends AlgoElement {

    // input
    private GeoElement oo;
    private GeoElement[] il;

    private GeoElement outGeo;

    public AlgoApplyMap(Construction cons, String label, GeoList inputList,
            GeoElement outputObject) {
        this(cons, inputList, outputObject);
        outGeo.setLabel(label);
    }

    public AlgoApplyMap(Construction cons, GeoList inputList, GeoElement outputObject) {
        super(cons);
        il = new GeoElement[inputList.size()];

        input = new GeoElement[2];
        input[0] = inputList;
        input[1] = outputObject;

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
        return Commands.ApplyMap;
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
        // ApplyMap({A, A'}, {D})
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

        if (!(oo instanceof GeoList)) {
            outGeo = apply(ae, oo, param);
        } else {
            // outGeo = new GeoList(cons);
            ((GeoList) outGeo).clear();
            int s = ((GeoList) oo).size();
            for (int i = 0; i < s; ++i) {
                GeoElement obj = apply(ae, ((GeoList) oo).get(i), param);
                ((GeoList) outGeo).add(obj);
            }
        }
    }

    GeoElement apply(AlgoElement algo, GeoElement obj, int p) {
        GeoElement og;
        AlgoElement nae = null;
        if (algo instanceof AlgoMirror) {
            if (p == 0) {
                GeoElementND about = algo.getInput(1);
                if (about instanceof GeoLineND) {
                    nae = new AlgoMirror(cons, obj, (GeoLineND) about);
                }
                if (about instanceof GeoConic) {
                    nae = new AlgoMirror(cons, obj, (GeoConic) about);
                }
                if (about instanceof GeoPoint) {
                    nae = new AlgoMirror(cons, obj, (GeoPoint) about);
                }
            }
            if (p == 1) {
                GeoElement about = algo.getInput(0).toGeoElement();
                if (obj instanceof GeoLineND) {
                    nae = new AlgoMirror(cons, about, (GeoLineND) obj);
                }
                if (obj instanceof GeoConic) {
                    nae = new AlgoMirror(cons, about, (GeoConic) obj);
                }
                if (obj instanceof GeoPoint) {
                    nae = new AlgoMirror(cons, about, (GeoPoint) obj);
                }
            }
        }
        if (nae == null) {
            return null;
        }
        og = nae.getOutput(0);
        // og.setLabel(obj.getLabelSimple()+"'");
        // og.setParentAlgorithm(this);
        og.setAdvancedVisualStyle(obj);
        // nae.remove();
        return og;

    }

}
