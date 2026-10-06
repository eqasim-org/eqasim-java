package org.eqasim.switzerland.ch_cmdp.tolls;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestMarginalCostOfTolls {
    @Test
    public void defaultsAndXmlParametersUseIndependentTollSettings() {
        var config = new org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup();
        assertEquals(15.0, config.getTollsValueOfTime(), 0.0);
        assertEquals(0.5, config.getTollsSigma(), 0.0);
        config.addParam("tollsSigma", "0.0");
        config.addParam("tollsValueOfTime", "18.0");
        assertEquals(200.0, new MarginalCostOfTolls(config.getTollsSigma(), config.getTollsValueOfTime())
                .getMarginalCostOfTolls(createVehicle("configured")), 0.0);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1.0}) {
            assertThrows(IllegalArgumentException.class, () -> config.setTollsSigma(invalid));
            assertThrows(IllegalArgumentException.class, () -> config.setTollsValueOfTime(invalid));
            assertThrows(IllegalArgumentException.class, () -> new MarginalCostOfTolls(invalid, 15.0));
            assertThrows(IllegalArgumentException.class, () -> new MarginalCostOfTolls(0.5, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> config.setTollsValueOfTime(0.0));
    }

    @Test
    public void sensitivitiesAreReproducibleAndHaveMeanOne() {
        var first = new MarginalCostOfTolls(0.5, 15.0, 4711L);
        var second = new MarginalCostOfTolls(0.5, 15.0, 4711L);
        double sum = 0.0;
        for (int i = 0; i < 10000; i++) {
            Vehicle vehicle = createVehicle("sample-" + i);
            double value = first.getMarginalCostOfTolls(vehicle);
            org.junit.Assert.assertTrue(Double.isFinite(value) && value > 0.0);
            assertEquals(value, first.getMarginalCostOfTolls(vehicle), 0.0);
            assertEquals(value, second.getMarginalCostOfTolls(vehicle), 0.0);
            sum += value;
        }
        assertEquals(240.0, sum / 10000, 240.0 * 0.03);
        org.junit.Assert.assertNotEquals(first.getMarginalCostOfTolls(createVehicle("seed")),
                new MarginalCostOfTolls(0.5, 15.0, 123L).getMarginalCostOfTolls(createVehicle("seed")), 0.0);
    }

    private static Vehicle createVehicle(String id) {
        return VehicleUtils.createVehicle(
                Id.createVehicleId(id),
                VehicleUtils.createVehicleType(Id.create("car", org.matsim.vehicles.VehicleType.class)));
    }

    @Test
    public void zeroSigmaStillUsesValueOfTime() {
        MarginalCostOfTolls marginalCost = new MarginalCostOfTolls(0.0, 18.0);

        assertEquals(200.0, marginalCost.getMarginalCostOfTolls(createVehicle("vehicle")), 1e-9);
    }

    @Test
    public void convertsCurrencyPerHourToSecondsWithRandomSensitivity() {
        Vehicle vehicle = createVehicle("vehicle");
        MarginalCostOfTolls marginalCost = new MarginalCostOfTolls(0.5, 12.0, 4711L);
        double sensitivity = marginalCost.drawCostSensitivity(vehicle.getId());

        assertEquals(sensitivity * 300.0, marginalCost.getMarginalCostOfTolls(vehicle), 1e-6);
    }

    @Test
    public void higherValueOfTimeReducesTollDisutility() {
        Vehicle vehicle = createVehicle("vehicle");
        double lowerValueOfTime = new MarginalCostOfTolls(0.0, 12.0).getMarginalCostOfTolls(vehicle);
        double higherValueOfTime = new MarginalCostOfTolls(0.0, 24.0).getMarginalCostOfTolls(vehicle);

        assertEquals(lowerValueOfTime / 2.0, higherValueOfTime, 1e-9);
    }

    @Test
    public void rejectsNonPositiveValueOfTime() {
        assertThrows(IllegalArgumentException.class, () -> new MarginalCostOfTolls(0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new MarginalCostOfTolls(0.0, -1.0));
    }
}
