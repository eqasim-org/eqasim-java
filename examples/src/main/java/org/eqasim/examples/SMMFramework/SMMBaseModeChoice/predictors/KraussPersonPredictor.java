package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussEqasimPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussPersonPredictor extends CachedVariablePredictor<KraussEqasimPersonVariables> {
	@Override
	public KraussEqasimPersonVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		int age_a = PredictorUtils.getAge(person);
		int bikeAcc=0;
		int carAcc=0;
		int ptPass=0;
		String attBAcc= (String) person.getAttributes().getAttribute("bikeAvailability");
		if(attBAcc=="all" || attBAcc=="some"){
			bikeAcc=1;
		}
		String attCAcc= (String) person.getAttributes().getAttribute("carAvailability");
		if(attBAcc=="all" || attBAcc=="some"){
			carAcc=1;
		}
		Boolean attPtPass=(Boolean) person.getAttributes().getAttribute("hasPtSubscription");
		if(attPtPass){
			ptPass=1;
		}

		return(new KraussEqasimPersonVariables(age_a,bikeAcc,carAcc,ptPass,0));
	}
}
