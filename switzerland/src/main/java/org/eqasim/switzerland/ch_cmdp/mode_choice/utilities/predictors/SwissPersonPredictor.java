package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import com.google.inject.Inject;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SwissPersonPredictor extends CachedVariablePredictor<SwissPersonVariables> {
	public final PersonPredictor delegate;

	@Inject
	public SwissPersonPredictor(PersonPredictor personPredictor) {
		this.delegate = personPredictor;
	}

	@Override
	protected SwissPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
                                           List<? extends PlanElement> elements) {
		Coord homeLocation = SwissPredictorUtils.getHomeLocation(person);
		boolean hasGeneralSubscription = SwissPredictorUtils.hasGeneralSubscription(person);
		boolean hasHalbtaxSubscription = SwissPredictorUtils.hasHalbtaxSubscription(person);
		boolean hasRegionalSubscription = SwissPredictorUtils.hasRegionalSubscription(person);
		boolean hasJuniorSubscription = SwissPredictorUtils.hasJuniorSubscription(person);
		boolean hasGleis7Subscription = SwissPredictorUtils.hasGleis7Subscription(person);
		int statedPreferenceRegion = SwissPredictorUtils.getStatedPreferenceRegion(person);

		Integer sex = SwissPredictorUtils.getSex(person);
		Double income = SwissPredictorUtils.getIncomePerCapita(person);
		Integer drivingLicense = SwissPredictorUtils.hasDrivingLicense(person);
		int cantonCluster = SwissPredictorUtils.getCluster(person);
		Double carOwnershipRatio = SwissPredictorUtils.getCarOwnershipRatio(person);
		String ovgk = SwissPredictorUtils.getOvgk(person);

		return new SwissPersonVariables(delegate.predictVariables(person, trip, elements), homeLocation,
				hasGeneralSubscription, hasHalbtaxSubscription, hasRegionalSubscription, hasJuniorSubscription, hasGleis7Subscription,
				statedPreferenceRegion, sex, income, drivingLicense, cantonCluster, carOwnershipRatio, ovgk);
	}
}
