package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork.wfh;
/**
 * @author amit
 */

public interface WFHActivity {

    String WFH_PLAN_TYPE = "WFH";

    boolean isWFHActivity(String actType);

    static boolean isWFHPlan(String planType) {
        if(planType==null){
            throw new RuntimeException("Plantype is null.");
        }
        return planType.equals(WFHActivity.WFH_PLAN_TYPE);
    };

}
