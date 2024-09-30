package org.eqasim.core.analysis.activities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class ActivityItem {
	public Id<Person> personId;
	public int activityIndex;
	public String purpose;
	public double startTime;
	public double endTime;
	public double x;
	public double y;

	public ActivityItem(Id<Person> personId, int activityIndex, String purpose, double startTime, double endTime,
			double x, double y) {
		this.personId = personId;
		this.activityIndex = activityIndex;
		this.purpose = purpose;
		this.startTime = startTime;
		this.endTime = endTime;
		this.x = x;
		this.y = y;
	}
}