/*******************************************************************************
 * Copyright (c) 2015 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database.vcd;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import com.minres.scviewer.database.BitValue;
import com.minres.scviewer.database.BitVector;

class VCDFileParser {
	private StreamTokenizer tokenizer;
	private IVCDDatabaseBuilder traceBuilder;
	private HashMap<String, Integer> nameToNetMap = new HashMap<>();
	private long picoSecondsPerIncrement;
	private boolean stripNetWidth;
	private boolean replaceColon;
	long currentTime;

	public VCDFileParser(boolean stripNetWidth) {
		this.stripNetWidth=stripNetWidth;
		this.replaceColon=false;
	}

	public boolean load(InputStream is, IVCDDatabaseBuilder builder) {
		tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(is)));
		tokenizer.resetSyntax();
		tokenizer.wordChars(33, 126);
		tokenizer.whitespaceChars('\r', '\r');
		tokenizer.whitespaceChars('\n', '\n');
		tokenizer.whitespaceChars(' ', ' ');
		tokenizer.whitespaceChars('\t', '\t');
		try {
			traceBuilder = builder;
			currentTime=0;
			while (parseDefinition());
			while (parseTransition());
			return true;
		} catch (Exception exc) {
			exc.printStackTrace();
			return false;
		}
	}

	private void parseScope() throws IOException, ParseException {
		nextToken(); // Scope type (ignore)
		nextToken();
		traceBuilder.enterModule(tokenizer.sval);
		match("$end");
	}

	private void parseUpscope() throws IOException, ParseException {
		match("$end");
		traceBuilder.exitModule();
	}

	private void parseVar() throws IOException {
		nextToken(); // type
		String type = tokenizer.sval;
		nextToken(); // size
		int width = Integer.parseInt(tokenizer.sval);
		if("real".equals(type))
			width=0;
		nextToken();
		String id = tokenizer.sval;
		nextToken();
		StringBuilder sb = new StringBuilder();
		sb.append(tokenizer.sval);
		while (nextToken() && !tokenizer.sval.equals("$end")) {
			sb.append(tokenizer.sval);
		}
		String netName = sb.toString();
		Integer net = nameToNetMap.get(id);
		if (net == null) { // We've never seen this net before
			int openBracket = netName.indexOf('[');
			if(stripNetWidth){
				if (openBracket != -1) netName = netName.substring(0, openBracket);
				openBracket = -1;
			}
			if(replaceColon) {
				if (openBracket != -1) {
					netName = netName.substring(0, openBracket).replace(":", ".")+netName.substring(openBracket);
				} else
					netName=netName.replace(":", ".");
			}
			nameToNetMap.put(id, traceBuilder.newNet(netName, -1, width));
		} else {
			// Shares data with existing net. Add as clone.
			traceBuilder.newNet(netName, net, width);
		}
	}

	private void parseComment() throws IOException {
		nextToken();
		StringBuilder s = new StringBuilder();
		s.append(tokenizer.sval);
		nextToken();
		while(!tokenizer.sval.equals("$end")){
			s.append(" ").append(tokenizer.sval);
			nextToken();
		}
		replaceColon|=s.toString().contains("ARTERIS Architecture");
	}

	private void parseTimescale() throws IOException {
		nextToken();
		StringBuilder sb = new StringBuilder();
		sb.append(tokenizer.sval);
		nextToken();
		while(!tokenizer.sval.equals("$end")){
			sb.append(" ").append(tokenizer.sval);
			nextToken();
		}
		String s = sb.toString();
		switch (s.charAt(s.length() - 2)){
		case 'p': // Nano-seconds
			picoSecondsPerIncrement = 1;
			s = s.substring(0, s.length() - 2).trim();
			break;
		case 'n': // Nano-seconds
			picoSecondsPerIncrement = 1000;
			s = s.substring(0, s.length() - 2).trim();
			break;
		case 'u': // Microseconds
			picoSecondsPerIncrement = 1000000;
			s = s.substring(0, s.length() - 2).trim();
			break;
		case 'm': // Microseconds
			picoSecondsPerIncrement = 1000000000;
			s = s.substring(0, s.length() - 2).trim();
			break;
		default: // Seconds
			picoSecondsPerIncrement = 1000000000000L;
			s = s.substring(0, s.length() - 1);
			break;
		}
		picoSecondsPerIncrement *= Long.parseLong(s);
	}

	private boolean parseDefinition() throws IOException, ParseException {
		nextToken();
		if (tokenizer.sval.equals("$scope"))
			parseScope();
		else if (tokenizer.sval.equals("$var"))
			parseVar();
		else if (tokenizer.sval.equals("$upscope"))
			parseUpscope();
		else if (tokenizer.sval.equals("$timescale"))
			parseTimescale();
		else if (tokenizer.sval.equals("$comment")) 
			parseComment();
		else if (tokenizer.sval.equals("$enddefinitions")) {
			match("$end");
			return false;
		} else {
			// Ignore this defintion
			do {
				if (!nextToken()) return false;
			} while (!tokenizer.sval.equals("$end"));
		}

		return true;
	}

	private boolean parseTransition() throws IOException {
		if (!nextToken()) return false;
		if (tokenizer.sval.charAt(0) == '#') {	// If the line begins with a #, this is a timestamp.
			currentTime = Long.parseLong(tokenizer.sval.substring(1)) * picoSecondsPerIncrement;
		} else {
			if(tokenizer.sval.equals("$comment")){
				do {
					if (!nextToken()) return false;
				} while (!tokenizer.sval.equals("$end"));
				return true;
			}
			if (tokenizer.sval.equals("$dumpvars") || tokenizer.sval.equals("$end"))
				return true;
			String value;
			String id;
			if (tokenizer.sval.charAt(0) == 'b' || tokenizer.sval.charAt(0) == 'r') {
				// Multiple value net. Value appears first, followed by space, then identifier
				value = tokenizer.sval.substring(1);
				nextToken();
				id = tokenizer.sval;
			} else {
				// Single value net. identifier first, then value, no space.
				value = tokenizer.sval.substring(0, 1);
				id = tokenizer.sval.substring(1);
			}

			Integer net = nameToNetMap.get(id);
			if (net == null) 
				return true;

			int netWidth = traceBuilder.getNetWidth(net);
			if(netWidth==0) {
				if("nan".equals(value))
					traceBuilder.appendTransition(net, currentTime, Double.NaN);
				else
					traceBuilder.appendTransition(net, currentTime, Double.parseDouble(value));
			} else {
				BitVector decodedValues = new BitVector(netWidth);
				if (value.equals("z") && netWidth > 1) {
					for (int i = 0; i < netWidth; i++)
						decodedValues.setValue(i, BitValue.Z);
				} else if (value.equals("x") && netWidth > 1) {
					for (int i = 0; i < netWidth; i++)
						decodedValues.setValue(i, BitValue.X);
				} else {
					int stringIndex = 0;
					for (int convertedIndex = netWidth -1; convertedIndex >=0; convertedIndex--) {
						if(convertedIndex<value.length()) {
							switch (value.charAt(stringIndex++)) {
							case 'z':
								decodedValues.setValue(convertedIndex, BitValue.Z);
								break;

							case '1':
								decodedValues.setValue(convertedIndex, BitValue.ONE);
								break;

							case '0':
								decodedValues.setValue(convertedIndex, BitValue.ZERO);
								break;

							case 'x':
								decodedValues.setValue(convertedIndex, BitValue.X);
								break;

							default:
								decodedValues.setValue(convertedIndex, BitValue.X);
							}
						} else {
							decodedValues.setValue(convertedIndex, BitValue.ZERO);
						}
					}
				}
				traceBuilder.appendTransition(net, currentTime, decodedValues);
			}
		}
		return true;
	}

	private void match(String value) throws ParseException, IOException {
		nextToken();
		if (!tokenizer.sval.equals(value)) 
			throw new ParseException("Line "+tokenizer.lineno()+": parse error, expected "+value+" got "+tokenizer.sval, tokenizer.lineno());
	}

	private boolean nextToken() throws IOException {
		return tokenizer.nextToken() != StreamTokenizer.TT_EOF;
	}

}
