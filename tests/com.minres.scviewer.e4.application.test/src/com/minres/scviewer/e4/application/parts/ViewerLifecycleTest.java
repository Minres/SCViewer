package com.minres.scviewer.e4.application.parts;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.junit.Test;

import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.ui.IWaveformView;
import com.minres.scviewer.e4.application.elements.RelationTypeToolControl;
import com.minres.scviewer.e4.application.internal.util.DatabaseLoadSession;
import com.minres.scviewer.e4.application.internal.util.IModificationChecker;

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

    @Test
    public void decliningReloadResumesMonitoringAndPreferenceChangesTakeEffect() throws Exception {
        class Viewer extends WaveformViewer {
            boolean closed;
            int prompts;
            @Override protected boolean isViewerDisposed() { return closed; }
            @Override protected boolean confirmReload() { prompts++; return false; }
            @Override public void closeDatabase() { closed = true; super.closeDatabase(); }
        }
        Viewer viewer = new Viewer();
        Path file = Files.createTempFile("scviewer-reload", ".tmp");
        IWaveformDb database = (IWaveformDb) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformDb.class},
                (proxy, method, args) -> method.getName().equals("isLoaded") ? true : null);
        DatabaseLoadSession session = new DatabaseLoadSession(database, List.of(file.toFile()), event -> {}, result -> {});
        try {
            viewer.sync = new UISynchronize() {
                @Override public void syncExec(Runnable task) { task.run(); }
                @Override public void asyncExec(Runnable task) { task.run(); }
                @Override protected boolean isUIThread(Thread thread) { return true; }
                @Override protected void showBusyWhile(Runnable task) { task.run(); }
                @Override protected boolean dispatchEvents() { return false; }
            };
            set(viewer, "database", database); set(viewer, "loadSession", session);
            set(viewer, "waveformPane", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IWaveformView.class},
                    (proxy, method, args) -> null));
            viewer.filesToLoad.add(file.toFile());
            viewer.reactOnReloadDatabaseChange(true);
            IModificationChecker first = viewer.fileChecker;
            assertNotNull(first);
            change(file); first.check();
            assertEquals(1, viewer.prompts); assertNotNull(viewer.fileChecker); assertNotSame(first, viewer.fileChecker);
            viewer.fileChecker.check();
            assertEquals("Declined change must be acknowledged", 1, viewer.prompts);
            change(file); viewer.fileChecker.check();
            assertEquals(2, viewer.prompts);
            IModificationChecker old = viewer.fileChecker;
            viewer.reactOnReloadDatabaseChange(false);
            assertNull(viewer.fileChecker);
            change(file); old.check();
            assertEquals(2, viewer.prompts);
            viewer.reactOnReloadDatabaseChange(true);
            assertNotNull(viewer.fileChecker);
            viewer.closeDatabase();
            viewer.reactOnReloadDatabaseChange(true);
            assertNull(viewer.fileChecker);
        } finally { viewer.closeDatabase(); Files.deleteIfExists(file); }
    }

    private void change(Path file) throws Exception {
        Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10000));
    }
}
