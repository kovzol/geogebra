package org.geogebra.web.html5.util;

import org.geogebra.common.util.Tarski;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.util.debug.LoggerW;
import org.geogebra.web.resources.JavaScriptInjector;
import org.geogebra.web.html5.css.TarskiResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;

/**
 * @author Zoltan Kovacs
 *
 *         Implements web dependent parts of Tarski.
 */
public class TarskiW extends Tarski {

	@Override
	public String eval(String command) {
		String response = "";
		response = tarskiEval(command);
		return response;
	}

	public static native String tarskiEval(String command) /*-{
	    var output = $wnd.TARSKIEVAL(command);
	    console.log("tarski < " + command);
	    console.log("tarski > " + output);
	    return output;
	}-*/;

	@Override
	public boolean init(int timeout) {
		GWT.runAsync(new RunAsyncCallback() {
			@Override
			public void onSuccess() {
				JavaScriptInjector.inject(TarskiResources.INSTANCE.tarskiJs());

				String tarskiLoader = "Tarski().then(function(Module) {\n"
						+ "var numcells = 50000000;\n"
						+ "var timeout = " + timeout + "\n"
						+ "TARSKIINIT = Module.cwrap(\"TARSKIINIT\", 'void', ['number', 'number']);\n"
						+ "TARSKIEVAL = Module.cwrap(\"TARSKIEVAL\", \"string\", [\"string\"]);\n"
						+ "TARSKIEND = Module.cwrap(\"TARSKIEND\", \"void\", []);\n"
						+ "TARSKIINIT(numcells, timeout);\n"
						+ "});";
				JavaScriptInjector.inject("tarski-loader", tarskiLoader);

				LoggerW.loaded("Tarski webAssembly injected");
			}

			@Override
			public void onFailure(Throwable reason) {
				Log.debug("Loading failure");
			}
		});

		return true;
	}

	@Override
	public boolean reinit(int timeout) {
		GWT.runAsync(new RunAsyncCallback() {
			@Override
			public void onSuccess() {
				String tarskiLoader = "var numcells = 50000000;\n"
						+ "var timeout = " + timeout + "\n"
						+ "TARSKIEND();\n"
						+ "TARSKIINIT(numcells, timeout);\n";
				JavaScriptInjector.inject("tarski-reloader", tarskiLoader);

				LoggerW.loaded("Tarski restarted");
			}

			@Override
			public void onFailure(Throwable reason) {
				Log.debug("Restarting failure");
			}
		});

		return true;
	}

	@Override
	public boolean end() {
		return true; // it seems no need to exit via TARSKIEND()
	}

}