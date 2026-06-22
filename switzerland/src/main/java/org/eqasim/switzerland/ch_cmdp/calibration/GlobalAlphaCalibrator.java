package org.eqasim.switzerland.ch_cmdp.calibration;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibrationUtils;
import org.eqasim.core.components.fast_calibration.AlphaCalibrator;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.util.List;
import java.util.Map;


public class GlobalAlphaCalibrator extends AlphaCalibrator {
    public GlobalAlphaCalibrator(Scenario scenario, OutputDirectoryHierarchy outputHierarchy, ModeParameters modeParameters, TripListConverter tripListConverter, Map<String, Double> targetModeShares, List<String> modesToCalibrate, double beta, boolean isActivated, EqasimConfigGroup eqasimConfigGroup) {
        super(scenario, outputHierarchy, modeParameters, tripListConverter, targetModeShares, modesToCalibrate, beta, isActivated, eqasimConfigGroup);
    }

    @Override
    protected boolean NotConsideredPerson(Person person) {
        return (super.NotConsideredPerson(person) ||
                !AlphaCalibrationUtils.isConsideredPerson(person, "isExternalFR"));
    }
}
