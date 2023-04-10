package playground.shivam.signals.signalSystems;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.Lane;

import java.util.Arrays;

public class CreateGroupsAndSystem {
    public static void createGroupsAndSystem(SignalSystemsData signalSystemsData, SignalGroupsData signalGroupsData) {
        SignalSystemData sys = signalSystemsData.getFactory().createSignalSystemData(Id.create("3", SignalSystem.class));
        signalSystemsData.addSignalSystemData(sys);
        SignalSystemsDataFactory factory = signalSystemsData.getFactory();

//        SignalUtils.createAndAddSignal(sys, factory, Id.create("23_1", Signal.class), Id.createLinkId("2_3"),
//                Arrays.asList(Id.create("2_3.l", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("23_2", Signal.class), Id.createLinkId("2_3"),
//                Arrays.asList(Id.create("2_3.s", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("23_3", Signal.class), Id.createLinkId("2_3"),
//                Arrays.asList(Id.create("2_3.r", Lane.class)));
//
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("73_1", Signal.class), Id.createLinkId("7_3"),
//                Arrays.asList(Id.create("7_3.l", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("73_2", Signal.class), Id.createLinkId("7_3"),
//                Arrays.asList(Id.create("7_3.s", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("73_3", Signal.class), Id.createLinkId("7_3"),
//                Arrays.asList(Id.create("7_3.r", Lane.class)));
//
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("43_1", Signal.class), Id.createLinkId("4_3"),
//                Arrays.asList(Id.create("4_3.l", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("43_2", Signal.class), Id.createLinkId("4_3"),
//                Arrays.asList(Id.create("4_3.s", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("43_3", Signal.class), Id.createLinkId("4_3"),
//                Arrays.asList(Id.create("4_3.r", Lane.class)));
//
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("83_1", Signal.class), Id.createLinkId("8_3"),
//                Arrays.asList(Id.create("8_3.l", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("83_2", Signal.class), Id.createLinkId("8_3"),
//                Arrays.asList(Id.create("8_3.s", Lane.class)));
//        SignalUtils.createAndAddSignal(sys, factory, Id.create("83_3", Signal.class), Id.createLinkId("8_3"),
//                Arrays.asList(Id.create("8_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("23_sl", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("23_r", Signal.class), Id.createLinkId("2_3"),
                Arrays.asList(Id.create("2_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("73_sl", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.l", Lane.class), Id.create("7_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("73_r", Signal.class), Id.createLinkId("7_3"),
                Arrays.asList(Id.create("7_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("43_sl", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("43_r", Signal.class), Id.createLinkId("4_3"),
                Arrays.asList(Id.create("4_3.r", Lane.class)));

        SignalUtils.createAndAddSignal(sys, factory, Id.create("83_sl", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.l", Lane.class), Id.create("8_3.s", Lane.class)));
        SignalUtils.createAndAddSignal(sys, factory, Id.create("83_r", Signal.class), Id.createLinkId("8_3"),
                Arrays.asList(Id.create("8_3.r", Lane.class)));
        // create a signal group for every signal
        SignalUtils.createAndAddSignalGroups4Signals(signalGroupsData, sys);

    }
}
