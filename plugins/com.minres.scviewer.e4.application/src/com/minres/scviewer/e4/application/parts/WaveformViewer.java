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
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.log.Logger;
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
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.wb.swt.SWTResourceManager;
import org.osgi.service.prefs.BackingStoreException;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.EmptyWaveform;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbFactory;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.RelationTypeFactory;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxRelation;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.IWaveformviewEventListener;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformSelection;
import com.minres.scviewer.database.ui.ZoomKind;
import com.minres.scviewer.database.ui.swt.Constants;
import com.minres.scviewer.database.ui.swt.IToolTipContentProvider;
import com.minres.scviewer.database.ui.swt.IToolTipHelpTextProvider;
import com.minres.scviewer.database.ui.swt.WaveformViewFactory;
import com.minres.scviewer.e4.application.AppModelId;
import com.minres.scviewer.e4.application.internal.util.ViewerSettings;
import com.minres.scviewer.e4.application.internal.util.DatabaseSessionManager;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.internal.status.WaveStatusBarControl;
import com.minres.scviewer.e4.application.preferences.PreferenceConstants;

/**
 * The Class WaveformViewerPart.
 */
public class WaveformViewer implements IPreferenceChangeListener, DisposeListener {

	/** The Constant ACTIVE_WAVEFORMVIEW. */
	public static final String ACTIVE_WAVEFORMVIEW = "Active_Waveform_View"; //$NON-NLS-1$

	/** The Constant ADD_WAVEFORM. */
	public static final String ADD_WAVEFORM = "AddWaveform"; //$NON-NLS-1$

	/** The Constant ID. */
	public static final String ID = "com.minres.scviewer.ui.TxEditorPart"; //$NON-NLS-1$

	/** The Constant WAVE_ACTION_ID. */
	public static final String WAVE_ACTION_ID = "com.minres.scviewer.ui.action.AddToWave"; //$NON-NLS-1$

	private static final String MENU_CONTEXT = "com.minres.scviewer.e4.application.popupmenu.wavecontext"; //$NON-NLS-1$

	/** The number of active DisposeListeners */
	private int disposeListenerNumber = 0;

	/** The factory. */
	WaveformViewFactory factory = new WaveformViewFactory();

	DesignBrowser designBrowser = null;

	TransactionDetails detailsView = null;

	TransactionListView transactionList = null;

	/** The waveform pane. */
	private IWaveformView waveformPane;

	/** get UISynchronize injected as field */
	@Inject UISynchronize sync;

	@Inject @Optional private Logger logger;

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

	private DatabaseSessionManager sessions;

	private volatile boolean disposed;

	private boolean checkForUpdates;

	private IEclipsePreferences observedPreferences;

	private PartListener partListener;

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


	@Inject IWaveformDbFactory dbFactory;

	@Inject Composite parent;

	@Inject	ECommandService commandService;

	@Inject	EHandlerService handlerService;
	
	private boolean showHover;
	
	private SashForm topSash = null;

	private SashForm middleSash = null;

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
		sessions = new DatabaseSessionManager(dbFactory, task -> sync.asyncExec(task),
				db -> { if (!isViewerDisposed()) waveformPane.setMaxTime(db.getMaxTime()); },
				db -> { if (!isViewerDisposed() && confirmReload()) reloadDatabase(); }, 60000);
		parent.setLayout(new FillLayout(SWT.HORIZONTAL));

		IEclipseContext ctx = myPart.getContext();
		ctx.set(WaveformViewer.class, this);
		ctx.set(IWaveformDb.class, getDatabase());

		topSash = new SashForm(parent, SWT.BORDER | SWT.SMOOTH | SWT.HORIZONTAL);
		Composite left = new Composite(topSash, SWT.NONE);
		middleSash = new SashForm(topSash, SWT.BORDER | SWT.SMOOTH | SWT.VERTICAL);
		Composite right = new Composite(topSash, SWT.NONE);
		topSash.setWeights(new int[] {20, 60, 20});

		Composite middleTop = new Composite(middleSash, SWT.NONE);
		Composite middleBottom = new Composite(middleSash, SWT.NONE);
		middleSash.setWeights(new int[] {75, 25});

		ctx.set(Composite.class, left);
		designBrowser = ContextInjectionFactory.make(DesignBrowser.class, ctx);

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
			eventBroker.post(WaveStatusBarControl.CURSOR_TIME, waveformPane.getWaveformZoom().timeToString(time));
			long marker = waveformPane.getMarkerTime(waveformPane.getSelectedMarker());
			eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getWaveformZoom().timeToString(time - marker));
		});
		waveformPane.addPropertyChangeListener(IWaveformView.MARKER_PROPERTY, evt -> {
			Long time = (Long) evt.getNewValue();
			eventBroker.post(WaveStatusBarControl.MARKER_TIME, waveformPane.getWaveformZoom().timeToString(time));
			long cursor = waveformPane.getCursorTime();
			eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getWaveformZoom().timeToString(cursor - time));
		});

		waveformPane.addSelectionChangedListener(event -> {
			if (event.getSelection() instanceof IStructuredSelection selection) {
				transactionList.setSelection(selection);
				selectionService.setSelection(selection);
			}
		});

		waveformPane.getWaveformControl().addListener(SWT.KeyDown, e -> {
			if((e.stateMask&SWT.MOD3)!=0) { // Alt key
			} else if((e.stateMask&SWT.MOD1)!=0) { //Ctrl/Cmd
				switch(e.keyCode) {
				case '+':
				case SWT.KEYPAD_ADD:
					waveformPane.getWaveformZoom().zoom(ZoomKind.IN);
					return;
				case '-':
				case SWT.KEYPAD_SUBTRACT:
					waveformPane.getWaveformZoom().zoom(ZoomKind.OUT);
					return;
				case SWT.ARROW_UP:
					waveformPane.moveSelectedTrack(-1);
					return;
				case SWT.ARROW_DOWN:
					waveformPane.moveSelectedTrack(1);
					return;
				case 'a':
					selectAll();
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
				default:
					break;
				}
			}
		});

		checkForUpdates = store.getBoolean(PreferenceConstants.DATABASE_RELOAD, true);
		sessions.setReloadEnabled(checkForUpdates);
		filesToLoad = new ArrayList<>();
		persistedState = part.getPersistedState();
		try {
			filesToLoad.addAll(ViewerSettings.databaseFiles(persistedState));
		} catch (IllegalArgumentException e) {
			reportSettingsError("Cannot restore database files", e.getMessage());
		}
		if (!filesToLoad.isEmpty())
			loadDatabase(persistedState);
		menuService.registerContextMenu(waveformPane.getNameControl(), MENU_CONTEXT);
		menuService.registerContextMenu(waveformPane.getValueControl(),	MENU_CONTEXT);
		menuService.registerContextMenu(waveformPane.getWaveformControl(), MENU_CONTEXT);
		partListener = new PartListener() {
			@Override
			public void partActivated(MPart part) {
				if (part == myPart && !disposed) {
					sessions.checkFiles();
					updateAll();
				}
			}
		};
		ePartService.addPartListener(partListener);
		waveformPane.addDisposeListener(this);

		waveformPane.addEventListner(new IWaveformviewEventListener() {
			@Override
			public void onTrackEntryDoubleClickEvent(TrackEntry trackEntry) {
				ParameterizedCommand command = commandService.createCommand(AppModelId.COMMAND_COM_MINRES_SCVIEWER_E4_APPLICATION_COMMAND_WAVE_DOUBLE_CLICK);
				handlerService.executeHandler(command);
			}
		});
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
						final Font font = SWTResourceManager.getFont("Terminal", 10, SWT.NORMAL);

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
						final Font font = SWTResourceManager.getFont("Terminal", 10, SWT.NORMAL);
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
		showTxDetails(false);
	}

	@PreDestroy
	public void closeDatabase() {
		if (disposed) return;
		disposed = true;
		if (sessions != null) sessions.close();
		if (observedPreferences != null) {
			observedPreferences.removePreferenceChangeListener(this);
			observedPreferences = null;
		}
		if (partListener != null) {
			ePartService.removePartListener(partListener);
			partListener = null;
		}

	}
	
	@Inject
	@Optional
	public void reactOnPrefsChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE) IEclipsePreferences prefs) {
		if (disposed || observedPreferences == prefs) return;
		if (observedPreferences != null) observedPreferences.removePreferenceChangeListener(this);
		observedPreferences = prefs;
		prefs.addPreferenceChangeListener(this);

	}

	@Inject
	@Optional
	public void reactOnShowHoverChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE, value = PreferenceConstants.SHOW_HOVER) Boolean hover) {
		showHover=hover;
	}

	@Inject
	@Optional
	public void reactOnShowTxDetailsChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE, value = PreferenceConstants.SHOW_TX_DETAILS) Boolean show) {
		showTxDetails(show);
	}

	@Inject
	@Optional
	public void reactOnReloadDatabaseChange(@Preference(nodePath = PreferenceConstants.PREFERENCES_SCOPE, value = PreferenceConstants.DATABASE_RELOAD) Boolean enabled) {
		checkForUpdates = Boolean.TRUE.equals(enabled);
		if (!disposed && sessions != null) sync.asyncExec(() -> sessions.setReloadEnabled(checkForUpdates));
	}

	protected boolean isViewerDisposed() {
		return disposed || myParent == null || myParent.isDisposed();
	}

	protected boolean confirmReload() {
		return MessageDialog.openQuestion(myParent.getShell(), Messages.WaveformViewer_17, Messages.WaveformViewer_18);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
	 */
	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (!PreferenceConstants.DATABASE_RELOAD.equals(event.getKey()) && !PreferenceConstants.SHOW_HOVER.equals(event.getKey())) {
			sync.asyncExec(() -> {
				if (!disposed && waveformPane != null && !myParent.isDisposed())
					waveformPane.setStyleProvider(new WaveformStyleProvider(store));
			});
		}
	}

	protected void loadDatabase(final Map<String, String> state) {
		loadDatabase(state, 1000L);
	}

	protected void loadDatabase(final Map<String, String> state, long delay) {
		if (disposed) return;
		setSelection(null);
		waveformPane.getStreamList().clear();
		waveformPane.update();
		final Map<String, String> requestedState = state == null ? Map.of() : new HashMap<>(state);
		final String requestedConfig = partConfig;
		partConfig = "";
		IWaveformDb requested = sessions.load(filesToLoad, delay, result -> {
			IWaveformDb current = sessions.database();
			if (isViewerDisposed()) return;
			if (!result.isOK()) {
				MessageDialog.openWarning(myParent.getShell(), "Error loading database", result.getMessage());
				ePartService.hidePart(myPart, true);
				return;
			}
			waveformPane.setMaxTime(current.getMaxTime());
			if (!requestedState.isEmpty()) restoreWaveformViewerState(requestedState);
			if (isViewerDisposed() || !sessions.isCurrent(current)) return;
			if (requestedConfig != null && !requestedConfig.isEmpty()) loadState(requestedConfig);
			if (isViewerDisposed() || !sessions.isCurrent(current)) return;
			if (designBrowser != null) designBrowser.setWaveformDb(current);
		});
		myPart.getContext().set(IWaveformDb.class, requested);
	}

	public void reloadDatabase() {
		Map<String, String> state = new HashMap<>();
		saveWaveformViewerState(state);
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
		captureSettings().replaceState(part.getPersistedState());
	}

	public void saveState(String fileName) {
		try { captureSettings().write(Path.of(fileName)); }
		catch (IOException | IllegalArgumentException e) {
			reportSettingsError("Cannot save settings", fileName + ": " + e.getMessage());
		}
	}

	public void loadState(String fileName) {
		try { applySettings(ViewerSettings.read(Path.of(fileName))); }
		catch (IOException | IllegalArgumentException e) {
			reportSettingsError("Cannot load settings", fileName + ": " + e.getMessage());
		}
	}

	protected void reportSettingsError(String title, String message) {
		if (logger != null) logger.error(title + ": " + message);
		if (!disposed && myParent != null && !myParent.isDisposed())
			MessageDialog.openError(myParent.getShell(), title, message);
	}

	/**
	 * Save waveform viewer state.
	 *
	 * @param persistingState the persisted state
	 */
	protected void saveWaveformViewerState(Map<String, String> state) {
		captureSettings().replaceState(state);
	}

	private static void captureTracks(List<TrackEntry> entries, Integer parent, List<ViewerSettings.Track> result) {
		for (TrackEntry entry : entries) {
			int index = result.size();
			result.add(new ViewerSettings.Track(entry.waveform.getFullName(), entry.waveform.getType(),
					entry.valueDisplay, entry.waveDisplay, entry.selected, parent, entry.isGroup(),
					entry.hierState == TrackEntry.HierState.OPENED));
			if (entry.isGroup()) captureTracks(entry.waveforms, index, result);
		}
	}

	private ViewerSettings captureSettings() {
		List<ViewerSettings.Track> tracks = new ArrayList<>();
		captureTracks(waveformPane.getStreamList(), null, tracks);
		WaveformSelection selection = WaveformSelection.from(waveformPane.getSelection());
		boolean selectedTransaction = selection.transaction() != null && selection.track() != null;
		return new ViewerSettings(tracks, waveformPane.getCursorList().stream().map(ICursor::getTime).toList(),
				waveformPane.getWaveformZoom().getScale(), waveformPane.getWaveformZoom().getMinVisibleTime(),
				selectedTransaction ? selection.transaction().getId() : null,
				selectedTransaction ? selection.track().waveform.getFullName() : null,
				store.getBoolean(PreferenceConstants.SHOW_TX_DETAILS, false), List.copyOf(filesToLoad));
	}

	/**
	 * Restore waveform viewer state.
	 *
	 * @param state the state
	 */
	protected void restoreWaveformViewerState(Map<String, String> state) {
		try {
			applySettings(ViewerSettings.parse(state));
		} catch (IllegalArgumentException e) {
			reportSettingsError("Cannot restore settings", e.getMessage());
		}
	}

	private record ResolvedTrack(IWaveform waveform, ViewerSettings.Track settings) {}

	private void applySettings(ViewerSettings settings) {
		// Resolve all references before replacing the current layout.
		List<ResolvedTrack> resolved = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		ITx selectedTransaction = null;
		IWaveform selectedWaveform = null;
		for (ViewerSettings.Track track : settings.tracks()) {
			IWaveform waveform = track.type() == WaveformType.EMPTY ? new EmptyWaveform(track.name())
					: ViewerSettings.resolveWaveform(getDatabase(), track.name());
			if (waveform == null) {
				missing.add(track.name());
				continue;
			}
			resolved.add(new ResolvedTrack(waveform, track));
			if (settings.transactionId() != null && waveform.getType().isTransaction()
					&& track.name().equals(settings.transactionTrack())) {
				selectedTransaction = ViewerSettings.findTransaction(waveform, settings.transactionId());
				selectedWaveform = waveform;
			}
		}
		waveformPane.setSelection(StructuredSelection.EMPTY);
		waveformPane.getStreamList().clear();
		List<TrackEntry> selectedTracks = new ArrayList<>();
		Map<ViewerSettings.Track, TrackEntry> entries = new java.util.IdentityHashMap<>();
		TrackEntry transactionTrack = null;
		for (ResolvedTrack track : resolved) {
			TrackEntry entry = waveformPane.addWaveform(track.waveform(), -1);
			entry.valueDisplay = track.settings().valueDisplay();
			entry.waveDisplay = track.settings().waveDisplay();
			if (track.settings().group()) entry.hierState = track.settings().expanded()
					? TrackEntry.HierState.OPENED : TrackEntry.HierState.CLOSED;
			entries.put(track.settings(), entry);
			if (track.settings().selected()) selectedTracks.add(entry);
			if (track.waveform() == selectedWaveform) transactionTrack = entry;
		}
		for (ViewerSettings.Track track : settings.tracks()) {
			if (track.parent() == null) continue;
			TrackEntry child = entries.get(track);
			TrackEntry parent = entries.get(settings.tracks().get(track.parent()));
			if (child != null && parent != null) {
				waveformPane.getStreamList().remove(child);
				parent.waveforms.add(child);
			}
		}
		List<TrackEntry> visible = TrackEntry.visibleTracks(waveformPane.getStreamList());
		selectedTracks.retainAll(visible);
		if (!visible.contains(transactionTrack)) transactionTrack = null;
		List<ICursor> cursors = waveformPane.getCursorList();
		if (settings.cursors().size() == cursors.size()) {
			for (int i = 0; i < cursors.size(); i++) cursors.get(i).setTime(settings.cursors().get(i));
		}
		if (settings.scale() != null) waveformPane.getWaveformZoom().setScale(settings.scale());
		if (settings.baseline() != null) waveformPane.getWaveformZoom().setMinVisibleTime(settings.baseline());
		if (settings.detailsShown() != null) showTxDetails(settings.detailsShown());
		waveformPane.update();
		if (selectedTransaction != null && transactionTrack != null)
			waveformPane.setSelection(WaveformSelection.of(selectedTransaction, transactionTrack));
		else waveformPane.setSelection(new StructuredSelection(selectedTracks));
		updateAll();
		if (!missing.isEmpty() && !disposed && !myParent.isDisposed())
			MessageDialog.openWarning(myParent.getShell(), "Missing waveforms",
					"These waveforms are absent from the current database and were skipped:\n" + String.join("\n", missing));
	}

	/**
	 * Update all status elements by posting respective events.
	 */
	private void updateAll() {
		eventBroker.post(ACTIVE_WAVEFORMVIEW, this);
		long cursor = waveformPane.getCursorTime();
		long marker = waveformPane.getMarkerTime(waveformPane.getSelectedMarker());
		eventBroker.post(WaveStatusBarControl.CURSOR_TIME, waveformPane.getWaveformZoom().timeToString(cursor));
		eventBroker.post(WaveStatusBarControl.MARKER_TIME, waveformPane.getWaveformZoom().timeToString(marker));
		eventBroker.post(WaveStatusBarControl.MARKER_DIFF, waveformPane.getWaveformZoom().timeToString(cursor - marker));
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
				NLS.bind(Messages.WaveformViewer_38,  txFile.getName()));
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
		return sessions == null ? null : sessions.database();
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
			@SuppressWarnings("unchecked")
			java.util.Optional<TrackEntry> o = selection.toList().stream().filter(e -> e instanceof TrackEntry).findFirst();
			if(o.isPresent()) {
				TrackEntry trackEntry = o.get();
				List<TrackEntry> siblings = TrackEntry.siblingsOf(waveformPane.getStreamList(), trackEntry);
				if (siblings != null) {
					int index = insert ? siblings.indexOf(trackEntry) : siblings.indexOf(trackEntry) + 1;
					for (IWaveform waveform : iWaveforms) {
						TrackEntry added = waveformPane.addWaveform(waveform, -1);
						waveformPane.getStreamList().remove(added);
						siblings.add(index++, added);
					}
					waveformPane.update();
				}

			}
		}
		showTxDetails(waveformPane.getStreamList().stream().filter(t -> t.waveform.getType().isTransaction()).findFirst().isPresent());
		setFocus();
	}

	public void removeSelectedStreamsFromList() {
		waveformPane.deleteSelectedTracks();
		showTxDetails(waveformPane.getStreamList().stream().filter(t -> t.waveform.getType().isTransaction()).findFirst().isPresent());
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
	 * Execute the zoom according to kind.
	 * 
	 * @param kind the type of zoom to execute
	 */
	public void zoom(ZoomKind kind) {
		waveformPane.getWaveformZoom().zoom(kind);
		updateAll();
	}

	/**
	 * Execute the zoom kind.
	 * 
	 * @param direction the direction of the pan (-1, 0, 1)
	 */
	public void pan(int direction) {
		switch(direction) {
		case -1: 
			waveformPane.scrollHorizontal(-10);
			return;
		case 1:
			waveformPane.scrollHorizontal(10);
			return;
		case 0:
			waveformPane.scrollTo(IWaveformView.CURSOR_POS);
			return;
		}
		updateAll();
	}

	public boolean canGroupSelectedTracks() {
		return waveformPane != null && TrackEntry.groupingSiblings(waveformPane.getStreamList(),
				WaveformSelection.from(getSelection()).tracks()) != null;
	}

	public boolean canUngroupSelectedTracks() {
		return waveformPane != null && WaveformSelection.from(getSelection()).tracks().stream()
				.anyMatch(track -> track.isGroup() && TrackEntry.siblingsOf(waveformPane.getStreamList(), track) != null);
	}

	public void groupSelectedTracks(String name) {
		if (canGroupSelectedTracks() && name != null && !name.isBlank()) waveformPane.groupSelectedTracks(name);
	}

	public void ungroupSelectedTracks() {
		if (waveformPane == null) return;
		for (TrackEntry track : WaveformSelection.from(getSelection()).tracks()) {
			if (track.isGroup()) waveformPane.ungroupTracks(track);
		}
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
		return waveformPane.getWaveformZoom().timeToString(time);
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
		res.addAll(getDatabase().getAllRelationTypes());
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
		ITx transaction = WaveformSelection.from(waveformPane.getSelection()).transaction();
		if (transaction != null) {
			for (ITxRelation relation : transaction.getIncomingRelations())
				if (!res.contains(relation.getRelationType())) res.add(relation.getRelationType());
			for (ITxRelation relation : transaction.getOutgoingRelations())
				if (!res.contains(relation.getRelationType())) res.add(relation.getRelationType());
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
		myParent.addDisposeListener(listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		if (myParent != null && !myParent.isDisposed())
			myParent.removeDisposeListener(listener);
	}

	/**
	 * triggers included actions if widget is disposed
	 * 
	 * @param e
	 */
	public void widgetDisposed(DisposeEvent e) {
		closeDatabase();
		disposeListenerNumber -= 1;
		if( disposeListenerNumber == 0) {  //if the last tab is closed, reset statusbar
			eventBroker.post(WaveStatusBarControl.CURSOR_TIME, null);
			eventBroker.post(WaveStatusBarControl.MARKER_TIME, null);
			eventBroker.post(WaveStatusBarControl.MARKER_DIFF, null);
		}
	}

	public DesignBrowser getDesignBrowser() {
		return designBrowser;
	}

	public void focusTransactionSearch() {
		if (disposed || transactionList == null) return;
		showTxDetails(true);
		transactionList.getControl().focusSearch();
	}

	public void search(String propName, DataType type, String propValue) {
		transactionList.getControl().setSearchProps(propName, type, propValue);
	}

	public void showTxDetails(boolean show) {
		if(middleSash==null || topSash==null || middleSash.isDisposed() || topSash.isDisposed())
			return;
		if(show) {
			middleSash.setWeights(new int[] {75, 25});
			topSash.setWeights(new int[] {20, 60, 20});
		} else {
			middleSash.setWeights(new int[] {100, 0});
			topSash.setWeights(new int[] {20, 80, 0});

		}
		detailsView.getControl().setVisible(show);
		transactionList.getControl().setVisible(show);
		parent.requestLayout();
		if(store.getBoolean(PreferenceConstants.SHOW_TX_DETAILS, false) != show) {
			store.putBoolean(PreferenceConstants.SHOW_TX_DETAILS, show);
			try {
				store.flush();
			} catch (BackingStoreException e) {}
		}
	}

	public void selectAll() {
		List<TrackEntry> entries = waveformPane.getStreamList();
		ISelection sel = new StructuredSelection(entries);
		waveformPane.setSelection(sel);
		designBrowser.selectAllWaveforms();
	}
}
