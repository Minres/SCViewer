/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * Copyright (c) 2020 MINRES Technologies GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

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

/**
 * The Class TextDbLoader.
 */
public class TextDbLoader implements IWaveformDbLoader {

	/** the file size limit of a zipped txlog where the loader starts to use a file mapped database */
	private static final long MEMMAP_LIMIT=256l*1024l*1024l;
	
	private static final long MAPDB_INITIAL_ALLOC = 512l*1024l*1024l;
	
	private static final long MAPDB_INCREMENTAL_ALLOC = 128l*1024l*1024l;
	
	/** The max time. */
	private Long maxTime = 0L;

	/** The map db. */
	DB mapDb = null;

	/** The attr values. */
	final List<String> attrValues = new ArrayList<>();

	/** The relation types. */
	final Map<String, RelationType> relationTypes = UnifiedMap.newMap();

	/** The tx streams. */
	final Map<Long, TxStream> txStreams = UnifiedMap.newMap();

	/** The tx generators. */
	final Map<Long, TxGenerator> txGenerators = UnifiedMap.newMap();

	/** The transactions. */
	Map<Long, ScvTx> transactions = null;

	/** The attribute types. */
	final Map<String, TxAttributeType> attributeTypes = UnifiedMap.newMap();

	/** The relations in. */
	final HashMultimap<Long, ScvRelation> relationsIn = HashMultimap.create();

	/** The relations out. */
	final HashMultimap<Long, ScvRelation> relationsOut = HashMultimap.create();

	/** The tx cache. */
	HashMap<Long, Tx> txCache = new HashMap<>();

	/** The threads. */
	List<Thread> threads = new ArrayList<>();

	/** The pcs. */
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/** The Constant x. */
	static final byte[] x = "scv_tr_stream".getBytes();

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
	public Long getMaxTime() {
		return maxTime;
	}

	/**
	 * Gets the transaction.
	 *
	 * @param txId the tx id
	 * @return the transaction
	 */
	public ITx getTransaction(long txId) {
		if (txCache.containsKey(txId))
			return txCache.get(txId);
		if(transactions.containsKey(txId)) {
			Tx tx = new Tx(this, txId);
			txCache.put(txId, tx);
			return tx;
		} else {
			Set<Long> keys = new TreeSet<>(transactions.keySet());
			throw new IllegalArgumentException();
		}
	}

	public ScvTx getScvTx(long id) {
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
		return new ArrayList<>(txStreams.values());
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
	 * Can load.
	 *
	 * @param inputFile the input file
	 * @return true, if successful
	 */
	@Override
	public boolean canLoad(File inputFile) {
		if (!inputFile.isDirectory() && inputFile.exists()) {
			boolean gzipped = isGzipped(inputFile);
			try(InputStream stream = gzipped ? new GZIPInputStream(new FileInputStream(inputFile)) : new FileInputStream(inputFile)){
				byte[] buffer = new byte[x.length];
				int readCnt = stream.read(buffer, 0, x.length);
				if (readCnt == x.length) {
					for (int i = 0; i < x.length; i++)
						if (buffer[i] != x[i])
							return false;
				}
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Checks if is gzipped.
	 *
	 * @param f the f
	 * @return true, if is gzipped
	 */
	private static boolean isGzipped(File f) {
		try (InputStream is = new FileInputStream(f)) {
			byte[] signature = new byte[2];
			int nread = is.read(signature); // read the gzip signature
			return nread == 2 && signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Load.
	 *
	 * @param db   the db
	 * @param file the file
	 * @return true, if successful
	 * @throws InputFormatException the input format exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void load(IWaveformDb db, File file) throws InputFormatException {
		dispose();
		boolean gzipped = isGzipped(file);
		if (file.length() < MEMMAP_LIMIT * (gzipped ? 1 : 10)
				|| "memory".equals(System.getProperty("ScvBackingDB", "file")))
			mapDb = DBMaker.memoryDirectDB().make();
		else {
			File mapDbFile;
			try {
				mapDbFile = File.createTempFile("." + file.getName(), ".mapdb", null /* file.parentFile */);
				Files.delete(Paths.get(mapDbFile.getPath()));
			} catch (IOException e) {
				throw new InputFormatException(e.toString());
			}
			mapDb = DBMaker.fileDB(mapDbFile).fileMmapEnable() // Always enable mmap
					.fileMmapPreclearDisable().allocateStartSize(MAPDB_INITIAL_ALLOC)
					.allocateIncrement(MAPDB_INCREMENTAL_ALLOC).cleanerHackEnable().make();
			mapDbFile.deleteOnExit();
		}
		TextDbParser parser = new TextDbParser(this);
		try {
			
//			parser.txSink = mapDb.treeMap("transactions", Serializer.LONG, Serializer.JAVA).createFromSink();
			parser.txSink = mapDb.hashMap("transactions", Serializer.LONG, Serializer.JAVA).create();
			parser.parseInput(gzipped ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file));
//			transactions = parser.txSink.create();
			transactions = parser.txSink;
		} catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
		} catch (Exception e) {
			throw new InputFormatException(e.toString());
		}
		txStreams.values().parallelStream().forEach(TxStream::calculateConcurrency);
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
		transactions = null;
		attributeTypes.clear();
		relationsIn.clear();
		relationsOut.clear();
		if (mapDb != null) {
			mapDb.close();
			mapDb = null;
		}
	}

	/**
	 * The Class TextDbParser.
	 */
	static class TextDbParser {

		/** The Constant scv_tr_stream. */
		static final Pattern scv_tr_stream = Pattern
				.compile("^scv_tr_stream\\s+\\(ID (\\d+),\\s+name\\s+\"([^\"]+)\",\\s+kind\\s+\"([^\"]+)\"\\)$");

		/** The Constant scv_tr_generator. */
		static final Pattern scv_tr_generator = Pattern
				.compile("^scv_tr_generator\\s+\\(ID\\s+(\\d+),\\s+name\\s+\"([^\"]+)\",\\s+scv_tr_stream\\s+(\\d+),$");

		/** The Constant begin_attribute. */
		static final Pattern begin_attribute = Pattern
				.compile("^begin_attribute \\(ID (\\d+), name \"([^\"]+)\", type \"([^\"]+)\"\\)$");

		/** The Constant end_attribute. */
		static final Pattern end_attribute = Pattern
				.compile("^end_attribute \\(ID (\\d+), name \"([^\"]+)\", type \"([^\"]+)\"\\)$");

		/** The loader. */
		final TextDbLoader loader;

		/** The transaction by id. */
		HashMap<Long, ScvTx> transactionById = new HashMap<>();

		/** The tx sink. */
		HTreeMap<Long, ScvTx> txSink;

		/** The reader. */
		BufferedReader reader = null;

		/** The generator. */
		TxGenerator generator = null;

		/** The attr value lut. */
		Map<String, Integer> attrValueLut = new HashMap<>();

		/**
		 * Instantiates a new text db parser.
		 *
		 * @param loader the loader
		 */
		public TextDbParser(TextDbLoader loader) {
			super();
			this.loader = loader;
		}

		/**
		 * Parses the input.
		 *
		 * @param inputStream the input stream
		 * @throws IOException Signals that an I/O exception has occurred.
		 * @throws InputFormatException Signals that the input format is wrong
		 */
		void parseInput(InputStream inputStream) throws IOException, InputFormatException {
			reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			String curLine = reader.readLine();
			String nextLine = null;
			while ((nextLine = reader.readLine()) != null && curLine != null) {
				curLine = parseLine(curLine, nextLine);
			}
			if (curLine != null)
				parseLine(curLine, nextLine);
			for(Entry<Long, ScvTx> e: transactionById.entrySet()) {
				ScvTx scvTx = e.getValue();
				scvTx.endTime=loader.maxTime;
				txSink.put(e.getKey(), scvTx);
			}
		}

		/**
		 * Gets the attr type.
		 *
		 * @param name     the name
		 * @param dataType the data type
		 * @param type     the type
		 * @return the attr type
		 */
		private TxAttributeType getAttrType(String name, DataType dataType, AssociationType type) {
			String key = name + "-" + dataType.toString();
			TxAttributeType res;
			if (loader.attributeTypes.containsKey(key)) {
				res = loader.attributeTypes.get(key);
			} else {
				res = new TxAttributeType(name, dataType, type);
				loader.attributeTypes.put(key, res);
			}
			return res;
		}

		/**
		 * Parses the line.
		 *
		 * @param curLine  the cur line
		 * @param nextLine the next line
		 * @return the string
		 * @throws IOException Signals that an I/O exception has occurred.
		 * @throws InputFormatException Signals that the input format is wrong
		 */
		private String parseLine(String curLine, String nextLine) throws IOException, InputFormatException {
			String[] tokens = curLine.split("\\s+");
			if ("tx_record_attribute".equals(tokens[0])) {
				Long id = Long.parseLong(tokens[1]);
				String name = tokens[2].substring(1, tokens[2].length()-1);
				DataType type = DataType.valueOf(tokens[3]);
				String remaining = tokens.length > 5 ? String.join(" ", Arrays.copyOfRange(tokens, 5, tokens.length)) : "";
				TxAttributeType attrType = getAttrType(name, type, AssociationType.RECORD);
				transactionById.get(id).attributes.add(new TxAttribute(attrType, getAttrString(attrType, remaining)));
			} else if ("tx_begin".equals(tokens[0])) {
				Long id = Long.parseLong(tokens[1]);
				Long genId = Long.parseLong(tokens[2]);
				TxGenerator gen = loader.txGenerators.get(genId);
				ScvTx scvTx = new ScvTx(id, gen.stream.getId(), genId,
						Long.parseLong(tokens[3]) * stringToScale(tokens[4]));
				loader.maxTime = loader.maxTime > scvTx.beginTime ? loader.maxTime : scvTx.beginTime;
				if (nextLine != null && nextLine.charAt(0) == 'a') {
					int idx = 0;
					while (nextLine != null && nextLine.charAt(0) == 'a') {
						String[] attrTokens = nextLine.split("\\s+");
						TxAttributeType attrType = gen.beginAttrs.get(idx);
						TxAttribute attr = new TxAttribute(attrType, getAttrString(attrType, attrTokens[1]));
						scvTx.attributes.add(attr);
						idx++;
						nextLine = reader.readLine();
					}
				}
				transactionById.put(id, scvTx);
			} else if ("tx_end".equals(tokens[0])) {
				Long id = Long.parseLong(tokens[1]);
				ScvTx scvTx = transactionById.get(id);
				assert Long.parseLong(tokens[2]) == scvTx.generatorId;
				scvTx.endTime = Long.parseLong(tokens[3]) * stringToScale(tokens[4]);
				loader.maxTime = loader.maxTime > scvTx.endTime ? loader.maxTime : scvTx.endTime;
				TxGenerator gen = loader.txGenerators.get(scvTx.generatorId);
				TxStream stream = loader.txStreams.get(gen.stream.getId());
				if (scvTx.beginTime == scvTx.endTime) {
					TxEvent evt = new TxEvent(loader, EventKind.SINGLE, id, scvTx.beginTime);
					stream.addEvent(evt);
					gen.addEvent(evt);
				} else {
					TxEvent begEvt = new TxEvent(loader, EventKind.BEGIN, id, scvTx.beginTime);
					stream.addEvent(begEvt);
					gen.addEvent(begEvt);
					TxEvent endEvt = new TxEvent(loader, EventKind.END, id, scvTx.endTime);
					stream.addEvent(endEvt);
					gen.addEvent(endEvt);
				}
				if (nextLine != null && nextLine.charAt(0) == 'a') {
					int idx = 0;
					while (nextLine != null && nextLine.charAt(0) == 'a') {
						String[] attrTokens = nextLine.split("\\s+");
						TxAttributeType attrType = gen.endAttrs.get(idx);
						TxAttribute attr = new TxAttribute(attrType, getAttrString(attrType, attrTokens[1]));
						scvTx.attributes.add(attr);
						idx++;
						nextLine = reader.readLine();
					}
				}
				txSink.put(scvTx.getId(), scvTx);
				transactionById.remove(id);
			} else if ("tx_relation".equals(tokens[0])) {
				Long tr2 = Long.parseLong(tokens[2]);
				Long tr1 = Long.parseLong(tokens[3]);
				String relType = tokens[1].substring(1, tokens[1].length() - 1);
				if (!loader.relationTypes.containsKey(relType))
					loader.relationTypes.put(relType, RelationTypeFactory.create(relType));
				ScvRelation rel = new ScvRelation(loader.relationTypes.get(relType), tr1, tr2);
				loader.relationsOut.put(tr1, rel);
				loader.relationsIn.put(tr2, rel);
			} else if ("scv_tr_stream".equals(tokens[0])) {
				Matcher matcher = scv_tr_stream.matcher(curLine);
				if (matcher.matches()) {
					Long id = Long.parseLong(matcher.group(1));
					TxStream stream = new TxStream(loader, id, matcher.group(2), matcher.group(3));
					add(id, stream);
				}
			} else if ("scv_tr_generator".equals(tokens[0])) {
				Matcher matcher = scv_tr_generator.matcher(curLine);
				if ((matcher.matches())) {
					Long id = Long.parseLong(matcher.group(1));
					TxStream stream = loader.txStreams.get(Long.parseLong(matcher.group(3)));
					generator = new TxGenerator(loader, id, matcher.group(2), stream);
					add(id, generator);
				}
			} else if ("begin_attribute".equals(tokens[0])) {
				Matcher matcher = begin_attribute.matcher(curLine);
				if ((matcher.matches())) {
					TxAttributeType attrType = getAttrType(matcher.group(2), DataType.valueOf(matcher.group(3)),
							AssociationType.BEGIN);
					generator.beginAttrs.add(attrType);
				}
			} else if ("end_attribute".equals(tokens[0])) {
				Matcher matcher = end_attribute.matcher(curLine);
				if ((matcher.matches())) {
					TxAttributeType attrType = getAttrType(matcher.group(2), DataType.valueOf(matcher.group(3)),
							AssociationType.END);
					generator.endAttrs.add(attrType);
				}
			} else if (")".equals(tokens[0])) {
				generator = null;
			} else
				throw new InputFormatException("Don't know what to do with: '" + curLine + "'");
			return nextLine;
		}

		/**
		 * Gets the attr string.
		 *
		 * @param attrType the attr type
		 * @param string   the string
		 * @return the attr string
		 */
		private String getAttrString(TxAttributeType attrType, String string) {
			String value;
			switch (attrType.getDataType()) {
			case STRING:
			case ENUMERATION:
				value = string.substring(1, string.length() - 1);
				break;
			default:
				value = string;
			}
			if (attrValueLut.containsKey(value)) {
				return loader.attrValues.get(attrValueLut.get(value));
			} else {
				attrValueLut.put(value, loader.attrValues.size());
				loader.attrValues.add(value);
				return value;
			}
		}

		/**
		 * String to scale.
		 *
		 * @param scale the scale
		 * @return the long
		 */
		private long stringToScale(String scale) {
			String cmp = scale.trim();
			if ("fs".equals(cmp))
				return 1L;
			if ("ps".equals(cmp))
				return 1000L;
			if ("ns".equals(cmp))
				return 1000000L;
			if ("us".equals(cmp))
				return 1000000000L;
			if ("ms".equals(cmp))
				return 1000000000000L;
			if ("s".equals(cmp))
				return 1000000000000000L;
			return 1L;
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
