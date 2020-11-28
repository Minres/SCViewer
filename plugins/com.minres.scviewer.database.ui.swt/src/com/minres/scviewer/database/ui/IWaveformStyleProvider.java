package com.minres.scviewer.database.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

public interface IWaveformStyleProvider {
	
	Font getNameFont();

	Font getNameFontHighlite();
	
	int getTrackHeight();

	Color getColor(WaveformColors type);
}
