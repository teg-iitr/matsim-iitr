package playground.nidhi.examJan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeanMedian {


    private final Map<Integer, Map<List<String>, List<Double>>> house2Member;

    private final Map<List<String>, List<Double>> memberInfo1=new HashMap<>();
    private final List<String> memberIDs1 = new ArrayList<>();
    private final List<Double> income1= new ArrayList<>();

    private final Map<List<String>, List<Double>> memberInfo2=new HashMap<>();
    private final List<String> memberIDs2 = new ArrayList<>();
    private final List<Double> income2= new ArrayList<>();

    private final Map<List<String>, List<Double>> memberInfo3=new HashMap<>();
    private final List<String> memberIDs3 = new ArrayList<>();
    private final List<Double> income3= new ArrayList<>();


    public MeanMedian() {
        house2Member = new HashMap<>();
    }


    public Map<Integer, Map<List<String>, List<Double>>> getHouse2Member() {
        int householdId1 = 1201;
        int householdId2 = 1202;
        int householdId3 = 1203;


        income1.add(23.0);
        income1.add(13.0);
        income1.add(5.0);
        for (int n = 1; n < 4; n++) {
            memberIDs1.add(householdId1 + "." + n);
        }

        income2.add(45.0);
        income2.add(78.0);
        income2.add(0.1);
        income2.add(2.2);
        for (int n = 1; n < 5; n++) {
            memberIDs2.add(householdId2 + "." + n);
        }

        income3.add(5.0);
        income3.add(19.0);
        income3.add(11.0);
        for (int n = 1; n < 4; n++) {
            memberIDs3.add(householdId3 + "." + n);
        }

        memberInfo1.put(memberIDs1,income1);
        memberInfo2.put(memberIDs2,income2);
        memberInfo3.put(memberIDs3,income3);

        house2Member.put(householdId1,memberInfo1);
        house2Member.put(householdId2,memberInfo2 );
        house2Member.put(householdId3, memberInfo3);

        System.out.println(house2Member);
        System.out.println();
        return house2Member;

    }


    public List<Double> getIncome1() {
        return income1;
    }

    public List<Double> getIncome2() {
        return income2;
    }

    public List<Double> getIncome3() {
        return income3;
    }

}