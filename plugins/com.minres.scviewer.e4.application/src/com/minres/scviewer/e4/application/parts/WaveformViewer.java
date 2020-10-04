/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
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
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.osgi.service.prefs.Preferences;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.TrackEntry.ValueDisplay;
import com.minres.scviewer.database.ui.TrackEntry.WaveDisplay;
import com.minres.scviewer.database.ui.swt.Constants;
import com.minres.scviewer.database.ui.swt.ToolTipContentProvider;
import com.minres.scviewer.database.ui.swt.ToolTipHelpTextProvider;
import com.minres.scviewer.database.ui.swt.WaveformViewFactory;
import com.minres.scviewer.database.ui.WaveformColors;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.internal.status.WaveStatusBarControl;
import com.minres.scviewer.e4.application.internal.ui.AbstractDesignBrowser;
import com.minres.scviewer.e4.application.internal.util.FileMonitor;
import com.minres.scviewer.e4.application.internal.util.IFileChangeListener;
import com.minres.scviewer.e4.application.internal.util.IModificationChecker;
import com.minres.scviewer.e4.application.preferences.PreferenceConstants;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;

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

	/** The number of active DisposeListeners */
	private static int disposeListenerNumber = 0;
	
	/** The factory. */
	WaveformViewFactory factory = new WaveformViewFactory();

	AbstractDesignBrowser browser = new AbstractDesignBrowser() {
		
		@Override
		protected void updateButtons() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void initializeButtonListeners() {
			// TODO Auto-generated method stub
			
		}
	};	
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
	
	private Boolean showHover;

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
		database.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("WAVEFORMS".equals(evt.getPropertyName())) { //$NON-NLS-1$
					myParent.getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							waveformPane.setMaxTime(database.getMaxTime());
						}
					});
				}
			}
		});
		parent.setLayout(new FillLayout(SWT.HORIZONTAL));
		SashForm sashFormTop = new SashForm(parent, SWT.NONE);
		
		Composite left = new Composite(sashFormTop, SWT.NONE);
		left.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		browser.createComposite(left);
		browser.setWaveformDb(database);
		
		Composite right = new Composite(sashFormTop, SWT.NONE);
		
		waveformPane = factory.createPanel(right);
		sashFormTop.setWeights(new int[] {25, 75});
		
		waveformPane.setMaxTime(0);
		setupColors();
		//set selection to empty selection when opening a new waveformPane
		selectionService.setSelection(new StructuredSelection());
		
		waveformPane.addPropertyChangeListener(IWaveformView.CURSOR_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Long time = (Long) evt.getNewValue();
				eventBroker.post(WaveStatusBarControl.CURSOR_TIME, waveformPane.getScaledTime(time));
				long marker = waveformPane.getMarkerTime(waveformPane.getSelectedMarkerId());
				eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getScaledTime(time - marker));

			}
		});
		waveformPane.addPropertyChangeListener(IWaveformView.MARKER_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Long time = (Long) evt.getNewValue();
				eventBroker.post(WaveStatusBarControl.MARKER_TIME, waveformPane.getScaledTime(time));
				long cursor = waveformPane.getCursorTime();
				eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getScaledTime(cursor - time));
			}
		});
		
		waveformPane.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					selectionService.setSelection(event.getSelection());
				}
			}
		});

		waveformPane.getWaveformControl().addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event e) {
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
					}
				} else if((e.stateMask&SWT.MOD2)!=0) { //Shift
					switch(e.keyCode) {
					case SWT.ARROW_LEFT:
						waveformPane.scrollHorizontal(-100);
						return;
					case SWT.ARROW_RIGHT:
						waveformPane.scrollHorizontal(100);
						return;
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
					case SWT.HOME:			return; //TODO: should be handled
					case SWT.END:			return; //TODO: should be handled
					}
				}
			}
		});
		
		zoomLevel = waveformPane.getZoomLevels();
		checkForUpdates = store.getBoolean(PreferenceConstants.DATABASE_RELOAD, true);
		filesToLoad = new ArrayList<File>();
		persistedState = part.getPersistedState();
		Integer files = persistedState.containsKey(DATABASE_FILE + "S") //$NON-NLS-1$
				? Integer.parseInt(persistedState.get(DATABASE_FILE + "S")) : 0; //$NON-NLS-1$
		for (int i = 0; i < files; i++) {
			filesToLoad.add(new File(persistedState.get(DATABASE_FILE + i)));
		}
		if (filesToLoad.size() > 0)
			loadDatabase(persistedState);
		eventBroker.post(WaveStatusBarControl.ZOOM_LEVEL, zoomLevel[waveformPane.getZoomLevel()]);
//		menuService.registerContextMenu(waveformPane.getNameControl(),
//				"com.minres.scviewer.e4.application.popupmenu.namecontext"); //$NON-NLS-1$
		menuService.registerContextMenu(waveformPane.getNameControl(),
				"com.minres.scviewer.e4.application.popupmenu.wavecontext"); //$NON-NLS-1$
		menuService.registerContextMenu(waveformPane.getValueControl(),
				"com.minres.scviewer.e4.application.popupmenu.wavecontext"); //$NON-NLS-1$
		menuService.registerContextMenu(waveformPane.getWaveformControl(),
				"com.minres.scviewer.e4.application.popupmenu.wavecontext"); //$NON-NLS-1$
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

		waveformPane.getWaveformControl().setData(Constants.HELP_PROVIDER_TAG, new ToolTipHelpTextProvider() {
			@Override
			public String getHelpText(Widget widget) {
				return "Waveform pane: press F2 to set the focus to the tooltip";
			}
		});
		waveformPane.getWaveformControl().setData(Constants.CONTENT_PROVIDER_TAG, new ToolTipContentProvider() {
			@Override
			public boolean createContent(Composite parent, Point pt) {
				if(!showHover) return false;
				List<Object> res = waveformPane.getElementsAt(pt);
				if(res.size()>0)
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
						parent.addPaintListener(new PaintListener() {
							@Override
							public void paintControl(PaintEvent e) {
								Rectangle area = parent.getClientArea();
								valueCol.setWidth(area.width - nameCol.getWidth());
							}
						});
						parent.addFocusListener(FocusListener.focusGainedAdapter(e -> {
							table.setFocus();
						}));
						return true;
					} else  if(res.get(0) instanceof TrackEntry) {
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
				return false;
			}
		});
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
		if (checkForUpdates) {
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
			setupColors();
		}
	}

	/**
	 * Setup colors.
	 */
	protected void setupColors() {
		Preferences defaultPrefs= store.parent().parent().node("/"+DefaultScope.SCOPE+"/"+PreferenceConstants.PREFERENCES_SCOPE);
		HashMap<WaveformColors, RGB> colorPref = new HashMap<>();
		for (WaveformColors c : WaveformColors.values()) {
			String key = c.name() + "_COLOR";
			String prefValue = store.get(key, defaultPrefs.get(key,  "")); //$NON-NLS-1$
			RGB rgb = StringConverter.asRGB(prefValue);
			colorPref.put(c, rgb);
		}
		waveformPane.setColors(colorPref);
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
		fileMonitor.removeFileChangeListener(this);
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
				} catch (OperationCanceledException | InterruptedException e) {
					throw new OperationCanceledException(Messages.WaveformViewer_14);
				}
				if (monitor.isCanceled())
					throw new OperationCanceledException(Messages.WaveformViewer_14);

				
				IStatus result = jobGroup.getResult();
				if( (!result.isMultiStatus() && result.getCode() != Status.OK_STATUS.getCode() ) ||
						(result.isMultiStatus() && result.getChildren().length > 0 && result.getChildren()[0].getCode() != Status.OK_STATUS.getCode() ) ){
					// kill editor and pop up warning for user
					sync.asyncExec(() -> {
						final Display display = myParent.getDisplay();
						MessageDialog.openWarning(display.getActiveShell(), "Error loading database", "Database cannot be loaded. Aborting...");
						ePartService.hidePart(myPart, true);
					});
				} else
					sync.asyncExec(()->{
						waveformPane.setMaxTime(database.getMaxTime());
						if (state != null)
							restoreWaveformViewerState(state);
						fileChecker = null;
						if (checkForUpdates)
							fileChecker = fileMonitor.addFileChangeListener(WaveformViewer.this, filesToLoad, FILE_CHECK_INTERVAL);
					});
				return result;
			}
		};
		job.setSystem(true);
		job.schedule(1000L); // let the UI initialize so that we have a progress monitor
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.e4.application.internal.util.IFileChangeListener#fileChanged(java.util.List)
	 */
	@Override
	public void fileChanged(List<File> file) {
		final Display display = myParent.getDisplay();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (MessageDialog.openQuestion(display.getActiveShell(), Messages.WaveformViewer_17,
						Messages.WaveformViewer_18)) {
					Map<String, String> state = new HashMap<>();
					saveWaveformViewerState(state);
					waveformPane.getStreamList().clear();
					database.clear();
					if (filesToLoad.size() > 0)
						loadDatabase(state);
				}
			}
		});
		fileMonitor.removeFileChangeListener(this);
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
		if(partConfig!=null) {
			this.partConfig=partConfig;
		}
		if (filesToLoad.size() > 0)
			loadDatabase(persistedState);
		if(partConfig.length()>0)
			loadState(partConfig);
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
		Map<String, String> persistedState = part.getPersistedState();
		persistedState.put(DATABASE_FILE + "S", Integer.toString(filesToLoad.size())); //$NON-NLS-1$
		Integer index = 0;
		for (File file : filesToLoad) {
			persistedState.put(DATABASE_FILE + index, file.getAbsolutePath());
			index++;
		}
		saveWaveformViewerState(persistedState);
	}

	public void saveState(String fileName){
		Map<String, String> persistedState = new HashMap<>();
		persistedState.put(DATABASE_FILE + "S", Integer.toString(filesToLoad.size())); //$NON-NLS-1$
		Integer index = 0;
		for (File file : filesToLoad) {
			persistedState.put(DATABASE_FILE + index, file.getAbsolutePath());
			index++;
		}
		saveWaveformViewerState(persistedState);
		Properties props = new Properties();
		props.putAll(persistedState);
		
		try {
			
				FileOutputStream out = new FileOutputStream(fileName);
                props.store(out, "Written by SCViewer"); //$NON-NLS-1$
			    out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadState(String fileName){
		Properties props = new Properties();
		try {
			//clear old streams before loading tab settings
			if(!waveformPane.getStreamList().isEmpty()) {
				waveformPane.getStreamList().clear();
				for (TrackEntry trackEntry : waveformPane.getStreamList()) {
					trackEntry.selected = false;
				}
			}
			FileInputStream in = new FileInputStream(fileName);
			props.load(in);
			in.close();
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HashMap<String, String> propMap = new HashMap<String, String>((Map) props);
			restoreWaveformViewerState(propMap);
		} catch(FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Save waveform viewer state.
	 *
	 * @param persistedState the persisted state
	 */
	protected void saveWaveformViewerState(Map<String, String> persistedState) {
		persistedState.put(SHOWN_WAVEFORM + "S", Integer.toString(waveformPane.getStreamList().size())); //$NON-NLS-1$
		Integer index = 0;
		for (TrackEntry trackEntry : waveformPane.getStreamList()) {
			persistedState.put(SHOWN_WAVEFORM + index, trackEntry.waveform.getFullName());
			persistedState.put(SHOWN_WAVEFORM + index + VALUE_DISPLAY, trackEntry.valueDisplay.toString());
			persistedState.put(SHOWN_WAVEFORM + index + WAVE_DISPLAY, trackEntry.waveDisplay.toString());
			persistedState.put(SHOWN_WAVEFORM + index + WAVEFORM_SELECTED, String.valueOf(trackEntry.selected).toUpperCase());
			index++;
		}
		List<ICursor> cursors = waveformPane.getCursorList();
		persistedState.put(SHOWN_CURSOR + "S", Integer.toString(cursors.size())); //$NON-NLS-1$
		index = 0;
		for (ICursor cursor : cursors) {
			persistedState.put(SHOWN_CURSOR + index, Long.toString(cursor.getTime()));
			index++;
		}
		persistedState.put(ZOOM_LEVEL, Integer.toString(waveformPane.getZoomLevel()));
		persistedState.put(BASE_LINE_TIME, Long.toString(waveformPane.getBaselineTime()));
		
		// get selected transaction	of a stream	
		ISelection selection = waveformPane.getSelection();
		if (!selection.isEmpty()) {
			List<Object> t = getISelection(selection);
			if(t.get(0) instanceof ITx) {
				ITx tx = (ITx) t.get(0);
				TrackEntry te = (TrackEntry) t.get(1);
				// get transaction id
				persistedState.put(SELECTED_TX_ID, Long.toString(tx.getId()));
				//get TrackEntry name
				String name = te.getStream().getFullName();
				persistedState.put(SELECTED_TRACKENTRY_NAME, name);
			}
		}
	}

	protected List<Object> getISelection(ISelection selection){
	    List<Object> result = new LinkedList<Object> ();

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
		List<TrackEntry> res = new LinkedList<>();
		for (int i = 0; i < waves; i++) {
			IWaveform waveform = database.getStreamByName(state.get(SHOWN_WAVEFORM + i));
			if (waveform != null) {
				TrackEntry t = new TrackEntry(waveform);
				//check if t is selected
				boolean isSelected = Boolean.valueOf(state.get(SHOWN_WAVEFORM + i + WAVEFORM_SELECTED));
				if(isSelected) {
					t.selected = true;
				} else {
					t.selected = false;
				}
				res.add(t);
				String v = state.get(SHOWN_WAVEFORM + i + VALUE_DISPLAY);
				if(v!=null)
					t.valueDisplay=ValueDisplay.valueOf(v);
				String s = state.get(SHOWN_WAVEFORM + i + WAVE_DISPLAY);
				if(s!=null)
					t.waveDisplay=WaveDisplay.valueOf(s);
			}
		}
		if (res.size() > 0)
			waveformPane.getStreamList().addAll(res);
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
				for(TrackEntry te : res) {
					if(te.waveform.getFullName().compareTo(trackentryName)==0) {
						boolean found = false;
						// TODO: find transaction by time? To avoid 3x for-loop
						for( List<ITxEvent> lev : te.getStream().getEvents().values() ) {
							if(lev == null) continue;
							for(ITxEvent itxe : lev) {
								if(itxe == null) continue;
								ITx itx = itxe.getTransaction();
								if(itx.getId() == txId) {
									found = true;
									ArrayList<Object> selectionList = new ArrayList<Object>();
									selectionList.add(te);
									selectionList.add(itx);
									waveformPane.setSelection(new StructuredSelection (selectionList));
									break;
								}
							}
							if(found) break;
						}
						break;
					}
				}
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
		if (txFile.exists() && MessageDialog.openQuestion(myParent.getDisplay().getActiveShell(), Messages.WaveformViewer_37,
				Messages.WaveformViewer_38 + txFile.getName() + Messages.WaveformViewer_39)) {
			return true;
		}
		return false;
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
	 * Gets the model.
	 *
	 * @return the model
	 */
	public IWaveformDb getModel() {
		return database;
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
		List<TrackEntry> streams = new LinkedList<>();
		for (IWaveform stream : iWaveforms)
			streams.add(new TrackEntry(stream));
		IStructuredSelection selection = (IStructuredSelection) waveformPane.getSelection();
		if (selection.size() == 0) {
			waveformPane.getStreamList().addAll(streams);
		} else {
			Object first = selection.getFirstElement();
			IWaveform stream = (first instanceof ITx) ? ((ITx) first).getStream() : (IWaveform) first;
			TrackEntry trackEntry = waveformPane.getEntryForStream(stream);
			if (insert) {
				int index = waveformPane.getStreamList().indexOf(trackEntry);
				waveformPane.getStreamList().addAll(index, streams);
			} else {
				waveformPane.getStreamList().addAll(streams);
			}

		}
		setFocus();
	}

	public void removeSelectedStreamsFromList() {
		waveformPane.deleteSelectedTracks();
	}

	public void removeStreamFromList(ISelection sel) {
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
		//System.out.println("setZoomLevel() - ZoomLevel: " + level);
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
		//actual max time of signal
		long maxTime = waveformPane.getMaxTime();
		
		//get area actually capable of displaying data, i.e. area of the receiver which is capable of displaying data
		Rectangle clientArea = myParent.getClientArea();
		long clientAreaWidth = clientArea.width;
				
    	boolean foundZoom=false;
		//try to find existing zoomlevel where scaleFactor*clientAreaWidth >= maxTime, if one is found set it as new zoomlevel
		for (int level=0; level<Constants.unitMultiplier.length*Constants.unitString.length; level++){
			long scaleFactor = (long) Math.pow(10, level/2);
		    if(level%2==1) scaleFactor*=3;
		    if(scaleFactor*clientAreaWidth >= maxTime) {
		    	setZoomLevel(level);
		    	foundZoom=true;
		    	break;
		    }
		}
		//if no zoom level is found, set biggest one available
		if(!foundZoom) setZoomLevel(Constants.unitMultiplier.length*Constants.unitString.length-1);
				
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
		setNavigationRelationType(RelationType.create(relationName));
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
}