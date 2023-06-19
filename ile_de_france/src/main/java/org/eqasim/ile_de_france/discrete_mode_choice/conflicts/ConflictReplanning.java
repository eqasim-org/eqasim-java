package org.eqasim.ile_de_france.discrete_mode_choice.conflicts;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictConstraint.ConflictConstraintFactory;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceStrategyProvider;
import org.matsim.contribs.discrete_mode_choice.replanning.NonSelectedPlanSelector;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;

import com.google.inject.Inject;

@Singleton
public class ConflictReplanning implements ReplanningListener, PlansReplanning {
	private final Logger logger = Logger.getLogger(ConflictReplanning.class);

	private final Provider<ReplanningContext> replanningContextProvider;
	private Population population;
	private StrategyManager strategyManager;

	private final ConflictConstraintFactory rejectionConstraintFactory;
	private final ConflictLogic rejectionLogic;

	private final DiscreteModeChoiceStrategyProvider strategyProvider;

	@Inject
	ConflictReplanning(StrategyManager strategyManager, Population pop,
			Provider<ReplanningContext> replanningContextProvider, ConflictLogic rejectionLogic,
			ConflictConstraintFactory rejectionConstraintFactory, DiscreteModeChoiceStrategyProvider strategyProvider) {
		this.population = pop;
		this.strategyManager = strategyManager;
		this.replanningContextProvider = replanningContextProvider;

		this.rejectionConstraintFactory = rejectionConstraintFactory;
		this.rejectionLogic = rejectionLogic;
		this.strategyProvider = strategyProvider;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		strategyManager.run(population, event.getIteration(), replanningContextProvider.get());

		IdMap<Person, Set<ConflictItem>> globalItems = new IdMap<>(Person.class);
		IdMap<Person, Set<ConflictItem>> currentItems = new IdMap<>(Person.class);

		boolean finished = false;
		int round = 0;

		while (!finished) {
			currentItems.clear();

			for (Person person : population.getPersons().values()) {
				person.removePlan(new NonSelectedPlanSelector().selectPlan(person));
			}

			ConflictHandler handler = new ConflictHandler(currentItems);
			rejectionLogic.run(population, handler);

			// Merge into global items
			for (var entry : currentItems.entrySet()) {
				globalItems.computeIfAbsent(entry.getKey(), id -> new HashSet<>()).addAll(entry.getValue());
			}

			if (currentItems.size() > 0) {
				logger.warn("Conflict resolution round " + ++round + " with " + currentItems.size() + " items");

				rejectionConstraintFactory.setItems(globalItems);
				PlanStrategy dmcStrategy = strategyProvider.get();

				// Perform mode choice again
				dmcStrategy.init(replanningContextProvider.get());

				for (Id<Person> personId : currentItems.keySet()) {
					dmcStrategy.run(population.getPersons().get(personId));
				}

				dmcStrategy.finish();
			} else {
				finished = true;
			}
		}
	}
}
