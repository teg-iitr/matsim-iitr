package playground.agarwalamit.mixedTraffic.patnaIndia.covidWork;
/**
 * @author amit
 */

public class WFHActivity {

    private final String wfhActivityNamePrefix;

    public WFHActivity(){
        this("WorkFromHome");
    }

    public WFHActivity(String wfhActivityNamePrefix) {
        this.wfhActivityNamePrefix = wfhActivityNamePrefix;
    }

    public String getWfhActivityNamePrefix(){
        return this.wfhActivityNamePrefix;
    }

}
