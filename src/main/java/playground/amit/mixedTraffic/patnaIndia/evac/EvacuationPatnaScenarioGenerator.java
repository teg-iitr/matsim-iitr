/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.amit.mixedTraffic.patnaIndia.evac;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuationgui.model.config.EvacuationConfigModule;
import org.matsim.evacuationgui.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.evacuationgui.utils.ScenarioCRSTransformation;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.opengis.feature.simple.SimpleFeature;

import playground.amit.analysis.modalShare.ModalShareFromEvents;
import playground.amit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.amit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.amit.mixedTraffic.patnaIndia.input.urban.UrbanDemandGenerator;
import playground.amit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.amit.utils.FileUtils;
import playground.amit.utils.LoadMyScenarios;
import playground.amit.utils.geometry.GeometryUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;

/**
 * @author amit
 */

public class EvacuationPatnaScenarioGenerator {

    private final String dir = FileUtils.RUNS_SVN + "/patnaIndia/run109/";

    private final String networkFile = "input/input.evacTest/network_7759.xml.gz";
    private final String outNetworkFile = "network_patna_evac_7759.xml.gz";
    private final String populationFile = "input/input.evacTest/baseCaseOutput_plans.xml.gz";
    private final String outPopFile = "plans_patna_evac_7759.xml.gz";

    private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Patna");
    private Scenario scenario;
    private Geometry evavcuationArea;

    public static void main(String[] args) {
        new EvacuationPatnaScenarioGenerator().run();
    }

    void run() {
        scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
        createEvacNetwork(scenario);

        createEvacPopulation(LoadMyScenarios.loadScenarioFromPlans(populationFile));

        createConfig();
    }

    private void createConfig() {
        Config config = scenario.getConfig();
        config.network().setInputFile(outNetworkFile);
        config.plans().setInputFile(outPopFile);
        config.controler().setOutputDirectory("output/evacTest");

        //config.addModule(new EvacuationConfigModule("evacuation"));

        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(100);
        config.controler().setMobsim("qsim");
        config.controler().setWriteEventsInterval(20);
        config.controler().setWritePlansInterval(20);


        config.global().setCoordinateSystem("EPSG:7759");
        ScenarioCRSTransformation.transform(this.scenario, "EPSG:7759");
        config.travelTimeCalculator().setTraveltimeBinSize(900);

        config.qsim().setSnapshotPeriod(5 * 60);
        config.qsim().setEndTime(30 * 3600);
        config.qsim().setStuckTime(100000);
        config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
        config.qsim().setMainModes(PatnaUtils.EXT_MAIN_MODES);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);

        StrategySettings expChangeBeta = new StrategySettings();
        expChangeBeta.setStrategyName("ChangeExpBeta");
        expChangeBeta.setWeight(0.9);

        StrategySettings reRoute = new StrategySettings();
        reRoute.setStrategyName("ReRoute");
        reRoute.setWeight(0.1);

        config.strategy().setMaxAgentPlanMemorySize(5);
        config.strategy().addStrategySettings(expChangeBeta);
        config.strategy().addStrategySettings(reRoute);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

        //vsp default
        config.vspExperimental().addParam("vspDefaultsCheckingLevel", "ignore");
        config.plans().setRemovingUnneccessaryPlanAttributes(true);
        config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);
        //vsp default

        ActivityParams homeAct = new ActivityParams("home");
        homeAct.setTypicalDuration(1 * 3600);
        config.planCalcScore().addActivityParams(homeAct);

        ActivityParams evacAct = new ActivityParams("evac");
        evacAct.setTypicalDuration(1 * 3600);
        config.planCalcScore().addActivityParams(evacAct);

        config.plansCalcRoute().setNetworkModes(PatnaUtils.EXT_MAIN_MODES);
        config.planCalcScore().addModeParams(new PlanCalcScoreConfigGroup.ModeParams("motorbike"));
        config.planCalcScore().addModeParams(new PlanCalcScoreConfigGroup.ModeParams("truck"));
        {
            ModeRoutingParams mrp = new ModeRoutingParams("walk");
            mrp.setTeleportedModeSpeed(4. / 3.6);
            mrp.setBeelineDistanceFactor(1.5);
            config.plansCalcRoute().addModeRoutingParams(mrp);
        }
        {
            ModeRoutingParams mrp = new ModeRoutingParams("pt");
            mrp.setTeleportedModeSpeed(20. / 3.6);
            mrp.setBeelineDistanceFactor(1.5);
            config.plansCalcRoute().addModeRoutingParams(mrp);
        }

        String outputDir = config.controler().getOutputDirectory()+"/evac_"+ config.qsim().getLinkDynamics().name()+"/";
        config.controler().setOutputDirectory(outputDir);
        config.controler().setDumpDataAtEnd(true);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.vspExperimental().setWritingOutputEvents(true);
        this.scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        PatnaVehiclesGenerator.createAndAddVehiclesToScenario(this.scenario, PatnaUtils.EXT_MAIN_MODES);

        final Controler controler = new Controler(this.scenario);

//		final RandomizingTimeDistanceTravelDisutilityFactory builder_bike =  new RandomizingTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                addTravelTimeBinding("bike").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("bike").to(carTravelDisutilityFactoryKey());

                addTravelTimeBinding("motorbike").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());
            }
        });

        controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
            @Override
            public void install() {
                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        controler.run();

        new File(outputDir+"/analysis/").mkdir();
        String outputEventsFile = outputDir+"/output_events.xml.gz";
        // write some default analysis
        String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
        ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile);
        mtta.run();
        mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

        ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
        msc.run();
        msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

        String outConfigFile = "config_patna_evac_7759.xml.gz";
        new ConfigWriter(config).write("input/input.evacTest/" + outConfigFile);
    }

    private void createEvacNetwork(Scenario sc) {


        //read shape file and get area
        ShapeFileReader reader = new ShapeFileReader();
        // String areaShapeFile = "area_epsg24345.shp";
        Collection<SimpleFeature> features = reader.readFileAndInitialize("input/input.evacTest/patna_polygon_7759.shp");
        evavcuationArea = (Geometry) features.iterator().next().getDefaultGeometry();
        // GeometryUtils.getGeometryFromListOfFeatures()

        // will create a network connecting with safe node.
        // Amit, I added this cast to prevent compilation errors.
        // Preferably, evacuationgui needs to be adapted to the more recent version of geotools. michal mar'19
        EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(sc, evavcuationArea, safeLinkId);
        net.run();

        //since the original network is multi-mode, the new links should also allow all modes
        for (Link l : sc.getNetwork().getLinks().values()) {
            Set<String> allowedModes = new HashSet<>(PatnaUtils.ALL_MAIN_MODES);
            l.setAllowedModes(allowedModes);
        }

        new NetworkWriter(sc.getNetwork()).write("input/input.evacTest/" + outNetworkFile);
    }

    private void createEvacPopulation(Scenario sc) {
        Set<Coord> handled = new HashSet<>();
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(PatnaUtils.EPSG, "EPSG:7759");

        for (Person pers : sc.getPopulation().getPersons().values()) {
            for (Plan pl : pers.getPlans()) {
                for (PlanElement el : pl.getPlanElements()) {
                    if (el instanceof Activity) {
                        Activity act = (Activity) el;
                        Coord c = act.getCoord();
                        if (handled.contains(c)) {
                            continue;
                        }
                        handled.add(c);
                        if (c!=null) {
                            Coord cc = ct.transform(c);

//                        c.setXY(cc.getX(), cc.getY());
                            act.setCoord(cc);
                        }
                    }
                }
            }
        }
        // population, (home & evac)
        Population popOut = scenario.getPopulation();
        PopulationFactory popFact = popOut.getFactory();
        Population regularPop = sc.getPopulation();

        Person pOut = null;
        for (Person p : regularPop.getPersons().values()) {
            if (p.getId().toString().contains("lum")) {
                PlanElement actPe = p.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
                Activity homeExisting = (Activity) actPe;
                Link link = null;
                Coord actCoord = homeExisting.getCoord();

                    link = NetworkUtils.getNearestLink(scenario.getNetwork(), homeExisting.getCoord());
                    Activity home = popFact.createActivityFromLinkId(homeExisting.getType(), link.getId());

                    //check if the person is in the area shape, if not leave them out

                    if (actCoord != null && !evavcuationArea.contains(MGC.coord2Point(actCoord))) {
                        continue;
                    }

                    // also exclude any home activity starting on link which is not included in evac network
                    if (!scenario.getNetwork().getLinks().containsKey(home.getLinkId())) {
                        continue;
                    }

                    pOut = popFact.createPerson(p.getId());
                    Plan planOut = popFact.createPlan();
                    pOut.addPlan(planOut);

                    planOut.addActivity(home);
                    home.setEndTime(9 * 3600);

                    PlanElement legPe = p.getSelectedPlan().getPlanElements().get(1);
                    Leg leg = popFact.createLeg(((Leg) legPe).getMode());
                    planOut.addLeg(leg);

                    Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkId);
                    planOut.addActivity(evacAct);

                    if (PatnaUtils.URBAN_MAIN_MODES.contains(leg.getMode())) {
                        TripRouter.Builder builder = new TripRouter.Builder(scenario.getConfig());
                        builder.setRoutingModule(
                                leg.getMode(),
                                DefaultRoutingModules.createPureNetworkRouter(
                                        leg.getMode(),
                                        popFact,
                                        scenario.getNetwork(),
                                        new DijkstraFactory().createPathCalculator(scenario.getNetwork(),
                                                new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()),
                                                new FreeSpeedTravelTime())
                                )
                        );
                        List<? extends PlanElement> routeInfo = builder.build().calcRoute(
                                leg.getMode(),
                                FacilitiesUtils.toFacility(home, null),
                                FacilitiesUtils.toFacility(evacAct, null),
                                home.getEndTime().seconds(),
                                pOut,
                                new Attributes());

                        Route route = ((Leg) routeInfo.get(0)).getRoute();
                        route.setStartLinkId(home.getLinkId());
                        route.setEndLinkId(evacAct.getLinkId());

                        leg.setRoute(route);
                        leg.setTravelTime(((Leg) routeInfo.get(0)).getTravelTime().seconds());
                    }
                }
                else {

                    continue;
                    //probably, re-create home and evac activities with coord here to include them in simulation.
                    //				ModeRouteFactory routeFactory = new ModeRouteFactory();
                    //				routeFactory.setRouteFactory(leg.getMode(), new GenericRouteFactory());
                    //
                    //				TripRouter router = new TripRouter();
                    //				router.setRoutingModule(leg.getMode(), DefaultRoutingModules.createTeleportationRouter(leg.getMode(), popFact, scOut.getConfig().plansCalcRoute().getModeRoutingParams().get(leg.getMode())));
                    //				List<? extends PlanElement> routeInfo = router.calcRoute(leg.getMode(), new ActivityWrapperFacility(home), new ActivityWrapperFacility(evacAct), home.getEndTime(), pOut);
                    //
                    //				Route route = ((Leg)routeInfo.get(0)).getRoute();
                    ////				Route route = routeFactory.createRoute(leg.getMode(), home.getLinkId(), evacAct.getLinkId());
                    //				leg.setRoute(route);
                    //				leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());
                }
                popOut.addPerson(pOut);

        }
        new PopulationWriter(popOut).write("input/input.evacTest/" + outPopFile);
    }
}
