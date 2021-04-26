package playground.amit.Delhi.gtfs;

import java.util.HashMap;
import java.util.Map;

import org.matsim.pt2matsim.gtfs.lib.Stop;
import org.matsim.pt2matsim.gtfs.lib.StopImpl;

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
    
    public static void main(String[] args) {
		
    	Map<Segment, Integer> temp = new HashMap<>();
    	temp.put(new Segment(new StopImpl("1", "A", 0, 0),new StopImpl("2", "B", 1, 1.0),23), 0);
    	temp.put(new Segment(new StopImpl("1", "A", 0, 0),new StopImpl("2", "B", 1.0, 1),22), 1);
    	temp.put(new Segment(new StopImpl("1", "A", 0, 0),new StopImpl("2", "B", 1, 1),23), 2);
    	
    	for (Segment seg : temp.keySet()) {
    		System.out.println(seg);
    		System.out.println(temp.get(seg));
    	}
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
    public int hashCode() {
    	int result = (int) (this.timebin ^ (this.timebin >>> 32));
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
