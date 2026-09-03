package org.eqasim.core.components.network_calibration;

import org.eqasim.core.components.network_calibration.cost_calibration.CostCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs.AgentAscsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.config.SubpopulationsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreeSpeedCalibrationConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class NetworkCalibrationConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "eqasim:networkCalibration";

    public static final String PENALTY = "penalty";
    public static final String FREESPEED = "freespeed";
    public static final String AGENT = "agent";
    public static final String SUBPOPULATIONS = "subpopulations";

    private static final String ACTIVATE = "activate";
    private static final String CALIBRATE = "calibrate";
    private static final String OBJECTIVE = "objective";
    private static final String COUNTS_FILE = "countsFile";
    private static final String CORRECT_CAPACITIES = "correctCapacities";
    private static final String MIN_SPEED = "minSpeed";
    private static final String MIN_CAPACITY = "minCapacity";
    private static final String MAX_CAPACITY = "maxCapacity";
    private static final String CATEGORY_FIVE_PROMOTION_LANE_THRESHOLD = "categoryFivePromotionLaneThreshold";
    private static final String CATEGORY_FIVE_PROMOTION_SPEED_THRESHOLD = "categoryFivePromotionSpeedThreshold";
    private static final String TOLLS_VALUE_OF_TIME = "tollsValueOfTime";


    private boolean activate;
    private String calibrate = "";
    private String objective = "";
    private String countsFile = "";
    private boolean correctCapacities = true;
    private double minSpeed = 1.0;
    private double minCapacity = 500.0;
    private double maxCapacity = 2_000.0;
    private double categoryFivePromotionLaneThreshold = 1.0;
    private double categoryFivePromotionSpeedThreshold = 45.0;
    private double tollsValueOfTime = 12.0;

    public NetworkCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE, "Whether the network-calibration module is active (default: false)");
        comments.put(CALIBRATE, "Optional comma-separated selector of active components whose values are updated: penalty, freespeed, agent, or subpopulations. Active components not listed here use their configured input/default values.");
        comments.put(OBJECTIVE, "Optional comma-separated selector: penalty, freespeed, agent, or subpopulations. When non-empty, it overrides the activate parameter of every child calibration group.");
        comments.put(COUNTS_FILE, "Counts CSV shared by flow-based calibration components; expected columns include linkId and count");
        comments.put(CORRECT_CAPACITIES, "Whether short-link capacities are corrected when the module is active (default: true)");
        comments.put(MIN_SPEED, "Minimum speed used for capacity correction in km/h (default: 1.0)");
        comments.put(MIN_CAPACITY, "Minimum capacity in veh/h/lane used to scale road categories (default: 500)");
        comments.put(MAX_CAPACITY, "Maximum capacity in veh/h/lane used to scale road categories (default: 1900)");
        comments.put(CATEGORY_FIVE_PROMOTION_LANE_THRESHOLD, "Category-five links above this lane count are treated as category four (default: 1.0)");
        comments.put(CATEGORY_FIVE_PROMOTION_SPEED_THRESHOLD, "Category-five links above this freespeed in km/h are treated as category four (default: 45)");
        comments.put(TOLLS_VALUE_OF_TIME, "Value of time used to convert road tolls into travel time  (default: 12.0)");
        return comments;
    }

    @StringGetter(ACTIVATE) public boolean isActivated() { return activate; }
    @StringSetter(ACTIVATE) public void setActivate(boolean value) { activate = value; }

    @StringGetter(CALIBRATE) public String getCalibrate() { return calibrate; }
    @StringSetter(CALIBRATE) public void setCalibrate(String value) { calibrate = value == null ? "" : value; }
    public List<String> getModulesToBeCalibratedAsList() { return Stream.of(calibrate.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList(); }
    public boolean isToBeCalibrated(String value) { return getModulesToBeCalibratedAsList().contains(value); }
    public void setCalibrate(boolean value) {
        calibrate = value ? String.join(",", PENALTY, FREESPEED, AGENT, SUBPOPULATIONS) : "";
    }

    @StringGetter(OBJECTIVE) public String getObjective() { return objective; }
    @StringSetter(OBJECTIVE)
    public void setObjective(String value) {
        objective = value == null ? "" : value;
        synchronizeExistingParameterSets();
    }
    public List<String> getAllObjectives() { return Stream.of(objective.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList(); }
    public boolean isOneOfObjectives(String value) { return getAllObjectives().contains(value); }
    @StringGetter(COUNTS_FILE) public String getCountsFile() { return countsFile; }
    @StringSetter(COUNTS_FILE) public void setCountsFile(String value) { countsFile = value; }
    public boolean hasCountsFile() { return !countsFile.isBlank() && countsFile.endsWith(".csv"); }
    @StringGetter(CORRECT_CAPACITIES) public boolean getCorrectCapacities() { return correctCapacities; }
    @StringSetter(CORRECT_CAPACITIES) public void setCorrectCapacities(boolean value) { correctCapacities = value; }
    @StringGetter(MIN_SPEED) public double getMinSpeed() { return minSpeed; }
    @StringSetter(MIN_SPEED) public void setMinSpeed(double value) { minSpeed = value; }
    @StringGetter(MIN_CAPACITY) public double getMinCapacity() { return minCapacity; }
    @StringSetter(MIN_CAPACITY) public void setMinCapacity(double value) { minCapacity = value; }
    @StringGetter(MAX_CAPACITY) public double getMaxCapacity() { return maxCapacity; }
    @StringSetter(MAX_CAPACITY) public void setMaxCapacity(double value) { maxCapacity = value; }
    @StringGetter(CATEGORY_FIVE_PROMOTION_LANE_THRESHOLD) public double getCategoryFivePromotionLaneThreshold() { return categoryFivePromotionLaneThreshold; }
    @StringSetter(CATEGORY_FIVE_PROMOTION_LANE_THRESHOLD) public void setCategoryFivePromotionLaneThreshold(double value) { categoryFivePromotionLaneThreshold = value; }
    @StringGetter(CATEGORY_FIVE_PROMOTION_SPEED_THRESHOLD) public double getCategoryFivePromotionSpeedThreshold() { return categoryFivePromotionSpeedThreshold; }
    @StringSetter(CATEGORY_FIVE_PROMOTION_SPEED_THRESHOLD) public void setCategoryFivePromotionSpeedThreshold(double value) { categoryFivePromotionSpeedThreshold = value; }
    @StringGetter(TOLLS_VALUE_OF_TIME) public double getTollsValueOfTime() { return tollsValueOfTime; }
    @StringSetter(TOLLS_VALUE_OF_TIME) public void setTollsValueOfTime(double value) { tollsValueOfTime = value; }

    public boolean isLinkPenaltyActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives(PENALTY) : getCostCalibrationConfigGroup().isActivated();
    }

    public boolean isFreeSpeedFactorActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives(FREESPEED) : getFreeSpeedCalibrationConfigGroup().isActivated();
    }

    public boolean isAgentAscsActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives(AGENT) : getAgentAscsCalibrationConfigGroup().isActivated();
    }

    public boolean isSubpopulationsActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives(SUBPOPULATIONS) : getSubpopulationsCalibrationConfigGroup().isActivated();
    }


    public boolean isLinkPenaltyCalibrationActivated() {
        return isToBeCalibrated(PENALTY);
    }

    public boolean isFreeSpeedFactorCalibrationActivated() {
        return isToBeCalibrated(FREESPEED);
    }

    public boolean isAgentAscsCalibrationActivated() {
        return isToBeCalibrated(AGENT);
    }

    public boolean isSubpopulationsCalibrationActivated() {
        return isToBeCalibrated(SUBPOPULATIONS);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        ConfigGroup parameterSet = switch (type) {
            case AgentAscsCalibrationConfigGroup.GROUP_NAME -> new AgentAscsCalibrationConfigGroup();
            case SubpopulationsCalibrationConfigGroup.GROUP_NAME -> new SubpopulationsCalibrationConfigGroup();
            case FreeSpeedCalibrationConfigGroup.GROUP_NAME -> new FreeSpeedCalibrationConfigGroup();
            case CostCalibrationConfigGroup.GROUP_NAME -> new CostCalibrationConfigGroup();
            default -> throw new IllegalArgumentException("Unknown network-calibration parameter-set type: " + type);
        };
        synchronizeActivation(parameterSet);
        return parameterSet;
    }

    public AgentAscsCalibrationConfigGroup getAgentAscsCalibrationConfigGroup() {
        return getOrCreateParameterSet(AgentAscsCalibrationConfigGroup.GROUP_NAME, AgentAscsCalibrationConfigGroup.class, AgentAscsCalibrationConfigGroup::new);
    }

    public SubpopulationsCalibrationConfigGroup getSubpopulationsCalibrationConfigGroup() {
        return getOrCreateParameterSet(SubpopulationsCalibrationConfigGroup.GROUP_NAME, SubpopulationsCalibrationConfigGroup.class, SubpopulationsCalibrationConfigGroup::new);
    }

    public FreeSpeedCalibrationConfigGroup getFreeSpeedCalibrationConfigGroup() {
        return getOrCreateParameterSet(FreeSpeedCalibrationConfigGroup.GROUP_NAME, FreeSpeedCalibrationConfigGroup.class, FreeSpeedCalibrationConfigGroup::new);
    }

    public CostCalibrationConfigGroup getCostCalibrationConfigGroup() {
        return getOrCreateParameterSet(CostCalibrationConfigGroup.GROUP_NAME, CostCalibrationConfigGroup.class, CostCalibrationConfigGroup::new);
    }

    private <T extends ConfigGroup> T getOrCreateParameterSet(String type, Class<T> configClass, java.util.function.Supplier<T> factory) {
        for (ConfigGroup parameterSet : getParameterSets(type)) {
            return configClass.cast(parameterSet);
        }
        T parameterSet = factory.get();
        synchronizeActivation(parameterSet);
        addParameterSet(parameterSet);
        return parameterSet;
    }

    private boolean hasObjectiveOverride() {
        return !getAllObjectives().isEmpty();
    }

    private void synchronizeExistingParameterSets() {
        if (!hasObjectiveOverride()) {
            return;
        }
        getParameterSets().values().stream()
                .flatMap(java.util.Collection::stream)
                .forEach(this::synchronizeActivation);
    }

    private void synchronizeActivation(ConfigGroup parameterSet) {
        if (!hasObjectiveOverride()) {
            return;
        }
        if (parameterSet instanceof CostCalibrationConfigGroup costConfig) {
            costConfig.setActivate(isOneOfObjectives(PENALTY));
        } else if (parameterSet instanceof FreeSpeedCalibrationConfigGroup freespeedConfig) {
            freespeedConfig.setActivate(isOneOfObjectives(FREESPEED));
        } else if (parameterSet instanceof AgentAscsCalibrationConfigGroup agentConfig) {
            agentConfig.setActivate(isOneOfObjectives(AGENT));
        } else if (parameterSet instanceof SubpopulationsCalibrationConfigGroup subpopulationsConfig) {
            subpopulationsConfig.setActivate(isOneOfObjectives(SUBPOPULATIONS));
        }
    }

    public void applyContext(Config config) {
        synchronizeExistingParameterSets();
        if (!countsFile.isBlank()) {
            URL url = ConfigGroup.getInputFileURL(config.getContext(), countsFile);
            if (url != null) countsFile = url.getPath();
        }
        getCostCalibrationConfigGroup().applyContext(config);
        getFreeSpeedCalibrationConfigGroup().applyContext(config);
    }

    public static NetworkCalibrationConfigGroup getOrCreate(Config config) {
        NetworkCalibrationConfigGroup group = (NetworkCalibrationConfigGroup) config.getModules().get(GROUP_NAME);
        if (group == null) {
            group = new NetworkCalibrationConfigGroup();
            config.addModule(group);
        }
        group.applyContext(config);
        return group;
    }
}
