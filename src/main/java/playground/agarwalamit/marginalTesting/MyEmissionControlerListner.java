/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.marginalTesting;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;


/**
 * @author amit
 */
public class MyEmissionControlerListner  implements StartupListener, IterationStartsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(MyEmissionControlerListner.class);

	Controler controler;
	String emissionEventOutputFile;
	Integer lastIteration;
	MyEmissionModule emissionModule;
	EventWriterXML emissionEventWriter;

	public MyEmissionControlerListner() {

	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		lastIteration = controler.getConfig().controler().getLastIteration();
		logger.info("emissions will be calculated for iteration " + lastIteration);

		Scenario scenario = controler.getScenario() ;
		emissionModule = new MyEmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EventsManager eventsManager = controler.getEvents();
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Integer iteration = event.getIteration();

		if(lastIteration.equals(iteration)){
			emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			logger.info("creating new emission events writer...");
			emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
			logger.info("adding emission events writer to emission events stream...");
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		logger.info("closing emission events file...");
		emissionEventWriter.closeFile();
		emissionModule.writeEmissionInformation(emissionEventOutputFile);
logger.info("Total Delays in hours is "+emissionModule.getTotalDelaysInHours());
	}
}