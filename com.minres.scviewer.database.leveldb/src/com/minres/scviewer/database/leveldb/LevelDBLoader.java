package com.minres.scviewer.database.leveldb;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.SeekingIterator;
import org.json.JSONObject;

import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.IWaveformEvent;
import com.minres.scviewer.database.RelationType;

public class LevelDBLoader implements IWaveformDbLoader {

	static byte[] toByteArray(String value) {
		return value.getBytes(UTF_8);
	}

	private StringDBWrapper levelDb;
	private IWaveformDb db;
	private Long maxTime=null;
	private List<RelationType> usedRelationsList = new ArrayList<>();
	
	@Override
	public boolean load(IWaveformDb db, File inp) throws Exception {
		try {
			this.db=db;
			levelDb = new StringDBWrapper(new Options(), inp);
		} catch(Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public Long getMaxTime() {
		if(maxTime==null) {
			SeekingIterator<String, String> it = levelDb.iterator();
			it.seek("st~");
			Entry<String, String> val = null;
			while(it.hasNext()) {
				Entry<String, String>  v = it.next();
				if(!v.getKey().startsWith("st~")) continue;
				val=v;
			}
			if(val==null) 
				maxTime = 0L;
			else {
				String[] token = val.getKey().split("~");
				maxTime = Long.parseLong(token[2], 16);
			}
		}
		return maxTime;
	}

	@Override
	public List<IWaveform<? extends IWaveformEvent>> getAllWaves() {
		List<IWaveform<? extends IWaveformEvent>> streams=new ArrayList<IWaveform<? extends IWaveformEvent>>();
		SeekingIterator<String, String> it = levelDb.iterator();
		it.seek("s~");
		while(it.hasNext()) {
			Entry<String, String> val = it.next();
			if(!val.getKey().startsWith("s~")) break;
			TxStream stream = new TxStream(levelDb, db, new JSONObject(val.getValue()));
			stream.setRelationTypeList(usedRelationsList);
			streams.add(stream);
		}
		return streams;
	}

	@Override
	public Collection<RelationType> getAllRelationTypes() {
//		return Collections.emptyList();
		return usedRelationsList;
	}

}
