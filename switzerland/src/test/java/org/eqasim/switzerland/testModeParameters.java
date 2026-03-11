package org.eqasim.switzerland;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;

import java.io.File;
import java.io.IOException;


public class testModeParameters {

    public static void main(String[] args){
        // ================== TEST SwissCmdpModeParameters Reads and Writes ==================
        // STEP 1: Create an instance of SwissCmdpModeParameters and save the parameters to yml file
        SwissCmdpModeParameters params = SwissCmdpModeParameters.buildDefault();
        // do some changes to params
        double cst = 3.21365;
        params.car.alpha_u = cst;
        params.car.betaTravelTime_u_min = cst;
        params.swissCanton.car.put("Bern", cst);
        params.swissCanton.pt.put("Aargau", cst);
        params.swissCanton.bike.put("Uri", cst);

        String parameterFilePath = "switzerland\\src\\test\\java\\org\\eqasim\\switzerland\\params.yml";
        try {
            params.saveToYamlFile(parameterFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // STEP 2: Make some changes and then load the parameters from the yml file
        params.swissCanton.car.put("Bern", 0.23);
        params.swissCanton.pt.put("Aargau", 1.01);
        params.swissCanton.bike.put("Uri", 2.79);

        File file = new File(parameterFilePath);
        ParameterDefinition.applyFile(file, params);

        // STEP 3: Verify that the parameters have been updated
        if (params.swissCanton.car.get("Bern") == cst &&
            params.swissCanton.pt.get("Aargau") == cst &&
            params.swissCanton.bike.get("Uri") == cst) {
            System.out.println("Parameters successfully updated from YAML file.");
        } else {
            System.out.println("Failed to update parameters from YAML file.");
        }

//        // ================== TEST SwissCmdpModeParameters Access to attributes ==================
//        SwissCmdpModeParameters params2 = SwissCmdpModeParameters.buildDefault();
//        ModeParameters params3 = SwissCmdpModeParameters.buildDefault();
//
//        ParameterDefinition.applyFile(file, params2);
//        ParameterDefinition.applyFile(file, params3);
//
//        System.out.println("Parameters successfully updated from YAML file.");
//        System.out.println("The value of car.alpha_u in SwissCmdpModeParameters is: " + params2.car.alpha_u);
//        System.out.println("The value of car.alpha_u in ModeParameters is: " + params3.car.alpha_u);
//
//        System.out.println("The value of car.betaTravelTime_u_min in SwissCmdpModeParameters is: " + params2.car.betaTravelTime_u_min);
//        System.out.println("The value of car.betaTravelTime_u_min in ModeParameters is: " + params3.car.betaTravelTime_u_min);

    }
}

