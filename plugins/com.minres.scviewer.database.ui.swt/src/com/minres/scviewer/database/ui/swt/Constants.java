package com.minres.scviewer.database.ui.swt;

import java.text.DecimalFormat;

public class Constants {

	public static final String[] UNIT_STRING={"fs", "ps", "ns", "us", "ms"};//, "s"};
	public static final long[]   UNIT_MULTIPLIER={1, 1000, 1000*1000, 1000*1000*1000, 1000*1000*1000*1000, 1000*1000*1000*1000*1000 };

    //public static final int[] UNIT_MULTIPLIER={1, 3, 10, 30, 100, 300};
    public static final long[] SCALE_MULTIPLIER={1, 2, 5, 10, 20, 50, 100, 200, 500};

	public static final String CONTENT_PROVIDER_TAG = "TOOLTIP_CONTENT_PROVIDER";
	public static final String HELP_PROVIDER_TAG = "TOOLTIP_HELP_PROVIDER";
	
	public static final DecimalFormat TIME_FORMAT_FS = new DecimalFormat("#"); 
	public static final DecimalFormat TIME_FORMAT_PS = new DecimalFormat("#"); 
	public static final DecimalFormat TIME_FORMAT_NS = new DecimalFormat("#.0##"); 
	public static final DecimalFormat TIME_FORMAT_US = new DecimalFormat("#.0#####"); 
	public static final DecimalFormat TIME_FORMAT_MS = new DecimalFormat("#.0#####"); 
	

	public static  DecimalFormat getTimeFormatForLevel(int level) {
		switch(level/SCALE_MULTIPLIER.length) {
		case 0:	return TIME_FORMAT_FS;
		case 1:	return TIME_FORMAT_PS;
		case 2:	return TIME_FORMAT_NS;
		case 3:	return TIME_FORMAT_US;
		case 4:	return TIME_FORMAT_MS;
		default:
			return TIME_FORMAT_FS;
		}
	}
	private Constants() {}

}
