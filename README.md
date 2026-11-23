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

To build the plugins the Eclipse SDK or PDE can be used.

The target platform uses Eclipse 2026-09 and a bundled
Java 25 runtime. Run Maven with JDK 25 (`mvn -version`), then run `mvn verify`.
For Eclipse launches, select JDK 25 and reload the target definition. The e4
application and OPCoach preferences use Jakarta injection and lifecycle APIs.
OPCoach reuses version 1.3.2 for its older Javax and newer Jakarta builds; refresh
cached target artifacts when switching from the old target.

Maven 3.10.x validates artifact coordinates more strictly. Tycho 5.0.4 generates
classifiers containing paths for nested bundle jars, so `.mvn/maven.config` uses
`maven.resolver.validation=mild` until the upstream Tycho fix is released
([Tycho #6399](https://github.com/eclipse-tycho/tycho/issues/6399)). This can produce
coordinate-validation warnings for nested jars; Maven 3.9.16 remains an alternative.

Key Shortcuts
=============

Legend:

* Left Mouse Button: LMB
* Middle Mouse Button: MMB
* Mouse Scroll wheel: MScrl
* Context any means Name List, Value List or Waveform
       
| Input      | Modifier   | Context  | Action                                   |
|------------|------------|----------|------------------------------------------|
| Key Q      | Ctrl       | any      | Quite the application                    |
| F1         |            | any      | Open help                                |
| Key O      | Ctrl       | Waveform | Open waveform database file              |
| Key R      | Ctrl       | Waveform | Reload the actual waveform file          |
| Key L      | Ctrl       | Waveform | Open scviewer config file                |
| Key S      | Ctrl       | Waveform | Save scviewer config file                |
| Key ,      | Ctrl       | any      | Open preferences dialog                  |
| Key A      | Ctrl       | any      | Select all                               |
| Key +      | Ctrl       | Waveform | Zoom in                                  |
| Key -      | Ctrl       | Waveform | Zoom out                                 |
| Key G      | ALt        | Waveform | Group selected tracks                    |
| Key G      | Alt-Shift  | Waveform | Ungroup selected tracks                  |
| Key C      | ALt        | Waveform | Combine selected bits to new bit vector  |
| Key S      | ALt        | Waveform | Add a separtor before the selected track |
| Key S      | ALt-Shift  | Waveform | Add a separtor after the selected track  |
| LMB click  |            | any      | select                                   |
| LMB click  | Shift      | Waveform | move selected marker to position         |
| LMB click  | Control    | Waveform | move cursor to position                  |
| LMB drag   |            | Waveform | zoom to range                            |
| MMB click  |            | Waveform | move selected marker to position         |
| MScrl      |            | any      | scroll window up/down                    |
| MScrl      | Shift      | any      | scroll window left/right                 |
| MScrl      | Control    | Waveform | zoom in/out                              |
| Key left   |            | Waveform | scroll window to the left (slow)         |
| Key right  |            | Waveform | scroll window to the right (slow)        |
| Key left   | Shift      | Waveform | scroll window to the left (fast)         |
| Key right  | Shift      | Waveform | scroll window to the right (fast)        |
| Key up     |            | Waveform | move selection up                        |
| Key down   |            | Waveform | move selection down                      |
| Key up     | Control    | Waveform | move selected track up                   |
| Key down   | Control    | Waveform | move selected track down                 |
| Key +      | Control    | Waveform | zoom in                                  |
| Key -      | Control    | Waveform | zoom out                                 |
| Key Pos1   |            | Waveform | jump to selected marker                  |
| Key End    |            | Waveform | jump to cursor                           |
| Key Del    |            | any      | delete selected entries                  |
| LMB click  |            | ZoomBar  | increment/decrement 1 page               |
| LMB drag   |            | ZoomBar  | drag both markers (pan)                  |
| LMB drag   | Control    | ZoomBar  | drag one marker (zoom)                   |
| MMB drag   |            | ZoomBar  | drag one marker (zoom)                   |
| xMB dclick |            | ZoomBar  | pan to position                          |
| MScrl      |            | ZoomBar  | scroll window left/right                 |
| MScrl      | Shift      | ZoomBar  | scroll window left/right double speed    |
| MScrl      | Control    | ZoomBar  | zoom in/out                              |
| Key left   |            | ZoomBar  | scroll window to the left (slow)         |
| Key right  |            | ZoomBar  | scroll window to the right (slow)        |
| Key up     |            | ZoomBar  | scroll window to the left (slow)         |
| Key down   |            | ZoomBar  | scroll window to the right (slow)        |
| Key PgUp   |            | ZoomBar  | scroll window to the left (fast)         |
| Key PgDown |            | ZoomBar  | scroll window to the right (fast)        |
| Key Pos1   |            | ZoomBar  | scroll to begin                          |
| Key End    |            | ZoomBar  | scroll to end                            |


Track groups
============

In the track name pane, select tracks and press Ctrl+G (Command+G on macOS)
to create a named group. Tracks, separators, and existing groups can be grouped
when they share a parent; selecting members within a group creates a subgroup. Click the disclosure arrow or double-click its header
to collapse or expand it. Select a group and press Ctrl+Shift+G
(Command+Shift+G on macOS) to ungroup it without removing its tracks.
Settings files and persisted viewer state preserve group names, membership, nesting,
and expansion state, including tracks inside collapsed groups. Older settings files
continue to load as an ordinary list of tracks.

Bitvector expansion
===================

Click the disclosure arrow beside a multi-bit signal in the waveform track list,
or double-click its name, to show its individual bits below the bus. The bus row
remains visible. Bits are ordered from the most significant bit to bit 0, with
bit 0 denoting the least significant position. Click again to collapse the bits.
Bit tracks show binary values, including unknown (`x`) and high-impedance (`z`)
states. Settings files preserve the expansion state and individual bit tracks.

Combining single bits
=====================

Select at least two single-bit signal tracks, including individual bits expanded
from buses, and choose **Combine bits…** from the Edit menu or the track context
menu. Enter a vector name and use **Move up** / **Move down** to arrange its bits.
The dialog shows the destination bit index; the top entry becomes the most
significant bit. The default order follows the tracks' display order.

The new bus is created at the top level after the last selected bit in display
order. For a bit inside a group or expanded bus, it appears after that bit's
top-level container. The source tracks remain available. The new bus supports
normal value formats and bit expansion. Inputs retain `0`, `1`, `x`, and `z`; an
input without a sample yet contributes `x`. Changes at the same timestamp are
combined into one value, and unchanged values do not create extra transitions.

Settings files preserve the bus definition, bit order, name, and expansion state.
Combining bits from another combined vector resolves them to the original recorded
signals. Removing a source track from the view does not remove its recorded data
or invalidate the bus. If a recording or source bit is missing when settings are
loaded, the affected bus is skipped and reported while other tracks are restored.

Development Notes
=================

## Embedded Help

Older target platforms use a legacy Jasper JSP loader that calls `File.toURL()`
for its generated-class directory.
A PDE configuration directory containing a literal percent escape (such as
`scviewer.product%20issue3`) can therefore produce HTTP 500 / `PWC6034` /
`ClassNotFoundException: org.apache.jsp.index_jsp` even when the class was compiled.
`-clearPersistedState` clears the application model; it does not correct that path.

In **Run/Debug Configurations > Configuration**, disable the default configuration
area and choose an explicit path without spaces or percent escapes, for example:

```
${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/scviewer-product
```

Use a distinct safe configuration directory for each launch. Stop and relaunch
the application after changing it; the new area will have a fresh JSP cache.
Graphics warnings about EGL/DRI3 are separate from JSP class loading.
