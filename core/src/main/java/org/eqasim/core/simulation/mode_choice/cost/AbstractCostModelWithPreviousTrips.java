package org.eqasim.core.simulation.mode_choice.cost;

public abstract class AbstractCostModelWithPreviousTrips extends AbstractCostModel
		implements CostModelWithPreviousTrips {
	protected AbstractCostModelWithPreviousTrips(String mode) {
		super(mode);
	}
}
