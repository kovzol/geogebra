package org.geogebra.common.kernel.prover.discovery;

import static java.util.Arrays.sort;

import java.util.HashSet;

import org.geogebra.common.kernel.geos.GeoPoint;

public class Point {
	private HashSet<GeoPoint> points = new HashSet<GeoPoint>();
	private GeoPoint geoPoint;

	public Boolean getTrivial() {
		return trivial;
	}

	public boolean isTheorem() {
		if (trivial != null && !trivial) {
			return true;
		}
		return false;
	}

	public void setTrivial(Boolean trivial) {
		this.trivial = trivial;
	}

	private Boolean trivial;

	public Point(GeoPoint p1) {
		points.add(p1);
	}

	public HashSet<GeoPoint> getPoints() {
		return points;
	}

	public void identical(GeoPoint p) {
		points.add(p);
	}

	public void deletePoint(GeoPoint p) {
		if (!points.contains(p)) {
			return; // do nothing
		}
		points.remove(p);
	}

	public String toString() {
		String[] labels = new String[points.size()];
		int i = 0;
		for (GeoPoint p : points) {
			labels[i] = p.getLabelSimple();
			i++;
		}
		sort(labels);
		StringBuilder ret = new StringBuilder();
		for (String l : labels) {
			ret.append(l + "=");
		}
		ret.deleteCharAt(ret.length() - 1);
		return ret.toString();
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public void setGeoPoint(GeoPoint gp) {
		geoPoint = gp;
	}

}
