package org.eqasim.core.scenario.spatial;

import org.eqasim.core.misc.ParallelProgress;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

public class ImputeSpatialAttribute {
	private final Geometry geometry;
	private final GeometryFactory factory = new GeometryFactory();
	private final StageActivityTypes stageActivityTypes;
	private final String attribute;

	public ImputeSpatialAttribute(Geometry geometry, String attribute, StageActivityTypes stageActivityTypes) {
		this.geometry = geometry;
		this.stageActivityTypes = stageActivityTypes;
		this.attribute = attribute;
	}

	public void run(Population population) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Imputing spatial population attributes ...",
				population.getPersons().size());

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Activity activity : TripStructureUtils.getActivities(plan, stageActivityTypes)) {
					Point point = factory
							.createPoint(new Coordinate(activity.getCoord().getX(), activity.getCoord().getY()));

					if (geometry.contains(point)) {
						activity.getAttributes().putAttribute(attribute, true);
					}
				}
			}

			progress.update();
		}

		progress.close();
	}

	public void run(Network network) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Imputing spatial network attributes ...",
				network.getLinks().size());

		for (Link link : network.getLinks().values()) {
			Point point = factory.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));

			if (geometry.covers(point)) {
				link.getAttributes().putAttribute(attribute, true);
			}

			progress.update();
		}

		progress.close();
	}
}
