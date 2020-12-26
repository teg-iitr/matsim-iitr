package playground.nidhi.practice.eventHandlingPract;

public class MN_analysis {
    public static void main(String[] args) {
//        String inputFile = "C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz";
//        EventsManager events = EventsUtils.createEventsManager();

        ModeShareEvent mse = new ModeShareEvent("C:/Users/Nidhi/Documents/GitHub/matsim-iitr/output/output_events.xml.gz");
        mse.run();
//        String outputFile = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/matsimFiles/event_analysis_result.txt";
//        mse.writeResults(outputFile);


    }
}
