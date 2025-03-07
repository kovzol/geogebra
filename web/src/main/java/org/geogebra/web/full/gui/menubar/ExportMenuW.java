package org.geogebra.web.full.gui.menubar;

import org.geogebra.common.export.CASExport;
import org.geogebra.common.geogebra3D.euclidian3D.printer3D.FormatSTL;
import org.geogebra.common.main.HTML5Export;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.common.util.FileExtensions;
import org.geogebra.common.util.StringUtil;
import org.geogebra.web.full.gui.dialog.ExportImageDialog;
import org.geogebra.web.html5.Browser;
import org.geogebra.web.html5.euclidian.EuclidianViewWInterface;
import org.geogebra.web.html5.gui.GPopupPanel;
import org.geogebra.web.html5.gui.util.AriaMenuBar;
import org.geogebra.web.html5.main.AppW;

import com.google.gwt.user.client.ui.Widget;

/**
 * @author bencze The "Export Image" menu, part of the "File" menu.
 */
public class ExportMenuW extends AriaMenuBar implements MenuBarI {

	/**
	 * Constructs the "Insert Image" menu
	 * 
	 * @param app
	 *            Application instance
	 */
	public ExportMenuW(AppW app) {
		super();

		addStyleName("GeoGebraMenuBar");
		MainMenu.addSubmenuArrow(this, app.isUnbundledOrWhiteboard());
		if (app.isUnbundled()) {
			addStyleName("floating-Popup");
		}
		initActions(this, app);
	}

	/**
	 * @param menu
	 *            menu
	 * @param app
	 *            application
	 */
	protected static void initActions(final MenuBarI menu, final AppW app) {

		menu.addItem(app.isWhiteboardActive()
				? menuText(app.getLocalization().getMenu("Download.SlidesGgs"))
				: menuText(app.getLocalization().getMenu("Download.GeoGebraFile")), true,
				new MenuCommand(app) {

					@Override
					public void doExecute() {
						menu.hide();
						app.getFileManager().export(app);
					}
				});

		menu.addItem(menuText(app.getLocalization().getMenu("Download.PNGImage")),
				true, new MenuCommand(app) {
					@Override
					public void execute() {
						menu.hide();
						app.toggleMenu();
						app.getSelectionManager().clearSelectedGeos();

						String url = ExportImageDialog.getExportDataURL(app);

						app.getFileManager().showExportAsPictureDialog(url,
								app.getExportTitle(), "png", "ExportAsPicture",
								app);
					}
				});

		menu.addItem(menuText(app.getLocalization().getMenu("Download.SVGImage")),
				true, new MenuCommand(app) {

					@Override
					public void execute() {
						menu.hide();
						app.toggleMenu();
						app.getSelectionManager().clearSelectedGeos();

						String svg = Browser
								.encodeSVG(((EuclidianViewWInterface) app
										.getActiveEuclidianView())
												.getExportSVG(1, false));

						app.getFileManager().showExportAsPictureDialog(svg,
								app.getExportTitle(), "svg", "ExportAsPicture",
								app);
					}
				});
		menu.addItem(menuText(app.getLocalization().getMenu("Download.PDFDocument")),
				true, new MenuCommand(app) {
					@Override
					public void execute() {

						menu.hide();
						app.toggleMenu();
						app.getSelectionManager().clearSelectedGeos();

						String pdf = app.getGgbApi().exportPDF(1, null, null);

						app.getFileManager().showExportAsPictureDialog(pdf,
								app.getExportTitle(), "pdf", "ExportAsPicture",
								app);
					}
				});
		// TODO add gif back when ready
		// if (!app.getLAF().isTablet()) {
		// addItem(menuText(
		// app.getLocalization().getMenu("AnimatedGIF")), true,
		// new MenuCommand(app) {
		// @Override
		// public void doExecute() {
		// hide();
		// dialogEvent("exportGIF");
		// ((DialogManagerW) app.getDialogManager())
		// .showAnimGifExportDialog();
		// }
		// });
		// }
		if (!app.isWhiteboardActive()) {
			menu.addItem(menuText("PSTricks (.txt)"), true, new MenuCommand(app) {

				@Override
				public void execute() {
					app.getActiveEuclidianView().setSelectionRectangle(null);
					app.getSelectionManager().clearSelectedGeos();

					menu.hide();
					app.getGgbApi()
							.exportPSTricks(exportCallback("Pstricks", app));
				}
			});

			menu.addItem(menuText("PGF/TikZ (.txt)"), true, new MenuCommand(app) {

				@Override
				public void execute() {
					app.getActiveEuclidianView().getEuclidianController()
							.clearSelectionAndRectangle();
					menu.hide();
					app.getGgbApi().exportPGF(exportCallback("PGF", app));
				}
			});

			menu.addItem(
					menuText(app.getLocalization()
							.getMenu("ConstructionProtocol") + " (."
							+ FileExtensions.HTML + ")"),
					true, new MenuCommand(app) {
						@Override
						public void doExecute() {
							menu.hide();
							app.exportStringToFile("html",
									app.getGgbApi().exportConstruction(true, "color",
											"name", "definition", "value"));
						}
					});

			menu.addItem(
					menuText(app.getLocalization()
							.getMenu("DynamicWorksheetAsWebpage") + " (."
							+ FileExtensions.HTML + ")"),
					true, new MenuCommand(app) {
						@Override
						public void doExecute() {
							menu.hide();
							app.exportStringToFile("html",
									HTML5Export.getFullString(app));
						}
					});

			menu.addItem(menuText("Asymptote (.txt)"), true, new MenuCommand(app) {

				@Override
				public void execute() {
					app.getActiveEuclidianView().getEuclidianController()
							.clearSelectionAndRectangle();
					menu.hide();
					app.getGgbApi()
							.exportAsymptote(exportCallback("Asymptote", app));
				}
			});

			menu.addItem(menuText(app.getLocalization()
					.getMenu("Download.3DPrint")), true, new MenuCommand(app) {
				@Override
				public void doExecute() {
					menu.hide();
					app.setExport3D(new FormatSTL());
				}
			});

			if (app.is3D()) {
				menu.addItem(menuText(app.getLocalization()
						.getMenu("Download.ColladaDae")), true, new MenuCommand(app) {
					@Override
					public void doExecute() {
						menu.hide();
						app.exportCollada(false);
					}
				});

				menu.addItem(menuText(app.getLocalization()
								.getMenu("Download.ColladaHtml")), true,
						new MenuCommand(app) {
							@Override
							public void doExecute() {
								menu.hide();
								app.exportCollada(true);
							}
						});

				menu.addItem(menuText(app.getLocalization()
						.getMenuDefault("Download.CASView", "CAS View (.html)")), true, new MenuCommand(app) {
					@Override
					public void doExecute() {
						menu.hide();
						CASExport casExp = new CASExport(app);
						String html = casExp.createHtml();
						html = StringUtil.fixForHTML(html);
						app.exportStringToFile("html", html);
					}
				});

				menu.addItem(menuText(app.getLocalization()
						.getMenuDefault("Download.CASViewMaple", "CAS View to Maple (.txt)")), true, new MenuCommand(app) {
					@Override
					public void doExecute() {
						menu.hide();
						CASExport casExp = new CASExport(app);
						String txt = casExp.createMapleTxt(true);
						app.exportStringToFile("txt", txt);
					}
				});

				menu.addItem(menuText(app.getLocalization()
						.getMenuDefault("Download.CASViewMathematica", "CAS View to Mathematica (.txt)")), true, new MenuCommand(app) {
					@Override
					public void doExecute() {
						menu.hide();
						CASExport casExp = new CASExport(app);
						String txt = casExp.createMathematicaTxt();
						app.exportStringToFile("txt", txt);
					}
				});

				menu.addItem(menuText(app.getLocalization()
						.getMenuDefault("Download.CASViewGiac", "CAS View to Giac (.txt)")), true, new MenuCommand(app) {
					@Override
					public void doExecute() {
						menu.hide();
						CASExport casExp = new CASExport(app);
						String txt = casExp.createGiacTxt();
						app.exportStringToFile("txt", txt);
					}
				});

				menu.addItem(menuText(app.getLocalization()
						.getMenuDefault("Download.CASViewLatex", "CAS View to LaTeX (.tex)")), true, new MenuCommand(app) {
					@Override
					public void doExecute() {
						menu.hide();
						CASExport casExp = new CASExport(app);
						String latex = casExp.createLatex();
						app.exportStringToFile("tex", latex);
						// FIXME: All web textual exports are encoded incorrectly (except HTML).
						// It seems they are not using Unicode but some ISO-8859-*
						// but the usual conversion techniques do not seem to work.
					}
				});

			}
		}
	}

	/**
	 * @param string
	 *            file type (for event logging)
	 * @param app
	 *            application
	 * @return callback for saving text export / images
	 */
	protected static AsyncOperation<String> exportCallback(final String string,
			final AppW app) {
		return new AsyncOperation<String>() {

			@Override
			public void callback(String obj) {
				String url = Browser.addTxtMarker(obj);
				app.getFileManager().showExportAsPictureDialog(url,
						app.getExportTitle(), "txt", "Export", app);
			}
		};
	}

	private static String menuText(String string) {
		return MainMenu.getMenuBarHtmlEmptyIcon(string);
	}

	/**
	 * Fire dialog open event
	 * 
	 * @param app
	 *            application to receive the evt
	 * 
	 * @param string
	 *            dialog name
	 */
	protected static void dialogEvent(AppW app, String string) {
		app.dispatchEvent(new org.geogebra.common.plugin.Event(
				EventType.OPEN_DIALOG, null, string));
	}

	/** hide the submenu */
	@Override
	public void hide() {
		Widget p = getParent();
		if (p instanceof GPopupPanel) {
			((GPopupPanel) p).hide();
		}
	}
}
