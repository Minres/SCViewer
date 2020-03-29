package com.minres.scviewer.e4.application.parts;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wb.swt.ResourceManager;

public class FileBrowserDialog extends TrayDialog {

	private Image folderImage;

	private Image fileImage;

	private Image dbImage;

	File currentDirFile;

	TreeViewer dirTreeViewer;

	TableViewer tableViewer;
	
	FileTableComparator fileTableComparator;
	
    private FileGlobber globber = new FileGlobber();
    
    private List<File> selectedFiles;
    
    String[] filterStrings = new String[] {"*"};
        
	public FileBrowserDialog(Shell parentShell) {
		super(parentShell);
		folderImage=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
		dbImage=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/database.png"); //$NON-NLS-1$ //$NON-NLS-2$
		fileImage=ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/page_white.png"); //$NON-NLS-1$ //$NON-NLS-2$
		currentDirFile = new File(".");
	}

	public void setFilterExtensions(String[] filterStrings) {
		if(filterStrings.length==0){
			globber = new FileGlobber();
		} else
			globber= new FileGlobber(filterStrings[0]);
		this.filterStrings=filterStrings;
	}
	
	public List<File> getSelectedFiles(){
		return selectedFiles;
	}
	
	@Override
	public int open() {
		dirTreeViewer.setInput("root");
		dirTreeViewer.refresh();
		setDirSelection(currentDirFile.getAbsoluteFile().getParentFile());
		getButton(IDialogConstants.OK_ID).setEnabled(!tableViewer.getSelection().isEmpty());
		return super.open();
	}

	@Override
	protected Control createContents(Composite parent) {
		Control ret = super.createContents(parent);
		if(parent instanceof Shell) {
			((Shell)parent).setSize(800, 400);
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
			if(entry.isDirectory()) {
				tableViewer.setInput(entry.listFiles());
			}
		});

		final Composite tableViewerParent = new Composite(sashForm, SWT.BORDER);
		tableViewerParent.setLayout(new GridLayout(1, true));
		tableViewer = new TableViewer(tableViewerParent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		tableViewer.getTable().setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		tableViewer.addSelectionChangedListener(event -> {
			getButton(IDialogConstants.OK_ID).setEnabled(!event.getStructuredSelection().isEmpty());
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
				TableItem element = (TableItem)tableViewer.getTable().getItem(new Point(e.x, e.y));
				final Table table = tableViewer.getTable();
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
				if(globber.matches(element)) return dbImage;
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
        
		TableViewerColumn colEmpty = new TableViewerColumn(tableViewer, SWT.CENTER);
		colEmpty.setLabelProvider(new FileTableLabelProvider() {
			@Override public String getText(Object element) {	return ""; }
		});
		//colEmpty.getColumn().setWidth(200);
		colEmpty.getColumn().setText("");
		
		fileTableComparator = new FileTableComparator();
		tableViewer.setComparator(fileTableComparator);
		sashForm.setWeights(new int[]{2, 3});
		return area;
	}

    private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
        SelectionAdapter selectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	fileTableComparator.setColumn(index);
                int dir = fileTableComparator.getDirection();
                tableViewer.getTable().setSortDirection(dir);
                tableViewer.getTable().setSortColumn(column);
                tableViewer.refresh();
            }
        };
        return selectionAdapter;
    }

    private void setDirSelection(File f) {
    	ArrayList<File> fileTree = getParentDirList(f);
    	TreeSelection selection = new TreeSelection(new TreePath(fileTree.toArray()));
    	dirTreeViewer.setSelection(selection);
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
			assert(f instanceof File);
			if(matchers.size()==0) return true;
			for (PathMatcher m : matchers) {
				if(m.matches(((File)f).toPath())) return true;
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
						.sorted(new Comparator<File>(){
							public int compare(File f1, File f2){return f1.getName().compareTo(f2.getName());} 
							})
						.collect(Collectors.toList());   ;
						return res.toArray();
			} else
				return new Object[0];
		}

		public Object getParent(Object arg0) {
			return ((File) arg0).getParentFile();
		}

		public boolean hasChildren(Object arg0) {
			Object[] obj = getChildren(arg0);
			return obj == null ? false : obj.length > 0;
		}

		public Object[] getElements(Object arg0) {
			return File.listRoots();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}
	}

	class FileTreeLabelProvider implements ILabelProvider {
		private List<ILabelProviderListener> listeners;

		private Image file;

		private Image dir;

		public FileTreeLabelProvider() {
			listeners = new ArrayList<ILabelProviderListener>();
		}

		public Image getImage(Object arg0) {
			return ((File) arg0).isDirectory() ? folderImage : file;
		}

		public String getText(Object arg0) {
			File f = (File)arg0;
			return f.getName().length() == 0? f.getPath() : f.getName();
		}

		public void addListener(ILabelProviderListener arg0) {
			listeners.add(arg0);
		}

		public void dispose() {
			// Dispose the images
			if (dir != null)
				dir.dispose();
			if (file != null)
				file.dispose();
		}

		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		public void removeListener(ILabelProviderListener arg0) {
			listeners.remove(arg0);
		}

	}
	
	public class FileTableComparator extends ViewerComparator {
	    private int propertyIndex = 0;
	    private boolean descending = false;

	    public FileTableComparator() {
	    }

	    public int getDirection() {
	        return descending ? SWT.DOWN : SWT.UP;
	    }

	    public void setColumn(int column) {
	    	descending = column == this.propertyIndex?!descending : false;
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
			return globber.matches(element) || ((File)element).isDirectory()? null: ResourceManager.getColor(SWT.COLOR_GRAY);
		}

	}
}

