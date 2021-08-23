package playground.amit.Dehradun;


import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;

/**
 * 
 * @author Amit
 *
 */
public final class DehradunUtils {
	
	public static final String EPSG = "EPSG:32644"; // same is used for Delhi.
	public static final CoordinateTransformation transformation = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, DehradunUtils.EPSG);

	public enum TravelModes{
		car, motorbike, bicycle, rail, walk, metro, metro_neo, bus, auto, IPT;
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
