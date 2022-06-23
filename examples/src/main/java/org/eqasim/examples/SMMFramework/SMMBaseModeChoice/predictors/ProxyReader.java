package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import com.google.common.io.Resources;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.net.URL;
import java.util.Map;

public class ProxyReader {
    public static void main(String[] args) {

        URL configUrl = Resources.getResource("corsica/corsica_config.xml");
        Config config = ConfigUtils.loadConfig(configUrl);
        Scenario scenario = ScenarioUtils.createScenario(config);
        TransitScheduleReader stops= new TransitScheduleReader(scenario);
        stops.readFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester 4\\Matsim\\eqasim-java-develop\\ile_de_france\\src\\main\\resources\\corsica\\corsica_transit_schedule.xml.gz");
        Map<Id<TransitStopFacility>, TransitStopFacility> stopsFacilities = scenario.getTransitSchedule().getFacilities();
        for(Id<TransitStopFacility> is: stopsFacilities.keySet() ){
            TransitStopFacility tempTS=stopsFacilities.get(is);
            System.out.println(tempTS.getLinkId());
            System.out.println("x: "+tempTS.getCoord().getX()+"- Y: "+tempTS.getCoord().getX());
        }
        System.out.println("jeje");
    }
}
