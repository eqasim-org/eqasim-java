package org.eqasim.core.components.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.mobsim.qsim.qnetsimengine.FastQNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import com.google.inject.Singleton;

public class EqasimTrafficQSimModule extends AbstractQSimModule {
	private static final Logger logger = LogManager.getLogger(EqasimTrafficQSimModule.class);

	@Override
	protected void configureQSim() {
		// here we check whether the bike is routed in the network or not, if it is, then we use the mixed vehicle speed calculator, otherwise we use the default one
		boolean vdfActivated = getConfig().getModules().get(VDFConfigGroup.GROUP_NAME) != null;
		if (vdfActivated) {
			logger.warn("VDFConfigGroup detected in config. EqasimLinkSpeedCalculator will not be bind at this point.");
			return;
		}

		addLinkSpeedCalculatorBinding().to(EqasimLinkSpeedCalculator.class);
		String engine = System.getProperty(FastQNetsimEngine.ENGINE_PROPERTY, "fast");
		if (engine.equals("fast")) {
			bind(QNetsimEngineI.class).to(FastQNetsimEngine.class).in(Singleton.class);
		} else if (!engine.equals("native")) {
			throw new IllegalArgumentException("eqasim.qsim.engine must be fast or native");
		}
		if (getConfig().qsim().getLinkDynamics() == LinkDynamics.SeepageQ) {
			logger.info("Using copy-avoiding seepage queues; native equal-time selection is preserved.");
		}

	}
}
