package org.eqasim.examples.zurich_parking.parking.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacility;
import org.matsim.facilities.*;

public class ZurichWhiteZoneParking extends ActivityFacilityImpl implements ParkingFacility {

    private final Id<ParkingFacility> parkingFacilityId;
    private final double maxParkingDuration;
    private final String parkingType;

    public ZurichWhiteZoneParking(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId,
                                  double maxParkingDuration, String parkingType, double capacity) {
        super(id, coordParking, linkId);
        this.parkingFacilityId = Id.create(id, ParkingFacility.class);
        this.maxParkingDuration = maxParkingDuration;
        this.parkingType = parkingType;

        // add parking activity option with capacity
        ActivityFacilitiesFactory facilitiesFactory = new ActivityFacilitiesFactoryImpl();
        ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
        activityOption.setCapacity(capacity);
        this.addActivityOption(activityOption);
    }

    @Override
    public Id<ParkingFacility> getParkingFacilityId() {
        return parkingFacilityId;
    }

    @Override
    public String getParkingType() {
        return parkingType;
    }

    @Override
    public double getMaxParkingDuration() {
        return maxParkingDuration;
    }

    @Override
    public double getParkingCost(double startTime, double endTime) {
        // by default, you pay for the entire parking duration
        double paidDuration = endTime - startTime;
        double fee = calculateParkingFee(paidDuration);

        // however, there are periods where parking is free
        // early morning case, i.e., before 9:00
        if (startTime < 9 * 3600.0) {
            double lowerBoundTime = 9 * 3600.0;

            // calculate new paid duration and fee
            paidDuration = Math.max(endTime - lowerBoundTime, 0.0);
            fee = calculateParkingFee(paidDuration);
        }
        // evening case, i.e., after 20:00 - maxParkingDuration
        else if (startTime >= 20*3600.0 - maxParkingDuration) {
            double upperBoundTime = 20*3600.0;
            double lowerBoundTime = (9 + 24) * 3600.0;

            // there can possibly be two paid periods,
            // one before the free evening parking period
            paidDuration = Math.max(upperBoundTime - startTime, 0.0);
            fee = calculateParkingFee(paidDuration);

            // and one after the free evening parking period
            paidDuration = Math.max(endTime - lowerBoundTime, 0.0);
            fee += calculateParkingFee(paidDuration);
        }

        return Math.max(fee, 0.0);
    }

    private double calculateParkingFee(double duration) {
        if (parkingType.equals(ParkingFacilityType.HighTariffWhiteZone.toString())) {
            return calculateHighTariffFee(duration / 60);
        } else if (parkingType.equals(ParkingFacilityType.LowTariffWhiteZone.toString())) {
            return calculateLowTariffFee(duration / 60);
        } else throw new RuntimeException("Unknown white zone parking facility type!");
    }

    // high-tariff zone fees
    private double calculateHighTariffParkingMeterControlFee(double durationInMinutes) {
        return Math.ceil(durationInMinutes / 20) * 0.5;
    }

    private double computeHighTariffParkingFee(double durationInMinutes) {
        double fee = 0;

        if (durationInMinutes > 30) {
            fee += Math.ceil((Math.min(durationInMinutes, 2 * 60) - 30) / 10) * 0.5;
        }

        if (durationInMinutes > 2 * 60) {
            fee += Math.ceil((durationInMinutes - (2 * 60)) / 60) * 0.5;
        }

        return fee;
    }

    private double calculateHighTariffFee(double durationInMinutes) {
        double parkingMeterControlFee = calculateHighTariffParkingMeterControlFee(durationInMinutes);
        double parkingFee = computeHighTariffParkingFee(durationInMinutes);
        return parkingMeterControlFee + parkingFee;
    }

    // low-tariff zone fees
    private double calculateLowTariffFee(double durationInMinutes) {
        return Math.ceil(durationInMinutes / 60) * 0.5;
    }


    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId, String purpose) {
        // the default max parking time is defined by the posted time limit
        double mustLeaveByTime = startTime + this.maxParkingDuration;

        // however, there are exceptions which allow you to park longer depending on when you arrive
        // early morning case, i.e., before 9:00
        if (startTime < 9 * 3600.0) {
            mustLeaveByTime = 9 * 3600.0 + maxParkingDuration; // must leave by 9:00 + maxParkingDuration
        }
        // evening case, i.e., after 20:00 - maxParkingDuration
        else if (startTime >= 20*3600.0 - maxParkingDuration) {
            mustLeaveByTime = (9 + 24) * 3600.0 + maxParkingDuration; // must leave by 9:00 the next day + maxParkingDuration
        }

        return !(endTime > mustLeaveByTime);
    }

}
