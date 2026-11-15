package com.minres.scviewer.e4.application.parts;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.database.ui.WaveformSelection;
import com.minres.scviewer.e4.application.internal.util.LatestJob;
import com.minres.scviewer.e4.application.internal.util.TransactionQuery;
import com.minres.scviewer.e4.application.internal.util.TransactionQuery.Attribute;
import com.minres.scviewer.e4.application.parts.txTableTree.AttributeLabelProvider;
import com.minres.scviewer.e4.application.parts.txTableTree.TxFilter;

public class TransactionList extends Composite {
    private final ComboViewer searchProperty;
    private final ComboViewer viewProperty;
    private final Text searchValue;
    private final TableViewer tableViewer;
    private final Button loadProperties;
    private final AttributeLabelProvider valueProvider;
    private final LatestJob<List<ITx>> rowsJob;
    private record PropertyDiscovery(List<Attribute> attributes, RuntimeException error) {}
    private final LatestJob<PropertyDiscovery> attributesJob;
    private final TxFilter filter = new TxFilter();
    private final Runnable applyFilter = this::refreshTransactions;
    private List<ITx> rows = List.of();
    private List<Attribute> attributes = List.of();
    private IWaveform stream;
    private TrackEntry track;
    private TransactionQuery query;

    public TransactionList(Composite parent, int style, WaveformViewer waveformViewer) {
        super(parent, style);
        parent.setLayout(new FillLayout());
        setLayout(new GridLayout(6, false));
        label("Property to match:");
        searchProperty = propertiesCombo();
        searchValue = new Text(this, SWT.BORDER);
        searchValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        label("Property to show:");
        viewProperty = propertiesCombo();
        loadProperties = new Button(this, SWT.PUSH);
        loadProperties.setText("Load properties");
        loadProperties.setToolTipText("Discover property names in this track; values are otherwise loaded only when needed.");
        loadProperties.setEnabled(false);

        tableViewer = new TableViewer(this, SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.BORDER);
        tableViewer.setUseHashlookup(true);
        tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 6, 1));
        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(new ILazyContentProvider() {
            @Override public void updateElement(int index) {
                if (index < rows.size()) tableViewer.replace(rows.get(index), index);
            }
            @Override public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
            @Override public void dispose() {}
        });
        column("Tx ID", 160, new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.NAME));
        column("Start time", 150, new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.TX_TIME));
        valueProvider = new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.VALUE);
        column("Property Value", 180, valueProvider);

        Display display = getDisplay();
        rowsJob = new LatestJob<>(task -> { if (!display.isDisposed()) display.asyncExec(task); }, result -> {
            if (isDisposed()) return;
            rows = result;
            tableViewer.setInput(rows);
            tableViewer.setItemCount(rows.size());
        });
        attributesJob = new LatestJob<>(task -> { if (!display.isDisposed()) display.asyncExec(task); }, result -> {
            if (isDisposed()) return;
            loadProperties.setEnabled(query != null);
            loadProperties.setText("Load properties");
            if (result.error() != null) {
                MessageDialog.openError(getShell(), "Cannot load properties", result.error().toString());
                return;
            }
            Object selected = ((IStructuredSelection) searchProperty.getSelection()).getFirstElement();
            String displayedProperty = valueProvider.getShowProp();
            attributes = result.attributes();
            searchProperty.setInput(attributes);
            viewProperty.setInput(attributes);
            if (attributes.contains(selected)) searchProperty.setSelection(new StructuredSelection(selected));
            else if (!attributes.isEmpty()) searchProperty.setSelection(new StructuredSelection(attributes.get(0)));
            attributes.stream().filter(attribute -> attribute.name().equals(displayedProperty)).findFirst()
                    .ifPresent(attribute -> viewProperty.setSelection(new StructuredSelection(attribute)));
            loadProperties.setEnabled(query != null);
        });
        addListener(SWT.Show, event -> {
            getDisplay().timerExec(-1, applyFilter);
            refreshTransactions();
        });
        searchValue.addModifyListener(event -> requestFilter(200));
        searchProperty.addSelectionChangedListener(event -> {
            if (event.getSelection() instanceof IStructuredSelection selected && selected.getFirstElement() instanceof Attribute attribute) {
                filter.setSearchProp(attribute.name(), attribute.type());
                requestFilter(0);
            }
        });
        viewProperty.addSelectionChangedListener(event -> {
            if (event.getSelection() instanceof IStructuredSelection selected && selected.getFirstElement() instanceof Attribute attribute) {
                valueProvider.setShowProp(attribute.name());
                tableViewer.refresh();
            }
        });
        loadProperties.addListener(SWT.Selection, event -> {
            TransactionQuery requested = query;
            if (requested == null) return;
            loadProperties.setEnabled(false);
            loadProperties.setText("Loading properties…");
            attributesJob.schedule("Discover transaction properties", monitor -> {
                try { return new PropertyDiscovery(requested.discoverAttributes(monitor::isCanceled), null); }
                catch (OperationCanceledException e) { throw e; }
                catch (RuntimeException e) { return new PropertyDiscovery(List.of(), e); }
            });
        });
        tableViewer.addSelectionChangedListener(event -> {
            if (event.getSelection() instanceof IStructuredSelection selected && selected.getFirstElement() instanceof ITx tx)
                waveformViewer.setSelection(WaveformSelection.of(tx, track));
        });
        addDisposeListener(event -> {
            display.timerExec(-1, applyFilter);
            rowsJob.close();
            attributesJob.close();
        });
    }

    private void label(String text) { new Label(this, SWT.NONE).setText(text); }

    private ComboViewer propertiesCombo() {
        ComboViewer combo = new ComboViewer(this, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(new LabelProvider() {
            @Override public String getText(Object element) {
                Attribute attribute = (Attribute) element;
                return attribute.name() + " [" + attribute.type() + "]";
            }
        });
        GridData layout = new GridData(SWT.FILL, SWT.CENTER, false, false);
        layout.widthHint = 140;
        combo.getCombo().setLayoutData(layout);
        return combo;
    }

    private void column(String name, int width, AttributeLabelProvider provider) {
        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(name);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new DelegatingStyledCellLabelProvider(provider));
    }

    public void setInput(TrackEntry requestedTrack) {
        track = requestedTrack;
        IWaveform requestedStream = track != null && track.waveform.getType().isTransaction() ? track.waveform : null;
        if (requestedStream != null && stream == requestedStream) return;
        getDisplay().timerExec(-1, applyFilter);
        rowsJob.cancel();
        attributesJob.cancel();
        stream = requestedStream;
        query = stream == null ? null : new TransactionQuery(stream);
        rows = List.of();
        attributes = List.of();
        searchProperty.setInput(attributes);
        viewProperty.setInput(attributes);
        valueProvider.setShowProp(null);
        filter.setSearchProp(null, null);
        tableViewer.setInput(rows);
        tableViewer.setItemCount(0);
        loadProperties.setEnabled(query != null);
        loadProperties.setText("Load properties");
        requestFilter(0);
    }

    private void requestFilter(int delay) {
        rowsJob.cancel();
        getDisplay().timerExec(-1, applyFilter);
        if (!getVisible()) return;
        if (delay == 0) refreshTransactions();
        else getDisplay().timerExec(delay, applyFilter);
    }

    private void refreshTransactions() {
        if (isDisposed() || query == null) return;
        TransactionQuery requested = query;
        filter.setSearchValue(searchValue.getText());
        java.util.function.Predicate<ITx> predicate = filter.snapshot();
        boolean unfiltered = searchValue.getText().isEmpty();
        rowsJob.schedule("Read transactions", monitor -> unfiltered ? requested.transactions(monitor::isCanceled)
                : requested.filter(predicate, monitor::isCanceled));
    }

    public void focusSearch() {
        if (isDisposed()) return;
        searchValue.setFocus();
        searchValue.selectAll();
    }

    public void setSearchProps(String name, DataType type, String value) {
        Attribute property = new Attribute(name, type);
        if (!attributes.contains(property)) {
            java.util.ArrayList<Attribute> known = new java.util.ArrayList<>(attributes);
            known.add(property);
            attributes = List.copyOf(known);
            searchProperty.setInput(attributes);
            viewProperty.setInput(attributes);
        }
        searchProperty.setSelection(new StructuredSelection(property));
        filter.setSearchProp(name, type);
        searchValue.setText(value);
        requestFilter(0);
    }
}
