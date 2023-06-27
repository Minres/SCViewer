SCViewer
========

SCViewer is composed of a set of eclipse plugins to display VCD (e.g. created by SystemC VCD trace) and transaction streams. Those streams can be 
created by the SystemC Verification Library (SCV, For further description of the SCV please refer to https://www.accellera.org/activities/working-groups/systemc-verification) or by the **L**ight**w**eight **T**ranasaction **R**ecording for SystemC ( [LWTR4SC](https://github.com/Minres/LWTR4SC) ).

The viewer has the following features
- support of VCD files (compressed and uncompressed)
 - real numbers
 - showing vectors and real numbers as analog (step-wise & continuous)
 - various value representations of bit vectors
- support of SCV transaction recordings in various formats
 - text log files (compressed and uncompressed)
 - sqlite based 
 - visualization of transaction relations

> If you encounter issue when running on Linux please try running as `SWT_GTK3=0 scviewer` as there exist issues wiht GTK3.

To build the plugins the Eclipse SDK or PDE can be used.

Key Shortcuts
=============

Legend:

* Left Mouse Button: LMB
* Middle Mouse Button: MMB
* Mouse Scroll wheel: MScrl
* Context any means Name List, Value List or Waveform

| Input      | Modifier | Context  | Action                            |
|------------|----------|----------|-----------------------------------|
| LMB click  |          | any      | select                            |
| LMB click  | Shift    | Waveform | move selected marker to position  |
| LMB click  | Control  | Waveform | move cursor to position           |
| LMB drag   |          | Waveform | zoom to range                     |
| MMB click  |          | Waveform | move selected marker to position  |
| MScrl      |          | any      | scroll window up/down             |
| MScrl      | Shift    | any      | scroll window left/right          |
| MScrl      | Control  | Waveform | zoom in/out                       |
| Key left   |          | Waveform | scroll window to the left (slow)  |
| Key right  |          | Waveform | scroll window to the right (slow) |
| Key left   | Shift    | Waveform | scroll window to the left (fast)  |
| Key right  | Shift    | Waveform | scroll window to the right (fast) |
| Key up     |          | Waveform | move selection up                 |
| Key down   |          | Waveform | move selection down               |
| Key up     | Control  | Waveform | move selected track up            |
| Key down   | Control  | Waveform | move selected track down          |
| Key +      | Control  | Waveform | zoom in                           |
| Key -      | Control  | Waveform | zoom out                          |
| Key Pos1   |          | Waveform | jump to selected marker           |
| Key End    |          | Waveform | jump to cursor                    |
| Key Del    |          | any      | delete selected entries           |
| LMB click  |          | ZoomBar  | increment/decrement 1 page        |
| LMB drag   |          | ZoomBar  | drag both markers (pan)           |
| LMB drag   | Control  | ZoomBar  | drag one marker (zoom)            |
| MMB drag   |          | ZoomBar  | drag one marker (zoom)            |
| xMB dclick |          | ZoomBar  | pan to position                   |
| MScrl      |          | ZoomBar  | scroll window left/right          |
| MScrl      | Shift    | ZoomBar  | scroll window left/right double speed |
| MScrl      | Control  | ZoomBar  | zoom in/out                       |
| Key left   |          | ZoomBar  | scroll window to the left (slow)  |
| Key right  |          | ZoomBar  | scroll window to the right (slow) |
| Key up     |          | ZoomBar  | scroll window to the left (slow)  |
| Key down   |          | ZoomBar  | scroll window to the right (slow) |
| Key PgUp   |          | ZoomBar  | scroll window to the left (fast)  |
| Key PgDown |          | ZoomBar  | scroll window to the right (fast) |
| Key Pos1   |          | ZoomBar  | scroll to begin                   |
| Key End    |          | ZoomBar  | scroll to end                     |

