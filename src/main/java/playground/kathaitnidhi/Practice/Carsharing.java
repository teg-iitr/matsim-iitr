package playground.kathaitnidhi.Practice;


import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;
import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.config.groups.FacilitiesConfigGroup;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemandImpl;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgentImpl;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseTheCompanyExample;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.ChooseVehicleTypeExample;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.qsim.CarSharingQSimModule;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

import playground.kathaitnidhi.config.FreeFloatingConfigGroup;
import playground.kathaitnidhi.config.OneWayCarsharingConfigGroup;
import playground.kathaitnidhi.config.TwoWayCarsharingConfigGroup;

import playground.kathaitnidhi.testcases.MatsimTestUtils;

public class Carsharing {

	private final static Logger log = Logger.getLogger(Carsharing.class);



	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void test() {
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "/config.xml",
				new FreeFloatingConfigGroup(),
				new OneWayCarsharingConfigGroup(),
				new TwoWayCarsharingConfigGroup(),
				new CarsharingConfigGroup(),
				new DvrpConfigGroup());
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("10persons.xml");
		config.facilities().setInputFile("facilities.xml");
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);
		
		config.plansCalcRoute().setAccessEgressType(AccessEgressType.none);
		config.plansCalcRoute().setRoutingRandomness(0.);
		
		CarsharingConfigGroup csConfig = (CarsharingConfigGroup) config.getModule(CarsharingConfigGroup.GROUP_NAME);
		csConfig.setvehiclelocations(utils.getClassInputDirectory() + "/CarsharingStations.xml");
		csConfig.setmembership(utils.getClassInputDirectory() + "/CSMembership.xml");
		
		config.subtourModeChoice().setBehavior(SubtourModeChoice.Behavior.fromAllModesToSpecifiedModes);
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.);

		{
			ModeRoutingParams params = new ModeRoutingParams(TransportMode.non_network_walk);
			params.setTeleportedModeSpeed(0.83333333333);
			//			params.setTeleportedModeSpeed( 2.0 );
			params.setBeelineDistanceFactor(1.3);
			config.plansCalcRoute().addModeRoutingParams(params);
		}
		{
			config.plansCalcRoute().removeModeRoutingParams(TransportMode.walk);
			ModeRoutingParams params = new ModeRoutingParams(TransportMode.walk);
			params.setTeleportedModeSpeed(0.83333333333);
			//			params.setTeleportedModeSpeed( 2.0 );
			params.setBeelineDistanceFactor(1.3);
			config.plansCalcRoute().addModeRoutingParams(params);
		}
		
		
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		config.plansCalcRoute().setAccessEgressType(AccessEgressType.accessEgressModeToLink);
		// ---

				final Controler controler = new Controler(scenario);
				//		controler.setDirtyShutdown(true);

				Carsharing.installCarSharing(controler);

				final MyAnalysis myAnalysis = new MyAnalysis();
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						this.bind(MyAnalysis.class).toInstance(myAnalysis);
						this.addControlerListenerBinding().toInstance(myAnalysis);
					}
				});

				// ---

				controler.run();

				log.info("done");
			}

		

			private static void installCarSharing(Controler controler) {
		// TODO Auto-generated method stub

					final Scenario scenario = controler.getScenario();
					TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
					Set<String> modes = new HashSet<>();
					modes.add("car");
					Network networkFF = NetworkUtils.createNetwork();
					filter.filter(networkFF, modes);
					CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(networkFF);

					final CarsharingConfigGroup configGroup = (CarsharingConfigGroup) scenario.getConfig()
							.getModule(CarsharingConfigGroup.GROUP_NAME);

					reader.readFile(configGroup.getvehiclelocations());

					Set<String> carsharingCompanies = reader.getCompanies().keySet();

					MembershipReader membershipReader = new MembershipReader();

					membershipReader.readFile(configGroup.getmembership());

					final MembershipContainer memberships = membershipReader.getMembershipContainer();

					final CostsCalculatorContainer costsCalculatorContainer = CarsharingUtils
							.createCompanyCostsStructure(carsharingCompanies);

					final CarsharingListener carsharingListener = new CarsharingListener();
					// final CarsharingSupplyInterface carsharingSupplyContainer = new
					// CarsharingSupplyContainer(controler.getScenario());

					final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
					final ChooseTheCompany chooseCompany = new ChooseTheCompanyExample();
					final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
					final RouterProvider routerProvider = new RouterProviderImpl();
					final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemandImpl(networkFF);
					// final CarsharingManagerInterface carsharingManager = new
					// CarsharingManagerNew();
					final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
					final VehicleChoiceAgent vehicleChoiceAgent = new VehicleChoiceAgentImpl();
					// ===adding carsharing objects on supply and demand infrastructure ===
					controler.addOverridingQSimModule(new CarSharingQSimModule());
					controler.addOverridingModule(new DvrpTravelTimeModule());
					controler.configureQSimComponents(CarSharingQSimModule::configureComponents);

					controler.addOverridingModule(new AbstractModule() {

						@Override
						public void install() {
							bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
							bind(ChooseTheCompany.class).toInstance(chooseCompany);
							bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
							bind(RouterProvider.class).toInstance(routerProvider);
							bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);
							bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
							bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
							bind(MembershipContainer.class).toInstance(memberships);
							bind(CarsharingSupplyInterface.class).to((Class<? extends CarsharingSupplyInterface>) CarsharingSupplyContainer.class);
							bind(CarsharingManagerInterface.class).to(CarsharingManagerNew.class);
							bind(VehicleChoiceAgent.class).toInstance(vehicleChoiceAgent);
							bind(DemandHandler.class).asEagerSingleton();
							bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
									.to(Network.class);

							bind(Network.class).annotatedWith(Names.named("carnetwork")).toInstance(networkFF);
							bind(TravelTime.class).annotatedWith(Names.named("ff"))
									.to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
						}

					});

					// === carsharing specific replanning strategies ===

					controler.addOverridingModule(new AbstractModule() {
						@Override
						public void install() {
							this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to((Class<? extends PlanStrategy>) RandomTripToCarsharingStrategy.class);
							this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to(CarsharingSubtourModeChoiceStrategy.class);
						}
					});

					// === adding qsimfactory, controller listeners and event handlers
					controler.addOverridingModule(new AbstractModule() {
						@Override
						public void install() {
							addControlerListenerBinding().toInstance(carsharingListener);
							addControlerListenerBinding().to(CarsharingManagerNew.class);
							// bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
							addEventHandlerBinding().to((Class<? extends EventHandler>) PersonArrivalDepartureHandler.class);
							addEventHandlerBinding().to((Class<? extends EventHandler>) DemandHandler.class);
						}
					});
					// === adding carsharing specific scoring factory ===
					controler.addOverridingModule(new AbstractModule() {

						@Override
						public void install() {

							bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
						}
					});

					// === routing moduels for carsharing trips ===

					controler.addOverridingModule(CarsharingUtils.createRoutingModule());
		
	}



			static class MyAnalysis implements AfterMobsimListener {

				@Inject private LegHistogram histogram;

				void testOutput(int iteration) {
					int nofLegs = 0;
					for (int nofDepartures : this.histogram.getDepartures()) {
						nofLegs += nofDepartures;
					}
					log.info("number of legs:\t" + nofLegs + "\t100%");

					for (String legMode : this.histogram.getLegModes()) {
						int nOfModeLegs = 0;
						for (int nofDepartures : this.histogram.getDepartures(legMode)) {
							nOfModeLegs += nofDepartures;
						}
						if (iteration == 0) {
							if (TransportMode.walk.equals(legMode)) {
								// walk is used for access+egress to car
								// -> number of walk legs for access+egress equals twice the number of car legs = 44
								Assert.assertEquals(44, nOfModeLegs);
							} else if ("oneway_vehicle".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							} else if (TransportMode.car.equals(legMode)) {
								Assert.assertEquals(22, nOfModeLegs);
							} else if ("egress_walk_ow".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							} else if ("access_walk_ow".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							}
						} else if (iteration == 10) {

							if (TransportMode.walk.equals(legMode)) {

								Assert.assertEquals(2, nOfModeLegs);
							} else if ("bike".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);
							} else if (TransportMode.car.equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							} else if ("twoway_vehicle".equals(legMode)) {

								Assert.assertEquals(6, nOfModeLegs);

							} else if ("oneway_vehicle".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);

							} else if ("egress_walk_ow".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							} else if ("access_walk_ow".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							} else if ("egress_walk_tw".equals(legMode)) {
								Assert.assertEquals(3, nOfModeLegs);
							} else if ("access_walk_tw".equals(legMode)) {
								Assert.assertEquals(3, nOfModeLegs);
							} else if ("egress_walk_ff".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);
							} else if ("access_walk_ff".equals(legMode)) {
								Assert.assertEquals(0, nOfModeLegs);
							}
						} else if (iteration == 20) {
							if (TransportMode.walk.equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);
							} else if ("bike".equals(legMode)) {
								Assert.assertEquals(4, nOfModeLegs);
							} else if ("twoway_vehicle".equals(legMode)) {
								Assert.assertEquals(4, nOfModeLegs);
							} else if ("freefloating_vehicle".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);
							} else if ("egress_walk_tw".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);
							} else if ("access_walk_tw".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);
							} else if ("access_walk_ff".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);

							} else if ("egress_walk_ff".equals(legMode)) {
								Assert.assertEquals(2, nOfModeLegs);

							}
						}
					}

				}

				@Override
				public void notifyAfterMobsim(AfterMobsimEvent event) {
					testOutput(event.getIteration());
				}
		
		
		
}
}
