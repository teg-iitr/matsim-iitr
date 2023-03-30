package playground.shivam.signals.signalSystems;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.ConflictDataFactory;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.conflicts.io.ConflictingDirectionsWriter;
import org.matsim.contrib.signals.model.SignalSystem;

public class CreateConflictData {
    public static void createConflictData(ConflictData conflictData, String outputDirectory) {
        ConflictDataFactory conflictDataFactory = conflictData.getFactory();
        IntersectionDirections intersectionDirections = conflictDataFactory.createConflictingDirectionsContainerForIntersection(Id.create("3", SignalSystem.class), Id.createNodeId("3"));

        // left lane direction - right of way
        Direction direction2_7 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("2_3"), Id.createLinkId("3_4"), Id.create("2_4", Direction.class));
        // straight lane direction - right of way
        Direction direction2_4 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("2_3"), Id.createLinkId("3_7"), Id.create("2_7", Direction.class));
        // right lane direction - must yield
        Direction direction2_8 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("2_3"), Id.createLinkId("3_8"), Id.create("2_8", Direction.class));

        // left lane direction
        Direction direction7_4 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("7_3"), Id.createLinkId("3_4"), Id.create("7_4", Direction.class));
        // straight lane direction
        Direction direction7_8 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("7_3"), Id.createLinkId("3_8"), Id.create("7_8", Direction.class));
        // right lane direction
        Direction direction7_2 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("7_3"), Id.createLinkId("3_2"), Id.create("7_2", Direction.class));

        // left lane direction - right of way
        Direction direction4_8 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("4_3"), Id.createLinkId("3_8"), Id.create("4_8", Direction.class));
        // straight lane direction - right of way
        Direction direction4_2 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("4_3"), Id.createLinkId("3_2"), Id.create("4_2", Direction.class));
        // right lane direction - must yield
        Direction direction4_7 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("4_3"), Id.createLinkId("3_7"), Id.create("4_7", Direction.class));

        // left lane direction
        Direction direction8_2 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("8_3"), Id.createLinkId("3_2"), Id.create("8_2", Direction.class));
        // straight lane direction
        Direction direction8_7 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("8_3"), Id.createLinkId("3_7"), Id.create("8_7", Direction.class));
        // right lane direction
        Direction direction8_4 = conflictDataFactory.createDirection(Id.create("3", SignalSystem.class), Id.createNodeId("3"), Id.createLinkId("8_3"), Id.createLinkId("3_4"), Id.create("8_4", Direction.class));

        {
            // for left approaching vehicles
            {
                //left turning
                direction2_7.addDirectionWhichMustYield(direction4_7.getId());

                direction2_7.addDirectionWithRightOfWay(direction8_7.getId());

                direction2_7.addNonConflictingDirection(direction2_4.getId());
                direction2_7.addNonConflictingDirection(direction2_8.getId());
                direction2_7.addNonConflictingDirection(direction4_2.getId());
                direction2_7.addNonConflictingDirection(direction4_8.getId());
                direction2_7.addNonConflictingDirection(direction7_2.getId());
                direction2_7.addNonConflictingDirection(direction7_4.getId());
                direction2_7.addNonConflictingDirection(direction7_8.getId());
                direction2_7.addNonConflictingDirection(direction8_2.getId());
                direction2_7.addNonConflictingDirection(direction8_4.getId());
            }

            {
                // straight going
                direction2_4.addConflictingDirection(direction4_7.getId());
                direction2_4.addConflictingDirection(direction7_2.getId());
                direction2_4.addConflictingDirection(direction7_8.getId());
                direction2_4.addConflictingDirection(direction8_7.getId());

                direction2_4.addDirectionWhichMustYield(direction7_4.getId());
                direction2_4.addDirectionWhichMustYield(direction8_4.getId());

                direction2_4.addNonConflictingDirection(direction2_7.getId());
                direction2_4.addNonConflictingDirection(direction2_8.getId());
                direction2_4.addNonConflictingDirection(direction4_2.getId());
                direction2_4.addNonConflictingDirection(direction4_8.getId());
                direction2_4.addNonConflictingDirection(direction8_2.getId());
            }

            // right of way
            {
                // right turning
                direction2_8.addConflictingDirection(direction4_2.getId());
                direction2_8.addConflictingDirection(direction4_7.getId());
                direction2_8.addConflictingDirection(direction7_2.getId());
                direction2_8.addConflictingDirection(direction8_4.getId());
                direction2_8.addConflictingDirection(direction8_7.getId());

                direction2_8.addDirectionWhichMustYield(direction4_8.getId());

                direction2_8.addDirectionWithRightOfWay(direction7_8.getId());

                direction2_8.addNonConflictingDirection(direction2_4.getId());
                direction2_8.addNonConflictingDirection(direction2_7.getId());
                direction2_8.addNonConflictingDirection(direction7_4.getId());
                direction2_8.addNonConflictingDirection(direction8_2.getId());
            }
        }


        {
            // for top approaching vehicles
            {
                // left turning
                direction7_4.addDirectionWithRightOfWay(direction2_4.getId());

                direction7_4.addDirectionWhichMustYield(direction8_4.getId());

                direction7_4.addNonConflictingDirection(direction2_7.getId());
                direction7_4.addNonConflictingDirection(direction2_8.getId());
                direction7_4.addNonConflictingDirection(direction4_2.getId());
                direction7_4.addNonConflictingDirection(direction4_7.getId());
                direction7_4.addNonConflictingDirection(direction4_8.getId());
                direction7_4.addNonConflictingDirection(direction7_2.getId());
                direction7_4.addNonConflictingDirection(direction7_8.getId());
                direction7_4.addNonConflictingDirection(direction8_2.getId());
                direction7_4.addNonConflictingDirection(direction8_7.getId());
            }

            {
                // straight going
                direction7_8.addConflictingDirection(direction2_4.getId());
                direction7_8.addConflictingDirection(direction4_2.getId());
                direction7_8.addConflictingDirection(direction4_7.getId());
                direction7_8.addConflictingDirection(direction8_4.getId());

                direction7_8.addDirectionWhichMustYield(direction2_8.getId());
                direction7_8.addDirectionWhichMustYield(direction4_8.getId());

                direction7_8.addNonConflictingDirection(direction2_7.getId());
                direction7_8.addNonConflictingDirection(direction7_2.getId());
                direction7_8.addNonConflictingDirection(direction7_4.getId());
                direction7_8.addNonConflictingDirection(direction8_2.getId());
                direction7_8.addNonConflictingDirection(direction8_7.getId());
            }

            // right of way
            {
                // right turning
                direction7_2.addConflictingDirection(direction2_4.getId());
                direction7_2.addConflictingDirection(direction2_8.getId());
                direction7_2.addConflictingDirection(direction4_7.getId());
                direction7_2.addConflictingDirection(direction8_4.getId());
                direction7_2.addConflictingDirection(direction8_7.getId());

                direction7_2.addDirectionWithRightOfWay(direction4_2.getId());

                direction7_2.addDirectionWhichMustYield(direction8_2.getId());

                direction7_2.addNonConflictingDirection(direction2_7.getId());
                direction7_2.addNonConflictingDirection(direction4_8.getId());
                direction7_2.addNonConflictingDirection(direction7_4.getId());
                direction7_2.addNonConflictingDirection(direction7_8.getId());
            }
        }


        {
            // for right approaching vehicles
            {
                // left turning
                direction4_8.addDirectionWhichMustYield(direction2_8.getId());

                direction4_8.addDirectionWithRightOfWay(direction7_8.getId());

                direction4_8.addNonConflictingDirection(direction2_4.getId());
                direction4_8.addNonConflictingDirection(direction2_7.getId());
                direction4_8.addNonConflictingDirection(direction4_2.getId());
                direction4_8.addNonConflictingDirection(direction4_7.getId());
                direction4_8.addNonConflictingDirection(direction7_2.getId());
                direction4_8.addNonConflictingDirection(direction7_4.getId());
                direction4_8.addNonConflictingDirection(direction8_2.getId());
                direction4_8.addNonConflictingDirection(direction8_4.getId());
                direction4_8.addNonConflictingDirection(direction8_7.getId());
            }

            {
                // straight going
                direction4_2.addConflictingDirection(direction2_8.getId());
                direction4_2.addConflictingDirection(direction7_8.getId());
                direction4_2.addConflictingDirection(direction8_4.getId());
                direction4_2.addConflictingDirection(direction8_7.getId());

                direction4_2.addDirectionWhichMustYield(direction7_2.getId());
                direction4_2.addDirectionWhichMustYield(direction8_2.getId());

                direction4_2.addNonConflictingDirection(direction2_7.getId());
                direction4_2.addNonConflictingDirection(direction2_4.getId());
                direction4_2.addNonConflictingDirection(direction4_7.getId());
                direction4_2.addNonConflictingDirection(direction4_8.getId());
                direction4_2.addNonConflictingDirection(direction7_4.getId());
            }

            // right of way
            {
                // right turning
                direction4_7.addConflictingDirection(direction2_4.getId());
                direction4_7.addConflictingDirection(direction2_8.getId());
                direction4_7.addConflictingDirection(direction7_2.getId());
                direction4_7.addConflictingDirection(direction7_8.getId());
                direction4_7.addConflictingDirection(direction8_4.getId());

                direction4_7.addDirectionWhichMustYield(direction2_7.getId());

                direction4_7.addDirectionWithRightOfWay(direction8_7.getId());

                direction4_7.addNonConflictingDirection(direction4_2.getId());
                direction4_7.addNonConflictingDirection(direction4_8.getId());
                direction4_7.addNonConflictingDirection(direction7_4.getId());
                direction4_7.addNonConflictingDirection(direction8_2.getId());
            }
        }

        {
            // for bottom approaching vehicles
            {
                // left turning
                direction8_2.addDirectionWhichMustYield(direction7_2.getId());

                direction8_2.addDirectionWithRightOfWay(direction4_2.getId());

                direction8_2.addNonConflictingDirection(direction2_4.getId());
                direction8_2.addNonConflictingDirection(direction2_7.getId());
                direction8_2.addNonConflictingDirection(direction2_8.getId());
                direction8_2.addNonConflictingDirection(direction4_7.getId());
                direction8_2.addNonConflictingDirection(direction4_8.getId());
                direction8_2.addNonConflictingDirection(direction7_4.getId());
                direction8_2.addNonConflictingDirection(direction7_8.getId());
                direction8_2.addNonConflictingDirection(direction8_4.getId());
                direction8_2.addNonConflictingDirection(direction8_7.getId());
            }

            {
                // straight going
                direction8_7.addConflictingDirection(direction2_4.getId());
                direction8_7.addConflictingDirection(direction2_8.getId());
                direction8_7.addConflictingDirection(direction4_2.getId());
                direction8_7.addConflictingDirection(direction7_2.getId());

                direction8_7.addDirectionWhichMustYield(direction2_7.getId());
                direction8_7.addDirectionWhichMustYield(direction4_7.getId());

                direction8_7.addNonConflictingDirection(direction4_8.getId());
                direction8_7.addNonConflictingDirection(direction8_2.getId());
                direction8_7.addNonConflictingDirection(direction8_4.getId());
                direction8_7.addNonConflictingDirection(direction7_4.getId());
                direction8_7.addNonConflictingDirection(direction7_8.getId());
            }

            // right of way
            {
                // right turning
                direction8_4.addConflictingDirection(direction2_8.getId());
                direction8_4.addConflictingDirection(direction4_2.getId());
                direction8_4.addConflictingDirection(direction4_7.getId());
                direction8_4.addConflictingDirection(direction7_2.getId());
                direction8_4.addConflictingDirection(direction7_8.getId());

                direction8_4.addDirectionWithRightOfWay(direction2_4.getId());

                direction8_4.addDirectionWhichMustYield(direction7_4.getId());

                direction8_4.addNonConflictingDirection(direction2_7.getId());
                direction8_4.addNonConflictingDirection(direction4_8.getId());
                direction8_4.addNonConflictingDirection(direction8_2.getId());
                direction8_4.addNonConflictingDirection(direction8_7.getId());
            }
        }

        intersectionDirections.addDirection(direction2_4);
        intersectionDirections.addDirection(direction2_7);
        intersectionDirections.addDirection(direction2_8);

        intersectionDirections.addDirection(direction7_2);
        intersectionDirections.addDirection(direction7_4);
        intersectionDirections.addDirection(direction7_8);

        intersectionDirections.addDirection(direction4_2);
        intersectionDirections.addDirection(direction4_7);
        intersectionDirections.addDirection(direction4_8);

        intersectionDirections.addDirection(direction8_2);
        intersectionDirections.addDirection(direction8_4);
        intersectionDirections.addDirection(direction8_7);

        conflictData.addConflictingDirectionsForIntersection(intersectionDirections.getSignalSystemId(), intersectionDirections.getNodeId(), intersectionDirections);
        ConflictingDirectionsWriter conflictingDirectionsWriter = new ConflictingDirectionsWriter(conflictData);
        conflictingDirectionsWriter.write(outputDirectory + "signals_conflict_data.xml.gz");
    }
}
