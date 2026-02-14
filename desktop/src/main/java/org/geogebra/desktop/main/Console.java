package org.geogebra.desktop.main;

import java.io.IOException;

import org.geogebra.common.kernel.Kernel;
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
 *
 * Currently, the Input Bar is emulated (that is, inputs will be directly sent to Algebra View).
 * Error handling is not implemented yet.
 *
 * You may need to compile GeoGebra by using "./gradlew :desktop:installDist" and
 * run GeoGebra from command line by "desktop/build/install/desktop/bin/desktop --console --silent"
 * in order to avoid falling back to dumb terminal mode.
 */
public class Console {
	public static void start(Kernel kernel) throws IOException {

		LineReader reader = LineReaderBuilder.builder().build();

		Thread inputThread = new Thread(() -> {
			while (true) {
				String line;
				try {
					line = reader.readLine("> ");
					kernel.getAlgebraProcessor().processAlgebraCommand(line, true);
				} catch (UserInterruptException | EndOfFileException e) {
					break;
				}
			}
		});

		inputThread.setDaemon(true);
		inputThread.start();
		}
	}
