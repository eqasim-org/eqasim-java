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

    private boolean activate;
    private boolean calibrate = true;
    private String objective = "";
    private String countsFile = "";
    private boolean correctCapacities = true;
    private double minSpeed = 1.0;
    private double minCapacity = 500.0;
    private double maxCapacity = 2_000.0;
    private double categoryFivePromotionLaneThreshold = 1.0;
    private double categoryFivePromotionSpeedThreshold = 45.0;

    public NetworkCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE, "Whether the network-calibration module is active (default: false)");
        comments.put(CALIBRATE, "Whether active components update parameters; when false, configured initial values remain fixed (default: true)");
        comments.put(OBJECTIVE, "Optional comma-separated selector: penalty, freespeed, agent, or subpopulations. When non-empty, it overrides the activate parameter of every child calibration group.");
        comments.put(COUNTS_FILE, "Counts CSV shared by flow-based calibration components; expected columns include linkId and count");
        comments.put(CORRECT_CAPACITIES, "Whether short-link capacities are corrected when the module is active (default: true)");
        comments.put(MIN_SPEED, "Minimum speed used for capacity correction in km/h (default: 1.0)");
        comments.put(MIN_CAPACITY, "Minimum capacity in veh/h/lane used to scale road categories (default: 500)");
        comments.put(MAX_CAPACITY, "Maximum capacity in veh/h/lane used to scale road categories (default: 1900)");
        comments.put(CATEGORY_FIVE_PROMOTION_LANE_THRESHOLD, "Category-five links above this lane count are treated as category four (default: 1.0)");
        comments.put(CATEGORY_FIVE_PROMOTION_SPEED_THRESHOLD, "Category-five links above this freespeed in km/h are treated as category four (default: 45)");
        return comments;
    }

    @StringGetter(ACTIVATE) public boolean isActivated() { return activate; }
    @StringSetter(ACTIVATE) public void setActivate(boolean value) { activate = value; }
    @StringGetter(CALIBRATE) public boolean getCalibrate() { return calibrate; }
    @StringSetter(CALIBRATE) public void setCalibrate(boolean value) { calibrate = value; }
    public boolean isCalibrationEnabled() { return calibrate; }
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

    public boolean isCostCalibrationActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives("penalty") : getCostCalibrationConfigGroup().isActivated();
    }

    public boolean isFreeSpeedCalibrationActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives("freespeed") : getFreeSpeedCalibrationConfigGroup().isActivated();
    }

    public boolean isAgentAscsCalibrationActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives("agent") : getAgentAscsCalibrationConfigGroup().isActivated();
    }

    public boolean isSubpopulationsCalibrationActivated() {
        return hasObjectiveOverride() ? isOneOfObjectives("subpopulations") : getSubpopulationsCalibrationConfigGroup().isActivated();
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
            costConfig.setActivate(isOneOfObjectives("penalty"));
        } else if (parameterSet instanceof FreeSpeedCalibrationConfigGroup freespeedConfig) {
            freespeedConfig.setActivate(isOneOfObjectives("freespeed"));
        } else if (parameterSet instanceof AgentAscsCalibrationConfigGroup agentConfig) {
            agentConfig.setActivate(isOneOfObjectives("agent"));
        } else if (parameterSet instanceof SubpopulationsCalibrationConfigGroup subpopulationsConfig) {
            subpopulationsConfig.setActivate(isOneOfObjectives("subpopulations"));
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
