package org.geogebra.common.kernel.prover.discovery;

import static java.util.Arrays.sort;

import java.util.HashSet;
import java.util.Iterator;

import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoPoint;

public class Circle {
    private HashSet<Point> points = new HashSet<Point>();
    private GeoConic geoConic;

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

    public Circle(Point p1, Point p2, Point p3) {
        points.add(p1);
        points.add(p2);
        points.add(p3);
    }

    public HashSet<Point> getPoints() {
        return points;
    }

    public GeoPoint[] getPoints3() {
        GeoPoint[] ps = new GeoPoint[3];
        Iterator<Point> it = points.iterator();
        ps[0] = it.next().getGeoPoint();
        ps[1] = it.next().getGeoPoint();
        ps[2] = it.next().getGeoPoint();
        return ps;
    }

    public void concyclic(Point p) {
        points.add(p);
    }

    public void deletePoint(Point p) {
        if (!points.contains(p)) {
            return; // do nothing
        }
        points.remove(p);
    }

    // Literally the same as Line.toString()
    public String toString() {
        GeoPoint[] geoPoints = new GeoPoint[points.size()];
        int i = 0;
        for (Point p : points) {
            geoPoints[i] = p.getGeoPoint();
            i++;
        }
        sort(geoPoints);
        String ret = "";

        for (GeoPoint gp : geoPoints) {
            String label = gp.getColoredLabel();
            label = label.replaceAll("\\<font color[^>]*>(.*?)\\</font>","$1");
            ret += label;
            i++;
        }
        return ret;
    }

    public GeoConic getGeoConic() {
        return geoConic;
    }

    public void setGeoConic(GeoConic gc) {
        geoConic = gc;
    }

}
