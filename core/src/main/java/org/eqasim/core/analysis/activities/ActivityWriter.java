package org.eqasim.core.analysis.activities;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class ActivityWriter {
	final private Collection<ActivityItem> activities;
	final private String delimiter;

	public ActivityWriter(Collection<ActivityItem> activities) {
		this(activities, ";");
	}

	public ActivityWriter(Collection<ActivityItem> activities, String delimiter) {
		this.activities = activities;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (ActivityItem activity : activities) {
			writer.write(formatActivity(activity) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(delimiter, new String[] { //
				"person_id", //
				"activity_index", //
				"start_time", //
				"end_time", //
				"x", //
				"y", //
		});
	}

	private String formatActivity(ActivityItem activity) {
		return String.join(delimiter, new String[] { //
				activity.personId.toString(), //
				String.valueOf(activity.activityIndex), //
				String.valueOf(activity.startTime), //
				String.valueOf(activity.endTime), //
				String.valueOf(activity.x), //
				String.valueOf(activity.y), //
		});
	}
}
