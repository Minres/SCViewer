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
package com.minres.scviewer.e4.application.parts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.RelationTypeFactory;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.ITxRelation;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.TrackEntry.ValueDisplay;
import com.minres.scviewer.database.ui.TrackEntry.WaveDisplay;
import com.minres.scviewer.database.ui.swt.Constants;
import com.minres.scviewer.database.ui.swt.IToolTipContentProvider;
import com.minres.scviewer.database.ui.swt.IToolTipHelpTextProvider;
import com.minres.scviewer.database.ui.swt.WaveformViewFactory;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.internal.status.WaveStatusBarControl;
import com.minres.scviewer.e4.application.internal.util.FileMonitor;
import com.minres.scviewer.e4.application.internal.util.IFileChangeListener;
import com.minres.scviewer.e4.application.internal.util.IModificationChecker;
import com.minres.scviewer.e4.application.preferences.PreferenceConstants;

/**
 * The Class WaveformViewerPart.
 */
@SuppressWarnings("restriction")
public class WaveformViewer implements IFileChangeListener, IPreferenceChangeListener, DisposeListener {

	/** The Constant ACTIVE_WAVEFORMVIEW. */
	public static final String ACTIVE_WAVEFORMVIEW = "Active_Waveform_View"; //$NON-NLS-1$

	/** The Constant ADD_WAVEFORM. */
	public static final String ADD_WAVEFORM = "AddWaveform"; //$NON-NLS-1$

	/** The Constant DATABASE_FILE. */
	protected static final String DATABASE_FILE = "DATABASE_FILE"; //$NON-NLS-1$
	
	/** The Constant SHOWN_WAVEFORM. */
	protected static final String SHOWN_WAVEFORM = "SHOWN_WAVEFORM"; //$NON-NLS-1$
	
	protected static final String VALUE_DISPLAY = ".VALUE_DISPLAY"; //$NON-NLS-1$
	
	protected static final String WAVE_DISPLAY = ".WAVE_DISPLAY"; //$NON-NLS-1$
	
	/** The Constant SHOWN_CURSOR. */
	protected static final String SHOWN_CURSOR = "SHOWN_CURSOR"; //$NON-NLS-1$
	
	/** The Constant ZOOM_LEVEL. */
	protected static final String ZOOM_LEVEL = "ZOOM_LEVEL"; //$NON-NLS-1$

	/** The Constant BASE_LINE_TIME. */
	protected static final String BASE_LINE_TIME = "BASE_LINE_TIME"; //$NON-NLS-1$
	
	/** The Constant SELECTED_TX_ID. */
	protected static final String SELECTED_TX_ID = "SELECTED_TX_ID"; //$NON-NLS-1$

	/** The Constant SELECTED_TRACKENTRY_NAME. */
	protected static final String SELECTED_TRACKENTRY_NAME = "SELECTED_TRACKENTRY_NAME"; //$NON-NLS-1$
	
	/** The Constant WAVEFORM_SELECTED. */
	protected static final String WAVEFORM_SELECTED = ".WAVEFORM_SELECTED"; //$NON-NLS-1$
	
	/** The Constant FILE_CHECK_INTERVAL. */
	protected static final long FILE_CHECK_INTERVAL = 60000;
	
	/** The zoom level. */
	private String[] zoomLevel;

	/** The Constant ID. */
	public static final String ID = "com.minres.scviewer.ui.TxEditorPart"; //$NON-NLS-1$

	/** The Constant WAVE_ACTION_ID. */
	public static final String WAVE_ACTION_ID = "com.minres.scviewer.ui.action.AddToWave"; //$NON-NLS-1$

	private static final String MENU_CONTEXT = "com.minres.scviewer.e4.application.popupmenu.wavecontext"; //$NON-NLS-1$
	
	/** The number of active DisposeListeners */
	private int disposeListenerNumber = 0;
	
	/** The factory. */
	WaveformViewFactory factory = new WaveformViewFactory();

	DesignBrowser browser = null;
	
	TransactionDetails detailsView = null;
	
	TransactionListView transactionList = null;
	
	/** The waveform pane. */
	private IWaveformView waveformPane;

	/** get UISynchronize injected as field */
	@Inject UISynchronize sync;

	/** The event broker. */
	@Inject
	private IEventBroker eventBroker;

	/** The menu service. */
	@Inject
	EMenuService menuService;

	/** The selection service. */
	@Inject
	ESelectionService selectionService;

	/** The part service. */
	@Inject
	EPartService ePartService;

	IEclipsePreferences store = null;

	@Inject @Optional DesignBrowser designBrowser;

	/** The database. */
	private IWaveformDb database;

	/** The check for updates. */
	private boolean checkForUpdates;

	/** The my part. */
	@Inject private MPart myPart;

	/** The my parent. */
	private Composite myParent;

	/** The files to load. */
	ArrayList<File> filesToLoad = new ArrayList<>();

	String partConfig = "";
	
	/** The persisted state. */
	Map<String, String> persistedState;

	/** The browser state. */
	private Object browserState;

	/** The details settings. */
	private Object detailsSettings;

	/** The navigation relation type. */
	private RelationType navigationRelationType=IWaveformView.NEXT_PREV_IN_STREAM ;

	/** The file monitor. */
	FileMonitor fileMonitor = new FileMonitor();

	/** The file checker. */
	IModificationChecker fileChecker;

	@Inject IWaveformDbFactory dbFactory;
	
	@Inject Composite parent;
	
	private boolean showHover;

	/**
	 * Creates the composite.
	 *
	 * @param part the part
	 * @param parent the parent
	 * @param dbFactory the db factory
	 */
	@PostConstruct
	public void createComposite(MPart part, EMenuService menuService, @Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE) IEclipsePreferences prefs, @Preference(value = PreferenceConstants.SHOW_HOVER) Boolean hover) {
		disposeListenerNumber += 1;
		
		myPart = part;
		myParent = parent;
		store=prefs;
		showHover=hover;
		database = dbFactory.getDatabase();
		database.addPropertyChangeListener(evt -> {
			if (IHierNode.WAVEFORMS.equals(evt.getPropertyName())) { //$NON-NLS-1$
				myParent.getDisplay().syncExec(() -> waveformPane.setMaxTime(database.getMaxTime()));
			}
		});
		parent.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		IEclipseContext ctx = myPart.getContext();
		ctx.set(WaveformViewer.class, this);
		ctx.set(IWaveformDb.class, database);

		SashForm topSash = new SashForm(parent, SWT.BORDER | SWT.SMOOTH | SWT.HORIZONTAL);
		Composite left = new Composite(topSash, SWT.NONE);
		SashForm middleSash = new SashForm(topSash, SWT.BORDER | SWT.SMOOTH | SWT.VERTICAL);
		Composite right = new Composite(topSash, SWT.NONE);
		topSash.setWeights(new int[] {20, 60, 20});

		Composite middleTop = new Composite(middleSash, SWT.NONE);
		Composite middleBottom = new Composite(middleSash, SWT.NONE);
		middleSash.setWeights(new int[] {75, 25});

		ctx.set(Composite.class, left);
		browser = ContextInjectionFactory.make(DesignBrowser.class, ctx);
		
		ctx.set(Composite.class, right);
		detailsView = ContextInjectionFactory.make(TransactionDetails.class, ctx);

		waveformPane = factory.createPanel(middleTop);
		
		ctx.set(Composite.class, middleBottom);
		transactionList = ContextInjectionFactory.make(TransactionListView.class, ctx);
		
		waveformPane.setMaxTime(0);
		//set selection to empty selection when opening a new waveformPane
		selectionService.setSelection(new StructuredSelection());
		
		waveformPane.addPropertyChangeListener(IWaveformView.CURSOR_PROPERTY, evt -> {
				Long time = (Long) evt.getNewValue();
				eventBroker.post(WaveStatusBarControl.CURSOR_TIME, waveformPane.getScaledTime(time));
				long marker = waveformPane.getMarkerTime(waveformPane.getSelectedMarkerId());
				eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getScaledTime(time - marker));
		});
		waveformPane.addPropertyChangeListener(IWaveformView.MARKER_PROPERTY, evt -> {
				Long time = (Long) evt.getNewValue();
				eventBroker.post(WaveStatusBarControl.MARKER_TIME, waveformPane.getScaledTime(time));
				long cursor = waveformPane.getCursorTime();
				eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getScaledTime(cursor - time));
		});
		
		waveformPane.addSelectionChangedListener(event -> {
				if (event.getSelection() instanceof IStructuredSelection) {
					selectionService.setSelection(event.getSelection());
				}
		});

		waveformPane.getWaveformControl().addListener(SWT.KeyDown, e -> {
				if((e.stateMask&SWT.MOD3)!=0) { // Alt key
				} else if((e.stateMask&SWT.MOD1)!=0) { //Ctrl/Cmd
					int zoomlevel = waveformPane.getZoomLevel();
					switch(e.keyCode) {
					case '+':
					case SWT.KEYPAD_ADD:
						if(zoomlevel>0)
							waveformPane.setZoomLevel(zoomlevel-1);
						return;
					case '-':
					case SWT.KEYPAD_SUBTRACT:
						if(zoomlevel<waveformPane.getZoomLevels().length-1)
							waveformPane.setZoomLevel(zoomlevel+1);
						return;
					case SWT.ARROW_UP:
						waveformPane.moveSelectedTrack(-1);
						return;
					case SWT.ARROW_DOWN:
						waveformPane.moveSelectedTrack(1);
						return;
					default:
						break;
					}
				} else if((e.stateMask&SWT.MOD2)!=0) { //Shift
					switch(e.keyCode) {
					case SWT.ARROW_LEFT:
						waveformPane.scrollHorizontal(-100);
						return;
					case SWT.ARROW_RIGHT:
						waveformPane.scrollHorizontal(100);
						return;
					default:
						break;
					}
				} else {
					switch(e.keyCode) {
					case SWT.ARROW_LEFT:
						waveformPane.scrollHorizontal(-10);
						return;
					case SWT.ARROW_RIGHT:
						waveformPane.scrollHorizontal(10);
						return;
					case SWT.ARROW_UP:
						waveformPane.moveSelection(GotoDirection.UP);
						return;
					case SWT.ARROW_DOWN:
						waveformPane.moveSelection(GotoDirection.DOWN);
						return;
					case SWT.HOME:
						waveformPane.scrollTo(IWaveformView.MARKER_POS);
						return;
					case SWT.END:
						waveformPane.scrollTo(IWaveformView.CURSOR_POS);
						return;
					case SWT.DEL:
						waveformPane.deleteSelectedTracks();
					default:
						break;
					}
				}
		});
		
		zoomLevel = waveformPane.getZoomLevels();
		checkForUpdates = store.getBoolean(PreferenceConstants.DATABASE_RELOAD, true);
		filesToLoad = new ArrayList<>();
		persistedState = part.getPersistedState();
		Integer files = persistedState.containsKey(DATABASE_FILE + "S") //$NON-NLS-1$
				? Integer.parseInt(persistedState.get(DATABASE_FILE + "S")) : 0; //$NON-NLS-1$
		for (int i = 0; i < files; i++) {
			filesToLoad.add(new File(persistedState.get(DATABASE_FILE + i)));
		}
		if (!filesToLoad.isEmpty())
			loadDatabase(persistedState);
		eventBroker.post(WaveStatusBarControl.ZOOM_LEVEL, zoomLevel[waveformPane.getZoomLevel()]);
		menuService.registerContextMenu(waveformPane.getNameControl(), MENU_CONTEXT);
		menuService.registerContextMenu(waveformPane.getValueControl(),	MENU_CONTEXT);
		menuService.registerContextMenu(waveformPane.getWaveformControl(), MENU_CONTEXT);
		ePartService.addPartListener(new PartListener() {
			@Override
			public void partActivated(MPart part) {
				if (part == myPart) {
					if (fileChecker != null)
						fileChecker.check();
					updateAll();
				}
			}
		});
		waveformPane.addDisposeListener(this);

		waveformPane.getWaveformControl().setData(Constants.HELP_PROVIDER_TAG, new IToolTipHelpTextProvider() {
			@Override
			public String getHelpText(Widget widget) {
				return "Waveform pane: press F2 to set the focus to the tooltip";
			}
		});
		waveformPane.getWaveformControl().setData(Constants.CONTENT_PROVIDER_TAG, new IToolTipContentProvider() {
			@Override
			public boolean createContent(Composite parent, Point pt) {
				if(!showHover) return false;
				List<Object> res = waveformPane.getElementsAt(pt);
				if(!res.isEmpty()) {
					if(res.get(0) instanceof ITx) {
						ITx tx = (ITx)res.get(0);
						final Display display = parent.getDisplay();
						final Font font = new Font(Display.getCurrent(), "Terminal", 10, SWT.NORMAL);

						final Label label = new Label(parent, SWT.SHADOW_IN);
						label.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
						label.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
						label.setText(tx.toString());
						label.setFont(font);
						GridData labelGridData = new GridData();
						labelGridData.horizontalAlignment = GridData.FILL;
						labelGridData.grabExcessHorizontalSpace = true;
						label.setLayoutData(labelGridData);

						final Table table = new Table(parent, SWT.NONE);
						table.setHeaderVisible(true);
						table.setLinesVisible(true);
						table.setFont(font);
						label.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
						label.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
						GridData tableGridData = new GridData();
						tableGridData.horizontalAlignment = GridData.FILL;
						tableGridData.grabExcessHorizontalSpace = true;
						table.setLayoutData(tableGridData);

						final TableColumn nameCol = new TableColumn(table, SWT.LEFT);
						nameCol.setText("Attribute");
						final TableColumn valueCol = new TableColumn(table, SWT.LEFT);
						valueCol.setText("Value");

						for (ITxAttribute iTxAttribute : tx.getAttributes()) {
							String value = iTxAttribute.getValue().toString();
							if((DataType.UNSIGNED == iTxAttribute.getDataType() || DataType.INTEGER==iTxAttribute.getDataType()) && !"0".equals(value)) {
								try {
									value += " [0x"+Long.toHexString(Long.parseLong(iTxAttribute.getValue().toString()))+"]";
								} catch(NumberFormatException e) { }
							}
							TableItem item = new TableItem(table, SWT.NONE);
							item.setText(0, iTxAttribute.getName());
							item.setText(1, value);
						}
						if(table.getHeaderVisible()) {
							// add dummy row to get make last row visible
							TableItem item = new TableItem(table, SWT.NONE);
							item.setText(0, "");
							item.setText(1, "");
						}
						nameCol.pack();
						valueCol.pack();
						table.setSize(table.computeSize(SWT.DEFAULT, SWT.DEFAULT));
						parent.addPaintListener( e -> {
								Rectangle area = parent.getClientArea();
								valueCol.setWidth(area.width - nameCol.getWidth());
						});
						parent.addFocusListener(FocusListener.focusGainedAdapter(e -> table.setFocus()));
						return true;
					} else if(res.get(0) instanceof TrackEntry) {
						TrackEntry te = (TrackEntry)res.get(0);
						final Font font = new Font(Display.getCurrent(), "Terminal", 10, SWT.NORMAL);

						final Label label = new Label(parent, SWT.NONE);
						label.setText(te.waveform.getFullName());
						label.setFont(font);
						GridData labelGridData = new GridData();
						labelGridData.horizontalAlignment = GridData.FILL;
						labelGridData.grabExcessHorizontalSpace = true;
						label.setLayoutData(labelGridData);
						return true;
					}
				}
				return false;
			}
		});
		waveformPane.setStyleProvider(new WaveformStyleProvider(store));
	}

	@Inject
	@Optional
	public void reactOnPrefsChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE) IEclipsePreferences prefs) {
		prefs.addPreferenceChangeListener(this);
		
	}
	
	@Inject
	@Optional
	public void reactOnShowHoverChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE, value = PreferenceConstants.SHOW_HOVER) Boolean hover) {
		showHover=hover;
	}
	
	@Inject
	@Optional
	public void reactOnReloadDatabaseChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE, value = PreferenceConstants.DATABASE_RELOAD) Boolean checkForUpdates) {
		if (checkForUpdates.booleanValue()) {
			fileChecker = fileMonitor.addFileChangeListener(WaveformViewer.this, filesToLoad, FILE_CHECK_INTERVAL);
		} else { 
			fileMonitor.removeFileChangeListener(WaveformViewer.this);
			fileChecker = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
	 */
	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (!PreferenceConstants.DATABASE_RELOAD.equals(event.getKey()) && !PreferenceConstants.SHOW_HOVER.equals(event.getKey())){
			waveformPane.setStyleProvider(new WaveformStyleProvider(store));		}
	}

	class DbLoadJob extends Job {
		final File file;
		public DbLoadJob(String name, final File file) {
			super(name);
			this.file=file;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			boolean res = database.load(file);
			database.addPropertyChangeListener(waveformPane);
			return res?Status.OK_STATUS:Status.CANCEL_STATUS;
		}
	}
	/**
	 * Load database.
	 *
	 * @param state the state
	 */
	protected void loadDatabase(final Map<String, String> state) {
		loadDatabase(state, 1000L);
	}

	protected void loadDatabase(final Map<String, String> state, long delay) {
		fileMonitor.removeFileChangeListener(this);
		database.setName(filesToLoad.stream().map(File::getName).reduce(null, (prefix, element) -> prefix==null? element : prefix + ","+ element));
		Job job = new Job(Messages.WaveformViewer_15) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IProgressMonitor progressGroup = getJobManager().createProgressGroup(); 
				JobGroup jobGroup = new JobGroup(Messages.WaveformViewer_15, filesToLoad.size(), filesToLoad.size());
				filesToLoad.forEach((final File file) -> {
					Job job = new DbLoadJob(Messages.WaveformViewer_16 + file.getName(), file);
					job.setProgressGroup(progressGroup, 1);
					job.setJobGroup(jobGroup);
					job.schedule();
				});
				try {
					jobGroup.join(0, monitor);
				} catch (OperationCanceledException e) {
					throw new OperationCanceledException(Messages.WaveformViewer_14);
				}catch (InterruptedException e) {
					// Restore interrupted state...
					Thread.currentThread().interrupt();
					throw new OperationCanceledException(Messages.WaveformViewer_14);
				}
				if (monitor.isCanceled())
					throw new OperationCanceledException(Messages.WaveformViewer_14);

				
				IStatus result = jobGroup.getResult();
				if( (!result.isMultiStatus() && result.getCode() != Status.OK_STATUS.getCode() ) ||
						(result.isMultiStatus() && result.getChildren().length > 0 && result.getChildren()[0].getCode() != Status.OK_STATUS.getCode() ) ){
					// kill editor and pop up warning for user
					sync.asyncExec(() -> {
						if(myParent.isDisposed()) return;
						final Display display = myParent.getDisplay();
						MessageDialog.openWarning(display.getActiveShell(), "Error loading database", "Database cannot be loaded. Aborting...");
						ePartService.hidePart(myPart, true);
					});
				} else
					sync.asyncExec(()->{
						waveformPane.setMaxTime(database.getMaxTime());
						if(partConfig!=null && !partConfig.isEmpty())
							loadState(partConfig);
						if (state != null && !state.isEmpty())
							restoreWaveformViewerState(state);
						fileChecker = null;
						if (checkForUpdates)
							fileChecker = fileMonitor.addFileChangeListener(WaveformViewer.this, filesToLoad, FILE_CHECK_INTERVAL);
					});
				return result;
			}
		};
		job.setName("Load Database");
		job.setSystem(true);
		job.schedule(delay); // let the UI initialize so that we have a progress monitor
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.e4.application.internal.util.IFileChangeListener#fileChanged(java.util.List)
	 */
	@Override
	public void fileChanged(List<File> file) {
		final Display display = myParent.getDisplay();
		display.asyncExec(() -> {
				if (MessageDialog.openQuestion(display.getActiveShell(), Messages.WaveformViewer_17,
						Messages.WaveformViewer_18)) {
					reloadDatabase();
				}
		});
		fileMonitor.removeFileChangeListener(this);
	}

	public void reloadDatabase() {
		Map<String, String> state = new HashMap<>();
		saveWaveformViewerState(state);
		waveformPane.getStreamList().clear();
		database =  dbFactory.getDatabase();
		database.addPropertyChangeListener(evt -> {
			if (IHierNode.WAVEFORMS.equals(evt.getPropertyName())) { //$NON-NLS-1$
				myParent.getDisplay().syncExec(() -> waveformPane.setMaxTime(database.getMaxTime()));
			}
		});
		if (!filesToLoad.isEmpty())
			loadDatabase(state, 0L);
	}

	/**
	 * Sets the part input.
	 *
	 * @param partInput the new part input
	 */
	@Inject
	@Optional
	public void setPartInput(@Named("input") List<String> partInput, @Named("config") String partConfig) {
		for(String s:partInput) {
			File file = new File(s);
			if(file.isFile() && "CURRENT".equals(file.getName()))
				file=file.getParentFile();
			if (file.exists()) 
				filesToLoad.add(file);
		}
		this.partConfig=partConfig;
		if (!filesToLoad.isEmpty())
			loadDatabase(persistedState);
	}

	/**
	 * Sets the focus.
	 */
	@Focus
	public void setFocus() {
		if(waveformPane!=null) waveformPane.getWaveformControl().setFocus();
	}

	/**
	 * Save state.
	 *
	 * @param part the part
	 */
	@PersistState
	public void saveState(MPart part) {
		// save changes
		Map<String, String> persistingState = part.getPersistedState();
		persistingState.put(DATABASE_FILE + "S", Integer.toString(filesToLoad.size())); //$NON-NLS-1$
		Integer index = 0;
		for (File file : filesToLoad) {
			persistingState.put(DATABASE_FILE + index, file.getAbsolutePath());
			index++;
		}
		saveWaveformViewerState(persistingState);
	}

	public void saveState(String fileName){
		Map<String, String> persistingState = new HashMap<>();
		persistingState.put(DATABASE_FILE + "S", Integer.toString(filesToLoad.size())); //$NON-NLS-1$
		Integer index = 0;
		for (File file : filesToLoad) {
			persistingState.put(DATABASE_FILE + index, file.getAbsolutePath());
			index++;
		}
		saveWaveformViewerState(persistingState);
		Properties props = new Properties();
		props.putAll(persistingState);
		
		try (FileOutputStream out = new FileOutputStream(fileName)) {
			props.store(out, "Written by SCViewer"); //$NON-NLS-1$
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadState(String fileName){
		//clear old streams before loading tab settings
		if(!waveformPane.getStreamList().isEmpty()) {
			waveformPane.getStreamList().clear();
			waveformPane.getStreamList().stream().forEach(e -> e.selected=false);
		}
		try (FileInputStream in = new FileInputStream(fileName)) {
			Properties props = new Properties();
			props.load(in);
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HashMap<String, String> propMap = new HashMap<>((Map) props);
			restoreWaveformViewerState(propMap);
		} catch(FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Save waveform viewer state.
	 *
	 * @param persistingState the persisted state
	 */
	protected void saveWaveformViewerState(Map<String, String> persistingState) {
		persistingState.put(SHOWN_WAVEFORM + "S", Integer.toString(waveformPane.getStreamList().size())); //$NON-NLS-1$
		Integer index = 0;
		for (TrackEntry trackEntry : waveformPane.getStreamList()) {
			persistingState.put(SHOWN_WAVEFORM + index, trackEntry.waveform.getFullName());
			persistingState.put(SHOWN_WAVEFORM + index + VALUE_DISPLAY, trackEntry.valueDisplay.toString());
			persistingState.put(SHOWN_WAVEFORM + index + WAVE_DISPLAY, trackEntry.waveDisplay.toString());
			persistingState.put(SHOWN_WAVEFORM + index + WAVEFORM_SELECTED, String.valueOf(trackEntry.selected).toUpperCase());
			index++;
		}
		List<ICursor> cursors = waveformPane.getCursorList();
		persistingState.put(SHOWN_CURSOR + "S", Integer.toString(cursors.size())); //$NON-NLS-1$
		index = 0;
		for (ICursor cursor : cursors) {
			persistingState.put(SHOWN_CURSOR + index, Long.toString(cursor.getTime()));
			index++;
		}
		persistingState.put(ZOOM_LEVEL, Integer.toString(waveformPane.getZoomLevel()));
		persistingState.put(BASE_LINE_TIME, Long.toString(waveformPane.getBaselineTime()));
		
		// get selected transaction	of a stream	
		ISelection selection = waveformPane.getSelection();
		if (!selection.isEmpty()) {
			List<Object> sel = getISelection(selection);
			if(sel.size()>1 && sel.get(0) instanceof ITx && sel.get(1) instanceof TrackEntry) {
				ITx tx = (ITx) sel.get(0);
				TrackEntry te = (TrackEntry) sel.get(1);
				// get transaction id
				persistingState.put(SELECTED_TX_ID, Long.toString(tx.getId()));
				//get TrackEntry name
				String name = te.waveform.getFullName();
				persistingState.put(SELECTED_TRACKENTRY_NAME, name);
			}
		}
	}

	protected List<Object> getISelection(ISelection selection){
	    List<Object> result = new LinkedList<> ();

	    if ( selection instanceof IStructuredSelection ) {
	        Iterator<?> i = ((IStructuredSelection)selection).iterator();
	        while (i.hasNext()){
	            Object o = i.next ();
	            if (o == null) {
	                continue;
	            }
	            result.add(o);
	        }
	    }
	    return result;
	}	
	
	/**
	 * Restore waveform viewer state.
	 *
	 * @param state the state
	 */
	protected void restoreWaveformViewerState(Map<String, String> state) {
		Integer waves = state.containsKey(SHOWN_WAVEFORM+"S") ? Integer.parseInt(state.get(SHOWN_WAVEFORM + "S")):0; //$NON-NLS-1$ //$NON-NLS-2$
		List<TrackEntry> trackEntries = new LinkedList<>();
		for (int i = 0; i < waves; i++) {
			IWaveform waveform = database.getStreamByName(state.get(SHOWN_WAVEFORM + i));
			if (waveform != null) {
				TrackEntry trackEntry = waveformPane.addWaveform(waveform, -1);
				//check if t is selected
				boolean isSelected = Boolean.parseBoolean(state.get(SHOWN_WAVEFORM + i + WAVEFORM_SELECTED));
				if(isSelected) {
					trackEntry.selected = true;
				} else {
					trackEntry.selected = false;
				}
				trackEntries.add(trackEntry);
				String v = state.get(SHOWN_WAVEFORM + i + VALUE_DISPLAY);
				if(v!=null)
					trackEntry.valueDisplay=ValueDisplay.valueOf(v);
				String s = state.get(SHOWN_WAVEFORM + i + WAVE_DISPLAY);
				if(s!=null)
					trackEntry.waveDisplay=WaveDisplay.valueOf(s);
			}
		}
		Integer cursorLength = state.containsKey(SHOWN_CURSOR+"S")?Integer.parseInt(state.get(SHOWN_CURSOR + "S")):0; //$NON-NLS-1$ //$NON-NLS-2$
		List<ICursor> cursors = waveformPane.getCursorList();
		if (cursorLength == cursors.size()) {
			for (int i = 0; i < cursorLength; i++) {
				Long time = Long.parseLong(state.get(SHOWN_CURSOR + i));
				cursors.get(i).setTime(time);
			}
		}
		if (state.containsKey(ZOOM_LEVEL)) {
			try {
				Integer scale = Integer.parseInt(state.get(ZOOM_LEVEL));
				waveformPane.setZoomLevel(scale);
			} catch (NumberFormatException e) {
			}
		}
		if (state.containsKey(BASE_LINE_TIME)) {
			try {
				Long scale = Long.parseLong(state.get(BASE_LINE_TIME));
				waveformPane.setBaselineTime(scale);
			} catch (NumberFormatException e) {
			}
		}
		if (state.containsKey(SELECTED_TX_ID) && state.containsKey(SELECTED_TRACKENTRY_NAME)) {
			try {
				Long txId = Long.parseLong(state.get(SELECTED_TX_ID));
				String trackentryName = state.get(SELECTED_TRACKENTRY_NAME);
				//get TrackEntry Object based on name and TX Object by id and put into selectionList
				trackEntries.stream().filter(e->trackentryName.equals(e.waveform.getFullName())).forEach(trackEntry ->
					trackEntry.waveform.getEvents().entrySet().stream()
					.map(e->e.events)
					.filter(Objects::nonNull)
					.forEach(entries-> 
						Arrays.stream(entries)
						.filter(e->e instanceof ITxEvent && txId.equals(((ITxEvent)e).getTransaction().getId()))
						.forEach(event ->
							waveformPane.setSelection(new StructuredSelection(
									new Object[] {((ITxEvent)event).getTransaction(), trackEntry}))
						)
					)
				);
			} catch (NumberFormatException e) {
			}
		}
		updateAll();
	}

	/**
	 * Update all status elements by posting respective events.
	 */
	private void updateAll() {
		eventBroker.post(ACTIVE_WAVEFORMVIEW, this);
		eventBroker.post(WaveStatusBarControl.ZOOM_LEVEL, zoomLevel[waveformPane.getZoomLevel()]);
		long cursor = waveformPane.getCursorTime();
		long marker = waveformPane.getMarkerTime(waveformPane.getSelectedMarkerId());
		eventBroker.post(WaveStatusBarControl.CURSOR_TIME, waveformPane.getScaledTime(cursor));
		eventBroker.post(WaveStatusBarControl.MARKER_TIME, waveformPane.getScaledTime(marker));
		eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getScaledTime(cursor - marker));
	}

	/**
	 * Gets the adds the waveform event.
	 *
	 * @param o the o
	 * @return the adds the waveform event
	 */
	@Inject
	@Optional
	public void getAddWaveformEvent(@UIEventTopic(WaveformViewer.ADD_WAVEFORM) Object o) {
		Object sel = o == null ? selectionService.getSelection() : o;
		if (sel instanceof IStructuredSelection)
			for (Object el : ((IStructuredSelection) sel).toArray()) {
				if (el instanceof IWaveform)
					addStreamToList((IWaveform) el, false);
			}
	}

	/**
	 * Ask if to load.
	 *
	 * @param txFile the tx file
	 * @return true, if successful
	 */
	protected boolean askIfToLoad(File txFile) {
		return txFile.exists() && MessageDialog.openQuestion(myParent.getDisplay().getActiveShell(), Messages.WaveformViewer_37,
				Messages.WaveformViewer_38 + txFile.getName() + Messages.WaveformViewer_39);
	}

	/**
	 * Rename file extension.
	 *
	 * @param source the source
	 * @param newExt the new ext
	 * @return the string
	 */
	protected static String renameFileExtension(String source, String newExt) {
		String target;
		String currentExt = getFileExtension(source);
		if (currentExt.equals("")) { //$NON-NLS-1$
			target = source + "." + newExt; //$NON-NLS-1$
		} else {
			target = source.replaceFirst(Pattern.quote("." + currentExt) + "$", Matcher.quoteReplacement("." + newExt)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return target;
	}

	/**
	 * Gets the file extension.
	 *
	 * @param f the f
	 * @return the file extension
	 */
	protected static String getFileExtension(String f) {
		String ext = ""; //$NON-NLS-1$
		int i = f.lastIndexOf('.');
		if (i > 0 && i < f.length() - 1) {
			ext = f.substring(i + 1);
		}
		return ext;
	}

	/**
	 * Gets the database.
	 *
	 * @return the database
	 */
	public IWaveformDb getDatabase() {
		return database;
	}

	/**
	 * Adds the stream to list.
	 *
	 * @param obj the obj
	 * @param insert the insert
	 */
	public void addStreamToList(IWaveform obj, boolean insert) {
		addStreamsToList(new IWaveform[] { obj }, insert);
	}

	/**
	 * Adds the streams to list.
	 *
	 * @param iWaveforms the i waveforms
	 * @param insert the insert
	 */
	public void addStreamsToList(IWaveform[] iWaveforms, boolean insert) {
		IStructuredSelection selection = (IStructuredSelection) waveformPane.getSelection();
		if (selection.size() == 0) {
			for (IWaveform waveform : iWaveforms)
				waveformPane.addWaveform(waveform, -1);
		} else {
			Object first = selection.getFirstElement();
			if(first instanceof ITx) {
				TrackEntry trackEntry = waveformPane.getEntryFor((ITx) first);
				if (insert) {
					int index = waveformPane.getStreamList().indexOf(trackEntry);
					for (IWaveform waveform : iWaveforms)
						waveformPane.addWaveform(waveform, index++);
				} else {
					for (IWaveform waveform : iWaveforms)
						waveformPane.addWaveform(waveform, -1);
				}
			} else if(first instanceof TrackEntry) {
				TrackEntry trackEntry = (TrackEntry) first;
				if (insert) {
					int index = waveformPane.getStreamList().indexOf(trackEntry);
					for (IWaveform waveform : iWaveforms)
						waveformPane.addWaveform(waveform, index++);
				} else {
					for (IWaveform waveform : iWaveforms)
						waveformPane.addWaveform(waveform, -1);
				}
			}

		}
		setFocus();
	}

	public void removeSelectedStreamsFromList() {
		waveformPane.deleteSelectedTracks();
	}

	public void removeSelectedStreamFromList() {
		waveformPane.deleteSelectedTracks();
	}
	/**
	 * Move selected.
	 *
	 * @param i the i
	 */
	public void moveSelected(int i) {
		waveformPane.moveSelectedTrack(i);
	}

	
	/**
	 * Move selection.
	 *
	 * @param direction the direction
	 */
	public void moveSelection(GotoDirection direction ) {
		moveSelection(direction, navigationRelationType); 
	}

	/**
	 * Move selection.
	 *
	 * @param direction the direction
	 * @param relationType the relation type
	 */
	public void moveSelection(GotoDirection direction, RelationType relationType) {
		waveformPane.moveSelection(direction, relationType);
	}

	/**
	 * Move cursor.
	 *
	 * @param direction the direction
	 */
	public void moveCursor(GotoDirection direction) {
		waveformPane.moveCursor(direction);
	}

	/**
	 * Sets the zoom level.
	 *
	 * @param level the new zoom level
	 */
	public void setZoomLevel(Integer level) {
		if (level < 0)
			level = 0;
		if (level > zoomLevel.length - 1)
			level = zoomLevel.length - 1;
		waveformPane.setZoomLevel(level);
		updateAll();
	}

	/**
	 * Sets the zoom fit.
	 */
	public void setZoomFit() {
		waveformPane.setZoomLevel(-1);
		updateAll();
	}

	/**
	 * Gets the zoom level.
	 *
	 * @return the zoom level
	 */
	public int getZoomLevel() {
		return waveformPane.getZoomLevel();
	}

	/**
	 * Gets the selection.
	 *
	 * @return the selection
	 */
	public ISelection getSelection() {
		if(waveformPane!=null)
			return waveformPane.getSelection();
		else
			return new StructuredSelection();
	}

	/**
	 * Sets the selection.
	 *
	 * @param structuredSelection the new selection
	 */
	public void setSelection(IStructuredSelection structuredSelection) {
		waveformPane.setSelection(structuredSelection, true);
	}

	/**
	 * Gets the scaled time.
	 *
	 * @param time the time
	 * @return the scaled time
	 */
	public String getScaledTime(Long time) {
		return waveformPane.getScaledTime(time);
	}

	/**
	 * Store design brower state.
	 *
	 * @param browserState the browser state
	 */
	public void storeDesignBrowerState(Object browserState) {
		this.browserState=browserState;
	}

	/**
	 * Retrieve design brower state.
	 *
	 * @return the object
	 */
	public Object retrieveDesignBrowerState() {
		return browserState;
	}

	/**
	 * Store transaction details settings
	 *
	 * @param detailsSettings the details settings
	 */
	public void storeDetailsSettings(Object detailsSettings) {
		this.detailsSettings=detailsSettings;
	}

	/**
	 * Retrieve design details settings.
	 *
	 * @return the details settings
	 */
	public Object retrieveDetailsSettings() {
		return detailsSettings;
	}

	/**
	 * Gets the all relation types.
	 *
	 * @return the all relation types
	 */
	public List<RelationType> getAllRelationTypes() {
		List<RelationType> res =new ArrayList<>();
		res.add(IWaveformView.NEXT_PREV_IN_STREAM);
		res.addAll(database.getAllRelationTypes());
		return res;
	}

	/**
	 * Gets the selection relation types.
	 *
	 * @return the selection relation types
	 */
	public List<RelationType> getSelectionRelationTypes() {
		List<RelationType> res =new ArrayList<>();
		res.add(IWaveformView.NEXT_PREV_IN_STREAM);
		ISelection selection = waveformPane.getSelection();
		if(selection instanceof IStructuredSelection && !selection.isEmpty()){
			IStructuredSelection sel=(IStructuredSelection) selection;
			if(sel.getFirstElement() instanceof ITx){
				ITx tx = (ITx) sel.getFirstElement();
				for(ITxRelation rel:tx.getIncomingRelations()){
					if(!res.contains(rel.getRelationType()))
						res.add(rel.getRelationType());
				}
				for(ITxRelation rel:tx.getOutgoingRelations()){
					if(!res.contains(rel.getRelationType()))
						res.add(rel.getRelationType());
				}
			}
		}
		return res;
	}

	/**
	 * Gets the relation type filter.
	 *
	 * @return the relation type filter
	 */
	public RelationType getRelationTypeFilter() {
		return navigationRelationType;
	}

	/**
	 * Sets the navigation relation type.
	 *
	 * @param relationName the new navigation relation type
	 */
	public void setNavigationRelationType(String relationName) {
		setNavigationRelationType(RelationTypeFactory.create(relationName));
	}

	/**
	 * Sets the navigation relation type.
	 *
	 * @param relationType the new navigation relation type
	 */
	public void setNavigationRelationType(RelationType relationType) {
		if(navigationRelationType!=relationType) waveformPane.setHighliteRelation(relationType);
		navigationRelationType=relationType;
	}
	
	public void update() {
		waveformPane.update();
	}

	/**
	 * add dispose listener
	 * 
	 * @param listener
	 */
	public void addDisposeListener (DisposeListener listener) {
		waveformPane.getControl().addDisposeListener(listener);
	}

	/**
	 * triggers included actions if widget is disposed
	 * 
	 * @param e
	 */
    public void widgetDisposed(DisposeEvent e) {
    	disposeListenerNumber -= 1;
    	if( disposeListenerNumber == 0) {  //if the last tab is closed, reset statusbar
			eventBroker.post(WaveStatusBarControl.ZOOM_LEVEL, null);
			eventBroker.post(WaveStatusBarControl.CURSOR_TIME, null);
			eventBroker.post(WaveStatusBarControl.MARKER_TIME, null);
			eventBroker.post(WaveStatusBarControl.MARKER_DIFF, null);
    	}
    }
    
	public void search(String propName, DataType type, String propValue) {
		transactionList.getControl().setSearchProps(propName, type, propValue);
	}
}