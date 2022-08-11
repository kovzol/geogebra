package org.geogebra.common.kernel.prover.adapters;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.algos.AlgoConicFivePoints;
import org.geogebra.common.kernel.algos.SymbolicParametersBotanaAlgo;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPolygon;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;
import org.geogebra.common.util.debug.Log;

public class PointOnPathAdapter extends ProverAdapter {

	public PPolynomial[] getBotanaPolynomials(GeoElement path)
			throws NoSymbolicParametersException {
		Kernel kernel = path.getKernel();
		if (botanaPolynomials != null) {
			return botanaPolynomials;
		}

		if (path instanceof GeoLine) {
			if (botanaVars == null) {
				botanaVars = new PVariable[2];
				botanaVars[0] = new PVariable(kernel); // ,true
				botanaVars[1] = new PVariable(kernel);
			}
			PVariable[] fv = ((SymbolicParametersBotanaAlgo) path)
					.getBotanaVars(path); // 4 variables
			if (fv == null) {
				fallback(kernel);
				return null;
			}
			botanaPolynomials = new PPolynomial[1];
			botanaPolynomials[0] = PPolynomial.collinear(fv[0], fv[1], fv[2],
					fv[3], botanaVars[0], botanaVars[1]);
			return botanaPolynomials;
		}

		if (path instanceof GeoConic) {
			if (((GeoConic) path).isCircle()) {
				if (botanaVars == null) {
					botanaVars = new PVariable[2];
					botanaVars[0] = new PVariable(kernel); // ,true
					botanaVars[1] = new PVariable(kernel);
				}
				PVariable[] fv = ((SymbolicParametersBotanaAlgo) path)
						.getBotanaVars(path); // 4 variables
				if (fv == null) {
					fallback(kernel);
					return null;
				}

				botanaPolynomials = new PPolynomial[1];
				// If this new point is D, and ABC is already a triangle with
				// the circumcenter O,
				// then here we must claim that e.g. AO=OD:
				botanaPolynomials[0] = PPolynomial.equidistant(fv[2], fv[3],
						fv[0], fv[1], botanaVars[0], botanaVars[1]);
				return botanaPolynomials;
			}
			if (((GeoConic) path).isParabola()) {
				if (botanaVars == null) {
					botanaVars = new PVariable[4];
					// point P on parabola
					botanaVars[0] = new PVariable(kernel); // ,true
					botanaVars[1] = new PVariable(kernel);
					// T- projection of P on AB
					botanaVars[2] = new PVariable(kernel);
					botanaVars[3] = new PVariable(kernel);
				}
				PVariable[] vparabola = ((SymbolicParametersBotanaAlgo) path)
						.getBotanaVars(path);
				if (vparabola == null) {
					fallback(kernel);
					return null;
				}

				/* We take the Botana polynomials of the parabola definition
				 * and substitute the first four variables by the current first four variables.
				 */
				PPolynomial[] parabolaPolys = ((SymbolicParametersBotanaAlgo) path)
						.getBotanaPolynomials(path);
				int neededPolys = parabolaPolys.length;
				botanaPolynomials = new PPolynomial[neededPolys];
				for (int i = 0; i < neededPolys; i++) {
					botanaPolynomials[i] = parabolaPolys[i];
					for (int j = 0; j < 4; j++) {
						botanaPolynomials[i] =
								botanaPolynomials[i].substitute(vparabola[j], botanaVars[j]);
					}
				}
				return botanaPolynomials;
			}
			if (((GeoConic) path).isEllipse()
					|| ((GeoConic) path).isHyperbola()) {
				if (botanaVars == null) {
					botanaVars = new PVariable[2];
					// P - point on ellipse/hyperbola
					botanaVars[0] = new PVariable(kernel); // ,true
					botanaVars[1] = new PVariable(kernel);
				}

				PVariable[] vellipse = ((SymbolicParametersBotanaAlgo) path)
						.getBotanaVars(path);
				if (vellipse == null) {
					fallback(kernel);
					return null;
				}

				/* We take the Botana polynomial of the ellipse definition
				 * and substitute its Botana variables by the current Botana variables.
				 */
				PPolynomial ellipsePoly = ((SymbolicParametersBotanaAlgo) path)
						.getBotanaPolynomials(path)[0];
				botanaPolynomials = new PPolynomial[1];
				botanaPolynomials[0] = ellipsePoly;
				for (int j = 0; j < 2; j++) {
					botanaPolynomials[0] =
						botanaPolynomials[0].substitute(vellipse[j], botanaVars[j]);
					}

				return botanaPolynomials;

			}
		}
		if (path instanceof GeoPolygon) {
			Log.debug("Putting a point on the perimeter of a polygon is not yet supported");
			throw new NoSymbolicParametersException();
		}
		fallback(kernel);
		return null;
		// throw new NoSymbolicParametersException();
	}

	void fallback(Kernel kernel) {
		// In the general case set up two dummy variables. They will be used
		// by the numerical substitution later in the prover.
		if (botanaVars != null) {
			return;
		}
		botanaVars = new PVariable[2];
		botanaVars[0] = new PVariable(kernel);
		botanaVars[1] = new PVariable(kernel);
	}

}
