/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import com.minres.scviewer.database.ui.TrackEntry;

public class CombineBitsHandler {
    public record Request(String name, List<TrackEntry> bits) {
        public Request { bits = List.copyOf(bits); }
    }

    @CanExecute public boolean canExecute(EPartService parts) {
        var viewer = GroupTracksHandler.activeViewer(parts);
        return viewer != null && viewer.canCombineSelectedBits();
    }

    @Execute public void execute(EPartService parts, Shell shell) {
        var viewer = GroupTracksHandler.activeViewer(parts);
        if (viewer == null || !viewer.canCombineSelectedBits()) return;
        Request request = requestVector(shell, viewer.selectedBits());
        if (request != null) viewer.combineBits(request.name(), request.bits());
    }

    protected Request requestVector(Shell shell, List<TrackEntry> bits) {
        CombineDialog dialog = new CombineDialog(shell, bits);
        return dialog.open() == Window.OK ? dialog.getRequest() : null;
    }

    public static class CombineDialog extends TitleAreaDialog {
        private final List<TrackEntry> bits;
        private Text name;
        private Table table;
        private Request request;
        public Request getRequest() { return request; }
        public CombineDialog(Shell shell, List<TrackEntry> bits) {
            super(shell);
            this.bits = new ArrayList<>(bits);
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }
        @Override protected Control createDialogArea(Composite parent) {
            Composite area = (Composite) super.createDialogArea(parent);
            setTitle("Combine bits");
            setMessage("Name the vector and order its bits. The top entry is the most significant bit.");
            Composite form = new Composite(area, SWT.NONE);
            form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            form.setLayout(new GridLayout(2, false));
            new Label(form, SWT.NONE).setText("Vector name");
            name = new Text(form, SWT.BORDER);
            name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            name.setText("Combined vector");
            table = new Table(form, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
            table.setHeaderVisible(true);
            GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
            data.widthHint = 450; data.heightHint = 220;
            table.setLayoutData(data);
            TableColumn index = new TableColumn(table, SWT.NONE); index.setText("Bit"); index.setWidth(55);
            TableColumn source = new TableColumn(table, SWT.NONE); source.setText("Source"); source.setWidth(380);
            Composite buttons = new Composite(form, SWT.NONE);
            buttons.setLayout(new GridLayout());
            Button up = new Button(buttons, SWT.PUSH); up.setText("Move up");
            Button down = new Button(buttons, SWT.PUSH); down.setText("Move down");
            up.addListener(SWT.Selection, event -> move(-1));
            down.addListener(SWT.Selection, event -> move(1));
            refresh(0);
            return area;
        }
        private void move(int offset) {
            int selected = table.getSelectionIndex(), target = selected + offset;
            if (selected < 0 || target < 0 || target >= bits.size()) return;
            java.util.Collections.swap(bits, selected, target);
            refresh(target);
        }
        private void refresh(int selected) {
            table.removeAll();
            for (int i = 0; i < bits.size(); i++) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(new String[]{Integer.toString(bits.size() - i - 1), bits.get(i).waveform.getDisplayName()});
            }
            table.setSelection(selected);
        }
        @Override protected void okPressed() {
            if (name.getText().isBlank()) { setErrorMessage("Enter a vector name"); return; }
            request = new Request(name.getText().trim(), bits);
            super.okPressed();
        }
    }
}
