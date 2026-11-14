package com.minres.scviewer.database.test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;
import com.minres.scviewer.database.tx.ITxEvent;
import com.minres.scviewer.database.tx.ITxRelation;

final class TxTestSupport {
    static java.io.File input(String name) {
        java.io.File local = new java.io.File("inputs/" + name);
        return local.exists() ? local : new java.io.File("tests/com.minres.scviewer.database.test/inputs/" + name);
    }

    static class Tx implements ITx {
        final long id, begin, end;
        int streamRow, generatorRow;
        Tx(long id, long begin, long end) { this.id = id; this.begin = begin; this.end = end; }
        public long getId() { return id; }
        public int getConcurrencyIndxInStream() { return streamRow; }
        public int getConcurrencyIdxInGen() { return generatorRow; }
        public long getBeginTime() { return begin; }
        public long getEndTime() { return end; }
        public IWaveform getStream() { return null; }
        public IWaveform getGenerator() { return null; }
        public List<ITxAttribute> getAttributes() { return Collections.emptyList(); }
        public Collection<ITxRelation> getIncomingRelations() { return Collections.emptyList(); }
        public Collection<ITxRelation> getOutgoingRelations() { return Collections.emptyList(); }
        public int compareTo(ITx other) {
            int c = Long.compare(begin, other.getBeginTime());
            return c != 0 ? c : Long.compare(id, other.getId());
        }
    }

    static class Event implements ITxEvent {
        final Tx tx;
        final EventKind kind;
        int row = -1;
        Event(Tx tx, EventKind kind) { this.tx = tx; this.kind = kind; }
        public long getTime() { return kind == EventKind.END ? tx.end : tx.begin; }
        public ITx getTransaction() { return tx; }
        public EventKind getKind() { return kind; }
        public int getRowIndex() { return row; }
        public WaveformType getType() { return WaveformType.STREAM; }
        public ITxEvent duplicate() { return new Event(tx, kind); }
    }
}
