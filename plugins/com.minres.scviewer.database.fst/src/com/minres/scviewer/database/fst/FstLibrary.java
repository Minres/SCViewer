package com.minres.scviewer.database.fst;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Callback;

public class FstLibrary {
	public static enum ScopeType {
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
	    ST_GEN_ATTRBEGIN(252),
	    ST_GEN_ATTREND(253),

	    ST_VCD_SCOPE(254),
	    ST_VCD_UPSCOPE(255);

	    public final int label;
	    private ScopeType(int label) {
	        this.label = label;
	    }
	};
	public static enum HierType {
	    HT_SCOPE(0),
	    HT_UPSCOPE(1),
	    HT_VAR(2),
	    HT_ATTRBEGIN(3),
	    HT_ATTREND(4),
	    HT_TREEBEGIN(5),
	    HT_TREEEND(6);
	    public final int type;
	    private HierType(int type) {
	        this.type = type;
	    }
	};
	public static enum VarType {
	    FST_VT_VCD_EVENT           (0),
	    FST_VT_VCD_INTEGER         (1),
	    FST_VT_VCD_PARAMETER       (2),
	    FST_VT_VCD_REAL            (3),
	    FST_VT_VCD_REAL_PARAMETER  (4),
	    FST_VT_VCD_REG             (5),
	    FST_VT_VCD_SUPPLY0         (6),
	    FST_VT_VCD_SUPPLY1         (7),
	    FST_VT_VCD_TIME            (8),
	    FST_VT_VCD_TRI             (9),
	    FST_VT_VCD_TRIAND          (10),
	    FST_VT_VCD_TRIOR           (11),
	    FST_VT_VCD_TRIREG          (12),
	    FST_VT_VCD_TRI0            (13),
	    FST_VT_VCD_TRI1            (14),
	    FST_VT_VCD_WAND            (15),
	    FST_VT_VCD_WIRE            (16),
	    FST_VT_VCD_WOR             (17),
	    FST_VT_VCD_PORT            (18),
	    FST_VT_VCD_SPARRAY         (19),    /* used to define the rownum (index) port for a sparse array */
	    FST_VT_VCD_REALTIME        (20),

	    FST_VT_GEN_STRING          (21),    /* generic string type   (max len is defined dynamically via fstWriterEmitVariableLengthValueChange) */

	    FST_VT_SV_BIT              (22),
	    FST_VT_SV_LOGIC            (23),
	    FST_VT_SV_INT              (24),    /* declare as size = 32 */
	    FST_VT_SV_SHORTINT         (25),    /* declare as size = 16 */
	    FST_VT_SV_LONGINT          (26),    /* declare as size = 64 */
	    FST_VT_SV_BYTE             (27),    /* declare as size = 8  */
	    FST_VT_SV_ENUM             (28),    /* declare as appropriate type range */
	    FST_VT_SV_SHORTREAL        (29);    /* declare and emit same as FST_VT_VCD_REAL (needs to be emitted as double, not a float) */
	    public final int varType;
	    private VarType(int varType) {
	        this.varType = varType;
	    }
	};

	public static enum AttrType {
	    FST_AT_MISC        ( 0),     /* self-contained: does not need matching FST_HT_ATTREND */
	    FST_AT_ARRAY       ( 1),
	    FST_AT_ENUM        ( 2),
	    FST_AT_PACK        ( 3);
	    public final int attrType;
	    private AttrType(int attrType) {
	        this.attrType = attrType;
	    }
	};

	@FieldOrder({"type","name","component", "name_length", "component_length"})
	public static class HierScope extends Structure {
		public byte type; /* FST_ST_MIN ... FST_ST_MAX */
        public String name;
        public String component;
        public int name_length;           /* strlen(u.scope.name) */
        public int component_length;      /* strlen(u.scope.component) */
	};

	@FieldOrder({"type","direction","svt_workspace", "sdt_workspace", "sxt_workspace", "name","length","handle","name_length", "is_alias"})
	public static class HierVar extends Structure {
		public byte type; /* FST_VT_MIN ... FST_VT_MAX */
		public byte direction; /* FST_VD_MIN ... FST_VD_MAX */
		public byte svt_workspace; /* zeroed out by FST reader, for client code use */
		public byte sdt_workspace; /* zeroed out by FST reader, for client code use */
		public int  sxt_workspace; /* zeroed out by FST reader, for client code use */
		public String name;
		public int length;
		public int handle; /*fstHandle*/
		public int name_length; /* strlen(u.var.name) */
        public int is_alias;
	};

	@FieldOrder({"type","subtype","name", "arg", "arg_from_name", "name_length"})
	public static class HierAttr extends Structure {
		public byte type; /* FST_AT_MIN ... FST_AT_MAX */
        public byte subtype; /* from fstMiscType, fstArrayType, fstEnumValueType, fstPackType */
        public String name;
        public long arg; /* number of array elements, struct members, or some other payload (possibly ignored) */
        public long arg_from_name; /* for when name is overloaded as a variable-length integer (FST_AT_MISC + FST_MT_SOURCESTEM) */
        public long name_length; /* strlen(u.attr.name) */
	};
	
	
    public static native  Pointer  fstReaderOpen(String name);
    public static native  Pointer  fstReaderOpenForUtilitiesOnly();
    public static native  void     fstReaderClose(Pointer ctx);
    public static native  String   fstReaderGetVersionString(Pointer ctx);
    public static native  String   fstReaderGetDateString(Pointer ctx);
    public static native  int      fstReaderGetFileType(Pointer ctx);
	public static native  long     fstReaderGetVarCount(Pointer ctx);
	public static native  long     fstReaderGetScopeCount(Pointer ctx);
	public static native  long     fstReaderGetAliasCount(Pointer ctx);
	public static native  long     fstReaderGetValueChangeSectionCount(Pointer ctx);
	public static native  long     fstReaderGetStartTime(Pointer ctx);
	public static native  long     fstReaderGetEndTime(Pointer ctx);
	public static native  byte     fstReaderGetTimescale(Pointer ctx);
	public static native  long     fstReaderGetTimezero(Pointer ctx);
	public static native int       fstReaderGetMaxHandle(Pointer ctx);

    public static native  void     fstReaderResetScope(Pointer ctx);
	public static native  String   fstReaderPushScope(Pointer ctx, String nam, Pointer user_info);
    public static native  String   fstReaderPopScope(Pointer ctx);
    public static native  int      fstReaderGetCurrentScopeLen(Pointer ctx);
    public static native  String   fstReaderGetCurrentFlatScope(Pointer ctx);

    public static native  int      fstReaderGetNumberDumpActivityChanges(Pointer ctx);
    public static native  long     fstReaderGetDumpActivityChangeTime(Pointer ctx, int idx);
    public static native  byte     fstReaderGetDumpActivityChangeValue(Pointer ctx, int idx);

	public static native int       fstReaderIterateHierRewind(Pointer ctx);
    public static native Pointer   fstReaderIterateHier(Pointer ctx);
    public static native int       getHierType(Pointer hier);
    public static native void      getHierScope(Pointer hier, HierScope scope);
    public static native void      getHierVar(Pointer hier, HierVar scope);
    public static native void      getHierAttr(Pointer hier, HierAttr scope);
    public static native int       fstReaderGetFacProcessMask(Pointer ctx, int facidx);
    public static native void      fstReaderSetFacProcessMask(Pointer ctx, int facidx);
    public static native void      fstReaderClrFacProcessMask(Pointer ctx, int facidx);
    public static native void      fstReaderSetFacProcessMaskAll(Pointer ctx);
    public static native void      fstReaderClrFacProcessMaskAll(Pointer ctx);

    public interface ValueChangeCallback extends Callback {
    	void callback(long time, int facidx, String value);
    }

    public static native void      iterateValueChanges(Pointer ctx, ValueChangeCallback vcc);

    /*	untranslated functions:
    int             fstReaderIterBlocks(Pointer ctx, ValueChangeCallback vcc, Pointer user_callback_data_pointer, Pointer vcdhandle);
	Pointer         fstReaderGetCurrentScopeUserInfo(Pointer ctx);
	int             fstReaderGetDoubleEndianMatchState(Pointer ctx);
	int             fstReaderGetFseekFailed(Pointer ctx);
	long            fstReaderGetMemoryUsedByWriter(Pointer ctx);
	String          fstReaderGetValueFromHandleAtTime(Pointer ctx, long tim, fstHandle facidx, Stringbuf);
	int             fstReaderIterBlocks2(Pointer ctx,
	                        void (*value_change_callback)(Pointer  user_callback_data_pointer, long time, fstHandle facidx, const unsigned Stringvalue),
	                        void (*value_change_callback_varlen)(Pointer  user_callback_data_pointer, long time, fstHandle facidx, const unsigned Stringvalue, int len),
	                        Pointer  user_callback_data_pointer, FILE *vcdhandle);
	void            fstReaderIterBlocksSetNativeDoublesOnCallback(Pointer ctx, int enable);
	int             fstReaderProcessHier(Pointer ctx, FILE *vcdhandle);
	void            fstReaderSetLimitTimeRange(Pointer ctx, long start_time, long end_time);
	void            fstReaderSetUnlimitedTimeRange(Pointer ctx);
	void            fstReaderSetVcdExtensions(Pointer ctx, int enable);
*/	

    static {
//    	System.setProperty("jna.debug_load", "true");
        Native.register("fstapi");
    }

}
