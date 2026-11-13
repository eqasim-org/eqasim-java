package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtPricingConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.opencsv.exceptions.CsvValidationException;

/**
 * Reads the inputs of the ptPricing module, the same way for the ch and ch_cmdp simulations and for the tools
 * (RunComputeTransitPrices, the multimodal router): every path is relative to the config file.
 */
public final class SwissPtPricingInputs {
    /**
     * The pricing description bundled with eqasim, used when the config has no pricingDescriptionPath: a copy of the
     * one eqasim-switzerland passes to MATSim (data_path/pt_pricing/output/pricingDescription.xml), to keep in sync.
     */
    public static final String DEFAULT_PRICING_DESCRIPTION = "/org/eqasim/switzerland/ch/mode_choice/pricingDescription.xml";

    private SwissPtPricingInputs() {
    }

    /** A path of the config, relative to the config file. */
    public static URL url(Config config, String path) {
        return ConfigGroup.getInputFileURL(config.getContext(), path);
    }

    /** A local file of the config, relative to the config file (also with spaces or other escaped characters). */
    public static File file(Config config, String path) throws IOException {
        URL url = url(config, path);
        try {
            return new File(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IOException("The PT pricing input " + path + " is not a local file: " + url, e);
        }
    }

    private static String required(String path, String parameter) throws IOException {
        if (!SwissPtPricingConfigGroup.isSet(path)) {
            throw new IOException(SwissPtPricingConfigGroup.GROUP_NAME + ": " + parameter + " is not set");
        }
        return path;
    }

    /** The zones of the stops (ptZonesFilePath) and the SBB zone of the stops of the SBB distances (sbbDistancesPath). */
    public static ZonalRegistry readZonalRegistry(Config config, SwissPtPricingConfigGroup group)
            throws IOException, CsvValidationException {
        File zonesFile = file(config, required(group.getZonePath(), "ptZonesFilePath"));
        File distancesFile = file(config, required(group.getSBBDistancesPath(), "sbbDistancesPath"));

        ZonalReader reader = new ZonalReader();
        Collection<Authority> authorities = reader.readTarifNetworks(zonesFile);
        ZonalRegistry registry = new ZonalRegistry(authorities, reader.readZones(zonesFile, authorities));
        registry.merge(SBBDistanceReader.createZonalRegistry(SBBDistanceReader.createZone(distancesFile)));
        return registry;
    }

    /** The SBB distances between the stations (sbbDistancesPath). */
    public static NetworkOfDistances readNetworkOfDistances(Config config, SwissPtPricingConfigGroup group)
            throws IOException, CsvValidationException {
        return SBBDistanceReader.createNetworkOfDistances(file(config, required(group.getSBBDistancesPath(), "sbbDistancesPath")));
    }

    /**
     * The fares of the pricing description (pricingDescriptionPath, else the bundled one) and the Léman Pass single
     * tickets, if the config has them.
     */
    public static SwissPtStageCostCalculator readCalculator(Config config, SwissPtPricingConfigGroup group) throws Exception {
        SwissPtStageCostCalculator calculator = SwissPtPricingConfigGroup.isSet(group.getPricingDescriptionPath())
                ? PricingDescriptionReader.readPriceDescription(url(config, group.getPricingDescriptionPath()))
                : readDefaultCalculator();
        calculator.setLemanPass(LemanPassReader.read(group, config.getContext()));
        return calculator;
    }

    /**
     * The fares of the bundled pricing description: the default price by distance, SBB, and every zone network
     * (Unireso around its centre zone). The rides in a network without prices (e.g. CTJU) are priced by distance, as
     * "None".
     */
    public static SwissPtStageCostCalculator readDefaultCalculator() {
        URL url = SwissPtPricingInputs.class.getResource(DEFAULT_PRICING_DESCRIPTION);
        if (url == null) {
            throw new IllegalStateException("The bundled " + DEFAULT_PRICING_DESCRIPTION + " is missing");
        }
        try {
            return PricingDescriptionReader.readPriceDescription(url);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read the bundled " + DEFAULT_PRICING_DESCRIPTION, e);
        }
    }
}
