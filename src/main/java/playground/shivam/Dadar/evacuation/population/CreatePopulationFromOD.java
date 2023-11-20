package playground.shivam.Dadar.evacuation.population;

import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.Dehradun.OD;
import playground.amit.jaipur.plans.ODMatrixGenerator;
import playground.amit.utils.geometry.GeometryUtils;
import playground.shivam.Dadar.evacuation.DadarUtils;

import java.util.Collection;
import java.util.Map;

import static playground.shivam.Dadar.evacuation.DadarUtils.*;
import static playground.shivam.Dadar.evacuation.modalSplit.ModalSplitDistribution.getTravelModeFromMainModes;
import static playground.shivam.Dadar.evacuation.modalSplit.ModalSplitDistribution.getTravelModeFromModalSplit;

public class CreatePopulationFromOD {
    public static void createPlansFromDadarOD(int choice) {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        Map<Id<OD>, OD> tripMatrix = ODMatrixGenerator.generateOD(OD_MATRIX);
        Collection<SimpleFeature> zones = ShapeFileReader.getAllFeatures(ZONES_SHAPEFILE);

        for (OD tripOd : tripMatrix.values()) {

            String originID = tripOd.getOrigin();
            String destinationID = tripOd.getDestination();
            double numberOfTrips = tripOd.getNumberOfTrips();

            if (numberOfTrips == 0) continue;

            SimpleFeature origin_zone = null;
            SimpleFeature destination_zone = null;

            for (SimpleFeature zone : zones) {
                String zone_key = "name";
                String zoneID = String.valueOf(zone.getAttribute(zone_key));
                if (origin_zone == null && zoneID.equals(originID)) origin_zone = zone;
                else if (destination_zone == null && zoneID.equals(destinationID)) destination_zone = zone;
            }

            if (origin_zone == null || destination_zone == null) {
                System.out.println("Either of the origin zone " + originID + " or destination zone " + destinationID + " is null.");
                continue;
            }

            for (int i = 0; i < numberOfTrips; i++) {

//                create matsim plans --> origin coord, destination coord, time, mode,

                Point originPoint = GeometryUtils.getRandomPointInsideFeature(origin_zone);
                Point destinationPoint = GeometryUtils.getRandomPointInsideFeature(destination_zone);

                Coord origin = DadarUtils.TRANSFORMATION_FROM_WSG_84.transform(MGC.point2Coord(originPoint));
                Coord destination = DadarUtils.TRANSFORMATION_FROM_WSG_84.transform(MGC.point2Coord(destinationPoint));

                Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
                Plan plan = factory.createPlan();

                Activity origAct = factory.createActivityFromCoord(ORIGIN_ACTIVITY, origin);
                // all agents leave around 6 am.
                origAct.setEndTime(6 * 3600 + MatsimRandom.getRandom().nextInt(3600));
                plan.addActivity(origAct);
                Leg leg;
                switch (choice) {
                    case 1:
                        leg = factory.createLeg("car");
                        plan.addLeg(leg);
                        break;
                    case 2:
                        leg = factory.createLeg(getTravelModeFromMainModes(MatsimRandom.getLocalInstance().nextInt(100)));
                        plan.addLeg(leg);
                        break;
                    default:
                        leg = factory.createLeg(getTravelModeFromModalSplit(MatsimRandom.getLocalInstance().nextDouble() * 100));
                        plan.addLeg(leg);
                }

                Activity destinAct = factory.createActivityFromCoord(DESTINATION_ACTIVITY, destination);
                plan.addActivity(destinAct);

                person.addPlan(plan);
                population.addPerson(person);

            }
        }
        new PopulationWriter(population).write(MATSIM_PLANS);
    }
}
