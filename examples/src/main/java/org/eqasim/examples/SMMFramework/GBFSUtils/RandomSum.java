package org.eqasim.examples.SMMFramework.GBFSUtils;

import java.util.ArrayList;
import java.util.Random;

public class RandomSum {
// Code divide  a number in set number of parts  extracted from
// https://stackoverflow.com/questions/22380890/generate-n-random-numbers-whose-sum-is-m-and-all-numbers-should-be-greater-than
// Constructed by user@Assylias

    public static  ArrayList<Integer> n_random(int targetSum, int numberOfDraws) {
        Random r = new Random();
        ArrayList<Integer> load = new ArrayList<Integer>();
        int temp = 0;
        int sum = 0;
        if(targetSum==0){
            for (int i = 1; i <= numberOfDraws; i++) {
                load.add(0);
            }
        }else{
            for (int i = 1; i <= numberOfDraws; i++) {
                if (!(i == numberOfDraws)) {
                    temp = r.nextInt(targetSum - sum);
                    System.out.println("Temp " + (i) + "    " + temp);
                    load.add(temp);
                    sum += temp;

                } else {
                    int last = (targetSum - sum);
                    load.add(last);
                    sum += last;
                }
            }

        }

        return load;
    }

}
