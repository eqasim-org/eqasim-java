package org.eqasim.switzerland.ovgk;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class OVGKCalculator {
	private final QuadTree<TransitStopFacility> index;

	public OVGKCalculator(TransitSchedule schedule) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (TransitStopFacility facility : schedule.getFacilities().values()) {
			Coord coord = facility.getCoord();

			minX = Math.min(minX, coord.getX());
			maxX = Math.max(maxX, coord.getX());
			minY = Math.min(minY, coord.getY());
			maxY = Math.max(maxY, coord.getY());
		}

		this.index = new QuadTree<>(minX, minY, maxX, maxY);

		for (TransitStopFacility facility : schedule.getFacilities().values()) {
			Coord coord = facility.getCoord();
			index.put(coord.getX(), coord.getY(), facility);
		}
	}

	private int getStopCategory(TransitStopFacility facility) {
		Integer attribute = (Integer) facility.getAttributes().getAttribute(OVGKConstants.STOP_CATEGORY_ATTRIBUTE);

		if (attribute == null) {
			attribute = OVGKConstants.WORST_STOP_CATEGORY;
		}

		return attribute;
	}

	private int rank(OVGK ovgk) {
		switch (ovgk) {
		case A:
			return 1;
		case B:
			return 2;
		case C:
			return 3;
		case D:
			return 4;
		case None:
			return 5;
		}

		throw new IllegalStateException();
	}

	private OVGK max(OVGK a, OVGK b) {
		int rankA = rank(a);
		int rankB = rank(b);

		if (rankA < rankB) {
			return a;
		} else {
			return b;
		}
	}

	public OVGK calculateOVGK(Coord coord) {
		List<TransitStopFacility> disk = new LinkedList<>(index.getDisk(coord.getX(), coord.getY(), 1000.0));

		// There are a couple of ways to potentially speed this up:
		// - 1. Sort stops by distance first
		// - 2. Ignore lower stages because within one stop category, OVGK always gets
		// worse with increasing distance

		OVGK bestOVGK = OVGK.None;

		for (TransitStopFacility facility : disk) {
			double distance_m = CoordUtils.calcEuclideanDistance(coord, facility.getCoord());
			int stopCategory = getStopCategory(facility);
			OVGK ovgk = OVGK.None;

			if (distance_m < 300.0) {
				if (stopCategory == 1 || stopCategory == 2) {
					ovgk = OVGK.A;
				} else if (stopCategory == 3) {
					ovgk = OVGK.B;
				} else if (stopCategory == 4) {
					ovgk = OVGK.C;
				} else if (stopCategory == 5) {
					ovgk = OVGK.D;
				}
			} else if (distance_m < 500.0) {
				if (stopCategory == 1) {
					ovgk = OVGK.A;
				} else if (stopCategory == 2) {
					ovgk = OVGK.B;
				} else if (stopCategory == 3) {
					ovgk = OVGK.C;
				} else if (stopCategory == 4) {
					ovgk = OVGK.D;
				}
			} else if (distance_m < 750.0) {
				if (stopCategory == 1) {
					ovgk = OVGK.B;
				} else if (stopCategory == 2) {
					ovgk = OVGK.C;
				} else if (stopCategory == 3) {
					ovgk = OVGK.D;
				}
			} else if (distance_m < 1000.0) {
				if (stopCategory == 1) {
					ovgk = OVGK.C;
				} else if (stopCategory == 2) {
					ovgk = OVGK.D;
				}
			}

			bestOVGK = max(bestOVGK, ovgk);

			if (bestOVGK == OVGKConstants.BEST_OVGK) {
				return bestOVGK;
			}
		}

		return bestOVGK;
	}
}
