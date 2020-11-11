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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.parts.txTableTree.AttributeLabelProvider;
import com.minres.scviewer.e4.application.parts.txTableTree.AbstractTransactionTreeContentProvider;
import com.minres.scviewer.e4.application.parts.txTableTree.TransactionTreeNode;
import com.minres.scviewer.e4.application.parts.txTableTree.TransactionTreeNodeType;
import com.minres.scviewer.e4.application.parts.txTableTree.TxAttributeFilter;
import com.minres.scviewer.e4.application.parts.txTableTree.TxAttributeViewerSorter;
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

	private Composite top;
	
	/**
	 * Creates the composite.
	 *
	 * @param parent the parent
	 */
	@PostConstruct
	public void createComposite(final Composite parent, @Optional WaveformViewer waveformViewerPart) {
		this.waveformViewerPart=waveformViewerPart;
		
		top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(1, false));

		nameFilter = new Text(top, SWT.BORDER);
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

		treeViewer = new TreeViewer(top);
		treeViewer.setContentProvider(new AbstractTransactionTreeContentProvider(waveformViewerPart) {
			@Override
			public Object[] getElements(Object element) {
				return new Object[]{
						new TransactionTreeNode((ITx)element, TransactionTreeNodeType.PROPS),  
						new TransactionTreeNode((ITx)element, TransactionTreeNodeType.ATTRS),  
						new TransactionTreeNode((ITx)element, TransactionTreeNodeType.IN_REL),
						new TransactionTreeNode((ITx)element, TransactionTreeNodeType.OUT_REL)
				};
			}
		});
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
		col1.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewerPart, AttributeLabelProvider.NAME)));
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
		col2.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewerPart, AttributeLabelProvider.TYPE)));
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
		col3.setLabelProvider(new DelegatingStyledCellLabelProvider(new AttributeLabelProvider(waveformViewerPart, AttributeLabelProvider.VALUE)));
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
		top.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Tree table = treeViewer.getTree();
				Rectangle area = top.getClientArea();
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

	public Control getControl() {
		return top;
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
			AbstractTransactionTreeContentProvider cp = (AbstractTransactionTreeContentProvider) treeViewer.getContentProvider();
			Object[] elems = cp.getElements(treeViewer.getInput());
			for(TreePath path: paths) {
				TransactionTreeNode firstSeg = (TransactionTreeNode)path.getFirstSegment();
				for(Object elem : elems) {
					if(((TransactionTreeNode)elem).type == firstSeg.type) {
						treeViewer.setExpandedState(elem, true);
						if(firstSeg.type==TransactionTreeNodeType.ATTRS && path.getSegmentCount()>1)
							expandSubNodes(path, 1, (TransactionTreeNode)elem);
						break;
					}
				}
			}
		}
	}

	private void expandSubNodes(TreePath path, int level, TransactionTreeNode elem) {
		if(level==path.getSegmentCount()) return;
		AbstractTransactionTreeContentProvider cp = (AbstractTransactionTreeContentProvider) treeViewer.getContentProvider();
		Object[] childs = cp.getChildren(elem);
		TransactionTreeNode nextSeg = (TransactionTreeNode)path.getSegment(level);
		for(Object child:childs) {
			if(child instanceof TransactionTreeNode) {
				TransactionTreeNode node = (TransactionTreeNode) child;
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
		if( part == null || ! (part.getObject() instanceof WaveformViewer )  || part.getObject() != waveformViewerPart)
			return;
		if(treeViewer!=null && selection!=null && !treeViewer.getTree().isDisposed()){
			if( selection instanceof IStructuredSelection && !selection.isEmpty()) {
				setInput(((IStructuredSelection)selection).getFirstElement());		
			}
		}
	}
}

