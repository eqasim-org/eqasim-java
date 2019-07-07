package org.eqasim.core.misc;

import org.apache.log4j.Logger;

public class ParallelProgress {
	private final Logger logger = Logger.getLogger(ParallelProgress.class);

	private final Thread thread;
	private final long totalCount;
	private final String description;

	private long currentCount = 0;
	private long lastCount = -1;

	public ParallelProgress(String description, long totalCount) {
		thread = new Thread(this::run);

		this.totalCount = totalCount;
		this.description = description;
	}

	private void run() {
		try {
			while (currentCount < totalCount) {
				if (currentCount > lastCount) {
					logger.info(String.format("%s %d/%d (%.2f%%)", description, currentCount, totalCount,
							100.0 * currentCount / totalCount));
				}

				lastCount = currentCount;
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
		}
	}

	public void start() {
		thread.start();
	}

	public void interrupt() {
		thread.interrupt();
	}

	public synchronized void update(int count) {
		currentCount += count;
	}

	public void update() {
		update(1);
	}

	public synchronized void set(int count) {
		currentCount = count;
	}

	public void close() throws InterruptedException {
		thread.interrupt();
		thread.join();

		if (currentCount == totalCount) {
			logger.info(String.format("%s Done!", description));
		}
	}
}
