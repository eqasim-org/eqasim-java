package org.eqasim.core.components.calibration;

import org.eqasim.core.components.fast_calibration.alphaCalibratorConfig;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.Map;

public class CalibrationConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:calibration";

    private boolean runCalibration = true;
    private int startIteration = 2;

    private String repoUrl = "https://github.com/Dib-AEK/EqasimParametersCalibration.git";
    private String repoBranch = "polars";
    private String repoCommit = "1de98c60cf0ca5c3f48d2342ea776f54cad05ffd";

    private String optimizerPath = "C:/Users/dabdelkader/Desktop/work/codes/zurichScenario/optimizer";
    private String optimizer = "cmaes";
    private String metric = "mse";
    private String selector = "MultinomialLogit";
    private String bounds = "car.alpha_u:3.0," +
                            "walk.alpha_u:3.0," +
                            "bike.alpha_u:3.0," +
                            "car.betaTravelTime_u_min:0.5," +
                            "walk.betaTravelTime_u_min:0.5," +
                            "bike.betaTravelTime_u_min:0.5," +
                            "pt.betaInVehicleTime_u_min:0.5";

    private int maxEval = 3000;
    private int populationSample = 1000000;
    private String momentum = "ema";

    private double betaMomentum = 0.8;
    private String eqasimCachePath = "/home/dabdelkader/Euler/ch-zh-synpop/cache10p100";
    private String objectives = "global,distance,mode_distance";
    private String distanceBins = "";

    private String pythonPath = "python3";

    public static final String RUN_CALIBRATION = "runCalibration";
    public static final String START_ITERATION = "startIteration";
    public static final String OPTIMIZER = "optimizer";
    public static final String OPTIMIZER_PATH = "optimizerPath";
    public static final String METRIC = "metric";
    public static final String SELECTOR = "selector";
    public static final String BOUNDS = "bounds";
    public static final String MAX_EVAL = "maxEval";
    public static final String MOMENTUM = "momentum";
    public static final String POPULATION_SAMPLE = "populationSample";
    public static final String BETA_MOMENTUM = "betaMomentum";
    public static final String EQASIM_CACHE_PATH = "eqasimCachePath";
    public static final String OBJECTIVES = "objectives";
    public static final String DISTANCE_BINS = "distanceBins";

    public static final String PYTHON_PATH = "pythonPath";
    public static final String REPO_URL = "repoUrl";
    public static final String REPO_BRANCH = "repoBranch";
    public static final String REPO_COMMIT = "repoCommit";

    public CalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = new HashMap<>();
        comments.put(RUN_CALIBRATION,
                "If True, the parameters of the discrete mode will be calibrated.");
        comments.put(START_ITERATION,
                "The iteration from which the simulation starts (better if it starts after mode shares start converging).");
        comments.put(OPTIMIZER,
                "Choose one of the optimizers to be applied. optimizers : ga,pso,random,bayesian,tpe,cmaes,spsa,adam,\n" +
                        "                                 'Nelder-Mead','Powell', 'CG',  'BFGS', 'Newton-CG','L-BFGS-B',\n" +
                        "                                 'TNC', 'COBYLA','COBYQA','SLSQP','trust-constr','dogleg','trust-ncg',\n" +
                        "                                 'trust-exact','trust-krylov'");
        comments.put(OPTIMIZER_PATH,
                "The path to the python file that runs optimization.");
        comments.put(METRIC,
                "The loss function to minimise. Metrics: mse, mae, cosine, kl, js, hellinger, tv.");
        comments.put(MAX_EVAL,
                "Maximum number of evaluation of the loss function in the optimizer within each iteration.");
        comments.put(MOMENTUM,
                "The momentum of the loss function in the optimizer within each iteration, choose between 'ema' and 'adam'.");
        comments.put(POPULATION_SAMPLE,
                "The number of agents used during the optimization. This improves the efficiency of the optimization.");

        comments.put(BETA_MOMENTUM,
                "The minimum beta of the momentum (increased through iterations).");
        comments.put(EQASIM_CACHE_PATH,
                "The path to the cache of the synthetic population, where mode shares can be obtained.");
        comments.put(OBJECTIVES,
                "The objectives to be used in the calibration, separated by commas. " +
                        "Available objectives: global,distance,canton,age,income,sp_region,mode_distance,mode_income,mode_age,mode_canton,vot");
        comments.put(BOUNDS,
                "The optimization bounds for the parameters to be calibrated, in the format: " +
                "parameter1:delta, parameter2:delta, ...\nThe min bound is x-delta and the max bound is x+delta for each parameter.");
        comments.put(PYTHON_PATH,
                "The path to the Python executable to run the calibration script.");
        comments.put(DISTANCE_BINS,
                "The distance bins to be used in the calibration, separated by commas. " +
                        "This is used to calculate the distance-based objectives.");
        return comments;
    }

    // ==================== GETTERS AND SETTERS ====================

    @StringSetter(RUN_CALIBRATION)
    public void setRunCalibration(boolean runCalibration) {
        this.runCalibration = runCalibration;
    }

    @StringGetter(RUN_CALIBRATION)
    public boolean getRunCalibration() {
        return runCalibration;
    }

    public boolean isActivated() {
        return runCalibration;
    }

    @StringSetter(DISTANCE_BINS)
    public void setDistanceBins(String distanceBins) {
        this.distanceBins = distanceBins;
    }
    @StringGetter(DISTANCE_BINS)
    public String getDistanceBins() {
        return distanceBins;
    }

    @StringGetter(PYTHON_PATH)
    public String getPythonPath() {
        return pythonPath;
    }
    @StringSetter(PYTHON_PATH)
    public void setPythonPath(String pythonPath) {
        this.pythonPath = pythonPath;
    }

    @StringSetter(REPO_URL)
    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
    @StringGetter(REPO_URL)
    public String getRepoUrl() {
        return repoUrl;
    }
    @StringSetter(REPO_BRANCH)
    public void setRepoBranch(String repoBranch) {
        this.repoBranch = repoBranch;
    }
    @StringGetter(REPO_BRANCH)
    public String getRepoBranch() {
        return repoBranch;
    }
    @StringSetter(REPO_COMMIT)
    public void setRepoCommit(String repoCommit) {
        this.repoCommit = repoCommit;
    }
    @StringGetter(REPO_COMMIT)
    public String getRepoCommit() {
        return repoCommit;
    }

    @StringSetter(START_ITERATION)
    public void setStartIteration(int startIteration) {
        this.startIteration = startIteration;
    }

    @StringGetter(START_ITERATION)
    public int getStartIteration() {
        return startIteration;
    }

    @StringSetter(OPTIMIZER)
    public void setOptimizer(String optimizer) {
        this.optimizer = optimizer;
    }

    @StringGetter(OPTIMIZER)
    public String getOptimizer() {
        return optimizer;
    }

    @StringGetter(OBJECTIVES)
    public String getObjectives() {
        return objectives;
    }
    @StringSetter(OBJECTIVES)
    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    @StringSetter(OPTIMIZER_PATH)
    public void setOptimizerPath(String optimizerPath) {
        this.optimizerPath = optimizerPath;
    }

    @StringGetter(OPTIMIZER_PATH)
    public String getOptimizerPath() {
        return optimizerPath;
    }

    @StringSetter(METRIC)
    public void setMetric(String metric) {
        this.metric = metric;
    }

    @StringGetter(METRIC)
    public String getMetric() {
        return metric;
    }

    @StringSetter(SELECTOR)
    public void setSelector(String selector) {
        this.selector = selector;
    }

    @StringGetter(SELECTOR)
    public String getSelector() {
        return selector;
    }

    @StringSetter(BOUNDS)
    public void setBounds(String bounds) {
        this.bounds = bounds;
    }

    @StringGetter(BOUNDS)
    public String getBounds() {
        return bounds;
    }

    @StringSetter(MAX_EVAL)
    public void setMaxEval(int maxEval) {
        this.maxEval = maxEval;
    }

    @StringGetter(MAX_EVAL)
    public int getMaxEval() {
        return maxEval;
    }

    @StringSetter(MOMENTUM)
    public void setMomentum(String momentum) {
        this.momentum = momentum;
    }

    @StringGetter(MOMENTUM)
    public String getMomentum() {
        return momentum;
    }

    @StringSetter(POPULATION_SAMPLE)
    public void setPopulationSample(int populationSample) {
        this.populationSample = populationSample;
    }

    @StringGetter(POPULATION_SAMPLE)
    public int getPopulationSample() {
        return populationSample;
    }

    @StringSetter(BETA_MOMENTUM)
    public void setBetaMomentum(double betaMomentum) {
        this.betaMomentum = betaMomentum;
    }

    @StringGetter(BETA_MOMENTUM)
    public double getBetaMomentum() {
        return betaMomentum;
    }

    @StringSetter(EQASIM_CACHE_PATH)
    public void setEqasimCachePath(String eqasimCachePath) {
        this.eqasimCachePath = eqasimCachePath;
    }

    @StringGetter(EQASIM_CACHE_PATH)
    public String getEqasimCachePath() {
        return eqasimCachePath;
    }

    public static CalibrationConfigGroup getOrCreate(Config config) {
        CalibrationConfigGroup group = (CalibrationConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new CalibrationConfigGroup();
            config.addModule(group);
        }

        return group;
    }
}
