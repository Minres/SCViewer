package com.minres.scviewer.database.fst;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

public class FstLibrary {
	public enum FstScopeType {
	    MIN(0),
	    VCD_MODULE(0),
	    VCD_TASK(1),
	    VCD_FUNCTION(2),
	    VCD_BEGIN(3),
	    VCD_FORK(4),
	    VCD_GENERATE(5),
	    VCD_STRUCT(6),
	    VCD_UNION(7),
	    VCD_CLASS(8),
	    VCD_INTERFACE(9),
	    VCD_PACKAGE(10),
	    VCD_PROGRAM(11),

	    VHDL_ARCHITECTURE(12),
	    VHDL_PROCEDURE(13),
	    VHDL_FUNCTION(14),
	    VHDL_RECORD(15),
	    VHDL_PROCESS(16),
	    VHDL_BLOCK(17),
	    VHDL_FOR_GENERATE(18),
	    VHDL_IF_GENERATE(19),
	    VHDL_GENERATE(20),
	    VHDL_PACKAGE(21),
	    MAX(21),
	    FST_ST_GEN_ATTRBEGIN(252),
	    FST_ST_GEN_ATTREND(253),

	    FST_ST_VCD_SCOPE(254),
	    FST_ST_VCD_UPSCOPE(255);

	    public final int label;
	    private FstScopeType(int label) {
	        this.label = label;
	    }
	};

    
    public static native  Pointer  fstReaderOpen(String name);
    public static native  void     fstReaderClose(Pointer ctx);
    public static native  String   fstReaderGetVersionString(Pointer  ctx);
    public static native  String   fstReaderGetDateString(Pointer  ctx);
    public static native  int      fstReaderGetFileType(Pointer  ctx);
	public static native  long     fstReaderGetVarCount(Pointer  ctx);
	public static native  long     fstReaderGetScopeCount(Pointer  ctx);
	public static native  long     fstReaderGetAliasCount(Pointer  ctx);
	public static native  long     fstReaderGetValueChangeSectionCount(Pointer  ctx);
	public static native  long     fstReaderGetStartTime(Pointer  ctx);
	public static native  long     fstReaderGetEndTime(Pointer  ctx);
	public static native  byte     fstReaderGetTimescale(Pointer  ctx);
	public static native  long     fstReaderGetTimezero(Pointer  ctx);

    public static native  void     fstReaderResetScope(Pointer  ctx);
	public static native  String   fstReaderPushScope(Pointer  ctx, String nam, Pointer  user_info);
    public static native  String   fstReaderPopScope(Pointer  ctx);
    public static native  int      fstReaderGetCurrentScopeLen(Pointer  ctx);

    /*	
	void            fstReaderClrFacProcessMask(Pointer  ctx, fstHandle facidx);
	void            fstReaderClrFacProcessMaskAll(Pointer  ctx);
	String     fstReaderGetCurrentFlatScope(Pointer  ctx);
	Pointer            fstReaderGetCurrentScopeUserInfo(Pointer  ctx);
	int             fstReaderGetDoubleEndianMatchState(Pointer  ctx);
	long        fstReaderGetDumpActivityChangeTime(Pointer  ctx, int idx);
	byte   fstReaderGetDumpActivityChangeValue(Pointer  ctx, int idx);
	int             fstReaderGetFacProcessMask(Pointer  ctx, fstHandle facidx);
	int             fstReaderGetFseekFailed(Pointer  ctx);
	fstHandle       fstReaderGetMaxHandle(Pointer  ctx);
	long        fstReaderGetMemoryUsedByWriter(Pointer  ctx);
	int        fstReaderGetNumberDumpActivityChanges(Pointer  ctx);
	String          fstReaderGetValueFromHandleAtTime(Pointer  ctx, long tim, fstHandle facidx, Stringbuf);
	struct fstHier *fstReaderIterateHier(Pointer  ctx);
	int             fstReaderIterateHierRewind(Pointer  ctx);
	int             fstReaderIterBlocks(Pointer  ctx,
	                        void (*value_change_callback)(Pointer  user_callback_data_pointer, long time, fstHandle facidx, const unsigned Stringvalue),
	                        Pointer  user_callback_data_pointer, FILE *vcdhandle);
	int             fstReaderIterBlocks2(Pointer  ctx,
	                        void (*value_change_callback)(Pointer  user_callback_data_pointer, long time, fstHandle facidx, const unsigned Stringvalue),
	                        void (*value_change_callback_varlen)(Pointer  user_callback_data_pointer, long time, fstHandle facidx, const unsigned Stringvalue, int len),
	                        Pointer  user_callback_data_pointer, FILE *vcdhandle);
	void            fstReaderIterBlocksSetNativeDoublesOnCallback(Pointer  ctx, int enable);
	Pointer            fstReaderOpenForUtilitiesOnly(void);
	int             fstReaderProcessHier(Pointer  ctx, FILE *vcdhandle);
	void            fstReaderSetFacProcessMask(Pointer  ctx, fstHandle facidx);
	void            fstReaderSetFacProcessMaskAll(Pointer  ctx);
	void            fstReaderSetLimitTimeRange(Pointer  ctx, long start_time, long end_time);
	void            fstReaderSetUnlimitedTimeRange(Pointer  ctx);
	void            fstReaderSetVcdExtensions(Pointer  ctx, int enable);
*/	

    static {
    	System.setProperty("jna.debug_load", "true");
//        System.out.println(System.getProperty("jna.library.path", "UNKNOWN"));
        Native.register("fstapi");
    }

}
