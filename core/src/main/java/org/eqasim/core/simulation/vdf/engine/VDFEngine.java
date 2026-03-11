package org.eqasim.core.simulation.vdf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eqasim.core.simulation.vdf.FlowEquivalentProvider;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;

public class VDFEngine implements DepartureHandler, MobsimEngine {
	private final List<String> modes;
	private final List<String> dynamicModes;

	private VDFTransitEngine transitEngine;
	private VDFDynamicEngine dynamicEngine;
	private VDFStaticEngine staticEngine;

	public VDFEngine(Collection<String> modes, Collection<String> dynamicModes, TraversalTime traversalTime,
			FlowEquivalentProvider flowEquivalentProvider, Network network, VDFTrafficHandler handler,
			boolean generateNetworkEvents, double simulationStep) {
		this.modes = new ArrayList<>(modes);
		this.dynamicModes = new ArrayList<>(dynamicModes);

		this.transitEngine = new VDFTransitEngine(traversalTime, flowEquivalentProvider, network, handler,
				generateNetworkEvents,
				simulationStep);
		this.dynamicEngine = new VDFDynamicEngine(traversalTime, flowEquivalentProvider, network, handler,
				generateNetworkEvents,
				simulationStep);
		this.staticEngine = new VDFStaticEngine(traversalTime, flowEquivalentProvider, network, handler,
				generateNetworkEvents, simulationStep);
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (modes.contains(agent.getMode())) {
			if (agent instanceof TransitDriverAgent) {
				// special case for transit agents
				return transitEngine.handleDeparture(now, agent, linkId);
			} else if (agent instanceof DynAgent || dynamicModes.contains(agent.getMode())) {
				// dynamic agent or enforced dynamic handling (for instance of within-day
				// changes are expected)
				return dynamicEngine.handleDeparture(now, agent, linkId);
			} else {
				// standard behavior
				return staticEngine.handleDeparture(now, agent, linkId);
			}
		}

		return false;
	}

	@Override
	public void doSimStep(double now) {
		transitEngine.doSimStep(now);
		dynamicEngine.doSimStep(now);
		staticEngine.doSimStep(now);
	}

	@Override
	public void onPrepareSim() {
		transitEngine.onPrepareSim();
		dynamicEngine.onPrepareSim();
		staticEngine.onPrepareSim();
	}

	@Override
	public void afterSim() {
		transitEngine.afterSim();
		dynamicEngine.afterSim();
		staticEngine.afterSim();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		transitEngine.setInternalInterface(internalInterface);
		dynamicEngine.setInternalInterface(internalInterface);
		staticEngine.setInternalInterface(internalInterface);
	}
}
