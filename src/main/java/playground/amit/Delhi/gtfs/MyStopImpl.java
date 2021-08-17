package playground.amit.Delhi.gtfs;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt2matsim.gtfs.lib.GtfsDefinitions;
import org.matsim.pt2matsim.gtfs.lib.Stop;
import org.matsim.pt2matsim.gtfs.lib.StopImpl;
import org.matsim.pt2matsim.gtfs.lib.Trip;

import java.util.Collection;

/**
 * Created by Amit on 07/05/2021.
 */
public class MyStopImpl implements Stop {

    private final StopImpl delegate;
    private final double lon;
    private final double lat;

    public MyStopImpl(String id, String name, double lon, double lat) {
        delegate = new StopImpl(id,name,lon,lat);
        this.lat = lat;
        this.lon= lon;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public double getLon() {
        return delegate.getLon();
    }

    @Override
    public double getLat() {
        return delegate.getLat();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Collection<Trip> getTrips() {
        return delegate.getTrips();
    }

    @Override
    public GtfsDefinitions.LocationType getLocationType() {
        return delegate.getLocationType();
    }

    @Override
    public String getParentStationId() {
        return delegate.getParentStationId();
    }

    @Override
    public Coord getCoord() {
        return delegate.getCoord();
    }

    public void setLocationType(GtfsDefinitions.LocationType type) {
        delegate.setLocationType(type);
    }

    public void setParentStation(String id) {
        delegate.setParentStation(id);
    }

    public void addTrip(Trip trip) {
        delegate.addTrip(trip);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        MyStopImpl stop = (MyStopImpl) o;

        if(Double.compare(stop.getLon(), lon) != 0) return false;
        if(Double.compare(stop.getLat(), lat) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lon);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public void setCoord(Coord newCoord) {
        delegate.setCoord(newCoord);
    }
}
