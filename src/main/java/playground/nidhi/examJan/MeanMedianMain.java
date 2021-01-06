package playground.nidhi.examJan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MeanMedianMain {
    public static void main(String[] args) {
        MeanMedian mn = new MeanMedian();
        mn.getHouse2Membr();

        List<Double> income1 = mn.getIncome1();
        List<Double> income2 = mn.getIncome2();
        List<Double> income3 = mn.getIncome3();


        //For household 1

        int sum1 = 0;
        for (double i : income1) {
            sum1 += i;
        }
        if (income1.isEmpty()) {
            System.out.println("Empty list 1!");
        } else {
            System.out.println("Mean of income of Household 1 = " + sum1 / (float) income1.size());
        }

        int middle1 = income1.size()/2;
        if (income1.size() % 2 == 1) {
            System.out.println("Median of income of Household 1 = "+ income1.get(middle1));
        } else {
            double x= (income1.get(middle1-1) + income1.get(middle1)) / 2.0;
            System.out.println( "Mean of income of Household 1 = " + x );
        }

        System.out.println();
        //for household 2
        int sum2 = 0;
        for (double i : income2) {
            sum2 += i;
        }
        if (income2.isEmpty()) {
            System.out.println("Empty list 2!");
        } else {
            System.out.println("Mean of income of Household 2 = " + sum2 / (float) income2.size());
        }
            int middle2 = income2.size()/2;
            if (income2.size() % 2 == 1) {
                System.out.println("Median of income of Household 2 = "+ income2.get(middle2));
            } else {
                double x= (income2.get(middle2-1) + income2.get(middle2)) / 2.0;
                System.out.println( "Mean of income of Household 2 = " + x );
            }

        System.out.println();
        //for household 3
            int sum3 = 0;
            for (double i : income3) {
                sum3 += i;
            }
            if (income3.isEmpty()) {
                System.out.println("Empty list 3!");
            } else {
                System.out.println("Mean of income of Household 3 = " + sum3 / (float) income3.size());

                }

        int middle3 = income3.size()/2;
        if (income3.size() % 2 == 1) {
            System.out.println("Median of income of Household 3 = "+ income3.get(middle3));
        } else {
            double x= (income3.get(middle3-1) + income3.get(middle3)) / 2.0;
            System.out.println( "Mean of income of Household 3 = " + x );
        }

        System.out.println();
        //for all household
       List<Double> allIncome= new ArrayList<>();
        allIncome.addAll(income1);
        allIncome.addAll(income2);
        allIncome.addAll(income3);

        int sumAll = 0;
        for (double i : allIncome) {
            sumAll += i;
        }
        if (allIncome.isEmpty()) {
            System.out.println("Empty list !");
        } else {
            System.out.println("Mean of income of all Household  = " + sumAll / (float) allIncome.size());

        }

        int middleAll = allIncome.size()/2;
        if (allIncome.size() % 2 == 1) {
            System.out.println("Median of income of all Household  = "+ allIncome.get(middleAll));
        } else {
            double x= (allIncome.get(middleAll-1) + allIncome.get(middleAll)) / 2.0;
            System.out.println( "Mean of income of all Household = " + x );
        }





            //end
        }
}
