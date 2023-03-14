package playground.shivam.Dadar.evacuation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.utils.PersonFilter;

import java.util.Arrays;
import java.util.List;

public class DadarPersonFilter implements PersonFilter {
    public static boolean isPersonBelongsToUrban(Id<Person> personId){
        return personId.toString().startsWith("slum") || personId.toString().startsWith("nonSlum");
    }

    public static boolean isPersonBelongsToCommuter(Id<Person> personId){
        return Arrays.asList(personId.toString().split("_")).contains("E2I");
    }

    public static boolean isPersonBelongsToThroughTraffic(Id<Person> personId){
        return Arrays.asList(personId.toString().split("_")).contains("E2E");
    }
    public static DadarUtils.DadarUserGroup getUserGroup(Id<Person> personId){
        if(isPersonBelongsToUrban(personId)) return DadarUtils.DadarUserGroup.urban;
        else if(isPersonBelongsToCommuter(personId)) return DadarUtils.DadarUserGroup.commuter;
        else if (isPersonBelongsToThroughTraffic(personId)) return DadarUtils.DadarUserGroup.through;
        else throw new RuntimeException("Person id "+personId+" do not belong to any of the predefined user group. Aborting ...");
    }
    @Override
    public String getUserGroupAsStringFromPersonId(Id<Person> personId) {
        return getUserGroup(personId).toString();
    }

    @Override
    public List<String> getUserGroupsAsStrings() {
        return null;
    }
}
