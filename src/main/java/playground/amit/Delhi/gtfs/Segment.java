package playground.amit.Delhi.gtfs;

import org.matsim.pt2matsim.gtfs.lib.Stop;

/**
 * Created by Amit on 23/04/2021
 */
public class Segment {

    private Stop stopA;
    private Stop stopB;
    private int timebin;

    public Segment (Stop stopA, Stop stopB, int timebin){
        this.stopA = stopA;
        this.stopB = stopB;
        this.timebin = timebin;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;

        Segment seg = (Segment) obj;

        return this.stopA.equals(seg.getStopA()) &&  this.stopB.equals(seg.getStopB()) && this.timebin==seg.getTimebin();
    }

    public Stop getStopA() {
        return stopA;
    }

    public Stop getStopB() {
        return stopB;
    }

    public int getTimebin() {
        return this.timebin;
    }

    @Override
    public String toString() {
        return "Segment{" +
                "stopA=" + stopA +
                ", stopB=" + stopB +
                ", time bin=" + timebin +
                "}";
    }
}
