/*******************************************************************************
 * Copyright (c) 2012 IT Just working.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT Just working - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.text;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.minres.scviewer.database.AssociationType;
import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.IWaveformDb;
import com.minres.scviewer.database.IWaveformDbLoader;
import com.minres.scviewer.database.InputFormatException;
import com.minres.scviewer.database.RelationType;
import com.minres.scviewer.database.tx.ITxGenerator;

public class TextDbLoader implements IWaveformDbLoader{

	private Long maxTime=0L;

	IWaveformDb db;

	List<IWaveform> streams;

	Map<String, RelationType> relationTypes=new HashMap<String, RelationType>();

	DB mapDb;

	public TextDbLoader() {
	}

	@Override
	public Long getMaxTime() {
		return maxTime;
	}

	@Override
	public Collection<IWaveform> getAllWaves() {
		return streams;
	}

	public Map<Long, ITxGenerator> getGeneratorsById() {
		TreeMap<Long, ITxGenerator> res = new TreeMap<Long, ITxGenerator>();
		for(IWaveform stream: streams){ for(ITxGenerator it: ((TxStream)stream).getGenerators()){res.put(it.getId(), it);} }
		return res;
	}

	static final byte[] x = "scv_tr_stream".getBytes();

	@Override
	public	boolean load(IWaveformDb db, File file) throws InputFormatException {
		if(file.isDirectory() || !file.exists()) return false;
		this.db=db;
		this.streams = new ArrayList<>();
		try {
			boolean gzipped = isGzipped(file);
			if(isTxfile(gzipped?new GZIPInputStream(new FileInputStream(file)):new FileInputStream(file))){
				File mapDbFile = File.createTempFile("."+file.getName(), null /*"tmp"*/, null /*file.parentFile*/);
				mapDbFile.delete();
				mapDbFile.deleteOnExit();
				this.mapDb = DBMaker
						.fileDB(mapDbFile)
						.fileMmapEnableIfSupported()
						.fileMmapPreclearDisable()
						.cleanerHackEnable()
						.allocateStartSize(64*1024*1024)
						.allocateIncrement(64*1024*1024)
						.make();
				// NPE here --->
				parseInput(gzipped?new GZIPInputStream(new FileInputStream(file)):new FileInputStream(file));
				for(IWaveform stream: streams){ stream.getWidth(); }
				return true;
			} else 
				return false;
		} catch(IllegalArgumentException|ArrayIndexOutOfBoundsException e) {
		} catch(Throwable e) {
			System.out.println("---->>> Exception "+e.toString()+" caught while loading database");
			e.printStackTrace();
		}
		return true;
	}

	private static boolean isTxfile(InputStream istream) {
		byte[] buffer = new byte[x.length];
		try {
			int readCnt = istream.read(buffer, 0, x.length);
			istream.close();
			if(readCnt==x.length){
				for(int i=0; i<x.length; i++)
					if(buffer[i]!=x[i]) return false;
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean isGzipped(File f) {
		try(InputStream is = new FileInputStream(f)) {
			byte [] signature = new byte[2];
			int nread = is.read( signature ); //read the gzip signature
			return nread == 2 && signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b;
		} catch (IOException e) {
			return false;
		}
	}

	private long stringToScale(String scale){
		String cmp = scale.trim();
		if("fs".equals(cmp)) return 1L;
		if("ps".equals(cmp)) return 1000L;
		if("ns".equals(cmp)) return 1000000L;
		if("us".equals(cmp)) return 1000000000L;
		if("ms".equals(cmp)) return 1000000000000L;
		if("s".equals(cmp) ) return 1000000000000000L;
		return 1L;
	}

	static final Pattern scv_tr_stream = Pattern.compile("^scv_tr_stream\\s+\\(ID (\\d+),\\s+name\\s+\"([^\"]+)\",\\s+kind\\s+\"([^\"]+)\"\\)$");
	static final Pattern scv_tr_generator = Pattern.compile("^scv_tr_generator\\s+\\(ID\\s+(\\d+),\\s+name\\s+\"([^\"]+)\",\\s+scv_tr_stream\\s+(\\d+),$");
	static final Pattern begin_attribute = Pattern.compile("^begin_attribute \\(ID (\\d+), name \"([^\"]+)\", type \"([^\"]+)\"\\)$");
	static final Pattern end_attribute = Pattern.compile("^end_attribute \\(ID (\\d+), name \"([^\"]+)\", type \"([^\"]+)\"\\)$");

	HashMap<Long, TxStream> streamsById = new HashMap<Long, TxStream>();
	HashMap<Long, TxGenerator> generatorsById = new HashMap<Long, TxGenerator>();
	HashMap<Long, Tx> transactionsById = new HashMap<Long, Tx>();
	TxGenerator generator = null;
	Tx transaction = null;
	boolean endTransaction=false;
	BufferedReader reader =null;
	
	private void parseInput(InputStream inputStream) throws IOException{
		reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		String curLine = reader.readLine();
		String nextLine = null;
		while((nextLine=reader.readLine())!=null && curLine!=null) {
			curLine=parseLine(curLine, nextLine);
		}
		if(curLine!=null)
			parseLine(curLine, nextLine);
	}

	private String parseLine(String curLine, String nextLine) throws IOException{
		String[] tokens = curLine.split("\\s+");
		if("tx_record_attribute".equals(tokens[0])){//matcher = line =~ /^tx_record_attribute\s+(\d+)\s+"([^"]+)"\s+(\S+)\s*=\s*(.+)$/
			Long id = Long.parseLong(tokens[1]);
			String name = tokens[2].substring(1, tokens[2].length());
			DataType type = DataType.valueOf(tokens[3]);
			String remaining = tokens.length>5?String.join(" ", Arrays.copyOfRange(tokens, 5, tokens.length-1)):"";
			transactionsById.get(id).getAttributes().add(new TxAttribute(name, type, AssociationType.RECORD, remaining));
		} else if("tx_begin".equals(tokens[0])){//matcher = line =~ /^tx_begin\s+(\d+)\s+(\d+)\s+(\d+)\s+([munpf]?s)/
			Long id = Long.parseLong(tokens[1]);
			TxGenerator gen=generatorsById.get(Long.parseLong(tokens[2]));
			transaction = new Tx(id, gen.getStream(), gen, Long.parseLong(tokens[3])*stringToScale(tokens[4]));
			gen.getTransactions().add(transaction);
			transactionsById.put(id, transaction);
			maxTime = maxTime>transaction.getBeginTime()?maxTime:transaction.getBeginTime();
			if(nextLine!=null && nextLine.charAt(0)=='a') {
				int idx=0;
				while(nextLine!=null && nextLine.charAt(0)=='a') {
					String[] attrTokens=nextLine.split("\\s+");
					TxAttribute attr = new TxAttribute(gen.getBeginAttrs().get(idx), attrTokens[1]);
					transaction.getAttributes().add(attr);
					idx++;
					nextLine=reader.readLine();
				}
			}
		} else if("tx_end".equals(tokens[0])){//matcher = line =~ /^tx_end\s+(\d+)\s+(\d+)\s+(\d+)\s+([munpf]?s)/
			Long id = Long.parseLong(tokens[1]);
			transaction = transactionsById.get(id);
			assert Integer.parseInt(tokens[2])==transaction.getGenerator().getId();
			transaction.setEndTime(Long.parseLong(tokens[3])*stringToScale(tokens[4]));
			maxTime = maxTime>transaction.getEndTime()?maxTime:transaction.getEndTime();
			if(nextLine!=null && nextLine.charAt(0)=='a') {
				TxGenerator gen = (TxGenerator) transaction.getGenerator();
				int idx=0;
				while(nextLine!=null && nextLine.charAt(0)=='a') {
					String[] attrTokens=nextLine.split("\\s+");
					TxAttribute attr = new TxAttribute(gen.getEndAttrs().get(idx), attrTokens[1]);
					transaction.getAttributes().add(attr);
					idx++;
					nextLine=reader.readLine();
				}
			}
		} else if("tx_relation".equals(tokens[0])){//matcher = line =~ /^tx_relation\s+\"(\S+)\"\s+(\d+)\s+(\d+)$/
			Tx tr2= transactionsById.get(Long.parseLong(tokens[2]));
			Tx tr1= transactionsById.get(Long.parseLong(tokens[3]));
			String relType=tokens[1].substring(1, tokens[1].length()-2);
			if(!relationTypes.containsKey(relType))
				relationTypes.put(relType, RelationType.create(relType));
			TxRelation rel = new TxRelation(relationTypes.get(relType), tr1, tr2);
			tr1.getOutgoingRelations().add(rel);
			tr2.getIncomingRelations().add(rel);
		} else if("scv_tr_stream".equals(tokens[0])){
			Matcher matcher = scv_tr_stream.matcher(curLine);
			if (matcher.matches()) {
				Long id = Long.parseLong(matcher.group(1));
				TxStream stream = new TxStream(this, id, matcher.group(2), matcher.group(3));
				streams.add(stream);
				streamsById.put(id, stream);
			}
		} else if("scv_tr_generator".equals(tokens[0])){
			Matcher matcher = scv_tr_generator.matcher(curLine);
			if ((matcher.matches())) {
				Long id = Long.parseLong(matcher.group(1));
				TxStream stream=streamsById.get(Long.parseLong(matcher.group(3)));
				generator=new TxGenerator(id, stream, matcher.group(2));
				stream.getGenerators().add(generator);
				generatorsById.put(id,  generator);
			}
		} else if("begin_attribute".equals(tokens[0])){
			Matcher matcher = begin_attribute.matcher(curLine);
			if ((matcher.matches())) {
				generator.getBeginAttrs().add(TxAttributeType.getAttrType(matcher.group(2), DataType.valueOf(matcher.group(3)), AssociationType.BEGIN));
			}
		} else if("end_attribute".equals(tokens[0])){
			Matcher matcher = end_attribute.matcher(curLine);
			if ((matcher.matches())) {
				generator.getEndAttrs().add(TxAttributeType.getAttrType(matcher.group(2), DataType.valueOf(matcher.group(3)), AssociationType.END));
			}
		} else if(")".equals(tokens[0])){
			generator=null;
		} else if("a".equals(tokens[0])){//matcher = line =~ /^a\s+(.+)$/
			System.out.println("Don't know what to do with: '"+curLine+"'");
		} else
			System.out.println("Don't know what to do with: '"+curLine+"'");
		return nextLine;
	}

	public Collection<RelationType> getAllRelationTypes(){
		return relationTypes.values();
	}

}

