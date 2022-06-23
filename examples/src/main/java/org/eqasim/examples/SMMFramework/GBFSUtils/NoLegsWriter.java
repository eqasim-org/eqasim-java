package org.eqasim.examples.SMMFramework.GBFSUtils;

import org.matsim.analysis.TripsAndLegsCSVWriter;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;

public class NoLegsWriter implements TripsAndLegsCSVWriter.CustomLegsWriterExtension {

    public NoLegsWriter() {
    }

    @Override
    public String[] getAdditionalLegHeader() {
        return new String[]{"none"};
    }

    @Override
    public List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg) {
        List<String> list=new ArrayList<>();
        list.add("None");
        return list;
    }
}
