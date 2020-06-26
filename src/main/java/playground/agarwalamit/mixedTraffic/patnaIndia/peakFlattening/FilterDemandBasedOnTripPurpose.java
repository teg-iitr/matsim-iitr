package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import java.util.*;

public class FilterDemandBasedOnTripPurpose {
    private final Logger logger = Logger.getLogger(FilterDemandBasedOnTripPurpose.class);

    private static final Random random = MatsimRandom.getRandom();
    private final Collection<SimpleFeature> features;
    private final Population existingPop;
    private final String activityType;
    private Map<String, List<Id<Person>>> zoneToPersonIds = new HashMap<>();
    private final GeometryFactory GF = new GeometryFactory();
    public static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(PatnaUtils.EPSG,TransformationFactory.WGS84);

    public FilterDemandBasedOnTripPurpose(String plansFile, String wardFile, String tripPurposeToFiler) {
        this(LoadMyScenarios.loadScenarioFromPlans(plansFile).getPopulation(),wardFile,tripPurposeToFiler);
    }

    public FilterDemandBasedOnTripPurpose(Population population, String wardFile, String tripPurposeToFiler) {
        existingPop = population;
        features = ShapeFileReader.getAllFeatures(wardFile);
        activityType = tripPurposeToFiler;
    }

    public static void main(String[] args) {
        String plansFile = "../../patna/input/baseCaseOutput_plans_June2020.xml.gz";
        String wardFile = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/wardFile/Wards.shp";
        double removalProbability = 0.5;

        FilterDemandBasedOnTripPurpose filterDemandBasedOnTripPurpose = new FilterDemandBasedOnTripPurpose(plansFile, wardFile, "work");
        filterDemandBasedOnTripPurpose.process(removalProbability);
    }

    public void process(double removalProbability){
        if(this.activityType==null ) {
            logger.warn("Trip purpose is not defined for which the persons are filtered. This will not do anything.");
            return;
        }
        logger.info("The population will be filtered based on trip purpose "+this.activityType+" and persons will be removed with a probability of "+removalProbability);
        storeZoneToPersons();
        removePersons(removalProbability);
    }

    private void removePersons(double removalProbability){
        for (String wardNumber : zoneToPersonIds.keySet()) {
            List<Id<Person>> persons = zoneToPersonIds.get(wardNumber);
            double removedPersons = 0.;
            Collections.shuffle(persons);
            for (Id<Person> p : persons) {
                if (random.nextDouble() <= removalProbability) {
                    existingPop.removePerson(p);
                    removedPersons++;
                    logger.info("Person is removed " + p);
                }
            }
            logger.info("Ward "+wardNumber+" had "+persons.size()+" persons of which "+removedPersons+" are removed. Share of removed persons is "+ removedPersons/persons.size());
        }
    }

    private void storeZoneToPersons() {
        for (Person person : existingPop.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (PlanElement pe : plan.getPlanElements()){
                if(pe instanceof Activity && ((Activity)pe).getType().equalsIgnoreCase(activityType)) {
                    Coord cord = ((Activity)pe).getCoord();
                    String wardNumber = getWardNumber(cord);
                    if (wardNumber!=null) zoneToPersonIds.get(wardNumber).add(person.getId());
                    break;
                }
            }
        }
    }

    private String getWardNumber(Coord cord) {
        for (SimpleFeature feature : features) {
            Geometry geom =   (Geometry) feature.getDefaultGeometry();
            Coord cord_t = ct.transform(cord);
            if (geom.contains(GF.createPoint(new Coordinate(cord_t.getX(), cord_t.getY())))) {
                String wardNumber = String.valueOf((Integer) feature.getAttribute(PatnaUtils.WARD_TAG_SHAPE_FILE));
                zoneToPersonIds.putIfAbsent(wardNumber, new ArrayList<>());
                return wardNumber;
            }
        }
        logger.warn("No ward found for the coordinates: "+cord); // possible for external trips.
        return null;
    }
}
