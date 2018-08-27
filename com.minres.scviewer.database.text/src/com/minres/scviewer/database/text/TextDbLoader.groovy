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

import java.nio.charset.CharsetDecoder;
import java.util.Collection;
import java.util.zip.GZIPInputStream
import org.apache.jdbm.DB
import org.apache.jdbm.DBMaker
import groovy.io.FileType

import com.minres.scviewer.database.AssociationType
import com.minres.scviewer.database.DataType
import com.minres.scviewer.database.ITxGenerator
import com.minres.scviewer.database.ITxStream
import com.minres.scviewer.database.IWaveform
import com.minres.scviewer.database.IWaveformDb
import com.minres.scviewer.database.IWaveformDbLoader
import com.minres.scviewer.database.RelationType

public class TextDbLoader implements IWaveformDbLoader, Serializable{

	private Long maxTime;

	transient IWaveformDb db;

	transient DB backingDb;

	transient def streamsById = [:]
	
	transient def generatorsById = [:]
	
	transient def transactionsById = [:]

	transient def relationTypes=[:]

	public TextDbLoader() {
	}

	@Override
	public Long getMaxTime() {
		return maxTime;
	}

	@Override
	public List<IWaveform> getAllWaves() {
		return new LinkedList<IWaveform>(streamsById.values());
	}

//	public Map<Long, ITxGenerator> getGeneratorsById() {
//		TreeMap<Long, ITxGenerator> res = new TreeMap<Long, ITxGenerator>();
//		streamsById.values().each{TxStream stream -> 
//			stream.generators.each{res.put(it.id, id)} }
//		return res;
//	}

	static final byte[] x = "scv_tr_stream".bytes

	@Override
	boolean load(IWaveformDb db, File file) throws Exception {
		this.db=db
		def gzipped = isGzipped(file)
		if(isTxfile(gzipped?new GZIPInputStream(new FileInputStream(file)):new FileInputStream(file))){
			def parentDir=file.absoluteFile.parent
			def filename=file.name
			new File(parentDir).eachFileRecurse (FileType.FILES) { f -> if(f.name=~/^\.${filename}/) f.delete() }
			this.backingDb = DBMaker.openFile(parentDir+File.separator+"."+filename+"_bdb")
					.deleteFilesAfterClose()
					.useRandomAccessFile()
					//.enableHardCache()
					.enableMRUCache()
					.setMRUCacheSize(1024*4096)
					.disableTransactions()
					.disableLocking()
					.make();
			streamsById = backingDb.createHashMap("streamsById")
			generatorsById = backingDb.createHashMap("generatorsById")
			transactionsById = backingDb.createHashMap("transactionsById")
			relationTypes=backingDb.createHashMap("relationTypes")				
			parseInput(gzipped?new GZIPInputStream(new FileInputStream(file)):new FileInputStream(file))
			calculateConcurrencyIndicees()
			return true
		}
		return false;
	}

	private static boolean isTxfile(InputStream istream) {
		byte[] buffer = new byte[x.size()]
		def readCnt = istream.read(buffer, 0, x.size())
		istream.close()
		if(readCnt==x.size()){
			for(int i=0; i<x.size(); i++)
				if(buffer[i]!=x[i]) return false
		}
		return true
	}

	private static boolean isGzipped(File f) {
		InputStream is = null;
		try {
			is = new FileInputStream(f);
			byte [] signature = new byte[2];
			int nread = is.read( signature ); //read the gzip signature
			return nread == 2 && signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b;
		} catch (IOException e) {
			return false;
		} finally {
			is.close()
		}
	}

	private stringToScale(String scale){
		switch(scale.trim()){
			case "fs":return 1L
			case "ps":return 1000L
			case "ns":return 1000000L
			case "us":return 1000000000L
			case "ms":return 1000000000000L
			case "s": return 1000000000000000L
		}
	}
	private def parseInput(InputStream inputStream){
		//def transactionsById = backingDb.createHashMap("transactionsById")
		TxGenerator generator
		Tx transaction
		boolean endTransaction=false
		def matcher
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		long lineCnt=0;
		reader.eachLine { line ->
			def tokens = line.split(/\s+/)
			switch(tokens[0]){
				case "scv_tr_stream":
				case "scv_tr_generator":
				case "begin_attribute":
				case "end_attribute":
					if ((matcher = line =~ /^scv_tr_stream\s+\(ID (\d+),\s+name\s+"([^"]+)",\s+kind\s+"([^"]+)"\)$/)) {
						def id = Long.parseLong(matcher[0][1])
						def stream = new TxStream(this, id, matcher[0][2], matcher[0][3])
						streamsById[id]=stream
					} else if ((matcher = line =~ /^scv_tr_generator\s+\(ID\s+(\d+),\s+name\s+"([^"]+)",\s+scv_tr_stream\s+(\d+),$/)) {
						def id = Long.parseLong(matcher[0][1])
						ITxStream stream=streamsById[Long.parseLong(matcher[0][3])]
						generator=new TxGenerator(this, id, stream.id, matcher[0][2])
						stream.generators<<id
						generatorsById[id]=generator
					} else if ((matcher = line =~ /^begin_attribute \(ID (\d+), name "([^"]+)", type "([^"]+)"\)$/)) {
						generator.begin_attrs << TxAttributeType.getAttrType(matcher[0][2], DataType.valueOf(matcher[0][3]), AssociationType.BEGIN)
					} else if ((matcher = line =~ /^end_attribute \(ID (\d+), name "([^"]+)", type "([^"]+)"\)$/)) {
						generator.end_attrs << TxAttributeType.getAttrType(matcher[0][2], DataType.valueOf(matcher[0][3]), AssociationType.END)
					}
					break;
				case ")":
					generator=null
					break
				case "tx_begin"://matcher = line =~ /^tx_begin\s+(\d+)\s+(\d+)\s+(\d+)\s+([munpf]?s)/
					def id = Long.parseLong(tokens[1])
					TxGenerator gen=generatorsById[Long.parseLong(tokens[2])]
					transaction = new Tx(this, id, gen.id, Long.parseLong(tokens[3])*stringToScale(tokens[4]))
					gen.transactions << transaction
					transactionsById[id]= transaction
					gen.begin_attrs_idx=0;
					maxTime = maxTime>transaction.beginTime?maxTime:transaction.beginTime
					endTransaction=false
					break
				case "tx_end"://matcher = line =~ /^tx_end\s+(\d+)\s+(\d+)\s+(\d+)\s+([munpf]?s)/
					def id = Long.parseLong(tokens[1])
					transaction = transactionsById[id]
					assert Long.parseLong(tokens[2])==transaction.generator.id
					transaction.endTime = Long.parseLong(tokens[3])*stringToScale(tokens[4])
					transaction.generator.end_attrs_idx=0;
					maxTime = maxTime>transaction.endTime?maxTime:transaction.endTime
					endTransaction=true
					break
				case "tx_record_attribute"://matcher = line =~ /^tx_record_attribute\s+(\d+)\s+"([^"]+)"\s+(\S+)\s*=\s*(.+)$/
					def id = Long.parseLong(tokens[1])
					transactionsById[id].attributes<<new TxAttribute(tokens[2][1..-2], DataType.valueOf(tokens[3]), AssociationType.RECORD, tokens[5..-1].join(' '))
					break
				case "a"://matcher = line =~ /^a\s+(.+)$/
					if(endTransaction){
						transaction.attributes << new TxAttribute(transaction.generator.end_attrs[0], tokens[1])
					} else {
						transaction.attributes << new TxAttribute(transaction.generator.begin_attrs[0], tokens[1])
					}
					break
				case "tx_relation"://matcher = line =~ /^tx_relation\s+\"(\S+)\"\s+(\d+)\s+(\d+)$/
					Tx tr2= transactionsById[Long.parseLong(tokens[2])]
					Tx tr1= transactionsById[Long.parseLong(tokens[3])]
					def relType=tokens[1][1..-2]
					if(!relationTypes.containsKey(relType)) relationTypes[relType]=RelationType.create(relType)
					def rel = new TxRelation(relationTypes[relType], tr1, tr2)
					tr1.outgoingRelations<<rel
					tr2.incomingRelations<<rel
					break
				default:
					println "Don't know what to do with: '$line'"

			}
			lineCnt++
		}
		backingDb.commit();
	}

	private def calculateConcurrencyIndicees(){
		streamsById.values().each{ TxStream  stream -> 
			stream.getMaxConcurrency() 
		}
	}


	public Collection<RelationType> getAllRelationTypes(){
		return relationTypes.values();
	}

}

