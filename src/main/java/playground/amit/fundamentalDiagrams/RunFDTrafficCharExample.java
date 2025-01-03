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

package playground.amit.fundamentalDiagrams;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.amit.fundamentalDiagrams.core.FDModule;
import playground.amit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

import java.util.Arrays;
import java.util.List;

/**
 * Created by amit on 16/02/2017.
 */

public class RunFDTrafficCharExample {

    public static void main(String[] args) {

        boolean runUsingConfig = false;

        Scenario scenario ;

        if (runUsingConfig ) {
            String configFile = "input/FD/output_config.xml";
            scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
        } else {
            scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        }

        String myDir = "output/FDTrafficCharExample";
        String outFolder ="/1lane/";
        scenario.getConfig().controller().setOutputDirectory(myDir+outFolder);


        QSimConfigGroup qsim = scenario.getConfig().qsim();
        List<String> mainModes = Arrays.asList("car", "bicycle");
        qsim.setMainModes(mainModes);
        Vehicles vehicles = scenario.getVehicles();
        for (String mode : mainModes) {
            VehicleType veh = VehicleUtils.createVehicleType(Id.create(mode,VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getStuckTime(mode));
            veh.setNetworkMode(mode);
            vehicles.addVehicleType(veh);
        }
        ChangeModeConfigGroup changeTripMode = scenario.getConfig().changeMode();
        changeTripMode.setModes(new String [] {TransportMode.car,"bicycle"});

        TrafficCharConfigGroup trafficCharConfigGroup = new TrafficCharConfigGroup();
        QSimConfigGroup qSimConfigGroupPassingQ = new QSimConfigGroup();
        qSimConfigGroupPassingQ.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        trafficCharConfigGroup.addQSimConfigGroup("PassingQ", qSimConfigGroupPassingQ);

        trafficCharConfigGroup.addQSimConfigGroup(TrafficCharConfigGroup.ROAD_TYPE_DEFAULT, scenario.getConfig().qsim());

//        scenario.getConfig().getModules().put(TrafficCharConfigGroup.GROUP_NAME, trafficCharConfigGroup);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new FDModule(scenario));
        controler.run();

    }
}
