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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.amit.fundamentalDiagrams.core.FDModule;
import playground.amit.utils.FileUtils;
import playground.shivam.trafficChar.TrafficCharModule;
import playground.shivam.trafficChar.core.TrafficCharConfigGroup;

/**
 * Created by amit on 16/02/2017.
 */

public class RunFDDataExample {

    public static void main(String[] args) {

        boolean runUsingConfig = false;

        Scenario scenario ;

        if (runUsingConfig ) {
            String configFile = "input/FD/output_config.xml";
            scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
        } else {
            scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        }

        String myDir = "output/FDDataExample";
        String outFolder ="/1lane/";
        scenario.getConfig().controler().setOutputDirectory(myDir+outFolder);
        TrafficCharConfigGroup trafficCharConfigGroup = new TrafficCharConfigGroup();

        QSimConfigGroup qSimConfigGroupFIFO = new QSimConfigGroup();
        qSimConfigGroupFIFO.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
        trafficCharConfigGroup.addQSimConfigGroup("FIFO", qSimConfigGroupFIFO);
        trafficCharConfigGroup.addQSimConfigGroup("default", scenario.getConfig().qsim());
        scenario.getConfig().getModules().put(TrafficCharConfigGroup.GROUP_NAME, trafficCharConfigGroup);

        Controler controler = new Controler(scenario);
//        controler.addOverridingQSimModule(new FDQSimModule());
        controler.addOverridingQSimModule(new TrafficCharModule());
        controler.addOverridingModule(new FDModule(scenario));
        controler.run();

        FDUtils.cleanOutputDir(scenario.getConfig().controler().getOutputDirectory());
    }
}
