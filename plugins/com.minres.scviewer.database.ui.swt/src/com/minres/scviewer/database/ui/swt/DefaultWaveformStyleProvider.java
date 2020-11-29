package com.minres.scviewer.database.ui.swt;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.SWTResourceManager;

import com.minres.scviewer.database.ui.IWaveformStyleProvider;
import com.minres.scviewer.database.ui.WaveformColors;

public class DefaultWaveformStyleProvider implements IWaveformStyleProvider {

	Composite parent;
	
	private Font nameFont;
	
	private Font nameFontB;

    Color[] colors = new Color[WaveformColors.values().length];

	public DefaultWaveformStyleProvider() {
		nameFont = Display.getCurrent().getSystemFont();
		nameFontB = SWTResourceManager.getBoldFont(nameFont);
        colors[WaveformColors.LINE.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_RED);
        colors[WaveformColors.LINE_HIGHLITE.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_CYAN);
        colors[WaveformColors.TRACK_BG_EVEN.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_BLACK);
        colors[WaveformColors.TRACK_BG_ODD.ordinal()] = SWTResourceManager.getColor(40, 40, 40);
        colors[WaveformColors.TRACK_BG_HIGHLITE.ordinal()] = SWTResourceManager.getColor(40, 40, 80);
        colors[WaveformColors.TX_BG.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_GREEN);
        colors[WaveformColors.TX_BG_HIGHLITE.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
        colors[WaveformColors.TX_BORDER.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_RED);
        colors[WaveformColors.SIGNAL0.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_GREEN);
        colors[WaveformColors.SIGNAL1.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_GREEN);
        colors[WaveformColors.SIGNALZ.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_DARK_YELLOW);
        colors[WaveformColors.SIGNALX.ordinal()] = SWTResourceManager.getColor(255,  51,  51);
        colors[WaveformColors.SIGNALU.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_YELLOW);
        colors[WaveformColors.SIGNAL_TEXT.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_WHITE);
        colors[WaveformColors.SIGNAL_REAL.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_YELLOW);
        colors[WaveformColors.SIGNAL_NAN.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_RED);
        colors[WaveformColors.CURSOR.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_RED);
        colors[WaveformColors.CURSOR_DRAG.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_GRAY);
        colors[WaveformColors.CURSOR_TEXT.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_WHITE);
        colors[WaveformColors.MARKER.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY);
        colors[WaveformColors.MARKER_TEXT.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_WHITE);
        colors[WaveformColors.REL_ARROW.ordinal()] = SWTResourceManager.getColor(SWT.COLOR_MAGENTA);
        colors[WaveformColors.REL_ARROW_HIGHLITE.ordinal()] = SWTResourceManager.getColor(255, 128, 255);
	}
	/** 
	 * needs redraw() afterwards
	 * @param colourMap
	 */
    public void initColors(Map<WaveformColors, RGB> colourMap) {
        Display d = parent.getDisplay();
        if (colourMap != null) {
            for (WaveformColors c : WaveformColors.values()) {
                if (colourMap.containsKey(c))
                    colors[c.ordinal()] = new Color(d, colourMap.get(c));
            }
        }
    }

	@Override
	public Font getNameFont() {
		return nameFont;
	}

	@Override
	public Font getNameFontHighlite() {
		return nameFontB;
	}

	@Override
	public int getTrackHeight() {
		return 50;
	}
	@Override
	public Color getColor(WaveformColors type) {
		return colors[type.ordinal()];
	}
	@Override
	public Color[] computeColor(String name) {
		return new Color[] {SWTResourceManager.getColor( 200,0,0), SWTResourceManager.getColor( 255,0,0)};

	}

}
