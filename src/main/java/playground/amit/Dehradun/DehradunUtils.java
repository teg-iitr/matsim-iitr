package playground.amit.Dehradun;


import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * @author Amit
 *
 */
public final class DehradunUtils {
	
	public static final String EPSG = "EPSG:32644"; // same is used for Delhi.
	public static final CoordinateTransformation transformation = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, EPSG);

	public static final double sampleSize = 0.1;
	public static final String origin = "O_Zone";
	public static final String destination = "D_Zone";

	public enum TravelModesBaseCase2017 {//Do NOT add Metro in this.
		car, motorbike, bicycle, walk, bus, IPT;
	}

	public enum TravelModesMetroCase2017 {//Do NOT add Metro in this.
		car, motorbike, bicycle, walk, bus, IPT, metro;
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
			case "bicycle": speed = 15/3.6; break;
//
			default: throw new RuntimeException("No speed is set for travel mode "+travelMode+ ".");
		}
		return speed;
	}

//	public static double ModalShare {
//		car (30), motorbike (30), bike (5), walk (10), rail(2), metro_neo(0) ;
//
//		private double modalShare;
//
//		public double getModeShare(){
//			return modalShare;
//		}
//
//		public static double getModalShare ( String mode ) {
//			return ModalShare.valueOf( mode ).getModeShare() ;
//		}
//
//		ModalShare(double firstArg) {
//			this.modalShare = firstArg;
//		}
//	}
}
