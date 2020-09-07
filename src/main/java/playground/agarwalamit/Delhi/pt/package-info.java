package playground.agarwalamit.Delhi.pt;

/**
 * <li>GTFS files for Delhi bus lines are available from https://opendata.iiitd.edu.in/data/static/.</li>
 * <li>GTFS2MATSimSchedule class from  https://github.com/amit2011/GTFS2MATSim.git is used to convert GTFS to MATSim transit schedule.</li>
 * <li>The GTFS from OpenTransitData does not include metro lines, which are extracted using OSM. For this, Osm2TransitSchedule class from https://github.com/matsim-org/pt2matsim.git is used to generate MATSim Transit Schedules.
 * <li>The OSM file for Metro lines ONLY is generated using https://overpass-turbo.eu/ and the used script is shown at: https://github.com/amit2011/resources</li>
 * <li>However, the OSM file is fixed as follows:
 * <li>The key 'operaor' is made uniform everywhere.</li>
 * <li>public final static String STATION = "station"; has been added to Osm class. Without it, lines were created only if "public_trnasport" tag has the key "stop_position".</li>
 * <li>The tag public_transport was missing from a few nodes (around 25 stations). This is added as "<tag k="public_transport" v="stop_position"/>" in osm file.</li>
 * <li> Operator name was added to grey line; ref is fixed for grey and airport express lines.</li>
 * </li>
 * <li>Next steps are:
 * <li>Merge the two MATSIm transit schedules.</li>
 * <li>Add the departure times to metro lines.</li>
 * <li>Fix the stops whose position doesn't make sense.</li>
 * <li>Generate Transit Vehicles.</li>
 * <li>The linkref IDs are missing from stop facilities which appears to be the reason for missing connections in VIA.</li>
 * </li>
 */
