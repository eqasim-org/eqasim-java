package org.eqasim.core.simulation.mode_choice.mode_availability_wrapper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

import java.util.Map;

public class ModeAvailabilityWrappersModule extends AbstractEqasimExtension {
    @Override
    protected void installEqasimExtension() {
        DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup = (DiscreteModeChoiceConfigGroup) getConfig().getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
        String modeAvailabilityString = discreteModeChoiceConfigGroup.getModeAvailability();
        if(!modeAvailabilityString.contains(":")) {
            return;
        }
        bindModeAvailability(modeAvailabilityString).toProvider(new Provider<>() {
            @Inject
            Map<String, Provider<ModeAvailability>> modeAvailabilityProviders;

            @Inject
            Map<String, Provider<ModeAvailabilityWrapperFactory>> modeAvailabilityWrapperFactories;

            @Override
            public ModeAvailability get() {
                String[] components = modeAvailabilityString.split(":");
                if(!modeAvailabilityProviders.containsKey(components[components.length-1])) {
                    throw new IllegalStateException("ModeAvailability not bound: " + components[components.length-1]);
                }
                ModeAvailability modeAvailability = modeAvailabilityProviders.get(components[components.length-1]).get();
                for(int i=components.length-2; i>=0; i--) {
                    if(!modeAvailabilityWrapperFactories.containsKey(components[i])) {
                        throw new IllegalStateException("ModeAvailabilityWrapperFactory not bound: " + components[i]);
                    }
                    ModeAvailabilityWrapperFactory modeAvailabilityWrapperFactory = modeAvailabilityWrapperFactories.get(components[i]).get();
                    modeAvailability = modeAvailabilityWrapperFactory.wrap(modeAvailability);
                }
                return modeAvailability;
            }
        });
    }
}
