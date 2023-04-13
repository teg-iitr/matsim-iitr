package playground.shivam.signals;

import java.util.Arrays;
import java.util.Collection;

public class SignalUtils {
    public static double LANE_LENGTH;
    public static int LANE_CAPACITY;
    public static int NO_LANES = 1;

    public static int ITERATION;
    public static double FLOW_CAPACITY_FACTOR;
    public static double STORAGE_CAPACITY_FACTOR;
    public static double LINK_LENGTH;
    public static int LINK_CAPACITY;

    public static int CYCLE;

    // 150 cycle
    public static int AGENTS_PER_LEFT_APPROACH;
    public static final int OFFSET_LEFT_APPROACH = 5;
    public static final int DROPPING_LEFT_APPROACH = 40;

    public static int AGENTS_PER_TOP_APPROACH;
    public static final int OFFSET_TOP_APPROACH = 45;
    public static final int DROPPING_TOP_APPROACH = 70;

    public static int AGENTS_PER_RIGHT_APPROACH;
    public static final int OFFSET_RIGHT_APPROACH = 80;
    public static final int DROPPING_RIGHT_APPROACH = 115;

    public static int AGENTS_PER_BOTTOM_APPROACH;
    public static final int OFFSET_BOTTOM_APPROACH = 120;
    public static final int DROPPING_BOTTOM_APPROACH = 150;

    // 120 cycle
    /*public static int AGENTS_PER_LEFT_APPROACH;
    public static final int OFFSET_LEFT_APPROACH = 5;
    public static final int DROPPING_LEFT_APPROACH = 30;

    public static int AGENTS_PER_TOP_APPROACH;
    public static final int OFFSET_TOP_APPROACH = 35;
    public static final int DROPPING_TOP_APPROACH = 55;

    public static int AGENTS_PER_RIGHT_APPROACH;
    public static final int OFFSET_RIGHT_APPROACH = 60;
    public static final int DROPPING_RIGHT_APPROACH = 85;

    public static int AGENTS_PER_BOTTOM_APPROACH;
    public static final int OFFSET_BOTTOM_APPROACH = 90;
    public static final int DROPPING_BOTTOM_APPROACH = 120;*/

    /*// 90 cycle
    public static int AGENTS_PER_LEFT_APPROACH;
    public static final int OFFSET_LEFT_APPROACH = 5;
    public static final int DROPPING_LEFT_APPROACH = 25;

    public static int AGENTS_PER_TOP_APPROACH;
    public static final int OFFSET_TOP_APPROACH = 30;
    public static final int DROPPING_TOP_APPROACH = 40;

    public static int AGENTS_PER_RIGHT_APPROACH;
    public static final int OFFSET_RIGHT_APPROACH = 45;
    public static final int DROPPING_RIGHT_APPROACH = 65;

    public static int AGENTS_PER_BOTTOM_APPROACH;
    public static final int OFFSET_BOTTOM_APPROACH = 70;
    public static final int DROPPING_BOTTOM_APPROACH = 85;*/

    /*// 60 cycle
    public static int AGENTS_PER_LEFT_APPROACH;
    public static final int OFFSET_LEFT_APPROACH = 5;
    public static final int DROPPING_LEFT_APPROACH = 15;

    public static int AGENTS_PER_TOP_APPROACH;
    public static final int OFFSET_TOP_APPROACH = 20;
    public static final int DROPPING_TOP_APPROACH = 30;

    public static int AGENTS_PER_RIGHT_APPROACH;
    public static final int OFFSET_RIGHT_APPROACH = 35;
    public static final int DROPPING_RIGHT_APPROACH = 45;

    public static int AGENTS_PER_BOTTOM_APPROACH;
    public static final int OFFSET_BOTTOM_APPROACH = 50;
    public static final int DROPPING_BOTTOM_APPROACH = 60;*/

    public static Collection<String> MAIN_MODES = Arrays.asList("car","truck");
}
