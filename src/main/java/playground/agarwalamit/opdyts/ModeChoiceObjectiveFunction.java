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

package playground.agarwalamit.opdyts;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.analysis.kai.DataMap;
import org.matsim.contrib.analysis.kai.Databins;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.agarwalamit.opdyts.equil.EquilMixedTrafficObjectiveFunctionPenalty;

import java.util.*;

/**
 *
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
public class ModeChoiceObjectiveFunction implements MATSimObjectiveFunction<MATSimState> {
    private static final Logger log = Logger.getLogger( ModeChoiceObjectiveFunction.class );

    private final MainModeIdentifier mainModeIdentifier ;

    private DistanceDistribution distriInfo ;

    @Inject private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
    @Inject private TripRouter tripRouter ;
    @Inject private Network network ;

    @Inject
    private ObjectiveFunctionEvaluator objectiveFunctionEvaluator;
	// Documentation: "Guice injects ... fields of all values that are bound using toInstance(). These are injected at injector-creation time."
    // https://github.com/google/guice/wiki/InjectionPoints
    // I read that as "the fields are injected (every time again) when the instance is injected".
    // This is the behavior that we want here.  kai, sep'16

    // statistics types:
    enum StatType {
        tripBeelineDistances
    }

    private final Map<StatType,Databins<String>> simStatsContainer = new TreeMap<>() ;
    private final Map<StatType,DataMap<String>> sumsContainer  = new TreeMap<>() ;
    private final Map<StatType,Databins<String>> refStatsContainer = new TreeMap<>() ;
	
	public ModeChoiceObjectiveFunction(
	        final DistanceDistribution distriInfo
//									   ,ObjectiveFunctionType objectiveFunctionType
    ) {
//		this.objectiveFunctionType = objectiveFunctionType;
        this.distriInfo = distriInfo;
        for ( StatType statType : StatType.values() ) {
            // define the bin boundaries:
            switch ( statType ) {
                case tripBeelineDistances: {
                    double[] dataBoundariesTmp = distriInfo.getDistClasses();
                    {
                        this.simStatsContainer.put( statType, new Databins<>( statType.name(), dataBoundariesTmp )) ;
                    }
                    {
                        final Databins<String> databins = new Databins<>( statType.name(), dataBoundariesTmp ) ;
                        fillDatabins(databins);
                        this.refStatsContainer.put( statType, databins) ;
                    }
                    break; }
                default:
                    throw new RuntimeException("not implemented") ;
            }
        }

        // for Patna, all legs have same trip mode.
        switch (this.distriInfo.getOpdytsScenario()){
            case EQUIL:
                mainModeIdentifier = new TransportPlanningMainModeIdentifier();
                break;
            case EQUIL_MIXEDTRAFFIC:
            case PATNA_1Pct:
            case PATNA_10Pct:
                mainModeIdentifier = new MainModeIdentifier() {
                    @Override
                    public String identifyMainMode(List<? extends PlanElement> tripElements) {
                        for (PlanElement pe : tripElements) {
                            if (pe instanceof Leg) return ((Leg) pe).getMode();
                        }
                        throw new RuntimeException("No instance of leg is found.");
                    }
                };
                break;
            default:
                throw new RuntimeException("not implemented");
        }
    }

    private void fillDatabins(final Databins<String> databins) {
        Map<String, double []> mode2distanceBasedLegs = distriInfo.getMode2DistanceBasedLegs();
        for (String mode : mode2distanceBasedLegs.keySet()) {
            double [] distBasedLegs = mode2distanceBasedLegs.get(mode);
            for (int idx = 0; idx < databins.getDataBoundaries().length; idx++) {
                databins.addValue(mode, idx, distBasedLegs[idx]);
            }
        }
    }

    @Override
    public double value(MATSimState state) {

        resetContainers();

        MATSimState matSimState = (MATSimState) state;
        Set<Id<Person>> persons = matSimState.getPersonIdView();

        StatType statType = StatType.tripBeelineDistances;

        for (Id<Person> personId : persons) {
            Plan plan = matSimState.getSelectedPlan(personId);
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan
//                    , tripRouter.getStageActivityTypes()
            );
            for (TripStructureUtils.Trip trip : trips) {
                List<String> tripTypes = new ArrayList<>();
                String mode = mainModeIdentifier.identifyMainMode(trip.getLegsOnly());
                tripTypes.add(mode);
                double item = calcBeelineDistance(trip.getOriginActivity(), trip.getDestinationActivity());
                addItemToAllRegisteredTypes(tripTypes, statType, item);
            }
        }

        double objectiveFnValue = 0.;

        for ( Map.Entry<StatType, Databins<String>> entry : simStatsContainer.entrySet() ) {
            StatType theStatType = entry.getKey();  // currently only one type ;
            log.warn("statType=" + theStatType);
            objectiveFnValue = objectiveFunctionEvaluator.getObjectiveFunctionValue(this.refStatsContainer.get(theStatType), entry.getValue());
        }

        double penalty = 0.;
        switch (this.distriInfo.getOpdytsScenario()) {
            case EQUIL:
            case PATNA_1Pct:
            case PATNA_10Pct:
                break;
            case EQUIL_MIXEDTRAFFIC:
                double ascBicycle = planCalcScoreConfigGroup.getModes().get("bicycle").getConstant();
                double bicycleShare = objectiveFunctionEvaluator.getModeToShare().get("bicycle");
                penalty = EquilMixedTrafficObjectiveFunctionPenalty.getPenalty(bicycleShare, ascBicycle);
        }

        return objectiveFnValue + penalty;
    }

    private void resetContainers() {
        for ( StatType statType : StatType.values() ) {
            this.simStatsContainer.get(statType).clear() ;
            this.sumsContainer.computeIfAbsent(statType, k -> new DataMap<>());
            this.sumsContainer.get(statType).clear() ;
        }
    }

    private void addItemToAllRegisteredTypes(List<String> filters, StatType statType, double item) {
        // ... go through all filter to which the item belongs ...
        for ( String filter : filters ) {

            // ...  add the "item" to the correct bin in the container:
            int idx = this.simStatsContainer.get(statType).getIndex(item) ;
            this.simStatsContainer.get(statType).inc( filter, idx ) ;

            // also add it to the sums container:
            this.sumsContainer.get(statType).addValue( filter, item ) ;

        }
    }

    private static int noCoordCnt = 0 ;
    private double calcBeelineDistance(final Activity fromAct, final Activity toAct) {
        double item;
        if ( fromAct.getCoord()!=null && toAct.getCoord()!=null ) {
            item = CoordUtils.calcEuclideanDistance(fromAct.getCoord(), toAct.getCoord()) ;
        } else {
            if ( noCoordCnt < 1 ) {
                noCoordCnt ++ ;
				log.warn("either fromAct or to Act has no Coord; using link coordinates as substitutes.") ;
				log.warn(Gbl.ONLYONCE ) ;
            }
            Link fromLink = network.getLinks().get( fromAct.getLinkId() ) ;
            Link   toLink = network.getLinks().get(   toAct.getLinkId() ) ;
            item = CoordUtils.calcEuclideanDistance( fromLink.getCoord(), toLink.getCoord() ) ;
        }
        return item;
    }
}
