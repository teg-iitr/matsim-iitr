package playground.shivam.signals.signalSystems;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;

import static playground.shivam.signals.SignalUtils.*;

public class CreateSignalControl {

    public static void createFixedSystemControl(SignalControlData control, Id<SignalSystem> signalSystemId, String SignalController) {
        SignalControlDataFactory fac = control.getFactory();

        // create and add signal control for the given system id
        SignalSystemControllerData controller = fac.createSignalSystemControllerData(signalSystemId);
        control.addSignalSystemControllerData(controller);
        controller.setControllerIdentifier(SignalController);

        // create and add signal plan with defined cycle time and offset 0
        SignalPlanData plan = SignalUtils.createSignalPlan(fac, CYCLE, 0);
        controller.addSignalPlanData(plan);

        // create and add control settings for signal groups - only needed for fixed time signal
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("23_1", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("23_2", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("23_3", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));

        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("73_1", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("73_2", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("73_3", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));

        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("43_1", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("43_2", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("43_3", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));

        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("83_1", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("83_2", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
                Id.create("83_3", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));

//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("23_sl", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("23_r", SignalGroup.class), OFFSET_LEFT_APPROACH, DROPPING_LEFT_APPROACH));
//
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("73_sl", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("73_r", SignalGroup.class), OFFSET_TOP_APPROACH, DROPPING_TOP_APPROACH));
//
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("43_sl", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("43_r", SignalGroup.class), OFFSET_RIGHT_APPROACH, DROPPING_RIGHT_APPROACH));
//
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("83_sl", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
//        plan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac,
//                Id.create("83_r", SignalGroup.class), OFFSET_BOTTOM_APPROACH, DROPPING_BOTTOM_APPROACH));
    }

    public static void createAdaptiveSystemControl(SignalControlData control, Id<SignalSystem> signalSystemId, String SignalController) {
        SignalControlDataFactory fac = control.getFactory();

        // create and add signal control for the given system id
        SignalSystemControllerData controller = fac.createSignalSystemControllerData(signalSystemId);
        control.addSignalSystemControllerData(controller);
        controller.setControllerIdentifier(SignalController);
    }
}
