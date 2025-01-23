package org.eqasim;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class TestConfigurator extends EqasimConfigurator {
    public TestConfigurator() throws ConfigurationException {
        super(new CommandLine.Builder(new String[0]).build());
    }

    public TestConfigurator(CommandLine commandLine) {
        super(commandLine);

        registerModule(new AbstractEqasimExtension() {
            @Override
            public void installEqasimExtension() {
                bind(ModeParameters.class);
                bindModeAvailability("DefaultModeAvailability").to(TestModeAvailability.class);
            }
        });
    }

    public static class TestModeAvailability implements ModeAvailability {
        @Override
        public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
            Set<String> modes = new HashSet<>();
            modes.add(TransportMode.walk);
            modes.add(TransportMode.pt);
            modes.add(TransportMode.car);
            modes.add(TransportMode.bike);
            // Add special mode "car_passenger" if applicable
            Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isPassenger");
            if (isCarPassenger) {
                modes.add("car_passenger");
            }
            return modes;
        }
    }
}
