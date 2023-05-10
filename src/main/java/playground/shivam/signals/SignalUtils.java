package playground.shivam.signals;

import java.util.Arrays;
import java.util.Collection;

public class SignalUtils {
    public static String RUN;
    public static double LANE_LENGTH;
    public static int LANE_CAPACITY;
    public static int NO_LANES = 1;

    public static int ITERATION;
    public static double FLOW_CAPACITY_FACTOR;
    public static double STORAGE_CAPACITY_FACTOR;
    public static double LINK_LENGTH;
    public static int LINK_CAPACITY;
    // 150 cycle
    public static int AGENTS_PER_LEFT_APPROACH;
    public static int OFFSET_LEFT_APPROACH;
    public static int DROPPING_LEFT_APPROACH;

    public static int AGENTS_PER_TOP_APPROACH;
    public static int OFFSET_TOP_APPROACH;
    public static int DROPPING_TOP_APPROACH;

    public static int AGENTS_PER_RIGHT_APPROACH;
    public static int OFFSET_RIGHT_APPROACH;
    public static int DROPPING_RIGHT_APPROACH;

    public static int AGENTS_PER_BOTTOM_APPROACH;
    public static int OFFSET_BOTTOM_APPROACH;
    public static int DROPPING_BOTTOM_APPROACH;

    public static int CYCLE;


    public static Collection<String> MAIN_MODES = Arrays.asList("car");
}
