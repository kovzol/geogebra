package org.geogebra.desktop.main;

import java.io.IOException;
import java.io.PrintStream;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.ExpressionNode;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.util.AsyncOperation;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * A command line interface to perform GeoGebra commands.
 * @author Zoltan Kovacs
 *
 * Start GeoGebra with --console (and, optionally, with --silent to mute debug messages)
 * and enter GeoGebra commands in the terminal (in the desktop version).
 * To mute all messages from Giac as well, redirect stderr to null.
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
 */
public class Console {
	public static boolean dumb;
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

	public static void start(Kernel kernel) throws IOException {
	Terminal terminal = TerminalBuilder.builder()
				.system(true).build();
		if (terminal.getType().equals("dumb")) {
			dumb = true;
		}
		LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

		Thread inputThread = new Thread(() -> {
			if (dumb) {
				line = "$1:=Eliminate({x^2+y,y},{x})";
				line = "aa";
				line = "Line((1,1),(2,2))";
				System.err.println("Dumb terminal mode, testing input " + line);
				process(kernel, line);
				System.err.println("End of dumb terminal mode");
				return;
			}

			while (true) {
				String line;
				try {
					line = reader.readLine("> ");
					process(kernel, line);
				} catch (UserInterruptException | EndOfFileException e) {
					System.out.println("Console session ended, exiting...");
					System.exit(0);
				}
			}
		});

		inputThread.setDaemon(true);
		inputThread.start();
		}
	}
