package playground.amit.jaipur.plans;

import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.OD;
import playground.amit.jaipur.JaipurUtils;
import playground.amit.utils.geometry.GeometryUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Amit, created on 03-03-2022
 */

public class ODMatrixGenerator {

    private static final String shapeFile = "C:\\Users\\Amit\\Documents\\git-repos\\matsim-Indian-scenarios\\Jaipur\\shapeFile\\Jaipur\\datameet\\Jaipur_Wards-SHP\\Jaipur_Wards.shp";
    private static final String ODMatrixFile = "C:\\Users\\Amit\\Downloads\\OD Matrix.csv";
    private static final String zone_key= "WARD_NO";
    public static final String ORIGIN_ACTIVITY = "origin";
    public static final String DESTINATION_ACTIVITY = "destination";
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, JaipurUtils.EPSG);

    public static void main(String[] args) {

        Collection<SimpleFeature> features =  ShapeFileReader.getAllFeatures(shapeFile);

        Map<Id<OD>, OD> remaining_OD = ODMatrixGenerator.generateOD(ODMatrixFile); //all - rail - bus

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        for(OD od : remaining_OD.values()) {
            String originID = od.getOrigin();
            String destinationID = od.getDestination();
            double numberOfTrips = od.getNumberOfTrips();

            for (int i =0 ; i < numberOfTrips ; i++){

//                create matsim plans --> origin coord, destination coord, time, mode,

                SimpleFeature origin_feature = null;
                SimpleFeature destination_feature = null;

                for (SimpleFeature feature : features){
                    String zoneID = (String) feature.getAttribute(zone_key);
                    if(zoneID.equals(originID)) origin_feature= feature;
                    else if(zoneID.equals(destinationID)) destination_feature = feature;
                }

                if(origin_feature==null || destination_feature == null) {
                    System.out.println("Origin zone " + originID);
                    System.out.println("Destination zone " + originID);
                    continue;
                }

                Point originPoint = GeometryUtils.getRandomPointInsideFeature(origin_feature);
                Point destinationPoint = GeometryUtils.getRandomPointInsideFeature(destination_feature);

                Coord origin = transformation.transform(MGC.point2Coord(originPoint));
                Coord destination = transformation.transform(MGC.point2Coord(destinationPoint));

                Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = factory.createPlan();

                Activity origAct = factory.createActivityFromCoord(ORIGIN_ACTIVITY, origin);
                origAct.setEndTime(6.*3600.+ MatsimRandom.getLocalInstance().nextInt(2*3600));
                plan.addActivity(origAct);

                Leg leg = factory.createLeg("car");
                plan.addLeg(leg);

                Activity destinAct = factory.createActivityFromCoord(DESTINATION_ACTIVITY, destination);
                plan.addActivity(destinAct);

                person.addPlan(plan);
                population.addPerson(person);

            }

        }

        new PopulationWriter(population).write("C:\\Users\\Amit\\Downloads\\testPlansJaipur.xml.gz");


    }

    public static Map<Id<OD>, OD> generateOD(String inputFile){
        Map<Id<OD>, OD> odMap = new HashMap<>();
        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        try {
            String line = reader.readLine();
            List<String> destinations = null;
            while (line!=null){
                String [] parts = line.split(",");
                if (destinations == null ){
                    destinations = Arrays.asList(parts);
                } else {
                    String origin = parts[0];
                    for (int index = 1; index<destinations.size()-1;index++){
                        OD od = new OD(origin, destinations.get(index));
                        od.setNumberOfTrips( (int) Math.round(Integer.parseInt(parts[index]) ) );
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
