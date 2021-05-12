package playground.amit.Delhi.gtfs.elements;

/**
 * Created by Amit on 10/05/2021.
 */
public enum SigmoidFunction {
    LogisticSigmoid,//1 / (1 + exp (-x) )
    BipolarSigmoid, // (1 - exp(-x)) / (1 + exp (-x) )
    HyperbolicTangent, // (1 - exp(-2x)) / (1 + exp (-2x) )
    AlgebraicSigmoid //  x  / (1 + |X| )
}
