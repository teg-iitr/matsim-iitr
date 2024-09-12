package playground.shivam.Dadar.evacuation.network;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuationgui.scenariogenerator.EvacuationNetworkGenerator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import playground.shivam.Dadar.evacuation.DadarUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static playground.shivam.Dadar.evacuation.DadarUtils.*;

public class CreateEvacuationNetworkFromMatsimNetwork {
    public static Geometry getEvacuationArea() {
        return evacuationArea;
    }

    public static Collection<Id<Node>> getSafeNodeAIds() {
        return safeNodeAIds;
    }

    private static Geometry evacuationArea;
    private static Collection<Id<Node>> safeNodeAIds = new ArrayList<>();
    public static void createDadarEvacNetwork(Scenario scenario) {
        // taking the zones that
        Geometry transformEvacuationArea = (Geometry) ShapeFileReader.getAllFeatures(EVACUATION_ZONES_SHAPEFILE).iterator().next().getDefaultGeometry();// --> WGS84

        try {
            evacuationArea = JTS.transform(transformEvacuationArea, CRS.findMathTransform(MGC.getCRS(TransformationFactory.WGS84), MGC.getCRS(DadarUtils.Dadar_EPSG), true));
        } catch (TransformException | FactoryException e) {
            throw new RuntimeException("Transformation isn't successful" + e);
        }

        EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(scenario, evacuationArea, safeLinkId);
        net.run(SAFE_POINTS);
       safeNodeAIds = net.getSafeNodeAIds();

        for (Link l : scenario.getNetwork().getLinks().values()) {
            Set<String> allowedModes = new HashSet<>(MAIN_MODES);
            l.setAllowedModes(allowedModes);
        }

        new NetworkWriter(scenario.getNetwork()).write(EVACUATION_NETWORK);
    }
}
