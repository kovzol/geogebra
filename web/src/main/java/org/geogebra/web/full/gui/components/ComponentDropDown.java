package org.geogebra.web.full.gui.components;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.event.PointerEventType;
import org.geogebra.web.full.gui.menubar.MainMenu;
import org.geogebra.web.full.javax.swing.GPopupMenuW;
import org.geogebra.web.html5.gui.util.AriaMenuItem;
import org.geogebra.web.html5.gui.util.ClickStartHandler;
import org.geogebra.web.html5.main.AppW;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author kriszta
 *
 *         dropdown material design component
 *
 */

public class ComponentDropDown extends FlowPanel {

	// for checking item position. It is 0 in normal case.
	private static final int POPUP_X_OFFSET = 32;

    private Label titleLabel;
    private Label selectedOptionLabel;
    private GPopupMenuW dropDownMenu;
    private List<AriaMenuItem> dropDownElementsList;
    private DropDownSelectionCallback selectionCallback;
	private int selectedIndex;

	/**
	 *
	 * @param app AppW
	 */
    public ComponentDropDown(AppW app) {
        buildGui(app);
    }

    private void buildGui(AppW app) {
        FlowPanel contentPanel = new FlowPanel();
        contentPanel.setStyleName("dropDownSelector");

        createTitleLabel();
        createSelectedOptionLabel();
        createDropDownMenu(app);

        contentPanel.add(titleLabel);
        contentPanel.add(selectedOptionLabel);

        add(contentPanel);
    }

    private void createTitleLabel() {
        titleLabel = new Label();

        titleLabel.setStyleName("titleLabel");
    }

    private void createSelectedOptionLabel() {
        selectedOptionLabel = new Label();

        selectedOptionLabel.setText("Hungarian");
        selectedOptionLabel.setStyleName("selectedOptionLabel");
    }

    private void createDropDownMenu(AppW app) {
        dropDownMenu = new GPopupMenuW(app);
        dropDownMenu.getPopupPanel().addStyleName("matMenu");
        dropDownMenu.getPopupPanel().addStyleName("dropDownPopup");
		final EuclidianView view = app.getActiveEuclidianView();
        ClickStartHandler.init(this, new ClickStartHandler(true, true) {

            @Override
            public void onClickStart(int x, int y, PointerEventType type) {
				openAt(view, titleLabel);
            }
        });
    }

	/**
	 * Opens DropDown at the top of the widget positioning selected item at the
	 * center.
	 */
	void openAt(EuclidianView view, Widget anchor) {
		int itemTop = (getSelectedIndex() + 1) * getItemHeight();

		if (itemTop < anchor.getAbsoluteTop()) {
			// everything fits fine, no scrollbar
			dropDownMenu.showAtPoint(anchor.getAbsoluteLeft(),
					(anchor.getAbsoluteTop() - itemTop) + POPUP_X_OFFSET);
		} else {
			openScrollablePopup(anchor, itemTop, view.getHeight() / 2);
		}
	}

	private void openScrollablePopup(Widget anchor, int itemTop, int popupHeight) {
		int anchorTop = anchor.getAbsoluteTop();
		int anchorLeft = anchor.getAbsoluteLeft() + POPUP_X_OFFSET;
		int allHeight = getAllItemsHeight();
		int h2 = popupHeight / 2;
		int scrollTop = itemTop - h2;
		int diff = allHeight - itemTop;
		if (diff < h2) {
			if (diff + h2 < anchorTop) {
				// many items, but there is space to go up;
				dropDownMenu.showAtPoint(anchorLeft, anchorTop - h2 - diff);
			} else {
				// no space: put at 0 and scroll
				// what if the popup is longer than anchortop??
				openAndScrollTo(anchorLeft, 0, itemTop);
			}
		} else {
			// center popup and scroll;
			openAndScrollTo(anchorLeft, anchorTop - h2, scrollTop);
		}

	}

	private void openAndScrollTo(int left, int top, int position) {
		dropDownMenu.showAtPoint(left, top);
		setVerticalScrollPosition(position);
	}

	private void setVerticalScrollPosition(int pos) {
		dropDownMenu.getPopupPanel().getElement().setScrollTop(pos);
	}

	private int getItemHeight() {
		return titleLabel.getOffsetHeight();
	}

	private int getAllItemsHeight() {
		return dropDownMenu.getComponentCount() * getItemHeight();
	}

	private void setupDropDownMenu(List<AriaMenuItem> menuItems) {
        for (AriaMenuItem menuItem : menuItems) {
            dropDownMenu.addItem(menuItem);
        }
    }

	private int getSelectedIndex() {
		return selectedIndex;
	}

	/**
	 * set the title of the dropdown in the preview view
	 *
	 * @param title the localized title which is displayed above the selected option
	 */
    public void setTitleText(String title) {
        titleLabel.setText(title);
    }

    /**
     * set the selected option in the preview view
     * @param selected
     *      index of the selected item from the dropdown list
     */
    public void setSelected(int selected) {
		selectedIndex = selected;
		selectedOptionLabel.setText(dropDownElementsList.get(selected).getElement().getInnerText());
    }

    /**
     * Set the elements of the dropdown list
     * @param dropDownList
     *      List of strings which will be shown in the dropdown list
     */
    public void setElements(final List<String> dropDownList) {
        dropDownElementsList = new ArrayList<>();

        for (int i = 0; i < dropDownList.size(); ++i) {
            final int currentIndex = i;
            AriaMenuItem item = new AriaMenuItem(
                    MainMenu.getMenuBarHtmlNoIcon(dropDownList.get(i)), true,
                    new Command() {
                        @Override
                        public void execute() {
                            setSelected(currentIndex);
                            if (selectionCallback != null) {
                                selectionCallback.onSelectionChanged(currentIndex);
                            }
                        }
                    });

            item.setStyleName("dropDownElement");
            dropDownElementsList.add(item);
        }
        setupDropDownMenu(dropDownElementsList);
    }

    /**
     * set itemSelected callback
     * @param callback
     *      which will be called after an item was selected from the dropdown
     *
     */
    public void setDropDownSelectionCallback(DropDownSelectionCallback callback) {
        selectionCallback = callback;
    }

    public interface DropDownSelectionCallback {
        void onSelectionChanged(int index);
    }
}