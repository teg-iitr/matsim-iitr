package playground.shivam.signals.lanes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.*;

import java.util.Arrays;
import java.util.Collections;

import static playground.shivam.signals.SignalUtils.*;

public class CreateLanes {
    public static void createLanes(Scenario scenario) {
        Lanes lanes = scenario.getLanes();
        LanesFactory factory = lanes.getFactory();

        // create lanes for link 2_3
        LanesToLinkAssignment lanesForLink23 = factory
                .createLanesToLinkAssignment(Id.createLinkId("2_3"));
        lanes.addLanesToLinkAssignment(lanesForLink23);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.s", Lane.class), Id.create("2_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);

        // straight lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink23, factory,
                Id.create("2_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);


        // create lanes for link 7_3
        LanesToLinkAssignment lanesForLink73 = factory
                .createLanesToLinkAssignment(Id.createLinkId("7_3"));
        lanes.addLanesToLinkAssignment(lanesForLink73);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("7_3.l", Lane.class), Id.create("7_3.s", Lane.class), Id.create("7_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);

        // straight lane (alignment 0)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink73, factory,
                Id.create("7_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);


        // create lanes for link 4_3
        LanesToLinkAssignment lanesForLink43 = factory
                .createLanesToLinkAssignment(Id.createLinkId("4_3"));
        lanes.addLanesToLinkAssignment(lanesForLink43);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.s", Lane.class), Id.create("4_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_8", Link.class)), null);

        // straight lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink43, factory,
                Id.create("4_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);


        // create lanes for link 8_3
        LanesToLinkAssignment lanesForLink83 = factory
                .createLanesToLinkAssignment(Id.createLinkId("8_3"));
        lanes.addLanesToLinkAssignment(lanesForLink83);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.ol", Lane.class), LANE_CAPACITY, LINK_LENGTH, 0, NO_LANES,
                null, Arrays.asList(Id.create("8_3.l", Lane.class), Id.create("8_3.s", Lane.class), Id.create("8_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.l", Lane.class), LANE_CAPACITY, LANE_LENGTH, 1, NO_LANES,
                Collections.singletonList(Id.create("3_2", Link.class)), null);

        // straight lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.s", Lane.class), LANE_CAPACITY, LANE_LENGTH, 0, NO_LANES,
                Collections.singletonList(Id.create("3_7", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink83, factory,
                Id.create("8_3.r", Lane.class), LANE_CAPACITY, LANE_LENGTH, -1, NO_LANES,
                Collections.singletonList(Id.create("3_4", Link.class)), null);
    }
}
