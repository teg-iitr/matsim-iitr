package playground.shivam.signals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.MixedTrafficSignals;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.controller.laemmerFix.MixedTrafficLaemmerSignalController;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import playground.shivam.signals.runner.RunMatsim;

import java.io.IOException;
import java.util.Scanner;

import static playground.shivam.signals.config.CreateConfig.defineConfig;
import static playground.shivam.signals.scenarios.CreateScenarioFromConfig.defineScenario;

public class RunSignals {
    private static Controler controler;
    private static String outputDirectory;
    private static String signalController;
    private static Class<? extends SignalControllerFactory> signalControllerFactoryClassName;

    public static void main(String[] args) throws IOException {
        Scanner sc= new Scanner(System.in);
        int a;
        do {
            System.out.println("1. Fixed Time Signal");
            System.out.println("2. Adaptive Signal ");
            System.out.print("Enter your choice: ");
            a = sc.nextInt();
            System.out.println();
            switch (a) {
                case 1:
                    fixedTimeSignal();
                    break;
                case 2:
                    adaptiveSignal();
                    break;
                default:
                    System.out.println("You have exited");
            }
        } while (a != 0);

    }

    private static void fixedTimeSignal() throws IOException {
        outputDirectory = "output/RunFixedMixedTrafficSignalSimpleIntersection/";
        signalController = DefaultPlanbasedSignalSystemController.IDENTIFIER;
        signalControllerFactoryClassName = DefaultPlanbasedSignalSystemController.FixedTimeFactory.class;

        final Config config = defineConfig(outputDirectory, false);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController, false);

        controler = new Controler(scenario);

        MixedTrafficSignals.configure(controler);

        RunMatsim.run(false, controler, signalController, signalControllerFactoryClassName, outputDirectory);
    }

    private static void adaptiveSignal() throws IOException {
        outputDirectory = "output/RunAdaptiveSignalSimpleNetwork/";
        signalController = MixedTrafficLaemmerSignalController.IDENTIFIER;
        signalControllerFactoryClassName = MixedTrafficLaemmerSignalController.LaemmerFactory.class;

        final Config config = defineConfig(outputDirectory, true);
        final Scenario scenario = defineScenario(config, outputDirectory, signalController, true);

        controler = new Controler(scenario);

        MixedTrafficSignals.configure(controler);

        RunMatsim.run(false, controler, signalController, signalControllerFactoryClassName, outputDirectory);
    }
}
