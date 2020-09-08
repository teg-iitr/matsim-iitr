package playground.agarwalamit.idling;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Amit on 08/09/2020.
 */
public class FuelWastageCalculator {

    private static final Random random = MatsimRandom.getLocalInstance();
    private List<PersonTrip> personTrips = new ArrayList<>();
    private Map<String, Double> fuelWastedPerCity = new HashMap<>();

    public static void main(String[] args) {
        String file = "C:\\Users\\Amit\\Documents\\git-repos\\workingPapers\\iitr\\2020\\papers\\vehicleIdlingTrafficSignals\\data\\surveyData_06072020.tsv";
        FuelWastageCalculator fuelWastageCalculator = new FuelWastageCalculator();
        fuelWastageCalculator.run(file);
        fuelWastageCalculator.calculateFuelWasted();
    }

    private void calculateFuelWasted(){
        for(PersonTrip personTrip : personTrips) {
            int numberOfSignalPerTrip = FuelWastageCalculator.getNumberOfSignalsFromRange(personTrip.signalsPerTripRange);
            if (! personTrip.isAnySignalPresentDuringTrip()) continue;
            double fuelWasted = 0;
            for (int i = 0; i < numberOfSignalPerTrip; i++){
                double thresholdCalculation = IdlingThresholdCalculator.getThresholdDuration(personTrip.vehicleClass,
                        FuelWastageCalculator.getLengthOfQueue(personTrip.positionInQ),
                        IdlingUtils.queueDissipation);
                double fuelWastedNew = IdlingThresholdCalculator.getWastedFule(thresholdCalculation,
                        FuelWastageCalculator.getSignalCountdownTimerAtWhichUserStop(personTrip.setEngineOffTimerRange), personTrip.vehicleClass);
                if(fuelWasted<0){
                    throw new RuntimeException("negative fuel wasted for person "+personTrip.personId);
                }
                fuelWasted += fuelWastedNew;
            }
            double wastePerCity_soFar = fuelWastedPerCity.getOrDefault(personTrip.city,0.);
            fuelWastedPerCity.put(personTrip.city, wastePerCity_soFar+fuelWasted);
        }

        for (String city : fuelWastedPerCity.keySet()) {
            System.out.println("Fuel wasted in cc for city "+city+ " is "+fuelWastedPerCity.get(city));
        }
    }

    private void run(String surveyData){
        try(BufferedReader reader = IOUtils.getBufferedReader(surveyData)) {
            String line = reader.readLine();
            boolean header= true;
            while (line!=null){
                if(header){
                    header=false;
                    line = reader.readLine();
                    continue;
                }
                String parts [] = line.split("\t");

                if(! parts[1].equals("Other") && !parts[2].equals("I don't drive")) {
                    PersonTrip personTrip = new PersonTrip(Id.createPersonId("person"+this.personTrips.size()), parts[7]);
                    personTrip.setCity(parts[1]);
                    personTrip.setAgeOfVehicle(parts[8]);
                    personTrip.setSignalsPerTripRange(parts[10]);
                    personTrip.setActionIfRedSignal(parts[11]);
                    personTrip.setVehiclePositionInQueue(parts[12]);
                    personTrip.setEngineOffTimerRange(parts[16]);
                    personTrips.add(personTrip);
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param range
     * @return a random number between the range.
     */
    private static int getNumberOfSignalsFromRange(String range){
        switch(range){
            case "<5": return random.nextInt(5);
            case "5-10": return 5+random.nextInt(10);
            case "10-20": return 10+random.nextInt(20);
            case ">20" : return 20+random.nextInt(30); //Assumed, maxi 30 signals
        }
        throw new RuntimeException("Range "+ range+ " is not recognized.");
    }

    /**
     *
     * @param choiceOfPositionInQueue
     * @return
     */
    private static double getLengthOfQueue(String choiceOfPositionInQueue){
        int maxQueueLength= 0;
        switch(choiceOfPositionInQueue){
            case "At the end of the queue": maxQueueLength=100; break; // max 100 m long
            case "On the left of the queue": maxQueueLength=70; break;// max 70 m long
            case "Try to come in front using the spaces between cars and other vehicles":
            case "Use footpath/ cycleway if available" : maxQueueLength=20; break;// max 20 m long
            case "" : maxQueueLength = 0; break;// i.e. no signals in the way or always jump signals
        }
        return random.nextDouble() * maxQueueLength;
    }

    /**
     *
     * @param range
     * @return
     */
    private static double getSignalCountdownTimerAtWhichUserStop(String range){
        switch(range){
            case "0-15 sec": return random.nextInt(15);
            case "15-30sec": return 15+random.nextInt(30);
            case "30-45 sec": return 30+random.nextInt(45);
            case ">45 sec" : return 45+random.nextInt(90); //Assumed, maximum 90 seconds for a signal
            case "" : return random.nextInt(90); //Assumed, maximum 90 seconds for a singal. It includes persons who never stops at a signal or no traffic signals
        }
        throw new RuntimeException("Range "+ range+ " is not recognized.");
    }

}
