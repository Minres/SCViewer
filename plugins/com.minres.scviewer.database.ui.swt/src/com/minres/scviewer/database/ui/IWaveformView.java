/*******************************************************************************
 * Copyright (c) 2015-2023 MINRES Technologies GmbH and others.
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

	static final RelationType NEXT_PREV_IN_STREAM = RelationTypeFactory.create("Prev/Next in stream"); 

	void addSelectionChangedListener(ISelectionChangedListener listener);

	void removeSelectionChangedListener(ISelectionChangedListener listener);
	
	void setStyleProvider(IWaveformStyleProvider styleProvider);
	
	void update();

	Control getControl();

	Control getNameControl();

	Control getValueControl();

	Control getWaveformControl();

	ISelection getSelection();

	void setSelection(ISelection selection);

	void setSelection(ISelection selection, boolean showIfNeeded);

	void addToSelection(ISelection selection, boolean showIfNeeded);

	void moveSelection(GotoDirection direction);

	void moveSelection(GotoDirection direction, RelationType relationType);

	void moveCursor(GotoDirection direction);

	List<TrackEntry> getStreamList();

	TrackEntry getEntryFor(ITx source);
	
	TrackEntry getEntryFor(IWaveform source);
	
	List<Object> getElementsAt(Point pt);
	
	void moveSelectedTrack(int i);
	
    void setHighliteRelation(RelationType relationType);

	void setMaxTime(long maxTime);

	void setCursorTime(long time);

	void setMarkerTime(int marker, long time);

	long getCursorTime();

	int getSelectedMarker();

	long getMarkerTime(int marker);

	void addPropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

	List<ICursor> getCursorList();

	void scrollHorizontal(int percent);
	
	void scrollTo(int pos);
	
	void addDisposeListener( DisposeListener listener );

	void addEventListner(IWaveformviewEventListener listener);
	
	void removeEventListner(IWaveformviewEventListener listener);
	
	void deleteSelectedTracks();

	TrackEntry addWaveform(IWaveform waveform, int pos);

	IWaveformZoom getWaveformZoom();
	
}