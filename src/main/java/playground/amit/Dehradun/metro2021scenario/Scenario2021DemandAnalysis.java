package playground.amit.Dehradun.metro2021scenario;

/**
 *
 * @author Amit
 *
 * <b>The utility parameters for the modes car, motorbike, IPT, bus, metro are given in the Table 6-7 of metro report.
 * However, the ASCs are not given. </b>
 * <b>The ASCs for the base case (2017; car, motorbike, IPT, bus, bicycle, walk) can be estimated by creating a simulation model using the OD matrix and the modal share. </b>
 * <b> We use the ASCs for the car, motorbike, IPT, bus, bicycle, walk from the base case and we calibrate the ASC for the metro using the given OD matrix for all trips and metro trips in 2021.</b>
 * <b> While applying MNL for merto OD matrix with respect to the overall OD matrix, only ASC for metro is unknown.</b>
 * <b> After this stage, we simply create the ring road scenario (probably use graphhoper routing engine) and travel time between Haridwar-Rishikesh connectivity using integrated graphhoper routing engine and here maps API.</b>
 * <b> This should give the new metro ridership, i.e., impact of ring road as well as the connectivity of NH between Haridwar and Rishikesh.</b>
 */
public class Scenario2021DemandAnalysis {


}
