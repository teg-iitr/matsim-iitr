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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import static org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool.totalWaitingTimePerSignal;
import static org.matsim.contrib.signals.analysis.TtQueueLengthAnalysisTool.totalWaitingTimePerSystem;
import static playground.amit.munich.controlerListener.MyEmissionCongestionMoneyEventControlerListener.log;
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
    private static double adativeTimeDelay;
    public static CSVWriter writer1;
    public static CSVWriter writer2;
    public static ArrayList<String> output1 = new ArrayList<>();
    public static ArrayList<String> output2 = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        Scanner sc= new Scanner(System.in);
        int a;

    }

    static void fixedTimeSignal() throws IOException {
        outputDirectory = "output/RunFixedMixedTrafficSignalSimpleIntersection/";
        signalController = DefaultPlanbasedSignalSystemController.IDENTIFIER;
        signalControllerFactoryClassName = DefaultPlanbasedSignalSystemController.FixedTimeFactory.class;

        final Config config = defineConfig(outputDirectory, false);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController, false);

        controler = new Controler(scenario);

        MixedTrafficSignals.configure(controler);

        RunMatsim.run(false, controler, signalController, signalControllerFactoryClassName, outputDirectory);

        fixedTimeDelay = totalDelay;

        for (Link link: controler.getScenario().getNetwork().getLinks().values()) {
            String avgDelayPerLinkTemp = "avgDelay_Fixed_"+link.getId()+"_"+avgDelayPerLink.get(link.getId());
            System.out.println(avgDelayPerLinkTemp);
            output1.add(String.valueOf(avgDelayPerLink.get(link.getId())));
        }

        for (Map.Entry<Id<Signal>, Double> entry : totalWaitingTimePerSignal.entrySet()) {
            String totalWaitingTimePerSignalTemp = "waitingTime_Fixed_"+entry.getKey()+"_"+entry.getValue();
            System.out.println(totalWaitingTimePerSignalTemp);
            output1.add(String.valueOf(entry.getValue()));
        }

        for (Map.Entry<Id<SignalSystem>, Double> entry : totalWaitingTimePerSystem.entrySet()) {
            String totalWaitingTimePerSystemTemp = "waitingTimePerSystem_Fixed_"+entry.getKey()+"_"+entry.getValue();
            System.out.println(totalWaitingTimePerSystemTemp);
            output1.add(String.valueOf(entry.getValue()));
        }

//        System.out.println(LANE_LENGTH +", "+LANE_CAPACITY+", "+LINK_LENGTH+", "+ LINK_CAPACITY+", "+CYCLE + ", "+AGENTS_PER_LEFT_APPROACH+", "+
//                AGENTS_PER_TOP_APPROACH+", "+AGENTS_PER_RIGHT_APPROACH+", "+
//                AGENTS_PER_BOTTOM_APPROACH+", "+ITERATION+", "+STORAGE_CAPFACTOR+", "+FLOW_CAPFACTOR);
    }

    static void adaptiveSignal() throws IOException {
        outputDirectory = "output/RunAdaptiveSignalSimpleNetwork/";
        signalController = MixedTrafficLaemmerSignalController.IDENTIFIER;
        signalControllerFactoryClassName = MixedTrafficLaemmerSignalController.LaemmerFactory.class;

        final Config config = defineConfig(outputDirectory, true);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController, true);

        controler = new Controler(scenario);

        MixedTrafficSignals.configure(controler);

        RunMatsim.run(false, controler, signalController, signalControllerFactoryClassName, outputDirectory);

        adativeTimeDelay = totalDelay;

        for (Link link: controler.getScenario().getNetwork().getLinks().values()) {
            String avgDelayPerLinkTemp = "avgDelay_Adaptive_"+link.getId()+"_"+avgDelayPerLink.get(link.getId());
            System.out.println(avgDelayPerLinkTemp);
            output2.add(String.valueOf(avgDelayPerLink.get(link.getId())));
        }

        for (Map.Entry<Id<Signal>, Double> entry : totalWaitingTimePerSignal.entrySet()) {
            String totalWaitingTimePerSignalTemp = "waitingTime_Adaptive_"+entry.getKey()+"_"+entry.getValue();
            System.out.println(totalWaitingTimePerSignalTemp);
            output2.add(String.valueOf(entry.getValue()));
        }

        for (Map.Entry<Id<SignalSystem>, Double> entry : totalWaitingTimePerSystem.entrySet()) {
            String totalWaitingTimePerSystemTemp = "waitingTimePerSystem_Adaptive_"+entry.getKey()+"_"+entry.getValue();
            System.out.println(totalWaitingTimePerSystemTemp);
            output2.add(String.valueOf(entry.getValue()));
        }
//        System.out.println(LANE_LENGTH +", "+LANE_CAPACITY+", "+LINK_LENGTH+", "+ LINK_CAPACITY+", "+CYCLE + ", "+AGENTS_PER_LEFT_APPROACH+", "+
//                AGENTS_PER_TOP_APPROACH+", "+AGENTS_PER_RIGHT_APPROACH+", "+
//                AGENTS_PER_BOTTOM_APPROACH+", "+ITERATION+", "+STORAGE_CAPFACTOR+", "+FLOW_CAPFACTOR);
    }
    public static void compareResults() throws IOException {
        String compareResults = "";
        if (fixedTimeDelay > adativeTimeDelay){
            System.out.println(fixedTimeDelay);
            System.out.println("yes");
            compareResults = "yes";
        }else{
            System.out.println(adativeTimeDelay);
            System.out.println("no");
            compareResults = "no";
        }

        addToBothLists(output1, output2, String.valueOf(LANE_LENGTH));
        addToBothLists(output1, output2, String.valueOf(LANE_CAPACITY));
        addToBothLists(output1, output2, String.valueOf(LINK_LENGTH));
        addToBothLists(output1, output2, String.valueOf(LINK_CAPACITY));
        addToBothLists(output1, output2, String.valueOf(CYCLE));
        addToBothLists(output1, output2, String.valueOf(AGENTS_PER_LEFT_APPROACH));
        addToBothLists(output1, output2, String.valueOf(AGENTS_PER_TOP_APPROACH));
        addToBothLists(output1, output2, String.valueOf(AGENTS_PER_RIGHT_APPROACH));
        addToBothLists(output1, output2, String.valueOf(AGENTS_PER_BOTTOM_APPROACH));
        addToBothLists(output1, output2, String.valueOf(ITERATION));
        addToBothLists(output1, output2, Double.toString(STORAGE_CAPFACTOR));
        addToBothLists(output1, output2, Double.toString(FLOW_CAPFACTOR));
        addToBothLists(output1, output2, Double.toString(fixedTimeDelay));
        addToBothLists(output1, output2, Double.toString(adativeTimeDelay));
        addToBothLists(output1, output2, compareResults);

        writer1.writeNext(output1.toArray(new String[0]));
        writer1.flush();

        writer2.writeNext(output2.toArray(new String[0]));
        writer2.flush();
    }
    public static void addToBothLists(ArrayList<String> output1, ArrayList<String> output2, String value) {
        output1.add(value);
        output2.add(value);
    }
}
