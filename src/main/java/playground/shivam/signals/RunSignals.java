package playground.shivam.signals;

import com.opencsv.CSVWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.controller.laemmerFix.MixedTrafficLaemmerSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import playground.shivam.signals.runner.RunMatsim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool.totalWaitingTimePerSignal;
import static org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool.totalWaitingTimePerSystem;
import static playground.shivam.signals.SignalUtils.*;
import static playground.shivam.signals.config.CreateConfig.defineConfig;
import static playground.shivam.signals.runner.RunMatsim.avgDelayPerLink;
import static playground.shivam.signals.runner.RunMatsim.totalDelay;
import static playground.shivam.signals.scenarios.CreateScenarioFromConfig.defineScenario;

public class RunSignals {
    private static Controler controler;
    private static String outputDirectory;
    private static String signalController;
    private static Class<? extends SignalControllerFactory> signalControllerFactoryClassName;
    private static double fixedTimeDelay;
    private static double adaptiveTimeDelay;
    public static CSVWriter fixedWriter;
    public static CSVWriter adaptiveWriter;
    public static ArrayList<String> fixedList = new ArrayList<>();
    public static ArrayList<String> adaptiveList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int a;
        do {
            System.out.println("0. Exit");
            System.out.println("1. Fixed Time Mixed-Signal");
            System.out.println("2. Adaptive Mixed-Signal ");
            System.out.println("3. Compare Mixed-Traffic signal results");
            System.out.println("4. Fixed Time Homogenous Signal");
            System.out.println("5. Adaptive Homogenous Signal ");
            System.out.println("6. Compare Homogenous signal results");

            System.out.print("Enter your choice: ");

            Scanner sc = new Scanner(System.in);
            a = sc.nextInt();
            System.out.println();
            switch (a) {
                case 1:
                    fixedTimeSignal(false);
                    break;
                case 2:
                    adaptiveTimeSignal(false);
                    break;
                case 3:
                    compareMixedTrafficSignals(false);
                    break;
                case 4:
                    fixedTimeSignal(true);
                    break;
                case 5:
                    adaptiveTimeSignal(true);
                    break;
                case 6:
                    compareMixedTrafficSignals(true);
                default:
                    System.out.println("You have exited");
            }
        } while (a != 0);
    }

    static void fixedTimeSignal(boolean homogenous) throws IOException {
        outputDirectory = "output/fixedTimeSignal/";
        signalController = DefaultPlanbasedSignalSystemController.IDENTIFIER;
        signalControllerFactoryClassName = DefaultPlanbasedSignalSystemController.FixedTimeFactory.class;

        final Config config = defineConfig(outputDirectory, false);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController, false);

        controler = new Controler(scenario);

        if (homogenous)
            Signals.configure(controler);
        else
            MixedTrafficSignals.configure(controler);

        RunMatsim.run(false, controler, signalController, signalControllerFactoryClassName, outputDirectory);

        fixedList.add(String.valueOf(totalDelay));

        for (Link link : controler.getScenario().getNetwork().getLinks().values()) {
            String avgDelayPerLinkTemp = "avgDelay_Fixed_" + link.getId() + "_" + avgDelayPerLink.get(link.getId());
            System.out.println(avgDelayPerLinkTemp);
            fixedList.add(String.valueOf(avgDelayPerLink.get(link.getId())));
        }

        for (Map.Entry<Id<SignalSystem>, Double> entry : totalWaitingTimePerSystem.entrySet()) {
            String totalWaitingTimePerSystemTemp = "waitingTimePerSystem_Fixed_" + entry.getKey() + "_" + entry.getValue();
            System.out.println(totalWaitingTimePerSystemTemp);
            fixedList.add(String.valueOf(entry.getValue()));
        }

        for (Map.Entry<Id<Signal>, Double> entry : totalWaitingTimePerSignal.entrySet()) {
            String totalWaitingTimePerSignalTemp = "waitingTime_Fixed_" + entry.getKey() + "_" + entry.getValue();
            System.out.println(totalWaitingTimePerSignalTemp);
            fixedList.add(String.valueOf(entry.getValue()));
        }

    }

    static void adaptiveTimeSignal(boolean homogenous) throws IOException {
        outputDirectory = "output/adaptiveTimeSignal/";
        if (homogenous) {
            signalController = LaemmerSignalController.IDENTIFIER;
            signalControllerFactoryClassName = LaemmerSignalController.LaemmerFactory.class;
        }
        else {
            signalController = MixedTrafficLaemmerSignalController.IDENTIFIER;
            signalControllerFactoryClassName = MixedTrafficLaemmerSignalController.LaemmerFactory.class;
        }
        final Config config = defineConfig(outputDirectory, true);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController, true);

        controler = new Controler(scenario);

        if (homogenous)
            Signals.configure(controler);
        else
            MixedTrafficSignals.configure(controler);

        RunMatsim.run(false, controler, signalController, signalControllerFactoryClassName, outputDirectory);

        adaptiveList.add(String.valueOf(totalDelay));

        for (Link link : controler.getScenario().getNetwork().getLinks().values()) {
            String avgDelayPerLinkTemp = "avgDelay_Adaptive_" + link.getId() + "_" + avgDelayPerLink.get(link.getId());
            System.out.println(avgDelayPerLinkTemp);
            adaptiveList.add(String.valueOf(avgDelayPerLink.get(link.getId())));
        }

        for (Map.Entry<Id<SignalSystem>, Double> entry : totalWaitingTimePerSystem.entrySet()) {
            String totalWaitingTimePerSystemTemp = "waitingTimePerSystem_Adaptive_" + entry.getKey() + "_" + entry.getValue();
            System.out.println(totalWaitingTimePerSystemTemp);
            adaptiveList.add(String.valueOf(entry.getValue()));
        }

        for (Map.Entry<Id<Signal>, Double> entry : totalWaitingTimePerSignal.entrySet()) {
            String totalWaitingTimePerSignalTemp = "waitingTime_Adaptive_" + entry.getKey() + "_" + entry.getValue();
            System.out.println(totalWaitingTimePerSignalTemp);
            adaptiveList.add(String.valueOf(entry.getValue()));
        }


    }

    public static void compareMixedTrafficSignals(boolean homogenous) throws IOException {


        // specify the path to your CSV file
        List<String> csvPaths = Arrays.asList("input/low_demand/signal_input.csv", "input/high_demand/signal_input.csv");
        int index = 0;
        // create a CSVReader instance
        String line = "";
        String delimiter = ",";
        boolean isFirstRowRead = true;
        boolean isFirstRowWrite = true;
        for (String csvPath: csvPaths) {
            try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
                while ((line = br.readLine()) != null) {

                    String[] values = line.split(delimiter);

                    if (isFirstRowRead) { // skip first row
                        isFirstRowRead = false;
                        continue;
                    }
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
                    STORAGE_CAPACITY_FACTOR = Double.parseDouble(values[10]);
                    FLOW_CAPACITY_FACTOR = Double.parseDouble(values[11]);

                    addToBothLists(fixedList, adaptiveList, String.valueOf(LANE_LENGTH));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(LANE_CAPACITY));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(LINK_LENGTH));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(LINK_CAPACITY));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(CYCLE));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(AGENTS_PER_LEFT_APPROACH));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(AGENTS_PER_TOP_APPROACH));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(AGENTS_PER_RIGHT_APPROACH));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(AGENTS_PER_BOTTOM_APPROACH));
                    addToBothLists(fixedList, adaptiveList, String.valueOf(ITERATION));
                    addToBothLists(fixedList, adaptiveList, Double.toString(STORAGE_CAPACITY_FACTOR));
                    addToBothLists(fixedList, adaptiveList, Double.toString(FLOW_CAPACITY_FACTOR));

                    if (homogenous) {
                        fixedTimeSignal(true);
                        adaptiveTimeSignal(true);
                    } else {
                        fixedTimeSignal(false);
                        adaptiveTimeSignal(false);
                    }

                    if (isFirstRowWrite) {
                        ArrayList<String> columnNameList = new ArrayList<>();

                        columnNameList.add("lane_len");
                        columnNameList.add("lane_cap");
                        columnNameList.add("link_len");
                        columnNameList.add("link_cap");
                        columnNameList.add("cycle");
                        columnNameList.add("west_agents");
                        columnNameList.add("north_agents");
                        columnNameList.add("east_agents");
                        columnNameList.add("south_agents");
                        columnNameList.add("iter");
                        columnNameList.add("storage_cap");
                        columnNameList.add("flow_cap");

                        columnNameList.add("total_delay");

                        for (Link link : controler.getScenario().getNetwork().getLinks().values()) {
                            columnNameList.add("avg_delay_" + link.getId());
                        }

                        columnNameList.add("total_waiting");

                        for (Map.Entry<Id<Signal>, Double> entry : totalWaitingTimePerSignal.entrySet()) {
                            columnNameList.add("total_waiting_" + entry.getKey());
                        }

                        if (homogenous) {
                            fixedWriter = new CSVWriter(new FileWriter("output/homogenousSignals/" + csvPath.split("/")[1] + "/fixedResult_cycle.csv"));

                            adaptiveWriter = new CSVWriter(new FileWriter("output/homogenousSignals/" + csvPath.split("/")[1] +  "/adaptiveResult_cycle.csv"));
                        }
                        else {
                            fixedWriter = new CSVWriter(new FileWriter("output/mixedTrafficSignals/" + csvPath.split("/")[1] + "/fixedResult_cycle.csv"));

                            adaptiveWriter = new CSVWriter(new FileWriter("output/mixedTrafficSignals/" + csvPath.split("/")[1]+ "/adaptiveResult_cycle.csv"));

                        }
                        fixedWriter.writeNext(columnNameList.toArray(new String[0]));
                        adaptiveWriter.writeNext(columnNameList.toArray(new String[0]));

                        isFirstRowWrite = false;
                    }

                    fixedWriter.writeNext(fixedList.toArray(new String[0]));
                    fixedWriter.flush();
                    fixedList.clear();

                    adaptiveWriter.writeNext(adaptiveList.toArray(new String[0]));
                    adaptiveWriter.flush();
                    adaptiveList.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fixedWriter.close();
        adaptiveWriter.close();
    }

    public static void addToBothLists(ArrayList<String> output1, ArrayList<String> output2, String value) {
        output1.add(value);
        output2.add(value);
    }
}
