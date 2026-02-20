package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.Coord;

public class SwissPersonVariables extends PersonVariables {
	public final Coord homeLocation;
	public final boolean hasGeneralSubscription;
	public final boolean hasHalbtaxSubscription;
	public final boolean hasRegionalSubscription;
	public final boolean hasJuniorSubscription;
	public final boolean hasGleis7Subscription;
	public final int statedPreferenceRegion;
	public final Integer sex;
	public final Double income;
	public final Integer drivingLicense;
	public final Integer cantonCluster;
	public final boolean detailedDataAvailable;
	public final Double carOwnershipRatio;
	public final String ovgk;
	public final boolean hasCar;

	public SwissPersonVariables(PersonVariables delegate, Coord homeLocation, boolean hasGeneralSubscription,
                                boolean hasHalbtaxSubscription, boolean hasRegionalSubscription,
								boolean hasJuniorSubscription, boolean hasGleis7Subscription,
								int statedPreferenceRegion,
                                Integer sex, Double income, Integer drivingLicense, Integer cantonCluster,
								Double carOwnershipRatio, String ovgk, Boolean hasCar) {
		super(delegate.age_a);
		this.homeLocation = homeLocation;
		this.hasGeneralSubscription = hasGeneralSubscription;
		this.hasHalbtaxSubscription = hasHalbtaxSubscription;
		this.hasRegionalSubscription = hasRegionalSubscription;
		this.hasGleis7Subscription = hasGleis7Subscription;
		this.hasJuniorSubscription = hasJuniorSubscription;
		this.statedPreferenceRegion = statedPreferenceRegion;
		this.sex = sex;
		this.income = income;
        this.drivingLicense = drivingLicense;
		this.cantonCluster = cantonCluster;
		this.carOwnershipRatio = carOwnershipRatio;
		this.ovgk = ovgk;
        this.detailedDataAvailable = ((sex != null) && (income != null) && (drivingLicense != null) && (cantonCluster != null));
		this.hasCar = hasCar;
	}

	public SwissPersonVariables(PersonVariables delegate, Coord homeLocation, boolean hasGeneralSubscription,
                                boolean hasHalbtaxSubscription, boolean hasRegionalSubscription, int statedPreferenceRegion) {
		this(delegate, homeLocation, hasGeneralSubscription, hasHalbtaxSubscription, hasRegionalSubscription, false, false,
				statedPreferenceRegion,null, null, null, null, null, null,null);
	}
}
