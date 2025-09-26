package org.geogebra.common.kernel.prover;

import java.util.TreeSet;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.util.Prover;
import org.geogebra.common.util.Prover.ProofResult;
import org.geogebra.common.util.debug.Log;

public class ProverCNIMethod {

	public static ProofResult prove(Prover prover) {

		GeoElement statement = prover.getStatement();

		TreeSet<GeoElement> allPredecessors = statement.getAllPredecessors();

		Log.debug("CNI prover finished");
		return ProofResult.UNKNOWN;

	}

}
