package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.SwissPtPricingInputs;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;

/**
 * The ptPricing module (and its former name, ptZones), the reading of its inputs, and the zone networks with a single
 * price list.
 */
public class TestPtPricingInputs {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void legacyModuleIsMovedIntoPtPricing() {
        Config config = ConfigUtils.createConfig();
        ConfigGroup legacy = config.createModule(SwissPtPricingConfigGroup.LEGACY_GROUP_NAME);
        legacy.addParam("ptZonesFilePath", "zones.csv");
        legacy.addParam("sbbDistancesPath", "distances.csv");
        legacy.addParam("pricingDescriptionPath", "null");

        SwissPtPricingConfigGroup.migrateLegacyModule(config);

        assertFalse(config.getModules().containsKey(SwissPtPricingConfigGroup.LEGACY_GROUP_NAME));
        SwissPtPricingConfigGroup group = (SwissPtPricingConfigGroup) config.getModules().get(SwissPtPricingConfigGroup.GROUP_NAME);
        assertEquals("zones.csv", group.getZonePath());
        assertEquals("distances.csv", group.getSBBDistancesPath());
        assertNull(group.getPricingDescriptionPath());
        assertSame(group, SwissPtPricingConfigGroup.getOrCreate(config));
    }

    @Test
    public void genericPtPricingModuleIsTyped() {
        Config config = ConfigUtils.createConfig();
        config.createModule(SwissPtPricingConfigGroup.GROUP_NAME).addParam("ptZonesFilePath", "zones.csv");
        assertEquals("zones.csv", SwissPtPricingConfigGroup.getOrCreate(config).getZonePath());
    }

    @Test
    public void withoutAnyModuleNothingIsAdded() {
        Config config = ConfigUtils.createConfig();
        SwissPtPricingConfigGroup.migrateLegacyModule(config);
        assertFalse(config.getModules().containsKey(SwissPtPricingConfigGroup.GROUP_NAME));
    }

    @Test
    public void conflictingModulesAreRejected() {
        Config config = ConfigUtils.createConfig();
        SwissPtPricingConfigGroup group = new SwissPtPricingConfigGroup();
        group.setZonePath("a.csv");
        config.addModule(group);
        config.createModule(SwissPtPricingConfigGroup.LEGACY_GROUP_NAME).addParam("ptZonesFilePath", "b.csv");
        assertThrows(IllegalStateException.class, () -> SwissPtPricingConfigGroup.getOrCreate(config));
    }

    @Test
    public void unsetPathsAreNull() {
        SwissPtPricingConfigGroup group = new SwissPtPricingConfigGroup();
        group.setZonePath("null");
        group.setSBBDistancesPath("  ");
        group.setPricingDescriptionPath("");
        assertNull(group.getZonePath());
        assertNull(group.getSBBDistancesPath());
        assertNull(group.getPricingDescriptionPath());
    }

    @Test
    public void inputsAreRelativeToTheConfigEvenWithSpaces() throws Exception {
        File directory = folder.newFolder("a folder with spaces");
        Path zones = directory.toPath().resolve("zones.csv");
        Files.writeString(zones, "stop_id,tarif network,zones\n8500001,[ZVV],ZVV:110\n", StandardCharsets.UTF_8);
        Path distances = directory.toPath().resolve("distances.csv");
        Files.writeString(distances, "origin_id,destination_id,distance\n8500001,8500002,12.0\n", StandardCharsets.UTF_8);
        Path configFile = directory.toPath().resolve("config.xml");
        Files.writeString(configFile, "<?xml version=\"1.0\" ?>\n<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/config_v2.dtd\">\n<config/>\n");

        Config config = ConfigUtils.loadConfig(configFile.toString());
        SwissPtPricingConfigGroup group = SwissPtPricingConfigGroup.getOrCreate(config);
        group.setZonePath("zones.csv");
        group.setSBBDistancesPath("distances.csv");

        ZonalRegistry registry = SwissPtPricingInputs.readZonalRegistry(config, group);
        assertEquals(2, registry.getZones("8500001").size()); // ZVV 110 and SBB
        assertEquals(12.0, SwissPtPricingInputs.readNetworkOfDistances(config, group).getDistance("8500002", "8500001"), 1e-9);

        // without pricingDescriptionPath, the bundled fares
        SwissPtStageCostCalculator calculator = SwissPtPricingInputs.readCalculator(config, group);
        assertNotNull(calculator.priceCalculators.get("None"));
        assertTrue(calculator.priceCalculators.get("ZVV") instanceof ZoneBasedPricing);
    }

    @Test
    public void missingZonesAreReported() {
        Config config = ConfigUtils.createConfig();
        Exception e = assertThrows(Exception.class,
                () -> SwissPtPricingInputs.readZonalRegistry(config, new SwissPtPricingConfigGroup()));
        assertTrue(e.getMessage().contains("ptPricing"));
    }

    @Test
    public void zoneNetworkWithASinglePriceList() {
        Authority network = new Authority("Net", 2, "ZoneBased");
        List<SwissPtLegVariables> legs = List.of(new SwissPtLegVariables(
                Map.of("Net", List.of(new Zone(network, "1", List.of("a")), new Zone(network, "2", List.of("b")))),
                0, 0, 1000, -1, "a", "b", "a", "b"));

        ZoneBasedPricing fullOnly = new ZoneBasedPricing("Net");
        fullOnly.setFullPriceList(new double[] { 3.0, 5.0 });
        fullOnly.checkPrices();
        assertEquals(5.0, fullOnly.calculatePrice("Net", legs, false, null), 1e-9);
        assertEquals(5.0, fullOnly.calculatePrice("Net", legs, true, null), 1e-9);

        ZoneBasedPricing halfOnly = new ZoneBasedPricing("Net");
        halfOnly.setHalbTaxPriceList(new double[] { 2.0, 2.5 });
        halfOnly.checkPrices();
        assertEquals(2.5, halfOnly.calculatePrice("Net", legs, false, null), 1e-9);
        assertEquals(2.5, halfOnly.calculatePrice("Net", legs, true, null), 1e-9);

        // around a centre zone: without the half-fare price of the centre zone, the full one
        ZoneBasedPricing centre = new ZoneBasedPricing("Net");
        centre.setCenterZoneID("1");
        centre.setPriceInCenterZone(3.0);
        centre.setPriceInAdditionalZoneHalbtax(1.0);
        centre.checkPrices();
        assertEquals(4.0, centre.calculatePrice("Net", legs, true, null), 1e-9);
        assertEquals(4.0, centre.calculatePrice("Net", legs, false, null), 1e-9);
    }
}
