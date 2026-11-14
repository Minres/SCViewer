package com.minres.scviewer.database.test;

import static org.junit.Assert.*;
import java.lang.reflect.Method;
import org.junit.Test;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.ftr.FtrDbLoader;

public class FtrLoadingTest {
    @Test
    public void cachedChunksStillParseEvents() throws Exception {
        for (String name : new String[] {"my_db.ftr", "my_db_c.ftr"}) {
            FtrDbLoader loader = new FtrDbLoader();
            try {
                loader.load(TxTestSupport.input(name));
                IWaveform stream = loader.getAllWaves().stream()
                        .filter(w -> w.getClass().getSimpleName().equals("TxStream") && w.getId() == 1)
                        .findFirst().get();
                Method chunks = stream.getClass().getMethod("getChunks");
                chunks.setAccessible(true);
                chunks.invoke(stream);
                assertNotNull(stream.getEventsAtTime(0));
                IEventList events = stream.getEvents();
                assertEquals(27, events.size());
                assertSame(events, stream.getEvents());
                assertEquals(2, stream.getRowCount());
                for (IWaveform wave : loader.getAllWaves()) wave.getEvents();
                assertEquals(27, stream.getEvents().size());
            } finally { loader.dispose(); }
        }
    }
}
