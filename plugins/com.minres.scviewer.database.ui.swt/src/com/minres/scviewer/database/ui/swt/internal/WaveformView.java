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
package com.minres.scviewer.database.ui.swt.internal;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
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
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
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
import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.ui.GotoDirection;
import com.minres.scviewer.database.ui.ICursor;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformColors;
import com.minres.scviewer.database.ui.swt.Constants;

public class WaveformView implements IWaveformView  {

	private ListenerList<ISelectionChangedListener> selectionChangedListeners = new ListenerList<ISelectionChangedListener>();

	private PropertyChangeSupport pcs;

	static final DecimalFormat df = new DecimalFormat("#0.00####"); 

	private ITx currentTxSelection;

	private ArrayList<TrackEntry> currentWaveformSelection = new ArrayList<>();

	private ScrolledComposite nameListScrolled;

	private ScrolledComposite valueListScrolled;

	private Control namePaneHeader;

	final private Canvas nameList;

	final private Canvas valueList;

	final WaveformCanvas waveformCanvas;

	final ToolTipHandler toolTipHandler;
	
	private boolean revealSelected=false;
	
	private Composite top;

	protected ObservableList<TrackEntry> streams;

	int selectedMarker = 0;

	private int trackVerticalHeight;

	private TreeMap<Integer, TrackEntry> trackVerticalOffset;

	private Font nameFont, nameFontB;

	protected TrackEntry lastClickedEntry;

	protected MouseListener nameValueMouseListener = new MouseAdapter() {
		
		@Override
		public void mouseDown(MouseEvent e) {
			if (e.button == 1) {
				Entry<Integer, TrackEntry> entry = trackVerticalOffset.floorEntry(e.y);
				entry.getValue().selected=true;
			} else if (e.button == 3) {
				Menu topMenu= top.getMenu();
				if(topMenu!=null) topMenu.setVisible(true);
			}
		}
		
		@Override
		public void mouseUp(MouseEvent e) {
			if (e.button == 1) {
				Entry<Integer, TrackEntry> entry = trackVerticalOffset.floorEntry(e.y);
				if (entry != null) {
					if((e.stateMask & SWT.SHIFT) != 0) {
						if(lastClickedEntry.selected) {
							int firstIdx=streams.indexOf(lastClickedEntry);
							int lastIdx = streams.indexOf(entry.getValue());
							List<TrackEntry> res = firstIdx>lastIdx?streams.subList(lastIdx, firstIdx+1):streams.subList(firstIdx, lastIdx+1);
							setSelection(new StructuredSelection(res), (e.stateMask & SWT.CTRL) !=0 , false);
						} else
							setSelection(new StructuredSelection(entry.getValue()), (e.stateMask & SWT.CTRL) !=0 , false);
					} else {
						setSelection(new StructuredSelection(entry.getValue()), (e.stateMask & SWT.CTRL) !=0 , false);
					}
				}
				lastClickedEntry = entry.getValue();
			}
		}
	};
	
	class WaveformMouseListener implements MouseMoveListener, MouseListener, PaintListener {
		Point start, end;
		List<Object> initialSelected;
		boolean down=false;

		@Override
		public void mouseDoubleClick(MouseEvent e) {
		}

		@Override
		public void mouseDown(MouseEvent e) {
			start=new Point(e.x, e.y);
			end=new Point(e.x, e.y);
			down=true;
			if((e.stateMask&SWT.MODIFIER_MASK)!=0) return; //don't react on modifier
			if (e.button ==  1) {	
				initialSelected = waveformCanvas.getElementsAt(start);
			} else if (e.button == 3) {
				Menu topMenu= top.getMenu();
				if(topMenu!=null) topMenu.setVisible(true);
			}
		}

		@Override
		public void mouseMove(MouseEvent e) {
			if(down) {
				end=new Point(e.x, e.y);
				asyncUpdate(e.widget);
			}	
		}
		
		@Override
		public void paintControl(PaintEvent e) {
			if(down) {
	            GC gc = e.gc;
	            gc.setAlpha(128);
	            int minX = Math.min(start.x, end.x);
	            int width = Math.max(start.x, end.x) - minX;
	            int y_top = waveformCanvas.getRulerHeight();
	            int y_bottom = waveformCanvas.getSize().y;
	            gc.fillRectangle(minX, y_top, width,y_bottom);
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			down=false;
			if(start==null) return;
			if((e.stateMask&SWT.MODIFIER_MASK&~SWT.SHIFT)!=0) return; //don't react on modifier except shift
			if(!start.equals(end)){
				asyncUpdate(e.widget);
				long startTime = waveformCanvas.getTimeForOffset(start.x);
				long endTime = waveformCanvas.getTimeForOffset(end.x);
				long targetTimeRange = endTime-startTime;
				long currentTimeRange = waveformCanvas.getMaxVisibleTime()-waveformCanvas.getMinVisibleTime();
				if(targetTimeRange==0) return;
				long relation = currentTimeRange/targetTimeRange;
				long i = 1;
				int level=0;//relation>0?0:0;
				do {
					if(relation<0 ) {
						if(-relation<i) {
							break;
						}
						level--;
						if(-relation<i*3) {
							break;
						}
						level--;						
					} else {
						if(relation<i) {
							break;
						}
						level++;
						if(relation<i*3) {
							break;
						}
						level++;
					}
					i=i*10;
				} while(i<10000);
				if(i<10000) {
					int curLevel = waveformCanvas.getZoomLevel();
					waveformCanvas.setZoomLevel(curLevel-level, (startTime+endTime)/2);
				}
			} else if (e.button ==  1 && ((e.stateMask&SWT.SHIFT)==0)) {
				// set cursor (button 1 and no shift)
				if(Math.abs(e.x-start.x)<3 && Math.abs(e.y-start.y)<3){				
					// first set cursor time
					setCursorTime(snapOffsetToEvent(start));
					// then set selection and reveal
					setSelection(new StructuredSelection(initialSelected));
					asyncUpdate(e.widget);
				}
			}else if (e.button ==  2 ||(e.button==1 && (e.stateMask&SWT.SHIFT)!=0)) {
				// set marker (button 1 and shift)
				setMarkerTime(snapOffsetToEvent(start), selectedMarker);
				asyncUpdate(e.widget);
			}        
		}
		
		protected long snapOffsetToEvent(Point p) {
			long time= waveformCanvas.getTimeForOffset(p.x);
			long scaling=5*waveformCanvas.getScaleFactor();
			for(Object o:waveformCanvas.getElementsAt(p)){
				Entry<Long, ?> floorEntry=null, ceilEntry=null;
				if(o instanceof TrackEntry){ 
					TrackEntry entry = (TrackEntry) o;
					if(entry.waveform instanceof ISignal<?>){
						NavigableMap<Long, ?> map = ((ISignal<?>)entry.waveform).getEvents();
						floorEntry = map.floorEntry(time);
						ceilEntry = map.ceilingEntry(time);
					} else if (entry.waveform instanceof ITxStream<?>){
						NavigableMap<Long, ?> map = ((ITxStream<?>)entry.waveform).getEvents();
						floorEntry = map.floorEntry(time);
						ceilEntry = map.ceilingEntry(time);
					}
				} else if(o instanceof ITx){
					NavigableMap<Long, ?> map = ((ITx)o).getStream().getEvents();
					floorEntry = map.floorEntry(time);
					ceilEntry = map.ceilingEntry(time);
				}
				if(floorEntry!=null && time-floorEntry.getKey()>scaling)
					floorEntry=null;
				if(ceilEntry!=null && ceilEntry.getKey()-time>scaling)
					ceilEntry=null;
				if(ceilEntry==null && floorEntry!=null){
					time=floorEntry.getKey();
				}else if(ceilEntry!=null && floorEntry==null){
					time=ceilEntry.getKey();
				}else if(ceilEntry!=null && floorEntry!=null){
					time=time-floorEntry.getKey()<ceilEntry.getKey()-time?floorEntry.getKey(): ceilEntry.getKey();
				}
			}
			return time;
		}

	};
	protected WaveformMouseListener waveformMouseListener = new WaveformMouseListener();

	public WaveformView(Composite parent) {
		pcs=new PropertyChangeSupport(this);

		trackVerticalOffset = new TreeMap<Integer, TrackEntry>();
		trackVerticalHeight=0;

		nameFont = parent.getDisplay().getSystemFont();
		nameFontB = SWTResourceManager.getBoldFont(nameFont);

		streams = new ObservableList<>();
		streams.addPropertyChangeListener("content", this);

		top = new Composite(parent, SWT.NONE);
		top.setLayout(new FillLayout(SWT.HORIZONTAL));

		SashForm topSash = new SashForm(top, SWT.SMOOTH);
		topSash.setBackground(topSash.getDisplay().getSystemColor(SWT.COLOR_GRAY));
				
		Composite composite = new Composite(topSash, SWT.NONE);
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));

		waveformCanvas = new WaveformCanvas(topSash, SWT.NONE);

		SashForm leftSash = new SashForm(composite, SWT.SMOOTH);
		leftSash.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));

		Composite namePane = createTextPane(leftSash, "Name");
		namePaneHeader= namePane.getChildren()[0];
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
		nameList.addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if(!trackVerticalOffset.isEmpty()) {
					GC gc = event.gc;
					Rectangle rect = ((Canvas) event.widget).getClientArea();
					paintNames(gc, rect);
				}
			}
		});
		nameList.addMouseListener(nameValueMouseListener);
		nameListScrolled.setContent(nameList);

		Composite valuePane = createTextPane(leftSash, "Value");
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
		valueList.addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if(!trackVerticalOffset.isEmpty()) {
					GC gc = event.gc;
					Rectangle rect = ((Canvas) event.widget).getClientArea();
					paintValues(gc, rect);
				}
			}
		});
		valueList.addMouseListener(nameValueMouseListener);
		valueListScrolled.setContent(valueList);

		waveformCanvas.setMaxTime(1); 
		waveformCanvas.addMouseListener(waveformMouseListener);
		waveformCanvas.addMouseMoveListener(waveformMouseListener);
		waveformCanvas.addPaintListener(waveformMouseListener);
		
		nameListScrolled.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int y = ((ScrollBar) e.widget).getSelection();
				Point v = valueListScrolled.getOrigin();
				valueListScrolled.setOrigin(v.x, y);
				Point t = waveformCanvas.getOrigin();
				waveformCanvas.setOrigin(t.x, -y);
			}
		});
		valueListScrolled.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int y = ((ScrollBar) e.widget).getSelection();
				nameListScrolled.setOrigin(nameListScrolled.getOrigin().x, y);
				waveformCanvas.setOrigin(waveformCanvas.getOrigin().x, -y);
			}
		});
		waveformCanvas.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int y = waveformCanvas.getVerticalBar().getSelection();
				nameListScrolled.setOrigin(nameListScrolled.getOrigin().x, y);
				valueListScrolled.setOrigin(valueListScrolled.getOrigin().x, y);
			}
		});
		topSash.setWeights(new int[] { 30, 70 });
		leftSash.setWeights(new int[] { 75, 25 });

		createStreamDragSource(nameList);
		createStreamDragSource(valueList);
		createStreamDropTarget(nameList);
		createStreamDropTarget(valueList);
		createWaveformDragSource(waveformCanvas);
		createWaveformDropTarget(waveformCanvas);
		
		toolTipHandler = new ToolTipHandler(parent.getShell());
		toolTipHandler.activateHoverHelp(waveformCanvas);
	}

	private Composite createTextPane(SashForm leftSash, String text) {
		Composite namePane = new Composite(leftSash, SWT.NONE);
		GridLayout gl_namePane = new GridLayout(1, false);
		gl_namePane.verticalSpacing = 0;
		gl_namePane.marginWidth = 0;
		gl_namePane.horizontalSpacing = 0;
		gl_namePane.marginHeight = 0;
		namePane.setLayout(gl_namePane);

		CLabel nameLabel = new CLabel(namePane, SWT.NONE);
		GridData gd_nameLabel = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		gd_nameLabel.heightHint = waveformCanvas.getRulerHeight() - 2;
		nameLabel.setLayoutData(gd_nameLabel);
		nameLabel.setText(text);

		Label nameSep = new Label(namePane, SWT.SEPARATOR | SWT.HORIZONTAL);
		nameSep.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		nameSep.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		GridData gd_nameSep = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_nameSep.heightHint = 2;
		nameSep.setLayoutData(gd_nameSep);
		return namePane;
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if ("size".equals(pce.getPropertyName()) || "content".equals(pce.getPropertyName())) {
			if(revealSelected) {
				waveformCanvas.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						update();
						currentWaveformSelection.stream().forEach(e -> waveformCanvas.reveal(e.waveform));
						valueList.redraw();
						nameList.redraw();
					}
				});
				revealSelected=false;
			} else 
				waveformCanvas.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
		}
	}

	public void update() {
		trackVerticalHeight = 0;
		int nameMaxWidth = 0;
		IWaveformPainter painter = null;
		trackVerticalOffset.clear();
		waveformCanvas.clearAllWaveformPainter(false);
		boolean even = true;
		TextLayout tl = new TextLayout(waveformCanvas.getDisplay());
		tl.setFont(nameFontB);
		for (TrackEntry streamEntry : streams) {
			streamEntry.height = waveformCanvas.getTrackHeight();
			streamEntry.vOffset=trackVerticalHeight;
			if (streamEntry.isStream()) {
				streamEntry.currentValue="";
				streamEntry.height *= streamEntry.getStream().getMaxConcurrency();
				painter = new StreamPainter(waveformCanvas, even, streamEntry);
			} else if (streamEntry.isSignal()) {
				streamEntry.currentValue="---";
				painter = new SignalPainter(waveformCanvas, even, streamEntry);
			}
			waveformCanvas.addWaveformPainter(painter, false);
			trackVerticalOffset.put(trackVerticalHeight, streamEntry);
			tl.setText(streamEntry.waveform.getFullName());
			nameMaxWidth = Math.max(nameMaxWidth, tl.getBounds().width);
			trackVerticalHeight += streamEntry.height;
			even = !even;
		}
		waveformCanvas.syncScrollBars();
		nameList.setSize(nameMaxWidth + 15, trackVerticalHeight);
		nameListScrolled.setMinSize(nameMaxWidth + 15, trackVerticalHeight);
		nameList.redraw();
		updateValueList();
		waveformCanvas.redraw();
		top.layout(new Control[] { valueList, nameList, waveformCanvas });
		if (trackVerticalOffset.isEmpty()){
			waveformCanvas.setOrigin(0, 0);
		}
	}

	private int calculateValueWidth() {
		TextLayout tl = new TextLayout(waveformCanvas.getDisplay());
		tl.setFont(nameFontB);
		int valueMaxWidth = 0;
		for (TrackEntry v : streams) {
			tl.setText(v.currentValue);
			valueMaxWidth = Math.max(valueMaxWidth, tl.getBounds().width);
		}
		return valueMaxWidth + 15;
	}

	private void updateValueList(){
		final Long time = getCursorTime();
		for(TrackEntry entry:streams){
			if(entry.isSignal()){
				ISignal<?> signal = (ISignal<?>) entry.waveform;
				Object value = signal.getWaveformValueBeforeTime(time);
				if(value instanceof BitVector){
					BitVector bv = (BitVector) value;
					if(bv.getWidth()==1)
						entry.currentValue="b'"+bv;
					else {
						// TODO: same code resides in SignalPainter, fix it
						switch(entry.valueDisplay) {
						case SIGNED:
							entry.currentValue=Long.toString(bv.toSignedValue());
							break;						
						case UNSIGNED:
							entry.currentValue=Long.toString(bv.toUnsignedValue());
							break;
						default:
							entry.currentValue="h'"+bv.toHexString();
						}
					}
				} else if(value instanceof Double){
					Double val = (Double) value;
					if(val>0.001)
						entry.currentValue=String.format("%1$,.3f", val);
					else
						entry.currentValue=Double.toString(val);
				}
			} else if(entry.isStream()){
				ITxStream<?> stream = (ITxStream<?>) entry.waveform;
				ITx[] resultsList = new ITx[stream.getMaxConcurrency()];
				Entry<Long, List<ITxEvent>> firstTx=stream.getEvents().floorEntry(time);
				if(firstTx!=null){
					do {
						for(ITxEvent evt:firstTx.getValue()){
							ITx tx=evt.getTransaction();
							if(evt.getType()==ITxEvent.Type.BEGIN && tx.getBeginTime()<=time && tx.getEndTime()>=time){
								if(resultsList[tx.getConcurrencyIndex()]==null)
									resultsList[tx.getConcurrencyIndex()]= evt.getTransaction();
							}
						}
						firstTx=stream.getEvents().lowerEntry(firstTx.getKey());	
					}while(firstTx!=null && !isArrayFull(resultsList));
					entry.currentValue="";
					boolean separator=false;
									
					for(ITx o:resultsList){
						if(separator) entry.currentValue+="|";
						if(o!=null) entry.currentValue+=((ITx)o).getGenerator().getName();
											
						separator=true;
					}
				}
			}
		}
		int width = calculateValueWidth();
		valueList.setSize(width, trackVerticalHeight);
		valueListScrolled.setMinSize(width, trackVerticalHeight);
		valueListScrolled.redraw();
	}
	
	
	private boolean isArrayFull(Object[] array){
		for(Object o:array){
			if(o==null) return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getControl()
	 */
	@Override
	public Control getControl() {
		return top;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getNameControl()
	 */
	@Override
	public Control getNameControl() {
		return nameList;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getValueControl()
	 */
	@Override
	public Control getValueControl() {
		return valueList;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getWaveformControl()
	 */
	@Override
	public Control getWaveformControl() {
		return waveformCanvas;
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		if (currentTxSelection != null) {
			ArrayList<Object> sel = new ArrayList<>();
			sel.add(currentTxSelection);
			sel.addAll(currentWaveformSelection.stream().map(e -> e.waveform).collect(Collectors.toList()));
			return new StructuredSelection(sel.toArray());
		} else if (currentWaveformSelection.size()>0) {
			return new StructuredSelection(currentWaveformSelection.toArray());
		} else
			return new StructuredSelection();
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void setSelection(ISelection selection) {
		setSelection(selection, false, false);
	}
	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	@Override
	public void setSelection(ISelection selection, boolean showIfNeeded) {
		setSelection(selection, false, showIfNeeded);
	}
	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#addToSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	@Override
	public void addToSelection(ISelection selection, boolean showIfNeeded) {
		setSelection(selection, true, showIfNeeded);		
	}
	
	public void setSelection(ISelection selection, boolean add, boolean addIfNeeded) {
		boolean selectionChanged = false;
		currentWaveformSelection.forEach(e->e.selected=false);
		if (selection instanceof IStructuredSelection) {
			if(((IStructuredSelection) selection).size()==0){
				selectionChanged = currentTxSelection!=null||currentWaveformSelection!=null;  
				currentTxSelection = null;
				currentWaveformSelection .clear();
			} else {
				if(!add) currentWaveformSelection.clear();
				for(Object sel:((IStructuredSelection) selection).toArray()){
					if (sel instanceof ITx && currentTxSelection != sel){
						ITx txSel = (ITx) sel;
						TrackEntry trackEntry = getEntryForStream(txSel.getStream());
						if(trackEntry==null && addIfNeeded){
							trackEntry=new TrackEntry(txSel.getStream());
							// compute fallback colors
							Color fallbackColors[] = TrackEntry.computeColor(txSel.getStream().getName(), TrackEntry.fallbackColor, TrackEntry.highlightedFallbackColor);
							trackEntry.setColor(fallbackColors[0], fallbackColors[1]);
							streams.add(trackEntry);
						}
						currentTxSelection = txSel;
//						if(trackEntry!=null) {
//							currentWaveformSelection.add((TrackEntry)sel);
//						}
						selectionChanged = true;
					} else if (sel instanceof TrackEntry && !currentWaveformSelection.contains(sel)) {
						currentWaveformSelection.add((TrackEntry)sel);
						if(currentTxSelection!=null && !selectionChanged)
							currentTxSelection=null;						
						selectionChanged = true;
					}            		
				}
			}
		} else {
			if (currentTxSelection != null || currentWaveformSelection.size() > 0)
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
		if(currentWaveformSelection==null) return;
		ISelection selection=getSelection();
		Object[] list = selectionChangedListeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((ISelectionChangedListener) list[i]).selectionChanged(new SelectionChangedEvent(this, selection));
		}
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#moveSelection(com.minres.scviewer.database.swt.GotoDirection)
	 */
	@Override
	public void moveSelection(GotoDirection direction) {
		if(direction==GotoDirection.NEXT || direction==GotoDirection.PREV)
			moveSelection(direction, NEXT_PREV_IN_STREAM) ;
		else {
			if(currentWaveformSelection.size()==1) {
			int idx = streams.indexOf(currentWaveformSelection.get(0));
			if(direction==GotoDirection.UP && idx>0) {
				setSelection(new StructuredSelection(streams.get(idx-1)));
			} else if(direction==GotoDirection.DOWN && idx<(streams.size()-1)) {
				setSelection(new StructuredSelection(streams.get(idx+1)));
			}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#moveSelection(com.minres.scviewer.database.swt.GotoDirection, com.minres.scviewer.database.RelationType)
	 */
	@Override
	public void moveSelection(GotoDirection direction, RelationType relationType) {
		TrackEntry selectedWaveform=null;
		if(currentTxSelection!=null)
			selectedWaveform = getEntryForStream(currentTxSelection.getStream());
		else if(currentWaveformSelection.size()!=1) return;
		if(selectedWaveform==null)
			selectedWaveform = currentWaveformSelection.get(1);
		if (selectedWaveform!=null && selectedWaveform.isStream() && currentTxSelection!=null) {
			if(relationType.equals(IWaveformView.NEXT_PREV_IN_STREAM)){
				ITxStream<? extends ITxEvent> stream = selectedWaveform.getStream();
				ITx transaction = null;
				if (direction == GotoDirection.NEXT) {
					List<ITxEvent> thisEntryList = stream.getEvents().get(currentTxSelection.getBeginTime());
					boolean meFound=false;
					for (ITxEvent evt : thisEntryList) {
						if (evt.getType() == ITxEvent.Type.BEGIN) {
							if(meFound){
								transaction = evt.getTransaction();
								break;
							}
							meFound|= evt.getTransaction().equals(currentTxSelection);
						}
					}
					if (transaction == null){
						Entry<Long, List<ITxEvent>> entry = stream.getEvents().higherEntry(currentTxSelection.getBeginTime());
						if (entry != null) do {
							for (ITxEvent evt : entry.getValue()) {
								if (evt.getType() == ITxEvent.Type.BEGIN) {
									transaction = evt.getTransaction();
									break;
								}
							}
							if (transaction == null)
								entry = stream.getEvents().higherEntry(entry.getKey());
						} while (entry != null && transaction == null);
					}
				} else if (direction == GotoDirection.PREV) {
					List<ITxEvent> thisEntryList = stream.getEvents().get(currentTxSelection.getBeginTime());
					boolean meFound=false;
					for (ITxEvent evt :  Lists.reverse(thisEntryList)) {
						if (evt.getType() == ITxEvent.Type.BEGIN) {
							if(meFound){
								transaction = evt.getTransaction();
								break;
							}
							meFound|= evt.getTransaction().equals(currentTxSelection);
						}
					}
					if (transaction == null){
						Entry<Long, List<ITxEvent>> entry = stream.getEvents().lowerEntry(currentTxSelection.getBeginTime());
						if (entry != null)
							do {
								for (ITxEvent evt : Lists.reverse(entry.getValue())) {
									if (evt.getType() == ITxEvent.Type.BEGIN) {
										transaction = evt.getTransaction();
										break;
									}
								}
								if (transaction == null)
									entry = stream.getEvents().lowerEntry(entry.getKey());
							} while (entry != null && transaction == null);
					}
				}
				if (transaction != null) {
					setSelection(new StructuredSelection(transaction));
				}
			} else {
				if (direction == GotoDirection.NEXT) {
					Collection<ITxRelation>  outRel=currentTxSelection.getOutgoingRelations();
					ITxRelation tx = selectTxToNavigateTo(outRel, relationType, true);
					if(tx!=null) setSelection(new StructuredSelection(tx.getTarget()), true);
				} else if (direction == GotoDirection.PREV) {
					Collection<ITxRelation>  inRel=currentTxSelection.getIncomingRelations();
					ITxRelation tx = selectTxToNavigateTo(inRel, relationType, false);
					if(tx!=null) setSelection(new StructuredSelection(tx.getSource()), true);
				}
			}
		}
	}

	private ITxRelation selectTxToNavigateTo(Collection<ITxRelation> rel, RelationType relationType, boolean target) {
		ArrayList<ITxRelation> candidates = rel.stream().filter(r -> relationType.equals(r.getRelationType())).collect(Collectors.toCollection(ArrayList::new));
		//new RelSelectionDialog(waveformCanvas.getShell(), candidates, target).open();
		switch (candidates.size()) {
		case 0: return null;
		case 1: return candidates.get(0);
		default:
			ArrayList<ITxRelation> visibleCandidates = candidates.stream().filter(r -> streamsVisible(r)).collect(Collectors.toCollection(ArrayList::new));
			if(visibleCandidates.size()==0) {
				return new RelSelectionDialog(waveformCanvas.getShell(), candidates, target).open();
			} else if(visibleCandidates.size()==1) {
				return visibleCandidates.size()==1?visibleCandidates.get(0):null;
			} else {
				return new RelSelectionDialog(waveformCanvas.getShell(), visibleCandidates, target).open();
			}
		}
	}
	
	private boolean streamsVisible(ITxRelation relation) {
		final ITxStream<ITxEvent> src = relation.getSource().getStream();
		final ITxStream<ITxEvent> tgt = relation.getTarget().getStream();
		return streams.stream().anyMatch(x -> x.waveform == src) && streams.stream().anyMatch(x -> x.waveform == tgt);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#moveCursor(com.minres.scviewer.database.swt.GotoDirection)
	 */
	@Override
	public void moveCursor(GotoDirection direction) {
		if(currentWaveformSelection.size()!=1) return;
		TrackEntry sel = currentWaveformSelection.get(1);
		long time = getCursorTime();
		NavigableMap<Long, ?> map=null;
		if(sel.isStream()){
			map=sel.getStream().getEvents();
		} else if(sel.isSignal()){
			map=sel.getSignal().getEvents();
		}
		if(map!=null){
			Entry<Long, ?> entry=direction==GotoDirection.PREV?map.lowerEntry(time):map.higherEntry(time);
			if(entry!=null) {
				time=entry.getKey();
				setCursorTime(time);
				waveformCanvas.reveal(time);
				waveformCanvas.redraw();
				updateValueList();
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getStreamList()
	 */
	@Override
	public List<TrackEntry> getStreamList() {
		return streams;
	}
	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#deleteSelectedTracks()
	 */
	@Override
	public void deleteSelectedTracks() {
		List<TrackEntry> streams = getStreamList();
		for (Object o : (IStructuredSelection)getSelection()) {
			if(o instanceof TrackEntry) {
				TrackEntry e = (TrackEntry) o;
				e.selected=false;
				streams.remove(e);				
			}
		}
		setSelection(new StructuredSelection());
		update();
	}
	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#moveSelected(int)
	 */
	@Override
	public void moveSelectedTrack(int i) {
		if(currentWaveformSelection.size()>0){
			int idx = streams.indexOf(currentWaveformSelection.get(0));
			for(Object o: currentWaveformSelection)
				streams.remove(o);
			int tgtIdx=idx+i;
			if(tgtIdx<0) tgtIdx=0;
			if(tgtIdx>=streams.size())
				streams.addAll(currentWaveformSelection);
			else
				streams.addAll(tgtIdx, currentWaveformSelection);
		}	
	}


	protected void paintNames(GC gc, Rectangle rect) {
		if (streams.size() > 0) {
			try {
				Integer firstKey = trackVerticalOffset.floorKey(rect.y);
				if (firstKey == null)
					firstKey = trackVerticalOffset.firstKey();
				Integer lastKey = trackVerticalOffset.floorKey(rect.y + rect.height);
				Rectangle subArea = new Rectangle(rect.x, 0, rect.width, waveformCanvas.getTrackHeight());
				if (lastKey == firstKey) {
					TrackEntry trackEntry=trackVerticalOffset.get(firstKey);
					IWaveform w = trackEntry.waveform;
					if (w instanceof ITxStream<?>)
						subArea.height *= ((ITxStream<?>) w).getMaxConcurrency();
					drawTextFormat(gc, subArea, firstKey, w.getFullName(), trackEntry.selected);
				} else {
					for (Entry<Integer, TrackEntry> entry : trackVerticalOffset.subMap(firstKey, true, lastKey, true).entrySet()) {
						IWaveform w = entry.getValue().waveform;
						subArea.height = waveformCanvas.getTrackHeight();
						if (w instanceof ITxStream<?>)
							subArea.height *= ((ITxStream<?>) w).getMaxConcurrency();
						drawTextFormat(gc, subArea, entry.getKey(), w.getFullName(), entry.getValue().selected);
					}
				}
			}catch(NoSuchElementException e){}
		}
	}

	protected void paintValues(GC gc, Rectangle rect) {
		if (streams.size() > 0) {
			try {
				Integer firstKey = trackVerticalOffset.floorKey(rect.y);
				if (firstKey == null)
					firstKey = trackVerticalOffset.firstKey();
				Integer lastKey = trackVerticalOffset.floorKey(rect.y + rect.height);
				Rectangle subArea = new Rectangle(rect.x, 0, rect.width, waveformCanvas.getTrackHeight());
				if (lastKey == firstKey) {
					TrackEntry trackEntry=trackVerticalOffset.get(firstKey);
					IWaveform w = trackEntry.waveform;
					if (w instanceof ITxStream<?>)
						subArea.height *= ((ITxStream<?>) w).getMaxConcurrency();
					drawValue(gc, subArea, firstKey, trackEntry.currentValue, trackEntry.selected);
				} else {
					for (Entry<Integer, TrackEntry> entry : trackVerticalOffset.subMap(firstKey, true, lastKey, true)
							.entrySet()) {
						IWaveform w = entry.getValue().waveform;
						subArea.height = waveformCanvas.getTrackHeight();
						if (w instanceof ITxStream<?>)
							subArea.height *= ((ITxStream<?>) w).getMaxConcurrency();
						drawValue(gc, subArea, entry.getKey(), entry.getValue().currentValue, entry.getValue().selected);
					}
				}
			}catch(NoSuchElementException e){}
		}
	}

	protected void drawValue(GC gc, Rectangle subArea, Integer yOffset, String value, boolean highlite) {
		int beginIndex=0;
		for(int offset=0; offset<subArea.height; offset+=waveformCanvas.getTrackHeight()){
			int endIndex=value.indexOf('|', beginIndex);
			String str = endIndex<0?value.substring(beginIndex):value.substring(beginIndex, endIndex);
			drawTextFormat(gc, new Rectangle(subArea.x, subArea.y, subArea.width, waveformCanvas.getTrackHeight()), yOffset+offset, str, highlite);
			beginIndex=endIndex<0?beginIndex:endIndex+1;
		}
	}

	protected void drawTextFormat(GC gc, Rectangle subArea, int yOffset, String value, boolean highlite) {
		Point size = gc.textExtent(value);
		if (highlite) {
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION));
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION_TEXT));
			gc.fillRectangle(subArea.x, subArea.y + yOffset, subArea.width, subArea.height);
			gc.setFont(nameFontB);
		} else {
			gc.setBackground(namePaneHeader.getBackground());
			gc.setForeground(namePaneHeader.getForeground());
			gc.setFont(nameFont);
		}
		gc.drawText(value, subArea.x + 5, subArea.y + yOffset + (waveformCanvas.getTrackHeight() - size.y) / 2, true);
	}


	public void setHighliteRelation(RelationType relationType){
		this.waveformCanvas.setHighliteRelation(relationType);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getMaxTime()
	 */
	@Override
	public long getMaxTime() {
		return waveformCanvas.getMaxTime();
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setMaxTime(long)
	 */
	@Override
	public void setMaxTime(long maxTime) {
		this.waveformCanvas.setMaxTime(maxTime);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setZoomLevel(int)
	 */
	@Override
	public void setZoomLevel(int scale) {
		waveformCanvas.setZoomLevel(scale);
		waveformCanvas.reveal(getCursorTime());
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getZoomLevel()
	 */
	@Override
	public int getZoomLevel() {
		return waveformCanvas.getZoomLevel();
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setCursorTime(long)
	 */
	@Override
	public void setCursorTime(long time){
		final Long oldVal= waveformCanvas.getCursorPainters().get(0).getTime();
		waveformCanvas.getCursorPainters().get(0).setTime(time);
		pcs.firePropertyChange(CURSOR_PROPERTY, oldVal, time);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#setMarkerTime(long, int)
	 */
	@Override
	public void setMarkerTime(long time, int index){
		if(waveformCanvas.getCursorPainters().size()>index+1){
			final Long oldVal= waveformCanvas.getCursorPainters().get(1+index).getTime();
			waveformCanvas.getCursorPainters().get(1+index).setTime(time);
			pcs.firePropertyChange(MARKER_PROPERTY, oldVal, time);
		}
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getCursorTime()
	 */
	@Override
	public long getCursorTime(){
		return waveformCanvas.getCursorPainters().get(0).getTime();   
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getActMarkerTime()
	 */
	@Override
	public int getSelectedMarkerId(){
		return selectedMarker;
	}

	@Override
	public List<ICursor> getCursorList(){
		List<ICursor> cursors = new LinkedList<>();
		for(CursorPainter painter:waveformCanvas.getCursorPainters()) cursors.add(painter);
		return Collections.unmodifiableList(cursors);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getMarkerTime(int)
	 */
	@Override
	public long getMarkerTime(int index){
		return waveformCanvas.getCursorPainters().get(index+1).getTime();   
	}

	private void createStreamDragSource(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DragSource dragSource = new DragSource(canvas, DND.DROP_MOVE);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceAdapter() {
			public void dragStart(DragSourceEvent event) {
				if (event.y < trackVerticalHeight) {
					event.doit = true;
					LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(currentWaveformSelection));
				}
			}

			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data =getSelection(); 
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
			public void drop(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
					ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
					if(s!=null && s instanceof IStructuredSelection){
						IStructuredSelection sel = (IStructuredSelection) s;
						for(Object o: sel.toList())
							streams.remove(o);
						DropTarget tgt = (DropTarget) event.widget;
						Point dropPoint = ((Canvas) tgt.getControl()).toControl(event.x, event.y);
						// extract all elements being selected
						if( dropPoint.y > trackVerticalOffset.lastKey()) {
							streams.addAll(sel.toList());
						} else {
							TrackEntry target = trackVerticalOffset.floorEntry(dropPoint.y).getValue();
							int tgtIdx=streams.indexOf(target);
							streams.addAll(tgtIdx, sel.toList());
						}	
					}
				}
			}

			public void dropAccept(DropTargetEvent event) {
				if (event.detail != DND.DROP_MOVE) {
					event.detail = DND.DROP_NONE;
				}
			}
		});
	}

	public TrackEntry getEntryForStream(IWaveform source) {
		for(TrackEntry trackEntry:streams)
			if(trackEntry.waveform.equals(source)) return trackEntry;
		return null;
	}

	public List<Object> getElementsAt(Point pt){
		return waveformCanvas.getElementsAt(pt);
	}
	
	private void createWaveformDragSource(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DragSource dragSource = new DragSource(canvas, DND.DROP_MOVE);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceAdapter() {
			public void dragStart(DragSourceEvent event) {
				event.doit = false;
				List<Object> clicked = waveformCanvas.getElementsAt(new Point(event.x, event.y));
				for(Object o:clicked){
					if(o instanceof CursorPainter){
						LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(o));
						((CursorPainter)o).setDragging(true);
						event.doit = true;
						return;
					}
				}
			}

			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data=waveformCanvas.getElementsAt(new Point(event.x, event.y)); 
				}
			}
		});
	}

	private void createWaveformDropTarget(final Canvas canvas) {
		Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		DropTarget dropTarget = new DropTarget(canvas, DND.DROP_MOVE);
		dropTarget.setTransfer(types);
		dropTarget.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
					ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
					if(sel!=null && sel instanceof IStructuredSelection &&
							((IStructuredSelection)sel).getFirstElement() instanceof CursorPainter){
						CursorPainter painter = (CursorPainter)((IStructuredSelection)sel).getFirstElement();
						painter.setDragging(false);
						updateWaveform(canvas, event, painter);
					}
				}
			}

			public void dropAccept(DropTargetEvent event) {
				Point offset = canvas.toControl(event.x, event.y); 
				if (event.detail != DND.DROP_MOVE || offset.y > trackVerticalOffset.lastKey() + waveformCanvas.getTrackHeight()) {
					event.detail = DND.DROP_NONE;
				}
			}

			public void dragOver(DropTargetEvent event){
				ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
				if(sel!=null && sel instanceof IStructuredSelection &&
						((IStructuredSelection)sel).getFirstElement() instanceof CursorPainter){
					updateWaveform(canvas, event, (CursorPainter) ((IStructuredSelection)sel).getFirstElement());
				}
			}

			protected void updateWaveform(final Canvas canvas, DropTargetEvent event, CursorPainter painter) {
				Point dropPoint = canvas.toControl(event.x, event.y);
				long time = waveformCanvas.getTimeForOffset(dropPoint.x);
				final Long oldVal= painter.getTime();
				painter.setTime(time);
				if(painter.id<0){
					pcs.firePropertyChange(CURSOR_PROPERTY, oldVal, time);
				}else{
					pcs.firePropertyChange(MARKER_PROPERTY, oldVal, time);
					pcs.firePropertyChange(MARKER_PROPERTY+painter.id, oldVal, time);
				}
				canvas.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if(!canvas.isDisposed()){
							canvas.redraw();
							updateValueList();
						}
					}
				});
			}
		});

	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#addPropertyChangeListener(java.beans.PropertyChangeListener)
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
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

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#removePropertyChangeListener(java.beans.PropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(propertyName, listener);
	}

	public boolean hasListeners(String propertyName) {
		return this.pcs.hasListeners(propertyName);
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getScaledTime(long)
	 */
	@Override
	public String getScaledTime(long time) {
		StringBuilder sb = new StringBuilder();
		Double dTime=new Double(time);
		Double scaledTime = dTime/waveformCanvas.getScaleFactorPow10();
		return sb.append(df.format(scaledTime)).append(waveformCanvas.getUnitStr()).toString();
	}

	/* (non-Javadoc)
	 * @see com.minres.scviewer.database.swt.IWaveformPanel#getZoomLevels()
	 */
	@Override
	public String[] getZoomLevels(){
		String[] res = new String[Constants.unitMultiplier.length*Constants.unitString.length];
		int index=0;
		for(String unit:Constants.unitString){
			for(int factor:Constants.unitMultiplier){
				res[index++]= new Integer(factor).toString()+unit;
			}
		}
		return res;
	}

	@Override
	public void setColors(HashMap<WaveformColors, RGB> colourMap) {
		waveformCanvas.initColors(colourMap);
	}

	@Override
	public long getBaselineTime() {
		return -waveformCanvas.getScaleFactorPow10()*waveformCanvas.getOrigin().x;
	}

	@Override
	public void setBaselineTime(Long time) {
		Point origin = waveformCanvas.getOrigin();
		origin.x=(int) (-time/waveformCanvas.getScaleFactorPow10());	
		waveformCanvas.setOrigin(origin);
	}
	
	@Override
	public void scrollHorizontal(int percent) {
		if(percent<-100) percent=-100;
		if(percent>100) percent=100;
		int diff = (waveformCanvas.getWidth()*percent)/100;
		Point o = waveformCanvas.getOrigin();
		waveformCanvas.setOrigin(o.x-diff, o.y);
		waveformCanvas.redraw();
	}

	public void asyncUpdate(Widget widget) {
		widget.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				waveformCanvas.redraw();
				updateValueList();
			}
		});
	}
	
	/// probably not the way it should be done
	public void addDisposeListener( DisposeListener listener ) {
		waveformCanvas.addDisposeListener(listener);
	}

}