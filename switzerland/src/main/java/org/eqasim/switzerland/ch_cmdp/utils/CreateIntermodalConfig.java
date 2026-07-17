package org.eqasim.switzerland.ch_cmdp.utils;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class CreateIntermodalConfig {

    private static final String USAGE = """
            Usage:
              CreateIntermodalConfig \\
                --input-path config.xml \\
                --output-path config_intermodal.xml \\
                --access-egress-mode "bike;initialSearchRadius=1000;maxRadius=5000;searchExtensionRadius=500;linkIdAttribute=bikeLinkId,car;initialSearchRadius=5000;maxRadius=25000;personFilterAttribute=carAvail;personFilterValue=always"

            Access/egress modes:
              --access-egress-modes bike,car
                  Adds simple modes using the global/default attributes.

              --access-egress-mode "bike:1000:5000:500,car:5000:25000:1000"
                  Adds per-mode radius settings as mode:initialSearchRadius:maxRadius:searchExtensionRadius.

              --access-egress-mode "mode=bike;initialSearchRadius=1000;maxRadius=5000;searchExtensionRadius=500;shareTripSearchRadius=0.5;linkIdAttribute=bikeLinkId;personFilterAttribute=bikeAvailability;personFilterValue=FOR_ALL;stopFilterAttribute=bikeAccess;stopFilterValue=true"
                  Adds one or more fully configured modes. Separate modes with commas.

            Global defaults, used unless a mode overrides them:
              --initial-search-radius 1000
              --max-radius 5000
              --search-extension-radius 500
              --share-trip-search-radius 0.5
              --link-id-attribute bikeLinkId
              --person-filter-attribute bikeAvailability
              --person-filter-value FOR_ALL
              --stop-filter-attribute bikeAccess
              --stop-filter-value true

            SwissRailRaptor options:
              --intermodal-leg-only-handling allow|avoid|forbid
              --intermodal-access-egress-mode-selection CalcLeastCostModePerStop|RandomSelectOneModePerRoutingRequestAndDirection
            """;

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        if (args.length == 0 || hasHelpOption(args)) {
            System.out.println(USAGE);
            return;
        }

        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("input-path", "output-path")
                .allowOptions(
                        "access-egress-modes",
                        "access-egress-mode",
                        "initial-search-radius",
                        "max-radius",
                        "search-extension-radius",
                        "share-trip-search-radius",
                        "link-id-attribute",
                        "person-filter-attribute",
                        "person-filter-value",
                        "stop-filter-attribute",
                        "stop-filter-value",
                        "intermodal-leg-only-handling",
                        "intermodal-access-egress-mode-selection")
                .build();

        if (!cmd.hasOption("access-egress-mode") && !cmd.hasOption("access-egress-modes")) {
            throw new CommandLine.ConfigurationException("Missing access/egress modes.\n\n" + USAGE);
        }
        SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"));
        configurator.updateConfig(config);
        SwissRailRaptorConfigGroup raptorConfig = (SwissRailRaptorConfigGroup) config.getModules()
                .get(SwissRailRaptorConfigGroup.GROUP);
        raptorConfig.setUseIntermodalAccessEgress(true);

        if (cmd.hasOption("intermodal-leg-only-handling")) {
            raptorConfig.setIntermodalLegOnlyHandling(cmd.getOptionStrict("intermodal-leg-only-handling"));
        }
        if (cmd.hasOption("intermodal-access-egress-mode-selection")) {
            raptorConfig.setIntermodalAccessEgressModeSelection(
                    SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection.valueOf(
                            cmd.getOptionStrict("intermodal-access-egress-mode-selection")));
        }

        addAccessEgressOptions(cmd, raptorConfig);

        new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
    }

    private static void addAccessEgressOptions(CommandLine cmd, SwissRailRaptorConfigGroup raptorConfig)
            throws CommandLine.ConfigurationException {
        if (cmd.hasOption("access-egress-mode")) {
            for (String modeSpec : cmd.getOptionStrict("access-egress-mode").split("\\s*,\\s*")) {
                if (!modeSpec.isBlank()) {
                    raptorConfig.addIntermodalAccessEgress(createParameterSet(modeSpec, cmd));
                }
            }
        }

        if (cmd.hasOption("access-egress-modes")) {
            for (String mode : cmd.getOptionStrict("access-egress-modes").split("\\s*,\\s*")) {
                if (!mode.isBlank()) {
                    raptorConfig.addIntermodalAccessEgress(createParameterSet(mode, cmd));
                }
            }
        }
    }

    private static IntermodalAccessEgressParameterSet createParameterSet(String modeSpec, CommandLine cmd)
            throws CommandLine.ConfigurationException {
        if (modeSpec.contains(";") || modeSpec.contains("=")) {
            return createParameterSetFromAttributes(modeSpec, cmd);
        }

        String[] values = modeSpec.split(":");
        IntermodalAccessEgressParameterSet parameterSet = new IntermodalAccessEgressParameterSet()
                .setMode(values[0])
                .setInitialSearchRadius(getDouble(cmd, "initial-search-radius", value(values, 1, 1000.0)))
                .setMaxRadius(getDouble(cmd, "max-radius", value(values, 2, 5000.0)))
                .setSearchExtensionRadius(getDouble(cmd, "search-extension-radius", value(values, 3, 500.0)))
                .setShareTripSearchRadius(getDouble(cmd, "share-trip-search-radius", Double.POSITIVE_INFINITY));

        if (cmd.hasOption("link-id-attribute")) {
            parameterSet.setLinkIdAttribute(cmd.getOptionStrict("link-id-attribute"));
        }
        if (cmd.hasOption("person-filter-attribute")) {
            parameterSet.setPersonFilterAttribute(cmd.getOptionStrict("person-filter-attribute"));
        }
        if (cmd.hasOption("person-filter-value")) {
            parameterSet.setPersonFilterValue(cmd.getOptionStrict("person-filter-value"));
        }
        if (cmd.hasOption("stop-filter-attribute")) {
            parameterSet.setStopFilterAttribute(cmd.getOptionStrict("stop-filter-attribute"));
        }
        if (cmd.hasOption("stop-filter-value")) {
            parameterSet.setStopFilterValue(cmd.getOptionStrict("stop-filter-value"));
        }

        return parameterSet;
    }

    private static IntermodalAccessEgressParameterSet createParameterSetFromAttributes(String modeSpec, CommandLine cmd)
            throws CommandLine.ConfigurationException {
        Map<String, String> attributes = parseAttributes(modeSpec);
        String mode = attributes.remove("mode");
        if (mode == null) {
            mode = attributes.remove("name");
        }
        if (mode == null) {
            String firstPart = modeSpec.split(";", 2)[0].trim();
            if (!firstPart.contains("=")) {
                mode = firstPart;
            }
        }
        if (mode == null || mode.isBlank()) {
            throw new CommandLine.ConfigurationException("Missing mode in access/egress mode spec: " + modeSpec);
        }

        IntermodalAccessEgressParameterSet parameterSet = new IntermodalAccessEgressParameterSet()
                .setMode(mode)
                .setInitialSearchRadius(getDouble(attributes, cmd, "initialSearchRadius", "initial-search-radius", 1000.0))
                .setMaxRadius(getDouble(attributes, cmd, "maxRadius", "max-radius", 5000.0))
                .setSearchExtensionRadius(getDouble(attributes, cmd, "searchExtensionRadius",
                        "search-extension-radius", 500.0))
                .setShareTripSearchRadius(getDouble(attributes, cmd, "shareTripSearchRadius",
                        "share-trip-search-radius", Double.POSITIVE_INFINITY));

        setString(attributes, cmd, "linkIdAttribute", "link-id-attribute", parameterSet::setLinkIdAttribute);
        setString(attributes, cmd, "personFilterAttribute", "person-filter-attribute",
                parameterSet::setPersonFilterAttribute);
        setString(attributes, cmd, "personFilterValue", "person-filter-value", parameterSet::setPersonFilterValue);
        setString(attributes, cmd, "stopFilterAttribute", "stop-filter-attribute", parameterSet::setStopFilterAttribute);
        setString(attributes, cmd, "stopFilterValue", "stop-filter-value", parameterSet::setStopFilterValue);

        if (!attributes.isEmpty()) {
            throw new CommandLine.ConfigurationException(
                    "Unknown access/egress attributes for mode " + mode + ": " + attributes.keySet());
        }

        return parameterSet;
    }

    private static Map<String, String> parseAttributes(String modeSpec) throws CommandLine.ConfigurationException {
        Map<String, String> attributes = new LinkedHashMap<>();
        String[] parts = modeSpec.split("\\s*;\\s*");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isBlank()) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator < 0) {
                if (i == 0) {
                    attributes.put("mode", part);
                    continue;
                }
                throw new CommandLine.ConfigurationException("Expected key=value in access/egress spec part: " + part);
            }
            attributes.put(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
        }
        return attributes;
    }

    private static double getDouble(Map<String, String> attributes, CommandLine cmd, String attribute, String option,
            double defaultValue) throws CommandLine.ConfigurationException {
        if (attributes.containsKey(attribute)) {
            return Double.parseDouble(attributes.remove(attribute));
        }
        return getDouble(cmd, option, defaultValue);
    }

    private static double getDouble(CommandLine cmd, String option, double defaultValue)
            throws CommandLine.ConfigurationException {
        if (cmd.hasOption(option)) {
            return Double.parseDouble(cmd.getOptionStrict(option));
        }
        return defaultValue;
    }

    private static void setString(Map<String, String> attributes, CommandLine cmd, String attribute, String option,
            StringSetter setter) throws CommandLine.ConfigurationException {
        if (attributes.containsKey(attribute)) {
            setter.set(attributes.remove(attribute));
        } else if (cmd.hasOption(option)) {
            setter.set(cmd.getOptionStrict(option));
        }
    }

    private static double value(String[] values, int index, double defaultValue) {
        if (values.length > index && !values[index].isBlank()) {
            return Double.parseDouble(values[index]);
        }
        return defaultValue;
    }

    private static boolean hasHelpOption(String[] args) {
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface StringSetter {
        void set(String value);
    }

}
