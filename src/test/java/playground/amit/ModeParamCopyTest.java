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

package playground.amit;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Created by amit on 07.06.17.
 */


public class ModeParamCopyTest {

    @Test
    public void test () {
        double sourceCarASC = 1.5;
        // create planCalcScoreConfigGroup
        ScoringConfigGroup sourcePlanCalcScore = new ScoringConfigGroup();
        sourcePlanCalcScore.getOrCreateModeParams(TransportMode.car).setConstant(sourceCarASC);
        Assert.assertEquals("ASC in source config is wrong.",sourceCarASC, sourcePlanCalcScore.getOrCreateModeParams(TransportMode.car).getConstant(), MatsimTestUtils.EPSILON);

        // now lets create another planCalcSCore and copy the car params from source
        ScoringConfigGroup copiedScoringConfigGroup = new ScoringConfigGroup();
        Assert.assertEquals("Default ASC for car in config is wrong.",0., copiedScoringConfigGroup.getOrCreateModeParams(TransportMode.car).getConstant(), MatsimTestUtils.EPSILON);

        copiedScoringConfigGroup.addModeParams( copyOfModeParam (sourcePlanCalcScore.getOrCreateModeParams(TransportMode.car)) );
        Assert.assertEquals("ASC in copied config is wrong.", sourceCarASC, copiedScoringConfigGroup.getOrCreateModeParams(TransportMode.car).getConstant(), MatsimTestUtils.EPSILON);

        // now change asc in copied planCalcScore only
        double copiedCarASC = 2.0;
        copiedScoringConfigGroup.getOrCreateModeParams(TransportMode.car).setConstant(copiedCarASC);

        Assert.assertEquals("ASC in source config is wrong.", sourceCarASC, sourcePlanCalcScore.getOrCreateModeParams(TransportMode.car).getConstant(), MatsimTestUtils.EPSILON);
        Assert.assertEquals("ASC in copied config is wrong.", copiedCarASC, copiedScoringConfigGroup.getOrCreateModeParams(TransportMode.car).getConstant(), MatsimTestUtils.EPSILON);
    }
    private ScoringConfigGroup.ModeParams copyOfModeParam(final ScoringConfigGroup.ModeParams modeParams) {
        ScoringConfigGroup.ModeParams newModeParams = new ScoringConfigGroup.ModeParams(modeParams.getMode());
        newModeParams.setConstant(modeParams.getConstant());
        return newModeParams;
    }
}
