/*******************************************************************************
 * Copyright (c) 2012 IT Just working
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.minres.scviewer.database.EventKind;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITxEvent;

/**
 * The Class AbstractTxStream.
 */
abstract class AbstractTxStream extends HierNode implements IWaveform {

	/** The id. */
	private Long id;

	/** The loader. */
	protected TextDbLoader loader;

	/** The events. */
	TreeMap<Long, IEvent[]> events = new TreeMap<>();

	/** The concurrency calculated. */
	boolean concurrencyCalculated = false;

	/**
	 * Instantiates a new abstract tx stream.
	 *
	 * @param loader the loader
	 * @param id     the id
	 * @param name   the name
	 */
	public AbstractTxStream(TextDbLoader loader, Long id, String name) {
		super(name);
		this.loader = loader;
		this.id = id;
	}

	/**
	 * Adds the event.
	 *
	 * @param evt the evt
	 */
	public void addEvent(ITxEvent evt) {
		if (!events.containsKey(evt.getTime()))
			events.put(evt.getTime(), new IEvent[] { evt });
		else {
			IEvent[] evts = events.get(evt.getTime());
			IEvent[] newEvts = Arrays.copyOf(evts, evts.length + 1);
			newEvts[evts.length] = evt;
			events.put(evt.getTime(), newEvts);
		}
	}

	/**
	 * Gets the events.
	 *
	 * @return the events
	 */
	@Override
	public NavigableMap<Long, IEvent[]> getEvents() {
		if (!concurrencyCalculated)
			calculateConcurrency();
		return events;
	}

	/**
	 * Gets the events at time.
	 *
	 * @param time the time
	 * @return the events at time
	 */
	@Override
	public IEvent[] getEventsAtTime(Long time) {
		if (!concurrencyCalculated)
			calculateConcurrency();
		return events.get(time);
	}

	/**
	 * Gets the events before time.
	 *
	 * @param time the time
	 * @return the events before time
	 */
	@Override
	public IEvent[] getEventsBeforeTime(Long time) {
		if (!concurrencyCalculated)
			calculateConcurrency();
		Entry<Long, IEvent[]> e = events.floorEntry(time);
		if (e == null)
			return new IEvent[] {};
		else
			return events.floorEntry(time).getValue();
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	@Override
	public WaveformType getType() {
		return WaveformType.TRANSACTION;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * Calculate concurrency.
	 */
	synchronized void calculateConcurrency() {
		if (concurrencyCalculated)
			return;
		ArrayList<Long> rowendtime = new ArrayList<>();
		events.entrySet().stream().forEach(entry -> {
			IEvent[] values = entry.getValue();
			Arrays.asList(values).stream().filter(e -> e.getKind() == EventKind.BEGIN).forEach(evt -> {
				Tx tx = (Tx) ((TxEvent) evt).getTransaction();
				int rowIdx = 0;
				for (; rowIdx < rowendtime.size() && rowendtime.get(rowIdx) > tx.getBeginTime(); rowIdx++)
					;
				if (rowendtime.size() <= rowIdx)
					rowendtime.add(tx.getEndTime());
				else
					rowendtime.set(rowIdx, tx.getEndTime());
				tx.setConcurrencyIndex(rowIdx);
			});
		});
		concurrencyCalculated = true;
	}

}
