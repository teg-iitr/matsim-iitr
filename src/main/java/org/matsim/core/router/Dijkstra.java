package org.matsim.core.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.priorityqueue.WrappedBinaryMinHeap;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class Dijkstra implements LeastCostPathCalculator {
    private static final Logger log = LogManager.getLogger(Dijkstra.class);
    protected Network network;
    protected final TravelDisutility costFunction;
    protected final TravelTime timeFunction;
    final HashMap<Id<Node>, DijkstraNodeData> nodeData;
    private int iterationID;
    private Node deadEndEntryNode;
    final boolean pruneDeadEnds;
    private final PreProcessDijkstra preProcessData;
    private RouterPriorityQueue<Node> heap;
    private String[] modeRestriction;
    Person person;
    Vehicle vehicle;

    public Dijkstra(Network network, TravelDisutility costFunction, TravelTime timeFunction) {
        this(network, costFunction, timeFunction, (PreProcessDijkstra)null);
    }

    protected Dijkstra(Network network, TravelDisutility costFunction, TravelTime timeFunction, PreProcessDijkstra preProcessData) {
        this.iterationID = -2147483647;
        this.heap = null;
        this.modeRestriction = null;
        this.person = null;
        this.vehicle = null;
        this.network = network;
        this.costFunction = costFunction;
        this.timeFunction = timeFunction;
        this.preProcessData = preProcessData;
        this.nodeData = new HashMap((int)((double)network.getNodes().size() * 1.1), 0.95F);
        if (preProcessData != null) {
            if (!preProcessData.containsData()) {
                this.pruneDeadEnds = false;
                log.warn("The preprocessing data provided to router class Dijkstra contains no data! Please execute its run(...) method first!");
                log.warn("Running without dead-end pruning.");
            } else {
                this.pruneDeadEnds = true;
            }
        } else {
            this.pruneDeadEnds = false;
        }

    }

    /** @deprecated */
    @Deprecated
    public void setModeRestriction(Set<String> modeRestriction) {
        if (modeRestriction == null) {
            this.modeRestriction = null;
        } else {
            this.modeRestriction = (String[])modeRestriction.toArray(new String[modeRestriction.size()]);
        }

    }

    public LeastCostPathCalculator.Path calcLeastCostPath(Node fromNode, Node toNode, double startTime, Person person2, Vehicle vehicle2) {
        this.checkNodeBelongToNetwork(fromNode);
        this.checkNodeBelongToNetwork(toNode);
        this.augmentIterationId();
        this.person = person2;
        this.vehicle = vehicle2;
        if (this.pruneDeadEnds) {
            this.deadEndEntryNode = this.getPreProcessData(toNode).getDeadEndEntryNode();
        }

        RouterPriorityQueue<Node> pendingNodes = (RouterPriorityQueue<Node>) this.createRouterPriorityQueue();
        this.initFromNode(fromNode, toNode, startTime, pendingNodes);
        Node foundToNode = this.searchLogic(fromNode, toNode, pendingNodes, person2, vehicle2);
        if (foundToNode == null) {
            return null;
        } else {
            DijkstraNodeData outData = this.getData(foundToNode);
            double arrivalTime = outData.getTime();
            return this.constructPath(fromNode, foundToNode, startTime, arrivalTime);
        }
    }

    void checkNodeBelongToNetwork(Node node) {
        if (this.network.getNodes().get(node.getId()) != node) {
            throw new IllegalArgumentException("The nodes passed as parameters are not part of the network stored by " + this.getClass().getSimpleName() + ": the validity of the results cannot be guaranteed. Aborting!");
        }
    }

    RouterPriorityQueue<? extends Node> createRouterPriorityQueue() {
        if (this.heap == null) {
            this.heap = new WrappedBinaryMinHeap(this.network.getNodes().size());
            Iterator var1 = this.network.getNodes().values().iterator();

            while(var1.hasNext()) {
                Node node = (Node)var1.next();
                this.heap.add(node, 0.0);
            }
        }

        this.heap.reset();
        return this.heap;
    }

    Node searchLogic(Node fromNode, Node toNode, RouterPriorityQueue<Node> pendingNodes, Person person, Vehicle vehicle) {
        boolean stillSearching = true;

        while(stillSearching) {
            Node outNode = (Node)pendingNodes.poll();
            if (outNode == null) {
                Logger var10000 = log;
                Id var10001 = fromNode.getId();
                var10000.warn("No route was found from node " + var10001 + " to node " + toNode.getId() + ". " + createInfoMessage(person, vehicle) + "Some possible reasons:");
                log.warn("  * Network is not connected.  Run NetworkCleaner().");
                log.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
                log.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
                log.warn("This will now return null, but it may fail later with a null pointer exception.");
                return null;
            }

            if (outNode == toNode) {
                stillSearching = false;
            } else {
                this.relaxNode(outNode, toNode, pendingNodes);
            }
        }

        return toNode;
    }

    static StringBuilder createInfoMessage(Person person, Vehicle vehicle) {
        StringBuilder strb = new StringBuilder();
        boolean flag = false;
        if (person != null) {
            strb.append(person.getId());
            flag = true;
        }

        if (vehicle != null) {
            strb.append(vehicle.getId());
            flag = true;
        }

        if (flag) {
            strb.append(". ");
        }

        return strb;
    }

    protected LeastCostPathCalculator.Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
        List<Node> nodes = new ArrayList();
        List<Link> links = new ArrayList();
        nodes.add(0, toNode);
        Link tmpLink = this.getData(toNode).getPrevLink();
        if (tmpLink != null) {
            while(true) {
                if (tmpLink.getFromNode() == fromNode) {
                    links.add(0, tmpLink);
                    nodes.add(0, tmpLink.getFromNode());
                    break;
                }

                links.add(0, tmpLink);
                nodes.add(0, tmpLink.getFromNode());
                tmpLink = this.getData(tmpLink.getFromNode()).getPrevLink();
            }
        }

        DijkstraNodeData toNodeData = this.getData(toNode);
        LeastCostPathCalculator.Path path = new LeastCostPathCalculator.Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());
        return path;
    }

    void initFromNode(Node fromNode, Node toNode, double startTime, RouterPriorityQueue<Node> pendingNodes) {
        DijkstraNodeData data = this.getData(fromNode);
        this.visitNode(fromNode, data, pendingNodes, startTime, 0.0, (Link)null);
    }

    protected void relaxNode(Node outNode, Node toNode, RouterPriorityQueue<Node> pendingNodes) {
        DijkstraNodeData outData = this.getData(outNode);
        double currTime = outData.getTime();
        double currCost = outData.getCost();
        if (this.pruneDeadEnds) {
            PreProcessDijkstra.DeadEndData ddOutData = this.getPreProcessData(outNode);
            Iterator var10 = outNode.getOutLinks().values().iterator();

            while(var10.hasNext()) {
                Link l = (Link)var10.next();
                this.relaxNodeLogic(l, pendingNodes, currTime, currCost, toNode, ddOutData);
            }
        } else {
            Iterator var13 = outNode.getOutLinks().values().iterator();

            while(var13.hasNext()) {
                Link l = (Link)var13.next();
                this.relaxNodeLogic(l, pendingNodes, currTime, currCost, toNode, (PreProcessDijkstra.DeadEndData)null);
            }
        }

    }

    void relaxNodeLogic(Link l, RouterPriorityQueue<Node> pendingNodes, double currTime, double currCost, Node toNode, PreProcessDijkstra.DeadEndData ddOutData) {
        if (this.pruneDeadEnds) {
            if (this.canPassLink(l)) {
                Node n = l.getToNode();
                PreProcessDijkstra.DeadEndData ddData = this.getPreProcessData(n);
                if (ddData.getDeadEndEntryNode() == null || ddOutData.getDeadEndEntryNode() != null || this.deadEndEntryNode != null && this.deadEndEntryNode.getId() == ddData.getDeadEndEntryNode().getId()) {
                    this.addToPendingNodes(l, n, pendingNodes, currTime, currCost, toNode);
                }
            }
        } else if (this.canPassLink(l)) {
            this.addToPendingNodes(l, l.getToNode(), pendingNodes, currTime, currCost, toNode);
        }

    }

    protected boolean addToPendingNodes(Link l, Node n, RouterPriorityQueue<Node> pendingNodes, double currTime, double currCost, Node toNode) {
        double travelTime = this.timeFunction.getLinkTravelTime(l, currTime, this.person, this.vehicle);
        double travelCost = this.costFunction.getLinkTravelDisutility(l, currTime, this.person, this.vehicle);
        DijkstraNodeData data = this.getData(n);
        if (!data.isVisited(this.getIterationId())) {
            this.visitNode(n, data, pendingNodes, currTime + travelTime, currCost + travelCost, l);
            return true;
        } else {
            double nCost = data.getCost();
            double totalCost = currCost + travelCost;
            if (totalCost < nCost) {
                this.revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
                return true;
            } else {
                if (totalCost == nCost) {
                    Link prevLink = data.getPrevLink();
                    if (prevLink != null && prevLink.getId().compareTo(l.getId()) > 0) {
                        this.revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
                        return true;
                    }
                }

                return false;
            }
        }
    }

    protected boolean canPassLink(Link link) {
        if (this.modeRestriction == null) {
            return true;
        } else {
            String[] var2 = this.modeRestriction;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String mode = var2[var4];
                if (link.getAllowedModes().contains(mode)) {
                    return true;
                }
            }

            return false;
        }
    }

    protected void revisitNode(Node n, DijkstraNodeData data, RouterPriorityQueue<Node> pendingNodes, double time, double cost, Link outLink) {
        data.visit(outLink, cost, time, this.getIterationId());
        pendingNodes.decreaseKey(n, this.getPriority(data));
    }

    protected void visitNode(Node n, DijkstraNodeData data, RouterPriorityQueue<Node> pendingNodes, double time, double cost, Link outLink) {
        data.visit(outLink, cost, time, this.getIterationId());
        pendingNodes.add(n, this.getPriority(data));
    }

    protected void augmentIterationId() {
        if (this.getIterationId() == Integer.MAX_VALUE) {
            this.iterationID = -2147483647;
            this.resetNetworkVisited();
        } else {
            ++this.iterationID;
        }

    }

    protected int getIterationId() {
        return this.iterationID;
    }

    private void resetNetworkVisited() {
        Iterator var1 = this.network.getNodes().values().iterator();

        while(var1.hasNext()) {
            Node node = (Node)var1.next();
            DijkstraNodeData data = this.getData(node);
            data.resetVisited();
        }

    }

    protected double getPriority(DijkstraNodeData data) {
        return data.getCost();
    }

    protected DijkstraNodeData getData(Node n) {
        DijkstraNodeData r = (DijkstraNodeData)this.nodeData.get(n.getId());
        if (null == r) {
            r = this.createNodeData();
            this.nodeData.put(n.getId(), r);
        }

        return r;
    }

    protected DijkstraNodeData createNodeData() {
        return new DijkstraNodeData();
    }

    protected PreProcessDijkstra.DeadEndData getPreProcessData(Node n) {
        return this.preProcessData.getNodeData(n);
    }

    protected final Person getPerson() {
        return this.person;
    }

    protected final Vehicle getVehicle() {
        return this.vehicle;
    }
}
