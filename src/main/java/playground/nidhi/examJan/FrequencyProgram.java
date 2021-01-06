package playground.nidhi.examJan;

import java.util.Random;

public class FrequencyProgram {
    public static void main(String[] args) {
        Random rand = new Random();
        int[] arrNumber = new int[10000];
        int min = 1;
        int max = 100;

        for (int i = 0; i < 10000; i++) {
            arrNumber[i] = rand.nextInt(max) + min;
        }

        int[] fre = new int[arrNumber.length];
        for (int i = 0; i < arrNumber.length; i++) {
            int count = 1;
            for (int j = i + 1; j < arrNumber.length; j++) {
                if (arrNumber[i] == arrNumber[j]) {
                    count++;
                }
                fre[i]=count;
            }
        }

        System.out.println(fre);
    }

}



