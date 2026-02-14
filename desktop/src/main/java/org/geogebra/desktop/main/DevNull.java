package org.geogebra.desktop.main;

import java.io.OutputStream;
import java.io.PrintStream;

public class DevNull {

	public static final PrintStream NULL_PRINT_STREAM =
			new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					// do nothing
				}
			});
}
