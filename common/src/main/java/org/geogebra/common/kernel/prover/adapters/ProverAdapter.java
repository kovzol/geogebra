package org.geogebra.common.kernel.prover.adapters;

import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

public class ProverAdapter {
	protected PPolynomial[] botanaPolynomials;
	protected PVariable[] botanaVars;
	protected String[] botanaVarsDescr;

	public PVariable[] getBotanaVars() {
		return botanaVars;
	}

	public String[] getBotanaVarsDescr() { return botanaVarsDescr; }
}
