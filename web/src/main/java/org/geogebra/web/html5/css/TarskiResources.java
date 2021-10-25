package org.geogebra.web.html5.css;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

/**
 * CAS resource bundle
 */
public interface TarskiResources extends ClientBundle {

	/**
	 * maybe it's better if INSTANCE is created later?
	 */
	TarskiResources INSTANCE = GWT.create(TarskiResources.class);

	/*
	 * Tarski must be compiled as WebAssembly to get tarski.js,
	 * by changing the target from tarski.html to tarski.js
	 * and adding the following settings to LDFLAGS:
	 * -s SINGLE_FILE=1 -s MODULARIZE=1 -s EXPORT_NAME=Tarski
	 */
	@Source("org/geogebra/web/resources/js/tarski/tarski.js")
	TextResource tarskiJs();
	@Source("org/geogebra/web/resources/js/tarski/tarski-loader.js")
	TextResource tarskiLoaderJs();
}
