package org.eqasim.examples.corsica_drt.GBFSUtils;

import java.util.ArrayList;
import java.util.Random;

public class RandomSum {
// Code divide  a number in set number of parts  extracted from
// https://stackoverflow.com/questions/22380890/generate-n-random-numbers-whose-sum-is-m-and-all-numbers-should-be-greater-than
// Constructed by user@Assylias
    public static void main(String[] args) {
        ArrayList<Integer>numbers=n_random(0,2);
        String x="d";
    }
//    public static List<Integer> n_random(int targetSum, int numberOfDraws) {
//        Random r = new Random();
//        List<Integer> load = new ArrayList<>();
//
//        //random numbers
//        int sum = 0;
//        for (int i = 0; i < numberOfDraws; i++) {
//            int next = r.nextInt(targetSum) + 1;
//            load.add(next);
//            sum += next;
//        }
//
//        //scale to the desired target sum
//        double scale = 1d * targetSum / sum;
//        sum = 0;
//        for (int i = 0; i < numberOfDraws; i++) {
//            load.set(i, (int) (load.get(i) * scale));
//            sum += load.get(i);
//        }
//
//        //take rounding issues into account
//        while(sum++ < targetSum) {
//            int i = r.nextInt(numberOfDraws);
//            load.set(i, load.get(i) + 1);
//        }
//
//        System.out.println("Random arraylist " + load);
//        System.out.println("Sum is "+ (sum - 1));
//        return load;
//    }

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
