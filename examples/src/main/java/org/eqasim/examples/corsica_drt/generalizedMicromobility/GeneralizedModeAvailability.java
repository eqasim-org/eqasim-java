package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.config.CommandLine;
import org.matsim.core.population.PersonUtils;

import java.lang.reflect.Field;
import java.util.*;

public class GeneralizedModeAvailability implements ModeAvailability {
    public CommandLine cmd;

    public Collection<String> modes=  new HashSet<>();


    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .allowOptions("use-rejection-constraint") //
                .allowPrefixes("mode-parameter", "cost-parameter","sharing-mode-name") //
                .build();
//        GeneralizedModeAvailability modeAvailability = GeneralizedModeAvailability.buildDefault();
//        try {
//            GeneralizedModeAvailability.applyCommandLineAvailability("sharing-mode-name",cmd,modeAvailability);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        System.out.println("uwu");


    }
    public static GeneralizedModeAvailability buildDefault(){
        return new GeneralizedModeAvailability();
    }
    public static void applyCommandLineAvailability(String prefix, CommandLine cmd, GeneralizedModeAvailability modeAvailability) throws Exception {
        Object activeObject = modeAvailability;
        Map<String, String> values = new HashMap<>();
        for (String option : cmd.getAvailableOptions()) {
            if (option.startsWith(prefix + ":")) {
                values.put(option.split(":")[1], cmd.getOptionStrict(option));
                Field field = activeObject.getClass().getField("modes");
                if (field.getType() == Collection.class) {

                    Collection<String> reeplacement = (Collection<String>) field.get(activeObject);
                    String[] parts = option.split(":");// divide el punto
                    String[] parts2 = parts[1].split("\\.");

                    if (parts2.length == 1) {
                        reeplacement.add("sharing:" + (option.split(":")[1]));
                    }

                }
            }

            applyIntermodalMap(modeAvailability, values);
        }
    }
    static public void applyIntermodalMap(GeneralizedModeAvailability modeAvailability,Map<String,String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String option = entry.getKey();
            String value = entry.getValue();
                try {
                    String[] parts = option.split("\\.");// divide el punto
                    if(parts.length!=1) {
                        if (parts[1].equals("Intermodal")) {
                            Collection<String> reeplacement = modeAvailability.modes;
                            reeplacement.add(parts[0] + "_PT");
                            reeplacement.add(parts[0] + "_PT_" + parts[0]);
                            reeplacement.add("PT_" + parts[0]);
                            System.out.println("uwu");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }



    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = this.modes;

        // Modes that are always available
        modes.add(TransportMode.walk);
        modes.add(TransportMode.pt);
        //modes.add("sharing:bikeShare");
//        modes.add("sharing:eScooter");
//        modes.add("Sharing_PT");
//        modes.add("PT_Sharing");
//        modes.add("Sharing_PT_Sharing");
//        modes.add("escooterPT");

        // Check car availability
        boolean carAvailability = true;

        if ("no".equals(PersonUtils.getLicense(person))) {
            carAvailability = false;
        }

        if ("none".equals((String) person.getAttributes().getAttribute("carAvailability"))) {
            carAvailability = false;
        }

        if (carAvailability) {
            modes.add(TransportMode.car);
        }

        // Check bike availability
        boolean bikeAvailability = true;

        if ("none".equals((String) person.getAttributes().getAttribute("bikeAvailability"))) {
            bikeAvailability = false;
        }

        if (bikeAvailability) {
            modes.add(TransportMode.bike);
        }

        // Add special mode "outside" if applicable
        Boolean isOutside = (Boolean) person.getAttributes().getAttribute("outside");

        if (isOutside != null && isOutside) {
            modes.add("outside");
        }

        // Add special mode "car_passenger" if applicable
        Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isPassenger");

        if (isCarPassenger != null && isCarPassenger) {
            modes.add("car_passenger");
        }

        return modes;
    }
}
