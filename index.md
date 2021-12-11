## Welcome to SCViewer

SCViewer is a simple tool to display VCD and transaction streams created by the SystemC VCD trace implementation and the SystemC Verification Library (SCV).
The viewer has the following features
* support of VCD files (compressed and uncompressed)
  * real numbers
  * showing vectors and real numbers as analog (step-wise & continuous)
  * various value representations of bit vectors
* support of SCV transaction recordings in various formats
   * text log files (compressed and uncompressed)
   * visualization of transaction relations

SCViewer is available as standalone version and can be downloaded at [Github](https://github.com/Minres/SCViewer/releases).

It can also be installed into an Eclipse application by using the update site at https://minres.github.io/SCViewer/repository.

## Key Shortcuts

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
| MScrl     | Control  | Waveform | increase/decrease zoom level      |
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
