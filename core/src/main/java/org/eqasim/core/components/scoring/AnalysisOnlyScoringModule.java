package org.eqasim.core.components.scoring;

import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

/** Skip conventional utility scoring only for a known, score-independent DMC configuration.
 * Experienced activities, legs, travel statistics and DMC utility estimation remain active.
 */
public final class AnalysisOnlyScoringModule extends AbstractModule {
    public static final String OPTION = "analysis-only-scoring";
    private static final Set<String> SCORE_INDEPENDENT_STRATEGIES =
            Set.of("DiscreteModeChoice", "KeepLastSelected", "ReRoute");

    public static boolean isEnabled(CommandLine command) throws CommandLine.ConfigurationException {
        String value = command.getOption(OPTION).orElse("true");
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new CommandLine.ConfigurationException("--" + OPTION + " must be true or false: " + value);
    }

    /** Conservative allowlist: also check zero-weight strategies, which a schedule may enable later. */
    public static Optional<String> incompatibility(Config config) {
        if (!(config.getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME)
                instanceof DiscreteModeChoiceConfigGroup dmc) || !dmc.getEnforceSinglePlan()) {
            return Optional.of("DMC must enforce a single plan");
        }
        if (config.replanning().getMaxAgentPlanMemorySize() != 1
                || !"NonSelectedPlanSelector".equals(config.replanning().getPlanSelectorForRemoval())) {
            return Optional.of("plan memory/removal must be independent of conventional scores");
        }
        for (var strategy : config.replanning().getStrategySettings()) {
            if (strategy.getStrategyName() == null
                    || !SCORE_INDEPENDENT_STRATEGIES.contains(strategy.getStrategyName())) {
                return Optional.of("strategy requires conventional scoring or is unknown: " + strategy.getStrategyName());
            }
        }
        var scoring = config.scoring();
        if (scoring.isWriteExperiencedPlans() || scoring.isWriteScoreExplanations()
                || scoring.isMemorizingExperiencedPlans()) {
            return Optional.of("score-bearing experienced-plan/explanation output is enabled");
        }
        if (scoring.getLearningRate() != 1.0 || scoring.getFractionOfIterationsToStartScoreMSA() != null) {
            return Optional.of("conventional score smoothing is enabled");
        }
        return Optional.empty();
    }

    @Override public void install() {
        var reason = incompatibility(getConfig());
        if (reason.isPresent()) {
            LogManager.getLogger(getClass()).info("Keeping conventional scoring: {}", reason.get());
            return;
        }
        LogManager.getLogger(getClass()).info("Analysis-only scoring: conventional plan scores are zero placeholders; "
                + "experienced travel records and DMC utilities remain enabled.");
        bind(ScoringFunctionFactory.class).toInstance(new Factory());
    }

    /** Marker recognised by the version-pinned ScoringFunctionsForPopulation compatibility copy.
     * A shared scorer is safe here because every method is stateless; no per-person allocation is needed.
     */
    public static final class Factory implements ScoringFunctionFactory {
        public static final ScoringFunction UNSCORED = new ScoringFunction() {
            @Override public void handleActivity(Activity activity) { }
            @Override public void handleLeg(Leg leg) { }
            @Override public void agentStuck(double time) { }
            @Override public void addMoney(double amount) { }
            @Override public void addScore(double amount) { }
            @Override public void finish() { }
            @Override public double getScore() { return 0.0; }
            @Override public void handleEvent(Event event) { }
        };

        @Override public ScoringFunction createNewScoringFunction(Person person) { return UNSCORED; }
    }
}
