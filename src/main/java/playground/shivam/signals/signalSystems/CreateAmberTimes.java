package playground.shivam.signals.signalSystems;

import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesDataFactory;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesWriter10;

public class CreateAmberTimes {
    public static void createAmberTimes(AmberTimesData amberTimesData, String outputDirectory) {
        amberTimesData.setDefaultAmber(5);
        amberTimesData.setDefaultAmberTimeGreen(2.0);
        AmberTimesDataFactory amberTimesDataFactory = amberTimesData.getFactory();
        AmberTimesWriter10 amberTimesWriter10 = new AmberTimesWriter10(amberTimesData);
        amberTimesWriter10.write(outputDirectory + "amber_time.xml.gz");
    }
}
