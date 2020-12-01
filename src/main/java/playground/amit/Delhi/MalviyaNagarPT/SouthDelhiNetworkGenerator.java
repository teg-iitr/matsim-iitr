package playground.amit.Delhi.MalviyaNagarPT;

import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import playground.amit.utils.FileUtils;

public class SouthDelhiNetworkGenerator {
    private static final String inputPBFFile = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/planet_77.183,28.513_77.247,28.55.osm.pbf";
    private static final String matsimNetworkFile = FileUtils.getLocalGDrivePath() + "project_data/delhiMalviyaNagar_PT/matsimFiles/south_delhi_matsim_network.xml.gz";

    public static void main(String[] args) {
        CoordinateTransformation transformation = TransformationFactory
                .getCoordinateTransformation(TransformationFactory.WGS84, MN_TransitDemandGenerator.toCoordinateSystem);
        CoordinateTransformation reverse_transformation = TransformationFactory
                .getCoordinateTransformation(MN_TransitDemandGenerator.toCoordinateSystem, TransformationFactory.WGS84);

        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(transformation)
                .build()
                .read(Paths.get(inputPBFFile));
        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(matsimNetworkFile);
    }
}
