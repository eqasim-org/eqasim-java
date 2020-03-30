package org.eqasim.wayne_county.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class WayneCountyPredictorUtils {

	static public int hhlIncomeClass(Person person) {
		String income = ((String)person.getAttributes().getAttribute("income"));
		if (income.length()<4)
			return -1;
		int hhlIncomeClass = (int) ((String)person.getAttributes().getAttribute("income")).charAt(3);
		return hhlIncomeClass;
	}
	
/*	static public boolean isFreight(Person person) {
		
		for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals("truck")) {
					return true;

				} 
			}
		}
		return false;
	
	}*/
}
