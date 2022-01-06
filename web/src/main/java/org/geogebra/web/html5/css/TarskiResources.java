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
	 * Tarski must be compiled as WebAssembly to get tarski.js.
	 */
	@Source("org/geogebra/web/resources/js/tarski/tarski.js")
	TextResource tarskiJs();
}
