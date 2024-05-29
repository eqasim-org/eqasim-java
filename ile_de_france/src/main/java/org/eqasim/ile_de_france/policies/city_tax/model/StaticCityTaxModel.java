package org.eqasim.ile_de_france.policies.city_tax.model;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;

public class StaticCityTaxModel implements CityTaxModel {
	private final IdSet<Link> affectedLinkIds;

	private final double travelDisutility;
	private final double cost_EUR;

	public StaticCityTaxModel(IdSet<Link> affectedLinkIds, double travelDisutility, double cost_EUR) {
		this.affectedLinkIds = affectedLinkIds;
		this.travelDisutility = travelDisutility;
		this.cost_EUR = cost_EUR;
	}

	@Override
	public double getTravelDisutility(Link link, double time) {
		return travelDisutility;
	}

	@Override
	public double getMonetaryCost(NetworkRoute route) {
		double total_EUR = 0.0;

		for (Id<Link> linkId : route.getLinkIds()) {
			if (affectedLinkIds.contains(linkId)) {
				total_EUR += cost_EUR;
			}
		}

		return total_EUR;
	}

	static public StaticCityTaxModel create(Network network, double travelDisutility, double cost_EUR, URL url)
			throws IOException {
		List<Geometry> shapes = new LinkedList<>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));

			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featureIterator = featureCollection.features();

			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				shapes.add((Geometry) feature.getDefaultGeometry());
			}

			featureIterator.close();
			dataStore.dispose();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		IdSet<Link> affectedLinkIds = new IdSet<>(Link.class);

		for (Link link : network.getLinks().values()) {
			for (Geometry geometry : shapes) {
				Point start = MGC.coord2Point(link.getFromNode().getCoord());
				Point end = MGC.coord2Point(link.getToNode().getCoord());

				if (!geometry.covers(start) && geometry.coveredBy(end)) {
					affectedLinkIds.add(link.getId());
				}
			}
		}

		return new StaticCityTaxModel(affectedLinkIds, travelDisutility, cost_EUR);
	}
}
