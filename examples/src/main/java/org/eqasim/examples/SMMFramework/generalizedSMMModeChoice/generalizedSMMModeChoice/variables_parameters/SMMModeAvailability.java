package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.config.CommandLine;
import org.matsim.core.population.PersonUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Mode availability object defines which  modes are available for the agent in DMC
 */
public class SMMModeAvailability implements ModeAvailability {
    public CommandLine cmd;

    public Collection<String> modes=  new HashSet<>();


    public static SMMModeAvailability buildDefault(){
        return new SMMModeAvailability();
    }

    /**
     * Method adds the SMM modes based on  command line arguments to the modes available to all agents
     * @param prefix identifies the option in command line
     * @param cmd  command line argumens
     * @param modeAvailability mode availability object
     * @throws Exception
     */
    public static void applyCommandLineAvailability(String prefix, CommandLine cmd, SMMModeAvailability modeAvailability) throws Exception {
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
                    // If the option has Service_Name  prefix then creates the mode in the modes collection of str
                    if(parts2[0].equals("Service_Name")){
                        reeplacement.add("sharing:" + cmd.getOptionStrict(option));

                    }

                }
            }
            // applies if the modes are Multimodal or not
            applyIntermodalMap(modeAvailability, values);
        }
    }


    /**
            * Reads if the SMM modes are multimodal applies the mode availability for all modes
     * @param modeAvailability mode availability object
     * @param values values of command line processed
     */

    static public void applyIntermodalMap(SMMModeAvailability modeAvailability, Map<String,String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String option = entry.getKey();
            String value = entry.getValue();
                try {
                    String[] parts = option.split("\\.");// divide el punto
                    if(parts.length!=1) {
                        if (parts[0].equals("Multimodal")) {
                            if(value.equals("Yes")){
                                Collection<String> reeplacement = modeAvailability.modes;
                                reeplacement.add(parts[1] + "_PT");
                                reeplacement.add(parts[1] + "_PT_" + parts[1]);
                                reeplacement.add("PT_" + parts[1]);
                            }

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
