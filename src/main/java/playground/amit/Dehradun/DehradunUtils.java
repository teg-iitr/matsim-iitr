package playground.amit.Dehradun;


import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * @author Amit
 *
 */
public final class DehradunUtils {
	
	public static final String Dehradun_EPGS = "EPSG:32644"; // same is used for Delhi.
	public static final CoordinateTransformation transformation = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, Dehradun_EPGS);

	public static final CoordinateTransformation Reverse_transformation = TransformationFactory.getCoordinateTransformation(DehradunUtils.Dehradun_EPGS,TransformationFactory.WGS84);

	public static final double sampleSize = 0.1;
	public static final String origin = "O_Zone";
	public static final String destination = "D_Zone";
//	public static final String subPopulation = "SubPopulation";
//	public static final String dehradun_subPop = "Dehradun";
//	public static final String rest_subPop = "rest";

	public enum TravelModesBaseCase2017 {//Do NOT add Metro in this.
		car, motorbike, bus, IPT;
	}

	public enum TravelModesMetroCase2021 {
		car, motorbike, bus, IPT, metro;
	}

	/**
	 * @param travelMode
	 * for which speed is required
	 */
	public static double getSpeed(final String travelMode){
		double speed;
		switch (travelMode) {
			case "motorbike": speed = 80/3.6; break;
			case "car": speed = 80/3.6; break;
//
//			case "bicycle": speed = 15/3.6; break;
//
			default: throw new RuntimeException("No speed is set for travel mode "+travelMode+ ".");
		}
		return speed;
	}

	public static double getSpeedKPHFromReport(final String travelMode){
		double speed;
		switch (travelMode) {
			case "motorbike": speed = 70; break;
			case "car": speed = 45; break;
			case "IPT": speed = 10; break;
			case "bus": speed = 24; break;
			case "metro": speed = 50; break;
			default: throw new RuntimeException("No speed is set for travel mode "+travelMode+ ".");
		}
		return speed;
	}
}
