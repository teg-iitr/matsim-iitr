package playground.shivam.signals;

import java.util.Arrays;
import java.util.Collection;

public class SignalUtils {

    public static final double LANE_LENGTH = 500;
    public static final int LANE_CAPACITY = 1000;
    public static final int NO_LANES = 1;

    public static int ITERATION = 100;

    public static final double LINK_LENGTH = 1000;
    public static final int LINK_CAPACITY = 2000;

    public static int CYCLE = 120;

    public static final int AGENTS_PER_LEFT_APPROACH = 200;
    public static final int OFFSET_LEFT_APPROACH = 5;
    public static final int DROPPING_LEFT_APPROACH = 25;

    public static final int AGENTS_PER_TOP_APPROACH = 90;
    public static final int OFFSET_TOP_APPROACH = 35;
    public static final int DROPPING_TOP_APPROACH = 55;

    public static final int AGENTS_PER_RIGHT_APPROACH = 200;
    public static final int OFFSET_RIGHT_APPROACH = 65;
    public static final int DROPPING_RIGHT_APPROACH = 85;

    public static final int AGENTS_PER_BOTTOM_APPROACH = 100;
    public static final int OFFSET_BOTTOM_APPROACH = 95;
    public static final int DROPPING_BOTTOM_APPROACH = 115;

    public static final Collection<String> MAIN_MODES = Arrays.asList("car","truck");
}
