package org.geogebra.common.euclidian.draw;

import java.util.ArrayList;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.EuclidianStatic;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.GeneralPathClipped;
import org.geogebra.common.factories.UtilFactory;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunctionNVar;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.plugin.EuclidianStyleConstants;
import org.geogebra.common.util.debug.Log;

/**
 * Graphical representation of inequality (external).
 * It uses Tarski to compute an SVG image of the solution of the inequality system
 * and then it processes the image and draws it directly in the EuclideanView.
 *
 * @author Zoltan Kovacs (zoltan@geogebra.org)
 * @thanks to Laszlo Gal (laszlo@geogebra.org) for several useful hints
 */

public class DrawInequalityExternal extends Drawable {

	private int XLABEL_DEFAULT = 10;
	private int YLABEL_DEFAULT = 10;
	private int XLABEL_OFFSET_DEFAULT = -20;
	private int YLABEL_OFFSET_DEFAULT = -20;

	double EPSILON = 0.00001;

	int CIRCLE_RADIUS = 4;

	private boolean isVisible;
	private GeoElementND function;
	private ArrayList<Double> pointvalues;
	private double[][] circlevalues;
	private int lines, circles, height, width;
	private boolean removeCAD = true; // TODO: Add an option to set this to false
	// and eventually change the order x/y

	private int startTime;

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

		double aspectratio_math = (ymax - ymin) / (xmax - xmin);

		width = view.getWidth();
		height = view.getHeight();

		double aspectratio_screen = ((double) height) / width;
		double stretch_factor = aspectratio_screen / aspectratio_math;
		Log.debug("aspectratio math=" + aspectratio_math + " screen=" + aspectratio_screen
			+ " stretch=" + stretch_factor);

		/* Create the Tarski command and get the result... */
		String def = "";
		AlgoElement ap = ((GeoFunctionNVar) function).getParentAlgorithm();
		if (ap != null && ap.getOutput().length > 0) {
			// This is a Plot2D command...
			def = ((GeoFunctionNVar) function).getParentAlgorithm().getOutput(0).getCASString(StringTemplate.tarskiTemplate, false);
		} else {
			// This is a direct inequality input...
			def = ((GeoFunctionNVar) function).getCASString(StringTemplate.tarskiTemplate, false);
		}

		String extraDef = "";

		// Add a half-plane to force two variables:
		if (!def.contains("x")) {
			extraDef += " /\\ (x < " + ((int) xmax+1) + ")";
		}
		if (!def.contains("y")) {
			extraDef += " /\\ (y < " + ((int) ymax+1) + ")";
		}
		// Otherwise tarski/plot2d complains about getting the input in just one variable.

		String removeCADlines = "";
		if (removeCAD) {
			removeCADlines = "'(sset true) ";
		}
		String command = "(plot2d [ " + def + extraDef + "] \"" + (int) (height / stretch_factor) + " " + width + " "
				+ xmin + " " + xmax + " "
				+ ymin + " " + " " + ymax + " -\" " + removeCADlines + "'(ord (x y))) ";
		Log.debug(command);
		startTime = (int) (UtilFactory.getPrototype().getMillisecondTime());
		String result = function.getApp().tarski.evalCached(command);
		debugElapsedTime();

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
		if (!result.startsWith("\"<!DOCTYPE svg")) {
			// There is an error during processing. Avoid further processing of the faulty SVG:
			Log.debug("Tarski could not compute the input");
			return;
		}

		int line = 0;
		int circle = 0;
		int position = 3;
		if (removeCAD) {
			position = 2;
		}

		for (String l : plotlines) {
			if (l.startsWith("<polyline") || l.startsWith("<circle")) {
				String c = "";
				// 5. Get the line style class for both types of graphics:
				int cindex = l.indexOf("class");
				c = l.substring(cindex + 7, cindex + 11);
				int style;

				char L = c.charAt(position);
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
				style += Integer.parseInt(c.substring(1,position));

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
					// 6. Read off the point:
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
		// Log.debug(def);
	}

	@Override
	public void draw(GGraphics2D g2) {

		if (!isVisible) {
			return;
		}

		// We are searching for a good position for the label:
		int xl = -100; // This should be invisible.
		int yl = -100; // This should be invisible.
		// At the beginning we put it on the top-left. If there is no better candidate,
		// we use this position.

		g2.setColor(geo.getSelColor());

		// Process polylines:
		int i = 0;
		for (int line = 0; line < lines; line++) {
			Double style = pointvalues.get(i);
			i++;
			Double entries = pointvalues.get(i);
			i++;
			int N = entries.intValue();
			N = N / 2; // two coordinates per point

			boolean shown = style >= 300;
			boolean removed = (style == 110 || style == 101);

			// Log.debug(style);

			if (shown || removed) {
				boolean area = false;
				if (removeCAD) {
					area = style == 300;
				} else {
					area = style == 311;
				}


				double x, y, sx, sy;
				x = pointvalues.get(i) / 1000 * width; // scaling back
				i++;
				y = height - pointvalues.get(i) / 1000 * height; // scaling back
				i++;
				sx = x; sy = y;

				GeneralPathClipped gp = new GeneralPathClipped(view);
				gp.resetWithThickness(geo.getLineThickness());
				gp.moveTo(x, y);

				for (int j = 0; j < N - 1; j++) {
					double x1 = pointvalues.get(i) / 1000 * width; // scaling back
					i++;
					double y1 = height - pointvalues.get(i) / 1000 * height; // scaling back
					i++;
					if (area) {
						gp.lineTo(x1, y1);
					} else {
						if (j < N - 2 || (Math.abs(sx-x1)>EPSILON || Math.abs(sy-y1)>EPSILON)) { // don't draw the last point
							// if it is the same as the first point (it confuses dashed line drawing)
							gp.lineTo(x1, y1);
						}
					}
					x = x1;
					y = y1;

					// This may be a candidate for the label position:
					if (xl <= XLABEL_DEFAULT && x > XLABEL_DEFAULT - XLABEL_OFFSET_DEFAULT
							&& y > YLABEL_DEFAULT - YLABEL_OFFSET_DEFAULT && y < height) {
						xl = (int) x + XLABEL_OFFSET_DEFAULT;
						yl = (int) y + YLABEL_OFFSET_DEFAULT;
						// Log.debug("label " + xl + " " + yl);
					}
				}
				if (area) {
					gp.closePath();
					g2.setPaint(geo.getSelColor());
					g2.fill(gp);
				} else {
					g2.setPaint(geo.getObjectColor());
					if (removed) {
						g2.setStroke(EuclidianStatic.getStroke(3,
								EuclidianStyleConstants.LINE_TYPE_DASHED_SHORT));
					} else {
						g2.setStroke(EuclidianStatic.getStroke(3,
								EuclidianStyleConstants.LINE_TYPE_FULL));
					}
					g2.draw(gp);
					// If highlighted, draw it another time, but with a different linetype:
					if (isHighlighted()) {
						g2.setPaint(geo.getSelColor());
						g2.setStroke(selStroke);
						updateStrokes(geo);
						g2.draw(gp);
					}
				}
			} else {
				i += N * 2; // skip the unnecessary components
			}
		}

		// Process small circles:
		for (int circle = 0; circle < circles; circle++) {
			int style = (int) circlevalues[circle][0];
			boolean area = style >= 300;
			double x = circlevalues[circle][1];
			double y = circlevalues[circle][2];
			if (area) {
				drawCircle(geo, g2, x, y, CIRCLE_RADIUS, true);
			} else {
				drawCircle(geo, g2, x, y, CIRCLE_RADIUS, true);
				drawCircle(geo, g2, x, y, CIRCLE_RADIUS - 1, false);
			}

			// This may be a candidate for the label position:
			if (xl <= XLABEL_DEFAULT && x > XLABEL_DEFAULT - XLABEL_OFFSET_DEFAULT
					&& y > YLABEL_DEFAULT - YLABEL_OFFSET_DEFAULT && y < height) {
				xl = (int) x + XLABEL_OFFSET_DEFAULT;
				yl = (int) y + YLABEL_OFFSET_DEFAULT;
			}
		}

		// Draw label:
		if (geo.isLabelVisible()) {
			labelDesc = geo.getLabelDescription();
			xLabel = xl;
			yLabel = yl;
			g2.setPaint(geo.getLabelColor());
			g2.setFont(view.getFontPoint());
			drawLabel(g2);
		}

	}

	// This is unused and should be removed:
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

	private void drawCircle(GeoElement geo, GGraphics2D g2, double x, double y, double r, boolean filled) {
		g2.setColor(geo.getObjectColor());
		GeneralPathClipped gp = new GeneralPathClipped(view);
		gp.resetWithThickness(geo.getLineThickness());
		gp.moveTo(x-r, y-r);
		gp.lineTo(x+r,y-r);
		gp.lineTo(x+r, y+r);
		gp.lineTo(x-r,y+r);
		gp.closePath();
		if (filled) {
			g2.setPaint(geo.getObjectColor());
		} else {
			g2.setPaint(view.getBackgroundCommon());
		}
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

	private void debugElapsedTime() {
		int elapsedTime = (int) (UtilFactory.getPrototype().getMillisecondTime()
				- startTime);

		Log.debug("Benchmarking: " + elapsedTime + " ms");
	}

}
