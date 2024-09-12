package playground.shivam.Dadar.evacuation.population;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import playground.shivam.Dadar.evacuation.DadarUtils;
import playground.shivam.Dadar.evacuation.network.CreateEvacuationNetworkFromMatsimNetwork;

import java.util.Collection;
import java.util.List;

import static playground.shivam.Dadar.evacuation.DadarUtils.EVACUATION_PLANS;
import static playground.shivam.Dadar.evacuation.DadarUtils.SAFE_POINTS;

public class CreateEvacuationPopulationFromMatsimPopulation {
    public static void createDadarEvacPlans(Scenario scenarioPlans, Scenario scenarioNetwork) {
        Geometry evacuationArea = CreateEvacuationNetworkFromMatsimNetwork.getEvacuationArea();
        Collection<Id<Node>> safeNodeAIds = CreateEvacuationNetworkFromMatsimNetwork.getSafeNodeAIds();

        Population dadarPop = scenarioPlans.getPopulation();

        Population evacPop = scenarioNetwork.getPopulation();
        PopulationFactory popFact = evacPop.getFactory();

        Person evacPerson;
        for (Person person : dadarPop.getPersons().values()) {
            PlanElement actPe = person.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
            Activity originExisting = (Activity) actPe;
            Coord originExistingCoord = originExisting.getCoord();

            Link link = NetworkUtils.getNearestLink(scenarioNetwork.getNetwork(), originExisting.getCoord());
            Activity origin = popFact.createActivityFromLinkId(originExisting.getType(), link.getId());

            //check if the person is in the area shape, if not leave them out
            if (originExistingCoord != null && !evacuationArea.contains(MGC.coord2Point(originExistingCoord)))
                continue;

            // also exclude any origin activity starting on link which is not included in evac network
            if (!scenarioNetwork.getNetwork().getLinks().containsKey(origin.getLinkId()))
                continue;

            // should skip when coord is one of the safeNodeAId
            if (safeNodeAIds.contains(link.getToNode().getId()))
                continue;

            evacPerson = popFact.createPerson(person.getId());
            for (Id<Link> safeLinkIdFromSafePoint : SAFE_POINTS.keySet()) {
                Plan planOut = popFact.createPlan();

                planOut.addActivity(origin);
                // TODO: check in simulation
                origin.setEndTime(9 * 3600);

                PlanElement legPe = person.getSelectedPlan().getPlanElements().get(1);
                Leg leg = popFact.createLeg(((Leg) legPe).getMode());
                planOut.addLeg(leg);

                Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkIdFromSafePoint);
                planOut.addActivity(evacAct);

                evacPerson.addPlan(planOut);
                if (DadarUtils.MAIN_MODES.contains(leg.getMode())) {
                    TripRouter.Builder builder = new TripRouter.Builder(scenarioNetwork.getConfig());
                    builder.setRoutingModule(
                            leg.getMode(),
                            DefaultRoutingModules.createPureNetworkRouter(
                                    leg.getMode(),
                                    popFact,
                                    scenarioNetwork.getNetwork(),
                                    new DijkstraFactory().createPathCalculator(scenarioNetwork.getNetwork(),
                                            new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()),
                                            new FreeSpeedTravelTime())
                            )
                    );
                    List<? extends PlanElement> routeInfo = builder.build().calcRoute(
                            leg.getMode(),
                            FacilitiesUtils.toFacility(origin, null),
                            FacilitiesUtils.toFacility(evacAct, null),
                            origin.getEndTime().seconds(),
                            evacPerson,
                            new Attributes());

                    Route route = ((Leg) routeInfo.get(0)).getRoute();
                    route.setStartLinkId(origin.getLinkId());
                    route.setEndLinkId(evacAct.getLinkId());

                    leg.setRoute(route);
                    leg.setTravelTime(((Leg) routeInfo.get(0)).getTravelTime().seconds());
                } else
                    continue;
            }
            evacPop.addPerson(evacPerson);

        }
        new PopulationWriter(evacPop).write(EVACUATION_PLANS);
    }
}
