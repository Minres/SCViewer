/*******************************************************************************
 * Copyright (c) 2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.handlers;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.ui.bindings.internal.BindingTable;
import org.eclipse.e4.ui.bindings.internal.BindingTableManager;
import org.eclipse.e4.ui.bindings.internal.ContextSet;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;

@SuppressWarnings("restriction")
public class WaveformBindingsTest {
    @Test
    public void childContextResolvesDeleteConflictAndRetainsMenuShortcutLookup() throws Exception {
        var contexts = new ContextManager();
        var window = contexts.getContext("org.eclipse.ui.contexts.window");
        window.define("Window", "", null);
        var waveform = contexts.getContext("com.minres.scviewer.e4.application.context.waveform");
        waveform.define("Waveform", "", window.getId());
        ContextSet.setComparator(new ContextSet.CComp(contexts));
        var context = EclipseContextFactory.create();
        try {
            var application = MApplicationFactory.INSTANCE.createApplication();
            application.setContext(context);
            context.set(MApplication.class, application);
            var manager = ContextInjectionFactory.make(BindingTableManager.class, context);
            var windowTable = new BindingTable(window, application);
            var waveformTable = new BindingTable(waveform, application);
            manager.addTable(windowTable); manager.addTable(waveformTable);
            var commands = new CommandManager();
            var category = commands.getCategory("test"); category.define("test", "");
            var standardCommand = commands.getCommand("org.eclipse.ui.edit.delete");
            standardCommand.define("Delete", "", category);
            var viewerCommand = commands.getCommand("com.minres.scviewer.e4.application.command.deleteWaveform");
            viewerCommand.define("Delete waveform", "", category);
            var standard = new ParameterizedCommand(standardCommand, null);
            var viewer = new ParameterizedCommand(viewerCommand, null);
            var key = KeySequence.getInstance("DEL");
            String scheme = "org.eclipse.ui.defaultAcceleratorConfiguration";
            var normal = new KeyBinding(key, standard, scheme, window.getId(), null, null, null, Binding.SYSTEM);
            var conflicting = new KeyBinding(key, viewer, scheme, window.getId(), null, null, null, Binding.USER);
            windowTable.addBinding(normal); windowTable.addBinding(conflicting);
            assertEquals(2, windowTable.getConflictsFor(key).size());
            assertNull(windowTable.getBestSequenceFor(viewer));
            windowTable.removeBinding(conflicting);
            var scoped = new KeyBinding(key, viewer, scheme, waveform.getId(), null, null, null, Binding.USER);
            waveformTable.addBinding(scoped);
            var active = manager.createContextSet(List.of(window, waveform));
            assertSame(scoped, manager.getPerfectMatch(active, key));
            assertSame(scoped, manager.getBestSequenceFor(active, viewer));
            assertSame(normal, manager.getPerfectMatch(manager.createContextSet(List.of(window)), key));
        } finally { context.dispose(); }
    }
}
