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
import org.geogebra.common.main.App;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.desktop.euclidian.EuclidianViewD;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
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
					String input = g.getDefinitionForInputBar();
					output += ">> " + input + "\n";
					ExpressionNode en = g.getDefinition();

					String enS = "", geS = "", gccS = "", out = "";

					if (en != null) {
						enS = g.getDefinition().toOutputValueString(StringTemplate.defaultTemplate);
						out = enS;
					}
					if ((out.equals("") || input.endsWith(out)) && g instanceof GeoElement) {
						geS = ((GeoElement) g).getAlgebraDescriptionDefault();
						out = geS;
					}
					if ((out.equals("") || input.endsWith(out)) && g instanceof org.geogebra.common.kernel.geos.GeoCasCell) {
						gccS = ((GeoCasCell) g).getAlgebraDescriptionDefault();
						out = gccS;
					}
					if (out.equals("")) {
						// Maybe we want to obtain the content of a CAS cell that has already been retrieved.
						// In such cases, for some strange reason, there is no direct way to get its content
						// as done above. Here is a workaround:
						if (g.getParentAlgorithm() instanceof org.geogebra.common.kernel.cas.AlgoDependentCasCell) {
							String numberS = ((org.geogebra.common.kernel.cas.AlgoDependentCasCell) (g.getParentAlgorithm())).
									getCasCell().toString(StringTemplate.defaultTemplate);
							if (numberS.startsWith("$")) {
								int number = Integer.parseInt(numberS.substring(1)) - 1;
								out = g.getKernel().getConstruction().getCasCell(number).
										getOutput(StringTemplate.defaultTemplate);
							}
						}
					}

					output += "<< " + out + "\n";
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

	private static boolean isRealTTY() {
		try {
			return System.getenv("TERM") != null
					&& System.console() != null;
		} catch (Exception e) {
			return false;
		}
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
			String line;
			LineReader reader;

			Terminal terminal;

			try {
				if (isRealTTY()) {
					terminal = TerminalBuilder.builder()
							.system(true)
							.build();
					reader = LineReaderBuilder.builder()
							.terminal(terminal)
							.variable(LineReader.HISTORY_FILE, System.getProperty("user.home") + "/.geogebra_history")
							.option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
							.option(LineReader.Option.HISTORY_REDUCE_BLANKS, true)
							.build();
				} else {
					terminal = TerminalBuilder.builder()
							.system(false)
							.dumb(true)
							.streams(System.in, System.out)
							.build();
					reader = LineReaderBuilder.builder()
							.terminal(terminal)
							.build();
				}
			} catch (Exception ex) {
				System.out.println("Error on starting terminal.");
				return;
			}

			while (true) {
				try {
					line = reader.readLine("> ");
					if (!line.equals("")) // attempt to fix Windows bug
						process(kernel, line);
				} catch (UserInterruptException | EndOfFileException e) {
					System.out.println("Console session ended, exiting...");
					AppD.exit(0); // This is needed to properly quit Tarski.
				}
			}
		});

		inputThread.setDaemon(true);
		inputThread.start();
		}
	}
