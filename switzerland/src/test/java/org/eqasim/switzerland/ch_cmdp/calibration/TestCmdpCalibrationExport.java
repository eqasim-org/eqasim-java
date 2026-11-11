package org.eqasim.switzerland.ch_cmdp.calibration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.*;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.*;
import org.eqasim.core.simulation.mode_choice.utilities.variables.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissParkingCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.*;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.PopulationUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import com.opencsv.CSVParserBuilder;

public class TestCmdpCalibrationExport {
    @Test public void exportsReproducibleInputsForEveryMode() throws Exception {
        Path root = System.getProperty("cmdp.fixture.dir") == null ? Files.createTempDirectory("cmdp-export")
                : Path.of(System.getProperty("cmdp.fixture.dir"));
        Files.createDirectories(root);
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            for (int regime = 0; regime < 2; regime++) {
                Path dir = root.resolve("regime" + regime);
                Files.createDirectories(dir);
                var p = SwissCmdpModeParameters.buildDefault();
                if (regime == 1) {
                    // Activate coefficients that happen to be zero in the fitted production parameters.
                    for (Object mode : List.of(p.car, p.pt, p.bike, p.walk, p.cp)) {
                        int index = 0;
                        for (var field : mode.getClass().getFields()) {
                            if (field.getName().startsWith("beta")) field.setDouble(mode, 0.031 * ++index);
                        }
                    }
                    p.timeScale_min = 7.0; p.distanceScale_km = 3.0; p.ageScale_year = 10.0;
                    p.betaDestinationEmployeeDensity_u = 0.7;
                    p.betaDestinationCompaniesDensity_u = -0.2;
                    p.betaDestinationPopulationDensity_u = 0.3;
                    p.pt.homePtQualityExponent = 0.0;
                    p.pt.destinationPtQualityExponent = -0.5;
                }
                p.saveToYamlFile(dir.resolve("parameters.yml").toString());
                var writer = new CmdpVariablesWriter();
                writer.init(dir.resolve("choice_variables.csv").toString());
                var f = PopulationUtils.getFactory();
                Map<String, List<Double>> expected = new LinkedHashMap<>();
                for (String mode : List.of("car", "pt", "bike", "walk", "car_passenger")) expected.put(mode, new ArrayList<>());
                for (int i = 0; i < 35; i++) {
                    double distance = new double[] {0.0, 0.5, 1.0, 13.0, 30.0, 40.0, 6.5}[i % 7];
                    Activity origin = f.createActivityFromCoord(i % 2 == 0 ? "home_secondary" : "work", new Coord(0, 0));
                    Activity destination = f.createActivityFromCoord(new String[] {"home", "work_secondary", "education_secondary", "shop", "leisure", "other", "unknown"}[i % 7], new Coord(distance * 1000, 0));
                    destination.getAttributes().putAttribute("municipalityType", new String[] {"urban", "urbancore", "suburban", "rural"}[i % 4]);
                    destination.getAttributes().putAttribute("municipalityId", new String[] {"261", "6621", "2701", "1061", "5586", "351", "0"}[i % 7]);
                    destination.getAttributes().putAttribute("ovgk", new String[] {"A", "B", "C", "D", "none"}[i % 5]);
                    if (i % 6 != 0) destination.getAttributes().putAttribute("employeeDensity", 130.0 + i);
                    destination.getAttributes().putAttribute("companiesDensity", 20 + i);
                    destination.getAttributes().putAttribute("populationDensity", 900L + i);
                    Person person = f.createPerson(Id.createPersonId("00;agent\"" + i));
                    if (i % 3 != 0) person.getAttributes().putAttribute("cantonName", i % 3 == 1 ? "vaud" : "unknown");
                    person.getAttributes().putAttribute("carASC", i * 0.13 - 1.0);
                    var leg = f.createLeg("car");
                    var trip = new DiscreteModeChoiceTrip(origin, destination, "car", List.of(leg), i, 0, 0, new AttributesImpl());
                    trip.setDepartureTime(28800.125 + i);
                    List<PlanElement> elements = List.of(leg);
                    var pv = new SwissPersonVariables(new PersonVariables(new int[] {15,17,18,65,80}[i % 5]), new Coord(0,0), false, false, false, false, false,
                            1, i % 3, new double[] {0,3000,8000,6000,12000}[i % 5], i % 2, i % 3, i * 0.02,
                            new String[] {"A","B","C","D",null}[i % 5], i % 2 == 0);
                    var personPredictor = mock(SwissPersonPredictor.class);
                    when(personPredictor.predictVariables(person, trip, elements)).thenReturn(pv);
                    var car = mock(SwissCarPredictor.class);
                    var cv = new CarVariables(10 + i, 2.5 + i, distance, 1);
                    when(car.predictVariables(person, trip, elements)).thenReturn(cv);
                    var parking = mock(SwissParkingCostModel.class);
                    when(parking.getParkingPrice_CFH(trip, cv)).thenReturn(3.4);
                    expected.get("car").add(new SwissCarDetailedUtilityEstimator(p, car, personPredictor, parking, writer).estimateUtility(person, trip, elements));
                    var pt = mock(PtPredictor.class);
                    when(pt.predictVariables(person, trip, elements)).thenReturn(new PtVariables(12+i, i%4, 2+i, i%3, 1+i, distance));
                    var cost = SwissCostParameters.buildDefault(); cost.ptRegionalRadius_km = 17.5;
                    expected.get("pt").add(new SwissPtDetailedUtilityEstimator(p, pt, personPredictor, writer, cost).estimateUtility(person, trip, elements));
                    var bike = mock(SwissBikePredictor.class);
                    when(bike.predictVariables(person, trip, elements)).thenReturn(new BikeVariables(11+i));
                    expected.get("bike").add(new SwissBikeDetailedUtilityEstimator(p, personPredictor, bike, writer).estimateUtility(person, trip, elements));
                    var walk = mock(WalkPredictor.class);
                    when(walk.predictVariables(person, trip, elements)).thenReturn(new WalkVariables(18+i));
                    expected.get("walk").add(new SwissWalkDetailedUtilityEstimator(p, walk, personPredictor, writer).estimateUtility(person, trip, elements));
                    var cp = mock(CarPassengerPredictor.class);
                    when(cp.predictVariables(person, trip, elements)).thenReturn(new SwissCarPassengerVariables(9+i, distance, 1));
                    expected.get("car_passenger").add(new SwissCarPassengerDetailedUtilityEstimator(p, cp, personPredictor, writer).estimateUtility(person, trip, elements));
                }
                writer.close();
                var parser = new CSVParserBuilder().withSeparator(';').build();
                for (var entry : expected.entrySet()) {
                    List<String> lines = Files.readAllLines(dir.resolve("choice_variables_" + entry.getKey() + ".csv"));
                    assertEquals(36, lines.size());
                    var header = Arrays.asList(parser.parseLine(lines.getFirst()));
                    assertTrue(header.containsAll(List.of("highIncome", "employeeDensity", "densitiesAvailable")));
                    for (int i = 0; i < 35; i++) {
                        String[] values = parser.parseLine(lines.get(i+1));
                        assertEquals(header.size(), values.length);
                        assertEquals("00;agent\""+i, values[0]);
                        assertEquals(28800.125+i, Double.parseDouble(values[2]), 0);
                        assertEquals(entry.getValue().get(i), Double.parseDouble(values[3]), 0);
                    }
                }
            }
        } finally { Locale.setDefault(previous); }
    }
}
