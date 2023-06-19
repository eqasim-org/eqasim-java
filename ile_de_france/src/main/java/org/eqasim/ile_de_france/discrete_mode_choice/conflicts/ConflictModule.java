package org.eqasim.ile_de_france.discrete_mode_choice.conflicts;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictConstraint.ConflictConstraintFactory;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.NoopConflictLogic;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceStrategyProvider;
import org.matsim.core.controler.corelisteners.PlansReplanning;

public class ConflictModule extends AbstractDiscreteModeChoiceExtension {
	@Override
	protected void installExtension() {
		bind(DiscreteModeChoiceStrategyProvider.class);

		bind(PlansReplanning.class).to(ConflictReplanning.class);
		bind(ConflictLogic.class).toInstance(new NoopConflictLogic());
		bind(ConflictConstraintFactory.class);
		bindTripConstraintFactory(ConflictConstraint.NAME).to(ConflictConstraintFactory.class);
	}

	static public void configure(DiscreteModeChoiceConfigGroup dmcConfig) {
		List<String> tripContraints = new ArrayList<>(dmcConfig.getTripConstraints());
		tripContraints.add(ConflictConstraint.NAME);
		dmcConfig.setTripConstraints(tripContraints);
	}
}
