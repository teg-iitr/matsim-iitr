/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.amit.mixedTraffic.patnaIndia.analysis;

import java.io.BufferedWriter;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.amit.analysis.linkVolume.LinkVolumeHandler;

/**
 * @author amit
 */

public class PatnaLinkVolumeWriter {
	
	private static final String dir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/shpNetwork/incomeDependent/c13/";
	private static final String eventsFile = dir+"/output_events.xml.gz";
	private final LinkVolumeHandler handler;

	public PatnaLinkVolumeWriter(String vehiclesFile){
		handler = new LinkVolumeHandler(vehiclesFile);
	}

	public PatnaLinkVolumeWriter(){
		handler = new LinkVolumeHandler();
	}

	public static void main(String[] args) {
		PatnaLinkVolumeWriter plvw = new PatnaLinkVolumeWriter();
		plvw.processEventsFile(eventsFile);
		plvw.writeCountData(dir+"/analysis/link2time2Vol.txt");
	}
	
	public void processEventsFile(final String eventsFile){
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
	}
	
	public void writeCountData(final String outFile){
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\ttimeBin\tcount\n");
			Map<Id<Link>, Map<Integer, Double>> linkId2TimeSlot2LinkCount = this.handler.getLinkId2TimeSlot2LinkCount();
			for(Id<Link> l : linkId2TimeSlot2LinkCount.keySet()) {
				for (int i : linkId2TimeSlot2LinkCount.get(l).keySet()) {
					writer.write(l + "\t" + i + "\t" + linkId2TimeSlot2LinkCount.get(l).get(i) + "\n");
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}

	public void writePCUData(final String outFile){
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\ttimeBin\tcount\n");
			Map<Id<Link>, Map<Integer, Double>> linkId2TimeSlot2LinkCount = this.handler.getLinkId2TimeSlot2LinkVolumePCU();
			for(Id<Link> l : linkId2TimeSlot2LinkCount.keySet()){
				for (int i : linkId2TimeSlot2LinkCount.get(l).keySet() ){
					writer.write(l+"\t"+i+"\t"+linkId2TimeSlot2LinkCount.get(l).get(i)+"\n");
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}

}

