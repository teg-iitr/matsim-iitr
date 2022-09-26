package playground.amit.mixedTraffic.patnaIndia.covidWork;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.amit.utils.LoadMyScenarios;
import java.util.*;
/**
 * @author amit
 */

public class FilterDemandBasedOnTripPurpose {
    private final Logger logger = LogManager.getLogger(FilterDemandBasedOnTripPurpose.class);

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
        storeZoneToPersons();
    }

    public static void main(String[] args) {
        String plansFile = "../../patna/input/baseCaseOutput_plans_June2020.xml.gz";
        String wardFile = "C:/Users/Amit Agarwal/Google Drive/iitr_gmail_drive/project_data/patna/wardFile/Wards.shp";
        double removalProbability = 0.5;

//        FilterDemandBasedOnTripPurpose filterDemandBasedOnTripPurpose = new FilterDemandBasedOnTripPurpose(plansFile, wardFile, "work");
//        filterDemandBasedOnTripPurpose.removePersons(removalProbability);

        FilterDemandBasedOnTripPurpose filterDemandBasedOnTripPurpose = new FilterDemandBasedOnTripPurpose(plansFile, wardFile, "educational");
        filterDemandBasedOnTripPurpose.shiftDepartureTime(0.5, new Tuple<>(5*3600.,5400), new Tuple<>(12*3600.,5400)); //adjusting time between 6 to 7 and 12 to 13
    }

    public void shiftDepartureTime(double shiftingProbability, Tuple<Double, Integer> morningShiftStart_RandomPeriod, Tuple<Double, Integer> afternoonShiftStart_RandomPeriod){
        for(String wardNumber : zoneToPersonIds.keySet()){
            List<Id<Person>> persons = zoneToPersonIds.get(wardNumber);
            Collections.shuffle(persons);

            List<Id<Person>> personsMorningShift = new ArrayList<>();
            List<Id<Person>> personsAfternoonShift = new ArrayList<>();

            for (Id<Person> p : persons) {
                if (random.nextDouble() <= shiftingProbability) {
                    personsMorningShift.add(p);
                } else {
                    personsAfternoonShift.add(p);
                }
            }
            ShiftPersonDepartureTime(morningShiftStart_RandomPeriod, personsMorningShift);
            ShiftPersonDepartureTime(afternoonShiftStart_RandomPeriod, personsAfternoonShift);

            logger.info("Ward "+wardNumber+" had "+persons.size()+" persons of which "+personsMorningShift.size()+" persons are departing between "
                    +morningShiftStart_RandomPeriod.getFirst()+" to "+morningShiftStart_RandomPeriod.getFirst()+morningShiftStart_RandomPeriod.getSecond()/3600.
                    +" and "+personsAfternoonShift.size()+" are departing between "+afternoonShiftStart_RandomPeriod.getSecond()
                    +" to "+afternoonShiftStart_RandomPeriod.getFirst()+afternoonShiftStart_RandomPeriod.getSecond()/3600.+".");
        }
    }

    private void ShiftPersonDepartureTime(Tuple<Double, Integer> afternoonShiftStart_RandomPeriod, List<Id<Person>> persons) {
        for(Id<Person> p : persons) {
            Person person = existingPop.getPersons().get(p);
            for (Plan plan : person.getPlans()) {
                Activity home = ((Activity) plan.getPlanElements().get(0));
                //TODO one can think of excluding persons who are departing before 6.
                Activity purpose = ((Activity) plan.getPlanElements().get(2)); //education
                double duration = Math.max(purpose.getEndTime().seconds() - home.getEndTime().seconds(), 6.*3600.); //duration capped by 6 hours
                double newActEndTime = afternoonShiftStart_RandomPeriod.getFirst() + random.nextInt(afternoonShiftStart_RandomPeriod.getSecond());
                home.setEndTime(newActEndTime);
                purpose.setEndTime(newActEndTime + duration);
                // remove routes
                ((Leg) plan.getPlanElements().get(1)).setRoute(null);
                ((Leg) plan.getPlanElements().get(3)).setRoute(null);
            }
        }
    }

    public void removePersons(double removalProbability){
        if(this.activityType==null ) {
            logger.warn("Trip purpose is not defined for which the persons are filtered. This will not do anything.");
            return;
        }
        logger.info("The population will be filtered based on trip purpose "+this.activityType+" and persons will be removed with a probability of "+removalProbability);


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
