package ch.sbb.matsim.routing.pt.raptor;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Same transfer cost as DefaultRaptorTransferCostCalculator, which the MATSim simulations use with the
 * mode-to-mode penalties of the static configuration, but the mode-to-mode penalties can be given with
 * each request (fromMode -> toMode -> penalty). Penalties missing from the request come from the static
 * configuration.
 *
 * ModeSpecificTransferCostCalculator cannot be used instead: SwissRailRaptorCore calls the calculator
 * when a transfer is created and again when the next vehicle is boarded, and adds the returned value
 * both times. DefaultRaptorTransferCostCalculator returns the change of the total transfer cost, so the
 * second call adds nothing, while ModeSpecificTransferCostCalculator returns the full penalty both
 * times, which counts every transfer twice.
 *
 * In this package because SwissRailRaptorCore.PathElement is package-private.
 */
public class RequestTransferCostCalculator implements RaptorTransferCostCalculator {
	private final Map<String, Map<String, Double>> penalties;

	public RequestTransferCostCalculator(Map<String, Map<String, Double>> penalties) {
		this.penalties = penalties;
	}

	@Override
	public double calcTransferCost(SwissRailRaptorCore.PathElement currentPE, Supplier<Transfer> transfer,
			RaptorStaticConfig staticConfig, RaptorParameters raptorParams, int totalTravelTime, int transferCount,
			double existingTransferCosts, double currentTime) {
		double modePenalty = getModePenalty(transfer.get(), staticConfig);

		double singleTransferCost = raptorParams.getTransferPenaltyFixCostPerTransfer() + modePenalty
				+ raptorParams.getTransferPenaltyPerTravelTimeHour() / 3600.0 * totalTravelTime;

		double minimum = raptorParams.getTransferPenaltyMinimum();
		double maximum = raptorParams.getTransferPenaltyMaximum();
		singleTransferCost = Math.min(singleTransferCost, Math.max(minimum, maximum));
		singleTransferCost = Math.max(singleTransferCost, Math.min(minimum, maximum));

		// As DefaultRaptorTransferCostCalculator: change of the total transfer cost of the path
		return singleTransferCost * transferCount - existingTransferCosts;
	}

	private double getModePenalty(Transfer transfer, RaptorStaticConfig staticConfig) {
		String fromMode = transfer.getFromTransitRoute().getTransportMode();
		String toMode = transfer.getToTransitRoute().getTransportMode();

		Map<String, Double> fromPenalties = penalties.get(fromMode);

		if (fromPenalties != null && fromPenalties.get(toMode) != null) {
			return fromPenalties.get(toMode);
		}

		return staticConfig.isUseModeToModeTransferPenalty() ? staticConfig.getModeToModeTransferPenalty(fromMode, toMode)
				: 0.0;
	}
}
