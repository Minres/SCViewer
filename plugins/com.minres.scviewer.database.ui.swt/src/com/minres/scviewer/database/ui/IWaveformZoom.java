package com.minres.scviewer.database.ui;

public interface IWaveformZoom {
	
	long getMaxVisibleTime();

	long getMinVisibleTime();

	void setMinVisibleTime(long scale);

	long getMaxTime();

	long getScale();

	void setScale(long factor);

	void setVisibleRange(long startTime, long endTime);
	
	void centerAt(long time);
	
	void zoom(ZoomKind kind);

	String timeToString(long time);
}
