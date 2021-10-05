package playground.amit.Dehradun.demand;

import org.locationtech.jts.geom.Geometry;
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
    private static final String plans_file = SVN_repo + "atIITR/matsim/DehradunMetropolitanArea_plans_0.1sample_17092021.xml.gz";
    private static final String zone_file = SVN_repo + "atIITR/zones_update_11092021/zones_updated.shp";
    private static final String OD_all_file = SVN_repo + "atIITR/FinalTripMatrix.txt";
    private static final String dma_boundariesShape = SVN_repo+"atIITR/boundary/OSMB-DMA-Boundary_no-smoothening.shp";

    private Collection<SimpleFeature> features ;
    private Geometry dehradunGeom;

    private Population population;
    private PopulationFactory pf;

    private final Random random = MatsimRandom.getLocalInstance();

    public static void main(String[] args) {
        new DMADemandGenerator().run();
    }

    private void run(){
        this.features = ShapeFileReader.getAllFeatures(zone_file);

        //allow autos in Dehradun only
//        Collection<SimpleFeature> features_boundaries = ShapeFileReader.getAllFeatures(dma_boundariesShape);
//        for (SimpleFeature feature: features_boundaries) {
//            if (feature.getAttribute("name").equals("Dehradun")){
//                this.dehradunGeom = (Geometry) feature.getDefaultGeometry();
//            }
//        }
//        if (dehradunGeom==null) throw new RuntimeException("Dehradun Geometry should not be null. Check the CRS.");

        Map<Id<OD>, OD> remaining_OD = generateOD(OD_all_file); //all - rail - bus
        if (this.population == null) {
            Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
            this.population = scenario.getPopulation();
            this.pf = this.population.getFactory();
        }

        remaining_OD.values().stream()
                .filter(e-> ! (e.getOrigin().equalsIgnoreCase("Total") || e.getDestination().equalsIgnoreCase("Total")))
                .forEach(e -> IntStream.range(0, e.getNumberOfTrips())
                        .forEach(i -> generatePlan(e.getOrigin(), e.getDestination())));

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
        //Table 6-4 of Metro report provides modal share by trips: 2W 36%, car 20%, Auto 5%, IPT 21%, Bus 18%.
        // since auto is primarily used in Dehradun which is not our study scenario and has a minor share, we are not including them in the simulatino.
        // the utility parameters are also not given for auto mode.
        // 2W 37.9, car 21.0, IPT 22.1, Bus, 18.9 --> to make the modal share 100.
        double rnd = this.random.nextDouble();
        if(rnd < 0.379) return DehradunUtils.TravelModesBaseCase2017.motorbike.toString();
        else if (rnd < 0.589) return DehradunUtils.TravelModesBaseCase2017.car.toString();
        else if (rnd < 0.81) return DehradunUtils.TravelModesBaseCase2017.IPT.toString();
        else  return DehradunUtils.TravelModesBaseCase2017.bus.toString();
    }

    private void generatePlan(String origin, String destination){
        Person person = pf.createPerson(Id.createPersonId(this.population.getPersons().size()));
        person.getAttributes().putAttribute(DehradunUtils.origin, origin);
        person.getAttributes().putAttribute(DehradunUtils.destination, destination);
        Plan plan = pf.createPlan();
        Coord startCoord = getRandomCoord(origin);
        Coord endCoord =  getRandomCoord(destination);
        Activity act = pf.createActivityFromCoord("FirstAct", startCoord);
        act.setEndTime(getTripEndTime()*3600.); // trip end between 05:00 and 23:00
        plan.addActivity(act);
        //
//        String subPop = DehradunUtils.rest_subPop;
//        if(withinDehradun(startCoord) && withinDehradun(endCoord)) {
//           subPop = DehradunUtils.dehradun_subPop;
//        }
//        person.getAttributes().putAttribute(DehradunUtils.subPopulation, subPop);
        String travelMode = null;

//        do {
            travelMode = getMode();
//        } while(travelMode.equals(DehradunUtils.TravelModesBaseCase2017.auto.toString()) && subPop.equals(DehradunUtils.rest_subPop) );

        plan.addLeg(pf.createLeg(travelMode));
        Activity a = pf.createActivityFromCoord("SecondAct",endCoord);
        plan.addActivity(a);
        person.addPlan(plan);
        this.population.addPerson(person);
    }

//    private boolean withinDehradun(Coord coord){
//        return GeometryUtils.isCoordInsideGeometry(dehradunGeom, coord);
//    }

    private double getTripEndTime(){
        // we have typical two peak patterns (two normal distributions), let us assume the weights of each peak is 1:1
        double share_morning_peak = 0.54; // estimated using the traffic volumes for locations m1 to m19,

        if (random.nextDouble() < share_morning_peak) {
            return RandomNumberUtils.getRNNormallyDistributed(9.0, 2.0);
        } else {
            return RandomNumberUtils.getRNNormallyDistributed(18.0, 2.0);
        }
    }

    private Coord getRandomCoord(String zoneId){
        if (zoneId.equals("181")) zoneId ="135"; // cannot distinguish between 181 and 135

        for (SimpleFeature feature : this.features){
            String handle = (String) feature.getAttribute("Zone"); // a unique key
            if (handle.equals(zoneId)){
                Point p = GeometryUtils.getRandomPointsInsideFeature(feature);
                return new Coord(p.getX(), p.getY());
            }
        }
        throw new RuntimeException("Zone "+zoneId+ " is not known.");
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

