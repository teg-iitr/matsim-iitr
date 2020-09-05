package playground.agarwalamit.Delhi.pt;

/**
 * <li>GTFS files for Delhi bus lines are available from https://opendata.iiitd.edu.in/data/static/.</li>
 * <li>GTFS2MATSimSchedule class from  https://github.com/amit2011/GTFS2MATSim.git is used to convert GTFS to MATSim transit schedule.</li>
 * <li>The GTFS from OpenTransitData does not include metro lines, which are extracted using OSM. For this, Osm2TransitSchedule class from https://github.com/matsim-org/pt2matsim.git is used to generate MATSim Transit Scheudles.
 * <li>The class is throwing an error because of same transit line ID which is derived from the relation (ref/name/operator/ etc.).</li>
 * <li>To fix this, if a line ID exists already, another line ID is created in an incremental order. Ideally, these will be transit routes of a transit line.</li>
 * <li>The bus line info appears to be wrong. Remove them.</li>
 * <li>The output MATSim transit scheudle file for metro lines still lack in many lines.</li>
 * </li>
 * <li>The generated GTFS is cleaned to extract only DMRC lines in it. See {@link playground.agarwalamit.Delhi.pt.OSMTransitSchedulePrep}</li>
 * <li>Next steps are:
 * <li>Merge the two MATSIm transit schedules.</li>
 * <li>Add the missing transit lines.</li>
 * </li>
 */
