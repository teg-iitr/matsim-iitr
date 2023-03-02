/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.amit.clustering;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.matsim.core.utils.charts.XYScatterChart;
import playground.amit.utils.NumberUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 *
 * If weight of all points is 1, it is called as K-Mean cluster algorithm
 *  @see <a href="https://home.deib.polimi.it/matteucc/Clustering/tutorial_html/kmeans.html">K-Means</a>.
 *
 * Created by amit on 15.07.17.
 */

public class ClusterAlgorithm {

    /**
     * Currently, two enums are implemented both are based on K-Means clustering algorithms.
     * However, EQUAL_POINTS offers same number of points in each cluster.
     */
    public enum ClusterType {
        /**
         * Clusters will be created such that number of points are same in each cluster.
         * TODO : adapt if number of points is not divisible by number of clusters
         */
        EQUAL_POINTS,
        /**
         * Simply use of K-Means algorithm which means, cluster may have different number of points.
         */
        K_MEANS }

    private static final Logger LOGGER = LogManager.getLogger(ClusterAlgorithm.class);

    private final int numberOfClusters;
    private final BoundingBox boundingBox;

    private final List<Cluster> clusters;
    private boolean terminate = false;

    private final ClusterType clusterType;

    public ClusterAlgorithm(final int numberOfClusters, final BoundingBox boundingBox, final ClusterType clusterType) {
        this.numberOfClusters = numberOfClusters;
        this.boundingBox = boundingBox;

        clusters = new ArrayList<>(numberOfClusters);
        this.clusterType = clusterType;
        LOGGER.info("Using clustering type "+ this.clusterType);
    }

    public void process(final List<Point> pointsForClustering) {
        for (int i =0; i< numberOfClusters ; i++) {
            Cluster cluster = new Cluster(i);
            Point centroid = ClusterUtils.getRandomPoint(boundingBox);
            cluster.setCentroid(centroid);
            this.clusters.add(cluster);
        }

        if (clusterType.equals(ClusterType.EQUAL_POINTS) && pointsForClustering.size()%numberOfClusters != 0) {
            LOGGER.warn("Number of points are "+ pointsForClustering.size() + " and number of required cluster are "+
                    numberOfClusters + ". The algorithm will continue by adding additional points to the nearest cluster.");
        }

        int iteration = 1;
        while ( ! terminate ) {
            LOGGER.info("Running "+String.valueOf(iteration++)+" iteration");
            clusters.stream().forEach(Cluster::clear);

            assignCluster(pointsForClustering);
            updateCentroids();
        }
    }

    public void plotClusters(){
        XYScatterChart scatterChart = new XYScatterChart("clusters", "x", "y");
        for (Cluster cluster : this.clusters) {
            double xs [] = new double [cluster.getPoints().size()];
            double ys [] = new double [cluster.getPoints().size()];
            for (int i =0; i < cluster.getPoints().size(); i++) {
                xs[i] = cluster.getPoints().get(i).getX();
                ys[i] = cluster.getPoints().get(i).getY();
            }
            scatterChart.addSeries("cluster id"+cluster.getId().toString(), xs, ys);
        }
        ChartPanel chartPanel = new ChartPanel(scatterChart.getChart(), false);
        chartPanel.setPreferredSize(new Dimension(600, 600));
        chartPanel.setVisible(true);

        JFrame jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.add(chartPanel, BorderLayout.CENTER);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void assignCluster(final List<Point> pointsForClustering) {
        int numberOfPointsPerCluster = pointsForClustering.size() / numberOfClusters ;

        double distance = 0.0;

        for(Point point : pointsForClustering) {
            SortedMap<Double, Integer> distanceToClusterIndexMap = new TreeMap<>();
            for(int clusterIndex = 0; clusterIndex < this.clusters.size() ; clusterIndex++ ) {
                Cluster cluster = this.clusters.get(clusterIndex);
                distance = ClusterUtils.euclideanDistance(cluster.getCentroid(), point);

                distanceToClusterIndexMap.put(distance, clusterIndex);
            }

            // assign cluster
            boolean isAssigned = false;
            Iterator<Map.Entry<Double, Integer>> iterator = distanceToClusterIndexMap.entrySet().iterator();
            while ( iterator.hasNext() ) {
                Map.Entry<Double, Integer> entry = iterator.next();
                int clusterIndex = entry.getValue();
                Cluster cluster = clusters.get(clusterIndex);
                if (cluster.getPoints().size() == numberOfPointsPerCluster
                        && this.clusterType.equals(ClusterType.EQUAL_POINTS) ) {
                    // nothing to do
                } else {
                    point.setCluster(cluster.getId());
                    cluster.addPoint(point);
                    isAssigned = true;
                    break;
                }
            }

            if (! isAssigned) {
                if (clusterType.equals(ClusterType.EQUAL_POINTS)) {
                    // assign the nearest cluster if all the clusters are full. This happens because number of points are not divisible by number of clusters.
                    Cluster cluster = clusters.get( distanceToClusterIndexMap.entrySet().iterator().next().getValue());
                    cluster.addPoint(point);
                    point.setCluster(cluster.getId());
                } else {
                    throw new RuntimeException("No cluster is assigned to point "+ point.toString()+". Aborting...");
                }
            }

        }
    }

    // K-Means
//    private void assignClusterKMeans(final List<Point> pointsForClustering) {
//        double min = Double.MAX_VALUE;
//        int clusterIndex = 0;
//        double distance = 0.0;
//
//        for(Point point : pointsForClustering) {
//            min = Double.MAX_VALUE;
//            for(int index = 0; index < this.clusters.size() ; index++ ) {
//                Cluster cluster = this.clusters.get(index);
//                distance = ClusterUtils.euclideanDistance(cluster.getCentroid(), point);
//
//                if(distance < min){
//                    min = distance;
//                    clusterIndex = index;
//                }
//            }
//            Cluster cluster = clusters.get(clusterIndex);
//            point.setCluster(cluster.getId());
//            cluster.addPoint(point);
//        }
//    }

    private void updateCentroids() {
        for(Cluster cluster : clusters) {
            double sumX = 0;
            double sumY = 0;
            List<Point> list = cluster.getPoints();
            double sumWeight = list.stream().mapToDouble(Point::getWeight).sum();

            // weighted sum: same as kmeans if weight of each point is 1.
            for(Point point : list) {
                sumX += point.getWeight() * point.getX();
                sumY += point.getWeight() * point.getY();
            }

            Point oldCentroid = cluster.getCentroid();
            double newX = sumX / sumWeight;
            double newY = sumY / sumWeight;
            Point newCentroid = new Point(newX, newY, oldCentroid.getWeight());
            cluster.setCentroid(newCentroid);

            double distBetweenCentroids = ClusterUtils.euclideanDistance(oldCentroid, newCentroid);
            if (NumberUtils.round(distBetweenCentroids, 5)==0) {
                this.terminate = true;
            } else {
                this.terminate = false;
            }
        }
    }

    public List<Cluster> getClusters() {
        return clusters;
    }
}
