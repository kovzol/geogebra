package org.geogebra.desktop.gui.dialog.options;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.geogebra.common.gui.SetLabels;
import org.geogebra.common.gui.dialog.options.OptionsCAS;
import org.geogebra.common.main.App;
import org.geogebra.common.main.Localization;
import org.geogebra.common.main.ProverSettings;
import org.geogebra.common.main.settings.CASSettings;
import org.geogebra.desktop.gui.util.LayoutUtil;
import org.geogebra.desktop.main.AppD;

/**
 * Options for the CAS view.
 */
public class OptionsCASD implements OptionPanelD, ActionListener, SetLabels {
	/**
	 * Application object.
	 */
	private AppD app;

	private CASSettings casSettings;

	/** */
	private JLabel timeoutLabel;

	/** */
	private JComboBox cbTimeout;

	/** show rational exponents as roots */
	private JCheckBox cbShowRoots;
	private JCheckBox cbShowNavigation;
	private JComboBox<String> cbEngine;

	private JPanel proverPanel;
	private JLabel engineLabel;

	private JPanel wrappedPanel;

	/**
	 * Construct CAS option panel.
	 * 
	 * @param app
	 */
	public OptionsCASD(AppD app) {
		wrappedPanel = new JPanel();
		wrappedPanel.setLayout(new BoxLayout(wrappedPanel, BoxLayout.PAGE_AXIS));

		this.app = app;
		casSettings = app.getSettings().getCasSettings();

		initGUI();
		updateGUI();
		setLabels();
	}

	/**
	 * Initialize the user interface.
	 * 
	 * @remark updateGUI() will be called directly after this method
	 * @remark Do not use translations here, the option dialog will take care of
	 *         calling setLabels()
	 */
	private void initGUI() {
		JPanel panel1 = new JPanel();
		cbTimeout = new JComboBox(OptionsCAS.getTimeoutOptions());
		cbTimeout.addActionListener(this);
		timeoutLabel = new JLabel();
		timeoutLabel.setLabelFor(cbTimeout);
		panel1.add(timeoutLabel);
		panel1.add(cbTimeout);

		JPanel panel2 = new JPanel();
		cbShowRoots = new JCheckBox();
		cbShowRoots.addActionListener(this);
		cbShowRoots.setSelected(casSettings.getShowExpAsRoots());
		panel2.add(cbShowRoots);

		JPanel panel3 = new JPanel();
		cbShowNavigation = new JCheckBox();
		cbShowNavigation.addActionListener(this);
		cbShowNavigation.setSelected(casSettings.getShowExpAsRoots());
		panel3.add(cbShowNavigation);

		proverPanel = new JPanel();
		engineLabel = new JLabel();
		cbEngine = new JComboBox<>(getEngineOptions(app));
		cbEngine.addActionListener(this);
		engineLabel.setLabelFor(cbEngine);
		proverPanel.add(engineLabel);
		proverPanel.add(cbEngine);

		wrappedPanel.add(panel1);
		wrappedPanel.add(panel2);
		wrappedPanel.add(panel3);
		wrappedPanel.add(proverPanel);

	}

	/**
	 * Update the user interface, ie change selected values.
	 * 
	 * @remark Do not call setLabels() here
	 */
	@Override
	public void updateGUI() {
		casSettings = app.getSettings().getCasSettings();
		cbTimeout.setSelectedItem(OptionsCAS
				.getTimeoutOption(casSettings.getTimeoutMilliseconds() / 1000));
		cbShowRoots.setSelected(casSettings.getShowExpAsRoots());
		cbShowNavigation.setSelected(app.showConsProtNavigation(App.VIEW_CAS));
		updateEngineOption();
	}

	/**
	 * React to actions.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// change timeout
		if (e.getSource() == cbTimeout) {
			casSettings.setTimeoutMilliseconds(
					((Integer) cbTimeout.getSelectedItem()) * 1000);
		}
		if (e.getSource() == cbShowNavigation) {
			app.toggleShowConstructionProtocolNavigation(App.VIEW_CAS);
		}
		/** show rational exponents as roots */
		if (e.getSource() == cbShowRoots) {
			casSettings.setShowExpAsRoots(cbShowRoots.isSelected());
		}
		if (e.getSource() == cbEngine) {
			int option = cbEngine.getSelectedIndex();
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
	}

	/**
	 * Update the language of the user interface.
	 */
	@Override
	public void setLabels() {
		Localization loc = app.getLocalization();
		timeoutLabel.setText(loc.getMenu("CasTimeout"));
		cbShowRoots.setText(loc.getMenu("CASShowRationalExponentsAsRoots"));
		cbShowNavigation.setText(loc.getMenu("NavigationBar"));
		proverPanel.setBorder(LayoutUtil.titleBorder(loc.getMenu("Prover")));
		engineLabel.setText(loc.getMenu("Engine"));
	}

	/**
	 * Apply changes
	 */
	@Override
	public void applyModifications() {
		// all controls fire immediately
	}

	@Override
	public JPanel getWrappedPanel() {
		return this.wrappedPanel;
	}

	@Override
	public void revalidate() {
		getWrappedPanel().revalidate();

	}

	@Override
	public void setBorder(Border border) {
		wrappedPanel.setBorder(border);
	}

	@Override
	public void updateFont() {
		Font font = app.getPlainFont();

		timeoutLabel.setFont(font);
		cbShowRoots.setFont(font);
		cbTimeout.setFont(font);
		cbShowNavigation.setFont(font);
		cbEngine.setFont(font);
		proverPanel.setFont(font);
	}

	@Override
	public void setSelected(boolean flag) {
		// see OptionsEuclidianD for possible implementation
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

	public void updateEngineOption() {
		String engine = ProverSettings.get().proverEngine;
		int index = 0;
		if ("Botana".equalsIgnoreCase(engine)) {
			index = 1;
		} else if ("Recio".equalsIgnoreCase(engine)) {
			index = 2;
		} else if ("CNI".equalsIgnoreCase(engine)) {
			index = 3;
		} else if ("Auto".equalsIgnoreCase(engine)) {
			index = 0;
		}
		cbEngine.setSelectedIndex(index);
	}
}
