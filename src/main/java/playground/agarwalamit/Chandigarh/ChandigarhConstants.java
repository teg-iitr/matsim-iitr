package playground.agarwalamit.Chandigarh;

import java.util.List;

public class ChandigarhConstants {

    //origins
    public static final String origin_link_1A = "3346";
    public static final String origin_link_1C = "422";
    public static final String origin_link_1E = "2280";
    public static final String origin_link_2C = "3337";
    public static final String origin_link_2E = "3300";
    public static final String origin_link_3C = "2395";
    public static final String origin_link_3E = "3502";
    public static final String origin_link_4A = "631";

    public static final String desti_from_1A_1C_1E_link_2B = "3359";
    public static final String desti_from_1A_1C_1E_link_2D = "1303-1676";
    public static final String desti_from_1A_1C_1E_link_3B = "2374";
    public static final String desti_from_1A_1C_1E_link_3D = "3494";
    public static final String desti_from_1A_1C_1E_link_4A = "3379";

    public static final String desti_from_2C_2E_link_1B = "538-531";
    public static final String desti_from_2C_2E_link_1D = "2282";
    public static final String desti_from_2C_2E_link_1F = "3354";
    public static final String desti_from_2C_2E_link_3B = "2374";
    public static final String desti_from_2C_2E_link_3D = "3494";
    public static final String desti_from_2C_2E_link_4A = "3379";

    public static final String desti_from_3C_3E_4A_link_1B = "538-531";
    public static final String desti_from_3C_3E_4A_link_1D = "2282";
    public static final String desti_from_3C_3E_4A_link_1F = "3354";
    public static final String desti_from_3C_3E_4A_link_2B = "3359";
    public static final String desti_from_3C_3E_4A_link_2D = "1303-1676";

    public static final String CH_EPSG = "EPSG:32643";

    public static final List<String> Unknown_Destinations_1A_1C_1E = List.of(desti_from_1A_1C_1E_link_2B,desti_from_1A_1C_1E_link_2D,desti_from_1A_1C_1E_link_3B,desti_from_1A_1C_1E_link_3D,desti_from_1A_1C_1E_link_4A);
    public static final List<String> Unknown_Destinations_2C_2E = List.of(desti_from_2C_2E_link_1B,desti_from_2C_2E_link_1D,desti_from_2C_2E_link_1F,desti_from_2C_2E_link_3B,desti_from_2C_2E_link_3D,desti_from_2C_2E_link_4A);
    public static final List<String> Unknown_Destinations_3C_3E_4A = List.of(desti_from_3C_3E_4A_link_1B,desti_from_3C_3E_4A_link_1D,desti_from_3C_3E_4A_link_1F,desti_from_3C_3E_4A_link_2B, desti_from_3C_3E_4A_link_2D);

}
