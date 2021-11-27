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
package com.minres.scviewer.database.ui;

import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.RelationTypeFactory;
import com.minres.scviewer.database.tx.ITx;

public interface IWaveformView extends PropertyChangeListener, ISelectionProvider{

	static final String CURSOR_PROPERTY = "cursor_time";
	
	static final String MARKER_PROPERTY = "marker_time";
	
	static final int CURSOR_POS = 0;
	
	static final int MARKER_POS = 1;

	public static final RelationType NEXT_PREV_IN_STREAM = RelationTypeFactory.create("Prev/Next in stream"); 

	public void addSelectionChangedListener(ISelectionChangedListener listener);

	public void removeSelectionChangedListener(ISelectionChangedListener listener);
	
	public void setStyleProvider(IWaveformStyleProvider styleProvider);
	
	public void update();

	public Control getControl();

	public Control getNameControl();

	public Control getValueControl();

	public Control getWaveformControl();

	public ISelection getSelection();

	public void setSelection(ISelection selection);

	public void setSelection(ISelection selection, boolean showIfNeeded);

	public void addToSelection(ISelection selection, boolean showIfNeeded);

	public void moveSelection(GotoDirection direction);

	public void moveSelection(GotoDirection direction, RelationType relationType);

	public void moveCursor(GotoDirection direction);

	public List<TrackEntry> getStreamList();

	public TrackEntry getEntryFor(ITx source);
	
	public TrackEntry getEntryFor(IWaveform source);
	
	public List<Object> getElementsAt(Point pt);
	
	public void moveSelectedTrack(int i);
	
    public void setHighliteRelation(RelationType relationType);

	public void setMaxTime(long maxTime);

	public void setCursorTime(long time);

	public void setMarkerTime(int marker, long time);

	public long getCursorTime();

	public int getSelectedMarker();

	public long getMarkerTime(int marker);

	public void addPropertyChangeListener(PropertyChangeListener listener);

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

	public void removePropertyChangeListener(PropertyChangeListener listener);

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

	public List<ICursor> getCursorList();

	public void scrollHorizontal(int percent);
	
	public void scrollTo(int pos);
	
	public void addDisposeListener( DisposeListener listener );

	public void deleteSelectedTracks();

	public TrackEntry addWaveform(IWaveform waveform, int pos);

	public IWaveformZoom getWaveformZoom();
	
}