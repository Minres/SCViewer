SCViewer
========

SCViewer is composed of a set of eclipse plugins to display VCD and transaction streams 
created by the SystemC VCD trace implementation and the SystemC Verification Library (SCV).
For further description of the SCV please refer to 
http://www.accellera.org/activities/committees/systemc-verification.

> If you encounter issue when running on Linux please try running as `SWT_GTK3=0 scviewer` as there exist issues wiht GTK3.

The viewer has the following features
- support of VCD files (compressed and uncompressed)
 - real numbers
 - showing vectors and real numbers as analog (step-wise & continuous)
 - various value representations of bit vectors
- support of SCV transaction recordings in various formats
 - text log files (compressed and uncompressed)
 - sqlite based 
 - visualization of transaction relations

To build the plugins the Eclipse SDK or PDE can be used.

Key Shortcuts
=============

Legend:

* Left Mouse Button: LMB
* Middle Mouse Button: MMB
* Mouse Scroll wheel: MScrl
* Context any means Name List, Value List or Waveform

| Input     | Modifier | Context  | Action                            |
|-----------|----------|----------|-----------------------------------|
| LMB klick |          | any      | select                            |
| LMB klick | Shift    | Waveform | move selected marker to position  |
| LMB klick | Control  | Waveform | move cursor to position           |
| LMB drag  |          | Waveform | zoom to range                     |
| MMB klick |          | Waveform | move selected marker to position  |
| MScrl     |          | any      | scroll window up/down             |
| MScrl     | Shift    | any      | scroll window left/right          |
| Key left  |          | Waveform | scroll window to the left (slow)  |
| Key right |          | Waveform | scroll window to the right (slow) |
| Key left  | Shift    | Waveform | scroll window to the left (fast)  |
| Key right | Shift    | Waveform | scroll window to the right (fast) |
| Key up    |          | Waveform | move selection up                 |
| Key down  |          | Waveform | move selection down               |
| Key up    | Control  | Waveform | move selected track up            |
| Key down  | Control  | Waveform | move selected track down          |
| Key +     | Control  | Waveform | zoom in                           |
| Key -     | Control  | Waveform | zoom out                          |
| Key Pos1  |          | Waveform | jump to selected marker           |
| Key End   |          | Waveform | jump to cursor                    |
| Key Del   |          | any      | delete selected entries           |
