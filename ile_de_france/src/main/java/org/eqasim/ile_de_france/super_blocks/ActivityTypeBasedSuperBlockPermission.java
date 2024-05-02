package org.eqasim.ile_de_france.super_blocks;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.Collection;

public class ActivityTypeBasedSuperBlockPermission implements SuperBlockPermission {

    private final Collection<String> activityTypes;

    public ActivityTypeBasedSuperBlockPermission(Collection<String> activityTypes) {
        this.activityTypes = activityTypes;
    }

    @Override
    public boolean isPersonAllowedInSuperBlock(Person person, SuperBlock superBlock) {
        for(PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
            if(planElement instanceof Activity activity) {
                if(this.activityTypes.contains(activity.getType()) && superBlock.containsCoord(activity.getCoord())) {
                    return true;
                }
            }
        }
        return false;
    }
}
