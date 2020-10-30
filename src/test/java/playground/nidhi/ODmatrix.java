package playground.nidhi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.opengis.feature.simple.SimpleFeature;

public class ODmatrix {
	
    public final static String DELHI_PLANS = "C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\delhiPlans.xml.gz";
    private final Random random = MatsimRandom.getLocalInstance();
    private final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
	
	
	


//	public static void main(String[] args) throws IOException {
//		// TODO Auto-generated method stub
//	 
//			BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\OD_pairs.csv"));  
//			  String line = reader.readLine();
//	}

	
	public static void main(String[] args) throws IOException   
	{    
	
	
	

	
//		
	 
	BufferedReader OD = new BufferedReader(new FileReader("C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\OD_pairs.csv")); 
	String line = OD.readLine();
	
	
	BufferedReader coordDelhi = new BufferedReader(new FileReader("C:\\Users\\Nidhi\\Desktop\\MATSim Paper\\Book1.csv")); 
	String line2 = coordDelhi.readLine();
	
//	String line = "";  
//	String splitBy = ",";
//	try   {   
//	while ((line = od.readLine()) != null){  
//	String[] Zone= line.split(splitBy);   
//    System.out.println(" [" + Zone[0] + ", " + Zone[1] + "," + Zone[2] + "," + Zone[3] + ", " + Zone[4] + ", " + Zone[5] +", " + Zone[5] +" , " + Zone[6] +", " + Zone[7] +", " + Zone[8] +", " + Zone[9] +", " + Zone[10] +", " + Zone[11] +", " + Zone[12] +", " + Zone[13] +", " + Zone[14] +", " + Zone[15] +", " + Zone[16] +", " + Zone[17] +", " + Zone[18] +", " + Zone[19] +", " + Zone[20] +", " + Zone[21] +"]");  
//	}  
//	}   
//	catch (IOException e){  
//	e.printStackTrace();  

}
}
