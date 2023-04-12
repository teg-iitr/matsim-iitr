package playground.shivam.signals;

import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

import static playground.shivam.signals.RunSignals.*;

public class SignalUtils {
    public static double LANE_LENGTH;
    public static int LANE_CAPACITY;
    public static int NO_LANES = 1;

    public static int ITERATION=50;
    public static double FLOW_CAPFACTOR;
    public static double STORAGE_CAPFACTOR;

    public static double LINK_LENGTH=1000;
    public static int LINK_CAPACITY=1000;

    public static int CYCLE;

    public static int AGENTS_PER_LEFT_APPROACH;
    public static final int OFFSET_LEFT_APPROACH = 5;
    public static final int DROPPING_LEFT_APPROACH = 30;

    public static int AGENTS_PER_TOP_APPROACH;
    public static final int OFFSET_TOP_APPROACH = 35;
    public static final int DROPPING_TOP_APPROACH = 55;

    public static int AGENTS_PER_RIGHT_APPROACH;
    public static final int OFFSET_RIGHT_APPROACH = 60;
    public static final int DROPPING_RIGHT_APPROACH = 85;

    public static int AGENTS_PER_BOTTOM_APPROACH;
    public static final int OFFSET_BOTTOM_APPROACH = 90;
    public static final int DROPPING_BOTTOM_APPROACH = 120;

    public static Collection<String> MAIN_MODES = Arrays.asList("car","truck");
    public static void main(String[] args) throws IOException {
        // specify the path to your CSV file
        String csvPath = "/home/madhu/Desktop/IIT Roorkee/matsim-iitr/src/main/java/playground/shivam/signals/inputdata.csv";
         int index = 0;
        // create a CSVReader instance
        String line = "";
        String delimiter = ",";
        boolean isFirstRow = true;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            writer1 = new CSVWriter(new FileWriter("/home/madhu/Desktop/IIT Roorkee/matsim-iitr/src/main/java/playground/shivam/signals/output1.csv"));
            writer1.writeNext(new String[] {"avgDelay_1_2", "avgDelay_2_1", "avgDelay_2_3", "avgDelay_3_2", "avgDelay_3_4", "avgDelay_4_3", "avgDelay_4_5", "avgDelay_5_4", "avgDelay_6_7", "avgDelay_7_6", "avgDelay_7_3", "avgDelay_3_7", "avgDelay_3_8", "avgDelay_8_3", "avgDelay_8_9", "avgDelay_9_8","waitingTime_83_sl", "waitingTime_23_r", "waitingTime_73_sl", "waitingTime_83_r", "waitingTime_73_r", "waitingTime_43_r", "waitingTime_43_sl", "avgDelay_23_sl","waitingTimePerSystem", "LANE_LENGTH", "LANE_CAPACITY", "LINK_LENGTH", "LINK_CAPACITY", "CYCLE", "AGENTS_PER_LEFT_APPROACH", "AGENTS_PER_TOP_APPROACH", "AGENTS_PER_RIGHT_APPROACH", "AGENTS_PER_BOTTOM_APPROACH", "ITERATION", "STORAGE_CAPFACTOR", "FLOW_CAPFACTOR", "fixed Time Delay", "Adaptive Time Delay", "CompareResults"});

            writer2 = new CSVWriter(new FileWriter("/home/madhu/Desktop/IIT Roorkee/matsim-iitr/src/main/java/playground/shivam/signals/output2.csv"));
            writer2.writeNext(new String[] {"avgDelay_1_2", "avgDelay_2_1", "avgDelay_2_3", "avgDelay_3_2", "avgDelay_3_4", "avgDelay_4_3", "avgDelay_4_5", "avgDelay_5_4", "avgDelay_6_7", "avgDelay_7_6", "avgDelay_7_3", "avgDelay_3_7", "avgDelay_3_8", "avgDelay_8_3", "avgDelay_8_9", "avgDelay_9_8","waitingTime_83_sl", "waitingTime_23_r", "waitingTime_73_sl", "waitingTime_83_r", "waitingTime_73_r", "waitingTime_43_r", "waitingTime_43_sl", "avgDelay_23_sl","waitingTimePerSystem", "LANE_LENGTH", "LANE_CAPACITY", "LINK_LENGTH", "LINK_CAPACITY", "CYCLE,AGENTS_PER_LEFT_APPROACH", "AGENTS_PER_TOP_APPROACH", "AGENTS_PER_RIGHT_APPROACH", "AGENTS_PER_BOTTOM_APPROACH", "ITERATION", "STORAGE_CAPFACTOR", "FLOW_CAPFACTOR", "fixed Time Delay", "Adaptive Time Delay", "CompareResults"});

            while ((line = br.readLine()) != null) {
                if (isFirstRow) { // skip first row
                    isFirstRow = false;
                    continue;
                }
                String[] values = line.split(delimiter);
                // access the values in each column

                LANE_LENGTH = Double.parseDouble(values[0]);
                LANE_CAPACITY = Integer.parseInt(values[1]);

                LINK_LENGTH = Double.parseDouble(values[2]);
                LINK_CAPACITY = Integer.parseInt(values[3]);
                CYCLE = Integer.parseInt(values[4]);

                AGENTS_PER_LEFT_APPROACH = Integer.parseInt(values[5]);

                AGENTS_PER_TOP_APPROACH = Integer.parseInt(values[6]);

                AGENTS_PER_RIGHT_APPROACH = Integer.parseInt(values[7]);

                AGENTS_PER_BOTTOM_APPROACH = Integer.parseInt(values[8]);
                ITERATION = Integer.parseInt(values[9]);

                STORAGE_CAPFACTOR = Double.parseDouble(values[10]);
                FLOW_CAPFACTOR = Double.parseDouble(values[11]);

                int a;
                do {
                    System.out.println("0. Exit");
                    System.out.println("1. Fixed Time Signal");
                    System.out.println("2. Adaptive Signal ");
                    System.out.println("3. compareResults");

                    System.out.print("Enter your choice: ");

                    Scanner sc= new Scanner(System.in);
                    a = sc.nextInt();
                    System.out.println();
                    switch (a) {
                        case 1:
                            fixedTimeSignal();
                            break;
                        case 2:
                            adaptiveSignal();
                            break;
                        case 3:
                            compareResults();
                            break;
                        default:
                            System.out.println("You have exited");
                    }
                } while (a != 0);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
