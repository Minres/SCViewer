 
package com.minres.scviewer.e4.application.elements;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.ui.di.AboutToHide;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.ItemType;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.BitVector;
import com.minres.scviewer.database.ISignal;
import com.minres.scviewer.database.ui.TrackEntry;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

public class WaveformPopupMenuContribution {
	int counter=0;
	
	@Inject MPart activePart;
		
	final TrackEntry nullEntry = new TrackEntry(null);
	
	@Evaluate
	public boolean evaluate() {
		Object obj = activePart.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer wfv = (WaveformViewer)obj;
			ISelection sel = wfv.getSelection();
			if(!sel.isEmpty() && sel instanceof IStructuredSelection) {
				Object selected = ((IStructuredSelection)sel).getFirstElement();
				if(selected instanceof ISignal<?>) {
					Object x = ((ISignal<?>) selected).getEvents().firstEntry().getValue();
					if((x instanceof BitVector) && ((BitVector)x).getWidth()==1) {
						return false;
					} else
						return true;
				}
			}
		}
		return false;
	}

	@AboutToShow
	public void aboutToShow(List<MMenuElement> items, MApplication application, EModelService modelService) {
		Object obj = activePart.getObject();
		if(obj instanceof WaveformViewer){
			WaveformViewer wfv = (WaveformViewer)obj;
			ISelection sel = wfv.getSelection();
			if(!sel.isEmpty() && sel instanceof IStructuredSelection) {
				Iterator<?> it = ((IStructuredSelection)sel).iterator();
				Object first = it.next();
				Object second=null;
				if(it.hasNext()) second=it.next();
				if(first instanceof ISignal<?>) {
					Object o = ((ISignal<?>) first).getEvents().firstEntry().getValue();
					//com.minres.scviewer.e4.application.menu.mulitvaluesettings
					if((o instanceof Double) || (o instanceof BitVector)) {
						TrackEntry entry=nullEntry;
						if(second instanceof TrackEntry)
							entry=(TrackEntry)second;
						if(o instanceof BitVector) {
						    addValueMenuItem(items, application, modelService, "hex", TrackEntry.ValueDisplay.DEFAULT, entry.valueDisplay);
						    addValueMenuItem(items, application, modelService, "unsigned", TrackEntry.ValueDisplay.UNSIGNED, entry.valueDisplay);
						    addValueMenuItem(items, application, modelService, "signed", TrackEntry.ValueDisplay.SIGNED, entry.valueDisplay);
							items.add(MMenuFactory.INSTANCE.createMenuSeparator());
							addWaveMenuItem(items, application, modelService, "bit vector", TrackEntry.WaveDisplay.DEFAULT, entry.waveDisplay);
						}						
						addWaveMenuItem(items, application, modelService, "analog step-wise", TrackEntry.WaveDisplay.STEP_WISE, entry.waveDisplay);
						addWaveMenuItem(items, application, modelService, "analog continous", TrackEntry.WaveDisplay.CONTINOUS, entry.waveDisplay);
					}
				}
			}
		}

	}

	private void addValueMenuItem(List<MMenuElement> items, MApplication application, EModelService modelService,
			String label, TrackEntry.ValueDisplay value, TrackEntry.ValueDisplay actual) {
		MHandledMenuItem item = MMenuFactory.INSTANCE.createHandledMenuItem();
		item.setType(ItemType.RADIO);
		item.setSelected(value==actual);
		item.setLabel("Show as "+label);
		item.setContributorURI("platform:/plugin/com.minres.scviewer.e4.application");
		List<MCommand> cmds = modelService.findElements(application, "com.minres.scviewer.e4.application.command.changevaluedisplay", MCommand.class, null);
		if(cmds.size()!=1) System.err.println("No command found!");
		else item.setCommand(cmds.get(0));
		MParameter param = MCommandsFactory.INSTANCE.createParameter();
		param.setName("com.minres.scviewer.e4.application.commandparameter.changevaluedisplay");
		param.setValue(value.toString());
		item.getParameters().add(param);
		items.add(item);
	}
	
	private void addWaveMenuItem(List<MMenuElement> items, MApplication application, EModelService modelService,
			String label, TrackEntry.WaveDisplay value, TrackEntry.WaveDisplay actual) {
		MHandledMenuItem item = MMenuFactory.INSTANCE.createHandledMenuItem();
		item.setType(ItemType.RADIO);
		item.setSelected(value==actual);
		item.setLabel("Render "+label);
		item.setContributorURI("platform:/plugin/com.minres.scviewer.e4.application");
		List<MCommand> cmds = modelService.findElements(application, "com.minres.scviewer.e4.application.command.changewavedisplay", MCommand.class, null);
		if(cmds.size()!=1) System.err.println("No command found!");
		else item.setCommand(cmds.get(0));
		MParameter param = MCommandsFactory.INSTANCE.createParameter();
		param.setName("com.minres.scviewer.e4.application.commandparameter.changewavedisplay");
		param.setValue(value.toString());
		item.getParameters().add(param);
		items.add(item);
	}
	
	@AboutToHide
	public void aboutToHide(List<MMenuElement> items) {
		
	}
		
}