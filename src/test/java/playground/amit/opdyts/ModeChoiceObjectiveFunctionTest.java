//package playground.amit.opdyts;
//
//import static playground.amit.opdyts.ObjectiveFunctionEvaluator.*;
//import java.io.PrintStream;
//import java.util.Map;
//import java.util.TreeMap;
//import floetteroed.opdyts.ObjectiveFunction;
//import floetteroed.opdyts.SimulatorState;
//import org.junit.Rule;
//import org.junit.Test;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.DefaultActivityTypes;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.api.core.v01.population.PopulationFactory;
//import org.matsim.contrib.opdyts.MATSimState;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.AbstractModule;
//import org.matsim.core.controler.ControlerDefaultsModule;
//import org.matsim.core.controler.Injector;
//import org.matsim.core.controler.NewControlerModule;
//import org.matsim.core.controler.OutputDirectoryHierarchy;
//import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
//import org.matsim.core.scenario.ScenarioByInstanceModule;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.io.IOUtils;
//import org.matsim.testcases.MatsimTestUtils;
//
//public class ModeChoiceObjectiveFunctionTest {
//	private static final Logger log = LogManager.getLogger(ModeChoiceObjectiveFunctionTest.class ) ;
//
//	@Rule
//	public final MatsimTestUtils helper = new MatsimTestUtils();
//
//	@Test
//	public void test() throws Exception {
//
//		PrintStream writer = IOUtils.getPrintStream(IOUtils.getFileUrl(helper.getOutputDirectory()+"results.txt"));
//
//		for ( int abc=1 ; abc < 99 ; abc ++ ) {
//
//			DistanceDistribution distriInfo = new DistanceDistribution() {
//				@Override
//				public double[] getDistClasses() {
//					return new double[]{0.};
//				}
//
//				@Override
//				public Map<String, double[]> getMode2DistanceBasedLegs() {
//					Map<String, double[]> map = new TreeMap<>();
//					map.put(TransportMode.car, new double[]{60.});
//					map.put(TransportMode.bike, new double[]{40.});
//					return map;
//				}
//
//				@Override
//				public OpdytsScenario getOpdytsScenario() {
//					return OpdytsScenario.PATNA_1Pct;
//				}
//			};
//			ObjectiveFunctionType objectiveFunctionType =
//					ObjectiveFunctionType.SUM_SQR_DIFF_NORMALIZED ;
//			ModeChoiceObjectiveFunction objective = new ModeChoiceObjectiveFunction(distriInfo);
//
//			final Config config = ConfigUtils.createConfig();
//			config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
//			Scenario scenario = ScenarioUtils.createScenario(config);
//			Population pop = scenario.getPopulation();
//			PopulationFactory pf = pop.getFactory();
//			for (int ii = 0; ii < 100; ii++) {
//				Person person = pf.createPerson(Id.createPersonId(ii));
//				{
//					Plan plan = pf.createPlan();
//					{
//						Activity act = pf.createActivityFromCoord(DefaultActivityTypes.home, new Coord(0., 0.));
//						plan.addActivity(act);
//					}
//					{
//						Leg leg = pf.createLeg(TransportMode.car);
//						if (ii < abc) {
//							leg = pf.createLeg(TransportMode.bike);
//						}
//						plan.addLeg(leg);
//					}
//					{
//						Activity act = pf.createActivityFromCoord(DefaultActivityTypes.work, new Coord(10000., 0.));
//						plan.addActivity(act);
//					}
//					person.addPlan(plan);
//				}
//				pop.addPerson(person);
//			}
//
//			Injector.createInjector(config, new AbstractModule() {
//				@Override
//				public void install() {
//					install(new NewControlerModule());
//					install(new ControlerDefaultCoreListenersModule());
//					install(new ControlerDefaultsModule());
//					install(new ScenarioByInstanceModule(scenario));
//
//					this.bind(ObjectiveFunctionType.class).toInstance(objectiveFunctionType);
//					this.bind(ObjectiveFunctionEvaluator.class).asEagerSingleton();
//
//					this.bind(ObjectiveFunction.class).toInstance(objective);
//				}
//			});
//
//
//			SimulatorState state = new MATSimState(pop, null);
//
//			final double value = objective.value(state);
//			log.warn("nBikes=" + abc + "; value=" + value);
//			writer.println( abc + "\t" + value );
//		}
//
//		writer.close();
//
//	}
//
//}
