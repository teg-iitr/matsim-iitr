package playground.amit.Delhi.gtfs;

import org.matsim.pt2matsim.gtfs.lib.StopImpl;

/**
 * Created by Amit on 24/04/2021
 * Need to override the 'equals' so that stops are equal if lat lon are same.
 */
public class MyStopImpl extends StopImpl {

    private final double lon;
    private final double lat;

    public MyStopImpl(String id, String name, double lon, double lat) {
        super(id, name, lon, lat);
        this.lon = lon;
        this.lat = lat;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        StopImpl stop = (StopImpl) o;

        if(Double.compare(stop.getLon(), this.lon) != 0) return false;
        if(Double.compare(stop.getLat(), this.lat) != 0) return false;

        return true;
    }
}
