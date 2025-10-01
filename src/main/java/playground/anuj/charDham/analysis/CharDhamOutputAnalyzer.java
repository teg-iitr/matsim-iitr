package playground.anuj.charDham.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static playground.anuj.charDham.population.CharDhamInitialPlans.*;
import static playground.anuj.charDham.runner.RunCharDhamSingleSimulation.*;

public class CharDhamOutputAnalyzer {

    private static final String BASE_OUTPUT_DIRECTORY = "output/charDham_runs/";
    // MODIFICATION START: Define the new subdirectory for all analysis outputs.
    private static final String ANALYSIS_SUBDIRECTORY = "analysis_run_outputs";
    // MODIFICATION END
    private static final String PARAMETER_RUNS_CSV = "input/parameter_runs.csv";
    private static final String ANALYSIS_SUMMARY_BASE_NAME = "analysis_summary";
    private static final String FACILITY_REST_ANALYSIS_BASE_NAME = "facility_rest_analysis";
    private static final String DHAM_ACTIVITY_ANALYSIS_BASE_NAME = "dham_activity_analysis";

    static final Collection<String> modes = Arrays.asList(CAR_MODE, TAXI_MODE, MOTORBIKE_MODE, BUS_MODE, MINI_BUS_MODE, WALK_MODE);

    static final Set<String> DHAM_ACTIVITY_TYPES = new HashSet<>(Arrays.asList(
            "Kedarnath", "Gangotri", "Yamunotri", "Badrinath", "Hemkund_Sahib"
    ));

    private static class RunParameters {
        // ... (This inner class remains unchanged)
        String runId;
        int lastIteration;

        RunParameters(String[] parts, String[] headers) {
            Map<String, String> dataMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                if (i < parts.length) {
                    dataMap.put(headers[i].trim(), parts[i].trim());
                }
            }
            this.runId = dataMap.getOrDefault("run_id", "default_run");
            this.lastIteration = Integer.parseInt(dataMap.getOrDefault("last_iteration", "20"));
        }
    }

    public static void main(String[] args) {
        List<RunParameters> runs = readRunParameters();

        if (runs.isEmpty()) {
            System.err.println("No simulation parameters found in " + PARAMETER_RUNS_CSV + ". Cannot perform analysis. Exiting.");
            return;
        }

        System.out.println("Starting analysis of MATSim simulation outputs...");

        // Note: The parent directory (BASE_OUTPUT_DIRECTORY/ANALYSIS_SUBDIRECTORY)
        // will be created on-demand when the first day-wise directory is created.
        // No need for a separate creation step here.

        Set<String> writtenHeaders = new HashSet<>();

        for (int i = 0; i < runs.size(); i++) {
            RunParameters params = runs.get(i);
            String runOutputDir = BASE_OUTPUT_DIRECTORY + params.runId + "/";
            String eventsFilePath = runOutputDir + "output_events.xml.gz";

            System.out.println("\n--- Analyzing Run: " + params.runId + " (Run " + (i + 1) + " of " + runs.size() + ") ---");
            System.out.println("Looking for events file: " + eventsFilePath);

            if (!Files.exists(Paths.get(eventsFilePath))) {
                System.err.println("!!! Events file not found for run " + params.runId + ". Skipping this run.");
                continue;
            }

            try {
                Map<String, SimulationAnalysisResult> resultsByPeriod = processSingleRunEvents(params.runId, params.lastIteration, eventsFilePath);

                for (Map.Entry<String, SimulationAnalysisResult> entry : resultsByPeriod.entrySet()) {
                    String day = entry.getKey();
                    SimulationAnalysisResult result = entry.getValue();

                    // MODIFICATION START: Create the day-specific directory inside the new analysis subdirectory.
                    Path dayDirectory = Paths.get(BASE_OUTPUT_DIRECTORY, ANALYSIS_SUBDIRECTORY, day);
                    Files.createDirectories(dayDirectory);
                    // MODIFICATION END

                    if (!writtenHeaders.contains(day)) {
                        writeAnalysisSummaryHeader(day);
                        writeFacilityRestAnalysisHeader(day);
                        writeDhamActivityAnalysisHeader(day);
                        writtenHeaders.add(day);
                    }

                    writeAnalysisResultToSummary(result);
                    writeFacilityRestAnalysisData(result);
                    writeDhamActivityAnalysisData(result);
                }
                System.out.println("--- Analysis for " + params.runId + " COMPLETED ---");
            } catch (Exception e) {
                System.err.println("!!! Error analyzing events for run " + params.runId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        // MODIFICATION START: Update the final log message to point to the correct location.
        System.out.println("\nAll analyses finished. Summaries available in subdirectories within: "
                + Paths.get(BASE_OUTPUT_DIRECTORY, ANALYSIS_SUBDIRECTORY));
        // MODIFICATION END
    }

    private static List<RunParameters> readRunParameters() {
        // ... (This method remains unchanged)
        List<RunParameters> runs = new ArrayList<>();
        try (BufferedReader br = IOUtils.getBufferedReader(PARAMETER_RUNS_CSV)) {
            String headerLine = br.readLine();
            if (headerLine == null) return runs;
            String[] headers = headerLine.split(",");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != headers.length) {
                    System.err.println("Warning: Skipping malformed line in " + PARAMETER_RUNS_CSV + ": " + line);
                    continue;
                }
                try {
                    runs.add(new RunParameters(parts, headers));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Skipping line due to number format error in " + PARAMETER_RUNS_CSV + ": " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading parameter runs CSV file: " + PARAMETER_RUNS_CSV);
            e.printStackTrace();
        }
        return runs;
    }

    private static Map<String, SimulationAnalysisResult> processSingleRunEvents(String runId, int iteration, String eventsFilePath) {
        // ... (This method remains unchanged)
        EventsManager eventsManager = EventsUtils.createEventsManager();
        CharDhamAnalysisEventHandler analysisHandler = new CharDhamAnalysisEventHandler(runId, iteration);
        eventsManager.addHandler(analysisHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFilePath);
        return analysisHandler.getAnalysisResults();
    }

    /**
     * MODIFICATION START:
     * Generates the file path inside the day-specific directory, which is now nested
     * within the ANALYSIS_SUBDIRECTORY. The filename itself does not need the day.
     * e.g., .../analysis_run_outputs/day1/analysis_summary.csv
     */
    private static String getFilePath(String baseName, String day) {
        return Paths.get(BASE_OUTPUT_DIRECTORY, ANALYSIS_SUBDIRECTORY, day, baseName + ".csv").toString();
    }
    // MODIFICATION END

    // The rest of the file (write...Header and write...Data methods) remains unchanged.
    // They will correctly use the updated getFilePath() method.
    private static void writeAnalysisSummaryHeader(String day) throws IOException {
        String filePath = getFilePath(ANALYSIS_SUMMARY_BASE_NAME, day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, false))) {
            List<String> headers = new ArrayList<>(Arrays.asList(
                    "run_id", "iteration", "total_agents_simulated",
                    "total_travel_time_s", "average_travel_time_s", "min_travel_time_s", "max_travel_time_s",
                    "total_legs", "average_leg_travel_time_s",
                    "total_unique_persons_resting", "total_unique_persons_at_dhams",
                    "total_night_travel_time_s", "num_agents_night_travel"
            ));
            for (String mode : modes) {
                headers.add("legs_" + mode);
                headers.add("travel_time_" + mode + "_s");
                headers.add("avg_travel_time_" + mode + "_s");
            }
            for (String mode : modes) {
                headers.add("percentage_legs_" + mode);
            }
            writer.println(String.join(",", headers));
        }
    }

    private static void writeAnalysisResultToSummary(SimulationAnalysisResult result) throws IOException {
        String filePath = getFilePath(ANALYSIS_SUMMARY_BASE_NAME, result.day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            List<String> row = new ArrayList<>();
            row.add(result.runId);
            row.add(String.valueOf(result.iteration));
            row.add(String.valueOf(result.totalAgentsSimulated));
            row.add(String.valueOf(result.totalTravelTime_s));
            row.add(String.valueOf(result.averageTravelTime_s));
            row.add(String.valueOf(result.minTravelTime_s));
            row.add(String.valueOf(result.maxTravelTime_s));
            row.add(String.valueOf(result.totalLegs));
            row.add(String.valueOf(result.averageLegTravelTime_s));

            long uniqueRestingPersons = result.uniqueRestActivityPersonsPerFacility.values().stream()
                    .flatMap(Set::stream).distinct().count();
            row.add(String.valueOf(uniqueRestingPersons));

            long uniqueDhamPersons = result.uniqueDhamActivityPersons.values().stream()
                    .flatMap(Set::stream).distinct().count();
            row.add(String.valueOf(uniqueDhamPersons));

            row.add(String.valueOf(result.totalNightTravelTime_s));
            row.add(String.valueOf(result.numAgentsNightTravel));

            for (String mode : modes) {
                row.add(String.valueOf(result.totalLegsPerMode.getOrDefault(mode, 0)));
                row.add(String.valueOf(result.totalTravelTimePerMode.getOrDefault(mode, 0.0)));
                row.add(String.valueOf(result.avgTravelTimePerMode.getOrDefault(mode, 0.0)));
            }
            for (String mode : modes) {
                row.add(String.valueOf(result.percentageLegsPerMode.getOrDefault(mode, 0.0)));
            }
            writer.println(String.join(",", row));
        }
    }

    private static void writeFacilityRestAnalysisHeader(String day) throws IOException {
        String filePath = getFilePath(FACILITY_REST_ANALYSIS_BASE_NAME, day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, false))) {
            writer.println("run_id,iteration,facility_id,unique_rest_persons");
        }
    }

    private static void writeFacilityRestAnalysisData(SimulationAnalysisResult result) throws IOException {
        String filePath = getFilePath(FACILITY_REST_ANALYSIS_BASE_NAME, result.day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            for (Map.Entry<Id<ActivityFacility>, Integer> entry : result.totalRestActivityUsesPerFacilityCount.entrySet()) {
                writer.println(String.join(",",
                        result.runId,
                        String.valueOf(result.iteration),
                        String.valueOf(entry.getKey()),
                        String.valueOf(entry.getValue())
                ));
            }
        }
    }

    private static void writeDhamActivityAnalysisHeader(String day) throws IOException {
        String filePath = getFilePath(DHAM_ACTIVITY_ANALYSIS_BASE_NAME, day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, false))) {
            writer.println("run_id,iteration,dham_activity_type,unique_dham_persons");
        }
    }

    private static void writeDhamActivityAnalysisData(SimulationAnalysisResult result) throws IOException {
        String filePath = getFilePath(DHAM_ACTIVITY_ANALYSIS_BASE_NAME, result.day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            for (String dhamType : DHAM_ACTIVITY_TYPES) {
                int uniquePersons = result.totalDhamActivityUsesCount.getOrDefault(dhamType, 0);
                writer.println(String.join(",",
                        result.runId,
                        String.valueOf(result.iteration),
                        dhamType,
                        String.valueOf(uniquePersons)
                ));
            }
        }
    }
}