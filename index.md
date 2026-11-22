---
title: SCViewer
---

# SCViewer

SCViewer is an Eclipse-based viewer for signal waveforms and transaction
recordings from simulations. Display signals and transactions together on a
common time axis, compare activity across multiple recording files, and inspect
the operations behind changes in a design.

It supports recordings produced by SystemC, the
[SystemC Verification Library (SCV)](https://www.accellera.org/activities/working-groups/systemc-verification),
[SystemC Components](https://github.com/Minres/SystemC-Components), and
[LWTR4SC](https://github.com/Minres/LWTR4SC). SCViewer is available as a standalone
application and as Eclipse plug-ins.

## Features

### Recording formats and browsing

- VCD signal traces, compressed or uncompressed.
- FST signal traces in the Linux and Windows builds.
- SCV transaction recordings in text format, compressed or uncompressed, and SQLite databases.
- LWTR transaction recordings in FTR format, compressed or uncompressed.
- Multiple recording files in the same waveform viewer.
- Hierarchical browsing and name filters for finding signals, streams, and generators.
- Reloading recordings after their files have changed.

### Signal waveforms and bitvectors

- Digital and real-valued signals, with values shown at the cursor position.
- Hexadecimal, binary, signed, and unsigned representations of bitvector values.
- Analog-style plots using step-wise or continuous display.
- Expansion of buses into individual bit traces while retaining the bus row.
- Unknown (`x`) and high-impedance (`z`) bit values.
- Combination of selected single-bit tracks into a named bitvector with a chosen bit order.
- Value formatting and bit expansion for combined vectors.

### Transaction analysis

- Concurrent transactions displayed in separate lanes.
- Expansion of transaction streams into child generator traces; the aggregate stream remains visible.
- Transaction properties, attributes, and incoming and outgoing relations in the details view.
- Attribute-based transaction searches, including numeric values and string patterns.
- Relation arrows and selection of the relation type to display.
- Floating attribute windows on hover, with scrolling for large amounts of information.

### Track organization and saved layouts

- Named groups, nested groups, and separators for related signals and transactions.
- Track reordering by dragging or using navigation commands.
- Display copies when grouping expanded bits or generators, or dragging them outside their parent.
  The original children remain under the source bus or stream.
- Renaming of groups and combined bitvectors.
- Saved settings for recording references, track order, groups, display formats,
  expansion states, combined bitvectors, cursor and marker positions, and the visible time range.
- Layout changes that leave the original recorded data unchanged.

## Download and installation

### Standalone application

Download [SCViewer](https://github.com/Minres/SCViewer/releases/latest):

Extract the archive and launch `scviewer` on Linux, `scviewer.exe` on Windows,
or `SCViewer.app` on macOS. The standalone packages include a Java runtime.
See the [release history](https://github.com/Minres/SCViewer/releases) for other versions.

### Eclipse plug-ins

In a compatible Eclipse installation, open **Help → Install New Software…** and
add this update-site address:

```text
https://minres.github.io/SCViewer/repository
```

Select the SCViewer features you need and complete the installation. The current
source targets Eclipse 2026-12 and Java 25; use an Eclipse installation compatible
with that platform when installing the current plug-ins.

## Getting started

1. Choose **File → Open Database** and open a supported recording.
2. Browse or filter the design hierarchy and add the signals or transaction streams
   you want to inspect to the waveform viewer.
3. Zoom, scroll, and position the cursor or markers to examine the time interval of interest.
4. Click the disclosure arrow or double-click a track name to expand a group,
   a multi-bit signal, or a transaction stream. Select transactions to inspect their
   attributes and relations, or use **Search Tx…** from the track context menu.
5. Use **File → Save active tab settings** to save the layout and
   **File → Load active tab settings** to restore it later.

Open **Help → Help Content** for the embedded user guide.

## Common keyboard shortcuts

For the command shortcuts below, use **Command** instead of **Ctrl** on macOS.
**Alt** corresponds to **Option** on macOS. Grouping and waveform actions apply
in the waveform viewer; select the relevant tracks first.

| Shortcut | Action |
|----------|--------|
| Ctrl+O | Open a recording |
| Ctrl+R | Reload the current recording |
| Ctrl+L | Load the active tab's settings |
| Ctrl+S | Save the active tab's settings |
| Ctrl+, | Open preferences |
| Ctrl+A | Select all tracks in the waveform viewer |
| Ctrl+Plus / Ctrl+Minus | Zoom in / out |
| Alt+G | Group selected tracks |
| Alt+Shift+G | Ungroup the selected group |
| Alt+C | Combine selected single-bit tracks into a bitvector |
| Alt+S | Add a separator before the selected track |
| Alt+Shift+S | Add a separator after the selected track |
| Delete | Remove selected tracks from the view |
| F1 | Open help |

### Mouse and waveform navigation

The mouse modifiers in this table are the literal **Ctrl** and **Shift** keys.

| Input | Action |
|-------|--------|
| Left-click a track or transaction | Select it |
| Click a disclosure arrow or double-click an expandable track name | Expand or collapse its children |
| Ctrl+left-click in the waveform | Move the cursor |
| Shift+left-click or middle-click in the waveform | Move the selected marker |
| Left-drag in the waveform | Zoom to the selected time range |
| Mouse wheel | Scroll vertically |
| Shift+mouse wheel | Scroll horizontally |
| Ctrl+mouse wheel in the waveform | Zoom in or out |
| Left / Right | Pan horizontally |
| Shift+Left / Shift+Right | Pan horizontally faster |
| Up / Down | Move the track selection |
| Ctrl+Up / Ctrl+Down | Move the selected track up or down |
| Home / End | Jump to the selected marker / cursor |

In the zoom bar, left-drag to pan the visible time range, or use Ctrl+left-drag
or middle-drag to change the zoom. Toolbar actions can show the full recording
or zoom to the interval between the cursor and the selected marker.

## Building from source

Build the current source with **JDK 25** and **Maven 3.9.16 or 3.10.1**. Confirm
that Maven itself uses the intended JDK with `mvn -version`.

```sh
git clone https://github.com/Minres/SCViewer.git
cd SCViewer
mvn verify
```

The build generates the documentation, runs the tests, assembles the update site,
and packages standalone products. See the
[project README](https://github.com/Minres/SCViewer/blob/main/README.md) for build
and Eclipse launch notes.

## Source and support

- [Source code](https://github.com/Minres/SCViewer)
- [Release notes and downloads](https://github.com/Minres/SCViewer/releases)
- [Issue tracker](https://github.com/Minres/SCViewer/issues)
