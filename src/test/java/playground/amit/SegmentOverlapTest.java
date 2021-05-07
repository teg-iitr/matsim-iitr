package playground.amit;

import org.junit.Assert;
import org.junit.Test;
import playground.amit.Delhi.gtfs.MyStop;
import playground.amit.Delhi.gtfs.Segment;

/**
 * Created by Amit on 07/05/2021.
 */
public class SegmentOverlapTest {

    @Test
    public void test() {

        // the stops must be same if lat, lon are same.
        MyStop stop1 = new MyStop("1","stop1",0.0, 0.0);
        MyStop stop2 = new MyStop("2","stop2",0.0, 0.0);

        Assert.assertEquals("The stops must be same.", stop1, stop2);

        // segments must be true if lat long of stops and time bin are same.
        Segment seg1 = new Segment(stop1, stop2, 0);
        Segment seg2 = new Segment(stop2, stop1, 0);

        Assert.assertEquals("The segments must be same.", seg1, seg2);

        stop2 = new MyStop("2","stop2",1.0, 1.0);
        seg1  = new Segment(stop1, stop2, 0);
        seg2  = new Segment(stop1, stop2, 0);
        Assert.assertEquals("The segments must be same.", seg1, seg2);

        //order of the stops in a segment should not make any difference
        seg2 = new Segment(stop2, stop1, 0);
        Assert.assertEquals("The segments must be same.", seg1, seg2);

        // following segments must not be same
        seg2 = new Segment(stop1, stop2, 1);
        Assert.assertFalse("The segments must not be same.", seg1.equals(seg2));

        MyStop stop3 = new MyStop("2","stop2",2.0, 2.0);
        seg2 = new Segment(stop2, stop3,0);
        Assert.assertFalse("The segments must not be same.", seg1.equals(seg2));
    }
}
