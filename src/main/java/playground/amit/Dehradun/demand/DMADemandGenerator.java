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
import playground.amit.Dehradun.OD;
import playground.amit.utils.RandomNumberUtils;
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
    private static final String plans_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_plans_0.1sample.xml.gz";
    private static final String zone_file = SVN_repo + "atIITR/zones_update_29082021_11092021/zones_updated.shp";
    private Collection<SimpleFeature> features ;

    private static final String OD_all_file = SVN_repo + "atIITR/FinalTripMatrix.txt";
//    private static final String OD_rail_file = SVN_repo + "atIITR/FinalTripMatrix_rail.txt";
//    private static final String OD_bus_file = SVN_repo + "atIITR/FinalTripMatrix_bus.txt";

    private Population population;
    private PopulationFactory pf;

    private final Random random = MatsimRandom.getLocalInstance();

    public static void main(String[] args) {
        new DMADemandGenerator().run();
    }

    private void run(){
        this.features = ShapeFileReader.getAllFeatures(zone_file);

        Map<Id<OD>, OD> remaining_OD = generateOD(OD_all_file); //all - rail - bus
//        Map<Id<OD>, OD> rail_OD = generateOD(OD_rail_file);
//        Map<Id<OD>, OD> bus_OD = generateOD(OD_bus_file);

//        generatePlans(rail_OD, DehradunUtils.TravelModes.rail.toString());
//        generatePlans(bus_OD, DehradunUtils.TravelModes.bus.toString());

        if (this.population == null) {
            Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
            this.population = scenario.getPopulation();
            this.pf = this.population.getFactory();
        }

        remaining_OD.values().stream()
                .filter(e-> ! (e.getOrigin().equalsIgnoreCase("Total") || e.getDestination().equalsIgnoreCase("Total")))
                .forEach(e -> IntStream.range(0, e.getNumberOfTrips())
                        .forEach(i -> generatePlan(e.getOrigin(), e.getDestination(), getMode())));

        //clean plans without coords (this should not happen anymore, Amit Sep'21)
        List<Person> personsOutsideZones =  this.population.getPersons().values().stream()
                .filter(this::isAnyActNull).collect(Collectors.toList());
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
        // except, bus (18%) and rail, shares of 2W, car, shared IPT (&others), walk, cycles are 36, 17, 10, 6, 13
        // making these to 100%; shares will be 44, 21, 12, 7, 16
        //Sep. 2021: considering only ONE OD Matrix.
        int rnd = this.random.nextInt(101);
//        if(rnd <= 44) return DehradunUtils.TravelModes.motorbike.toString();
//        else if (rnd <= 65) return DehradunUtils.TravelModes.car.toString();
//        else if (rnd <= 77) return DehradunUtils.TravelModes.IPT.toString();
//        else if (rnd <= 84) return DehradunUtils.TravelModes.walk.toString();
//        else  return DehradunUtils.TravelModes.bicycle.toString();
        if(rnd <= 36) return DehradunUtils.TravelModesBaseCase2017.motorbike.toString();
        else if (rnd <= 53) return DehradunUtils.TravelModesBaseCase2017.car.toString();
        else if (rnd <= 63) return DehradunUtils.TravelModesBaseCase2017.IPT.toString();
        else if (rnd <= 69) return DehradunUtils.TravelModesBaseCase2017.walk.toString();
        else if (rnd <= 82) return DehradunUtils.TravelModesBaseCase2017.bicycle.toString();
        else  return DehradunUtils.TravelModesBaseCase2017.bus.toString();
    }

    private void generatePlan(String origin, String destination, String travelMode){
        Person person = pf.createPerson(Id.createPersonId(this.population.getPersons().size()));
        person.getAttributes().putAttribute(DehradunUtils.origin, origin);
        person.getAttributes().putAttribute(DehradunUtils.destination, destination);
        Plan plan = pf.createPlan();
        Activity act = pf.createActivityFromCoord("FirstAct", getRandomCoord(origin));
        act.setEndTime(getTripEndTime()*3600.); // trip end between 05:00 and 23:00
        plan.addActivity(act);
        plan.addLeg(pf.createLeg(travelMode));
        Activity a = pf.createActivityFromCoord("SecondAct", getRandomCoord(destination));
        plan.addActivity(a);
        person.addPlan(plan);
        this.population.addPerson(person);
    }

    private double getTripEndTime(){
        // we have typical two peak patterns (two normal distributions), let us assume the weights of each peak is 1:1
        double share_morning_peak = 0.5; //TODO : do the math for this

        if (random.nextDouble() < share_morning_peak) {
            return RandomNumberUtils.getRNNormallyDistributed(9.0, 2.0);
        } else {
            return RandomNumberUtils.getRNNormallyDistributed(18.0, 2.0);
        }
    }

//    private void generatePlans( Map<Id<OD>, OD> odMap, String travelMode){
//        if (this.population == null) {
//            Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//            this.population = scenario.getPopulation();
//            this.pf = this.population.getFactory();
//        }
//        odMap.values().forEach(od -> generatePlan(od.origin, od.destination, travelMode));
//    }

    private Coord getRandomCoord(String zoneId){
        if (zoneId.equals("181")) zoneId ="135"; // cannot distinguish between 181 and 135

        for (SimpleFeature feature : this.features){
            String handle = (String) feature.getAttribute("Zone"); // a unique key
            if (handle.equals(zoneId)){
                Point p = GeometryUtils.getRandomPointsInsideFeature(feature);
                return new Coord(p.getX(), p.getY());
            }
        }
        return null; // zone file does not match.
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
                        od.setNumberOfTrips( (int) Math.round(Integer.parseInt(parts[index]) * DehradunUtils.sampleSize) );
                        odMap.put(od.getId(), od);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return odMap;
    }
}

