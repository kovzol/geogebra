package org.geogebra.common.kernel.prover.discovery;
import static java.util.Arrays.sort;

import java.util.HashSet;

import org.geogebra.common.awt.GColor;

import com.himamis.retex.editor.share.util.Unicode;

public class OrthogonalParallelLines {
	private ParallelLines pl1, pl2;
	private Boolean trivial;
	private GColor color;

	public Boolean getTrivial() {
		return trivial;
	}

	public boolean isTheorem() {
		if (trivial != null && !trivial) {
			return true;
		}
		return false;
	}

	public ParallelLines getFirstParallelLines() {
		return pl1;
	}

	public ParallelLines getOrthogonalParallelLines() {
		return pl2;
	}

	public void setTrivial(Boolean trivial) {
		this.trivial = trivial;
	}

	public OrthogonalParallelLines(ParallelLines d) {
		pl1 = d;
	}

	public OrthogonalParallelLines(ParallelLines d1, ParallelLines d2) {
		pl1 = d1;
		pl2 = d2;
	}

	public void orthogonal(ParallelLines d) {
		pl2 = d;
	}

	public String toString() {
		return toHTML(false);
	}

	public String toHTML(boolean color) {
		if (pl2 == null) {
			if (color) {
				return pl1.toHTML(true);
			}
			return pl1.toString();
		}
		if (pl1.toString().compareTo(pl2.toString()) > 0) {
			String ret;
			if (color) {
				ret = pl2.toHTML(true);
			} else {
				ret = pl2.toString();
			}
			ret += " " + Unicode.PERPENDICULAR + " ";
			if (color) {
				ret = pl1.toHTML(true);
			} else {
				ret = pl1.toString();
			}
		}
		if (color) {
			return pl1.toHTML(true) + " " + Unicode.PERPENDICULAR + " " + pl2.toHTML(true);
		}
		return pl1.toString() + " " + Unicode.PERPENDICULAR + " " + pl2.toString();
	}

	public void setColor(GColor c) {
		color = c;
	}

	public GColor getColor() {
		return color;
	}

}