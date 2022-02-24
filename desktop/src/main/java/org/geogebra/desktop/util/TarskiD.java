package org.geogebra.desktop.util;

import org.geogebra.common.cas.CASparser;
import org.geogebra.common.jre.cas.giac.CASgiacJre;
import org.geogebra.common.main.App;
import org.geogebra.common.util.debug.Log;
import org.geogebra.desktop.cas.giac.MyClassPathLoader;
import org.geogebra.desktop.main.AppD;
import tarski.*;
import org.geogebra.common.util.Tarski;

/**
 * Implements desktop dependent parts of Tarski.
 *
 * @author Zoltan Kovacs
 *
 */
public class TarskiD extends Tarski {

	@Override
	public String eval(String command) {
		String response = "";
		response = tarski.TARSKIEVAL(command);
		return response;
	}

	@Override
	public boolean init(int timeout) {
		// System.loadLibrary("tarski");
		loadTarskiLibrary(); // a la GeoGebra
		tarski.TARSKIINIT(50000000, timeout);
		return true;
	}

	@Override
	public boolean reinit(int timeout) {
		tarski.TARSKIEND();
		tarski.TARSKIINIT(50000000, timeout);
		return true;
	}

    private boolean loadTarskiLibrary() {
		// This piece of code loads Tarski in a similar way Giac is loaded,
		// for transparency. We use the same file naming conventions.

		boolean tarskiLoaded = false;

		try {
			Log.debug("Loading Tarski dynamic library");

			String file;

			if (AppD.MAC_OS) {
				// Architecture on OSX seems to be x86_64, but let's make sure
				file = "javatarski";
			} else if ("AMD64".equals(System.getenv("PROCESSOR_ARCHITECTURE"))
					// System.getenv("PROCESSOR_ARCHITECTURE") can return null
					// (seems to
					// happen on linux)
					|| "amd64".equals(System.getProperty("os.arch"))) {
				file = "javatarski64";
			} else {
				file = "javatarski";
			}

			Log.debug("Loading Tarski dynamic library: " + file);

			// When running from local jars we can load the library files from
			// inside a jar like this
			MyClassPathLoader loader = new MyClassPathLoader();
			tarskiLoaded = loader.loadLibrary(file);

			if (!tarskiLoaded) {
				// "classic" method
				// for Webstart, eg loading
				// javatarski.dll from javatarski-win32.jar
				// javatarski64.dll from javatarski-win64.jar
				// libjavatarski.so from javatarski-linux32.jar
				// libjavatarski64.so from javatarski-linux64.jar
				// libjavatarski.jnilib from javatarski-mac.jar

				Log.debug("Trying to load Tarski library (alternative method)");
				System.loadLibrary(file);
				tarskiLoaded = true;

			}

		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}

		if (tarskiLoaded) {
			Log.debug("Tarski dynamic library loaded");
		} else {
			Log.debug("Failed to load Tarski dynamic library");
		}

		return tarskiLoaded;

	}

}
