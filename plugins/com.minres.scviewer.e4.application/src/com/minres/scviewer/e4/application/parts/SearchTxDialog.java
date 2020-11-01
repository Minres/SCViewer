package com.minres.scviewer.e4.application.parts;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.ResourceManager;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITxEvent;
import com.minres.scviewer.database.ITxStream;

public class SearchTxDialog extends TitleAreaDialog {
	private ComboViewer propNameComboViewer = null; 

	private Text propValueText = null;

	private String propName="";
	private DataType propType=null;
	private String propValue="";
	
	private ITxStream<? extends ITxEvent> stream;
	
	private ConcurrentHashMap<String, DataType> propNames=new ConcurrentHashMap<String, DataType>();
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 * @param iTxStream 
	 */
	public SearchTxDialog(Shell parentShell, ITxStream<? extends ITxEvent> iTxStream) {
		super(parentShell);
		setShellStyle(SWT.BORDER | SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL);
		stream=iTxStream;
		new Thread() {
			public void run() {
				stream.getEvents().values().parallelStream().forEach(evtLst -> {
					evtLst.forEach(evt -> {
						 evt.getTransaction().getAttributes().stream().forEach(attr -> {
							 propNames.put(attr.getName(), attr.getDataType());
						 });
					});
				});
				parentShell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (propNameComboViewer!=null) {
							propNameComboViewer.setInput(getEntries());
							propNameComboViewer.setSelection(new StructuredSelection(propNameComboViewer.getElementAt(0)));
						}
					}
				});
			}
		}.run();
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("Specify property name and value to search for");
		setTitleImage(ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/Minres_logo.png"));
		setTitle("Search Tx in stream");
		final Composite area = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = (GridLayout) area.getLayout();
		gridLayout.marginTop = 10;
		gridLayout.marginBottom = 10;
		final Composite container = new Composite(area, SWT.NONE);
		final GridLayout gl_container = new GridLayout(2, false);
		gl_container.horizontalSpacing = 2;
		container.setLayout(gl_container);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Label header = new Label(container, SWT.CENTER | SWT.WRAP);
		GridData gd_header = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		gd_header.verticalIndent = 10;
		header.setLayoutData(gd_header);
		header.setText("Stream: "+stream.getName());
		
		final Label propNameLabel = new Label(container, SWT.NONE);
		propNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		propNameLabel.setText("Property Name:");
		
		propNameComboViewer = new ComboViewer(container, SWT.NONE);
		propNameComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		propNameComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		propNameComboViewer.setLabelProvider(new LabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getText(Object element) {
				Map.Entry<String, DataType> e = (Map.Entry<String, DataType>)element;	
				return e.getKey()+" ("+e.getValue().name()+")";
			}

		});
		propNameComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = event.getStructuredSelection();
				Map.Entry<String, DataType> e = (Map.Entry<String, DataType>)sel.getFirstElement();
				propName=e.getKey();
				propType=e.getValue();
			}
		});
		propNameComboViewer.setInput(getEntries());
		propNameComboViewer.setSelection(new StructuredSelection(propNameComboViewer.getElementAt(0)));
		
		final Label propValueLabel = new Label(container, SWT.NONE);
		propValueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		propValueLabel.setText("Property Value:");
		
		propValueText = new Text(container, SWT.BORDER);
		propValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		return area;
	}

	private List<Entry<String,DataType>> getEntries() {
		return propNames.entrySet().stream().sorted((e1,e2)->e1.getKey().compareTo(e2.getKey())).collect(Collectors.toList());
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		final Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setImage(ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/tick.png"));
		final Button cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		cancelButton.setImage(ResourceManager.getPluginImage("com.minres.scviewer.e4.application", "icons/cross.png"));
	}

	protected void constrainShellSize() {
		super.constrainShellSize();
		getShell().setMinimumSize(getShell().computeSize(-1, -1));

	}

	@Override
	protected void okPressed() {
		propValue=propValueText.getText();
		super.okPressed();
	}

	public String getPropName() {
		return propName;
	}

	public DataType getPropType() {
		return propType;
	}
	
	public String getPropValue() {
		return propValue;
	}
}
