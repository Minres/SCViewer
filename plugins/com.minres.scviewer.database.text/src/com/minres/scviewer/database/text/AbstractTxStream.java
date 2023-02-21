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
import java.util.HashMap;

import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxEvent;

/**
 * The Class AbstractTxStream.
 */
abstract class AbstractTxStream extends HierNode implements IWaveform {

	private final String fullName;

	/** The id. */
	private Long id;

	/** The loader. */
	protected TextDbLoader loader;

	/** The events. */
	IEventList events = new EventList();

	/** The max concurrency. */
	private int rowCount = -1;

	/**
	 * Instantiates a new abstract tx stream.
	 *
	 * @param loader the loader
	 * @param id     the id
	 * @param name   the name
	 */
	protected AbstractTxStream(TextDbLoader loader, Long id, String name) {
		super(name);
		fullName=name;
		this.loader = loader;
		this.id = id;
	}

	/**
	 * Gets the full hierarchical name.
	 *
	 * @return the full name
	 */
	@Override
	public String getFullName() {
		return  fullName;
	}
	/**
	 * Adds the event.
	 *
	 * @param evt the evt
	 */
	public void addEvent(ITxEvent evt) {
		events.put(evt.getTime(), evt);
	}

	/**
	 * Gets the events.
	 *
	 * @return the events
	 */
	@Override
	public IEventList getEvents() {
		return events;
	}

	/**
	 * Gets the events at time.
	 *
	 * @param time the time
	 * @return the events at time
	 */
	@Override
	public IEvent[] getEventsAtTime(long time) {
		return events.get(time);
	}

	/**
	 * Gets the events before time.
	 *
	 * @param time the time
	 * @return the events before time
	 */
	@Override
	public IEvent[] getEventsBeforeTime(long time) {
		EventEntry e = events.floorEntry(time);
		if (e == null)
			return new IEvent[] {};
		else
			return events.floorEntry(time).events;
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
	public long getId() {
		return id;
	}

	/**
	 * Gets the width.
	 *
	 * @return the width
	 */
	@Override
	public int getRowCount() {
		if (rowCount<0)
			calculateConcurrency();
		return rowCount;
	}

	/**
	 * Calculate concurrency.
	 */
	void calculateConcurrency() {
		if (rowCount>=0)
			return;
		ArrayList<Long> rowEndTime = new ArrayList<>();
		HashMap<Long, Integer> rowByTxId = new HashMap<>();
		for(EventEntry entry: events) {
			for(IEvent evt:entry.events) {
				TxEvent txEvt = (TxEvent) evt;
				ITx tx = txEvt.getTransaction();
				int rowIdx = 0;
				switch(evt.getKind()) {
				case END:
					Long txId = txEvt.getTransaction().getId();
					txEvt.setConcurrencyIndex(rowByTxId.get(txId));
					rowByTxId.remove(txId);
					break;
				case SINGLE:
					for (; rowIdx < rowEndTime.size() && rowEndTime.get(rowIdx)>tx.getBeginTime(); rowIdx++);
					if (rowEndTime.size() <= rowIdx)
						rowEndTime.add(tx.getEndTime());
					else
						rowEndTime.set(rowIdx, tx.getEndTime());
					((TxEvent) evt).setConcurrencyIndex(rowIdx);
					break;
				case BEGIN:
					for (; rowIdx < rowEndTime.size() && rowEndTime.get(rowIdx)>tx.getBeginTime(); rowIdx++);
					if (rowEndTime.size() <= rowIdx)
						rowEndTime.add(tx.getEndTime());
					else
						rowEndTime.set(rowIdx, tx.getEndTime());
					((TxEvent) evt).setConcurrencyIndex(rowIdx);
					rowByTxId.put(tx.getId(), rowIdx);
					break;
				}
			}
		}
		rowCount=rowEndTime.size()>0?rowEndTime.size():1;
		//getChildNodes().parallelStream().forEach(c -> ((TxGenerator)c).calculateConcurrency());
		getChildNodes().stream().forEach(c -> ((TxGenerator)c).calculateConcurrency());
	}
}
