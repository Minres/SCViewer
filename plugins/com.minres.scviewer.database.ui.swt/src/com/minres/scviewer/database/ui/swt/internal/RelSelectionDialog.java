package com.minres.scviewer.database.swt.internal;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxRelation;

class RelSelectionDialog extends Dialog {
	private java.util.List<ITxRelation> entries;
	
	private java.util.List<ITx> entryTx;
	
	private ITxRelation selected = null;
	
	public RelSelectionDialog(Shell shell, ArrayList<ITxRelation> candidates, boolean target) {
		super(shell);
		entries = candidates;
		entryTx = entries.stream().map(r->target?r.getTarget():r.getSource()).collect(Collectors.toCollection(ArrayList::new));
	}

public ITxRelation open() {
    Shell parent = getParent();
    Shell dialog = new Shell(parent, SWT.SHEET | SWT.APPLICATION_MODAL);
    dialog.setMinimumSize(10, 10);
    
    RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
    //rowLayout.fill = true; // Overriding default values.
    rowLayout.marginWidth=3;
    rowLayout.marginHeight=0;
    rowLayout.marginLeft = 3;
    rowLayout.marginTop = 0;
    rowLayout.marginRight = 3;
    rowLayout.marginBottom = 0;
    dialog.setLayout(rowLayout);
    final Label lbl = new Label(dialog,SWT.NONE);
    lbl.setText("Select one:");
    final List list = new List (dialog, SWT.NONE);
    for (ITx iTx : entryTx) {
    	list.add ("#tx" + iTx.getId()+" ("+iTx.getStream().getFullName()+")");
	}
	list.addListener (SWT.Selection, e -> {
		int selection = list.getSelectionIndex();
		if(selection>=0) {
			selected=entries.get(selection);
			dialog.close();
		}
	});
	final Button bt = new Button(dialog, SWT.PUSH | SWT.RIGHT);
	bt.setText("Dismiss");
	bt.setAlignment(SWT.CENTER);
	bt.addSelectionListener(widgetSelectedAdapter(e -> dialog.close()));
	dialog.pack();
    dialog.open();
    Display display = parent.getDisplay();
    while (!dialog.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    return selected;
  }
}