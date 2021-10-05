package playground.amit.Dehradun;

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
        System.out.println( GHNetworkDistanceCalculator.getDistance(null, null, null, null));
    }

    public static Tuple<Double, Double> getDistance(Coord origin, Coord destination, String mode, String routeType) {
        if (origin == null ) throw new RuntimeException("Origin is null. Aborting...");
        if (destination == null ) throw new RuntimeException("Destination is null. Aborting...");
        if (mode == null ) throw new RuntimeException("Transport mode is null. Aborting...");
        if (routeType == null ) throw new RuntimeException("Route type is null. Aborting...");

        String base_url = "http://localhost:9098/routing?";
        String json_suffix = "&mediaType=json";
//        String url_string = base_url+"StartLoc=77.09652%2C28.555764&EndLoc=77.32%2C28.57&RouteType=fastest&Vehicle=bike&mediaType=json";
        String url_string = base_url+"StartLoc="+origin.getY()+"%2C"+origin.getX()+
                "&EndLoc="+destination.getY()+"%2C"+destination.getX()+
                "RouteType="+routeType+"&Vehicle="+mode+json_suffix;

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
            return new Tuple<>((Double) summary.get("distance"), (Double) summary.get("distance"));
        } catch (IOException | ParseException e) {
            throw new RuntimeException("URL is not connected.");
        }
    }
}
