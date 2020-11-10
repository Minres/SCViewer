package com.minres.scviewer.e4.application.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.observable.list.ObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxEvent.Type;
import com.minres.scviewer.database.ITxStream;
import com.minres.scviewer.database.ui.TrackEntry;
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

	private TreeViewer treeViewer = null; 

	private TreeColumn valueColumn = null;

	private AttributeLabelProvider nameLabelProvider = null;

	private ITxStream<? extends ITxEvent> stream;

	private ObservableList<AttributeNameBean> attrNames = new WritableList<AttributeNameBean>();

	private List<ITx> eventList = new ArrayList<ITx>();

	private List<ITx> emptyList = new ArrayList<ITx>();

	TxFilter txFilter;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public TransactionList(Composite parent, int style, WaveformViewer waveformViewer) {
		super(parent, style);
		setLayout(new GridLayout(3, false));
		txFilter = new TxFilter();

		searchPropComboViewer = new ComboViewer(this, SWT.NONE);
		searchPropComboViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				AttributeNameBean entry = (AttributeNameBean) element;
				return entry.getName()+" ["+entry.getType().toString()+"]";
			}
		});
		searchPropComboViewer.setContentProvider(new ObservableListContentProvider<AttributeNameBean>());
		searchPropComboViewer.setInput(attrNames);
		Combo searchPropCombo = searchPropComboViewer.getCombo();
		searchPropCombo.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1));
		searchPropCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = searchPropCombo.getSelectionIndex();
				AttributeNameBean sel = attrNames.get(idx);
				txFilter.setSearchProp(sel.getName(), sel.getType());
				treeViewer.refresh();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { 
				int idx = searchPropCombo.getSelectionIndex();
				AttributeNameBean sel = attrNames.get(idx);
				txFilter.setSearchProp(sel.getName(), sel.getType());
				treeViewer.refresh();
			}
		});

		searchPropValue = new Text(this, SWT.BORDER);
		searchPropValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		searchPropValue.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				txFilter.setSearchValue(((Text) e.widget).getText());
				treeViewer.refresh();
			}
		});

		viewPropComboViewer = new ComboViewer(this, SWT.NONE);
		viewPropComboViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				AttributeNameBean entry = (AttributeNameBean) element;
				return entry.getName()+" ["+entry.getType().toString()+"]";
			}
		});
		viewPropComboViewer.setContentProvider(new ObservableListContentProvider<AttributeNameBean>());
		viewPropComboViewer.setInput(attrNames);
		Combo viewPropCombo = viewPropComboViewer.getCombo();
		viewPropCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		viewPropCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = viewPropCombo.getSelectionIndex();
				nameLabelProvider.setShowProp(attrNames.get(idx).getName());
				treeViewer.refresh(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});

		treeViewer = new TreeViewer(this);
		treeViewer.setContentProvider(new AbstractTransactionTreeContentProvider(waveformViewer) {

			@SuppressWarnings("unchecked")
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof ArrayList<?>) {
					return ((ArrayList<ITx>) inputElement).stream().map(tx-> new TransactionTreeNode(tx, TransactionTreeNodeType.TX)).collect(Collectors.toList()).toArray();
				}
				return new Object[0];
			}
		});
		treeViewer.addFilter(txFilter);
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ITreeSelection treeSelection = treeViewer.getStructuredSelection();
				Object selected = treeSelection.getFirstElement();
				if(selected instanceof ITx){
					waveformViewer.setSelection(new StructuredSelection(selected));
				} else if(selected instanceof TransactionTreeNode && ((TransactionTreeNode)selected).type == TransactionTreeNodeType.TX) {
					waveformViewer.setSelection(new StructuredSelection(((TransactionTreeNode)selected).element));
				}
			}
		});
		
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		TreeViewerColumn nameColumnViewer = new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn nameColumn = nameColumnViewer.getColumn();
		nameColumn.setWidth(200);
		nameColumn.setText("Tx ID");
		nameColumn.setResizable(true);
		nameColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.NAME)));

		TreeViewerColumn timeColumnViewer = new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn timeColumn = timeColumnViewer.getColumn();
		timeColumn.setAlignment(SWT.RIGHT);
		timeColumn.setWidth(150);
		timeColumn.setText("Start time");
		timeColumn.setResizable(true);
		timeColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.TX_TIME)));

		TreeViewerColumn typeColumnViewer = new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn typeColumn = typeColumnViewer.getColumn();
		typeColumn.setAlignment(SWT.RIGHT);
		typeColumn.setWidth(150);
		typeColumn.setText("Type");
		typeColumn.setResizable(true);
		typeColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.TYPE)));

		TreeViewerColumn valueColumnViewer = new TreeViewerColumn(treeViewer, SWT.NONE);
		valueColumn = valueColumnViewer.getColumn();
		valueColumn.setWidth(150);
		valueColumn.setText("Value");
		valueColumn.setResizable(true);
		nameLabelProvider= new AttributeLabelProvider(waveformViewer, AttributeLabelProvider.VALUE);
		valueColumnViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(nameLabelProvider));

		// Turn on the header and the lines
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
	}

	public void setInput(TrackEntry trackEntry) {
		if(trackEntry==null || !trackEntry.isStream()) {
			attrNames.clear();
			treeViewer.setInput(emptyList);
		} else { 
			stream=trackEntry.getStream();
			treeViewer.setInput(emptyList);
			new Thread() {
				private ConcurrentHashMap<String, DataType> propNames=new ConcurrentHashMap<String, DataType>();

				private List<AttributeNameBean> getEntries() {
					return propNames.entrySet().stream()
							.sorted((e1,e2)->e1.getKey().compareTo(e2.getKey()))
							.map(e -> new AttributeNameBean(e.getKey(), e.getValue()))
							.collect(Collectors.toList());
				}

				public void run() {
					eventList = stream.getEvents().values().parallelStream()
							.flatMap(List::stream)
							.filter(evt -> evt.getType()==Type.BEGIN)
							.map(evt-> {
								ITx tx = evt.getTransaction();
								for(ITxAttribute attr: tx.getAttributes()) {
									propNames.put(attr.getName(), attr.getDataType());
								}
								return tx;
							})
							.sorted((t1, t2)-> t1.getBeginTime().compareTo(t2.getBeginTime()))
							.collect(Collectors.toList());
					getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							treeViewer.setInput(eventList);
							attrNames.clear();
							attrNames.addAll(getEntries());
							searchPropComboViewer.getCombo().select(0);
							txFilter.setSearchProp(attrNames.get(0).getName(), attrNames.get(0).getType());
							if (searchPropComboViewer!=null) {
								searchPropComboViewer.setInput(attrNames);
								searchPropComboViewer.setSelection(new StructuredSelection(searchPropComboViewer.getElementAt(0)));
							}
							treeViewer.refresh(true);
						}
					});
				}
			}.run();
		}
	}
	
	public void setSearchProps(String propName, DataType type, String propValue) {
		for(int i=0; i<attrNames.size(); ++i) {
			AttributeNameBean e = attrNames.get(i);
			if(propName.equals(e.getName()) && type.equals(e.getType())) {
				searchPropComboViewer.getCombo().select(i);
				break;
			}
		}
		searchPropValue.setText(propValue);
	}

}
