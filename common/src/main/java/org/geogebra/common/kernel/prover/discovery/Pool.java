package org.geogebra.common.kernel.prover.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.prover.AlgoProveDetails;
import org.geogebra.common.kernel.prover.Combinations;
import org.geogebra.common.util.MaxSizeHashMap;
import org.geogebra.common.util.debug.Log;

public class Pool {

    public ArrayList<Point> points = new ArrayList<>();
    public ArrayList<Line> lines = new ArrayList<>();
    public ArrayList<Circle> circles = new ArrayList<>();
    public ArrayList<ParallelLines> directions = new ArrayList<>();
    public ArrayList<Segment> segments = new ArrayList<>();
    public ArrayList<EqualLongSegments> equalLongSegments = new ArrayList<>();
    public ArrayList<OrthogonalParallelLines> orthogonalParallelLines = new ArrayList<>();

    public MaxSizeHashMap<String, GeoList> algoProveDetailsCache = new MaxSizeHashMap<>(5000);
    private boolean enabled = false;

    public GeoList AlgoProveDetailsCached (GeoElement root, String command) {
        // String command = root.getParentAlgorithm().toString();
        if (algoProveDetailsCache.containsKey(command)) {
            return algoProveDetailsCache.get(command);
        }
        AlgoProveDetails apd = new AlgoProveDetails(root.getConstruction(), root, false, true, false);
        apd.compute();
        GeoElement[] o = apd.getOutput();
        GeoList ret = (GeoList) o[0];
        algoProveDetailsCache.put(command, ret);
        apd.remove();
        return ret;
    }

    public Point getPoint(GeoPoint p1) {
        for (Point p : points) {
            HashSet<GeoPoint> points = p.getPoints();
            if (points.contains(p1)) {
                return p;
            }
        }
        return null;
    }

    public Line getLine(Point p1, Point p2) {
        if (p1 != null && p1.equals(p2)) {
            Log.error("getLine() called with p1=p2=" + p1.getGeoPoint().getLabelSimple());
            return null;
        }
        for (Line l : lines) {
            HashSet<Point> points = l.getPoints();
            if (points.contains(p1) && points.contains(p2)) {
                return l;
            }
        }
        return null;
    }

    public Circle getCircle(Point p1, Point p2, Point p3) {
        for (Circle c : circles) {
            HashSet<Point> points = c.getPoints();
            if (points.contains(p1) && points.contains(p2) && points.contains(p3)) {
                return c;
            }
        }
        return null;
    }

    public Segment getSegment(Point p1, Point p2) {
        if (p1.equals(p2)) {
            Log.error("getSegment() called with p1=p2=" + p1.getGeoPoint().getLabelSimple());
            return null;
        }
        for (Segment s : segments) {
            if ((p1.equals(s.getStartPoint()) && p2.equals(s.getEndPoint())) ||
                    (p2.equals(s.getStartPoint()) && p1.equals(s.getEndPoint()))) {
                return s;
            }
        }
        return null;
    }

    public boolean pointExists(GeoPoint p1) {
        if (getPoint(p1) == null) {
            return false;
        }
        return true;
    }

    public boolean lineExists(Point p1, Point p2) {
        if (getLine(p1, p2) == null) {
            return false;
        }
        return true;
    }

    public boolean circleExists(Point p1, Point p2, Point p3) {
        if (getCircle(p1, p2, p3) == null) {
            return false;
        }
        return true;
    }

    public boolean segmentExists(Point p1, Point p2) {
        if (getSegment(p1, p2) == null) {
            return false;
        }
        return true;
    }

    public Point addPoint(GeoPoint p1) {
        Point p = getPoint(p1);
        if (p == null) {
            Point point = new Point(p1);
            points.add(point);
            return point;
        }
        return p;
    }

    public Line addLine(Point p1, Point p2) {
        if (p1 != null && p1.equals(p2)) {
            return null;
        }
        Line l = getLine(p1, p2);
        if (l == null) {
            Line line = new Line(p1, p2);
            lines.add(line);
            return line;
        }
        return l;
    }

    public Circle addCircle(Point p1, Point p2, Point p3) {
        Circle c = getCircle(p1, p2, p3);
        if (c == null) {
            Circle circle = new Circle(p1, p2, p3);
            circles.add(circle);
            return circle;
        }
        return c;
    }

    public Segment addSegment(Point p1, Point p2) {
        if (p1.equals(p2)) {
            return null;
        }
        Segment s = getSegment(p1, p2);
        if (s == null) {
            Segment segment = new Segment(p1, p2);
            segments.add(segment);
            return segment;
        }
        return s;
    }

    public ParallelLines addDirection(Line l) {
        ParallelLines pl = getDirection(l);
        if (pl == null) {
            pl = new ParallelLines(l);
            directions.add(pl);
        }
        return pl;
    }

    public EqualLongSegments addEquality(Segment s) {
        EqualLongSegments els = getEqualLongSegments(s);
        if (els == null) {
            EqualLongSegments equalLongSegment = new EqualLongSegments(s);
            equalLongSegments.add(equalLongSegment);
        }
        return els;
    }

    private void setIdentical(Point p, GeoPoint q) {
        /* Claim that q is equivalent to the point(s) p.
         * Consider that A=B and C=D are already identical
         * and it is stated that A=C by the function call.
         * Now all points A, B, C and D must
         * be identical.
         */
        if (p.getPoints().contains(q)) {
            return; // nothing to do
        }
        HashSet<GeoPoint> pointlist = (HashSet<GeoPoint>) p.getPoints().clone();
        HashSet<GeoPoint> pointsToAdd = new HashSet<>();
        pointsToAdd.add(q);
        for (GeoPoint pl : pointlist) {
            Point ps = getPoint(q);
            if (ps != null && !ps.equals(q)) {
                for (GeoPoint ep : ps.getPoints()) {
                    pointsToAdd.add(ep);
                }
                points.remove(ps);
            }
        }
        for (GeoPoint pl : pointsToAdd) {
            p.identical(pl);
        }

    }

    private void setCollinear(Line l, Point p) {
        /* Claim that p lies on l.
         * Consider that 123 and 345 are already collinear
         * and it is stated that 2 lies on 45 by the function call.
         * Since 3 lies on 45 and 23 exists, all points 12345 must
         * be collinear. So we do the following:
         * For each point pl of l (45) we check if the line el joining pl and p
         * (here 23) already exists. If yes, all points ep (1,2,3) of this line el will
         * be claimed to be collinear to l. Finally we remove the line el (23).
         *
         * If there is no such problem, we simply add p to l.
         */
        if (l.getPoints().contains(p)) {
            return; // nothing to do
        }
        HashSet<Point> pointlist = (HashSet<Point>) l.getPoints().clone();
        HashSet<Point> pointsToAdd = new HashSet<>();
        pointsToAdd.add(p);
        for (Point pl : pointlist) {
            Line el = getLine(pl, p);
            if (el != null && !el.equals(l)) {
                for (Point ep : el.getPoints()) {
                    pointsToAdd.add(ep);
                }
                lines.remove(el);
            }
        }
        for (Point pl : pointsToAdd) {
            l.collinear(pl);
        }
    }

    private void setConcylic(Circle c, Point p) {
        /* Claim that p lies on c.
         * Consider that 1236 and 3456 are already concyclic
         * and it is stated that 2 lies on 3456 by the function call.
         * Since 3 lies on 3456 and 236 exists, all points 123456 must
         * be concyclic. So we do the following:
         * For each point pairs ppc (eg. 36) of c (3456) we check if the circle ec that lies on ppc and p
         * (here 1236) already exists. If yes, all points cp (1,2,3,6) of this circle ec will
         * be claimed to be concyclic to c. Finally we remove the circle ec (1238).
         *
         * If there is no such problem, we simply add p to c.
         */
        if (c.getPoints().contains(p)) {
            return; // nothing to do
        }
        Combinations pairlist = new Combinations(c.getPoints(), 2);
        HashSet<Point> pointsToAdd = new HashSet<>();
        pointsToAdd.add(p);
        while (pairlist.hasNext()) {
            Set<Point> ppc = pairlist.next();
            Iterator<Point> i = ppc.iterator();
            Point p1 = i.next();
            Point p2 = i.next();
            Circle ec = getCircle(p1, p2, p);
            if (ec != null && !ec.equals(c)) {
                for (Point cp : ec.getPoints()) {
                    pointsToAdd.add(cp);
                }
                circles.remove(ec);
            }
        }
        for (Point pl : pointsToAdd) {
            c.concyclic(pl);
        }
    }

    public Point addIdenticality(GeoPoint p1, GeoPoint p2) {
        Point p;
        if (pointExists(p1)) {
            p = getPoint(p1);
            setIdentical(p, p2);
            return p;
        }
        if (pointExists(p2)) {
            p = getPoint(p2);
            setIdentical(p, p1);
            return p;
        }
        p = addPoint(p1);
        setIdentical(p, p2);
        return p;
    }

    public Line addCollinearity(Point p1, Point p2, Point p3) {
        Line l;
        if (lineExists(p1, p2)) {
            l = getLine(p1, p2);
            setCollinear(l, p3);
            return l;
        }
        if (lineExists(p1, p3)) {
            l = getLine(p1, p3);
            setCollinear(l, p2);
            return l;
        }
        if (lineExists(p2, p3)) {
            l = getLine(p2, p3);
            setCollinear(l, p1);
            return l;
        }
        l = addLine(p1, p2);
        setCollinear(l, p3);
        return l;
    }

    public Circle addConcyclicity(Point p1, Point p2, Point p3, Point p4) {
        Circle c;
        if (circleExists(p1, p2, p3)) {
            c = getCircle(p1, p2, p3);
            setConcylic(c, p4);
            return c;
        }
        if (circleExists(p1, p2, p4)) {
            c = getCircle(p1, p2, p4);
            setConcylic(c, p3);
            return c;
        }
        if (circleExists(p1, p3, p4)) {
            c = getCircle(p1, p3, p4);
            setConcylic(c, p2);
            return c;
        }
        if (circleExists(p2, p3, p4)) {
            c = getCircle(p2, p3, p4);
            setConcylic(c, p1);
            return c;
        }
        c = addCircle(p1, p2, p3);
        setConcylic(c, p4);
        return c;
    }

    public boolean areIdentical(GeoPoint p1, GeoPoint p2) {
        Point p = getPoint(p1);
        Point q = getPoint(p2);
        if (p == null) {
            return false;
        }
        if (p.equals(q)) {
            return true;
        }
        return false;
    }

    public boolean areCollinear(Point p1, Point p2, Point p3) {
        Line l = getLine(p1, p2);
        if (l != null && l.getPoints().contains(p3)) {
            return true;
        }
        return false;
    }

    public boolean areConcyclic(Point p1, Point p2, Point p3, Point p4) {
        Circle c = getCircle(p1, p2, p3);
        if (c != null && c.getPoints().contains(p4)) {
            return true;
        }
        return false;
    }

    public boolean areParallel(Line l1, Line l2) {
        ParallelLines pl1 = getDirection(l1);
        ParallelLines pl2 = getDirection(l2);
        if (pl1 != null && pl2 != null && pl1.equals(pl2)) {
            return true;
        }
        return false;
    }

    public boolean areEqualLong(Segment s1, Segment s2) {
        EqualLongSegments els1 = getEqualLongSegments(s1);
        EqualLongSegments els2 = getEqualLongSegments(s2);
        if (els1 != null && els2 != null && els1.equals(els2)) {
            return true;
        }
        return false;
    }

    public ParallelLines getDirection(Line l) {
        for (ParallelLines pl : directions) {
            if (pl.getLines().contains(l)) {
                return pl;
            }
        }
        return null;
    }

    public EqualLongSegments getEqualLongSegments(Segment s) {
        for (EqualLongSegments els : equalLongSegments) {
            if (els.getSegments().contains(s)) {
                return els;
            }
        }
        return null;
    }

    public boolean directionExists(Line l) {
        if (getDirection(l) != null) {
            return true;
        }
        return false;
    }

    /* l1 != l2 */
    public ParallelLines addParallelism(Line l1, Line l2) {
        if (l1.equals(l2)) {
            return getDirection(l1); // no action is needed
        }
        ParallelLines dir1 = getDirection(l1);
        ParallelLines dir2 = getDirection(l2);
        if (dir1 == null && dir2 == null) {
            ParallelLines dir = new ParallelLines(l1, l2);
            directions.add(dir);
            return dir;
        }
        if (dir1 != null && dir2 == null) {
            dir1.parallel(l2);
            return dir1;
        }
        if (dir1 == null && dir2 != null) {
            dir2.parallel(l1);
            return dir2;
        }
        // Unifying the two directions as one:
        for (Line l : dir1.getLines()) {
            dir2.parallel(l);
        }
        directions.remove(dir1);
        return dir2;
    }

    public OrthogonalParallelLines addPerpendicularity(ParallelLines d) {
        OrthogonalParallelLines opl = new OrthogonalParallelLines(d);
        orthogonalParallelLines.add(opl);
        return opl;
    }

    public OrthogonalParallelLines addPerpendicularity(ParallelLines d1, ParallelLines d2) {
        for (OrthogonalParallelLines opl : orthogonalParallelLines) {
            if (opl.getFirstParallelLines().equals(d1)) {
                opl.orthogonal(d2);
                return opl;
            }
        }
        OrthogonalParallelLines opl = new OrthogonalParallelLines(d1, d2);
        orthogonalParallelLines.add(opl);
        return opl;
    }

    /* s1 != s2 */
    public EqualLongSegments addEquality(Segment s1, Segment s2) {
        if (s1.equals(s2)) {
            return getEqualLongSegments(s1); // no action is needed
        }
        EqualLongSegments els1 = getEqualLongSegments(s1);
        EqualLongSegments els2 = getEqualLongSegments(s2);
        if (els1 == null && els2 == null) {
            equalLongSegments.add(new EqualLongSegments(s1, s2));
            return els1;
        }
        if (els1 != null && els2 == null) {
            els1.equalLong(s2);
            return els1;
        }
        if (els1 == null && els2 != null) {
            els2.equalLong(s1);
            return els1;
        }
        // Unifying the two sets as one:
        for (Segment s : els1.getSegments()) {
            els2.equalLong(s);
        }
        equalLongSegments.remove(els1);
        return els2;
    }

    public void removePoint(GeoPoint gp) {
        Point p = getPoint(gp);
        if (p == null) {
            return;
        }

        ArrayList<Line> oldLines = (ArrayList<Line>) lines.clone();
        for (Line l : oldLines) {
            if (l.getPoints().contains(p)) {
                l.deletePoint(p);
                if (l.getPoints().size() < 3) {
                    ParallelLines pl = getDirection(l);
                    pl.deleteParallelLine(l);
                    if (pl.getLines().size() == 0) {
                            directions.remove(pl);
                          }
                    lines.remove(l);
                    if (l.getGeoLine() != null) {
                        l.getGeoLine().remove();
                    }
                }
            }
        }
        // TODO: Maybe this is an overkill, but it should work:
        orthogonalParallelLines.clear();

        ArrayList<Circle> oldCircles = (ArrayList<Circle>) circles.clone();
        for (Circle c : oldCircles) {
            if (c.getPoints().contains(p)) {
                c.deletePoint(p);
                if (c.getPoints().size() < 4) {
                    circles.remove(c);
                    if (c.getGeoConic() != null) {
                        c.getGeoConic().remove();
                    }
                }
            }
        }

        ArrayList<Segment> oldSegments = (ArrayList<Segment>) segments.clone();
        for (Segment s : oldSegments) {
            if (p.getPoints().contains(s.getStartPoint().getGeoPoint()) ||
                    p.getPoints().contains(s.getEndPoint().getGeoPoint())) {
                EqualLongSegments els = getEqualLongSegments(s);
                els.deleteSegment(s);
                if (els.getSegments().size() == 0) {
                    equalLongSegments.remove(els);
                }
                segments.remove(s);
                if (s.getGeoSegment() != null) {
                    s.getGeoSegment().remove();
                }

            }
        }
        // This is an overkill. Not all elements need to be deleted:
        algoProveDetailsCache.clear(); // TODO: Find a better way.

        points.remove(p);
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

}

