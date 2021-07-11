package com.minres.scviewer.e4.application.parts;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;

import com.minres.scviewer.e4.application.Constants;

public class FileBrowserDialog extends TrayDialog {

	private Image folderImage;

	private Image fileImage;

	private Image dbImage;

	File currentDirFile;

	TreeViewer dirTreeViewer;

	TableViewer tableViewer;
	
	Text fileNameEntry;
	
	Combo filterCombo;
	
	FileTableComparator fileTableComparator;
	
    private FileGlobber globber = new FileGlobber();
    
    private FileGlobber imageGlobber = new FileGlobber();

    private File selectedDir;
    
    private List<File> selectedFiles;
    
    String[] filterStrings = new String[] {"*"};
        
	public FileBrowserDialog(Shell parentShell) {
		super(parentShell);
		folderImage=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/folder.png"); //$NON-NLS-1$
		dbImage=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/database.png"); //$NON-NLS-1$
		fileImage=ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/page_white.png"); //$NON-NLS-1$
		currentDirFile = new File(".");
	}

	public void setFilterExtensions(String[] filterStrings) {
		if(filterStrings.length==0){
			globber = new FileGlobber();
		} else {
			globber= new FileGlobber(filterStrings[0]);
			imageGlobber = new FileGlobber(filterStrings[0]);
			if(filterCombo!=null) {
			filterCombo.setItems(filterStrings);
			filterCombo.select(0);
			filterCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			}
		}
		this.filterStrings=filterStrings;
	}
	
	public List<File> getSelectedFiles(){
		return selectedFiles;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control ret = super.createContents(parent);
		setDirSelection(currentDirFile.getAbsoluteFile().getParentFile());
		getButton(IDialogConstants.OK_ID).setEnabled(!tableViewer.getSelection().isEmpty());
		if(parent instanceof Shell) {
			Point size = ((Shell)parent).computeSize(SWT.DEFAULT, SWT.DEFAULT);
			((Shell)parent).setSize(size.x, 400);
			((Shell)parent).setText("Select database");
		}
		return ret;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		final SashForm sashForm = new SashForm(area, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		dirTreeViewer = new TreeViewer(sashForm);
		dirTreeViewer.setContentProvider(new FileTreeContentProvider());
		dirTreeViewer.setLabelProvider(new FileTreeLabelProvider());
		dirTreeViewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = event.getStructuredSelection();
			File entry = (File) sel.getFirstElement();
			if(entry!=null && entry.isDirectory()) {
				selectedDir = entry;
				tableViewer.setInput(selectedDir.listFiles());
			}
		});
		dirTreeViewer.setInput("root");
		
		final Composite tableViewerParent = new Composite(sashForm, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, true);
		gridLayout.horizontalSpacing=0;
		gridLayout.verticalSpacing=5;
		gridLayout.marginHeight=0;
		gridLayout.marginHeight=0;
		tableViewerParent.setLayout(gridLayout);
		final ToolBar toolBar = new ToolBar(tableViewerParent, SWT.HORIZONTAL |SWT.SHADOW_OUT);
		toolBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
	    final ToolItem toolbarItemUp = new ToolItem(toolBar, SWT.PUSH);
	    toolbarItemUp.setToolTipText("up one level");
	    toolbarItemUp.setImage(ResourceManager.getPluginImage(Constants.PLUGIN_ID, "icons/arrow_up.png")); //$NON-NLS-1$
	    toolbarItemUp.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(selectedDir.getParentFile()!=null) {
					selectedDir=selectedDir.getParentFile();
					tableViewer.setInput(selectedDir.listFiles());
				}
			}
		});
		tableViewer = new TableViewer(tableViewerParent, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tableViewer.getTable().setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		tableViewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = event.getStructuredSelection();
			getButton(IDialogConstants.OK_ID).setEnabled(!sel.isEmpty());
			@SuppressWarnings("unchecked")
			Object text = sel.toList().stream().map(e -> ((File)e).getName()).collect(Collectors.joining(";"));
			fileNameEntry.setText(text.toString());
		});
		tableViewer.addDoubleClickListener(event -> {
			IStructuredSelection sel = tableViewer.getStructuredSelection();
			if(sel.isEmpty()) return;
			if(sel.size()==1) {
				File elem = (File) sel.getFirstElement();
				if(globber.matches(elem))
					buttonPressed(IDialogConstants.OK_ID);
				else if(elem.isDirectory())
					setDirSelection(elem);
			} else 
				buttonPressed(IDialogConstants.OK_ID);
		});
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) { mouseUp(e); }
			@Override
			public void mouseUp(MouseEvent e) {
				final Table table = tableViewer.getTable();
				TableItem element = table.getItem(new Point(e.x, e.y));
				if (element == null )//&& (e.stateMask&SWT.MODIFIER_MASK)!=0)
					table.deselectAll();
				else {
					int[] indices = table.getSelectionIndices();
					if(indices.length==1) {
						TableItem ti = table.getItem(indices[0]);
						if(!globber.matches(ti.getData()) && !((File)ti.getData()).isDirectory())
							table.deselect(indices[0]);
					} else {
						for (int idx : indices) {
							TableItem ti = table.getItem(idx);
							if(!globber.matches(ti.getData()))
								table.deselect(idx);
						}
					}
				}
			}
		});
		TableViewerColumn colName = new TableViewerColumn(tableViewer, SWT.NONE);
		colName.setLabelProvider(new FileTableLabelProvider() {
			@Override public String getText(Object element) { return ((File) element).getName(); }
			@Override public Image getImage(Object element){
				if(imageGlobber.matches(element)) return dbImage;
				return ((File) element).isDirectory()?folderImage:fileImage; 
			}
		});
		colName.getColumn().setWidth(300);
		colName.getColumn().setText("Name");
		colName.getColumn().addSelectionListener(getSelectionAdapter(colName.getColumn(), 0));

		TableViewerColumn colSize = new TableViewerColumn(tableViewer, SWT.RIGHT);
		colSize.setLabelProvider(new FileTableLabelProvider() {
			@Override public String getText(Object element) { return String.format("%d", ((File) element).length()); }
		});
		colSize.getColumn().setWidth(100);
		colSize.getColumn().setText("Size");
		colSize.getColumn().addSelectionListener(getSelectionAdapter(colSize.getColumn(), 1));
        
		TableViewerColumn colDate = new TableViewerColumn(tableViewer, SWT.RIGHT);
		colDate.setLabelProvider(new FileTableLabelProvider() {
			@Override public String getText(Object element) { return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(((File) element).lastModified()); }
		});
		colDate.getColumn().setWidth(200);
		colDate.getColumn().setText("Modification Date");
		colDate.getColumn().addSelectionListener(getSelectionAdapter(colDate.getColumn(), 2));
        
		TableViewerColumn colEmpty = new TableViewerColumn(tableViewer, SWT.CENTER);
		colEmpty.setLabelProvider(new FileTableLabelProvider() {
			@Override public String getText(Object element) {	return ""; }
		});
		colEmpty.getColumn().setText("");
		
		fileTableComparator = new FileTableComparator();
		tableViewer.setComparator(fileTableComparator);
		tableViewer.addFilter(new FileTableFilter());
		
		Composite bottomBar = new Composite(tableViewerParent, SWT.NONE);
		bottomBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		GridLayout gridLayoutBottom = new GridLayout(2, false);
		gridLayoutBottom.horizontalSpacing=0;
		gridLayoutBottom.verticalSpacing=0;
		gridLayoutBottom.marginHeight=0;
		gridLayoutBottom.marginWidth=0;
		bottomBar.setLayout(gridLayoutBottom);
		
		fileNameEntry = new Text(bottomBar, SWT.BORDER);
		fileNameEntry.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fileNameEntry.setEditable(false);
		fileNameEntry.setEnabled(false);
		
		filterCombo = new Combo(bottomBar, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		filterCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		filterCombo.setItems(filterStrings);
		filterCombo.select(0);
		filterCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				globber= new FileGlobber(filterCombo.getText());
				tableViewer.setInput(selectedDir.listFiles());
			}
		});
		sashForm.setWeights(new int[]{2, 3});
		return area;
	}

    private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	fileTableComparator.setColumn(index);
                int dir = fileTableComparator.getDirection();
                tableViewer.getTable().setSortDirection(dir);
                tableViewer.getTable().setSortColumn(column);
                tableViewer.refresh();
            }
        };
    }

    private void setDirSelection(File f) {
    	ArrayList<File> fileTree = getParentDirList(f);
    	TreeSelection selection = new TreeSelection(new TreePath(fileTree.toArray()));
    	dirTreeViewer.setSelection(selection, true);
    }

	private ArrayList<File> getParentDirList(File actual){
		if(actual==null)
			return new ArrayList<>();
		else {
			ArrayList<File> l = getParentDirList(actual.getParentFile());
			l.add(actual);
			return l;
		}
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	// save content of the Text fields because they get disposed
	// as soon as the Dialog closes
	@SuppressWarnings("unchecked")
	private void saveInput() {
		selectedFiles= tableViewer.getStructuredSelection().toList();
	}

	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

	class FileGlobber {
		
		List<PathMatcher> matchers;
		
		public FileGlobber() {
			matchers = new ArrayList<>();		}
		
		public FileGlobber(String expr) {
			ArrayList<PathMatcher> m = new ArrayList<>();
			if(expr.length()>0) {
				String[] tok = expr.split(";");
				for (String string : tok) {
					m.add(FileSystems.getDefault().getPathMatcher("glob:**/"+string));
				}
			}
			matchers = m;
		}
		
		public boolean matches(Object f) {
			if(f instanceof File) {
				if(matchers.isEmpty()) return true;
				for (PathMatcher m : matchers) {
					try {
					if(m.matches(((File)f).toPath())) return true;
					} catch (Exception e) {
						return false;
					}
				}
			}
			return false;
		}
	}
	
	class FileTreeContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object arg0) {
			File[] entries = ((File) arg0).listFiles();
			if(entries != null) {
				List<File> res = Arrays.stream(entries)
						.filter(file -> !(file.isFile()||file.getName().startsWith(".") ||globber.matches(file)))
						.sorted( (f1, f2) -> f1.getName().compareTo(f2.getName()))
						.collect(Collectors.toList());
						return res.toArray();
			} else
				return new Object[0];
		}

		public Object getParent(Object arg0) {
			return ((File) arg0).getParentFile();
		}

		public boolean hasChildren(Object arg0) {
			Object[] obj = getChildren(arg0);
			return obj != null && obj.length > 0;
		}

		public Object[] getElements(Object arg0) {
			return File.listRoots();
		}

	}

	class FileTreeLabelProvider implements ILabelProvider {
		private List<ILabelProviderListener> listeners;

		public FileTreeLabelProvider() {
			listeners = new ArrayList<>();
		}

		public Image getImage(Object arg0) {
			return ((File) arg0).isDirectory() ? folderImage : fileImage;
		}

		public String getText(Object arg0) {
			File f = (File)arg0;
			return f.getName().length() == 0? f.getPath() : f.getName();
		}

		public void addListener(ILabelProviderListener arg0) {
			listeners.add(arg0);
		}

		@Override
		public void dispose() {
			// nothing to ispose
		}

		public boolean isLabelProperty(Object arg0, String arg1) {
			return true;
		}

		public void removeListener(ILabelProviderListener arg0) {
			listeners.remove(arg0);
		}

	}
	
	public class FileTableFilter extends ViewerFilter {

	    @Override
	    public boolean select(Viewer viewer, Object parentElement, Object element) {
	        File p = (File) element;
	        return !p.getName().startsWith(".");
	    }
	}
	
	public class FileTableComparator extends ViewerComparator {
	    private int propertyIndex = 0;
	    private boolean descending = false;

	    public int getDirection() {
	        return descending ? SWT.DOWN : SWT.UP;
	    }

	    public void setColumn(int column) {
	    	descending = column == this.propertyIndex && !descending;
	    	this.propertyIndex = column;
	    }

	    @Override
	    public int compare(Viewer viewer, Object e1, Object e2) {
	        File p1 = (File) e1;
	        File p2 = (File) e2;
	        int rc = 0;
	        switch (propertyIndex) {
	        case 0:
	            rc = p1.getName().compareTo(p2.getName());
	            break;
	        case 1:
	            rc = Long.valueOf(p1.length()).compareTo(p2.length());
	            break;
	        case 2:
	            rc = Long.valueOf(p1.lastModified()).compareTo(p2.lastModified());
	            break;
	        default:
	            rc = 0;
	        }
	        // If descending order, flip the direction
	        return descending? -rc : rc;
	    }

	}
	
	public class FileTableLabelProvider extends  ColumnLabelProvider {
		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			return globber.matches(element) || ((File)element).isDirectory()? null: SWTResourceManager.getColor(SWT.COLOR_GRAY);
		}

	}
}

