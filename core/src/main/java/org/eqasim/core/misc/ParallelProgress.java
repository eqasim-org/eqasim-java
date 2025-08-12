package org.eqasim.core.misc;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParallelProgress {
	private final Logger logger = LogManager.getLogger(ParallelProgress.class);

	private final Thread thread;
	private final long totalCount;
	private final String description;

	private boolean showElapsedTime = true;
	private boolean showRemainingTime = true;
	private boolean showSpeed = true;

	private long currentCount = 0;
	private long lastCount = -1;
	private int interval = 10000;

	public ParallelProgress(String description, long totalCount) {
		thread = new Thread(this::run);

		this.totalCount = totalCount;
		this.description = description;
	}

	public void setShowElapsedTime(boolean val) {
		this.showElapsedTime = val;
	}

	public void setShowRemainingTime(boolean val) {
		this.showRemainingTime = val;
	}

	public void showSpeed(boolean val) {
		this.showSpeed = val;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	private void run() {
		long startTime = System.currentTimeMillis();
		long currentTime = startTime;

		try {
			while (currentCount < totalCount) {
				if (currentCount > lastCount) {
					List<String> message = new LinkedList<>();
					message.add(description);
					message.add(String.format("%d/%d", currentCount, totalCount));
					message.add(String.format("(%.2f%%)", 100.0 * currentCount / totalCount));

					currentTime = System.currentTimeMillis();
					long elapsedTime = currentTime - startTime;

					if (showElapsedTime) {
						message.add("+" + writeTime(elapsedTime));
					}

					double speed = (double) currentCount / elapsedTime;

					if (showSpeed) {
						if (speed >= 1.0) {
							message.add(String.format("%.2fit/s", speed));
						} else {
							message.add(String.format("%.2fs/it", speed));
						}
					}

					if (showRemainingTime) {
						long remainingTime = (long) ((double) (totalCount - currentCount) / speed);
						message.add("-" + writeTime(remainingTime));
					}
					logger.info(message);
				}

				lastCount = currentCount;
				Thread.sleep(interval);
			}
		} catch (InterruptedException e) {
		}
	}

	private String writeTime(long seconds) {
		long days = seconds / (3600 * 24);
		seconds -= days * 24 * 3600;
		long hours = seconds / 3600;
		seconds -= hours * 3600;
		long minutes = seconds / 60;
		seconds -= minutes * 60;

		if (days > 0) {
			return String.format("%d-%02d:%02d:%02d", days, hours, minutes, seconds);
		} else if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else if (minutes > 0) {
			return String.format("%02d:%02d", minutes, seconds);
		} else {
			return String.format("%02ds", seconds);
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
