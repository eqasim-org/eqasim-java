package org.eqasim.examples.graphicalOutput;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;

public class PlansSHpFile {
    public static void main(final String [] args) {
        // FIXME hard-coded file names; does this class really need a main-method?
        final String populationFilename = "C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\simulation_output\\output_plans.xml.gz";
        final String networkFilename = "C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\ile_de_france\\src\\main\\resources\\corsica\\corsica_network.xml.gz";
        //		final String populationFilename = "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
        //		final String networkFilename = "./test/scenarios/berlin/network.xml.gz";

        final String outputDir = "./plans/";
        new File(outputDir).mkdir();

        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
        new PopulationReader(scenario).readFile(populationFilename);

        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:2154");
        PlansWriter sp = new PlansWriter(scenario.getPopulation(), scenario.getNetwork(), crs, outputDir);
        sp.setOutputSample(0.05);
        sp.setActBlurFactor(100);
        sp.setLegBlurFactor(100);
        sp.setWriteActs(true);
//        sp.setWriteLegs(true);

        sp.write();
    }
}
