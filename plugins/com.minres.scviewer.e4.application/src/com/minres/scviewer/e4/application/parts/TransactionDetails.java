/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.provider.TxPropertiesLabelProvider;

/**
 * The Class TransactionDetails shows the details of a selected transaction. 
 */
public class TransactionDetails {

	/** The Constant COLUMN_FIRST. */
	// Column constants
	public static final int COLUMN_FIRST = 0;

	/** The Constant COLUMN_SECOND. */
	public static final int COLUMN_SECOND = 1;

	/** The Constant COLUMN_THIRD. */
	public static final int COLUMN_THIRD = 2;

	/** The event broker. */
	@Inject IEventBroker eventBroker;

	/** The selection service. */
	@Inject	ESelectionService selectionService;

	/** The name filter. */
	private Text nameFilter;

	/** The tree viewer. */
	private TreeViewer treeViewer;

	/** The col3. */
	private TreeViewerColumn col1, col2, col3;

	/** The attribute filter. */
	TxAttributeFilter attributeFilter;

	/** The view sorter. */
	TxAttributeViewerSorter viewSorter;

	/** The waveform viewer part. */
	private WaveformViewer waveformViewerPart;


	/**
	 * Creates the composite.
	 *
	 * @param parent the parent
	 */
	@PostConstruct
	public void createComposite(final Composite parent, @Optional WaveformViewer waveformViewerPart) {
		this.waveformViewerPart=waveformViewerPart;
		
		parent.setLayout(new GridLayout(1, false));

		nameFilter = new Text(parent, SWT.BORDER);
		nameFilter.setMessage(Messages.TransactionDetails_0);
		nameFilter.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				attributeFilter.setSearchText(((Text) e.widget).getText());
				treeViewer.refresh();
				treeViewer.expandAll(true);
			}
		});

		nameFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		attributeFilter = new TxAttributeFilter();
		viewSorter = new TxAttributeViewerSorter();

		treeViewer = new TreeViewer(parent);
		treeViewer.setContentProvider(new TransactionTreeContentProvider());
		treeViewer.setLabelProvider(new TxPropertiesLabelProvider());
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.addFilter(attributeFilter);
		treeViewer.setComparator(viewSorter);
		treeViewer.setAutoExpandLevel(2);
		treeViewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				treeViewer.getSelection();
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				treeViewer.getSelection();
			}

		});
		// Set up the table
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		// Add the name column
		col1 = new TreeViewerColumn(treeViewer, SWT.NONE);
		col1.getColumn().setText(Messages.TransactionDetails_1);
		col1.getColumn().setResizable(true);
		col1.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(AttributeLabelProvider.NAME)));
		col1.getColumn().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((TxAttributeViewerSorter) treeViewer.getComparator()).doSort(COLUMN_FIRST);
				treeViewer.refresh();
			}
		});
		// Add the type column
		col2 = new TreeViewerColumn(treeViewer, SWT.NONE);
		col2.getColumn().setText(Messages.TransactionDetails_2);
		col2.getColumn().setResizable(true);
		col2.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(AttributeLabelProvider.TYPE)));
		col2.getColumn().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((TxAttributeViewerSorter) treeViewer.getComparator()).doSort(COLUMN_SECOND);
				treeViewer.refresh();
			}
		});
		// Add the value column
		col3 = new TreeViewerColumn(treeViewer, SWT.NONE);
		col3.getColumn().setText(Messages.TransactionDetails_3);
		col3.getColumn().setResizable(true);
		col3.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(AttributeLabelProvider.VALUE)));
		col3.getColumn().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((TxAttributeViewerSorter) treeViewer.getComparator()).doSort(COLUMN_SECOND);
				treeViewer.refresh();
			}
		});
		// Pack the columns
		//		for (int i = 0, n = table.getColumnCount(); i < n; i++) {
		//			table.getColumn(i).pack();
		//		}

		// Turn on the header and the lines
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		treeViewer.addDoubleClickListener(new IDoubleClickListener(){

			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = treeViewer.getSelection();
				if(selection instanceof IStructuredSelection){
					IStructuredSelection structuredSelection = (IStructuredSelection) selection;
					Object selected = structuredSelection.getFirstElement();
					if(selected instanceof Object[]){
						Object[] selectedArray = (Object[]) selected;
						if(selectedArray.length==3 && selectedArray[2] instanceof ITx){
							waveformViewerPart.setSelection(new StructuredSelection(selectedArray[2]));
							setInput(selectedArray[2]);
						}
					}
				}
			}

		});
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Tree table = treeViewer.getTree();
				Rectangle area = parent.getClientArea();
				Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - 2*table.getBorderWidth();
				if (preferredSize.y > area.height + table.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = table.getVerticalBar().getSize();
					width -= vBarSize.x;
				}
				Point oldSize = table.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns 
					// smaller first and then resize the table to
					// match the client area width
					col1.getColumn().setWidth(width/3);
					col2.getColumn().setWidth(width/4);
					col3.getColumn().setWidth(width - col1.getColumn().getWidth());
					table.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table 
					// bigger first and then make the columns wider
					// to match the client area width
					table.setSize(area.width, area.height);
					col1.getColumn().setWidth(width/3);
					col2.getColumn().setWidth(width/4);
					col3.getColumn().setWidth(width - col1.getColumn().getWidth()- col2.getColumn().getWidth());
				}
			}
		});
	}

	/**
	 * Sets the focus.
	 */
	@Focus
	public void setFocus() {
		if(treeViewer!=null)
			treeViewer.getTree().setFocus();
	}

	/**
	 * Gets the status event.
	 *
	 * @param part the part
	 * @return the status event
	 */
	@Inject @Optional
	public void  getStatusEvent(@UIEventTopic(WaveformViewer.ACTIVE_WAVEFORMVIEW) WaveformViewer part) {
		this.waveformViewerPart=part;
	}

	class ViewSettings {
		public ViewSettings(List<String> names, TreePath[] paths) {
			super();
			this.names = names;
			this.paths = paths;
		}
		public List<String> names;
		public TreePath[] paths;
	}
	HashMap<Integer, ViewSettings> settings = new HashMap<>();
	
	public void setInput(Object object) {
		if(object instanceof ITx){
			Object oldInput = treeViewer.getInput();
			if(oldInput!=null) {
				final Integer hash = getAttrNameHash(oldInput);
				final List<String> names = getTopItemHier(treeViewer.getTree().getTopItem());
				final TreePath[] paths = treeViewer.getInput()!=null?treeViewer.getExpandedTreePaths():null;
				settings.put(hash, new ViewSettings(names, paths));
			}
			treeViewer.setInput(object);
			final Integer newHash = getAttrNameHash(object);
			final ViewSettings newSettings = settings.get(newHash);
			if(newSettings!=null) {
				setExpandedState(newSettings.paths);
				setTopItemFromHier(newSettings.names, treeViewer.getTree().getItems());
			} else
				setExpandedState(null);
		} else {
			treeViewer.setInput(null);
		}

	}

	int getAttrNameHash(Object o) {
		if(o instanceof ITx) {
			ITx tx = (ITx) o;
			List<String> attr_names = tx.getAttributes().stream().map(a -> a.getName()).collect(Collectors.toList());
			return Objects.hash(attr_names);
		} else
			return o.hashCode();

	}
	
	private void setExpandedState(TreePath[] paths) {
		if(paths==null)
			treeViewer.setAutoExpandLevel(2);
		else {
			TransactionTreeContentProvider cp = (TransactionTreeContentProvider) treeViewer.getContentProvider();
			Object[] elems = cp.getElements(treeViewer.getInput());
			for(TreePath path: paths) {
				TreeNode firstSeg = (TreeNode)path.getFirstSegment();
				for(Object elem : elems) {
					if(((TreeNode)elem).type == firstSeg.type) {
						treeViewer.setExpandedState(elem, true);
						if(firstSeg.type==TransactionDetails.Type.ATTRS && path.getSegmentCount()>1)
							expandSubNodes(path, 1, (TreeNode)elem);
						break;
					}
				}
			}
		}
	}

	private void expandSubNodes(TreePath path, int level, TreeNode elem) {
		if(level==path.getSegmentCount()) return;
		TransactionTreeContentProvider cp = (TransactionTreeContentProvider) treeViewer.getContentProvider();
		Object[] childs = cp.getChildren(elem);
		TreeNode nextSeg = (TreeNode)path.getSegment(level);
		for(Object child:childs) {
			if(child instanceof TreeNode) {
				TreeNode node = (TreeNode) child;
				if(nextSeg.toString().equals(node.toString())) {
					treeViewer.setExpandedState(node, true);
					expandSubNodes(path, level+1, node);
					break;
				}
			}
		}
	}

	List<String> getTopItemHier(TreeItem node){
		if(node == null) {
			return new ArrayList<String>();
		} else {
			List<String> elems = getTopItemHier(node.getParentItem());
			elems.add(node.getText(0));
			return elems;
		}
	}
	
	private void setTopItemFromHier(List<String> names, TreeItem [] items) {
		for (TreeItem item : items) { // find item from category
			if(item.getText(0).equals(names.get(0))) {
				if(names.size()==1 || item.getItemCount()==0) {
					treeViewer.getTree().setTopItem(item);
				} else {
					setTopItemFromHier(names.subList(1,  names.size()), item.getItems());
				}
				return;
			}
		}
	}
	
	/**
	 * Sets the selection.
	 *
	 * @param selection the new selection
	 */
	@Inject
	public void setSelection(@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection, EPartService partService){
		// only react if selection is actually from the WaveformViewer and nothing else
		MPart part = partService.getActivePart();
		if( part == null || ! (part.getObject() instanceof WaveformViewer ) )
			return;
		if(treeViewer!=null && selection!=null && !treeViewer.getTree().isDisposed()){
			if( selection instanceof IStructuredSelection && !selection.isEmpty()) {
				setInput(((IStructuredSelection)selection).getFirstElement());		
			}
		}
	}

	/**
	 * Time to string.
	 *
	 * @param time the time
	 * @return the string
	 */
	String timeToString(Long time){
		return waveformViewerPart.getScaledTime(time);
	}

	/**
	 * Tx to string.
	 *
	 * @param tx the tx
	 * @return the string
	 */
	String txToString(ITx tx){
		StringBuilder sb = new StringBuilder();
		sb.append("tx#").append(tx.getId()).append("[").append(timeToString(tx.getBeginTime())); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" - ").append(timeToString(tx.getEndTime())).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return sb.toString();
	}

	/**
	 * The Class TxAttributeViewerSorter.
	 */
	class TxAttributeViewerSorter extends ViewerComparator {

		/** The Constant ASCENDING. */
		private static final int ASCENDING = 0;

		/** The Constant DESCENDING. */
		private static final int DESCENDING = 1;

		/** The column. */
		private int column;

		/** The direction. */
		private int direction;

		/**
		 * Does the sort. If it's a different column from the previous sort, do an
		 * ascending sort. If it's the same column as the last sort, toggle the sort
		 * direction.
		 *
		 * @param column the column
		 */
		public void doSort(int column) {
			if (column == this.column) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an ascending sort
				this.column = column;
				direction = ASCENDING;
			}
		}

		/**
		 * Compares the object for sorting.
		 *
		 * @param viewer the viewer
		 * @param e1 the e1
		 * @param e2 the e2
		 * @return the int
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			int rc = 0;
			if(e1 instanceof ITxAttribute && e2 instanceof ITxAttribute){
				ITxAttribute p1 = (ITxAttribute) e1;
				ITxAttribute p2 = (ITxAttribute) e2;
				// Determine which column and do the appropriate sort
				switch (column) {
				case COLUMN_FIRST:
					rc = getComparator().compare(p1.getName(), p2.getName());
					break;
				case COLUMN_SECOND:
					rc = getComparator().compare(p1.getDataType().name(), p2.getDataType().name());
					break;
				case COLUMN_THIRD:
					rc = getComparator().compare(p1.getValue().toString(), p2.getValue().toString());
					break;
				}
				// If descending order, flip the direction
				if (direction == DESCENDING) rc = -rc;
			}
			return rc;
		}
	}

	/**
	 * The Class TxAttributeFilter.
	 */
	class TxAttributeFilter extends ViewerFilter {

		/** The search string. */
		private String searchString;

		/**
		 * Sets the search text.
		 *
		 * @param s the new search text
		 */
		public void setSearchText(String s) {
			this.searchString = ".*" + s + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {

			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			if(element instanceof TreeNode) {
				return true;
			}
			if(element instanceof ITxAttribute){
				try {
					return (((ITxAttribute) element).getName().toLowerCase().matches(searchString.toLowerCase()));
				} catch (PatternSyntaxException e) {
					return true;
				}
			} 
			if(element instanceof Object[]) {
				try {
					return (((Object[])element)[0]).toString().toLowerCase().matches(searchString.toLowerCase());	
				} catch (PatternSyntaxException e) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * The Enum Type.
	 */
	enum Type {
		PROPS,  /** The props. */
		ATTRS,  /** The attrs. */
		IN_REL, /** The in rel. */ 
		OUT_REL,/** The out rel. */
		HIER
		}

	/**
	 * The Class TreeNode.
	 */
	class TreeNode implements Comparable<TreeNode>{

		/** The type. */
		public Type type;

		/** The element. */
		public ITx element;

		private String hier_path;
		/**
		 * Instantiates a new tree node.
		 *
		 * @param element the element
		 * @param type the type
		 */
		public TreeNode(ITx element, Type type){
			this.element=element;
			this.type=type;
			this.hier_path="";
		}

		public TreeNode(ITx element, String path){
			this.element=element;
			this.type=Type.HIER;
			this.hier_path=path;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString(){
			switch(type){
			case PROPS:      return Messages.TransactionDetails_10;
			case ATTRS:	     return Messages.TransactionDetails_11;
			case IN_REL:     return Messages.TransactionDetails_12;
			case OUT_REL:    return Messages.TransactionDetails_13;
			case HIER:{
				String[] tokens = hier_path.split("\\.");
				return tokens[tokens.length-1];
			}
			}
			return ""; //$NON-NLS-1$
		}
		
		public Object[] getAttributeListForHier() {
			if(childs==null) {
				Map<String, Object> res = element.getAttributes().stream()
				.filter(txAttr -> txAttr.getName().startsWith(hier_path))
				.map(txAttr -> {
					String target = hier_path.length()==0?txAttr.getName():txAttr.getName().replace(hier_path+'.', "");
					String[] tokens = target.split("\\.");
					if(tokens.length==1)
						return new AbstractMap.SimpleEntry<>(tokens[0], txAttr);
					else 
						return new AbstractMap.SimpleEntry<>(tokens[0], new TreeNode(element, hier_path.length()>0?hier_path+"."+tokens[0]:tokens[0]));
				})
				.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue(), (first, second) -> first));
				childs = new TreeMap<String, Object>(res).values().toArray();
			}
			return childs;
		}
		
		private Object[] childs=null;

		@Override
		public boolean equals(Object o) {
			if(o instanceof TreeNode) {
				TreeNode t = (TreeNode) o;
				return type==t.type && hier_path.equals(t.hier_path); 
			}
			return false;
		}
		
		@Override
		public int compareTo(TreeNode o) {
			int res1 = type.compareTo(o.type);
			if(res1==0) {
				return hier_path.compareTo(o.hier_path);
			} else
				return res1;
		}
	}

	/**
	 * The Class TransactionTreeContentProvider.
	 */
	class TransactionTreeContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		@Override
		public void dispose() {	}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
		 */
		@Override
		public Object[] getElements(Object element) {
			return new Object[]{
					new TreeNode((ITx)element, Type.PROPS),  
					new TreeNode((ITx)element, Type.ATTRS),  
					new TreeNode((ITx)element, Type.IN_REL),
					new TreeNode((ITx)element, Type.OUT_REL)
			};
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		@Override
		public Object[] getChildren(Object element) {
			if(element instanceof TreeNode){
				TreeNode propertyHolder=(TreeNode) element;
				if(propertyHolder.type == Type.PROPS){
					return new Object[][]{
						{Messages.TransactionDetails_1, Messages.TransactionDetails_16, propertyHolder.element.getStream().getFullName()},
						{Messages.TransactionDetails_2, Messages.TransactionDetails_16, propertyHolder.element.getGenerator().getName()},
						{Messages.TransactionDetails_19, Messages.TransactionDetails_20, timeToString(propertyHolder.element.getBeginTime())},
						{Messages.TransactionDetails_21, Messages.TransactionDetails_20, timeToString(propertyHolder.element.getEndTime())}
					};
				}else if(propertyHolder.type == Type.ATTRS || propertyHolder.type == Type.HIER)
					return propertyHolder.getAttributeListForHier();
				else if(propertyHolder.type == Type.IN_REL){
					Vector<Object[] > res = new Vector<>();
					for(ITxRelation rel:propertyHolder.element.getIncomingRelations()){
						res.add(new Object[]{
								rel.getRelationType(), 
								rel.getSource().getGenerator().getName(), 
								rel.getSource()});
					}
					return res.toArray();
				} else if(propertyHolder.type == Type.OUT_REL){
					Vector<Object[] > res = new Vector<>();
					for(ITxRelation rel:propertyHolder.element.getOutgoingRelations()){
						res.add(new Object[]{
								rel.getRelationType(), 
								rel.getTarget().getGenerator().getName(), 
								rel.getTarget()});
					}
					return res.toArray();
				}
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		@Override
		public Object getParent(Object element) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element)!=null;
		}

	}

	/**
	 * The Class AttributeLabelProvider.
	 */
	class AttributeLabelProvider extends LabelProvider implements IStyledLabelProvider {

		/** The field. */
		final int field;

		/** The Constant NAME. */
		public static final int NAME=0;

		/** The Constant TYPE. */
		public static final int TYPE=1;

		/** The Constant VALUE. */
		public static final int VALUE=2;

		/**
		 * Instantiates a new attribute label provider.
		 *
		 * @param field the field
		 */
		public  AttributeLabelProvider(int field) {
			this.field=field;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
		 */
		@Override
		public StyledString getStyledText(Object element) {
			switch(field){
			case NAME:
				if (element instanceof ITxAttribute) {
					ITxAttribute attribute = (ITxAttribute) element;
					String[] tokens = attribute.getName().split("\\.");
					return new StyledString(tokens[tokens.length-1]);
				}else if (element instanceof ITxRelation) {
					return new StyledString(Messages.TransactionDetails_4);
				}else if(element instanceof Object[]){
					Object[] elements = (Object[]) element;
					return new StyledString(elements[field].toString());
				} else 
					return new StyledString(element.toString());
			case TYPE:
				if (element instanceof ITxAttribute) {
					ITxAttribute attribute = (ITxAttribute) element;
					return new StyledString(attribute.getDataType().toString());
				}else if(element instanceof Object[]){
					Object[] elements = (Object[]) element;
					return new StyledString(elements[field].toString());
				}else 
					return new StyledString("");					 //$NON-NLS-1$
			default:
				if (element instanceof ITxAttribute) {
					ITxAttribute attribute = (ITxAttribute) element;
					String value = attribute.getValue().toString();
					if((DataType.UNSIGNED == attribute.getDataType() || DataType.INTEGER==attribute.getDataType()) && !"0".equals(value)) {
						try {
							value += " [0x"+Long.toHexString(Long.parseLong(attribute.getValue().toString()))+"]";
						} catch(NumberFormatException e) { }
					}
					return new StyledString(value);
				}else if(element instanceof Object[]){
					Object[] elements = (Object[]) element;
					Object o = elements[field];
					if(o instanceof ITx) {
						ITx tx = (ITx)o;
						return new StyledString(txToString(tx)+" ("+tx.getStream().getFullName()+")");
					} else
						return new StyledString(o.toString());
				} else if(element instanceof ITx){
					return new StyledString(txToString((ITx) element));
				}else 
					return new StyledString("");					 //$NON-NLS-1$
			}
		}
	}
}

