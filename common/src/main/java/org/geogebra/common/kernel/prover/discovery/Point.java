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
		geoPoint = p1;
	}

	public HashSet<GeoPoint> getPoints() {
		return points;
	}

	public void identical(GeoPoint p) {
		points.add(p);
		if (geoPoint.getLabelSimple().compareTo(p.getLabelSimple()) > 0) {
			geoPoint = p;
		}
	}

	public String toString() {
		GeoPoint[] geoPoints = new GeoPoint[points.size()];
		int i = 0;
		for (GeoPoint p : points) {
			geoPoints[i] = p;
			i++;
		}
		sort(geoPoints);
		StringBuilder ret = new StringBuilder();
		for (GeoPoint gp : geoPoints) {
			ret.append(gp.getColoredLabel() + "=");
		}
		ret.deleteCharAt(ret.length() - 1);
		return ret.toString();
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

}
