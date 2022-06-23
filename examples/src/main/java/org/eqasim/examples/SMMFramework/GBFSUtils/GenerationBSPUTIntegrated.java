package org.eqasim.examples.SMMFramework.GBFSUtils;

import com.google.common.io.Resources;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GenerationBSPUTIntegrated {
    public static void main(String[] args) {
        Map<String, Geometry> shapes = RandomStationServiceCorsica.readShapeFile("C:\\Users\\juan_\\Desktop\\Corsica-shp\\shape\\Corsica1x1km2grid.shp", "UNIQUE_ID");
        Map<String, Geometry> polygons = RandomStationServiceCorsica.readShapeFile("C:\\Users\\juan_\\Desktop\\Corsica-shp\\shape\\Corsica1x1km2grid.shp", "UNIQUE_ID");
        URL configUrl = Resources.getResource("corsica/corsica_config.xml");
        Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        HashMap<String, HashMap<String, Station>> lastItStations = RandomStationServiceCorsica.createStationMapBsedOnPUT(scenario, polygons);
        Map<String, Integer> map = RandomStationServiceCorsica.calculateNumberofVehicles(polygons, 100000, "UNIQUE_ID", population);
            Map<String, Integer> vehicles = RandomStationServiceCorsica.calculateNumberofStations("C:\\Users\\juan_\\Desktop\\Corsica-shp\\shape\\Corsica1x1km2grid.shp", 20000, "UNIQUE_ID", map);
            lastItStations = RandomStationServiceCorsica.createGBFSStationBased2(vehicles, map, polygons, scenario, String.valueOf(20000), String.valueOf(100000), lastItStations);

        String x="uwu";
    }
}
