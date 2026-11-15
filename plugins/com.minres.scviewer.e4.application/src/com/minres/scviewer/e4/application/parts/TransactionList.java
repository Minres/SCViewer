package com.minres.scviewer.e4.application.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.databinding.observable.list.ObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.internal.util.LatestJob;
import com.minres.scviewer.e4.application.parts.txTableTree.AbstractTransactionTreeContentProvider;
import com.minres.scviewer.e4.application.parts.txTableTree.AttributeLabelProvider;
import com.minres.scviewer.e4.application.parts.txTableTree.TransactionTreeNode;
import com.minres.scviewer.e4.application.parts.txTableTree.TransactionTreeNodeType;
import com.minres.scviewer.e4.application.parts.txTableTree.TxFilter;

public class TransactionList extends Composite {
	public class AttributeNameBean {
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public DataType getType() {
			return type;
		}
		public void setType(DataType type) {
			this.type = type;
		}
		public AttributeNameBean(String name, DataType type) {
			super();
			this.name = name;
			this.type = type;
		}
		String name;
		DataType type;

	}

	private ComboViewer searchPropComboViewer = null;

	private ComboViewer viewPropComboViewer = null;

	private Text searchPropValue;

	private TableViewer tableViewer = null; 

	private LatestJob<TransactionData> updateJob;

	private record TransactionData(List<ITx> transactions, List<AttributeNameBean> attributes) {}

	private TableColumn valueColumn = null;

	private AttributeLabelProvider valueLabelProvider = null;

	private IWaveform stream;

	private ObservableList<AttributeNameBean> attrNames = new WritableList<>();

	private List<ITx> emptyList = new ArrayList<>();

	TxFilter txFilter;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public TransactionList(Composite parent, int style, WaveformViewer waveformViewer) {
		super(parent, style);
		parent.setLayout(new FillLayout());

		setLayout(new GridLayout(5, false));
		txFilter = new TxFilter();

		Label lbl1 = new Label(this, SWT.NONE);
		lbl1.setAlignment(SWT.RIGHT);
		lbl1.setText("Property to match:");
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		searchPropComboViewer = new ComboViewer(this, SWT.NONE);
		searchPropComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				AttributeNameBean entry = (AttributeNameBean) element;
				return entry.getName()+" ["+entry.getType().toString()+"]";
			}
		});
		searchPropComboViewer.setContentProvider(new ObservableListContentProvider<AttributeNameBean>());
		searchPropComboViewer.setInput(attrNames);
		Combo searchPropCombo = searchPropComboViewer.getCombo();
		GridData gdSearchProp = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gdSearchProp.widthHint=100;
		searchPropCombo.setLayoutData(gdSearchProp);
		searchPropCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				extracted(searchPropCombo);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
				extracted(searchPropCombo);
			}
			private void extracted(Combo searchPropCombo) {
				int idx = searchPropCombo.getSelectionIndex();
				if(idx<0) return;
				AttributeNameBean sel = attrNames.get(idx);
				txFilter.setSearchProp(sel.getName(), sel.getType());
				tableViewer.refresh();
			}
		});

		searchPropValue = new Text(this, SWT.BORDER);
		GridData gdSearchPropValue = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gdSearchPropValue.minimumWidth = 50;
		searchPropValue.setLayoutData(gdSearchPropValue);
		searchPropValue.addModifyListener(e -> {
			txFilter.setSearchValue(((Text) e.widget).getText());
			tableViewer.refresh();
		});

		Label lbl2 = new Label(this, SWT.NONE);
		lbl2.setAlignment(SWT.RIGHT);
		lbl2.setText("Property to show:");
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		viewPropComboViewer = new ComboViewer(this, SWT.NONE);
		viewPropComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				AttributeNameBean entry = (AttributeNameBean) element;
				return entry.getName()+" ["+entry.getType().toString()+"]";
			}
		});
		viewPropComboViewer.setContentProvider(new ObservableListContentProvider<AttributeNameBean>());
		viewPropComboViewer.setInput(attrNames);
		Combo viewPropCombo = viewPropComboViewer.getCombo();
		GridData gdviewProp = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gdviewProp.widthHint=100;
		viewPropCombo.setLayoutData(gdviewProp);
		viewPropCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = viewPropCombo.getSelectionIndex();
				if(idx<0) return;
				valueLabelProvider.setShowProp(attrNames.get(idx).getName());
				tableViewer.refresh(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { /* do nothing */ }
		});

		tableViewer = new TableViewer(this);
		tableViewer.setContentProvider(new AbstractTransactionTreeContentProvider(waveformViewer) {

			@SuppressWarnings("unchecked")
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List<?>) {
					return ((List<ITx>) inputElement).stream().map(tx-> new TransactionTreeNode(tx, TransactionTreeNodeType.TX)).collect(Collectors.toList()).toArray();
				}
				return new Object[0];
			}
		});
		tableViewer.addFilter(txFilter);
		tableViewer.addSelectionChangedListener(event -> {
			ISelection treeSelection = event.getSelection();
			if(treeSelection instanceof IStructuredSelection) {
				Object selected = ((IStructuredSelection)treeSelection).getFirstElement();
				if(selected instanceof ITx){
					waveformViewer.setSelection(new StructuredSelection(selected));
				} else if(selected instanceof TransactionTreeNode && ((TransactionTreeNode)selected).type == TransactionTreeNodeType.TX) {
					waveformViewer.setSelection(new StructuredSelection(((TransactionTreeNode)selected).element));
				}
			}
		});

		Table table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

		TableViewerColumn nameColumnViewer = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn nameColumn = nameColumnViewer.getColumn();
		nameColumn.setWidth(200);
		nameColumn.setText("Tx ID");
		nameColumn.setResizable(true);
		nameColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.NAME)));

		TableViewerColumn timeColumnViewer = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn timeColumn = timeColumnViewer.getColumn();
		timeColumn.setAlignment(SWT.RIGHT);
		timeColumn.setWidth(150);
		timeColumn.setText("Start time");
		timeColumn.setResizable(true);
		timeColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.TX_TIME)));

		TableViewerColumn valueColumnViewer = new TableViewerColumn(tableViewer, SWT.NONE);
		valueColumn = valueColumnViewer.getColumn();
		valueColumn.setWidth(150);
		valueColumn.setText("Property Value");
		valueColumn.setResizable(true);
		valueLabelProvider= new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.VALUE);
		valueColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(valueLabelProvider));

		// Turn on the header and the lines
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		final Display display = getDisplay();
		updateJob = new LatestJob<>(task -> {
			if (!display.isDisposed()) display.asyncExec(task);
		}, data -> {
			if (isDisposed()) return;
			attrNames.addAll(data.attributes());
			if (!attrNames.isEmpty()) {
				txFilter.setSearchProp(attrNames.get(0).getName(), attrNames.get(0).getType());
				txFilter.setSearchValue(searchPropValue.getText());
			}
			tableViewer.setInput(data.transactions());
			searchPropComboViewer.refresh();
			viewPropComboViewer.refresh();
		});
		addDisposeListener(event -> {
			updateJob.close();
			attrNames.dispose();
		});
	}

	public void setInput(TrackEntry trackEntry) {
		final IWaveform requestedStream = trackEntry != null && trackEntry.waveform.getType().isTransaction()
				? trackEntry.waveform : null;
		if (requestedStream != null && stream == requestedStream) return;
		updateJob.cancel();
		stream = requestedStream;
		attrNames.clear();
		tableViewer.setInput(emptyList);
		if (requestedStream == null) return;

		updateJob.schedule("Read transactions", monitor -> {
			final Map<String, DataType> propNames = new HashMap<>();
			final List<ITx> txList = new ArrayList<>();
			if (monitor.isCanceled()) throw new OperationCanceledException();
			for (EventEntry entry : requestedStream.getEvents().entrySet()) {
				if (monitor.isCanceled()) throw new OperationCanceledException();
				if (entry.events == null) continue;
				for (IEvent event : entry.events) {
					if (monitor.isCanceled()) throw new OperationCanceledException();
					if (event.getKind() == EventKind.BEGIN || event.getKind() == EventKind.SINGLE) {
						ITx tx = ((ITxEvent) event).getTransaction();
						tx.getAttributes().forEach(attr -> propNames.put(attr.getName(), attr.getDataType()));
						txList.add(tx);
					}
				}
			}
			if (monitor.isCanceled()) throw new OperationCanceledException();
			txList.sort((t1, t2) -> Long.compare(t1.getBeginTime(), t2.getBeginTime()));
			final List<AttributeNameBean> newAttrNames = propNames.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(e -> new AttributeNameBean(e.getKey(), e.getValue()))
					.collect(Collectors.toList());
			return new TransactionData(txList, newAttrNames);
		});
	}

	public void setSearchProps(String propName, DataType type, String propValue) {
		for(int i=0; i<searchPropComboViewer.getCombo().getItemCount(); ++i) {
			String itemName  = searchPropComboViewer.getCombo().getItem(i);
			if(propName.equals(itemName)) {
				searchPropComboViewer.getCombo().select(i);
				break;
			}
		}
		searchPropValue.setText(propValue);
	}

}
