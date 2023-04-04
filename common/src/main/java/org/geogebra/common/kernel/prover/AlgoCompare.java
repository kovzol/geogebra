package org.geogebra.common.kernel.prover;

import static org.apache.commons.math3.util.ArithmeticUtils.lcm;
import static org.geogebra.common.kernel.prover.ProverBotanasMethod.AlgebraicStatement;
import static org.geogebra.common.kernel.prover.ProverBotanasMethod.paramLookup;

import org.geogebra.common.cas.realgeom.Compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.geogebra.common.cas.GeoGebraCAS;
import org.geogebra.common.cas.realgeom.RealGeomWebService;
import org.geogebra.common.factories.UtilFactory;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoDependentBoolean;
import org.geogebra.common.kernel.algos.AlgoDependentNumber;
import org.geogebra.common.kernel.algos.AlgoDistancePoints;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoJoinPointsSegment;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.arithmetic.ExpressionValue;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Localization;
import org.geogebra.common.main.ProverSettings;
import org.geogebra.common.main.RealGeomWSSettings;
import org.geogebra.common.plugin.Operation;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.debug.Log;

import com.himamis.retex.editor.share.util.Unicode;

/**
 * Compares two objects geometrically by using real quantifier elimination.
 *
 * @author Zoltan Kovacs <zoltan@geogebra.org>
 */
public class AlgoCompare extends AlgoElement {

    private GeoElement inpElem[] = new GeoElement[2];
    private String lr_var[] = new String[2];
    private String lr_expr[] = new String[2];
    private int deg[] = new int[2];
    private int exponent[] = new int[2];
    private String inp[] = new String[2];
    private boolean htmlMode;

    private GeoText outputText; // output

    private String cachedProblem = null;

    /**
     * Compares two objects
     *
     * @param cons          The construction the objects depend on
     * @param inputElement1 the first object
     * @param inputElement2 the second object
     */
    public AlgoCompare(Construction cons, GeoElement inputElement1,
            GeoElement inputElement2, boolean htmlMode) {
        super(cons);
        this.inpElem[0] = inputElement1;
        this.inpElem[1] = inputElement2;
        this.htmlMode = htmlMode;

        outputText = new GeoText(cons);

        setInputOutput();
        compute();

    }

    /**
     * Compares two objects
     *
     * @param cons          The construction the objects depend on
     * @param label         the label for the AlgoAreCompare object
     * @param inputElement1 the first object
     * @param inputElement2 the second object
     */
    public AlgoCompare(Construction cons, String label,
            GeoElement inputElement1, GeoElement inputElement2, boolean htmlMode) {
        this(cons, inputElement1, inputElement2, htmlMode);
        outputText.setLabel(label);
    }

    @Override
    public Commands getClassName() {
        return Commands.Compare;
    }

    @Override
    protected void setInputOutput() {
        input = new GeoElement[2];
        input[0] = inpElem[0];
        input[1] = inpElem[1];

        super.setOutputLength(1);
        super.setOutput(0, outputText);
        setDependencies(); // done by AlgoElement
    }

    /**
     * Gets the result of the test
     *
     * @return the result of comparison
     */

    public GeoText getResult() {
        return outputText;
    }

    AlgebraicStatement as;

    StringTemplate portableFormat = StringTemplate.casCopyTemplate;
    StringTemplate fancyFormat = StringTemplate.algebraTemplate;

    private double startTime;
    private String retval = "";

    private void debugElapsedTime() {
        int elapsedTime = (int) (UtilFactory.getPrototype().getMillisecondTime()
                - startTime);

        /*
         * Don't remove this. It is needed for automated testing. (String match
         * is assumed.)
         */
        Log.debug("Benchmarking: " + elapsedTime + " ms");
        Log.debug("COMPARISON RESULT IS " + retval);
    }

    /*
     * Convert Sqrt[...Sqrt[...]...] type expressions to Unicode variants.
     */
    String squareRootToUnicode(String in) {
        String ret;
        String sqrt = "Sqrt";
        boolean found;
        do {
            found = false;
            int pos = in.indexOf(sqrt);
            if (pos >= 0) {
                found = true;
                int repl = pos + sqrt.length() + 1;
                int parens = 1;
                boolean justdigits = true;
                while (parens > 0 && repl < in.length()) {
                    repl++;
                    if (in.charAt(repl) == '[') {
                        parens++;
                    } else if (in.charAt(repl) == ']') {
                        parens--;
                    } else if (in.charAt(repl) < '0' || in.charAt(repl) > '9') {
                        justdigits = false;
                    }
                }
                ret = in.substring(0, pos) + Unicode.SQUARE_ROOT;
                if (!justdigits) {
                    ret += "(";
                }
                ret += in.substring(pos + sqrt.length() + 1, repl);
                if (!justdigits) {
                    ret += ")";
                }
                ret += in.substring(repl + 1);
                in = ret;
            }
        } while (found);
        return in;
    }

    private String computeSegmentLabel(GeoElement inputElement) {
        String inp;
        if (htmlMode) {
            inp = inputElement.getColoredLabel();
            if (inp == null) {
                inp = inputElement.getDefinition(fancyFormat);
            }
        } else {
            inp = inputElement.getLabelSimple();
            if (inp == null) {
                inp = inputElement.getDefinition(portableFormat);
            }
        }
        return inp;
    }

    private String computeNumericLabel(int i) {
        GeoElement inputElement = inpElem[i];
        String var = lr_var[i];
        String inp;
        extraPolys.add("-" + var + "+(" + lr_expr[i] + ")^EXPONENT" + i);
        extraVars.add(var);
        if (htmlMode) {
            if (inputElement.getLabelSimple() != null) {
                inp = inputElement.getColoredLabel();
            } else {
                inp = "(" + inputElement.getColoredLabel() + ")";
            }
        } else {
            if (inputElement.getLabelSimple() != null) {
                inp = inputElement.getLabelSimple();
            } else {
                inp = "(" + inputElement.getDefinition(fancyFormat) + ")";
            }
        }
        return inp;

    }

    ArrayList<String> extraPolys = new ArrayList<>();
    ArrayList<String> extraVars = new ArrayList<>();
    private String or;

    GeoGebraCAS cas;

    @Override
    public final void compute() {

        if (inpElem[0].getKernel().isSilentMode()) {
            return;
        }

        // setInputOutput();
        do {
            cons.removeFromAlgorithmList(this);
        } while (cons.getAlgoList().contains(this));
        // Adding this again:
        cons.addToAlgorithmList(this);
        cons.removeFromConstructionList(this);
        // Adding this again:
        cons.addToConstructionList(this, true);
        // TODO: consider moving setInputOutput() out from compute()

        RealGeomWebService realgeomWS = cons.getApplication().getRealGeomWS();

        // Let us construct the command Prove(inpElem[0]==inpElem[1]) to translate the
        // underlying construction easily.
        ExpressionValue[] ve = new ExpressionValue[2];
        ExpressionNode[] en = new ExpressionNode[2];
        for (int i = 0; i < 2; ++i) {
            if (inpElem[i] instanceof GeoSegment) {
                ve[i] = inpElem[i].toValidExpression();
                en[i] = new ExpressionNode(kernel, ve[i]);
            }

            if (inpElem[i] instanceof GeoNumeric) {
                en[i] = inpElem[i].getDefinition();
            }
        }
        ExpressionNode root = new ExpressionNode(kernel, en[0], Operation.EQUAL_BOOLEAN, en[1]);

        AlgoDependentBoolean adb = new AlgoDependentBoolean(cons, root);
        GeoBoolean gb = new GeoBoolean(cons);
        gb.setParentAlgorithm(adb);
        Prover p = UtilFactory.getPrototype().newProver();
        p.setProverEngine(Prover.ProverEngine.BOTANAS_PROVER);
        as = new AlgebraicStatement(gb, null, p, true);
        as.removeThesis();

        String mepCode = null;
        try {
            mepCode = adb.minimalExtendedPolyGiacCode();
        } catch (NoSymbolicParametersException e) {
            Log.debug("Error during creating MEP code");
            return;
        }
        String pCode = adb.exprGiacCode();
        cas = (GeoGebraCAS) kernel.getGeoGebraCAS();
        try {
            String dummyThesisEq = cas.evaluateRaw(pCode);
            lr_expr[0] = as.trimBraces(cas.evaluate(adb.getProverAdapter().exprCodeLeft()));
            lr_expr[1] = as.trimBraces(cas.evaluate(adb.getProverAdapter().exprCodeRight()));
        } catch (Throwable t) {
            Log.debug("Error on computing dummy thesis eq");
            return;
        }
        gb.remove();

        inp[0] = "";
        inp[1] = "";

        String currentProblem = p.getTextFormat(p.getStatement(), false);
        Localization loc = kernel.getLocalization();
        String def1 = inpElem[0].getDefinitionDescription(fancyFormat);
        String def2 = inpElem[1].getDefinitionDescription(fancyFormat);
        def1 = (def1.substring(0,1)).toLowerCase(Locale.ROOT) + def1.substring(1);
        def2 = (def2.substring(0,1)).toLowerCase(Locale.ROOT) + def2.substring(1);
        currentProblem += loc.getPlain("CompareAandB", def1, def2);
        Log.debug("currentProblem = " + currentProblem);
        // Log.debug("cachedProblem = " + cachedProblem);
        if (cachedProblem != null && currentProblem.equals(cachedProblem)) {
            return;
        }
        cachedProblem = currentProblem;

        // Adding benchmarking:
        startTime = UtilFactory.getPrototype().getMillisecondTime();

        try {
            for (int i = 0; i < 2; i++) {
                if (inpElem[i] instanceof GeoSegment) {
                    lr_var[i] = adb.getBotanaVar(inpElem[i]).toString();
                    if (!extraVars.contains(lr_var[i])) {
                        extraVars.add(lr_var[i]);
                    }
                    inp[i] = computeSegmentLabel(inpElem[i]);
                    deg[i] = 1;
                }
            }

            for (int i = 0; i < 2; i++) {
                if (inpElem[i] instanceof GeoNumeric) {
                    lr_var[i] = "w" + (i+1);
                    inp[i] = computeNumericLabel(i);
                    deg[i] = as.getDegree(lr_expr[i]);
                    if (deg[i] == -1) {
                        // The expression is not homogeneous, no general statement exists.
                        // TODO: Maybe here "false" should be given. Discuss.
                        outputText.setTextString(retval);
                        return;
                    }
                }
            }

        } catch (Throwable t) {
            // Maybe there is something unimplemented.
            // In such cases we just acknowledge this and return with no result.
            outputText.setTextString(retval);
            return;
        }

        if (deg[0] * deg[1]==0) {
            // One of the expressions is a constant. This is probably a false statement. TODO: Discuss.
            outputText.setTextString(retval);
            return;
        }

        int l = lcm(deg[0], deg[1]);
        exponent[0] = l/deg[0];
        exponent[1] = l/deg[1];

        for (int j = 0; j < extraPolys.size(); j++) {
            for (int i = 0; i < 2; i++) {
                extraPolys.set(j, extraPolys.get(j).replace("EXPONENT" + i,
                        Integer.toString(exponent[i])));
            }
        }

        // Correct lr_var if it was not updated with the correct exponent:
        for (int i = 0; i < 2; i++) {
            if (inpElem[i] instanceof GeoSegment && exponent[i] > 1) {
                lr_var[i] += "^" + exponent[i];
            }
        }

        for (String v : extraVars) {
            as.addExtVar(v);
        }
        for (String po : extraPolys) {
            as.addExtPoly(po);
        }
        // Computation of rgParameters must be done before the first round with elimination
        // (it implicitly computes all strings).
        StringBuilder rgParameters = as.getRGParameters();
        String vars = as.getRGVars();

        // Start of direct Giac computation.
        /* Example code:
           [assume(m>0),solve(eliminate(subst([-v6+v4+v3-v1,-v5-v4+v3+v2,v7+v4-v2-v1,v8-v3-v2+v1,
           -v9^2+v8^2+v7^2-2*v8*v4+v4^2-2*v7*v3+v3^2,-v10^2+v4^2+v3^2-2*v4*v2+v2^2-2*v3*v1+v1^2,-w1+v10+v10,w1*m-(v9)],
           [v1=0,v2=0,v3=0,v4=1]),[v1,v2,v3,v4,v5,v6,v7,v8,v9,v10,w1])[0],m)][1]
         */
        StringBuilder gc = new StringBuilder();
        gc.append("[assume(m>0),solve(eliminate(subst([");
        gc.append(as.getPolys());
        extraPolys = as.getExtPolys(); // Update the polys with the segments,
        // they have been generated in the RG part.
        for (String po : extraPolys) {
            gc.append(",").append(po);
        }
        gc.append(",").append(lr_var[1]).
                append("*m-(").append(lr_var[0]).append(")");
        // Assume that the rhs_var is non-zero (because of non-degeneracy):
        gc.append(",(").append(lr_var[1]).append(")*n-1");
        gc.append("],[");

        StringBuilder varsubst = new StringBuilder();
        int i = 0;
        Set<PVariable> allowedFreeVariables = as.freeVariables;
        allowedFreeVariables.removeAll(as.almostFreeVariables);
        for (PVariable v : allowedFreeVariables) {
            if (i<4) {
                int value = 0;
                if (i == 2)
                    value = 1;
                // 0,0,1,0 according to (0,0) and (1,0)
                if (i > 0)
                    varsubst.append(",");
                varsubst.append(v).append("=").append(value);
                ++i;
            }
        }

        or = loc.getMenu("Symbol.Or").toLowerCase();

        gc.append(varsubst).append("]),[");
        gc.append(vars);
        // Add non-degeneracy nonce variable:
        gc.append(",n");
        gc.append("])[0],m)][1]");
        boolean useGiac = RealGeomWSSettings.isUseGiacElimination();
        boolean useRealGeom = false;
        outputText.setTextString(retval); // retval == "" here

        if (useGiac) {
            try {
                String elimSol = cas.getCurrentCAS().evaluateRaw(gc.toString());
                if (!elimSol.equals("?") && !elimSol.equals("{}")) {
                    elimSol = elimSol.substring(1, elimSol.length() - 1);
                    String[] cases = elimSol.split(",");
                    for (String result : cases) {
                        if (!"".equals(retval)) {
                            retval += " " + or + " ";
                            // Multiple results found, so let the situation be clarified via RealGeom
                            useRealGeom = true;
                        }
                        result = result.replace("m=", "");
                        result = result.replace("*", "" + Unicode.CENTER_DOT);
                        retval += inpWithExponent(0) + " = " + result + " " + Unicode.CENTER_DOT
                                + " " + inpWithExponent(1);
                    }
                    if (retval.contains("ERROR")) {
                        retval = "";
                    }
                    outputText.setTextString(retval);
                    debugElapsedTime();
                    if (!useRealGeom) {
                        return;
                    }
                }
                // The result is not just a number. (Or a set of numbers.)
            } catch (Throwable throwable) {
                Log.debug("Error when trying elimination");
            }
        }
        // End of direct Giac computation.

        String rgCommand = "euclideansolver";
        rgParameters.append("&lhs=" + lr_var[0] + "&" + "rhs=" + lr_var[1]);
        rgParameters.append("&mode=explore");
        String label = cons.getTitle();
        label = label.trim().replaceAll("\\s+", " ");
        Log.debug("constructionLabel = " + label);
        rgParameters.append("&label=" + label);
        Log.debug(rgParameters);

        String rgResult;
        if (realgeomWS != null && realgeomWS.isAvailable()) {
            rgResult = realgeomWS.directCommand(rgCommand, rgParameters.toString());
        } else { // compute locally
            String[] rgs = rgParameters.toString().split("&");
            rgResult =
                    Compute.euclideanSolverExplore(kernel, lr_var[0], lr_var[1], paramLookup(rgs, "ineqs"),
                            paramLookup(rgs, "polys"), paramLookup(rgs, "triangles"),
                            paramLookup(rgs, "vars"), paramLookup(rgs, "posvariables"));
            if (rgResult.contains("ERROR")) {
                retval = "";
                debugElapsedTime();
                outputText.setTextString(retval);
                return;
            }
        }

        rgResult = as.rewriteResult(rgResult);
        String rgwsCas = "";
        if (realgeomWS != null) {
            rgwsCas = realgeomWS.getCAS();
        }

        if (rgwsCas.equals("mathematica") && rgResult != null && !rgResult.equals("")) {
            // If there was some useful result in RealGeom, then use it and forget the previous results from Giac.
            retval = rgMathematica2ggb(rgResult);
        }

        if ((rgwsCas.equals("") || rgwsCas.equals("qepcad") || (rgwsCas.equals("tarski")))
                && rgResult != null && !rgResult.equals("[]") && !rgResult.equals("")) {
            // If there was some useful result in RealGeom, then use it and forget the previous results from Giac.
            retval = rgQepcad2ggb(rgResult);
        }

        debugElapsedTime();
        outputText.setTextString(retval);
    }

    private String rgMathematica2ggb(String rgResult) {
        retval = "";
        String[] cases = rgResult.split("\\|\\|");
        inp[0] = inpWithExponent(0);
        inp[1] = inpWithExponent(1);

        for (String result : cases) {

            if ("m > 0".equals(result)) {
                continue;
            }

            if (!"".equals(retval)) {
                retval += " " + or + " ";
            }

                /*
                String oldResult = "";
                while (!oldResult.equals(result)) {
                    oldResult = result;
                    // This is just a workaround. E.g. "m == Sqrt[40 - 6*Sqrt[3]/3]" is converted to
                    // "m == √(40 - 6*Sqrt[3)/3]" which is syntactically wrong, and also incomplete.
                    // So we repeat this step as many times as it is required.
                    result = result.replaceAll("Sqrt\\[(.*?)\\]", Unicode.SQUARE_ROOT + "$1");
                }
                */
            result = squareRootToUnicode(result);

            // Root[1 - #1 - 2*#1^2 + #1^3 & , 2, 0]
            result = result.replaceAll("Root\\[(.*?) \\& , (.*?), 0\\]", "$2. root of $1");
            result = result.replaceAll("[^\\&]#1", "x");

            // Inequality[0, Less, m, LessEqual, 2]
            result = result.replaceAll("Inequality\\[(.*?), (.*?), m, (.*?), (.*?)\\]",
                    "($1) " + Unicode.CENTER_DOT + " " + inp[1] +
                            " $2 " + inp[0] + " $3 ($4) " + Unicode.CENTER_DOT + " " + inp[1]);
            // Remove "(0)*inp2 Less" from the beginning (it's trivial)
            result = result.replaceAll("^\\(0\\) " + Unicode.CENTER_DOT + " .*? Less ", "");
            // m >= 1/2
            result = result.replaceAll("m >= (.*)",
                    inp[0] + " GreaterEqual ($1) " + Unicode.CENTER_DOT + " " + inp[1]);
            // m > 1/2
            result = result.replaceAll("m > (.*)",
                    inp[0] + " Greater ($1) " + Unicode.CENTER_DOT + " " + inp[1]);
            // m == 1
            result = result.replaceAll("m == (.*)",
                    inp[0] + " = ($1) " + Unicode.CENTER_DOT + " " + inp[1]);

            // remove spaces at parentheses
            result = result.replaceAll("\\(\\s", "(");
            result = result.replaceAll("\\s\\)", ")");

            // Simplify (1)*... to ...
            result = result.replaceAll("\\(1\\)(\\s)" + Unicode.CENTER_DOT + "\\s", "");            // Use math symbols instead of Mathematica notation
            result = result.replace("LessEqual", String.valueOf(Unicode.LESS_EQUAL));
            String repl = "<";
            if (htmlMode) {
                repl = "&lt;";
            }
            result = result.replace("Less", repl);
            result = result.replace("GreaterEqual", String.valueOf(Unicode.GREATER_EQUAL));
            repl = ">";
            if (htmlMode) {
                repl = "&gt;";
            }
            result = result.replace("Greater", repl);
            // result = result.replace("==", "=");
            result = result.replace("&& m > 0", "");
            result = result.replace("m > 0", "");
            result = result.replace("*", "" + Unicode.CENTER_DOT);

            retval += result;
        }
        return retval;
    }

    private String rgQepcad2ggb(String rgResult) {
        retval = "";
        String[] cases = rgResult.split(",");
        inp[0] = inpWithExponent(0);
        inp[1] = inpWithExponent(1);

        for (String result : cases) {

            if (result.indexOf("m") == -1) {
                result = "m=" + result;
            }

            if ("m>0".equals(result)) {
                continue;
            }

            if (!"".equals(retval)) {
                retval += " " + or + " ";
            }

            result = result.replaceAll("sqrt", "√");

            // Root[1 - #1 - 2*#1^2 + #1^3 & , 2, 0]
            result = result.replaceAll("Root\\[(.*?) \\& , (.*?), 0\\]", "$2. root of $1");
            result = result.replaceAll("[^\\&]#1", "x");

            // ((m>=1) and (m<=(sqrt2)))
            result = result.replaceAll("\\(\\(m>=(.*?)\\) and \\(m<=(.*?)\\)\\)",
                    "($1) " + Unicode.CENTER_DOT + " " + inp[1] +
                            " LessEqual " + inp[0] + " LessEqual ($2) " + Unicode.CENTER_DOT + " " + inp[1]);
            // ((m>1) and (m<=(sqrt2)))
            result = result.replaceAll("\\(\\(m>(.*?)\\) and \\(m<=(.*?)\\)\\)",
                    "($1) " + Unicode.CENTER_DOT + " " + inp[1] +
                            " Less " + inp[0] + " LessEqual ($2) " + Unicode.CENTER_DOT + " " + inp[1]);
            // ((m>1) and (m<=(sqrt2)))
            result = result.replaceAll("\\(\\(m>=(.*?)\\) and \\(m<(.*?)\\)\\)",
                    "($1) " + Unicode.CENTER_DOT + " " + inp[1] +
                            " LessEqual " + inp[0] + " Less ($2) " + Unicode.CENTER_DOT + " " + inp[1]);
            // ((m>1) and (m<<(sqrt2)))
            result = result.replaceAll("\\(\\(m>(.*?)\\) and \\(m<(.*?)\\)\\)",
                    "($1) " + Unicode.CENTER_DOT + " " + inp[1] +
                            " Less " + inp[0] + " Less ($2) " + Unicode.CENTER_DOT + " " + inp[1]);

            // Remove "(0)*inp2 Less" from the beginning (it's trivial)
            result = result.replaceAll("^\\(0\\) " + Unicode.CENTER_DOT + " .*? Less ", "");
            // m >= 1/2
            result = result.replaceAll("m>=(.*)",
                    inp[0] + " GreaterEqual ($1) " + Unicode.CENTER_DOT + " " + inp[1]);
            // m > 1/2
            result = result.replaceAll("m>(.*)",
                    inp[0] + " Greater ($1) " + Unicode.CENTER_DOT + " " + inp[1]);
            // m == 1
            result = result.replaceAll("m=(.*)",
                    inp[0] + " = ($1) " + Unicode.CENTER_DOT + " " + inp[1]);

            // remove spaces at parentheses
            result = result.replaceAll("\\(\\s", "(");
            result = result.replaceAll("\\s\\)", ")");

            // Simplify (1)*... to ...
            result = result.replaceAll("\\(1\\)(\\s)" + Unicode.CENTER_DOT + "\\s", "");            // Use math symbols instead of Mathematica notation
            result = result.replace("LessEqual", String.valueOf(Unicode.LESS_EQUAL));
            String repl = "<";
            if (htmlMode) {
                repl = "&lt;";
            }
            result = result.replace("Less", repl);
            result = result.replace("GreaterEqual", String.valueOf(Unicode.GREATER_EQUAL));
            repl = ">";
            if (htmlMode) {
                repl = "&gt;";
            }
            result = result.replace("Greater", repl);
            // result = result.replace("==", "=");
            result = result.replace(",m>0", "");
            result = result.replace("m>0", "");
            result = result.replace("*", "" + Unicode.CENTER_DOT);

            retval += result;
        }
        return retval;
    }

    String inpWithExponent(int i) {
        if (exponent[i]==1) {
            return inp[i];
        }
        if (htmlMode) {
            return "(" + inp[i] + ")<sup>" + exponent[i] + "</sup>";
        }
        return "(" + inp[i] + ")^" + exponent[i];
    }

}
