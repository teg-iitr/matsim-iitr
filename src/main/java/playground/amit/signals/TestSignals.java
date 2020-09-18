package playground.amit.signals;

import org.matsim.codeexamples.adaptiveSignals.RunAdaptiveSignalsExample;

public class TestSignals {

    public static void main(String[] args) {
        String configFileName = "C:\\Users\\Amit Agarwal\\Documents\\git\\matsim\\contribs\\signals\\examples\\tutorial\\example90TrafficLights\\useSignalInput\\withLanes\\config.xml";
        String outputDir ="./output/";
        boolean visualize = true;
        RunAdaptiveSignalsExample.run(configFileName, outputDir, visualize);
    }

}
