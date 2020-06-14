package com.minres.scviewer.e4.application.internal.status;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * The Class TextContributionItem.
 */
class TextContributionItem extends ContributionItem {

	/** The label string. */
	final String labelString;
	
	/** The text. */
	CLabel label, text;
	
	/** The content. */
	private String content;

	/**
	 * Instantiates a new text contribution item.
	 *
	 * @param labelString the label string
	 */
	public TextContributionItem(String labelString) {
		super();
		this.labelString = labelString;
		content=""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#fill(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void fill(Composite parent) {
		Composite box=new Composite(parent, SWT.NONE); //NONE
		box.setLayout(new GridLayout(2, false));
		label=new CLabel(box, SWT.SHADOW_NONE);	
		label.setText(labelString);
		text=new CLabel(box, SWT.SHADOW_IN);
		text.setAlignment(SWT.RIGHT);
		text.setText(" ");
		Point p = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		GridData layoutData=new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false);
		layoutData.minimumWidth=12*p.x;
		text.setLayoutData(layoutData);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
		return true;
	}

	/**
	 * Sets the text.
	 *
	 * @param message the new text
	 */
	public void setText(String message){
		this.content=message;
		if(text!=null && !text.isDisposed()) text.setText(content);
	}

}
