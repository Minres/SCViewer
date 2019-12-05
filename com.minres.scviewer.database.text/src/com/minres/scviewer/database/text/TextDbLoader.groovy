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

import org.codehaus.groovy.ast.stmt.CatchStatement
import org.mapdb.DB
import org.mapdb.DBMaker

import groovy.io.FileType

import com.minres.scviewer.database.AssociationType
import com.minres.scviewer.database.DataType
import com.minres.scviewer.database.ITxGenerator
import com.minres.scviewer.database.ITxStream
import com.minres.scviewer.database.IWaveform
import com.minres.scviewer.database.IWaveformDb
import com.minres.scviewer.database.IWaveformDbLoader
import com.minres.scviewer.database.RelationType
import com.minres.scviewer.database.DataType

public class TextDbLoader implements IWaveformDbLoader{

	private Long maxTime;

	IWaveformDb db;

	def streams = []

	def relationTypes=[:]

	DB mapDb
	
	public TextDbLoader() {
	}

	@Override
	public Long getMaxTime() {
		return maxTime;
	}

	@Override
	public List<IWaveform> getAllWaves() {
		return new LinkedList<IWaveform>(streams);
	}

	public Map<Long, ITxGenerator> getGeneratorsById() {
		TreeMap<Long, ITxGenerator> res = new TreeMap<Long, ITxGenerator>();
		streams.each{TxStream stream -> stream.generators.each{res.put(it.id, id)} }
		return res;
	}

	static final byte[] x = "scv_tr_stream".bytes

	@Override
	boolean load(IWaveformDb db, File file) throws Exception {
		this.db=db
		this.streams=[]
		try {
			def gzipped = isGzipped(file)
			if(isTxfile(gzipped?new GZIPInputStream(new FileInputStream(file)):new FileInputStream(file))){
				def mapDbFile = File.createTempFile("."+file.name, "tmp", file.parentFile)
				mapDbFile.delete()
				mapDbFile.deleteOnExit()
				this.mapDb = DBMaker
				.fileDB(mapDbFile)
				.fileMmapEnableIfSupported()
				.fileMmapPreclearDisable()
				.cleanerHackEnable()
				.allocateStartSize(64*1024*1024)
				.allocateIncrement(64*1024*1024)
				.make()
				// NPE here --->
				parseInput(gzipped?new GZIPInputStream(new FileInputStream(file)):new FileInputStream(file))
				calculateConcurrencyIndicees()
				return true
			}
		} catch(EOFException e) {
			return true;
		} catch(Exception e) {
			System.out.println("---->>> Exception caught while loading database. StackTrace following... ");
			e.printStackTrace()
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
		def streamsById = [:]
		def generatorsById = [:]
		def transactionsById = [:]
		TxGenerator generator
		Tx transaction
		boolean endTransaction=false
		def matcher
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		long lineCnt=0;
		reader.eachLine { line ->
			def tokens = line.split(/\s+/) as ArrayList
			switch(tokens[0]){
				case "scv_tr_stream":
				case "scv_tr_generator":
				case "begin_attribute":
				case "end_attribute":
					if ((matcher = line =~ /^scv_tr_stream\s+\(ID (\d+),\s+name\s+"([^"]+)",\s+kind\s+"([^"]+)"\)$/)) {
						def id = Integer.parseInt(matcher[0][1])
						def stream = new TxStream(this, id, matcher[0][2], matcher[0][3])
						streams<<stream
						streamsById[id]=stream
					} else if ((matcher = line =~ /^scv_tr_generator\s+\(ID\s+(\d+),\s+name\s+"([^"]+)",\s+scv_tr_stream\s+(\d+),$/)) {
						def id = Integer.parseInt(matcher[0][1])
						ITxStream stream=streamsById[Integer.parseInt(matcher[0][3])]
						generator=new TxGenerator(id, stream, matcher[0][2])
						stream.generators<<generator
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
					def id = Integer.parseInt(tokens[1])
					TxGenerator gen=generatorsById[Integer.parseInt(tokens[2])]
					transaction = new Tx(id, gen.stream, gen, Long.parseLong(tokens[3])*stringToScale(tokens[4]))
					gen.transactions << transaction
					transactionsById[id]= transaction
					gen.begin_attrs_idx=0;
					maxTime = maxTime>transaction.beginTime?maxTime:transaction.beginTime
					endTransaction=false
					break
				case "tx_end"://matcher = line =~ /^tx_end\s+(\d+)\s+(\d+)\s+(\d+)\s+([munpf]?s)/
					def id = Integer.parseInt(tokens[1])
					transaction = transactionsById[id]
					assert Integer.parseInt(tokens[2])==transaction.generator.id
					transaction.endTime = Long.parseLong(tokens[3])*stringToScale(tokens[4])
					transaction.generator.end_attrs_idx=0;
					maxTime = maxTime>transaction.endTime?maxTime:transaction.endTime
					endTransaction=true
					break
				case "tx_record_attribute"://matcher = line =~ /^tx_record_attribute\s+(\d+)\s+"([^"]+)"\s+(\S+)\s*=\s*(.+)$/
					def id = Integer.parseInt(tokens[1])
					def name = tokens[2][1..-2]
					def type = tokens[3] as DataType
					def remaining = tokens.size()>5?tokens[5..-1].join(' '):""
					transactionsById[id].attributes<<new TxAttribute(name, type, AssociationType.RECORD, remaining)
					break
				case "a"://matcher = line =~ /^a\s+(.+)$/
					if(endTransaction){
						transaction.attributes << new TxAttribute(transaction.generator.end_attrs[transaction.generator.end_attrs_idx], tokens[1])
						transaction.generator.end_attrs_idx++
					} else {
						transaction.attributes << new TxAttribute(transaction.generator.begin_attrs[transaction.generator.begin_attrs_idx], tokens[1])
						transaction.generator.begin_attrs_idx++
					}
					break
				case "tx_relation"://matcher = line =~ /^tx_relation\s+\"(\S+)\"\s+(\d+)\s+(\d+)$/
					Tx tr2= transactionsById[Integer.parseInt(tokens[2])]
					Tx tr1= transactionsById[Integer.parseInt(tokens[3])]
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
	}

	private def toDataType(String str){
		switch (str)
		{
			case "BOOLEAN": return DataType.                       BOOLEAN                      
			case "ENUMERATION": return DataType.                   ENUMERATION                  
			case "INTEGER": return DataType.                       INTEGER                      
			case "UNSIGNED": return DataType.                      UNSIGNED                     
			case "FLOATING_POINT_NUMBER": return DataType.         FLOATING_POINT_NUMBER        
			case "BIT_VECTOR": return DataType.                    BIT_VECTOR                   
			case "LOGIC_VECTOR": return DataType.                  LOGIC_VECTOR                 
			case "FIXED_POINT_INTEGER": return DataType.           FIXED_POINT_INTEGER          
			case "UNSIGNED_FIXED_POINT_INTEGER": return DataType.  UNSIGNED_FIXED_POINT_INTEGER 
			case "RECORD": return DataType.                        RECORD                       
			case "POINTER": return DataType.                       POINTER                      
			case "ARRAY": return DataType.                         ARRAY                        
			case "STRING": return DataType.                        STRING
			default: return DataType.INTEGER
		}
	}
	
	private def calculateConcurrencyIndicees(){
		streams.each{ TxStream  stream -> stream.getMaxConcurrency() }
	}


	public Collection<RelationType> getAllRelationTypes(){
		return relationTypes.values();
	}

}

