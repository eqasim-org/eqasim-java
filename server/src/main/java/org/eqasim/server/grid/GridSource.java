package org.eqasim.server.grid;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.BasicLocation;

public class GridSource {
	private final List<Pair<Double, Double>> coordinates;
	private final Bounds bounds;

	GridSource(List<Pair<Double, Double>> coordinates) {
		this.coordinates = coordinates;

		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (var coordinate : coordinates) {
			minX = Math.min(minX, coordinate.getLeft());
			maxX = Math.max(maxX, coordinate.getLeft());
			minY = Math.min(minY, coordinate.getRight());
			maxY = Math.max(maxY, coordinate.getRight());
		}

		this.bounds = new Bounds(minX, maxX, minY, maxY);
	}

	public List<Pair<Double, Double>> getCoordinates() {
		return coordinates;
	}

	public Bounds getBounds() {
		return bounds;
	}

	static public class Builder {
		private final List<Pair<Double, Double>> coordinates = new LinkedList<>();

		public Builder add(double x, double y) {
			coordinates.add(Pair.of(x, y));
			return this;
		}

		public Builder add(Pair<Double, Double> point) {
			coordinates.add(point);
			return this;
		}

		public Builder add(BasicLocation location) {
			coordinates.add(Pair.of(location.getCoord().getX(), location.getCoord().getY()));
			return this;
		}

		public Builder add(Collection<? extends BasicLocation> locations) {
			locations.forEach(this::add);
			return this;
		}

		public GridSource build() {
			return new GridSource(coordinates);
		}
	}

	static public class Bounds {
		public final double minX;
		public final double maxX;
		public final double minY;
		public final double maxY;

		Bounds(double minX, double maxX, double minY, double maxY) {
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
		}
	}
}
