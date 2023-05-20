package org.eqasim.ile_de_france.analysis.counts.calibration;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CalibrationUpdate {
	public int iteration;
	public double correctionFactor;

	public double rmse;
	public double mae;

	public List<LinkItem> links = new LinkedList<>();

	static public class LinkItem {
		public Id<Link> linkId;

		public double referenceCount;
		public double simulationCount;
		public double scaledCount;
		public double correctedCount;

		public double initialCapacity;
		public double currentCapacity;
		public double updatedCapacity;

		public int updatedLinks;
	}

}
