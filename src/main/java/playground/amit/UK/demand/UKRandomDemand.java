package playground.amit.UK.demand;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import playground.amit.Dehradun.DehradunUtils;
import playground.amit.UK.network.OSMToUKNetwork;
import playground.amit.clustering.BoundingBox;
import playground.amit.utils.geometry.GeometryUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UKRandomDemand {

    public static final String matsimPlansFile = "C:\\Users\\amit2\\Downloads\\UK-ChaarDham\\Uk-plans.xml.gz";
    public static final String importantLocations = "C:\\Users\\amit2\\Downloads\\UK-ChaarDham\\UKCharDhamPrimeLocations.txt";
    private final Map<String, Coord> location2Coord = new HashMap<>();
    private final Random random = MatsimRandom.getRandom();

    public static void main(String[] args) {
        new UKRandomDemand().run();
    }

    public void run() {
        storeLocations();

        String network = OSMToUKNetwork.matsimNetworkFile;
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(network);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        Geometry UKBoundaryGeom = GeometryUtils.getGeometryFromListOfFeatures(ShapeFileReader.getAllFeatures(OSMToUKNetwork.boundaryShapeFile));

        for (int i = 0; i <100; i++) {
            Person person = populationFactory.createPerson(Id.createPersonId(population.getPersons().size()));
            Plan plan = populationFactory.createPlan();
            Activity haridwar = populationFactory.createActivityFromCoord("Haridwar", location2Coord.get("Haridwar"));
            haridwar.setEndTime(5.*3600.+random.nextDouble()*3600.); // leave between 5-6 from Haridwar
            plan.addActivity(haridwar);
            {
                Leg leg = populationFactory.createLeg("car");
                plan.addLeg(leg);
            }
            Activity jankiChatti = populationFactory.createActivityFromCoord("JankiChatti", location2Coord.get("Janki Chatti Yamunotri"));
            jankiChatti.setEndTime(29.*3600.+random.nextDouble()*3600.); // leave between 5-6
            plan.addActivity(jankiChatti);
            {
                Leg leg = populationFactory.createLeg("walk");
                plan.addLeg(leg);
            }
            Activity yamunotri = populationFactory.createActivityFromCoord("Yamunotri", location2Coord.get("Yamunotri dham"));
            yamunotri.setEndTime(33.*3600.+random.nextDouble()*3600.*2);
            plan.addActivity(yamunotri);
            {
                Leg leg = populationFactory.createLeg("walk");
                plan.addLeg(leg);
            }
            Activity jankiChattiReturn = populationFactory.createActivityFromCoord("JankiChatti", location2Coord.get("Janki Chatti Yamunotri"));
            jankiChattiReturn.setEndTime(36.*3600.+random.nextDouble()*3600.*2);
            plan.addActivity(jankiChattiReturn);
            {
                Leg leg = populationFactory.createLeg("car");
                plan.addLeg(leg);
            }
            Activity gangotri = populationFactory.createActivityFromCoord("Gangotri", location2Coord.get("Gangotri dham"));
            gangotri.setEndTime(53.*3600.+random.nextDouble()*3600.);
            plan.addActivity(gangotri);
            {
                Leg leg = populationFactory.createLeg("car");
                plan.addLeg(leg);
            }
            Activity sonprayag = populationFactory.createActivityFromCoord("Sonprayag", location2Coord.get("Sonprayag"));
            sonprayag.setEndTime(76.*3600.+random.nextDouble()*3600.*2);
            plan.addActivity(sonprayag);
            {
                Leg leg = populationFactory.createLeg("shuttle");
                plan.addLeg(leg);
            }
            Activity gauriKund = populationFactory.createActivityFromCoord("GauriKund", location2Coord.get("Gauri Kund"));
            gauriKund.setEndTime(77.*3600.+random.nextDouble()*3600.);
            plan.addActivity(gauriKund);
            {
                Leg leg = populationFactory.createLeg("walk");
                plan.addLeg(leg);
            }
            Activity kedarnath = populationFactory.createActivityFromCoord("Kedarnath", location2Coord.get("Kedarnath Dham"));
            kedarnath.setEndTime(96.*3600.+random.nextDouble()*3600.);
            plan.addActivity(kedarnath);
            {
                Leg leg = populationFactory.createLeg("walk");
                plan.addLeg(leg);
            }
            Activity gauriKundReturn = populationFactory.createActivityFromCoord("GauriKund", location2Coord.get("Gauri Kund"));
            gauriKundReturn.setEndTime(105.*3600.+random.nextDouble()*3600.);
            plan.addActivity(gauriKundReturn);
            {
                Leg leg = populationFactory.createLeg("shuttle");
                plan.addLeg(leg);
            }
            Activity sonprayagReturn = populationFactory.createActivityFromCoord("Sonprayag", location2Coord.get("Sonprayag"));;
            sonprayagReturn.setEndTime(106.*3600.+random.nextDouble()*3600.);
            plan.addActivity(sonprayagReturn);
            {
                Leg leg = populationFactory.createLeg("car");
                plan.addLeg(leg);
            }
            Activity badrinath = populationFactory.createActivityFromCoord("Badrinath", location2Coord.get("Badrinath"));
            badrinath.setEndTime(130.*3600.+random.nextDouble()*3600.);
            plan.addActivity(badrinath);
            {
                Leg leg = populationFactory.createLeg("car");
                plan.addLeg(leg);
            }
            plan.addActivity(haridwar);

            person.addPlan(plan);
            population.addPerson(person);
        }

        new PopulationWriter(scenario.getPopulation()).write(matsimPlansFile);

    }

    private void storeLocations(){
        BufferedReader reader = IOUtils.getBufferedReader(importantLocations);
        String line = null;
        boolean header= true;
        try {
            line = reader.readLine();
            if(header) {
                header=false;
                line= reader.readLine();
            }
            while (line!=null ) {
                String[] parts = line.split("\t");
                location2Coord.put(parts[0], DehradunUtils.transformation.transform(
                        new Coord(Double.parseDouble(parts[2]),Double.parseDouble(parts[1]))));
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("File is not read. Reason: " + e);
        }


    }


}
