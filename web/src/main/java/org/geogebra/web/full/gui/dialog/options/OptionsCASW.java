package org.geogebra.web.full.gui.dialog.options;

import org.geogebra.common.main.App;
import org.geogebra.common.main.Localization;
import org.geogebra.common.main.ProverSettings;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.gui.util.FormLabel;
import org.geogebra.web.html5.gui.util.LayoutUtilW;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.html5.util.tabpanel.MultiRowsTabPanel;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Settings for CAS in HTML5
 *
 */
public class OptionsCASW implements OptionPanelW, ClickHandler {

	private AppW app;
	private FlowPanel optionsPanel;
	private CheckBox showRoots;
	private CheckBox showNavigation;
	private Label lblProver;
	private FormLabel lblEngine;
	private ListBox engineOption;

	/**
	 * @param app
	 *            app
	 */
	public OptionsCASW(AppW app) {
		this.app = app;
		createGUI();
    }

	private void createGUI() {
		showRoots = new CheckBox();
		showRoots.addClickHandler(this);
		showRoots.setStyleName("checkBoxPanel");

		showNavigation = new CheckBox();
		showNavigation.addClickHandler(this);
		showNavigation.setStyleName("checkBoxPanel");

		lblProver = new Label();
		lblProver.addStyleName("panelTitle");

		engineOption = new ListBox();
		lblEngine = new FormLabel(
				getApp().getLocalization().getMenu("Engine") + ":")
				.setFor(engineOption);

		optionsPanel = new FlowPanel();
		optionsPanel.addStyleName("propertiesPanel");
		optionsPanel.addStyleName("simplePropertiesPanel");

		// optionsPanel.add(cbShowFormulaBar);
		optionsPanel.add(showRoots);
		optionsPanel.add(showNavigation);

		optionsPanel.add(lblProver);

		optionsPanel.add(LayoutUtilW.panelRowIndent(lblEngine, engineOption));
		engineOption.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				int option = engineOption.getSelectedIndex();
				// Log.debug("selected = " + option);
				ProverSettings proverSettings = ProverSettings.get();
				switch (option) {
				case 0:
					proverSettings.proverEngine = "AUTO";
					break;
				case 1:
					proverSettings.proverEngine = "Botana";
					break;
				case 2:
					proverSettings.proverEngine = "Recio";
					break;
				case 3:
					proverSettings.proverEngine = "CNI";
					break;
				default:
					proverSettings.proverEngine = "AUTO";
					break;
				}
			}
		});

		// spacer
		// layoutOptions.add(Box.createVerticalStrut(16));

		setLabels();
		updateGUI();

	}

	/**
	 * Update the language
	 */
	public void setLabels() {
		Localization loc = app.getLocalization();
		showRoots.setText(loc.getMenu("CASShowRationalExponentsAsRoots"));
		showNavigation.setText(loc.getMenu("NavigationBar"));
		lblProver.setText(getApp().getLocalization().getMenu("Prover"));
	}

	@Override
	public void updateGUI() {
		showRoots.setValue(app.getSettings().getCasSettings()
				.getShowExpAsRoots());
		showNavigation.setValue(app.showConsProtNavigation(App.VIEW_CAS));
		updateEngineOption();
    }

	public void updateEngineOption() {
		engineOption.clear();
		for (String o : getEngineOptions(getApp())) {
			engineOption.addItem(getApp().getLocalization().getMenu(o));
		}
		int index = 0;
		String engine = ProverSettings.get().proverEngine;

		if ("Botana".equalsIgnoreCase(engine)) {
			index = 1;
		} else if ("Recio".equalsIgnoreCase(engine)) {
			index = 2;
		} else if ("CNI".equalsIgnoreCase(engine)) {
			index = 3;
		} else if ("Auto".equalsIgnoreCase(engine)) {
			index = 0;
		}

		engineOption.setSelectedIndex(index);
	}

	@Override
	public Widget getWrappedPanel() {
		return optionsPanel;
    }

	@Override
    public void onResize(int height, int width) {
	    // TODO Auto-generated method stub
	    
    }

	@Override
	public void onClick(ClickEvent event) {
		actionPerformed(event.getSource());

	}

	private void actionPerformed(Object source) {
		if (source == showRoots) {
			app.getSettings().getCasSettings()
					.setShowExpAsRoots(showRoots.getValue());
		}

		else if (source == showNavigation) {
			app.toggleShowConstructionProtocolNavigation(App.VIEW_CAS);
		}

		updateGUI();

	}

	@Override
	public MultiRowsTabPanel getTabPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return application
	 */
	public AppW getApp() {
		return app;
	}

	/**
	 * @param app
	 *            application
	 * @return available engine options
	 */
	public static String[] getEngineOptions(App app) {
		Localization loc = app.getLocalization();
		return new String[] { loc.getMenu("Auto"),
				loc.getMenu("Gröbner basis method (Botana)"), loc.getMenu("Exact checks (Recio)"),
				loc.getMenu("Complex number identity")};
	}

}
