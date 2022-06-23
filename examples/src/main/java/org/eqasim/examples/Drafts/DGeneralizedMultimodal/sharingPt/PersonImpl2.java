package org.eqasim.examples.Drafts.DGeneralizedMultimodal.sharingPt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.core.scenario.Lockable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class PersonImpl2 implements Person, Lockable {



        private List<Plan> plans = new ArrayList(6);
        private Id<Person> id;
        private Plan selectedPlan = null;
        private Customizable customizableDelegate;
        private boolean locked;
        private final Attributes attributes = new Attributes();

        public PersonImpl2(Id<Person> id) {
            this.id = id;
        }

        public final Plan getSelectedPlan() {
            return this.selectedPlan;
        }

        public boolean addPlan(Plan plan) {
            plan.setPerson(this);
            if (this.selectedPlan == null) {
                this.selectedPlan = plan;
            }

            return this.plans.add(plan);
        }

        public final void setSelectedPlan(Plan selectedPlan) {
            if (selectedPlan != null && !this.plans.contains(selectedPlan)) {
                throw new IllegalStateException("The plan to be set as selected is not null nor stored in the person's plans");
            } else {
                this.selectedPlan = selectedPlan;
            }
        }

        public Plan createCopyOfSelectedPlanAndMakeSelected() {
            Plan oldPlan = this.getSelectedPlan();
            if (oldPlan == null) {
                return null;
            } else {
                Plan newPlan = PopulationUtils.createPlan(oldPlan.getPerson());
                PopulationUtils.copyFromTo(oldPlan, newPlan);
                this.getPlans().add(newPlan);
                this.setSelectedPlan(newPlan);
                return newPlan;
            }
        }

        public Id<Person> getId() {
            return this.id;
        }

        void changeId(Id<Person> newId) {
            try {
                this.testForLocked();
            } catch (Exception var3) {
                Logger.getLogger(this.getClass()).warn("cannot change oerson id while in population.  remove the person, change Id, re-add.");
                throw var3;
            }

            this.id = newId;
        }

        public final String toString() {
            StringBuilder b = new StringBuilder();
            b.append("[id=").append(this.getId()).append("]");
            b.append("[nof_plans=").append(this.getPlans() == null ? "null" : this.getPlans().size()).append("]");
            return b.toString();
        }

        public boolean removePlan(Plan plan) {
            boolean result = this.getPlans().remove(plan);
            if (this.getSelectedPlan() == plan && result) {
                this.setSelectedPlan((Plan)(new RandomPlanSelector()).selectPlan(this));
            }

            return result;
        }

        public List<Plan> getPlans() {
            return this.plans;
        }

        public Map<String, Object> getCustomAttributes() {
            if (this.customizableDelegate == null) {
                this.customizableDelegate = CustomizableUtils.createCustomizable();
            }

            return this.customizableDelegate.getCustomAttributes();
        }

        public Attributes getAttributes() {
            return this.attributes;
        }

        public final void setLocked() {
            this.locked = true;
        }

        private void testForLocked() {
            if (this.locked) {
                throw new RuntimeException("too late to do this");
            }
        }
}


