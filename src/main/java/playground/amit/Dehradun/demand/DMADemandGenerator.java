package playground.amit.Dehradun.demand;

import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author Amit
 *
 */
public class DMADemandGenerator {

    private static final String SVN_repo = "C:/Users/Amit/Documents/svn-repos/shared/data/project_data/DehradunMetroArea_MetroNeo_data/";
    private static final String plans_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_plans.xml.gz";
    private static final String zone_file = SVN_repo + "atIITR/zones_update_29082021/zone_data_update.shp";
    private Collection<SimpleFeature> features ;

    private static final String OD_all_file = SVN_repo + "atIITR/FinalTripMatrix.txt";
    private static final String OD_rail_file = SVN_repo + "atIITR/FinalTripMatrix_rail.txt";
    private static final String OD_bus_file = SVN_repo + "atIITR/FinalTripMatrix_bus.txt";

    private Population population;
    private PopulationFactory pf;

    private Random random = MatsimRandom.getLocalInstance();

    public static void main(String[] args) {
        new DMADemandGenerator().run();
    }

    private void run(){
        this.features = ShapeFileReader.getAllFeatures(zone_file);

        Map<Id<OD>, OD> remaining_OD = generateOD(OD_all_file); //all - rail - bus
        Map<Id<OD>, OD> rail_OD = generateOD(OD_rail_file);
        Map<Id<OD>, OD> bus_OD = generateOD(OD_bus_file);

        generatePlans(rail_OD, DehradunUtils.TravelModes.rail.toString());
        generatePlans(bus_OD, DehradunUtils.TravelModes.bus.toString());

        remaining_OD.forEach((key, value) -> {
            int numberOfTrips = value.numberOfTrips - rail_OD.get(key).numberOfTrips - bus_OD.get(key).numberOfTrips;
            IntStream.range(0, numberOfTrips).forEach(i -> generatePlan(value.origin, value.destination, getMode()));
        });

        //clean plans without coords
        List<Person> personsOutsideZones =  this.population.getPersons().values().stream().filter(this::isAnyActNull).collect(Collectors.toList());
        personsOutsideZones.forEach(p -> this.population.removePerson(p.getId()));
        new PopulationWriter(this.population).write(plans_file);
    }

    private boolean isAnyActNull(Person p){
        List<PlanElement> pes = p.getPlans().get(0).getPlanElements();
        for (PlanElement pe : pes) {
            if (  pe instanceof Activity && ((Activity)pe).getCoord()==null) return true;
        }
        return false;
    }

    private String getMode(){
        //Table 6-4 of Metro report provides modal share by trips
        // except, bus (18%), rail, shares of 2W, car, auto, shared IPT are 36, 20, 5, 21,
        // making these to 100%; shares will be 44, 24, 6, 26
        int rnd = this.random.nextInt(101);
        if(rnd <= 44) return DehradunUtils.TravelModes.motorbike.toString();
        else if (rnd <= 68) return DehradunUtils.TravelModes.car.toString();
        else if (rnd <= 74) return DehradunUtils.TravelModes.auto.toString();
        else  return DehradunUtils.TravelModes.IPT.toString();
    }

    private void generatePlan(String origin, String destination, String travelMode){
        Person person = pf.createPerson(Id.createPersonId(this.population.getPersons().size()));
        Plan plan = pf.createPlan();
        Activity act = pf.createActivityFromCoord("FirstAct", getRandomCoord(origin));
        act.setEndTime(5*3600. + random.nextInt(18*60+1)*60.); // trip end between 05:00 and 23:00
        plan.addActivity(act);
        plan.addLeg(pf.createLeg(travelMode));
        Activity a = pf.createActivityFromCoord("SecondAct", getRandomCoord(destination));
        plan.addActivity(a);
        person.addPlan(plan);
        this.population.addPerson(person);
    }

    private void generatePlans( Map<Id<OD>, OD> odMap, String travelMode){
        if (this.population == null) {
            Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
            this.population = scenario.getPopulation();
            this.pf = this.population.getFactory();
        }
        odMap.values().forEach(od -> generatePlan(od.origin, od.destination, travelMode));
    }

    private Coord getRandomCoord(String zoneId){
        for (SimpleFeature feature : this.features){
            String handle = (String) feature.getAttribute("Zone"); // a unique key
            if (handle.equals(zoneId)){
                Point p = GeometryUtils.getRandomPointsInsideFeature(feature);
                Coord cord = new Coord(p.getX(), p.getY());
//                return DehradunUtils.transformation.transform(cord);
                return cord;
            }
        }
        return null; // zone file does not match.
    }

    static Id<OD> getID(String origin, String destination){
        return Id.create(origin+"_"+destination, OD.class);
    }

    private Map<Id<OD>, OD> generateOD(String inputFile){
        Map<Id<OD>, OD> odMap = new HashMap<>();
        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        try {
            String line = reader.readLine();
            List<String> destinations = null;
            while (line!=null){
                String [] parts = line.split("\t");
                if (destinations == null ){
                    destinations = Arrays.asList(parts);
                } else {
                    String origin = parts[0];
                    for (int index = 1; index<destinations.size()-1;index++){
                        OD od = new OD(origin, destinations.get(index));
                        od.numberOfTrips = Integer.parseInt(parts[index]);
                        odMap.put(od.id, od);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return odMap;
    }

    public static class OD {
        final String origin;
        final String destination;
        final Id<OD> id;

        int numberOfTrips = 0;

        public OD (String origin, String destination) {
            this.origin = origin;
            this.destination = destination;
            this.id = DMADemandGenerator.getID(this.origin, this.destination);
        }

        public void setNumberOfTrips(int trips){
            this.numberOfTrips = trips;
        }

        @Override
        public String toString() {
            return "Origin: "+this.origin+"\t Destination: "+this.destination+"\t number of trips: "+this.numberOfTrips;
        }

        public String getOrigin() {
            return origin;
        }

        public String getDestination() {
            return destination;
        }

        public int getNumberOfTrips() {
            return numberOfTrips;
        }

        public Id<OD> getId() {
            return id;
        }
    }
}

