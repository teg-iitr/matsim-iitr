package playground.amit.mixedTraffic.patnaIndia.covidWork;

import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import javax.inject.Inject;
import javax.inject.Provider;
/**
 * @author amit
 */

public class MyChangeTripMode implements Provider<PlanStrategy> {

    private final GlobalConfigGroup globalConfigGroup;
    private final ChangeModeConfigGroup changeLegModeConfigGroup;
    private Provider<TripRouter> tripRouterProvider;
    private ActivityFacilities activityFacilities;

    @Inject
    protected MyChangeTripMode(GlobalConfigGroup globalConfigGroup, ChangeModeConfigGroup changeLegModeConfigGroup, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
        this.globalConfigGroup = globalConfigGroup;
        this.changeLegModeConfigGroup = changeLegModeConfigGroup;
        this.activityFacilities = activityFacilities;
        this.tripRouterProvider = tripRouterProvider;
    }

    @Override
    public PlanStrategy get() {
        PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new MyRandomPlanSelector<>());
        builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, globalConfigGroup));
        builder.addStrategyModule(new ChangeLegMode(globalConfigGroup, changeLegModeConfigGroup));
        builder.addStrategyModule(new ReRoute(activityFacilities, tripRouterProvider, globalConfigGroup));
        return builder.build();
    }

}
