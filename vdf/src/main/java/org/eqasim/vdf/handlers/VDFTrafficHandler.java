package org.eqasim.vdf.handlers;

import java.util.List;

import org.eqasim.vdf.io.VDFReaderInterface;
import org.eqasim.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;

public interface VDFTrafficHandler {
	void processEnterLink(double time, Id<Link> linkId);

	IdMap<Link, List<Double>> aggregate();

	VDFReaderInterface getReader();

	VDFWriterInterface getWriter();
}
