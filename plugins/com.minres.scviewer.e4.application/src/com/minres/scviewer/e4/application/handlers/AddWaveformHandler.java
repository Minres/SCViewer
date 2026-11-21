/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/

package com.minres.scviewer.e4.application.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.e4.application.parts.DesignBrowser;
import com.minres.scviewer.e4.application.parts.WaveformViewer;
import java.util.Arrays;
import org.eclipse.jface.viewers.ISelection;

public class AddWaveformHandler {

	public static final String PARAM_WHERE_ID="com.minres.scviewer.e4.application.command.addwaveform.where"; //$NON-NLS-1$
	public static final String PARAM_ALL_ID="com.minres.scviewer.e4.application.command.addwaveform.all"; //$NON-NLS-1$
	
	@CanExecute
	public boolean canExecute(@Named(PARAM_WHERE_ID) String where, @Named(PARAM_ALL_ID) String all,
			EPartService partService,
			@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection) {
		DesignBrowser browser = getListPart(partService);
		return canAdd(browser, where) && candidates(browser, all, selection).length > 0;
	}

	@Execute
	public void execute(@Named(PARAM_WHERE_ID) String where, @Named(PARAM_ALL_ID) String all,
			EPartService partService,
			@Named(IServiceConstants.ACTIVE_SELECTION) @Optional IStructuredSelection selection) {
		DesignBrowser browser = getListPart(partService);
		if (!canAdd(browser, where)) return;
		IWaveform[] waveforms = candidates(browser, all, selection);
		if (waveforms.length > 0)
			browser.getActiveWaveformViewerPart().addStreamsToList(waveforms, "before".equalsIgnoreCase(where));
	}

	private boolean canAdd(DesignBrowser browser, String where) {
		WaveformViewer target = browser == null ? null : browser.getActiveWaveformViewerPart();
		if (target == null || target.getDatabase() == null || !target.getDatabase().isLoaded()) return false;
		ISelection selection = target.getSelection();
		return !"before".equalsIgnoreCase(where) || (selection != null && !selection.isEmpty());
	}

	private IWaveform[] candidates(DesignBrowser browser, String all, IStructuredSelection selection) {
		Object[] entries = "true".equalsIgnoreCase(all) ? browser.getFilteredChildren()
				: selection == null ? new Object[0] : selection.toArray();
		if (entries == null) return new IWaveform[0];
		return Arrays.stream(entries).filter(IWaveform.class::isInstance).toArray(IWaveform[]::new);
	}

	protected DesignBrowser getListPart(EPartService partService) {
		MPart part = partService == null ? null : partService.getActivePart();
		if (part == null) return null;
		if (part.getObject() instanceof DesignBrowser browser) return browser;
		if (part.getObject() instanceof WaveformViewer viewer) {
			DesignBrowser browser = viewer.getDesignBrowser();
			return browser != null && browser.getActiveWaveformViewerPart() == viewer ? browser : null;
		}
		return null;
	}
}
