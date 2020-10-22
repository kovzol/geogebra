package org.geogebra.common.kernel.prover.discovery;

import static java.util.Arrays.sort;

import java.util.HashSet;
import java.util.Iterator;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;

public class Line {
    private HashSet<Point> points = new HashSet<Point>();
    private GeoLine geoLine;

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

    public Line(Point p1, Point p2) {
        points.add(p1);
        points.add(p2);
    }

    public HashSet<Point> getPoints() {
        return points;
    }

    // Here we prefer the first two points that were created.
    // Reason: they are more probably uniquely defined and the chance
    // of selecting a point that leads to a component-like object
    // is smaller. Also, this ensures deterministic output.
    public GeoPoint[] getPoints2() {
        GeoPoint[] ps0 = new GeoPoint[points.size()];
        Iterator<Point> it = points.iterator();
        int i = 0;
        while (it.hasNext()) {
            ps0[i] = it.next().getGeoPoint();
            i++;
        }
        sort(ps0);
        GeoPoint[] ps = new GeoPoint[2];
        ps[0] = ps0[0];
        ps[1] = ps0[1];
        return ps;
        /*
        Iterator<Point> it = points.iterator();
        ps[0] = it.next().getGeoPoint();
        ps[1] = it.next().getGeoPoint();
        return ps;
         */
    }

    public void collinear(Point p) {
        points.add(p);
    }

    public void deletePoint(Point p) {
        if (!points.contains(p)) {
            return; // do nothing
        }
        points.remove(p);
    }

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

    public GeoLine getGeoLine() {
        return geoLine;
    }

    public void setGeoLine(GeoLine gl) {
        geoLine = gl;
    }

}
