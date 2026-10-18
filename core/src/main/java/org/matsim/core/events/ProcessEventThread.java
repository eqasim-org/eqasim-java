/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.events;

import java.lang.InterruptedException;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

/**
 * The wrapper around the Events class for allowing parallelization.
 *
 * @author rashid_waraich
 */
/*package*/ class ProcessEventThread implements Runnable {
	// Version-pinned adaptation of MATSim 2026.0-2025w33. Producer order,
	// handler execution and end-of-iteration markers retain the native protocol.
	static final String BATCH_PROPERTY = "eqasim.events.batchQueues";
	private ArrayList<Event> preInputBuffer;
	private final BlockingQueue<Event> eventQueue;
	private final BlockingQueue<ArrayList<Event>> batches;
	private final AtomicReference<ArrayList<Event>> spare = new AtomicReference<>();
	private ArrayList<Event> currentBatch;
	private int batchIndex;
	private final EventsManager events;
	private final int preInputBufferMaxLength;

	public ProcessEventThread(
			final EventsManager events,
			final int preInputBufferMaxLength) {
		this.events = events;
		this.preInputBufferMaxLength = preInputBufferMaxLength;
		String setting = System.getProperty(BATCH_PROPERTY, "true");
		if (!setting.equalsIgnoreCase("true") && !setting.equalsIgnoreCase("false")) {
			throw new IllegalArgumentException(BATCH_PROPERTY + " must be true or false");
		}
		boolean batching = Boolean.parseBoolean(setting);
		eventQueue = batching ? null : new LinkedBlockingQueue<>();
		batches = batching ? new LinkedBlockingQueue<>() : null;
		preInputBuffer = new ArrayList<Event>( preInputBufferMaxLength + 1);
	}

	public synchronized void processEvent(final Event event) {
		// first approach (quick on office computer, but not on satawal)
		// eventQueue.add(event);

		// second approach, lesser locking => faster on Satawal
		preInputBuffer.add(event);
		if (preInputBuffer.size() > preInputBufferMaxLength) {
			emptyPreBuffer();
		}
	}

	private synchronized void emptyPreBuffer() {
		if (preInputBuffer.isEmpty()) return;
		if (batches == null) {
			eventQueue.addAll(preInputBuffer);
			preInputBuffer.clear();
		} else {
			ArrayList<Event> completed = preInputBuffer;
			ArrayList<Event> reusable = spare.getAndSet(null);
			preInputBuffer = reusable != null ? reusable : new ArrayList<>(preInputBufferMaxLength + 1);
			// The producer relinquishes this array until the consumer clears
			// and returns it. One queue node/lock replaces one per event.
			batches.add(completed);
		}
	}

	private Event nextEvent() throws InterruptedException {
		if (batches == null) return eventQueue.take();
		// LinkedBlockingQueue.take checks this on every event, including
		// when the queue is nonempty. Preserve cancellation within a batch.
		if (Thread.interrupted()) throw new InterruptedException();
		if (currentBatch == null || batchIndex == currentBatch.size()) {
			releaseBatch();
			currentBatch = batches.take();
			batchIndex = 0;
		}
		// Match the old queue's immediate release of consumed event references.
		return currentBatch.set(batchIndex++, null);
	}

	private void releaseBatch() {
		ArrayList<Event> completed = currentBatch;
		currentBatch = null;
		if (completed != null) {
			completed.clear();
			// Bound retained spare capacity to one array per event worker.
			spare.compareAndSet(null, completed);
		}
	}

	private boolean hasQueuedEvents() {
		return batches == null ? !eventQueue.isEmpty()
				: batchIndex < currentBatch.size() || !batches.isEmpty();
	}

	@Override
	public void run() {
		try {
			// process events, until LastEventOfIteration arrives
			while (true) {
				// take waits for an element to exist before returning:
				//  - thread sleeps until there is an event to process
				//  - we do not have to bother checking if the element exists
				Event nextEvent = nextEvent();
				if (nextEvent instanceof LastEventOfIteration) {
					Gbl.printCurrentThreadCpuTime();

					// if there are more events generated after end of simulation
					// (generated in events handler), process them before stopping events handling.
					// in order to do this, LastEventOfIteration is moved to the back of the queue.
					if (hasQueuedEvents()){
						processEvent(nextEvent);
						emptyPreBuffer();
						nextEvent = nextEvent();
					} else {
						return;
					}
				}
				getEvents().processEvent(nextEvent);
			}
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( e );
		}
		finally {
			if (currentBatch != null) currentBatch.clear();
			currentBatch = null;
			spare.set(null);
		}
	}

	// schedule LastEventOfIteration and flush buffered events
	// the LastEventOfIteration lets the event handler threads know,
	// that there is no more work, as soon as they have processed this,
	// they are allowed to go to sleep
	public synchronized void close() {
		processEvent(new LastEventOfIteration(0.0));
		emptyPreBuffer();
	}

	public EventsManager getEvents() {
		return events;
	}

}
