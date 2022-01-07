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
package com.minres.scviewer.database.ui.swt.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.collect.Lists;
import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.DoubleVal;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.ITxRelation;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.IWaveformStyleProvider;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.IWaveformZoom;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.swt.internal.slider.ZoomBar;

public class WaveformView implements IWaveformView {

	private ListenerList<ISelectionChangedListener> selectionChangedListeners = new ListenerList<>();

	private PropertyChangeSupport pcs;

	private ITx currentTxSelection;

	private ArrayList<TrackEntry> currentWaveformSelection = new ArrayList<>();

	private ScrolledComposite nameListScrolled;

	private ScrolledComposite valueListScrolled;

	private Control namePaneHeader;

	private final Canvas nameList;

	private final Canvas valueList;

	final WaveformCanvas waveformCanvas;

	final ToolTipHandler toolTipHandler;

	private boolean revealSelected = false;

	private Composite top;

	protected ObservableList<TrackEntry> streams;

	int selectedMarker = 0;

	private int tracksVerticalHeight;

	private TreeMap<Integer, TrackEntry> trackVerticalOffset;

	private IWaveformStyleProvider styleProvider;

	protected TrackEntry lastClickedEntry;

	protected MouseListener nameValueMouseListener = new MouseAdapter() {

		@Override
		public void mouseDown(MouseEvent e) {
			if (e.button == 1) {
				Entry<Integer, TrackEntry> entry = trackVerticalOffset.floorEntry(e.y);
				if (entry != null)
					entry.getValue().selected = true;
			} else if (e.button == 3) {
				Menu topMenu = top.getMenu();
				if (topMenu != null)
					topMenu.setVisible(true);
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (e.button == 1) {
				Entry<Integer, TrackEntry> entry = trackVerticalOffset.floorEntry(e.y);
				if (entry != null) {
					if ((e.stateMask & SWT.SHIFT) != 0) {
						if (lastClickedEntry.selected) {
							int firstIdx = streams.indexOf(lastClickedEntry);
							int lastIdx = streams.indexOf(entry.getValue());
							List<TrackEntry> res = firstIdx > lastIdx ? streams.subList(lastIdx, firstIdx + 1)
									: streams.subList(firstIdx, lastIdx + 1);
							setSelection(new StructuredSelection(res), (e.stateMask & SWT.CTRL) != 0, false);
						} else
							setSelection(new StructuredSelection(entry.getValue()), (e.stateMask & SWT.CTRL) != 0, false);
					} else {
						setSelection(new StructuredSelection(entry.getValue()), (e.stateMask & SWT.CTRL) != 0, false);
					}
					lastClickedEntry = entry.getValue();
				}
			}
		}
	};

	class WaveformMouseListener implements PaintListener, Listener {
		Point start;
		Point end;
		List<Object> initialSelected;
		boolean down = false;

		@Override
		public void paintControl(PaintEvent e) {
			if (down) {
				GC gc = e.gc;
				gc.setAlpha(128);
				int minX = Math.min(start.x, end.x);
				int width = Math.max(start.x, end.x) - minX;
				int yTop = waveformCanvas.getRulerHeight();
				int yBottom = waveformCanvas.getSize().y;
				gc.fillRectangle(minX, yTop, width, yBottom);
			}
		}

		private void mouseUp(MouseEvent e) {
			down = false;
			if (start == null)
				return;
			if ((e.stateMask & SWT.MODIFIER_MASK & ~(SWT.SHIFT | SWT.CTRL)) != 0)
				return; // don't react on modifier except shift and control
			boolean isCtrl = (e.stateMask & SWT.CTRL)!=0;
			boolean isShift = (e.stateMask & SWT.SHIFT)!=0;
			if (e.button == 1) {
				if (Math.abs(e.x - start.x) > 3) { // was drag event
					asyncUpdate(e.widget);
					long startTime = waveformCanvas.getTimeForOffset(start.x);
					long endTime = waveformCanvas.getTimeForOffset(end.x);
					if(startTime<endTime) {
						waveformCanvas.setVisibleRange(startTime, endTime);
					} else if(start.x!=end.x){
						long targetTimeRange = startTime-endTime;
						long currentTimeRange = waveformCanvas.getMaxVisibleTime() - waveformCanvas.getMinVisibleTime();
						long factor = currentTimeRange/targetTimeRange *waveformCanvas.getScale();
						waveformCanvas.setScalingFactor(factor, (startTime+endTime)/2);
												
					}
				} else if( isShift) { // set marker (button 1 and shift)
					setMarkerTime(selectedMarker, snapOffsetToEvent(start));
				} else if(isCtrl) {	// set cursor (button 1 and ctrl)
					setCursorTime(snapOffsetToEvent(start));
				} else { // set cursor (button 1 only)
					if (Math.abs(e.y - start.y) < 3) {
						// first set cursor time
						setCursorTime(snapOffsetToEvent(start));
						// then set selection and reveal
						setSelection(new StructuredSelection(initialSelected));
					}
				}
			} else if (e.button == 2) { // set marker (button 2)
				setMarkerTime(selectedMarker, snapOffsetToEvent(start));
			}
			asyncUpdate(e.widget);
		}

		protected long snapOffsetToEvent(Point p) {
			long time = waveformCanvas.getTimeForOffset(p.x);
			long scaling = 5 * waveformCanvas.getScale();
			for (Object o : waveformCanvas.getElementsAt(p)) {
				EventEntry floorEntry = null;
				EventEntry ceilEntry = null;
				if (o instanceof TrackEntry) {
					TrackEntry entry = (TrackEntry) o;
					IEventList map = entry.waveform.getEvents();
					floorEntry = map.floorEntry(time);
					ceilEntry = map.ceilingEntry(time);
				} else if (o instanceof ITx) {
					IEventList map = ((ITx) o).getStream().getEvents();
					floorEntry = map.floorEntry(time);
					ceilEntry = map.ceilingEntry(time);
				}
				if (floorEntry != null && time - floorEntry.timestamp > scaling)
					floorEntry = null;
				if (ceilEntry != null && ceilEntry.timestamp - time > scaling)
					ceilEntry = null;
				if (ceilEntry == null && floorEntry != null) {
					time = floorEntry.timestamp;
				} else if (ceilEntry != null && floorEntry == null) {
					time = ceilEntry.timestamp;
				} else if (ceilEntry != null && floorEntry != null) {
					time = time - floorEntry.timestamp < ceilEntry.timestamp - time ? floorEntry.timestamp
							: ceilEntry.timestamp;
				}
			}
			return time;
		}

		@Override
		public void handleEvent(Event e) {
			switch (e.type) {
			case SWT.MouseWheel:
				if((e.stateMask & SWT.CTRL) != 0) {
					if(e.count<0) // up scroll
						waveformCanvas.setScale(waveformCanvas.getScale()*11/10);
					else // down scroll
						waveformCanvas.setScale(waveformCanvas.getScale()*10/11);
					e.doit=false;
				} else if((e.stateMask & SWT.SHIFT) != 0) {
					long upper = waveformCanvas.getMaxVisibleTime();
					long lower = waveformCanvas.getMinVisibleTime();
					long duration = upper-lower;
					if(e.count<0) { // up scroll
						long newLower = Math.min(waveformCanvas.getMaxTime()-duration, lower+duration/10);
						waveformCanvas.setMinVisibleTime(newLower);
					} else {// down scroll
						long newLower = Math.max(0, lower-duration/10);
						waveformCanvas.setMinVisibleTime(newLower);
					}
					e.doit=false;
				}
				break;
			case SWT.MouseDown:
				start = new Point(e.x, e.y);
				end = new Point(e.x, e.y);
				if ((e.stateMask & SWT.MODIFIER_MASK) != 0)
					return; // don't react on modifier
				if (e.button == 1) {
					down = true;
					initialSelected = waveformCanvas.getElementsAt(start);
				} else if (e.button == 3) {
					Menu topMenu = top.getMenu();
					if (topMenu != null)
						topMenu.setVisible(true);
				}
				break;
			case SWT.MouseUp:
				mouseUp(new MouseEvent(e));
				break;
			case SWT.MouseMove:
				if (down) {
					end = new Point(e.x, e.y);
					asyncUpdate(e.widget);
				}
				break;
			default:
				break;
			}

		}

	}

	protected WaveformMouseListener waveformMouseListener = new WaveformMouseListener();

	public WaveformView(Composite parent, IWaveformStyleProvider styleProvider) {
		this.styleProvider = styleProvider;

		pcs = new PropertyChangeSupport(this);

		trackVerticalOffset = new TreeMap<>();
		tracksVerticalHeight = 0;

		streams = new ObservableList<>();
		streams.addPropertyChangeListener("content", this);

		top = parent;
		top.setLayout(new FillLayout(SWT.HORIZONTAL));

		SashForm topSash = new SashForm(top, SWT.SMOOTH);
		topSash.setBackground(topSash.getDisplay().getSystemColor(SWT.COLOR_GRAY));

		Composite namePane = new Composite(topSash, SWT.NONE);
		Composite rightPane = new Composite(topSash, SWT.NONE);
		rightPane.setLayout(new FillLayout(SWT.HORIZONTAL));

		SashForm rightSash = new SashForm(rightPane, SWT.SMOOTH);
		rightSash.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));

		Composite valuePane = new Composite(rightSash, SWT.NONE);
		
		Composite waveformPane = new Composite(rightSash, SWT.NONE);
		GridLayout gl_waveformPane = new GridLayout(1, false);
		gl_waveformPane.verticalSpacing = 0;
		gl_waveformPane.marginWidth = 0;
		gl_waveformPane.marginHeight = 0;
		waveformPane.setLayout(gl_waveformPane);
		
		waveformCanvas = new WaveformCanvas(waveformPane, SWT.NONE | SWT.V_SCROLL /*| SWT.H_SCROLL*/, styleProvider, new ZoomBar.IProvider() {
			
			@Override
			public ZoomBar getScrollBar() {
				ZoomBar timeSliderPane = new ZoomBar(waveformPane, SWT.NONE);
				GridData gd_timeSliderPane = new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1);
				timeSliderPane.setLayoutData(gd_timeSliderPane);
				return timeSliderPane;
			}
		});
		waveformCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		// create the name pane
		createTextPane(namePane, "Name");

		namePaneHeader = namePane.getChildren()[0];
		namePane.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));

		nameListScrolled = new ScrolledComposite(namePane, SWT.H_SCROLL | SWT.V_SCROLL);
		nameListScrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		nameListScrolled.setExpandHorizontal(true);
		nameListScrolled.setExpandVertical(true);
		nameListScrolled.setAlwaysShowScrollBars(true);
		nameListScrolled.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				nameListScrolled.getVerticalBar().setVisible(false);

			}
		});
		nameList = new Canvas(nameListScrolled, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				Rectangle bounds = super.getClientArea();
				return new Point(bounds.width, bounds.height);
			}
		};
		nameList.addListener(SWT.Paint, (Event event) -> {
			if (!trackVerticalOffset.isEmpty()) {
				GC gc = event.gc;
				Rectangle rect = ((Canvas) event.widget).getClientArea();
				paintNames(gc, rect);
			}
		});
		nameList.addMouseListener(nameValueMouseListener);
		nameListScrolled.setContent(nameList);

		createTextPane(valuePane, "Value");

		valuePane.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		valueListScrolled = new ScrolledComposite(valuePane, SWT.H_SCROLL | SWT.V_SCROLL);
		valueListScrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		valueListScrolled.setExpandHorizontal(true);
		valueListScrolled.setExpandVertical(true);
		valueListScrolled.setAlwaysShowScrollBars(true);
		valueListScrolled.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				valueListScrolled.getVerticalBar().setVisible(false);

			}
		});
		valueList = new Canvas(valueListScrolled, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				Rectangle bounds = super.getClientArea();
				return new Point(bounds.width, bounds.height);
			}
		};
		valueList.addListener(SWT.Paint, (Event event) -> {
			if (!trackVerticalOffset.isEmpty()) {
				GC gc = event.gc;
				Rectangle rect = ((Canvas) event.widget).getClientArea();
				paintValues(gc, rect);
			}
		});
		valueList.addMouseListener(nameValueMouseListener);
		valueListScrolled.setContent(valueList);

		waveformCanvas.setMaxTime(1);
		waveformCanvas.addPaintListener(waveformMouseListener);
		waveformCanvas.addListener(SWT.MouseDown, waveformMouseListener);
		waveformCanvas.addListener(SWT.MouseUp, waveformMouseListener);
		waveformCanvas.addListener(SWT.MouseMove, waveformMouseListener);
		waveformCanvas.addListener(SWT.MouseWheel, waveformMouseListener);

		nameListScrolled.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int y = ((ScrollBar) e.widget).getSelection();
				Point v = valueListScrolled.getOrigin();
				valueListScrolled.setOrigin(v.x, y);
				Point t = waveformCanvas.getOrigin();
				waveformCanvas.setOrigin(t.x, -y);
			}
		});
		valueListScrolled.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int y = ((ScrollBar) e.widget).getSelection();
				nameListScrolled.setOrigin(nameListScrolled.getOrigin().x, y);
				waveformCanvas.setOrigin(waveformCanvas.getOrigin().x, -y);
			}
		});
		waveformCanvas.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int y = waveformCanvas.getVerticalBar().getSelection();
				nameListScrolled.setOrigin(nameListScrolled.getOrigin().x, y);
				valueListScrolled.setOrigin(valueListScrolled.getOrigin().x, y);
			}
		});
		topSash.setWeights(new int[] { 25, 75 });
		rightSash.setWeights(new int[] { 10, 90 });

		createStreamDragSource(nameList);
		createStreamDragSource(valueList);
		createStreamDropTarget(nameList);
		createStreamDropTarget(valueList);
		createWaveformDragSource(waveformCanvas);
		createWaveformDropTarget(waveformCanvas);

		toolTipHandler = new ToolTipHandler(parent.getShell());
		toolTipHandler.activateHoverHelp(waveformCanvas);
	    // This is the filter that prevents the default handling of mouse wheel in waveformCanvas
	    getControl().getDisplay().addFilter(SWT.MouseWheel, new Listener() {
	        @Override
	        public void handleEvent(Event e) {
	            // Check if it's the correct widget
	            if(e.widget.equals(waveformCanvas) && (e.stateMask & SWT.CTRL) != 0) {
	            	waveformMouseListener.handleEvent(e);
	                e.doit = false;
	            }
	        }
	    });
	}

	private void createTextPane(Composite namePane, String text) {
		GridLayout glNamePane = new GridLayout(1, false);
		glNamePane.verticalSpacing = 0;
		glNamePane.marginWidth = 0;
		glNamePane.horizontalSpacing = 0;
		glNamePane.marginHeight = 0;
		namePane.setLayout(glNamePane);

		CLabel nameLabel = new CLabel(namePane, SWT.NONE);
		GridData gdNameLabel = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		gdNameLabel.heightHint = waveformCanvas.getRulerHeight() - 2;
		nameLabel.setLayoutData(gdNameLabel);
		nameLabel.setText(text);

		Label nameSep = new Label(namePane, SWT.SEPARATOR | SWT.HORIZONTAL);
		nameSep.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		nameSep.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		GridData gdNameSep = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gdNameSep.heightHint = 2;
		nameSep.setLayoutData(gdNameSep);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if ("size".equals(pce.getPropertyName()) || "content".equals(pce.getPropertyName())) {
			if (revealSelected) {
				waveformCanvas.getDisplay().asyncExec(() -> {
					update();
					currentWaveformSelection.stream().forEach(e -> waveformCanvas.reveal(e.waveform));
					valueList.redraw();
					nameList.redraw();
				});
				revealSelected = false;
			} else
				waveformCanvas.getDisplay().asyncExec(WaveformView.this::update);
		}
	}

	public void update() {
		tracksVerticalHeight = 0;
		int nameMaxWidth = 0;
		IWaveformPainter painter = null;
		trackVerticalOffset.clear();
		waveformCanvas.clearAllWaveformPainter(false);
		boolean even = true;
		TextLayout tl = new TextLayout(waveformCanvas.getDisplay());
		tl.setFont(styleProvider.getNameFont());
		for (TrackEntry streamEntry : streams) {
			streamEntry.height = styleProvider.getTrackHeight();
			streamEntry.vOffset = tracksVerticalHeight;
			if (streamEntry.waveform.getType() == WaveformType.TRANSACTION) {
				streamEntry.currentValue = "";
				streamEntry.height *= streamEntry.waveform.getRowCount();
				painter = new StreamPainter(waveformCanvas, even, streamEntry);
			} else if (streamEntry.waveform.getType() == WaveformType.SIGNAL) {
				streamEntry.currentValue = "---";
				painter = new SignalPainter(waveformCanvas, even, streamEntry);
			}
			waveformCanvas.addWaveformPainter(painter, false);
			trackVerticalOffset.put(tracksVerticalHeight, streamEntry);
			tl.setText(streamEntry.waveform.getFullName());
			nameMaxWidth = Math.max(nameMaxWidth, tl.getBounds().width);
			tracksVerticalHeight += streamEntry.height;
			even = !even;
		}
		waveformCanvas.syncSb();
		nameList.setSize(nameMaxWidth + 15, tracksVerticalHeight);
		nameListScrolled.setMinSize(nameMaxWidth + 15, tracksVerticalHeight);
		nameList.redraw();
		updateValueList();
		waveformCanvas.redraw();
		top.layout(new Control[] { valueList, nameList, waveformCanvas });
		if (trackVerticalOffset.isEmpty()) {
			waveformCanvas.setOrigin(0, 0);
		}
	}

	private int calculateValueWidth() {
		TextLayout tl = new TextLayout(waveformCanvas.getDisplay());
		tl.setFont(styleProvider.getNameFontHighlite());
		int valueMaxWidth = 0;
		for (TrackEntry v : streams) {
			tl.setText(v.currentValue);
			valueMaxWidth = Math.max(valueMaxWidth, tl.getBounds().width);
		}
		return valueMaxWidth + 15;
	}

	private void updateValueList() {
		final Long time = getCursorTime();
		for (TrackEntry entry : streams) {
			if (entry.waveform.getType() == WaveformType.SIGNAL) {
				IEvent[] value = entry.waveform.getEventsBeforeTime(time);
				if (value[0] instanceof BitVector) {
					BitVector bv = (BitVector) value[0];
					if (bv.getWidth() == 1)
						entry.currentValue = "b'" + bv;
					else {
						switch (entry.valueDisplay) {
						case SIGNED:
							entry.currentValue = Long.toString(bv.toSignedValue());
							break;
						case UNSIGNED:
							entry.currentValue = Long.toString(bv.toUnsignedValue());
							break;
						default:
							entry.currentValue = "h'" + bv.toHexString();
						}
					}
				} else if (value[0] instanceof DoubleVal) {
					Double val = ((DoubleVal) value[0]).value;
					if (val > 0.001)
						entry.currentValue = String.format("%1$,.3f", val);
					else
						entry.currentValue = Double.toString(val);
				}
			} else if (entry.waveform.getType() == WaveformType.TRANSACTION) {
				ITx[] resultsList = new ITx[entry.waveform.getRowCount()];
				EventEntry firstTx = entry.waveform.getEvents().floorEntry(time);
				if (firstTx != null) {
					do {
						for (IEvent e : firstTx.events) {
							if (e instanceof ITxEvent) {
								ITxEvent evt = ((ITxEvent) e);
								ITx tx = evt.getTransaction();
								if ((evt.getKind() == EventKind.BEGIN || evt.getKind() == EventKind.SINGLE)
										&& tx.getBeginTime() <= time && tx.getEndTime() >= time
										&& resultsList[evt.getRowIndex()] == null)
									resultsList[evt.getRowIndex()] = evt.getTransaction();
							}
						}
						firstTx = entry.waveform.getEvents().lowerEntry(firstTx.timestamp);
					} while (firstTx != null && !isArrayFull(resultsList));
					boolean separator = false;
					StringBuilder sb = new StringBuilder();
					for (ITx o : resultsList) {
						if (separator)
							sb.append("|");
						if (o != null)
							sb.append(o.getGenerator().getName());
						separator = true;
					}
					entry.currentValue = sb.toString();

				}
			}
		}
		int width = calculateValueWidth();
		valueList.setSize(width, tracksVerticalHeight);
		valueListScrolled.setMinSize(width, tracksVerticalHeight);
		valueListScrolled.redraw();
	}

	private boolean isArrayFull(Object[] array) {
		for (Object o : array) {
			if (o == null)
				return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#addSelectionChangedListener(
	 * org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#
	 * removeSelectionChangedListener(org.eclipse.jface.viewers.
	 * ISelectionChangedListener)
	 */
	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getControl()
	 */
	@Override
	public Control getControl() {
		return top;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getNameControl()
	 */
	@Override
	public Control getNameControl() {
		return nameList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getValueControl()
	 */
	@Override
	public Control getValueControl() {
		return valueList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getWaveformControl()
	 */
	@Override
	public Control getWaveformControl() {
		return waveformCanvas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		ArrayList<Object> sel = new ArrayList<>();
		if (currentTxSelection != null) {
			sel.add(currentTxSelection);
		}
		sel.addAll(currentWaveformSelection);
		return new StructuredSelection(sel.toArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#setSelection(org.eclipse.
	 * jface.viewers.ISelection)
	 */
	@Override
	public void setSelection(ISelection selection) {
		setSelection(selection, false, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#setSelection(org.eclipse.
	 * jface.viewers.ISelection, boolean)
	 */
	@Override
	public void setSelection(ISelection selection, boolean showIfNeeded) {
		setSelection(selection, false, showIfNeeded);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#addToSelection(org.eclipse.
	 * jface.viewers.ISelection, boolean)
	 */
	@Override
	public void addToSelection(ISelection selection, boolean showIfNeeded) {
		setSelection(selection, true, showIfNeeded);
	}

	public void setSelection(ISelection selection, boolean add, boolean addIfNeeded) {
		boolean selectionChanged = false;
		currentWaveformSelection.forEach(e -> e.selected = false);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			if (sel.size() == 0) {
				selectionChanged = currentTxSelection != null || currentWaveformSelection != null;
				currentTxSelection = null;
				currentWaveformSelection.clear();
			} else {
				if (!add)
					currentWaveformSelection.clear();
				List<?> selList = sel.toList();
				for(Object o: selList) {
					if (o instanceof ITx) {
						ITx txSel = (ITx) o;
						TrackEntry trackEntry = selList.size() == 2 && selList.get(1) instanceof TrackEntry
								? (TrackEntry) selList.get(1)
										: null;
						if (trackEntry == null) {
							trackEntry = getEntryFor(txSel);
							if (trackEntry == null && addIfNeeded) {
								trackEntry = new TrackEntry(txSel.getStream(), styleProvider);
								streams.add(trackEntry);
							}
						}
						currentTxSelection = txSel;
						currentWaveformSelection.clear();
						currentWaveformSelection.add(trackEntry);
						selectionChanged = true;
					} else if (o instanceof TrackEntry) {
						TrackEntry e = (TrackEntry)o;
						if(!currentWaveformSelection.contains(e))
							currentWaveformSelection.add(e);
						selectionChanged = true;
					}
					
				}
			}
		} else {
			if (currentTxSelection != null || !currentWaveformSelection.isEmpty())
				selectionChanged = true;
			currentTxSelection = null;
			currentWaveformSelection.clear();
		}
		currentWaveformSelection.forEach(e -> e.selected = true);
		if (selectionChanged) {
			currentWaveformSelection.forEach(e -> waveformCanvas.reveal(e.waveform));
			waveformCanvas.setSelected(currentTxSelection);
			valueList.redraw();
			nameList.redraw();
			fireSelectionChanged();
		}
	}

	protected void fireSelectionChanged() {
		if (currentWaveformSelection == null)
			return;
		ISelection selection = getSelection();
		Object[] list = selectionChangedListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((ISelectionChangedListener) list[i]).selectionChanged(new SelectionChangedEvent(this, selection));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#moveSelection(com.minres.
	 * scviewer.database.swt.GotoDirection)
	 */
	@Override
	public void moveSelection(GotoDirection direction) {
		if (direction == GotoDirection.NEXT || direction == GotoDirection.PREV)
			moveSelection(direction, NEXT_PREV_IN_STREAM);
		else {
			if (currentWaveformSelection.size() == 1) {
				int idx = streams.indexOf(currentWaveformSelection.get(0));
				if (direction == GotoDirection.UP && idx > 0) {
					setSelection(new StructuredSelection(streams.get(idx - 1)));
				} else if (direction == GotoDirection.DOWN && idx < (streams.size() - 1)) {
					setSelection(new StructuredSelection(streams.get(idx + 1)));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#moveSelection(com.minres.
	 * scviewer.database.swt.GotoDirection,
	 * com.minres.scviewer.database.RelationType)
	 */
	@Override
	public void moveSelection(GotoDirection direction, RelationType relationType) {
		if (currentWaveformSelection.size() != 1 && currentTxSelection == null)
			return;
		TrackEntry selectedWaveform = currentWaveformSelection.size() == 1 ? currentWaveformSelection.get(0)
				: getEntryFor(currentTxSelection);
		if (selectedWaveform.waveform.getType() == WaveformType.TRANSACTION && currentTxSelection != null) {
			if (relationType.equals(IWaveformView.NEXT_PREV_IN_STREAM)) {
				ITx transaction = null;
				if (direction == GotoDirection.NEXT) {
					IEvent[] eventsList = selectedWaveform.waveform.getEvents().get(currentTxSelection.getBeginTime());
					boolean meFound = false;
					for (IEvent evt : eventsList) {
						if (evt instanceof ITxEvent && evt.getKind() == EventKind.BEGIN
								|| evt.getKind() == EventKind.SINGLE) {
							if (meFound) {
								transaction = ((ITxEvent) evt).getTransaction();
								break;
							}
							meFound |= ((ITxEvent) evt).getTransaction().equals(currentTxSelection);
						}
					}
					if (transaction == null) {
						EventEntry entry = selectedWaveform.waveform.getEvents()
								.higherEntry(currentTxSelection.getBeginTime());
						if (entry != null)
							do {
								for (IEvent evt : entry.events) {
									if (evt instanceof ITxEvent && (evt.getKind() == EventKind.BEGIN
											|| evt.getKind() == EventKind.SINGLE)) {
										transaction = ((ITxEvent) evt).getTransaction();
										break;
									}
								}
								if (transaction == null)
									entry = selectedWaveform.waveform.getEvents().higherEntry(entry.timestamp);
							} while (entry != null && transaction == null);
					}
				} else if (direction == GotoDirection.PREV) {
					IEvent[] eventsList = selectedWaveform.waveform.getEvents().get(currentTxSelection.getBeginTime());
					boolean meFound = false;
					for (IEvent evt : Lists.reverse(Arrays.asList(eventsList))) {
						if (evt instanceof ITxEvent
								&& (evt.getKind() == EventKind.BEGIN || evt.getKind() == EventKind.SINGLE)) {
							if (meFound) {
								transaction = ((ITxEvent) evt).getTransaction();
								break;
							}
							meFound |= ((ITxEvent) evt).getTransaction().equals(currentTxSelection);
						}
					}
					if (transaction == null) {
						EventEntry entry = selectedWaveform.waveform.getEvents()
								.lowerEntry(currentTxSelection.getBeginTime());
						if (entry != null)
							do {
								for (IEvent evt : Lists.reverse(Arrays.asList(entry.events))) {
									if (evt instanceof ITxEvent && (evt.getKind() == EventKind.BEGIN
											|| evt.getKind() == EventKind.SINGLE)) {
										transaction = ((ITxEvent) evt).getTransaction();
										break;
									}
								}
								if (transaction == null)
									entry = selectedWaveform.waveform.getEvents().lowerEntry(entry.timestamp);
							} while (entry != null && transaction == null);
					}
				}
				if (transaction != null) {
					setSelection(new StructuredSelection(transaction));
				}
			} else {
				ITxRelation tx = null;
				if (direction == GotoDirection.NEXT) {
					Collection<ITxRelation> outRel = currentTxSelection.getOutgoingRelations();
					tx = selectTxToNavigateTo(outRel, relationType, true);
					if (tx != null)
						setSelection(new StructuredSelection(tx.getTarget()), true);
				} else if (direction == GotoDirection.PREV) {
					Collection<ITxRelation> inRel = currentTxSelection.getIncomingRelations();
					tx = selectTxToNavigateTo(inRel, relationType, false);
					if (tx != null)
						setSelection(new StructuredSelection(tx.getSource()), true);
				}
			}
		}
	}

	private ITxRelation selectTxToNavigateTo(Collection<ITxRelation> rel, RelationType relationType, boolean target) {
		ArrayList<ITxRelation> candidates = rel.stream().filter(r -> relationType.equals(r.getRelationType()))
				.collect(Collectors.toCollection(ArrayList::new));
		switch (candidates.size()) {
		case 0:
			return null;
		case 1:
			return candidates.get(0);
		default:
			ArrayList<ITxRelation> visibleCandidates = candidates.stream().filter(this::streamsVisible)
			.collect(Collectors.toCollection(ArrayList::new));
			if (visibleCandidates.isEmpty()) {
				return new RelSelectionDialog(waveformCanvas.getShell(), candidates, target).open();
			} else if (visibleCandidates.size() == 1) {
				return visibleCandidates.size() == 1 ? visibleCandidates.get(0) : null;
			} else {
				return new RelSelectionDialog(waveformCanvas.getShell(), visibleCandidates, target).open();
			}
		}
	}

	private boolean streamsVisible(ITxRelation relation) {
		final IWaveform src = relation.getSource().getStream();
		final IWaveform tgt = relation.getTarget().getStream();
		return streams.stream().anyMatch(x -> x.waveform == src) && streams.stream().anyMatch(x -> x.waveform == tgt);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#moveCursor(com.minres.
	 * scviewer.database.swt.GotoDirection)
	 */
	@Override
	public void moveCursor(GotoDirection direction) {
		if (currentWaveformSelection.size() != 1)
			return;
		TrackEntry sel = currentWaveformSelection.get(0);
		long time = getCursorTime();
		IEventList map = null;
		if (sel.waveform.getType() == WaveformType.TRANSACTION || sel.waveform.getType() == WaveformType.SIGNAL) {
			map = sel.waveform.getEvents();
		}
		if (map != null) {
			EventEntry entry = direction == GotoDirection.PREV ? map.lowerEntry(time) : map.higherEntry(time);
			if (entry != null) {
				time = entry.timestamp;
				setCursorTime(time);
				waveformCanvas.reveal(time);
				waveformCanvas.redraw();
				updateValueList();
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getStreamList()
	 */
	@Override
	public List<TrackEntry> getStreamList() {
		return streams;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#deleteSelectedTracks()
	 */
	@Override
	public void deleteSelectedTracks() {
		List<TrackEntry> streamList = getStreamList();
		for (Object o : (IStructuredSelection) getSelection()) {
			if (o instanceof TrackEntry) {
				TrackEntry e = (TrackEntry) o;
				e.selected = false;
				streamList.remove(e);
			}
		}
		setSelection(new StructuredSelection());
		update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#moveSelected(int)
	 */
	@Override
	public void moveSelectedTrack(int i) {
		if (!currentWaveformSelection.isEmpty()) {
			int idx = streams.indexOf(currentWaveformSelection.get(0));
			for (Object o : currentWaveformSelection)
				streams.remove(o);
			int tgtIdx = idx + i;
			if (tgtIdx < 0)
				tgtIdx = 0;
			if (tgtIdx >= streams.size())
				streams.addAll(currentWaveformSelection);
			else
				streams.addAll(tgtIdx, currentWaveformSelection);
		}
	}

	protected void paintNames(GC gc, Rectangle rect) {
		if (!streams.isEmpty()) {
			try {
				Integer firstKey = trackVerticalOffset.floorKey(rect.y);
				if (firstKey == null)
					firstKey = trackVerticalOffset.firstKey();
				Integer lastKey = trackVerticalOffset.floorKey(rect.y + rect.height);
				Rectangle subArea = new Rectangle(rect.x, 0, rect.width, styleProvider.getTrackHeight());
				if (lastKey.equals(firstKey)) {
					TrackEntry trackEntry = trackVerticalOffset.get(firstKey);
					IWaveform w = trackEntry.waveform;
					if (w.getType() == WaveformType.TRANSACTION)
						subArea.height *= w.getRowCount();
					drawTextFormat(gc, subArea, firstKey, w.getFullName(), trackEntry.selected);
				} else {
					for (Entry<Integer, TrackEntry> entry : trackVerticalOffset.subMap(firstKey, true, lastKey, true)
							.entrySet()) {
						IWaveform w = entry.getValue().waveform;
						subArea.height = styleProvider.getTrackHeight();
						if (w.getType() == WaveformType.TRANSACTION)
							subArea.height *= w.getRowCount();
						drawTextFormat(gc, subArea, entry.getKey(), w.getFullName(), entry.getValue().selected);
					}
				}
			} catch (NoSuchElementException e) {
			}
		}
	}

	protected void paintValues(GC gc, Rectangle rect) {
		if (!streams.isEmpty()) {
			try {
				Integer firstKey = trackVerticalOffset.floorKey(rect.y);
				if (firstKey == null)
					firstKey = trackVerticalOffset.firstKey();
				Integer lastKey = trackVerticalOffset.floorKey(rect.y + rect.height);
				Rectangle subArea = new Rectangle(rect.x, 0, rect.width, styleProvider.getTrackHeight());
				if (lastKey.equals(firstKey)) {
					TrackEntry trackEntry = trackVerticalOffset.get(firstKey);
					IWaveform w = trackEntry.waveform;
					if (w.getType() == WaveformType.TRANSACTION)
						subArea.height *= w.getRowCount();
					drawValue(gc, subArea, firstKey, trackEntry.currentValue, trackEntry.selected);
				} else {
					for (Entry<Integer, TrackEntry> entry : trackVerticalOffset.subMap(firstKey, true, lastKey, true)
							.entrySet()) {
						IWaveform w = entry.getValue().waveform;
						subArea.height = styleProvider.getTrackHeight();
						if (w.getType() == WaveformType.TRANSACTION)
							subArea.height *= w.getRowCount();
						drawValue(gc, subArea, entry.getKey(), entry.getValue().currentValue,
								entry.getValue().selected);
					}
				}
			} catch (NoSuchElementException e) {
			}
		}
	}

	protected void drawValue(GC gc, Rectangle subArea, Integer yOffset, String value, boolean highlite) {
		int beginIndex = 0;
		for (int offset = 0; offset < subArea.height; offset += styleProvider.getTrackHeight()) {
			int endIndex = value.indexOf('|', beginIndex);
			String str = endIndex < 0 ? value.substring(beginIndex) : value.substring(beginIndex, endIndex);
			drawTextFormat(gc, new Rectangle(subArea.x, subArea.y, subArea.width, styleProvider.getTrackHeight()),
					yOffset + offset, str, highlite);
			beginIndex = endIndex < 0 ? beginIndex : endIndex + 1;
		}
	}

	protected void drawTextFormat(GC gc, Rectangle subArea, int yOffset, String value, boolean highlite) {
		Point size = gc.textExtent(value);
		if (highlite) {
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION));
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION_TEXT));
			gc.fillRectangle(subArea.x, subArea.y + yOffset, subArea.width, subArea.height);
			gc.setFont(styleProvider.getNameFontHighlite());
		} else {
			gc.setBackground(namePaneHeader.getBackground());
			gc.setForeground(namePaneHeader.getForeground());
			gc.setFont(styleProvider.getNameFont());
		}
		gc.drawText(value, subArea.x + 5, subArea.y + yOffset + (styleProvider.getTrackHeight() - size.y) / 2, true);
	}

	public void setHighliteRelation(RelationType relationType) {
		this.waveformCanvas.setHighliteRelation(relationType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setMaxTime(long)
	 */
	@Override
	public void setMaxTime(long maxTime) {
		this.waveformCanvas.setMaxTime(maxTime);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setCursorTime(long)
	 */
	@Override
	public void setCursorTime(long time) {
		final Long oldVal = waveformCanvas.getCursorPainters().get(0).getTime();
		waveformCanvas.getCursorPainters().get(0).setTime(time);
		pcs.firePropertyChange(CURSOR_PROPERTY, oldVal, time);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setMarkerTime(long, int)
	 */
	@Override
	public void setMarkerTime(int marker, long time) {
		if (waveformCanvas.getCursorPainters().size() > marker + 1) {
			final Long oldVal = waveformCanvas.getCursorPainters().get(1 + marker).getTime();
			waveformCanvas.getCursorPainters().get(1 + marker).setTime(time);
			pcs.firePropertyChange(MARKER_PROPERTY, oldVal, time);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getCursorTime()
	 */
	@Override
	public long getCursorTime() {
		return waveformCanvas.getCursorPainters().get(0).getTime();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getActMarkerTime()
	 */
	@Override
	public int getSelectedMarker() {
		return selectedMarker;
	}

	@Override
	public List<ICursor> getCursorList() {
		List<ICursor> cursors = new LinkedList<>();
		for (CursorPainter painter : waveformCanvas.getCursorPainters())
			cursors.add(painter);
		return Collections.unmodifiableList(cursors);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getMarkerTime(int)
	 */
	@Override
	public long getMarkerTime(int index) {
		return waveformCanvas.getCursorPainters().get(index + 1).getTime();
	}

	private void createStreamDragSource(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DragSource dragSource = new DragSource(canvas, DND.DROP_MOVE);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				if (event.y < tracksVerticalHeight) {
					event.doit = true;
					LocalSelectionTransfer.getTransfer()
					.setSelection(new StructuredSelection(currentWaveformSelection));
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = getSelection();
				}
			}
		});
	}

	private void createStreamDropTarget(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DropTarget dropTarget = new DropTarget(canvas, DND.DROP_MOVE);
		dropTarget.setTransfer(types);

		dropTarget.addDropListener(new DropTargetAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void drop(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
					if (s instanceof IStructuredSelection) {
						IStructuredSelection sel = (IStructuredSelection) s;
						for (Object o : sel.toList())
							streams.remove(o);
						DropTarget tgt = (DropTarget) event.widget;
						Point dropPoint = ((Canvas) tgt.getControl()).toControl(event.x, event.y);
						// extract all elements being selected
						if (dropPoint.y > trackVerticalOffset.lastKey()) {
							streams.addAll(sel.toList());
						} else {
							TrackEntry target = trackVerticalOffset.floorEntry(dropPoint.y).getValue();
							int tgtIdx = streams.indexOf(target);
							streams.addAll(tgtIdx, sel.toList());
						}
					}
				}
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
				if (event.detail != DND.DROP_MOVE) {
					event.detail = DND.DROP_NONE;
				}
			}
		});
	}

	public TrackEntry getEntryFor(ITx source) {
		Optional<TrackEntry> optGen = streams.stream().filter(e -> source.getGenerator().equals(e.waveform))
				.findFirst();
		if (optGen.isPresent())
			return optGen.get();
		Optional<TrackEntry> optStr = streams.stream().filter(e -> source.getStream().equals(e.waveform)).findFirst();
		if (optStr.isPresent())
			return optStr.get();
		return null;
	}

	@Override
	public TrackEntry getEntryFor(IWaveform source) {
		Optional<TrackEntry> optGen = streams.stream().filter(e -> source.equals(e.waveform)).findFirst();
		if (optGen.isPresent())
			return optGen.get();
		return null;
	}

	public List<Object> getElementsAt(Point pt) {
		return waveformCanvas.getElementsAt(pt);
	}

	private void createWaveformDragSource(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DragSource dragSource = new DragSource(canvas, DND.DROP_MOVE);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = false;
				List<Object> clicked = waveformCanvas.getElementsAt(new Point(event.x, event.y));
				for (Object o : clicked) {
					if (o instanceof CursorPainter) {
						LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(o));
						((CursorPainter) o).setDragging(true);
						event.doit = true;
						return;
					}
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = waveformCanvas.getElementsAt(new Point(event.x, event.y));
				}
			}
		});
	}

	private void createWaveformDropTarget(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DropTarget dropTarget = new DropTarget(canvas, DND.DROP_MOVE);
		dropTarget.setTransfer(types);
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
					if (sel instanceof IStructuredSelection
							&& ((IStructuredSelection) sel).getFirstElement() instanceof CursorPainter) {
						CursorPainter painter = (CursorPainter) ((IStructuredSelection) sel).getFirstElement();
						painter.setDragging(false);
						updateWaveform(canvas, event, painter);
					}
				}
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
				Point offset = canvas.toControl(event.x, event.y);
				if (event.detail != DND.DROP_MOVE
						|| offset.y > trackVerticalOffset.lastKey() + styleProvider.getTrackHeight()) {
					event.detail = DND.DROP_NONE;
				}
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
				if (sel instanceof IStructuredSelection
						&& ((IStructuredSelection) sel).getFirstElement() instanceof CursorPainter) {
					updateWaveform(canvas, event, (CursorPainter) ((IStructuredSelection) sel).getFirstElement());
				}
			}

			protected void updateWaveform(final Canvas canvas, DropTargetEvent event, CursorPainter painter) {
				Point dropPoint = canvas.toControl(event.x, event.y);
				long time = waveformCanvas.getTimeForOffset(dropPoint.x);
				final Long oldVal = painter.getTime();
				painter.setTime(time);
				if (painter.id < 0) {
					pcs.firePropertyChange(CURSOR_PROPERTY, oldVal, time);
				} else {
					pcs.firePropertyChange(MARKER_PROPERTY, oldVal, time);
					pcs.firePropertyChange(MARKER_PROPERTY + painter.id, oldVal, time);
				}
				canvas.getDisplay().asyncExec(() -> {
					if (!canvas.isDisposed()) {
						canvas.redraw();
						updateValueList();
					}
				});
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#addPropertyChangeListener(
	 * java.beans.PropertyChangeListener)
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#addPropertyChangeListener(
	 * java.lang.String, java.beans.PropertyChangeListener)
	 */
	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(propertyName, listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return this.pcs.getPropertyChangeListeners();
	}

	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		return this.pcs.getPropertyChangeListeners(propertyName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#removePropertyChangeListener(
	 * java.beans.PropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.minres.scviewer.database.swt.IWaveformPanel#removePropertyChangeListener(
	 * java.lang.String, java.beans.PropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(propertyName, listener);
	}

	public boolean hasListeners(String propertyName) {
		return this.pcs.hasListeners(propertyName);
	}

	@Override
	public void scrollHorizontal(int percent) {
		if (percent < -100)
			percent = -100;
		if (percent > 100)
			percent = 100;
		long minTime = waveformCanvas.getMinVisibleTime();
		long duration = waveformCanvas.getMaxVisibleTime()-minTime;
		long diff = (duration * percent) / 100;
		waveformCanvas.setMinVisibleTime(minTime+diff);
	}

	@Override
	public void scrollTo(int pos) {
		long time = 0;
		switch(pos) {
		case IWaveformView.CURSOR_POS:
			time = getCursorTime();
			break;
		case IWaveformView.MARKER_POS:
			time = getMarkerTime(selectedMarker);
			break;
		default:
			break;
		}
		waveformCanvas.centerAt(time);
	}

	public void asyncUpdate(Widget widget) {
		widget.getDisplay().asyncExec(() -> {
			waveformCanvas.redraw();
			updateValueList();
		});
	}

	/// probably not the way it should be done
	@Override
	public void addDisposeListener(DisposeListener listener) {
		waveformCanvas.addDisposeListener(listener);
	}

	@Override
	public void setStyleProvider(IWaveformStyleProvider styleProvider) {
		this.styleProvider = styleProvider;
		waveformCanvas.setStyleProvider(styleProvider);
		update();
	}

	@Override
	public TrackEntry addWaveform(IWaveform waveform, int idx) {
		TrackEntry e = new TrackEntry(waveform, styleProvider);
		if (idx < 0)
			getStreamList().add(e);
		else
			getStreamList().add(idx, e);
		return e;
	}
	
	public boolean waveformsContainsTx() {
		return  streams.stream().filter(e -> e.waveform.getType() == WaveformType.TRANSACTION).findFirst().isPresent();
	}

	@Override
	public IWaveformZoom getWaveformZoom() {
		return waveformCanvas;
	}
}
