package playground.amit.Delhi.gtfs;

/**
 * Created by Amit on 08/05/2021.
 */
public class SigmoidFunctionUtils {

    public enum SigmoidFunction {
        LogisticSigmoid,//1 / (1 + exp (-x) )
        BipolarSigmoid, // (1 - exp(-x)) / (1 + exp (-x) )
        HyperbolicTangent, // (1 - exp(-2x)) / (1 + exp (-2x) )
        AlgebraicSigmoid //  x  / (1 + |X| )
    }

    public static double getValue(SigmoidFunction sigmoidFunction, double x) {
        switch (sigmoidFunction) {
            case LogisticSigmoid:
                return 1/(1+Math.exp(-x));
            case BipolarSigmoid:
                double ex = Math.exp(-x);
                return (1- ex) / (1 + ex);
            case HyperbolicTangent:
                double e2x = Math.exp(-2*x);
                return (1- e2x) / (1 + e2x);
            case AlgebraicSigmoid:
                return x / (1 + Math.abs(x));
        }
        return 0.;
    }
}
