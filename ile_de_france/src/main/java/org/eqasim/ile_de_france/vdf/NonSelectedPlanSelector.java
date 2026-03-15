package org.eqasim.ile_de_france.vdf;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.replanning.selectors.PlanSelector;

public class NonSelectedPlanSelector implements PlanSelector<Plan, Person> {
    static public final String NAME = "NonSelectedPlanSelector";

    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> member) {
        for (Plan plan : member.getPlans()) {
            if (!plan.equals(member.getSelectedPlan())) {
                return plan;
            }
        }

        return null;
    }

    static public void install(Controller controller) {
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindPlanSelectorForRemoval().toInstance(new NonSelectedPlanSelector());
                addControllerListenerBinding().to(TravelTimeTracker.class);
            }
        });
    }
}
