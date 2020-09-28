package org.eqasim.jakarta.roadpricing;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelDisutility;
import org.eqasim.jakarta.roadpricing.JakartaMcCalcAverageTolledTripLength;
import org.eqasim.jakarta.roadpricing.JakartaMcCalcPaidToll;
import org.eqasim.jakarta.roadpricing.JakartaMcRoadPricingScheme;
import org.eqasim.jakarta.roadpricing.JakartaMcRoadPricingWriterXMLv1;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.  Does the 
 * following:
 * <p></p>
 * Initialization:
 * <ul>
 * 		<li> Reads the road pricing scheme and adds it as a scenario element.
 * 		<li> Adds the {@link CalcPaidToll} events listener (to calculate the 
 * 			 toll per agent).
 * 		<li> Adds the toll to the {@link TravelDisutility} for the router (by 
 * 			 wrapping the pre-existing {@link TravelDisutility} object).
 * </ul>
 * After mobsim:
 * <ul>
 * 		<li> Send toll as money events to agents.
 * </ul>
 * Will also generate and output some statistics ...
 *
 * @author mrieser
 */
class JakartaMcRoadPricingControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	final static private Logger log = Logger.getLogger(JakartaMcRoadPricingControlerListener.class);

	private final JakartaMcRoadPricingScheme scheme;
	private final JakartaMcCalcPaidToll calcPaidToll;
	private final JakartaMcCalcAverageTolledTripLength cattl;
	private OutputDirectoryHierarchy controlerIO;

	@Inject
	JakartaMcRoadPricingControlerListener(JakartaMcRoadPricingScheme scheme, JakartaMcCalcPaidToll calcPaidToll, JakartaMcCalcAverageTolledTripLength cattl, OutputDirectoryHierarchy controlerIO) {
		this.scheme = scheme;
		this.calcPaidToll = calcPaidToll;
		this.cattl = cattl;
		this.controlerIO = controlerIO;
		Gbl.printBuildInfo("RoadPricing", "/org.matsim.contrib/roadpricing/revision.txt");
	}

	@Override
	public void notifyStartup(final StartupEvent event) {}

	

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info("The sum of all paid tolls : " + this.calcPaidToll.getAllAgentsToll() + " monetary units.");
		log.info("The number of people who paid toll : " + this.calcPaidToll.getDraweesNr());
		log.info("The average paid trip length : " + this.cattl.getAverageTripLength() + " m.");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String filename = this.controlerIO.getOutputFilename("output_toll.xml.gz") ;
		new JakartaMcRoadPricingWriterXMLv1(this.scheme).writeFile(filename);
	}

}