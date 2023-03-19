package playground.shivam.Dadar.evacuation;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import playground.amit.utils.LoadMyScenarios;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import static playground.shivam.Dadar.evacuation.DadarUtils.MATSIM_NETWORK;
import static playground.shivam.Dadar.evacuation.SafePoints.SafePoints.safePoints;
import static playground.shivam.Dadar.evacuation.network.CreateEvacuationNetworkFromMatsimNetwork.createDadarEvacNetwork;
import static playground.shivam.Dadar.evacuation.network.CreateNetworkFromOSM.createDadarNetworkFromOSM;
import static playground.shivam.Dadar.evacuation.population.CreateEvacuationPopulationFromMatsimPopulation.createDadarEvacPlans;
import static playground.shivam.Dadar.evacuation.population.CreatePopulationFromOD.createPlansFromDadarOD;
import static playground.shivam.Dadar.evacuation.scenarios.DadarAllModes.createDadarAllModesConfig;
import static playground.shivam.Dadar.evacuation.scenarios.DadarSingleMode.createDadarSingleModeConfig;
import static playground.shivam.Dadar.evacuation.scenarios.DadarWEvacuation.createDadarWEvacuationConfig;
import static playground.shivam.Dadar.evacuation.scenarios.DadarWOTeleportedModes.createDadarWOTeleportedModesConfig;

/**
 * @author Shivam
 */
public class RunDadarScenariosOptional {
    private static int a = 0;

    private final Collection<String> DADAR_MAIN_MODES = DadarUtils.MAIN_MODES;
    private final Collection<String> DADAR_TELEPORTED_MODES = DadarUtils.TELEPORTED_MODES;
    public Collection<Id<Node>> safeNodeAIds = new ArrayList<>();
    private Scenario scenario;


    public void runDadarEvacuation() {
        createDadarNetworkFromOSM();

        Scenario scenarioFromNetwork = LoadMyScenarios.loadScenarioFromNetwork(MATSIM_NETWORK);

        safePoints();

        createDadarEvacNetwork(scenarioFromNetwork);

        createPlansFromDadarOD(a);

        Scenario scenarioFromPlans = LoadMyScenarios.loadScenarioFromNetwork(MATSIM_NETWORK);

        createDadarEvacPlans(scenarioFromPlans, scenarioFromNetwork);

        createDadarWEvacuationConfig();

    }
    private void runDadarWOTeleported() {
        createDadarNetworkFromOSM();
        // createDadarPseudoCounts();
        createPlansFromDadarOD(a);
        createDadarWOTeleportedModesConfig();
    }
    private void runDadarAllModes() {
        createDadarNetworkFromOSM();
        // createDadarPseudoCounts();
        createPlansFromDadarOD(a);
        createDadarAllModesConfig();
    }
    private void runDadarSingleMode() {
        createDadarNetworkFromOSM();
        // createDadarPseudoCounts();
        createPlansFromDadarOD(a);
        createDadarSingleModeConfig();
    }

    public static void main(String[] args) {
        Scanner sc= new Scanner(System.in);    //System.in is a standard input stream
        System.out.println("1. Dadar traffic simulation with single mode");
        System.out.println("2. Dadar traffic simulation with main modes and without teleported modes");
        System.out.println("3. Dadar traffic simulation with all modes, even teleported");
        System.out.println("4. Dadar evacuation traffic simulation");
        System.out.print("Enter your choice: ");
        a = 3;
        System.out.println();
        switch (a){
            case 1:
                new RunDadarScenariosOptional().runDadarSingleMode();
                break;
            case 2:
                new RunDadarScenariosOptional().runDadarWOTeleported();
                break;
            case 3:
                new RunDadarScenariosOptional().runDadarAllModes();
                break;
            case 4:
                new RunDadarScenariosOptional().runDadarEvacuation();
                break;
            default:
                throw new RuntimeException("Input Something");
        }

    }

}
