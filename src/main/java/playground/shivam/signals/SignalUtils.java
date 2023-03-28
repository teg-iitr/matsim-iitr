package playground.shivam.signals;

import java.util.Arrays;
import java.util.Collection;

public class SignalUtils {
    public static final double LANE_LENGTH = 500;
    public static final int LANE_CAPACITY = 1000;
    public static final int NO_LANES = 1;
    public static final double LINK_LENGTH = 1000;

    public static final int LINK_CAPACITY = 1000;
    public static int CYCLE = 120;

    public static final int AGENTS_PER_LEFT_APPROACH = 100;
    // seconds
    public static final int OFFSET_LEFT_APPROACH = 10;
    public static final int DROPPING_LEFT_APPROACH = 30;
    public static final int AGENTS_PER_TOP_APPROACH = 150;
    public static final int OFFSET_TOP_APPROACH = 30;
    public static final int DROPPING_TOP_APPROACH = 60;
    public static final int AGENTS_PER_RIGHT_APPROACH = 140;
    public static final int OFFSET_RIGHT_APPROACH = 60;
    public static final int DROPPING_RIGHT_APPROACH = 90;
    public static final int AGENTS_PER_BOTTOM_APPROACH = 160;
    public static final int OFFSET_BOTTOM_APPROACH = 90;
    public static final int DROPPING_BOTTOM_APPROACH = 120;
    public static final Collection<String> MAIN_MODES = Arrays.asList("car","truck");
}
