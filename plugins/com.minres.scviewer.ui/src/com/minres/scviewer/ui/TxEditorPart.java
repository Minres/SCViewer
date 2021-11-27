/*******************************************************************************
 * Copyright (c) 2015-2021 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.ZoomKind;
import com.minres.scviewer.database.ui.swt.WaveformViewFactory;
import com.minres.scviewer.ui.views.TxOutlinePage;

public class TxEditorPart extends EditorPart implements ITabbedPropertySheetPageContributor {

	private static IWaveformDbFactory waveformDbFactory;
	
	public synchronized void bind(IWaveformDbFactory factory){
		waveformDbFactory=factory;
	}

	public synchronized void unbind(IWaveformDbFactory factory){
		if(waveformDbFactory==factory)
			waveformDbFactory=null;
	}
	
	public static final String ID = "com.minres.scviewer.ui.TxEditorPart"; //$NON-NLS-1$

	public static final String WAVE_ACTION_ID = "com.minres.scviewer.ui.action.AddToWave";

	private IWaveformView waveformView;

	/** This is the root of the editor's model. */
	private IWaveformDb database;

	private Composite myParent;

    private StatusLineContributionItem cursorStatusLineItem;

	public TxEditorPart() {
	}

	/**
	 * Create contents of the editor part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		myParent=parent;		
		database=waveformDbFactory.getDatabase();
		database.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if("WAVEFORMS".equals(evt.getPropertyName())) {
					myParent.getDisplay().syncExec(new Runnable() {						
						@Override
						public void run() {
							waveformView.setMaxTime(database.getMaxTime());
						}
					});
				}		
			}
		});
		WaveformViewFactory factory = new WaveformViewFactory();
		waveformView = factory.createPanel(parent);
		waveformView.setMaxTime(0);
		waveformView.addPropertyChangeListener(IWaveformView.CURSOR_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Long time = (Long) evt.getNewValue();
                cursorStatusLineItem.setText("Cursor: "+ time/1000000+"ns");
            }
        });
		getSite().setSelectionProvider(waveformView);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadDatabases();
				} catch (InvocationTargetException | IOException | InterruptedException e) {
					handleLoadException(e);
				}
			}
		}).run();
		cursorStatusLineItem.setText("Cursor: "+ waveformView.getCursorTime()/1000000+"ns");
		MenuManager menuMgr = new MenuManager("#PopupMenu");
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(IMenuManager manager) {
//				fillContextMenu(manager);
//			}
//		});
		Menu menu = menuMgr.createContextMenu(waveformView.getControl());
		waveformView.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, waveformView);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		if(input instanceof IFileEditorInput && !(input instanceof TxEditorInput))
			super.setInput(new TxEditorInput(((IFileEditorInput)input).getFile()));
		setPartName(input.getName());
	}

	protected void loadDatabases() throws IOException, InvocationTargetException, InterruptedException {
		IWorkbench wb = PlatformUI.getWorkbench();
		IProgressService ps = wb.getProgressService();
		IEditorInput input = getEditorInput();
		File file=null;
		boolean loadSecondary=false;
		boolean dontAskForSecondary=false;
		ArrayList<File> filesToLoad=new ArrayList<File>();
		if(input instanceof TxEditorInput){
			TxEditorInput txInput = (TxEditorInput) input;
			file = txInput.getFile().getLocation().toFile();
			loadSecondary=txInput.isSecondaryLoaded()==null||txInput.isSecondaryLoaded();
			dontAskForSecondary=txInput.isSecondaryLoaded()!=null;
		} else if(input instanceof FileStoreEditorInput){
			file=new File(((FileStoreEditorInput) input).getURI().getPath());
		}
		if(file.exists()){
			filesToLoad.add(file);
			if(loadSecondary){
			String ext = getFileExtension(file.getName());
			if("vcd".equals(ext.toLowerCase())){
				if(dontAskForSecondary || askIfToLoad(new File(renameFileExtension(file.getCanonicalPath(), "txdb")))){
					filesToLoad.add(new File(renameFileExtension(file.getCanonicalPath(), "txdb")));
					if(input instanceof TxEditorInput)  ((TxEditorInput) input).setSecondaryLoaded(true);
				}else if(dontAskForSecondary ||askIfToLoad(new File(renameFileExtension(file.getCanonicalPath(), "txlog")))){
					filesToLoad.add(new File(renameFileExtension(file.getCanonicalPath(), "txlog")));
					if(input instanceof TxEditorInput)  ((TxEditorInput) input).setSecondaryLoaded(true);
				}
			} else if("txdb".equals(ext.toLowerCase()) || "txlog".equals(ext.toLowerCase())){
				if(dontAskForSecondary || askIfToLoad(new File(renameFileExtension(file.getCanonicalPath(), "vcd")))){
					filesToLoad.add(new File(renameFileExtension(file.getCanonicalPath(), "vcd")));
					if(input instanceof TxEditorInput)  ((TxEditorInput) input).setSecondaryLoaded(true);
				}
			}
			}

		}
		final File[] files=filesToLoad.toArray(new File[filesToLoad.size()]);
		ps.run(true, false, new IRunnableWithProgress() {
		//ps.busyCursorWhile(new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				pm.beginTask("Loading database "+files[0].getName(), files.length);
				try {
					database.load(files[0]);
					database.addPropertyChangeListener(waveformView);
					pm.worked(1);
					if(pm.isCanceled()) return;
					if(files.length==2){
						database.load(files[1]);
						pm.worked(1);
					}
					myParent.getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							updateTxDisplay();
						}
					});
				} catch (Exception e) {
					database=null;
					throw new InvocationTargetException(e);
				}
				pm.done();
			}
		});
	}

	protected boolean askIfToLoad(File txFile) {
		if(txFile.exists() &&
				MessageDialog.openQuestion(myParent.getDisplay().getActiveShell(), "Database open", 
					"Would you like to open the adjacent database "+txFile.getName()+" as well?")){
				return true;
		}
		return false;
	}

	protected void updateTxDisplay() {
		waveformView.setMaxTime(database.getMaxTime());
		if(TxEditorPart.this.getEditorInput() instanceof TxEditorInput &&
				((TxEditorInput) TxEditorPart.this.getEditorInput()).getStreamNames().size()>0){
			for(String streamName:((TxEditorInput) TxEditorPart.this.getEditorInput()).getStreamNames()){
				IWaveform waveform = database.getStreamByName(streamName);
				if(waveform!=null)
					waveformView.addWaveform(waveform, -1);
			}
		}
	}

	protected static String renameFileExtension(String source, String newExt) {
		String target;
		String currentExt = getFileExtension(source);
		if (currentExt.equals("")){
			target=source+"."+newExt;
		} else {
			target=source.replaceFirst(Pattern.quote("."+currentExt)+"$", Matcher.quoteReplacement("."+newExt));
		}
		return target;
	}

	protected static String getFileExtension(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');
		if (i > 0 &&  i < f.length() - 1) {
			ext = f.substring(i + 1);
		}
		return ext;
	}

	private void handleLoadException(Exception e) {
		e.printStackTrace();
		MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
				"Error loading database", e.getMessage());
		database = null;
	}

	@Override
	public void setFocus() {
		myParent.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class type) {
		if (type == IContentOutlinePage.class) // outline page
			return new TxOutlinePage(this);
		else if (type == IPropertySheetPage.class) // use tabbed property sheet instead of standard one
			return new TabbedPropertySheetPage(this);
		return super.getAdapter(type);
	}

	public IWaveformDb getModel() {
		return database;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		// Initialize the editor part
		setSite(site);
		setInput(input);
        cursorStatusLineItem = new StatusLineContributionItem("TxEditorCursorContributionItem");
		IActionBars actionBars = getEditorSite().getActionBars();
		IStatusLineManager manager = actionBars.getStatusLineManager();
        manager.add(cursorStatusLineItem);
		actionBars.updateActionBars();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public IWaveformDb getDatabase() {
		return database;
	}

	public void addStreamToList(IWaveform obj){
		if(getEditorInput() instanceof TxEditorInput && !((TxEditorInput) getEditorInput()).getStreamNames().contains(obj.getFullName())){
			((TxEditorInput) getEditorInput()).getStreamNames().add(obj.getFullName());
			waveformView.addWaveform(obj, -1);
		} else
			waveformView.addWaveform(obj, -1);

	}

	public void addStreamsToList(IWaveform[] iWaveforms){
		for(IWaveform stream:iWaveforms)
			addStreamToList(stream);
	}

	public void removeStreamFromList(IWaveform waveform){
		if(getEditorInput() instanceof TxEditorInput && ((TxEditorInput) getEditorInput()).getStreamNames().contains(waveform.getFullName())){
			((TxEditorInput) getEditorInput()).getStreamNames().remove(waveform.getFullName());
		}
		TrackEntry entry=null;
		for(TrackEntry e:waveformView.getStreamList()) {
			if(e.waveform==waveform) {
				entry=e;
				break;
			}
		}
		waveformView.getStreamList().remove(entry);
	}

	public void removeStreamsFromList(IWaveform[] iWaveforms){
		for(IWaveform stream:iWaveforms)
			removeStreamFromList(stream);
	}

	public List<TrackEntry> getStreamList(){
		return waveformView.getStreamList();
	}

	public void setSelection(final ISelection selection){
		myParent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				waveformView.setSelection(selection);
			}
		});
	}

	public ISelection getSelection(){
		return waveformView.getSelection();
	}
	
	@Override
	public String getContributorId() {
		return getSite().getId();
	}

	public void moveSelection(GotoDirection next) {
		waveformView.moveSelection( next);		
	}

	public void setZoom(ZoomKind kind) {
		waveformView.getWaveformZoom().zoom(kind);		
	}

	public void removeSelected() {
		// TODO TxDisplay needs to be extended
	}

}
