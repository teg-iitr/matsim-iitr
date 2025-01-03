/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author knagel
 *
 * @see DefaultQNetworkFactory
 */
public final class TrafficCharQNetworkFactory implements QNetworkFactory {

	private final QSimConfigGroup qsimConfig;
	private final EventsManager events;
	private final Network network;
	private final Scenario scenario;
//	private NetsimEngineContext context;
	private QNetsimEngineI.NetsimInternalInterface netsimEngine;
	private LinkSpeedCalculator linkSpeedCalculator;// = new DefaultLinkSpeedCalculator();
	private TurnAcceptanceLogic turnAcceptanceLogic = new DefaultTurnAcceptanceLogic();
	private final TrafficCharConfigGroup tcConfigGroup;
	private final Map<String, NetsimEngineContext> roadType2Contexts = new HashMap<>();

	@Inject
	public TrafficCharQNetworkFactory(EventsManager events, Scenario scenario, LinkSpeedCalculator linkSpeedCalculator ) {
		this.events = events;
		this.scenario = scenario;
		this.network = scenario.getNetwork();
		this.qsimConfig = scenario.getConfig().qsim();
		this.tcConfigGroup = (TrafficCharConfigGroup) scenario.getConfig().getModules().get(TrafficCharConfigGroup.GROUP_NAME);
		this.linkSpeedCalculator = linkSpeedCalculator;
	}

	@Override
	public void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, QNetsimEngineI.NetsimInternalInterface netsimEngine1) {
  		this.netsimEngine = netsimEngine1;
		double effectiveCellSize = network.getEffectiveCellSize();
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (!Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngineWithThreadpool.createAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);

		this.tcConfigGroup.getRoadType2TrafficChar().forEach((key, value) -> {
			NetsimEngineContext context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, value, mobsimTimer, linkWidthCalculator);
			this.roadType2Contexts.put(key, context);
		});
	}
	@Override
	public QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {
		String rT = (String) link.getAttributes().getAttribute(TrafficCharConfigGroup.ROAD_TYPE);
		NetsimEngineContext context = this.roadType2Contexts.get(rT);

		QueueWithBuffer.Builder laneFactory = new QueueWithBuffer.Builder(context);
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine);
		linkBuilder.setLaneFactory(laneFactory);
		linkBuilder.setLinkSpeedCalculator( linkSpeedCalculator );

		return linkBuilder.build(link, toQueueNode);
	}


    @Override
	public QNodeI createNetsimNode(final Node node) {
		// just using one of the qsimConfigGroup and netsimEngineContext in the node logic. Mar'22 AA, SA
//		Map.Entry<String, QSimConfigGroup> next = this.tcConfigGroup.getRoadType2TrafficChar().entrySet().iterator().next();

		NetsimEngineContext context = this.roadType2Contexts.get(TrafficCharConfigGroup.ROAD_TYPE_DEFAULT);

		QNodeImpl.Builder builder = new QNodeImpl.Builder(netsimEngine, context, context.qsimConfig);

		builder.setTurnAcceptanceLogic( this.turnAcceptanceLogic );

		return builder.build( node );
	}
	public final void setLinkSpeedCalculator(LinkSpeedCalculator linkSpeedCalculator) {
		this.linkSpeedCalculator = linkSpeedCalculator;
	}
	public final void setTurnAcceptanceLogic( TurnAcceptanceLogic turnAcceptanceLogic ) {
		this.turnAcceptanceLogic = turnAcceptanceLogic;
	}
}
