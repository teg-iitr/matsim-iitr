package playground.agarwalamit.mixedTraffic.patnaIndia.peakFlattening;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class WorkFromHomeModule extends AbstractModule {
        private final String wfhStrategyName;
        private final String wfhMode;

        public WorkFromHomeModule(String wfhStrategyName, String wfhMode) {
            this.wfhStrategyName = wfhStrategyName;
            this.wfhMode = wfhMode;
        }

        @Override
        public void install() {
            addPlanStrategyBinding(wfhStrategyName).toProvider(new Provider<PlanStrategy>() {
                @Inject
                Scenario sc;
                @Inject
                Provider<TripRouter> tripRouterProvider ;

                @Override
                public PlanStrategy get() {
                    final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
                    builder.addStrategyModule(new WorkFromHome(sc.getConfig().global(), List.of("work","educational"), wfhMode));
                    builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
                    return builder.build();
                }
            });
        }

    private static class WorkFromHome extends AbstractMultithreadedModule {

        private final List<String> tripPurposes;
        private final String wfhMode;

        public WorkFromHome(GlobalConfigGroup globalConfigGroup, List<String> activityTypesForWFH, String wfhMode) {
            super(globalConfigGroup);
            tripPurposes = activityTypesForWFH;
            this.wfhMode = wfhMode;
        }

        @Override
        public PlanAlgorithm getPlanAlgoInstance() {
            return new PlanAlgorithm() {
                @Override
                public void run(Plan plan) {
                    List<PlanElement> pes = plan.getPlanElements();
                    Activity secondAct = (Activity)pes.get(2);
                    if (tripPurposes.contains(secondAct.getType())) {
                        Activity firstActivity = (Activity) pes.get(0);
                        Leg firstLeg = ((Leg)pes.get(1));
                        firstLeg.setRoute(null);
                        firstLeg.setMode(wfhMode);
                        secondAct.setLinkId(firstActivity.getLinkId());
                        secondAct.setCoord(firstActivity.getCoord());
                        secondAct.setFacilityId(firstActivity.getFacilityId());
                        Leg secondLeg = ((Leg)pes.get(3));
                        secondLeg.setRoute(null);
                        secondLeg.setMode(wfhMode);
                    }
                }
            };
        }
    }
}


