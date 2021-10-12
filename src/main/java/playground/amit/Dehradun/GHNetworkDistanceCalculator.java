package playground.amit.Dehradun;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 * @author Amit, created on 20-09-2021
 */

public class GHNetworkDistanceCalculator {

    public static void main(String[] args) {
        System.out.println( GHNetworkDistanceCalculator.getDistanceInKmTimeInHr(new Coord(28.555764, 77.09652), new Coord(28.57,77.32), "car", "fastest"));
    }

    public static Tuple<Double, Double> getDistanceInKmTimeInHr(Coord origin, Coord destination, String mode, String routeType) {
        if (origin == null ) throw new RuntimeException("Origin is null. Aborting...");
        if (destination == null ) throw new RuntimeException("Destination is null. Aborting...");
        if (mode == null ) {
            Logger.getLogger(GHNetworkDistanceCalculator.class).warn("Transport mode is null. Setting to "+"car");
            mode = "car";
        }
        if (routeType == null ) {
            Logger.getLogger(GHNetworkDistanceCalculator.class).warn("Route type is null. Setting to "+"fastest");
            routeType = "fastest";
        }

        String base_url = "http://localhost:9098/routing?";
        String json_suffix = "&mediaType=json";
//        String url_string = base_url+"StartLoc=77.09652%2C28.555764&EndLoc=77.32%2C28.57&RouteType=fastest&Vehicle=bike&mediaType=json";
        String url_string = base_url+"StartLoc="+origin.getY()+"%2C"+origin.getX()+
                "&EndLoc="+destination.getY()+"%2C"+destination.getX()+
                "&RouteType="+routeType+"&Vehicle="+mode+json_suffix;
        System.out.println("URL is "+url_string);
        try {
            URL url = new URL(url_string);
            URLConnection request = url.openConnection();
            request.connect();

            Scanner sc = new Scanner(url.openStream());
            StringBuilder inline = new StringBuilder();
            while(sc.hasNext())
            {
                inline.append(sc.nextLine());
            }
            sc.close();

            JSONParser parse = new JSONParser();
            JSONObject json =  (JSONObject)parse.parse(inline.toString());
            JSONObject summary = (JSONObject) json.get("summary");
            return new Tuple<>((Double) summary.get("distance")/1000., (Double) summary.get("time")/3600.);
        } catch (IOException | ParseException e) {
            throw new RuntimeException("URL is not connected.");
        }
    }
}
