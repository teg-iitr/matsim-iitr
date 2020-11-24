package playground.amit.Delhi.MalviyaNagarPT;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.List;

/*
 * @author Nidhi
 */
public final class MN_Routes {

        // FIX ME: you should rather have link IDs only (which are constants); Do not use any non-final variable from other classes here (Do not use SouthDelhiTransitSchedulerCreator.scenario)
//        public static final Link startLink1 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5145215660013f"), Id.createLinkId(Link.class));
//        public static final Link endLink1 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("6888270010007f"), Id.createLinkId(Link.class));
//
//        public static  final Link startLink2 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5542886270001f"), Id.createLinkId(Link.class));
//        public static final Link endLink2 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5714501110001f"), Id.createLinkId(Link.class));
//
//        public static Link startLink3 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5145215660013f"), Id.createLinkId(Link.class));
//        public static Link endLink3 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5542886270001f"), Id.createLinkId(Link.class));

        public static final Id<Link> startLink1 = Id.create("5145215660013f", Link.class);
        public static final Id<Link> endLink1 = Id.create("6888270010007f", Link.class);

        public static final Id<Link> startLink2 = Id.create("5714501110001f", Link.class);
        public static final Id<Link> endLink2 = Id.create("5542886270001f", Link.class);

        public static final Id<Link> startLink3 = Id.create("5145215660013f", Link.class);
        public static final Id<Link> endLink3 = Id.create("5542886270001f", Link.class);



        public static final List<Id<Link>> linkList1 = List.of(Id.createLinkId("5823673530001f"),Id.createLinkId("5823673530003f"),Id.createLinkId("5823673530005f")
                ,Id.createLinkId("5823673530006f"),Id.createLinkId("5823673530007f"),Id.createLinkId("6036480960000f"),Id.createLinkId("6837664700000f")
                ,Id.createLinkId("6837664700001f"),Id.createLinkId("773639460000f"),Id.createLinkId("773639460007f"),Id.createLinkId("773639460008f")
                ,Id.createLinkId("6897059630000f"),Id.createLinkId("5705260650001f"),Id.createLinkId("773639930002f"),Id.createLinkId("5145215680000f")
                ,Id.createLinkId("5145215680001f"),Id.createLinkId("5145215680002f"),Id.createLinkId("5795078100002f"),Id.createLinkId("5705260610000f")
                ,Id.createLinkId("5618596540000f"),Id.createLinkId("1912048830006f"),Id.createLinkId("1912048830007f"),Id.createLinkId("5707583810000f")
                ,Id.createLinkId("5707583800006f"),Id.createLinkId("5707583790006f"), Id.createLinkId("5707583770003f"), Id.createLinkId("5707583770004f"),Id.createLinkId("5707583780002f")
                ,Id.createLinkId("5510430360001f"),Id.createLinkId("5705260660004f"),Id.createLinkId("5707583710001f"),Id.createLinkId("5707583730007f")
                ,Id.createLinkId("317565150000f"),Id.createLinkId("317565150001f"),Id.createLinkId("317565150002f"),Id.createLinkId("317565150004f")
                ,Id.createLinkId("317565150005f"),Id.createLinkId("317565150006f"),Id.createLinkId("317565150007f"),Id.createLinkId("317565150008f")
                ,Id.createLinkId("317565150009f"),Id.createLinkId("317565150015f"),Id.createLinkId("317565150021f"),Id.createLinkId("317565150025f")
                ,Id.createLinkId("317565150027f"),Id.createLinkId("317565150028f"),Id.createLinkId("317565150034f")
                ,Id.createLinkId("317565150037f"),Id.createLinkId("317565150039f"),Id.createLinkId("317565150042f"),Id.createLinkId("5705260620001f")
                ,Id.createLinkId("5705260620004f"),Id.createLinkId("5705260710001f"),Id.createLinkId("317565100002f"),Id.createLinkId("317565100003f")
                ,Id.createLinkId("317565100006f"),Id.createLinkId("317565100007f"),Id.createLinkId("317565100009f"),Id.createLinkId("317565100010f")
                ,Id.createLinkId("317565100011f"),Id.createLinkId("317565100018f"),Id.createLinkId("317565100020f"),Id.createLinkId("317565100022f")
                ,Id.createLinkId("317565100023f"),Id.createLinkId("5637147670004f"),Id.createLinkId("5637147670005f"),Id.createLinkId("5637147670008f")
                ,Id.createLinkId("5637147670010f"),Id.createLinkId("5637147670011f"),Id.createLinkId("5637147670013f"),Id.createLinkId("1912048850000f")
                ,Id.createLinkId("1912048840002f"),Id.createLinkId("1912048840005f"),Id.createLinkId("1912048840006f"),Id.createLinkId("1912048840007f")
                ,Id.createLinkId("1912048840009f"),Id.createLinkId("1912048840011f"),Id.createLinkId("1912048840014f"),Id.createLinkId("5666803780001f")
                ,Id.createLinkId("5831437080006f"),Id.createLinkId("5831437080008f"),Id.createLinkId("433168420000f"),Id.createLinkId("433168420001f")
                ,Id.createLinkId("433168420002f"),Id.createLinkId("433168420005f"),Id.createLinkId("433168420006f"),Id.createLinkId("433168420007f")
                ,Id.createLinkId("433168420008f"),Id.createLinkId("433168420018f"),Id.createLinkId("433168420025f"),Id.createLinkId("433168420026f")
                ,Id.createLinkId("433168420027f"),Id.createLinkId("7799643160000f"),Id.createLinkId("7591141750006f"),Id.createLinkId("6568056490000f")
                ,Id.createLinkId("6568056490001f"),Id.createLinkId("5719040720001f"),Id.createLinkId("5719040720002f"),Id.createLinkId("5719040720004f")
                ,Id.createLinkId("5719040720006f"),Id.createLinkId("6888270010000f"),Id.createLinkId("6888270010001f"));




        //transit line 2
        public static final List<Id<Link>> linkList2 = List.of(Id.createLinkId("5714500940005f"), Id.createLinkId("5606511100005f"), Id.createLinkId("5714500900000f"), Id.createLinkId("5714500900001f"), Id.createLinkId("7502263930000f"),
                Id.createLinkId("6156254730000f"), Id.createLinkId("6156254730003f"), Id.createLinkId("6156254730005f"), Id.createLinkId("6156254710000f"), Id.createLinkId("6156254710001f"), Id.createLinkId("5666803800005f"), Id.createLinkId("6156254620000f"), Id.createLinkId("6156254620001f"),
                Id.createLinkId("5637147690000f"), Id.createLinkId("6156254680000f"), Id.createLinkId("5705260720001f"), Id.createLinkId("5705260720004f"), Id.createLinkId("5705260720009f"), Id.createLinkId("5719040700000f"), Id.createLinkId("5705260620001f"), Id.createLinkId("5705260620004f"),
                Id.createLinkId("5705260710001f"), Id.createLinkId("317565100002f"), Id.createLinkId("317565100003f"), Id.createLinkId("317565100006f"), Id.createLinkId("317565100007f"), Id.createLinkId("317565100009f"), Id.createLinkId("317565100010f"), Id.createLinkId("317565100011f"), Id.createLinkId("317565100018f"),
                Id.createLinkId("317565100020f"), Id.createLinkId("317565100022f"), Id.createLinkId("317565100023f"), Id.createLinkId("5637147670004f"), Id.createLinkId("5637147670005f"), Id.createLinkId("5637147670008f"), Id.createLinkId("5637147670010f"), Id.createLinkId("5637147670011f"), Id.createLinkId("5637147670013f"),
                Id.createLinkId("1912048850000f"), Id.createLinkId("1912048840002f"), Id.createLinkId("1912048840005f"), Id.createLinkId("1912048840006f"), Id.createLinkId("1912048840007f"), Id.createLinkId("1912048840009f"), Id.createLinkId("1912048840011f"), Id.createLinkId("1912048840014f"), Id.createLinkId("5606511090000f"),
                Id.createLinkId( "5606511090002f"), Id.createLinkId("5606511090003f"), Id.createLinkId("5606511090004f"), Id.createLinkId("5606511090005f"), Id.createLinkId("5606511090007f"), Id.createLinkId("5606511090010f"), Id.createLinkId("5606511090012f"), Id.createLinkId("5577071650000f"), Id.createLinkId("5577071660001f"),
                Id.createLinkId("5577071660003f"), Id.createLinkId("5551971890001f"), Id.createLinkId("5551971890002f"), Id.createLinkId("5551971890003f"), Id.createLinkId("5551971890004f"), Id.createLinkId("5551971890005f"), Id.createLinkId("773639480003f"));




        //transit line 3

        public static final List<Id<Link>> linkList3 = List.of (Id.createLinkId("5823673530001f"), Id.createLinkId("5823673530003f"), Id.createLinkId("5823673530005f"), Id.createLinkId("5823673530006f"), Id.createLinkId("5823673530007f"), Id.createLinkId("6036480960000f"),
                Id.createLinkId("6837664700000f"), Id.createLinkId("6837664700001f"), Id.createLinkId("773639460000f"), Id.createLinkId("773639460007f"), Id.createLinkId("773639460008f"), Id.createLinkId("6897059630000f"), Id.createLinkId("5705260650001f"), Id.createLinkId("773639930002f"), Id.createLinkId("5145215680000f"),
                Id.createLinkId("5145215680001f"), Id.createLinkId("5145215680002f"), Id.createLinkId("5795078100002f"), Id.createLinkId("5705260610000f"), Id.createLinkId("5705260590000f"), Id.createLinkId("5618596580000f"), Id.createLinkId( "5618596580003f"), Id.createLinkId("5618596560000f"), Id.createLinkId("5418873140000f"),
                Id.createLinkId( "5418873140001f"), Id.createLinkId("5418873140002f"), Id.createLinkId("5418873140004f"), Id.createLinkId("773640050001f"), Id.createLinkId("773640050003f"), Id.createLinkId("773640050005f"), Id.createLinkId("773640050007f"), Id.createLinkId("5577071660001f"), Id.createLinkId("5577071660003f"),
                Id.createLinkId("5551971890001f"), Id.createLinkId("5551971890002f"), Id.createLinkId("5551971890003f"), Id.createLinkId("5551971890004f"), Id.createLinkId("5551971890005f"), Id.createLinkId("773639480003f"));







}
