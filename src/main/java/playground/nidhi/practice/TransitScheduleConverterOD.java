package playground.nidhi.practice;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;


import de.topobyte.osm4j.core.model.iface.OsmMetadata;

public class TransitScheduleConverterOD {




	private TransitSchedule transitSchedule;
	private TransitScheduleFactory factory;
	private CoordinateTransformation transformation;
	private OsmMetadata osmData;
	private OsmMetadata osmData2;
	

//		String transitScheduleFile_OSM = "C:\\Users\\Nidhi\\Desktop\\MalviyaNagar_PT\\Files\\TransitSchedule.xml.gz";
//		Config config = new Config();
//		config.transit().setTransitScheduleFile(transitScheduleFile_OSM);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		TransitSchedule transitSchedule = scenario.getTransitSchedule();
//		
//		
		
//	public TransitScheduleConverterOD(OsmData osmData) {
//		this.osmData = osmData;
//		this.osmData = osmData;
//	}
//	
	
		public void convert(TransitSchedule schedule, CoordinateTransformation transformation) {
			this.transitSchedule = schedule;
			this.factory = transitSchedule.getFactory();
			this.transformation = transformation;
	
      
		 
		 
		 
		
		
		
		
		
		
		
		
		
		
		
		
	}

}
