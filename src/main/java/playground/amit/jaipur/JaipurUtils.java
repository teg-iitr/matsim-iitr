package playground.amit.jaipur;


import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * @author Amit
 *
 */
public final class JaipurUtils {
	
	public static final String EPSG = "EPSG:32643"; // same is used for Delhi.
	public static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, JaipurUtils.EPSG);
	

}
