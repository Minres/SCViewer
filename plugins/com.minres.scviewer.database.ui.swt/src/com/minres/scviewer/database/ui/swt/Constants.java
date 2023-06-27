package com.minres.scviewer.database.ui.swt;

import java.text.DecimalFormat;

public class Constants {

	public static final String[] UNIT_STRING={"fs", "ps", "ns", "us", "ms", "s"};
	public static final long[]   UNIT_MULTIPLIER={1l, 1000l, 1000l*1000, 1000l*1000*1000, 1000l*1000*1000*1000, 1000l*1000*1000*1000*1000 };

    //public static final int[] UNIT_MULTIPLIER={1, 3, 10, 30, 100, 300};
    public static final long[] SCALE_MULTIPLIER={1, 2, 5, 10, 20, 50, 100, 200, 500};

	public static final String CONTENT_PROVIDER_TAG = "TOOLTIP_CONTENT_PROVIDER";
	public static final String HELP_PROVIDER_TAG = "TOOLTIP_HELP_PROVIDER";
	
	public static final DecimalFormat TIME_FORMAT_FS  = new DecimalFormat("#0"); 
	public static final DecimalFormat TIME_FORMAT_PS  = new DecimalFormat("#0"); 
	public static final DecimalFormat TIME_FORMAT_NS  = new DecimalFormat("#0.0##"); 
	public static final DecimalFormat TIME_FORMAT_UMS = new DecimalFormat("#0.0#####"); 
	public static final long[] POWERS_OF_TEN = {
			1L,
			10L,
			100L,
			1_000L,
			10_000L,
			100_000L,
			1_000_000L,
			10_000_000L,
			100_000_000L,
			1_000_000_000L,
			10_000_000_000L,
			100_000_000_000L,
			1_000_000_000_000L,
			10_000_000_000_000L,
			100_000_000_000_000L,
			1_000_000_000_000_000L};

	public static  DecimalFormat getTimeFormatForLevel(int idx) {
		switch(idx) {
		case 0:	return TIME_FORMAT_FS;
		case 1:	return TIME_FORMAT_PS;
		case 2:	return TIME_FORMAT_NS;
		default:
			return TIME_FORMAT_UMS;
		}
	}
	private Constants() {}

}
