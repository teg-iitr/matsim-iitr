package playground.agarwalamit.idling;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * Created by Amit on 08/09/2020.
 */
public class PersonTrip {

    final Id<Person> personId;
    final IdlingUtils.VehicleClass vehicleClass;

    String signalsPerTripRange ;
    String city;
    String positionInQ;
    String setEngineOffTimerRange;
    String ageOfVehicle;
    String actionIfRedSignal;

    public PersonTrip(Id<Person> personId, String travelMode) {
        this.personId = personId;
        if (travelMode.equals("Scooter/ Motorcycle/ motorized two-wheelers")) this.vehicleClass = IdlingUtils.VehicleClass.MTW;
        else if(travelMode.equals("Car")) this.vehicleClass = IdlingUtils.VehicleClass.Car;
        else this.vehicleClass = IdlingUtils.VehicleClass.valueOf(travelMode);
    }

    public void setSignalsPerTripRange(String signalsPerTripRange){
        this.signalsPerTripRange = signalsPerTripRange;
    }

    public void setCity(String city){
        this.city = city;
    }

    public void setVehiclePositionInQueue(String positionInQueue){
        this.positionInQ = positionInQueue;
    }

    public void setEngineOffTimerRange(String setEngineOffTimerRange) {
        this.setEngineOffTimerRange = setEngineOffTimerRange;
    }

    public void setAgeOfVehicle(String ageOfVehicle) {
        this.ageOfVehicle = ageOfVehicle;
    }

    public void setActionIfRedSignal(String actionIfRedSignal) {
        this.actionIfRedSignal = actionIfRedSignal;
    }

    public boolean isAnySignalPresentDuringTrip(){
        return !this.actionIfRedSignal.equals("No traffic signals in my way");
    }
}
