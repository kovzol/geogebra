package org.geogebra.common.kernel.algos;

import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

/**
 * This interface describes the symbolic parameters of algorithms for the Botana
 * method. Based on Simon's SymbolicParametersAlgo.java.
 * 
 * @author Zoltan Kovacs
 * @author Jonathan H. Yu
 */
public interface SymbolicParametersBotanaAlgo {

	/**
	 * Calculates the variables of an object for the Botana method
	 * 
	 * @param geo
	 *            The corresponding GeoElement
	 * @return array of the variables
	 * @throws NoSymbolicParametersException
	 *             if it is not possible to obtain suitable polynomials
	 */
	public PVariable[] getBotanaVars(GeoElementND geo)
			throws NoSymbolicParametersException;

	/**
	 * Gives the description of variables of an object for the Botana method
	 *
	 * @param geo
	 *            The corresponding GeoElement
	 * @return array of descriptions, null if it should already be defined
	 * @throws NoSymbolicParametersException
	 *             if it is not possible to obtain suitable polynomials
	 */

	public String[] getBotanaVarsDescr(GeoElementND geo)
			throws NoSymbolicParametersException;


	/**
	 * Calculates the polynomials of an object for the Botana method
	 * 
	 * @param geo
	 *            The corresponding GeoElement
	 * @return array of the polynomials
	 * @throws NoSymbolicParametersException
	 *             if it is not possible to obtain suitable polynomials
	 */
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException;

	/**
	 * Deletes all entries computed already by the prover
	 */
	public void reset();
}
