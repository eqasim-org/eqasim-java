package org.eqasim.core.components.calibration;

public interface Optimizer {
    void run(int iteration, String newParametersFilePath, String lastParametersFilePath, String variablesIterationPath);
}