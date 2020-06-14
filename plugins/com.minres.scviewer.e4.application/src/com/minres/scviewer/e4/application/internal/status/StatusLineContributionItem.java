package com.minres.scviewer.e4.application.internal.status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.resource.JFaceColors;


/**
 * Contribution item for the status line.
 * @since 2.0
 */
public class StatusLineContributionItem extends ContributionItem {

	/**
	 * Internal mouse listener to track double clicking the status line item.
	 * @since 3.0
	 */
	private class Listener extends MouseAdapter {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (fActionHandler != null && fActionHandler.isEnabled())
				fActionHandler.run();
		}
	}

	/**
	 * Left and right margin used in CLabel.
	 * @since 2.1
	 */
	private static final int INDENT= 3;
	/**
	 * Default number of characters that should fit into the item.
	 * @since 3.0
	 */
	static final int DEFAULT_WIDTH_IN_CHARS = 19;
	/**
	 * Pre-computed label width hint.
	 * @since 2.1
	 */
	private int fFixedWidth= -1;
	/**
	 * Pre-computed label height hint.
	 * @since 3.0
	 */
	private int fFixedHeight= -1;
	/** The text */
	private String fText;
	/** The image */
	private Image fImage;
	/**
	 * The error text.
	 * @since 3.0
	 */
	private String fErrorText;
	/**
	 * The error image.
	 * @since 3.0
	 */
	private Image fErrorImage;
	/**
	 * The tool tip text.
	 * @since 3.0
	 */
	private String fToolTipText;
	/**
	 * Number of characters that should fit into the item.
	 * @since 3.0
	 */
	private int fWidthInChars;
	/** The status line label widget */
	private CLabel fLabel;
	/**
	 * The action handler.
	 * @since 3.0
	 */
	private IAction fActionHandler;
	/**
	 * The mouse listener
	 * @since 3.0
	 */
	private MouseListener fMouseListener;


	/**
	 * Creates a new item with the given attributes.
	 *
	 * @param id the item's id
	 * @param visible the visibility of this item
	 * @param widthInChars the width in characters
	 * @since 3.0
	 */
	public StatusLineContributionItem(String id, boolean visible, int widthInChars) {
		super(id);
		setVisible(visible);
		fWidthInChars= widthInChars;
	}

	@Override
	public void fill(Composite parent) {

		Label sep= new Label(parent, SWT.SEPARATOR);
		CLabel label=new CLabel(parent, SWT.SHADOW_NONE);	
		label.setText(getId());
		fLabel= new CLabel(parent, SWT.SHADOW_IN);
		fLabel.setAlignment(SWT.RIGHT);

		fLabel.addDisposeListener(e -> fMouseListener = null);
		if (fActionHandler != null) {
			fMouseListener= new Listener();
			fLabel.addMouseListener(fMouseListener);
		}

		StatusLineLayoutData data= new StatusLineLayoutData();
		data.widthHint= getWidthHint(parent);
		fLabel.setLayoutData(data);

		data= new StatusLineLayoutData();
		data.heightHint= getHeightHint(parent);
		sep.setLayoutData(data);

		updateMessageLabel();
	}

	public void setActionHandler(IAction actionHandler) {
		if (fActionHandler != null && actionHandler == null && fMouseListener != null) {
			if (!fLabel.isDisposed())
				fLabel.removeMouseListener(fMouseListener);
			fMouseListener= null;
		}

		fActionHandler= actionHandler;

		if (fLabel != null && !fLabel.isDisposed() && fMouseListener == null && fActionHandler != null) {
			fMouseListener= new Listener();
			fLabel.addMouseListener(fMouseListener);
		}
	}

	/**
	 * Returns the width hint for this label.
	 *
	 * @param control the root control of this label
	 * @return the width hint for this label
	 * @since 2.1
	 */
	private int getWidthHint(Composite control) {
		if (fFixedWidth < 0) {
			GC gc= new GC(control);
			gc.setFont(control.getFont());
			fFixedWidth = (int) gc.getFontMetrics().getAverageCharacterWidth() * fWidthInChars;
			fFixedWidth += INDENT * 2;
			gc.dispose();
		}
		return fFixedWidth;
	}

	/**
	 * Returns the height hint for this label.
	 *
	 * @param control the root control of this label
	 * @return the height hint for this label
	 * @since 3.0
	 */
	private int getHeightHint(Composite control) {
		if (fFixedHeight < 0) {
			GC gc= new GC(control);
			gc.setFont(control.getFont());
			fFixedHeight= gc.getFontMetrics().getHeight();
			gc.dispose();
		}
		return fFixedHeight;
	}

	/**
	 * Updates the message label widget.
	 *
	 * @since 3.0
	 */
	private void updateMessageLabel() {
		if (fLabel != null && !fLabel.isDisposed()) {
			Display display= fLabel.getDisplay();
			if ((fErrorText != null && !fErrorText.isEmpty()) || fErrorImage != null) {
				fLabel.setForeground(JFaceColors.getErrorText(display));
				fLabel.setText(fErrorText);
				fLabel.setImage(fErrorImage);
				if (fToolTipText != null)
					fLabel.setToolTipText(fToolTipText);
				else if (fErrorText.length() > fWidthInChars)
					fLabel.setToolTipText(fErrorText);
				else
					fLabel.setToolTipText(null);

			} else {
				fLabel.setForeground(fLabel.getParent().getForeground());
				fLabel.setText(fText);
				fLabel.setImage(fImage);
				if (fToolTipText != null)
					fLabel.setToolTipText(fToolTipText);
				else if (fText != null && fText.length() > fWidthInChars)
					fLabel.setToolTipText(fText);
				else
					fLabel.setToolTipText(null);
			}
		}
	}

	public void setText(String message){
		this.fText=message;
		updateMessageLabel();
	}

	public void setErrorText(String message){
		this.fErrorText=message;
		updateMessageLabel();
	}
}

