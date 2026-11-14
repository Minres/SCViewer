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


import com.minres.scviewer.database.EventEntry;
import com.minres.scviewer.database.EventList;
import com.minres.scviewer.database.HierNode;
import com.minres.scviewer.database.IEvent;
import com.minres.scviewer.database.IEventList;
import com.minres.scviewer.database.IWaveform;
import com.minres.scviewer.database.WaveformType;
import com.minres.scviewer.database.tx.TxRowAllocator;
import com.minres.scviewer.database.tx.ITxEvent;

/**
 * The Class AbstractTxStream.
 */
abstract class AbstractTxStream extends HierNode implements IWaveform {

	private final String fullName;

	/** The id. */
	private Long id;

	/** The loader. */
	protected FtrDbLoader loader;

	/** The events. */
	protected IEventList events = new EventList();
    private EventList.Builder eventBuilder = new EventList.Builder();

	/** The max concurrency. */
	private int rowCount = -1;

	/**
	 * Instantiates a new abstract tx stream.
	 *
	 * @param loader the loader
	 * @param id     the id
	 * @param name   the name
	 */
	protected AbstractTxStream(FtrDbLoader loader, Long id, String name) {
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
		eventBuilder.add(evt.getTime(), evt);
	}

    void finishEvents() {
        if (eventBuilder != null) {
            events = eventBuilder.build();
            eventBuilder = null;
        }
    }

	/**
	 * Gets the events at time.
	 *
	 * @param time the time
	 * @return the events at time
	 */
	@Override
	public IEvent[] getEventsAtTime(long time) {
		return getEvents().get(time);
	}

	/**
	 * Gets the events before time.
	 *
	 * @param time the time
	 * @return the events before time
	 */
	@Override
	public IEvent[] getEventsBeforeTime(long time) {
		EventEntry e = getEvents().floorEntry(time);
		if (e == null)
			return new IEvent[] {};
		else
			return e.events;
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
	public synchronized int getRowCount() {
		if (rowCount<0)
			calculateConcurrency();
		return rowCount;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	/**
	 * Calculate concurrency.
	 */
	synchronized void calculateConcurrency() {
        if (rowCount >= 0) return;
        int count = TxRowAllocator.assign(getEvents(), (event, row) -> {
            ((TxEvent) event).setConcurrencyIndex(row);
            Tx tx = (Tx) event.getTransaction();
            if (this instanceof TxGenerator) tx.setConcurrencyIdxInGen(row);
            else tx.setConcurrencyIndxInStream(row);
        });
        for (com.minres.scviewer.database.IHierNode child : getChildNodes())
            ((TxGenerator) child).calculateConcurrency();
        rowCount = count;
    }
}
