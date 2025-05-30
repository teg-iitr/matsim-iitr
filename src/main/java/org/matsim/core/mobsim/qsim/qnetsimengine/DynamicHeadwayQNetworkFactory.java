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


import jakarta.inject.Inject;
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


import static org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQNetsimEngine.createAgentSnapshotInfoBuilder;


/**
 *
 * @author knagel
 *
 * @see DefaultQNetworkFactory
 */
public final class DynamicHeadwayQNetworkFactory implements QNetworkFactory {

	private final QSimConfigGroup qsimConfig;
	private final EventsManager events;
	private final Network network;
	private final Scenario scenario;
	private NetsimEngineContext context;
	private QNetsimEngineI.NetsimInternalInterface netsimEngine;
	private LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator();
	private TurnAcceptanceLogic turnAcceptanceLogic = new DefaultTurnAcceptanceLogic();

	@Inject
	public DynamicHeadwayQNetworkFactory(EventsManager events, Scenario scenario ) {
		this.events = events;
		this.scenario = scenario;
		this.network = scenario.getNetwork();
		this.qsimConfig = scenario.getConfig().qsim();
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
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );


		context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator);

	}
	@Override
	public QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {

		DynamicHeadwayQueueWithBuffer.Builder laneFactory = new DynamicHeadwayQueueWithBuffer.Builder(context);
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine);
		linkBuilder.setLaneFactory(laneFactory);
		linkBuilder.setLinkSpeedCalculator( linkSpeedCalculator );

		return linkBuilder.build(link, toQueueNode);
	}


    @Override
	public QNodeI createNetsimNode(final Node node) {
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
