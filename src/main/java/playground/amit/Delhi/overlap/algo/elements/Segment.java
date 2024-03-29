package playground.amit.Delhi.overlap.algo.elements;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt2matsim.gtfs.lib.Stop;
import playground.amit.Delhi.overlap.algo.MyStopImpl;

/**
 * Created by Amit on 23/04/2021
 */
public class Segment {

    private final Stop stopA;
    private final Stop stopB;
    private final int timebin;

    private double timeSpentOnSegment = Double.NaN;
    private Tuple<Integer, Integer> stopSequence;
    private double length;

    public Segment (Stop stopA, Stop stopB, int timebin){
        if ( !(stopA instanceof MyStopImpl && stopB instanceof MyStopImpl)) {
            throw new RuntimeException("Stops must be of type MyStopImpl.");
        }
        this.stopA = stopA;
        this.stopB = stopB;
        this.timebin = timebin;
    }

    public double getTimeSpentOnSegment() {
        return timeSpentOnSegment;
    }

    public void setTimeSpentOnSegment(double timeSpentOnSegment) {
        this.timeSpentOnSegment = timeSpentOnSegment;
    }

    public Tuple<Integer, Integer> getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Tuple<Integer, Integer> stopSequence) {
        this.stopSequence = stopSequence;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;

        Segment seg = (Segment) obj;

        if ( this.timebin!=seg.getTimebin() ) return false;

        return (this.getStopA().equals(seg.getStopA()) &&  this.getStopB().equals(seg.getStopB())
                || this.getStopA().equals(seg.getStopB()) &&  this.getStopB().equals(seg.getStopA()));
    }
    
    public MyStopImpl getStopA() {
        return (MyStopImpl) stopA;
    }

    public MyStopImpl getStopB() {
        return (MyStopImpl) stopB;
    }

    public int getTimebin() {
        return this.timebin;
    }
    
    @Override
    public int hashCode() {
    	int result = this.timebin ^ this.timebin >>> 32;
        result = 31 * result + this.stopA.hashCode();
        result = 31 * result + this.stopB.hashCode();
        return result;
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
