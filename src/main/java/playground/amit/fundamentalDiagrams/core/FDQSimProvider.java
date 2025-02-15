package playground.amit.fundamentalDiagrams.core;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.shivam.trafficChar.TrafficCharQSimModule;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.util.Map;
import java.util.stream.Collectors;

public class FDQSimProvider implements Provider<Mobsim> {
	
	public static final String PERSON_MODE_ATTRIBUTE_KEY = "travelMode";
	
	private final Scenario scenario;
	private final EventsManager events;
//  QNetworkFactory should only be bound via AbstractQSimModule AA, Apr22
//	private final QNetworkFactory qnetworkFactory;
	private final Map<String, VehicleType> modeToVehicleTypes ;
	private final FDNetworkGenerator fdNetworkGenerator;
	private final FDStabilityTester stabilityTester;
	
	@Inject
	private FDQSimProvider(Scenario scenario, EventsManager events,
//						   QNetworkFactory qnetworkFactory,
						   FDNetworkGenerator fdNetworkGenerator, FDStabilityTester stabilityTester) {
		this.scenario = scenario;
		this.events = events;
//		this.qnetworkFactory = qnetworkFactory;
		this.modeToVehicleTypes = this.scenario.getVehicles()
											   .getVehicleTypes()
											   .entrySet()
											   .stream()
											   .collect(Collectors.toMap(e -> e.getKey().toString(),
													   Map.Entry::getValue));
		this.fdNetworkGenerator = fdNetworkGenerator;
		this.stabilityTester = stabilityTester;
	}
	
	@Override
	public Mobsim get() {
		final QSim qSim;
		if (scenario.getConfig().getModules().containsKey(TrafficCharConfigGroup.GROUP_NAME))
			qSim = new QSimBuilder(scenario.getConfig()) //
					.useDefaults() //
					.addOverridingQSimModule(new TrafficCharQSimModule())
					.configureQSimComponents( components -> {
							components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
						} )
					.build(scenario, events);
		else
			qSim = new QSimBuilder(scenario.getConfig()) //
					.useDefaults() //
					.configureQSimComponents( components -> {
						components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
					} )
					.build(scenario, events);

		FDModule.LOG.info("=======================");
		FDModule.LOG.info("Mobsim agents' are directly added to AgentSource.");
		FDModule.LOG.info("=======================");

		AgentSource agentSource = () -> {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				String travelMode = (String) person.getAttributes().getAttribute(PERSON_MODE_ATTRIBUTE_KEY);
				double randDouble = MatsimRandom.getRandom().nextDouble();
				double actEndTime = randDouble * FDModule.MAX_ACT_END_TIME;

				FDTrackMobsimAgent agent = new FDTrackMobsimAgent(person.getId(), actEndTime, travelMode, fdNetworkGenerator);
				agent.setStabilityTester(stabilityTester);
				qSim.insertAgentIntoMobsim(agent);

//					AttributableVehicle attributableVehicle = new AttributableVehicle(Id.create(agent.getId(), Vehicle.class), modeToVehicleTypes.get(travelMode));
				Vehicle attributableVehicle = VehicleUtils.createVehicle(Id.create(agent.getId(), Vehicle.class), modeToVehicleTypes.get(travelMode));
				final QVehicle vehicle = new QVehicleImpl(
//							VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), modeToVehicleTypes.get(travelMode))
						attributableVehicle
				);
				vehicle.setDriver(agent);
				scenario.getVehicles().removeVehicle(vehicle.getId());
				scenario.getVehicles().addVehicle(vehicle.getVehicle());
				agent.setVehicle(vehicle);
				final Id<Link> linkId4VehicleInsertion = fdNetworkGenerator.getTripDepartureLinkId();
//					qSim.createAndParkVehicleOnLink(vehicle.getVehicle(), linkId4VehicleInsertion);
				qSim.addParkedVehicle(vehicle, linkId4VehicleInsertion);
			}
		};

		qSim.addAgentSource(agentSource);

		if ( FDModule.isUsingLiveOTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
			OTFClientLive.run(scenario.getConfig(), server);
		}
		return qSim;
	}
}
