package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.estimators.SMMBikeShareEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.predictors.SMMBikeSharePredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels.SMMBikeShareCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPersonPredictor;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.SharingPTModeChoiceModule;
import org.matsim.core.config.CommandLine;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.geotools.feature.type.DateUtil.isEqual;

public class SMMCostParameters implements ParameterDefinition {
    public double carCost_Km;
    public double bookingCostBikeShare;
    public double bikeShareCost_Km;
    public double bookingCostEScooter;
    public double eScooterCost_km;
    public double pTTicketCost;
    public HashMap<String, Double> sharingBookingCosts;
    public HashMap<String, Double> sharingMinCosts;

//    public static void main(String[] args) throws Exception {
//        CommandLine cmd = new CommandLine.Builder(args) //
//                .allowOptions("use-rejection-constraint") //
//                .allowPrefixes("mode-parameter", "cost-parameter") //
//                .build();
//        GeneralizedCostParameters parameters = GeneralizedCostParameters.buildDefault();
//        GeneralizedCostParameters.applyCommandLineMicromobility("cost-parameter", cmd, parameters);
//        List<? extends PlanElement> sharingAccess = null;
//        PopulationFactory popFact = new newPopFactory2(null);
//        Leg walk = popFact.createLeg("walk");
//        walk.setDepartureTime((9 * 60 * 60) + (59 * 60));
//        walk.setTravelTime(60);
//        Route walkRoute1 = new RouteTestImpl();
//        walkRoute1.setDistance(100.0);
//        walkRoute1.setTravelTime(60.0);
//        walk.setRoute(walkRoute1);
//        Activity booking = ((newPopFactory2) popFact).createTestActivity("sharing booking interaction", ((10 * 60 * 60) + 60), (10 * 60 * 60), new Coord(0, 1));
//        booking.setMaximumDuration(60);
//        booking.getMaximumDuration();
//        Leg walk2 = popFact.createLeg("walk");
//        walk2.setDepartureTime((10 * 60 * 60) + (60));
//        walk2.setTravelTime(0);
//        Route walkRoute2 = new RouteTestImpl();
//        walkRoute2.setDistance(0);
//        walkRoute2.setTravelTime(0);
//        Activity pickUp = ((newPopFactory2) popFact).createTestActivity("sharing pickup interaction", ((10 * 60 * 60) + 60), ((10 * 60 * 60) + 60), new Coord(0, 1));
//        pickUp.setMaximumDuration(60);
//        Leg bikeLeg = popFact.createLeg("bike");
//        bikeLeg.setDepartureTime((10 * 60 * 60) + (60));
//        bikeLeg.setTravelTime(15 * 60);
//        Route bikeRoute = new RouteTestImpl();
//        bikeRoute.setDistance(4000);
//        bikeRoute.setTravelTime(15 * 60);
//        bikeLeg.setRoute(bikeRoute);
//
//        Activity dropOff = ((newPopFactory2) popFact).createTestActivity("sharing dropoff interaction", ((10 * 60 * 60) + (15 * 60)), ((10 * 60 * 60) + (15 * 60)), new Coord(15, 20));
////        dropOff.setMaximumDuration(60);
//
//        Leg walk3 = popFact.createLeg("walk");
//        walk3.setDepartureTime((10 * 60 * 60) + (15 * 60));
//        walk3.setTravelTime(5 * 60);
//        Route walkRoute3 = new RouteTestImpl();
//        walkRoute3.setDistance(100);
//        walkRoute3.setTravelTime(5 * 60);
//        walk3.setRoute(walkRoute3);
//
//        List<PlanElement> sharingEgress = new LinkedList<>();
//        sharingEgress.add((PlanElement) walk);
//        sharingEgress.add((PlanElement) booking);
//        sharingEgress.add((PlanElement) walk2);
//        sharingEgress.add((PlanElement) pickUp);
//        sharingEgress.add((PlanElement) bikeLeg);
//        sharingEgress.add((PlanElement) dropOff);
//        sharingEgress.add((PlanElement) walk3);
//        GeneralizedBikeShareCostModel costModel = new GeneralizedBikeShareCostModel("BikeShareStandard", parameters);
//        Double distance = costModel.getInVehicleDistance_km(sharingEgress);
//        Double cost = costModel.calculateCost_MU(null, null, sharingEgress);
//
//        GeneralizedBikeShareEstimator estimator = buildDefault().addSharingServiceToEqasim(new EqasimConfigGroup(), cmd, "BikeShareStandard");
//
//        DiscreteModeChoiceTrip trip = new DiscreteModeChoiceTrip(booking, dropOff, "walk",
//                Collections.emptyList(), 0, 0, 0);
//
//        Person personProx = popFact.createPerson(Id.createPersonId("person"));
//        personProx.getAttributes().putAttribute("age_a", 25);
//        personProx.getAttributes().putAttribute("bikeAvailability", "some");
//        personProx.getAttributes().putAttribute("carAvailavility", "none");
//        personProx.getAttributes().putAttribute("hasPtSubscription", false);
//        personProx.getAttributes().putAttribute("age", 25);
//
//        Double utility = estimator.estimateUtility(personProx, trip, sharingEgress);
//        String x = "Uwu";
//
//
//    }
//
    public static void applyCommandLineMicromobility(String prefix, CommandLine cmd, ParameterDefinition parameterDefinition) throws Exception {
        Map<String, String> values = new HashMap<>();

        for (String option : cmd.getAvailableOptions()) {
            if (option.startsWith(prefix + ":")) {
                try {
                    values.put(option.split(":")[1], cmd.getOptionStrict(option));
                } catch (CommandLine.ConfigurationException e) {
                    // Should not happen
                }
            }
        }

        // ParameterDefinition.applyMap(parameterDefinition, values);
        applyMapMicromobilityCost(parameterDefinition, values);
        validateSharingCostParameters((SMMCostParameters) parameterDefinition);

    }

    private static void applyMapMicromobilityCost(ParameterDefinition parameterDefinition, Map<String, String> values) throws Exception {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String option = entry.getKey();
            String value = entry.getValue();

            try {
                String[] parts = option.split("\\.");
                int numberOfParts = parts.length;
                if (parts[0].equals("sharingBookingCosts") || parts[0].equals("sharingMinCosts")) {
                    Object activeObject = parameterDefinition;


                    Field field = activeObject.getClass().getField(parts[0]);
                    if (field.getType() == HashMap.class || field.getType() == Map.class) {
                        HashMap<String, Double> reeplacement = (HashMap<String, Double>) field.get(activeObject);
                        reeplacement.put("sharing:" + parts[1], Double.parseDouble(value));

                        System.out.println("xdxd");
                        //field.set(activeObject,reeplacement);
                        System.out.println("xdxd");
                    }


                    logger.info(String.format("Set %s = %s", option, value));
                } else {
                    Object activeObject = parameterDefinition;

                    for (int i = 0; i < parts.length; i++) {
                        Field field = activeObject.getClass().getField(parts[i]);// coge el campo

                        if (i == numberOfParts - 1) {
                            // We need to set the value
                            if (field.getType() == Double.class || field.getType() == double.class) {
                                field.setDouble(activeObject, Double.parseDouble(value));
                            } else if (field.getType() == Float.class || field.getType() == float.class) {
                                field.setFloat(activeObject, Float.parseFloat(value));
                            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                                field.setInt(activeObject, Integer.parseInt(value));
                            } else if (field.getType() == Long.class || field.getType() == long.class) {
                                field.setLong(activeObject, Long.parseLong(value));
                            } else if (field.getType() == String.class) {
                                field.set(activeObject, value);
                            } else if (field.getType().isEnum()) {
                                Class<Enum> enumType = (Class<Enum>) field.getType();
                                field.set(activeObject, Enum.valueOf(enumType, value));
                            } else {
                                throw new IllegalStateException(
                                        String.format("Cannot convert parameter %s because type %s is not supported",
                                                option, field.getType().toString()));
                            }
                        } else {
                            // We need to traverse the objects
                            activeObject = field.get(activeObject);
                        }
                    }

                }
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(String.format("Parameter %s does not exist", option));
            } catch (SecurityException | IllegalArgumentException e) {
                logger.error("Error while processing option " + option);
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }


        }


    }


    public static SMMCostParameters buildDefault() {
        SMMCostParameters parameters = new SMMCostParameters();
        parameters.carCost_Km= 0.5;
        parameters.pTTicketCost=1.8;
        parameters.sharingBookingCosts = new HashMap<String, Double>();
        parameters.sharingBookingCosts.put("Standard BikeShare", 0.25);
        parameters.sharingMinCosts = new HashMap<String, Double>();
        parameters.sharingMinCosts.put("Standard BikeShare", 1.0);
        parameters.sharingBookingCosts.put("Standard eScooter", 0.5);
        parameters.sharingMinCosts.put("Standard eScooter", 0.5);

        return parameters;
    }

    public static void validateSharingCostParameters(SMMCostParameters parameterDefinition) throws Exception {
        Set<String> sharingKMCosts = parameterDefinition.sharingMinCosts.keySet();
        Set<String> sharingBookingCosts = parameterDefinition.sharingBookingCosts.keySet();
        boolean isEqual = isEqual(sharingBookingCosts, sharingKMCosts);
        if (isEqual == false) {
            throw new IllegalArgumentException(" One of the sharing modes does not have booking or km cost");
        }
    }


    @Provides
    @Singleton
    public static SMMCostParameters provideCostParameters(EqasimConfigGroup config, CommandLine commandLine) throws Exception {
        SMMCostParameters parameters = SMMCostParameters.buildDefault();

        if (config.getCostParametersPath() != null) {
            ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
        }

        SMMCostParameters.applyCommandLineMicromobility("cost-parameter", commandLine, parameters);
        return parameters;
    }

    private SMMBikeShareEstimator addSharingServiceToEqasim(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
        SMMCostParameters costParameters = SMMCostParameters.provideCostParameters(config, commandLine);
        SMMBikeShareCostModel costModel = new SMMBikeShareCostModel(name, costParameters);
        SMMBikeSharePredictor bikePredictor = new SMMBikeSharePredictor(costModel, name);
        KraussPersonPredictor personPredictor = new KraussPersonPredictor();
        SMMParameters modeParams = new SharingPTModeChoiceModule(commandLine, null).provideModeChoiceParameters(config);
        SMMBikeShareEstimator bikeEstimator = new SMMBikeShareEstimator(modeParams, bikePredictor, personPredictor, name);
        return bikeEstimator;
    }

    public static void applyMapMicromobilityCosts(SMMCostParameters parameterDefinition, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String option = entry.getKey();
            String value = entry.getValue();

            try {
                String[] parts = option.split("\\.");
                int numberOfParts = parts.length;

                Object activeObject = parameterDefinition;


                Field field = activeObject.getClass().getField(parts[0]);


                logger.info(String.format("Set %s = %s", option, value));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(String.format("Parameter %s does not exist", option));
            } catch (SecurityException | IllegalArgumentException e) {
                logger.error("Error while processing option " + option);
                throw new RuntimeException(e);
            }


        }


    }
}



//    @Provides
//    @Singleton
//    public KraussCostParameters provideCostParameters(EqasimConfigGroup config) {
//        KraussCostParameters parameters = KraussCostParameters.buildDefault();
//
//        if (config.getCostParametersPath() != null) {
//            ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
//        }
//
//        ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
//        return parameters;
//    }
