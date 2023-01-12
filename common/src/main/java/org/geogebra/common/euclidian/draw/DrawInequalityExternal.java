package org.geogebra.common.euclidian.draw;

import java.util.ArrayList;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.GeneralPathClipped;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunctionNVar;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.util.debug.Log;

/**
 * Graphical representation of inequality (external).
 * It uses Tarski to compute an SVG image of the solution of the inequality system
 * and then it processes the image and draws it directly in the EuclideanView.
 */
public class DrawInequalityExternal extends Drawable {

	private boolean isVisible;
	private GeoElementND function;
	private ArrayList<Double> pointvalues;
	private double[][] circlevalues;
	private int lines, circles, height, width;

	public DrawInequalityExternal(EuclidianView view, GeoElementND function) {
		this.view = view;
		this.geo = function.toGeoElement();
		this.function = function;

		update();

	}

	@Override
	public void update() {
		isVisible = geo.isEuclidianVisible();
		if (!isVisible) {
			return;
		}

		// Get bounding box:
		double xmax = view.getXmax();
		double xmin = view.getXmin();
		double ymax = view.getYmax();
		double ymin = view.getYmin();

		width = view.getWidth();
		height = view.getHeight();

		// Create the Tarski command and get the result:
		String def = ((GeoFunctionNVar) function).getCASString(StringTemplate.tarskiTemplate, true);
		String command = "(plot2d [ " + def + "] \"" + height + " " + width + " "
				+ xmin + " " + xmax + " "
				+ ymin + " " + " " + ymax + " -\" '(ord (x y))) ";
		String result = function.getApp().tarski.eval(command);
		// Tarski returns the SVG as a widthxheight image, but the coordinates are
		// scaled to fit in a box [0,0]-[1000,1000].

		// Conversion of the SVG result is a bit hacky.
		// 1. First we trim the first 25 lines that end in "-->":
		String plotdata = result.substring(result.indexOf("-->") + 3);
		// 2. We split the data into rows:
		String[] plotlines = plotdata.split("\n");
		// 3. We are interested in rows like:
		// <polyline id="c_9_2" class="c10F" points="862.568115235 711.962890625...
		// <circle cx="137.431640625" cy="288.037109375" r="2.50000000000"  class="c00F"/>
		lines = 0;
		circles = 0;
		for (String l : plotlines) {
			if (l.startsWith("<polyline")) {
				lines++;
			}
			if (l.startsWith("<circle")) {
				circles++;
			}
		}
		// 4. We (re)create the data arrays that will be used on plotting the data:
		circlevalues = new double[circles][3];
		pointvalues = new ArrayList<Double>();
		int line = 0;
		int circle = 0;

		for (String l : plotlines) {
			if (l.startsWith("<polyline") || l.startsWith("<circle")) {
				String c = "";
				// 5. Get the line style class for both types of graphics:
				int cindex = l.indexOf("class");
				c = l.substring(cindex + 7, cindex + 11);
				int style;

				char L = c.charAt(3);
				switch (L) {
					case 'F':
						style = 100;
						break;
					case 'U':
						style = 200;
						break;
					case 'T':
						style = 300;
						break;
					default:
						style = -1;
						break;
				}
				style += Integer.parseInt(c.substring(1,3));

				if (l.startsWith("<polyline")) {
					// 6. Read off the points:
					int pointsindex = l.indexOf("points");
					String points = l.substring(pointsindex + 8);
					points = points.substring(0, points.length() - 3); // trim the last 3 chars
					String[] coords = points.split(" ");
					pointvalues.add((double) style);
					int size = coords.length;
					pointvalues.add((double) size);
					for (String coord : coords) {
						pointvalues.add(Double.parseDouble(coord));
					}
				}
				if (l.startsWith("<circle")) {
					circlevalues[circle][0] = style;
					int cxindex = l.indexOf("cx=");
					int cyindex = l.indexOf("cy=");
					c = l.substring(cxindex + 4, cyindex - 2);
					double cx = Double.parseDouble(c);
					circlevalues[circle][1] = cx / 1000 * width; // scaling back
					int rindex = l.indexOf("r=");
					c = l.substring(cyindex + 4, rindex - 2);
					double cy = Double.parseDouble(c);
					circlevalues[circle][2] = height - (cy / 1000 * height); // scaling back
					circle ++;
				}
			}
		}
		Log.debug(def);
	}

	@Override
	public void draw(GGraphics2D g2) {
		if (!isVisible) {
			return;
		}
		g2.setColor(geo.getObjectColor());

		// Process polylines:
		int i = 0;
		for (int line = 0; line < lines; line++) {
			Double style = pointvalues.get(i);
			i++;
			Double entries = pointvalues.get(i);
			i++;
			int N = entries.intValue();
			N = N / 2; // two coordinates per point
			double x, y;
			x = pointvalues.get(i) / 1000 * width; // scaling back
			i++;
			y = height - pointvalues.get(i) / 1000 * height; // scaling back
			i++;

			boolean area = false;
			area = (style >= 300);

			GeneralPathClipped gp = new GeneralPathClipped(view);
			if (area) {
				gp.resetWithThickness(geo.getLineThickness());
				gp.moveTo(x, y);
			}
			for (int j = 0; j < N - 1; j++) {
				double x1 = pointvalues.get(i) / 1000 * width; // scaling back
				i++;
				double y1 = height - pointvalues.get(i) / 1000 * height; // scaling back
				i++;
				if (area) {
					gp.lineTo(x1, y1);
				} else {
					g2.drawLine((int) x, (int) y, (int) x1, (int) y1);
				}
				x = x1;
				y = y1;
			}
			if (area) {
				gp.closePath();
				g2.setStroke(objStroke);
				g2.setPaint(geo.getObjectColor());
				g2.setColor(geo.getObjectColor());
				g2.fill(gp);
			}
		}

		// Process small circles:
		for (int circle = 0; circle < circles; circle++) {
			int style = (int) circlevalues[circle][0];
			g2.setColor(mycolor(geo, style));
			drawCircle(g2, circlevalues[circle][1], circlevalues[circle][2], 1);
		}
	}

	private GColor mycolor(GeoElement geo, int style) {
		GColor c = geo.getObjectColor();
		switch (style) {
		case 300:
		case 301:
		case 310:
		case 311:
			c = GColor.BLUE;
			break;
		case 200:
		case 201:
		case 210:
		case 211:
			c = GColor.GEOGEBRA_GRAY;
			break;
		case 100:
		case 101:
		case 110:
		case 111:
			c = GColor.YELLOW;
			break;
		}
		return c;
	}

	private void drawCircle(GGraphics2D g2, double x, double y, double r) {
		GeneralPathClipped gp = new GeneralPathClipped(view);
		gp.resetWithThickness(geo.getLineThickness());
		gp.moveTo(x-r, y-r);
		gp.lineTo(x+r,y-r);
		gp.lineTo(x+r, y+r);
		gp.lineTo(x-r,y+r);
		gp.closePath();
		g2.fill(gp);
		// FIXME: draw a small circle, not a square
	}

	@Override
	public boolean hit(int x, int y, int hitThreshold) {
		return false;
	}

	@Override
	public boolean isInside(GRectangle rect) {
		return false;
	}
}
