package playground.agarwalamit.Chandigarh;

import java.util.List;

public class ChandigarhConstants {

    // activity types
    public static final String start_act_type = "start";
    public static final String end_act_type = "end";

    //EPSG
    public static final String CH_EPSG = "EPSG:32643";

    //origins
    public static final String link_1A = "3362";//"3346";
    public static final String link_1C = "422";//"422";
    public static final String link_1E = "2294";//"2280";
    public static final String link_2C = "3352";//"3337";
    public static final String link_2E = "3314";//"3300";
    public static final String link_3C = "2409";//"2395";
    public static final String link_3E = "3523";//"3502";
    public static final String link_4F = "633";//"631";

    //destinations
    public static final String link_2B = "3379";//"3359";
    public static final String link_2D = "1315-1688";//"1303-1676";
    public static final String link_3B = "2388";//"2374";
    public static final String link_3D = "3515";//"3494";
    public static final String link_4A = "3400";//"3379";

    public static final String link_1B = "540-533";//"538-531";
    public static final String link_1D = "2296";//"2282";
    public static final String link_1F = "3373";//"3354";

    public static final List<String> Unknown_Destinations_1A_1C_1E = List.of(link_2B, link_2D, link_3B, link_3D, link_4A);
    public static final List<String> Unknown_Destinations_2C_2E_towards_3A = List.of( link_3B, link_3D, link_4A);
    static final List<String> Unknown_Destinations_2C_2E_towards_2F= List.of(link_1B, link_1D, link_1F);
    public static final List<String> Unknown_Destinations_3C_3E_4F = List.of(link_1B, link_1D, link_1F, link_2B, link_2D);

    public static final String link_2A = "4215";//"4192";
    public static final String link_2F = "3367";//"3350";
    public static final String link_3A = "3394";//"3373";
    public static final String link_3F = "638";//"636";
}
