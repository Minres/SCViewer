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
package com.minres.scviewer.e4.application.internal.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;

import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IHierNode;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.parts.LoadingWaveformDb;
import com.minres.scviewer.e4.application.provider.TxDbContentProvider;
import com.minres.scviewer.e4.application.provider.TxDbLabelProvider;

/**
 * The Class DesignBrowser. It contains the design tree, a list of Streams & signals and a few buttons to 
 * add them them to the waveform view 
 */
public abstract class AbstractDesignBrowser {

	public TreeViewer getDbTreeViewer() {
		return dbTreeViewer;
	}

	public TableViewer getStreamTableViewer() {
		return streamTableViewer;
	}

	/** The Constant POPUP_ID. */
	private static final String POPUP_ID="com.minres.scviewer.e4.application.parts.DesignBrowser.popupmenu"; //$NON-NLS-1$

	/** The event broker. */
	@Inject IEventBroker eventBroker;
	
	/** The selection service. */
	@Inject	ESelectionService selectionService;

	/** The menu service. */
	@Inject EMenuService menuService;

	/** The eclipse ctx. */
	@Inject IEclipseContext eclipseCtx;
	
	/** The sash form. */
	private SashForm sashForm;
	
	/** The top. */
	Composite top;

	/** The bottom. */
	private Composite bottom;
	
	/** The tree viewer. */
	private TreeViewer dbTreeViewer;

	/** The name filter of the design browser tree. */
	private Text treeNameFilter;

	/** The attribute filter. */
	StreamTTreeFilter treeAttributeFilter;

	/** The name filter. */
	private Text tableNameFilter;

	/** The attribute filter. */
	StreamTableFilter tableAttributeFilter;

	/** The tx table viewer. */
	protected TableViewer streamTableViewer;

	/** The append all item. */
	protected ToolItem appendItem, insertItem;

	/** The other selection count. */
	int thisSelectionCount=0, otherSelectionCount=0;

	IWaveformDb waveformDb=null;

	/** The tree viewer pcl. */
	private PropertyChangeListener treeViewerPCL = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if("CHILDS".equals(evt.getPropertyName())){ //$NON-NLS-1$
				dbTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {					
					@Override
					public void run() {
						dbTreeViewer.refresh();
					}
				});
			}
		}
	};

	/** The sash paint listener. */
	protected PaintListener sashPaintListener=new PaintListener() {					
		@Override
		public void paintControl(PaintEvent e) {
			int size=Math.min(e.width, e.height)-1;
			e.gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
			e.gc.setFillRule(SWT.FILL_EVEN_ODD);
			if(e.width>e.height)
				e.gc.drawArc(e.x+(e.width-size)/2, e.y, size, size, 0, 360);
			else
				e.gc.drawArc(e.x, e.y+(e.height-size)/2, size, size, 0, 360);
		}
	};

	
	/**
	 * Creates the composite.
	 *
	 * @param parent the parent
	 */
	@PostConstruct
	public void createComposite(Composite parent) {
		sashForm = new SashForm(parent, SWT.BORDER | SWT.SMOOTH | SWT.VERTICAL);

		top = new Composite(sashForm, SWT.NONE);
		createTreeViewerComposite(top);
		bottom = new Composite(sashForm, SWT.NONE);
		createTableComposite(bottom);
		
		sashForm.setWeights(new int[] {100, 100});
		sashForm.SASH_WIDTH=5;
		top.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				sashForm.getChildren()[2].addPaintListener(sashPaintListener);
				top.removeControlListener(this);
			}
		});
	}
	
	/**
	 * Creates the tree viewer composite.
	 *
	 * @param parent the parent
	 */
	public void createTreeViewerComposite(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		treeNameFilter = new Text(parent, SWT.BORDER);
		treeNameFilter.setMessage(Messages.DesignBrowser_3);
		treeNameFilter.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				treeAttributeFilter.setSearchText(((Text) e.widget).getText());
				dbTreeViewer.refresh();
			}
		});
		treeNameFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		treeAttributeFilter = new StreamTTreeFilter();

		dbTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		dbTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		dbTreeViewer.setContentProvider(new TxDbContentProvider());
		dbTreeViewer.setLabelProvider(new TxDbLabelProvider());
		dbTreeViewer.addFilter(treeAttributeFilter);
		dbTreeViewer.setUseHashlookup(true);
		dbTreeViewer.setAutoExpandLevel(2);
		dbTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection=event.getSelection();
				if( selection instanceof IStructuredSelection) { 
					Object object= ((IStructuredSelection)selection).getFirstElement();			
					if(object instanceof IHierNode && ((IHierNode)object).getChildNodes().size()!=0){
						streamTableViewer.setInput(object);
						updateButtons();
					}
					else { //if selection is changed but empty
						streamTableViewer.setInput(null);
						updateButtons();
					}
				}
			}
		});
	}

	/**
	 * Creates the table composite.
	 *
	 * @param parent the parent
	 */
	public void createTableComposite(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		tableNameFilter = new Text(parent, SWT.BORDER);
		tableNameFilter.setMessage(Messages.DesignBrowser_2);
		tableNameFilter.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				tableAttributeFilter.setSearchText(((Text) e.widget).getText());
				updateButtons();
				streamTableViewer.refresh();
			}
		});
		tableNameFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		tableAttributeFilter = new StreamTableFilter();

		streamTableViewer = new TableViewer(parent);
		streamTableViewer.setContentProvider(new TxDbContentProvider(true));
		streamTableViewer.setLabelProvider(new TxDbLabelProvider());
		streamTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		streamTableViewer.addFilter(tableAttributeFilter);
		streamTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectionService.setSelection(event.getSelection());
				updateButtons();
			}
		});
		menuService.registerContextMenu(streamTableViewer.getControl(), POPUP_ID);

		ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		toolBar.setBounds(0, 0, 87, 20);

		appendItem = new ToolItem(toolBar, SWT.NONE);
		appendItem.setToolTipText(Messages.DesignBrowser_4);
		appendItem.setImage(ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/append_wave.png")); //$NON-NLS-1$ //$NON-NLS-2$
		appendItem.setEnabled(false);

		insertItem = new ToolItem(toolBar, SWT.NONE);
		insertItem.setToolTipText(Messages.DesignBrowser_8);
		insertItem.setImage(ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/insert_wave.png")); //$NON-NLS-1$ //$NON-NLS-2$
		insertItem.setEnabled(false);
	}

	public IWaveformDb getWaveformDb() {
		return waveformDb;
	}

	@SuppressWarnings("unchecked")
	public void setWaveformDb(IWaveformDb waveformDb) {
		this.waveformDb = waveformDb;
		Object input = dbTreeViewer.getInput();
		if(input!=null && input instanceof List<?>){
			IWaveformDb db = ((List<IWaveformDb>)input).get(0);
			if(db==waveformDb) return; // do nothing if old and new database is the same
			((List<IWaveformDb>)input).get(0).removePropertyChangeListener(treeViewerPCL);
		}
		dbTreeViewer.setInput(Arrays.asList(waveformDb.isLoaded()?new IWaveformDb[]{waveformDb}:new IWaveformDb[]{new LoadingWaveformDb()}));
		// Set up the tree viewer
		waveformDb.addPropertyChangeListener(treeViewerPCL);
	}

	/**
	 * Sets the focus.
	 */
	@Focus
	public void setFocus() {
		if(streamTableViewer!=null) {
			streamTableViewer.getTable().setFocus();
			IStructuredSelection selection = (IStructuredSelection)streamTableViewer.getSelection();
			if(selection.size()==0){
				appendItem.setEnabled(false);
			}
			selectionService.setSelection(selection);
			thisSelectionCount=selection.toList().size();
		}
		updateButtons();
	}
	
	/** 
	 * reset tree viewer and tableviewer after every closed tab
	 */
	protected void resetTreeViewer() {
		//reset tree- and tableviewer
		dbTreeViewer.setInput(null);
		streamTableViewer.setInput(null);
		streamTableViewer.setSelection(null);
	}

	public void selectAllWaveforms() {
		int itemCount = streamTableViewer.getTable().getItemCount();
		ArrayList<Object> list = new ArrayList<>();
		for(int i=0; i<itemCount; i++) {
			list.add(streamTableViewer.getElementAt(i));
		}
		StructuredSelection sel = new StructuredSelection(list);
		streamTableViewer.setSelection(sel);
	}
	
	/**
	 * Sets the selection.
	 *
	 * @param selection the selection
	 * @param partService the part service
	 */
	@Inject
	public void setSelection(@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection, EPartService partService){
		MPart part = partService.getActivePart();
		if(part!=null && part.getObject() != this && selection!=null){
			if( selection instanceof IStructuredSelection) {
				Object object= ((IStructuredSelection)selection).getFirstElement();			
				if(object instanceof IHierNode&& ((IHierNode)object).getChildNodes().size()!=0)
					streamTableViewer.setInput(object);
				otherSelectionCount = (object instanceof IWaveform || object instanceof ITx)?1:0;
			}
		}
		updateButtons();
	}

	/**
	 * Initialize the listeners for the buttons.
	 */
	protected abstract void initializeButtonListeners();
	/**
	 * Update buttons.
	 */
	protected abstract void updateButtons();

	/**
	 * The Class StreamTableFilter.
	 */
	public class StreamTableFilter extends ViewerFilter {

		/** The search string. */
		private String searchString;
		private Pattern pattern;

		/**
		 * Sets the search text.
		 *
		 * @param s the new search text
		 */
		public void setSearchText(String s) {
			try {
		        pattern = Pattern.compile(".*" + s + ".*"); //$NON-NLS-1$ //$NON-NLS-2$
		        this.searchString = ".*" + s + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
			} catch (PatternSyntaxException e) {}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			if(element instanceof IWaveform) {
				if (pattern.matcher(((IWaveform) element).getName()).matches())
					return true;
			}
			return false;
		}
	}

	public class StreamTTreeFilter extends ViewerFilter {

		/** The search string. */
		private String searchString;
		private Pattern pattern;

		/**
		 * Sets the search text.
		 *
		 * @param s the new search text
		 */
		public void setSearchText(String s) {
			try {
		        pattern = Pattern.compile(".*" + s + ".*");
		        this.searchString = ".*" + s + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
			} catch (PatternSyntaxException e) {}

		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return selectTreePath(viewer, new TreePath(new Object[] { parentElement }), element);
		}
		
		private boolean selectTreePath(Viewer viewer, TreePath parentPath, Object element) {
			// Cut off children of elements that are shown repeatedly.
			for (int i = 0; i < parentPath.getSegmentCount() - 1; i++) {
				if (element.equals(parentPath.getSegment(i))) {
					return false;
				}
			}

			if (!(viewer instanceof TreeViewer)) {
				return true;
			}
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			TreeViewer treeViewer = (TreeViewer) viewer;
			Boolean matchingResult = isMatchingOrNull(element);
			if (matchingResult != null) {
				return matchingResult;
			}
			return hasUnfilteredChild(treeViewer, parentPath, element);
		}

		Boolean isMatchingOrNull(Object element) {
			if(element instanceof IWaveform) {
				if (pattern.matcher(((IWaveform) element).getName()).matches())
					return Boolean.TRUE;
			} else if(element instanceof IWaveformDb) {
				return Boolean.TRUE;
			} else if(element instanceof HierNode) {
				HierNode n = (HierNode) element;
				try {
					if (pattern.matcher(n.getFullName()).matches())
						return Boolean.TRUE;
				} catch (PatternSyntaxException e) {
					return Boolean.TRUE;
				}
			} else {
				return Boolean.FALSE;
			}
			/* maybe children are matching */
			return null;
		}

		private boolean hasUnfilteredChild(TreeViewer viewer, TreePath parentPath, Object element) {
			TreePath elementPath = parentPath.createChildPath(element);
			IContentProvider contentProvider = viewer.getContentProvider();
			Object[] children = contentProvider instanceof ITreePathContentProvider
					? ((ITreePathContentProvider) contentProvider).getChildren(elementPath)
					: ((ITreeContentProvider) contentProvider).getChildren(element);

			/* avoid NPE + guard close */
			if (children == null || children.length == 0) {
				return false;
			}
			for (int i = 0; i < children.length; i++) {
				if (selectTreePath(viewer, elementPath, children[i])) {
					return true;
				}
			}
			return false;
		}
	}
	/**
	 * Gets the filtered children.
	 *
	 * @param viewer the viewer
	 * @return the filtered children
	 */
	protected Object[] getFilteredChildren(TableViewer viewer){
		Object parent = viewer.getInput();
		if(parent==null) return new Object[0];
		Object[] result = null;
		if (parent != null) {
			IStructuredContentProvider cp = (IStructuredContentProvider) viewer.getContentProvider();
			if (cp != null) {
				result = cp.getElements(parent);
				if(result==null) return new Object[0];
				for (int i = 0, n = result.length; i < n; ++i) {
					if(result[i]==null) return new Object[0];
				}
			}
		}
		ViewerFilter[] filters = viewer.getFilters();
		if (filters != null) {
			for (ViewerFilter f:filters) {
				Object[] filteredResult = f.filter(viewer, parent, result);
				result = filteredResult;
			}
		}
		return result;
	}

	/**
	 * Gets the filtered children.
	 *
	 * @return the filtered children
	 */
	public Object[] getFilteredChildren() {
		return getFilteredChildren(streamTableViewer);
	}

	/**
	 * The Class DBState.
	 */
	class DBState {
		
		/**
		 * Instantiates a new DB state.
		 */
		public DBState() {
			this.expandedElements=dbTreeViewer.getExpandedElements();
			this.treeSelection=dbTreeViewer.getSelection();
			this.tableSelection=streamTableViewer.getSelection();
		}
		
		/**
		 * Apply.
		 */
		public void apply() {
			dbTreeViewer.setExpandedElements(expandedElements);
			dbTreeViewer.setSelection(treeSelection, true);
			streamTableViewer.setSelection(tableSelection, true);
			
		}

		/** The expanded elements. */
		private Object[] expandedElements;
		
		/** The tree selection. */
		private ISelection treeSelection;
		
		/** The table selection. */
		private ISelection tableSelection;
	}
};