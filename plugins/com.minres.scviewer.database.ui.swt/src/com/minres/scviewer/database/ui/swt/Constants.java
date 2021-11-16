package com.minres.scviewer.database.ui.swt;

import java.text.DecimalFormat;

public class Constants {

	public static final String[] UNIT_STRING={"fs", "ps", "ns", "us", "ms"};//, "s"};
    
    public static final int[] UNIT_MULTIPLIER={1, 3, 10, 30, 100, 300};

	public static final String CONTENT_PROVIDER_TAG = "TOOLTIP_CONTENT_PROVIDER";
	public static final String HELP_PROVIDER_TAG = "TOOLTIP_HELP_PROVIDER";
	
	public static final DecimalFormat TIME_FORMAT_FS = new DecimalFormat("#"); 
	public static final DecimalFormat TIME_FORMAT_PS = new DecimalFormat("#"); 
	public static final DecimalFormat TIME_FORMAT_NS = new DecimalFormat("#.0##"); 
	public static final DecimalFormat TIME_FORMAT_US = new DecimalFormat("#.0#####"); 
	public static final DecimalFormat TIME_FORMAT_MS = new DecimalFormat("#.0#####"); 
	
	public static final DecimalFormat[] TIME_FORMAT = {
			TIME_FORMAT_FS, TIME_FORMAT_FS, TIME_FORMAT_FS, TIME_FORMAT_FS, TIME_FORMAT_FS, TIME_FORMAT_FS,
			TIME_FORMAT_PS, TIME_FORMAT_PS, TIME_FORMAT_PS, TIME_FORMAT_PS, TIME_FORMAT_PS, TIME_FORMAT_PS,
			TIME_FORMAT_NS, TIME_FORMAT_NS, TIME_FORMAT_NS, TIME_FORMAT_NS, TIME_FORMAT_NS, TIME_FORMAT_NS,
			TIME_FORMAT_US, TIME_FORMAT_US, TIME_FORMAT_US, TIME_FORMAT_US, TIME_FORMAT_US, TIME_FORMAT_US,
			TIME_FORMAT_MS, TIME_FORMAT_MS, TIME_FORMAT_MS, TIME_FORMAT_MS, TIME_FORMAT_MS, TIME_FORMAT_MS,
	};

	private Constants() {}

}
