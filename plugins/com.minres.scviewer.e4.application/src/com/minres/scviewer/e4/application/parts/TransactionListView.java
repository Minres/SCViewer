package com.minres.scviewer.e4.application.parts;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.minres.scviewer.database.ui.TrackEntry;

public class TransactionListView {
	private WaveformViewer waveformViewer;

	private TransactionList transactionList;
		
	/** The event broker. */
	@Inject IEventBroker eventBroker;

	/** The selection service. */
	@Inject	ESelectionService selectionService;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	/**
	 * Creates the composite.
	 *
	 * @param parent the parent
	 */
	@PostConstruct
	public void createComposite(final Composite parent, @Optional WaveformViewer waveformViewer) {
		this.waveformViewer=waveformViewer;
		transactionList = new TransactionList(parent, SWT.BORDER, waveformViewer);
	}
	
	@Focus
	public void setFocus() {
		if(transactionList!=null)
			transactionList.setFocus();
	}
	/**
	 * Sets the selection.
	 *
	 * @param selection the new selection
	 */
	@Inject
	public void setSelection(@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection, EPartService partService){
		// only react if selection is actually from the WaveformViewer and nothing else
		MPart part = partService.getActivePart();
		if( part == null || ! (part.getObject() instanceof WaveformViewer )  || part.getObject() != waveformViewer)
			return;
		if(selection==null || selection.isEmpty())
			transactionList.setInput(null);	
		else if(selection instanceof IStructuredSelection) {
			TrackEntry e = findTrackEntry(((StructuredSelection)selection).toArray());
			if(e!=null)
				transactionList.setInput(e);		
		}
	}

	private TrackEntry findTrackEntry(Object[] elems) {
		for(Object o: elems)
			if(o instanceof TrackEntry)
				return (TrackEntry)o;
		return null;
	}

	public TransactionList getControl() {
		return transactionList;
	}

}
