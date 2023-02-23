/*******************************************************************************
 * Copyright (c) 2023 MINRES Technologies GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.ftr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.RelationTypeFactory;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;

import jacob.CborDecoder;
import jacob.CborType;

/**
 * The Class TextDbLoader.
 */
public class FtrDbLoader implements IWaveformDbLoader {

	enum FileType { NONE, PLAIN, GZIP, LZ4};

	/** The max time. */
	private Long maxTime = 0L;

	ArrayList<String> strDict = new ArrayList<>();


	/** The attr values. */
	final List<String> attrValues = new ArrayList<>();

	/** The relation types. */
	final Map<String, RelationType> relationTypes = UnifiedMap.newMap();

	/** The tx streams. */
	final Map<Long, TxStream> txStreams = UnifiedMap.newMap();

	/** The tx generators. */
	final Map<Long, TxGenerator> txGenerators = UnifiedMap.newMap();

	/** The transactions. */
	final Map<Long, FtrTx> transactions = UnifiedMap.newMap();

	/** The attribute types. */
	final Map<String, TxAttributeType> attributeTypes = UnifiedMap.newMap();

	/** The relations in. */
	final HashMultimap<Long, FtrRelation> relationsIn = HashMultimap.create();

	/** The relations out. */
	final HashMultimap<Long, FtrRelation> relationsOut = HashMultimap.create();

	/** The tx cache. */
	final Map<Long, Tx> txCache = UnifiedMap.newMap();

	/** The threads. */
	List<Thread> threads = new ArrayList<>();

	File file;
	
	private static final Logger LOG = LoggerFactory.getLogger(FtrDbLoader.class);
	
	/** The pcs. */
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	long time_scale_factor = 1000l;
	
	/**
	 * Adds the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	/**
	 * Removes the property change listener.
	 *
	 * @param l the l
	 */
	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	/**
	 * Gets the max time.
	 *
	 * @return the max time
	 */
	@Override
	public long getMaxTime() {
		return maxTime;
	}

	/**
	 * Gets the transaction.
	 *
	 * @param txId the tx id
	 * @return the transaction
	 */
	public synchronized ITx getTransaction(long txId) {
		if (txCache.containsKey(txId))
			return txCache.get(txId);
		if(transactions.containsKey(txId)) {
			Tx tx = new Tx(this, transactions.get(txId));
			txCache.put(txId, tx);
			return tx;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public FtrTx getScvTx(long id) {
		if(transactions.containsKey(id))
			return transactions.get(id);
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Gets the all waves.
	 *
	 * @return the all waves
	 */
	@Override
	public Collection<IWaveform> getAllWaves() {
		ArrayList<IWaveform> ret =  new ArrayList<>(txStreams.values());
		ret.addAll(txGenerators.values());
		return ret;
	}

	/**
	 * Gets the all relation types.
	 *
	 * @return the all relation types
	 */
	public Collection<RelationType> getAllRelationTypes() {
		return relationTypes.values();
	}

	/**
	 * Load.
	 *
	 * @param db   the db
	 * @param file the file
	 * @return true, if successful
	 * @throws InputFormatException the input format exception
	 */
	@Override
	public void load(IWaveformDb db, File file) throws InputFormatException {
		dispose();
		this.file=file;
		try(FileInputStream fis = new FileInputStream(file)) {
			new CborDbParser(this, fis);
		} catch (IOException e) {
			LOG.warn("Problem parsing file "+file.getName()+": " , e);
		} catch (Exception e) {
			LOG.error("Error parsing file "+file.getName()+ ": ", e);
			transactions.clear();
			throw new InputFormatException(e.toString());
		}
		txStreams.values().parallelStream().forEach(TxStream::calculateConcurrency);
	}

	public List<? extends byte[]> getChunksAtOffsets(ArrayList<Long> fileOffsets) throws InputFormatException {
		List<byte[]> ret = new ArrayList<>();
		try(FileInputStream fis = new FileInputStream(file)) {
			FileChannel fc = fis.getChannel();
			for (Long offset : fileOffsets) {
				if(offset>=0) {
					fc.position(offset);
					CborDecoder parser = new CborDecoder(fis);
					ret.add(parser.readByteString());
				} else {
					fc.position(-offset);
					CborDecoder parser = new CborDecoder(fis);
					BlockLZ4CompressorInputStream decomp = new BlockLZ4CompressorInputStream(new ByteArrayInputStream(parser.readByteString()));
					ret.add(decomp.readAllBytes());
					decomp.close();
				}
			}
		} catch (Exception e) {
			LOG.error("Error parsing file "+file.getName(), e);
			transactions.clear();
			throw new InputFormatException(e.toString());
		}
		return ret;
	}

	public List<? extends ITxAttribute> parseAtrributes(byte[] chunk, long blockOffset) {
		List<ITxAttribute> ret = new ArrayList<>();
		ByteArrayInputStream bais = new ByteArrayInputStream(chunk);
		bais.skip(blockOffset);
		CborDecoder cborDecoder = new CborDecoder(bais);
		try {
			long tx_size = cborDecoder.readArrayLength();
			for(long i = 0; i<tx_size; ++i) {
				long tag = cborDecoder.readTag();
				switch((int)tag) {
				case 6: // id/generator/start/end
					long len = cborDecoder.readArrayLength();
					assert(len==4);
					cborDecoder.readInt();
					cborDecoder.readInt();
					cborDecoder.readInt();
					cborDecoder.readInt();
					break;
				default:  { // skip over 7:begin attr, 8:record attr, 9:end attr
					long sz = cborDecoder.readArrayLength();
					assert(sz==3);
					long name_id = cborDecoder.readInt();
					long type_id = cborDecoder.readInt();
					String attrName = strDict.get((int)name_id);
					TxAttributeType attrType = getOrAddAttributeType(tag, type_id, attrName);
					switch((int)type_id) {
					case 0: // BOOLEAN
						ITxAttribute b = new TxAttribute(attrType, cborDecoder.readBoolean()?"True":"False");
						ret.add(b);
						break;
					case 2: // INTEGER
					case 3: // UNSIGNED
					case 10: // POINTER
						ITxAttribute a = new TxAttribute(attrType, String.valueOf(cborDecoder.readInt()));
						ret.add(a);
						break;
					case 4: // FLOATING_POINT_NUMBER
					case 7: // FIXED_POINT_INTEGER
					case 8: // UNSIGNED_FIXED_POINT_INTEGER
						ITxAttribute v = new TxAttribute(attrType, String.valueOf(cborDecoder.readFloat()));
						ret.add(v);
						break;
					case 1: // ENUMERATION
					case 5: // BIT_VECTOR
					case 6: // LOGIC_VECTOR
					case 12: // STRING
						ITxAttribute s = new TxAttribute(attrType, strDict.get((int)cborDecoder.readInt()));
						ret.add(s);
						break;
					default:
						LOG.warn("Unsupported data type: "+type_id);
					}
				}
				}
			}
		} catch (IOException e) {
			LOG.error("Error parsing file "+file.getName(), e);
		}
		return ret;
	}

	private synchronized TxAttributeType getOrAddAttributeType(long tag, long type_id, String attrName) {
		if(!attributeTypes.containsKey(attrName)) {
			attributeTypes.put(attrName, new TxAttributeType(attrName, DataType.values()[(int)type_id], AssociationType.values()[(int)tag-7]));
		} 
		TxAttributeType attrType = attributeTypes.get(attrName);
		return attrType;
	}

	/**
	 * Dispose.
	 */
	@Override
	public void dispose() {
		attrValues.clear();
		relationTypes.clear();
		txStreams.clear();
		txGenerators.clear();
		transactions.clear();
		attributeTypes.clear();
		relationsIn.clear();
		relationsOut.clear();
	}

	/**
	 * The Class TextDbParser.
	 */
	static class CborDbParser extends CborDecoder {

		static final private CborType break_type = CborType.valueOf(0xff);

		/** The loader. */
		final FtrDbLoader loader;

		/**
		 * Instantiates a new text db parser.
		 *
		 * @param loader the loader
		 */
		public CborDbParser(FtrDbLoader loader, FileInputStream inputStream) {
			super(inputStream);
			this.loader = loader;
			try {
				long cbor_tag = readTag();
				assert(cbor_tag == 55799);
				long array_len = readArrayLength();
				assert(array_len==-1);
				CborType next = peekType();
				int chunk_idx=0;
				while(next != null && !break_type.isEqualType(next)) {
					long tag = readTag();
					switch((int)tag) {
					case 6: { // info
						CborDecoder cbd = new CborDecoder(new ByteArrayInputStream(readByteString()));
						long sz = cbd.readArrayLength();
						assert(sz==3);
						long time_numerator=cbd.readInt();
						long time_denominator=cbd.readInt();
						loader.time_scale_factor = 1000000000000000l*time_numerator/time_denominator;
						long epoch_tag = cbd.readTag();
						assert(epoch_tag==1);
						cbd.readInt(); // epoch
						break;
					}
					case 8: { // dictionary uncompressed
						parseDict(new CborDecoder(new ByteArrayInputStream(readByteString())));
						break;
					}
					case 9: { // dictionary compressed
						long sz = readArrayLength();
						assert(sz==2);
						readInt(); // uncompressed size
						parseDict(new CborDecoder(new BlockLZ4CompressorInputStream(new ByteArrayInputStream(readByteString()))));
						break;
					}
					case 10: { // directory uncompressed
						parseDir(new CborDecoder(new ByteArrayInputStream(readByteString())));
						break;
					}
					case 11: { // directory compressed
						long sz = readArrayLength();
						assert(sz==2);
						readInt(); // uncompressed size
						parseDir(new CborDecoder(new BlockLZ4CompressorInputStream(new ByteArrayInputStream(readByteString()))));
						break;
					}
					case 12: { //tx chunk uncompressed
						long len = readArrayLength(); 
						assert(len==2);
						long stream_id = readInt();
						TxStream txStream = loader.txStreams.get(stream_id);
						txStream.fileOffsets.add(inputStream.getChannel().position());
						parseTx(txStream, txStream.fileOffsets.size()-1, readByteString());
						break;
					}
					case 13: { //tx chunk compressed
						long len = readArrayLength(); 
						assert(len==3);
						long stream_id = readInt();
						readInt(); // uncompressed size
						TxStream txStream = loader.txStreams.get(stream_id);
						txStream.fileOffsets.add(0-inputStream.getChannel().position());
						BlockLZ4CompressorInputStream decomp = new BlockLZ4CompressorInputStream(new ByteArrayInputStream(readByteString()));
						parseTx(txStream, txStream.fileOffsets.size()-1, decomp.readAllBytes());
						decomp.close();
						break;
					}
					case 14: { // relations uncompressed
						parseRel(new CborDecoder(new ByteArrayInputStream(readByteString())));
						break;
					}
					case 15: { // relations uncompressed
						long sz = readArrayLength();
						assert(sz==2);
						readInt(); // uncompressed size
						parseRel(new CborDecoder(new BlockLZ4CompressorInputStream(new ByteArrayInputStream(readByteString()))));
						break;
					}
					}
					next = peekType();
					chunk_idx++;
				}
			} catch(IOException e) {
				long pos = 0;
				try {pos=inputStream.getChannel().position(); } catch (Exception ee) {}
				LOG.error("Error parsing file input stream at position" + pos, e);
			}
		}


		private void parseDict(CborDecoder cborDecoder) throws IOException {
			long size = cborDecoder.readMapLength();
			ArrayList<String> lst = new ArrayList<>((int)size);
			for(long i = 0; i<size; ++i) {
				long idx = cborDecoder.readInt();
				assert(idx==loader.strDict.size()+1);
				lst.add(cborDecoder.readTextString());
			}
			loader.strDict.addAll(lst);
		}

		private void parseDir(CborDecoder cborDecoder) throws IOException {
			long size = cborDecoder.readArrayLength();
			if(size<0) {
				CborType next = cborDecoder.peekType();
				while(next != null && !break_type.isEqualType(next)) {
					parseDictEntry(cborDecoder);
					next = cborDecoder.peekType();
				}				
			} else 
				for(long i = 0; i<size; ++i) {
					parseDictEntry(cborDecoder);
				}
		}


		private void parseDictEntry(CborDecoder cborDecoder) throws IOException {
			long id = cborDecoder.readTag();
			if(id==16) { // a stream
				long len = cborDecoder.readArrayLength();
				assert(len==3);
				long stream_id = cborDecoder.readInt();
				long name_id = cborDecoder.readInt();
				long kind_id = cborDecoder.readInt();
				add(stream_id, new TxStream(loader, stream_id, loader.strDict.get((int)name_id), loader.strDict.get((int)kind_id)));
			} else if(id==17) { // a generator
				long len = cborDecoder.readArrayLength();
				assert(len==3);
				long gen_id = cborDecoder.readInt();
				long name_id = cborDecoder.readInt();
				long stream_id = cborDecoder.readInt();
				if(loader.txStreams.containsKey(stream_id))
					add(gen_id, new TxGenerator(loader, gen_id, loader.strDict.get((int)name_id), loader.txStreams.get(stream_id)));
			} else {
				throw new IOException("Illegal tage ncountered: "+id);
			}
		}

		private void parseTx(TxStream txStream, long blockId, byte[] chunk) throws IOException {
			CborDecoder cborDecoder = new CborDecoder(new ByteArrayInputStream(chunk));
			long size = cborDecoder.readArrayLength();
			assert(size==-1);
			CborType next = cborDecoder.peekType();
			while(next != null && !break_type.isEqualType(next)) {
				long blockOffset = cborDecoder.getPos();
				long tx_size = cborDecoder.readArrayLength();
				long txId = 0;
				long genId = 0;
				long attr_idx=0;
				for(long i = 0; i<tx_size; ++i) {
					long tag = cborDecoder.readTag();
					switch((int)tag) {
					case 6: // id/generator/start/end
						long len = cborDecoder.readArrayLength();
						assert(len==4);
						txId = cborDecoder.readInt();
						genId = cborDecoder.readInt();
						long startTime = cborDecoder.readInt()*loader.time_scale_factor;
						long endTime = cborDecoder.readInt()*loader.time_scale_factor;
						TxGenerator gen = loader.txGenerators.get(genId);
						FtrTx scvTx = new FtrTx(txId, gen.stream.getId(), genId, startTime, endTime, blockId, blockOffset);
						loader.maxTime = loader.maxTime > scvTx.endTime ? loader.maxTime : scvTx.endTime;
						loader.transactions.put(txId, scvTx);
						TxStream stream = loader.txStreams.get(gen.stream.getId());
						if (scvTx.beginTime == scvTx.endTime) {
							stream.addEvent(new TxEvent(loader, EventKind.SINGLE, txId, scvTx.beginTime));
							gen.addEvent(new TxEvent(loader, EventKind.SINGLE, txId, scvTx.beginTime));
						} else {
							stream.addEvent(new TxEvent(loader, EventKind.BEGIN, txId, scvTx.beginTime));
							gen.addEvent(new TxEvent(loader, EventKind.BEGIN, txId, scvTx.beginTime));
							stream.addEvent(new TxEvent(loader, EventKind.END, txId, scvTx.endTime));
							gen.addEvent(new TxEvent(loader, EventKind.END, txId, scvTx.endTime));
						}
						break;
					default:  { // skip over 7:begin attr, 8:record attr, 9:end attr
						long sz = cborDecoder.readArrayLength();
						assert(sz==3);
						long name_id = cborDecoder.readInt();
						String name = loader.strDict.get((int)name_id);
						long type_id = cborDecoder.readInt();
						switch((int)type_id) {
						case 0: // BOOLEAN
							cborDecoder.readBoolean();
							break;
						case 4: // FLOATING_POINT_NUMBER
						case 7: // FIXED_POINT_INTEGER
						case 8: // UNSIGNED_FIXED_POINT_INTEGER
							cborDecoder.readFloat();
							break;
						default:
							cborDecoder.readInt();
						}
						attr_idx++;
					}
					}
				}
				next = cborDecoder.peekType();
			}							
		}

		private void parseRel(CborDecoder cborDecoder) throws IOException {
			long size = cborDecoder.readArrayLength();
			assert(size==-1);
			CborType next = cborDecoder.peekType();
			while(next != null && !break_type.isEqualType(next)) {
				long sz = cborDecoder.readArrayLength();
				assert(sz==3);
				long type_id = cborDecoder.readInt();
				long from_id = cborDecoder.readInt();
				long to_id = cborDecoder.readInt();
				String rel_name = loader.strDict.get((int)type_id);
				FtrRelation ftrRel = new FtrRelation(loader.relationTypes.getOrDefault(rel_name, RelationTypeFactory.create(rel_name)), from_id, to_id);
				loader.relationsOut.put(from_id, ftrRel);
				loader.relationsIn.put(to_id, ftrRel);
				next = cborDecoder.peekType();
			}							
			
		}
		
		private void add(Long id, TxStream stream) {
			loader.txStreams.put(id, stream);
			loader.pcs.firePropertyChange(IWaveformDbLoader.STREAM_ADDED, null, stream);
		}

		private void add(Long id, TxGenerator generator) {
			loader.txGenerators.put(id, generator);
			loader.pcs.firePropertyChange(IWaveformDbLoader.GENERATOR_ADDED, null, generator);
		}
	}

}
