/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.amit.fundamentalDiagrams.core.pointsToRun;

import com.google.inject.Inject;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import playground.amit.fundamentalDiagrams.core.FDDataContainer;

/**
 * Created by amit on 23.05.18.
 */

public class FDAgentsGeneratorControlerListner implements IterationStartsListener, TerminationCriterion {


    @Inject
    private FDDataContainer fdDataContainer;
    @Inject
    private FDAgentsGenerator fdAgentsGenerator;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        this.fdAgentsGenerator.createPersons();
    }

//    @Override
//    public boolean continueIterations(int iteration) {
//        return ! this.fdDataContainer.getListOfPointsToRun().isEmpty();
//    }

    @Override
    public boolean mayTerminateAfterIteration(int i) {
        return false;
    }

    @Override
    public boolean doTerminate(int i) {
        return ! this.fdDataContainer.getListOfPointsToRun().isEmpty();
    }
}
