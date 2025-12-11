package playground.anuj.charDham.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * This code generates the MATSim network file from a shapefile boundary and an OSM PBF file.
 * It then modifies the network by reading link attributes from multiple CSV files for different purposes:
 * - Updating speed, lanes, and capacity for 'dham_2_dham' links.
 * - Setting 'roadType' attribute to "FIFO" for 'fifo_links'.
 * - Updating speed, lanes, and capacity for 'major_routes'.
 * It also embeds a 'nightToll' attribute into each link to facilitate night travel restrictions.
 */
public class CharDhamModifyNetwork {

    public static final String matsimNetworkFile = "output/network_charDham.xml.gz";
    public static final String matsimModifiedNetworkFile = "output/network_charDham_modified.xml.gz";
    // CSV files for network updates
    public static final String DHAM_2_DHAM_CSV = "input/uk_network_updates/dham_2_dham.csv";
    public static final String FIFO_LINKS_CSV = "input/uk_network_updates/fifo_links.csv";
    public static final String MAJOR_ROUTES_CSV = "input/uk_network_updates/major_routes_final.csv";


    // Helper class to store data read from CSV for a link
    private static class LinkUpdateData {
        final double dist;
        final double cap;
        final double lanes;
        final double freespeed; // Stored in m/s

        LinkUpdateData(double dist, double cap, double lanes, double freespeed) {
            this.dist = dist;
            this.cap = cap;
            this.lanes = lanes;
            this.freespeed = freespeed;
        }

        public double getDist() { return dist; }
        public double getCap() { return cap; }
        public double getLanes() { return lanes; }
        public double getFreespeed() { return freespeed; }
    }
    public static Network readNetwork(String fileName) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(fileName);
        return network;
    }
    public static void main(String[] args) {
        Network network = readNetwork(matsimNetworkFile);

        // --- 1. Update links from dham_2_dham.csv ---
//        System.out.println("\nUpdating links from " + DHAM_2_DHAM_CSV);
//        Map<Id<Link>, LinkUpdateData> dham2DhamData = readLinkAttributesFromCsv(DHAM_2_DHAM_CSV);
//        applyLinkAttributes(network, dham2DhamData);
//        System.out.println("Finished updating " + dham2DhamData.size() + " links from " + DHAM_2_DHAM_CSV);

        // --- 2. Update links from fifo_links.csv for FIFO dynamics ---
        System.out.println("\nSetting FIFO dynamics for links from " + FIFO_LINKS_CSV);
        Map<Id<Link>, LinkUpdateData> fifoLinksData = readLinkAttributesFromCsv(FIFO_LINKS_CSV);
        int fifoUpdatedCount = 0;
        for (Link link : network.getLinks().values()) {
            if (fifoLinksData.containsKey(link.getId())) {
                link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE, QSimConfigGroup.LinkDynamics.FIFO);
                fifoUpdatedCount++;
            }
            else
                link.getAttributes().putAttribute(TrafficCharConfigGroup.ROAD_TYPE_DEFAULT, QSimConfigGroup.LinkDynamics.PassingQ);
        }
        System.out.println("Set 'roadType' to 'FIFO' for " + fifoUpdatedCount + " links from " + FIFO_LINKS_CSV);

        // --- 3. Update links from major_routes.csv ---
        System.out.println("\nUpdating major routes from " + MAJOR_ROUTES_CSV);
        Map<Id<Link>, LinkUpdateData> majorRoutesData = readLinkAttributesFromCsv(MAJOR_ROUTES_CSV);
        applyLinkAttributes(network, majorRoutesData);
        System.out.println("Finished updating " + majorRoutesData.size() + " links from " + MAJOR_ROUTES_CSV);

        // Ensure none of the link lengths are smaller than Euclidean distance
        System.out.println("\nChecking and correcting link lengths...");
        for (Link l : network.getLinks().values()) {
            double beelineDist = org.matsim.core.network.NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord());
            if (l.getLength() < beelineDist) {
                l.setLength(Math.ceil(beelineDist));
            }
        }
        System.out.println("Link length check complete.");

//        NetworkUtils.simplifyNetwork(network);

        new NetworkWriter(network).write(matsimModifiedNetworkFile);

        System.out.println("\nWriting modified network to " + matsimModifiedNetworkFile);

        System.out.println("Done. Modified network saved.");
    }

    /**
     * Reads link attributes from a specified CSV file.
     *
     * @param filePath The path to the CSV file.
     * @return A map where keys are Link IDs and values are LinkUpdateData objects.
     */
    private static Map<Id<Link>, LinkUpdateData> readLinkAttributesFromCsv(String filePath) {
        Map<Id<Link>, LinkUpdateData> linkData = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // Expected columns: id,from,to,dist,cap,lanes,modes,freespeed
                if (parts.length == 7) {
                    try {
                        Id<Link> linkId = Id.create(parts[0], Link.class);
                        double dist = Double.parseDouble(parts[3]);
                        double cap = Double.parseDouble(parts[4]);
                        double lanes = Double.parseDouble(parts[5]);
                        double freespeed = Double.parseDouble(parts[6]); // freespeed is in km/h
                        linkData.put(linkId, new LinkUpdateData(dist, cap, lanes, freespeed)); // Convert km/h to m/s
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Could not parse numeric value in line: " + line + " from " + filePath + ". Skipping. Error: " + e.getMessage());
                    }
                } else {
                    System.err.println("Warning: Skipping malformed line in CSV (not enough columns): " + line + " from " + filePath);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading link attributes CSV file: " + filePath);
            e.printStackTrace();
        }
        return linkData;
    }

    /**
     * Applies the attributes from the provided map to the corresponding links in the network.
     *
     * @param network The MATSim network to modify.
     * @param dataMap A map of Link IDs to LinkUpdateData containing the new attributes.
     */
    private static void applyLinkAttributes(Network network, Map<Id<Link>, LinkUpdateData> dataMap) {
        int updatedCount = 0;
        for (Map.Entry<Id<Link>, LinkUpdateData> entry : dataMap.entrySet()) {
            Id<Link> linkId = entry.getKey();
            LinkUpdateData csvData = entry.getValue();
            Link link = network.getLinks().get(linkId);
            if (link != null) {
                link.setLength(csvData.getDist());
                link.setCapacity(csvData.getCap());
                link.setNumberOfLanes(csvData.getLanes());
                link.setFreespeed(csvData.getFreespeed());
                updatedCount++;
            } else {
                System.err.println("Warning: Link with ID " + linkId + " found in CSV but not in network. Skipping update.");
            }
        }
        System.out.println("Applied attributes to " + updatedCount + " links.");
    }
}