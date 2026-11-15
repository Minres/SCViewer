package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.junit.Test;

import com.minres.scviewer.e4.application.elements.RelationTypeToolControl;

public class ViewerLifecycleTest {
    private void set(WaveformViewer viewer, String name, Object value) throws Exception {
        java.lang.reflect.Field field = WaveformViewer.class.getDeclaredField(name);
        field.setAccessible(true); field.set(viewer, value);
    }

    private IEclipsePreferences preferences(AtomicInteger adds, AtomicInteger removes) {
        return (IEclipsePreferences) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IEclipsePreferences.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addPreferenceChangeListener")) adds.incrementAndGet();
                    if (method.getName().equals("removePreferenceChangeListener")) removes.incrementAndGet();
                    return null;
                });
    }

    @Test
    public void preferenceReinjectionAndRepeatedDisposalDoNotLeakListeners() throws Exception {
        WaveformViewer viewer = new WaveformViewer();
        AtomicInteger addsA = new AtomicInteger(), removesA = new AtomicInteger();
        AtomicInteger addsB = new AtomicInteger(), removesB = new AtomicInteger();
        AtomicInteger removedParts = new AtomicInteger();
        IEclipsePreferences a = preferences(addsA, removesA), b = preferences(addsB, removesB);
        viewer.ePartService = (EPartService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EPartService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("removePartListener")) removedParts.incrementAndGet();
                    return null;
                });
        set(viewer, "partListener", new PartListener());
        try {
            viewer.reactOnPrefsChange(a); viewer.reactOnPrefsChange(a); viewer.reactOnPrefsChange(b);
            assertEquals(1, addsA.get()); assertEquals(1, removesA.get());
            assertEquals(1, addsB.get());
            viewer.closeDatabase(); viewer.closeDatabase(); viewer.reactOnPrefsChange(a);
            assertEquals(1, removesB.get()); assertEquals(1, removedParts.get()); assertEquals(1, addsA.get());
        } finally { viewer.closeDatabase(); }
    }

    @Test
    public void toolbarPartListenerIsRemovedEvenWhenDisposedBeforeGuiCreation() {
        AtomicInteger added = new AtomicInteger(), removed = new AtomicInteger();
        EPartService parts = (EPartService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EPartService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addPartListener")) added.incrementAndGet();
                    if (method.getName().equals("removePartListener")) removed.incrementAndGet();
                    return null;
                });
        RelationTypeToolControl control = new RelationTypeToolControl(parts);
        control.dispose(); control.dispose();
        assertEquals(1, added.get()); assertEquals(1, removed.get());
    }

}
