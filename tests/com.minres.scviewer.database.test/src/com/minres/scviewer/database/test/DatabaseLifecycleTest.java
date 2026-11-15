package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import com.minres.scviewer.database.*;
import com.minres.scviewer.database.ftr.FtrDbLoader;
import com.minres.scviewer.database.internal.WaveformDb;

public class DatabaseLifecycleTest {
    private static class Loader implements IWaveformDbLoader {
        final PropertyChangeSupport events = new PropertyChangeSupport(this);
        final boolean fail;
        int disposals;
        Loader(boolean fail) { this.fail = fail; }
        public void addPropertyChangeListener(PropertyChangeListener l) { events.addPropertyChangeListener(l); }
        public void removePropertyChangeListener(PropertyChangeListener l) { events.removePropertyChangeListener(l); }
        public void load(File file) throws InputFormatException {
            events.firePropertyChange(STREAM_ADDED, null, new EmptyWaveform("stream"));
            if (fail) throw new InputFormatException("broken");
        }
        public long getMaxTime() { return 100; }
        public Collection<IWaveform> getAllWaves() { return Collections.emptyList(); }
        public Collection<RelationType> getAllRelationTypes() { return Collections.emptyList(); }
        public void dispose() { disposals++; }
    }
    private IWaveformDbLoaderFactory factory(Loader loader) {
        return new IWaveformDbLoaderFactory() {
            public boolean canLoad(File f) { return f.getName().endsWith(".review"); }
            public IWaveformDbLoader getLoader() { return loader; }
        };
    }

    @Test
    public void failedAndUnsupportedLoadsPublishNothing() {
        WaveformDb db = new WaveformDb();
        Loader loader = new Loader(true);
        IWaveformDbLoaderFactory factory = factory(loader);db.bind(factory);
        try {
            assertFalse(db.load(new File("broken.review")));
            assertFalse(db.isLoaded());
            assertTrue(db.getAllWaves().isEmpty());
            assertTrue(db.getChildNodes().isEmpty());
            assertEquals(0, db.getMaxTime());
            assertEquals(1, loader.disposals);
            assertEquals(0, loader.events.getPropertyChangeListeners().length);
            assertFalse(db.load(new File("unsupported.review-unknown")));
            db.close();assertEquals(1, loader.disposals);
        } finally { db.unbind(factory); }
    }

    @Test
    public void successfulCloseReleasesStateAndIsIdempotent() {
        WaveformDb db = new WaveformDb();Loader loader = new Loader(false);
        IWaveformDbLoaderFactory factory = factory(loader);db.bind(factory);
        try {
            assertTrue(db.load(new File("success.review")));
            assertTrue(db.isLoaded());assertEquals(1, db.getAllWaves().size());
            assertEquals(100, db.getMaxTime());
            assertEquals(0, loader.events.getPropertyChangeListeners().length);
            db.close();db.close();
            assertEquals(1, loader.disposals);assertFalse(db.isLoaded());
            assertTrue(db.getAllWaves().isEmpty());assertTrue(db.getChildNodes().isEmpty());
            assertEquals(0, db.getMaxTime());
        } finally { db.unbind(factory);db.close(); }
    }

    @Test
    public void notificationsRunOutsideDatabaseLockAndSeeCommittedState() {
        WaveformDb db = new WaveformDb();
        Loader loader = new Loader(false);
        IWaveformDbLoaderFactory factory = factory(loader);
        db.bind(factory);
        java.util.List<String> notifications = new java.util.ArrayList<>();
        db.addPropertyChangeListener(event -> {
            assertFalse("Listeners must be able to hand work to a thread calling close()", Thread.holdsLock(db));
            assertTrue(db.isLoaded());
            assertEquals(100, db.getMaxTime());
            assertEquals(1, db.getAllWaves().size());
            notifications.add(event.getPropertyName());
        });
        try {
            assertTrue(db.load(new File("success.review")));
            assertTrue(notifications.contains(IHierNode.WAVEFORMS));
            assertTrue(notifications.contains(IHierNode.CHILDS));
            assertTrue(notifications.contains(IHierNode.LOADING_FINISHED));
        } finally { db.unbind(factory); db.close(); }
    }

    @Test
    public void invalidFtrFailsAndLoaderCanBeReusedAfterDisposal() throws Exception {
        FtrDbLoader loader = new FtrDbLoader();Path invalid = Files.createTempFile("invalid-ftr-", ".ftr");
        try {
            Files.write(invalid, new byte[] {0});
            try { loader.load(invalid.toFile());fail("Invalid header must fail without assertions"); }
            catch (InputFormatException e) { assertNotNull(e.getCause());assertNotNull(e.getMessage()); }
            assertTrue(loader.getAllWaves().isEmpty());
            loader.load(TxTestSupport.input("my_db.ftr"));
            for (IWaveform wave : loader.getAllWaves()) wave.getEvents();
            assertNotNull(loader.getTransaction(1));
            loader.dispose();
            assertNull(loader.getTransaction(1));assertEquals(0, loader.getMaxTime());
            for (String name : new String[] {"strDict", "txCache"}) {
                java.lang.reflect.Field field = FtrDbLoader.class.getDeclaredField(name);field.setAccessible(true);
                Object value = field.get(loader);
                assertTrue(value instanceof java.util.Map ? ((java.util.Map<?,?>)value).isEmpty() : ((Collection<?>)value).isEmpty());
            }
            loader.load(TxTestSupport.input("my_db_c.ftr"));
            assertEquals(3400000000L, loader.getMaxTime());
        } finally { loader.dispose();Files.deleteIfExists(invalid); }
    }
}
