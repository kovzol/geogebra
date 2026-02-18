package org.geogebra.desktop.main;

import static org.geogebra.desktop.export.GraphicExportDialog.exportEPS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.geos.GeoCasCell;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.desktop.euclidian.EuclidianViewD;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;

/**
 * A command line interface to perform GeoGebra commands.
 * @author Zoltan Kovacs
 *
 * Start GeoGebra with --console (and, optionally, with --silent to mute debug messages)
 * and enter GeoGebra commands in the terminal (in the desktop version).
 * To mute all messages from Giac as well, redirect stderr to null.
 * TODO: Mute Giac completely (by modifying its source code), and do the same for Tarski, too.
 *
 * Example on Linux:
 *
 * $ ./gradlew :desktop:installDist && desktop/build/install/desktop/bin/desktop --console --silent 2>/dev/null
 *
 * GeoGebra Discovery 2026Feb12
 * GeoGebra 5.0.641.0 04 May 2021 Java 11.0.30-64bit
 *
 * > (1,1)
 * >> A = (1, 1)
 * << (1, 1)
 * > B=(2,3)
 * >> B = (2, 3)
 * << (2, 3)
 * > Line(A,B)
 * >> f: Line(A, B)
 * << f: -2x + y = -1
 * > $1:=Eliminate({x^2+y,y},{x})
 * >> $1 = Eliminate({x² + y, y},{x})
 * << {-y}
 * > abc
 * Parse error
 *
 * Currently, the Input Bar is emulated (that is, inputs will be directly sent to Algebra View).
 * When falling back to dumb terminal mode, only one command line is evaluated.
 *
 * When no real terminal is used, no line editing is allowed. When the argument --texmacs is
 * added, the TeXmacs plugin protocol jumps in by sending minimal communication to set up
 * receiving input and sending output as verbatim code.
 *
 * To create a minimal TeXmacs plugin, install GeoGebra Discovery via snap to have the executable
 * geogebra-discovery and create $HOME/.TeXmacs/plugins/geogebra-discovery/progs/init-geogebra-discovery.scm
 * with the following content:
 *
 * (plugin-configure geogebra-discovery
 *   (:require (url-exists-in-path? "geogebra-discovery"))
 *   (:launch "geogebra-discovery --texmacs --silent")
 *   (:session "GeoGebra Discovery"))
 *
 * A screenshot can be taken by entering a period ("."). It is shown with a fixed width (800).
 */
public class Console {
	public static String line;
	public static PrintStream originalOut = System.out;
	public static PrintStream originalErr = System.err;

	static final AsyncOperation<GeoElementND[]> callback = new AsyncOperation<GeoElementND[]>() {
		public void callback(GeoElementND[] newGeos) {
			System.setOut(originalOut);
			System.setErr(originalErr);
			String output = "";
			if (newGeos == null) {
				System.out.println("Parse error");
				return;
			} else {
				for (GeoElementND g : newGeos) {
					output += ">> " + g.getDefinitionForInputBar() + "\n";
					ExpressionNode en = g.getDefinition();
					if (en != null) {
						output += "<< " + g.getDefinition().toOutputValueString(StringTemplate.defaultTemplate) + "\n";
					} else if (g instanceof GeoElement) {
						output += "<< " + ((GeoElement) g).getAlgebraDescriptionDefault() + "\n";
					} else if (g instanceof org.geogebra.common.kernel.geos.GeoCasCell) {
						output += "<< " + ((GeoCasCell) g).getAlgebraDescriptionDefault() + "\n";
					}
				}
			}
			System.out.print(output);
		}
	};

	static private void process(Kernel kernel, String input) {
		System.setOut(DevNull.NULL_PRINT_STREAM);
		System.setErr(DevNull.NULL_PRINT_STREAM);
		kernel.getAlgebraProcessor().processAlgebraCommandConsole(input, callback);
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	public static void start(Kernel kernel, boolean texmacs) throws IOException {

		boolean interactive = System.console() != null; // most probably TeXmacs is communicating...

		Thread inputThread = new Thread(() -> {
			if (!interactive) {
				BufferedReader br =	new BufferedReader(new InputStreamReader(System.in));
				try {
					if (texmacs) { // but listening to TeXmacs must be forced this way
						System.out.print("\002channel:prompt\005> \005"); // set prompt to "> "
						// see the mycas example plugin in TeXmacs for more details
					}
					while ((line = br.readLine()) != null) {
						if (line.trim().equals(".")) { // create a screenshot
							if (texmacs) {
								StringBuilder sb = new StringBuilder();
								double w = ((AppD) kernel.getApplication()).getActiveEuclidianView().getViewWidth();
								double h = ((AppD) kernel.getApplication()).getActiveEuclidianView().getViewHeight();
								double exportScale = w/h;
								double ew = 800; // fix size for the width
								double eh = ew/exportScale;
								exportEPS((AppD) kernel.getApplication(),
										(EuclidianViewD) kernel.getApplication()
												.getActiveEuclidianView(),
										sb, true, (int) ew,
										(int) eh, exportScale);
								System.out.print("\002ps:" + sb.toString() + "\005");
							}
							continue;
						}
						if (texmacs) {
							System.out.print("\002verbatim:");
						}
						process(kernel, line);
						if (texmacs) {
							System.out.print("\005");
							System.out.flush();
							System.out.print("\002channel:prompt\005");
						}
					}
					return;
				} catch (Exception e) {
					return;
				}
			}

			// Interactive operation mode:
			while (true) {
				String line;
				LineReader reader = LineReaderBuilder.builder().build();
				try {
					line = reader.readLine("> ");
					process(kernel, line);
				} catch (UserInterruptException | EndOfFileException e) {
					System.out.println("Console session ended, exiting...");
					System.exit(0); // exit GeoGebra as well
				}
			}
		});

		inputThread.setDaemon(true);
		inputThread.start();
		}
	}
