package org.geogebra.common.kernel.prover.discovery;

import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;

public class Segment {
    private Point startPoint, endPoint;
    private GeoSegment geoSegment;

    public Segment(Point p1, Point p2) {
        startPoint = p1;
        endPoint = p2;
    }

    public String toString() {
        String p1 = startPoint.getGeoPoint().getLabelSimple();
        String p2 = endPoint.getGeoPoint().getLabelSimple();
        if (p1.compareTo(p2) > 0) {
            // swap them if the alphabetic order is wrong
            String p = p1;
            p1 = p2;
            p2 = p;
        }
        return p1 + p2;
    }

    public GeoSegment getGeoSegment() {
        return geoSegment;
    }

    public Point getStartPoint() { return startPoint; }

    public Point getEndPoint() { return endPoint; }

    public void setGeoSegment(GeoSegment gs) {
        geoSegment = gs;
    }

}
