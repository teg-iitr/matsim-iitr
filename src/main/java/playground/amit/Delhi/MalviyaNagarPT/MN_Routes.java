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
//        public static final Link startLink1 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5145215660013f", Link.class));
//        public static final Link endLink1 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("6888270010007f", Link.class));
//
//        public static  final Link startLink2 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5542886270001f", Link.class));
//        public static final Link endLink2 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5714501110001f", Link.class));
//
//        public static Link startLink3 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5145215660013f", Link.class));
//        public static Link endLink3 = SouthDelhiTransitSchedulerCreator.scenario.getNetwork().getLinks().get(Id.create("5542886270001f", Link.class));

        public static final Id<Link> startLink1 = Id.create("5145215660013f", Link.class);
        public static final Id<Link> endLink1 = Id.create("6888270010007f", Link.class);

        public static final Id<Link> startLink2 = Id.create("5542886270001f", Link.class);
        public static final Id<Link> endLink2 = Id.create("5714501110001f", Link.class);

        public static final Id<Link> startLink3 = Id.create("5145215660013f", Link.class);
        public static final Id<Link> endLink3 = Id.create("5542886270001f", Link.class);


        //        public static final List<Id<Link>> linkList1 = List.of(Id.create("5823673530001f", Link.class),Id.create("5823673530003f", Link.class));
        public static ArrayList<Id<Link>> linkList1 = new ArrayList<>();
        public static ArrayList<Id<Link>> linkList2 = new ArrayList<>();
        public static ArrayList<Id<Link>> linkList3 = new ArrayList<>();

        //transit line 1
        public static final ArrayList<Id<Link>> getLinkList1() {
                linkList1.add(Id.create("5823673530001f", Link.class));
                linkList1.add(Id.create("5823673530003f", Link.class));
                linkList1.add(Id.create("5823673530005f", Link.class));
                linkList1.add(Id.create("5823673530006f", Link.class));
                linkList1.add(Id.create("5823673530007f", Link.class));
                linkList1.add(Id.create("6036480960000f", Link.class));
                linkList1.add(Id.create("6837664700000f", Link.class));
                linkList1.add(Id.create("6837664700001f", Link.class));
                linkList1.add(Id.create("773639460000f", Link.class));
                linkList1.add(Id.create("773639460007f", Link.class));
                linkList1.add(Id.create("773639460008f", Link.class));
                linkList1.add(Id.create("6897059630000f", Link.class));
                linkList1.add(Id.create("5705260650001f", Link.class));
                linkList1.add(Id.create("773639930002f", Link.class));
                linkList1.add(Id.create("5145215680000f", Link.class));
                linkList1.add(Id.create("5145215680001f", Link.class));
                linkList1.add(Id.create("5145215680002f", Link.class));
                linkList1.add(Id.create("5795078100002f", Link.class));
                linkList1.add(Id.create("5705260610000f", Link.class));
                linkList1.add(Id.create("5618596540000f", Link.class));
                linkList1.add(Id.create("1912048830006f", Link.class));
                linkList1.add(Id.create("1912048830007f", Link.class));
                linkList1.add(Id.create("5707583810000f", Link.class));
                linkList1.add(Id.create("5707583800006f", Link.class));
                linkList1.add(Id.create("5707583790006f", Link.class));
                linkList1.add(Id.create("5707583770003f", Link.class));
                linkList1.add(Id.create("5707583770004f", Link.class));
                linkList1.add(Id.create("5707583780002f", Link.class));
                linkList1.add(Id.create("5510430360001f", Link.class));
                linkList1.add(Id.create("5705260660004f", Link.class));
                linkList1.add(Id.create("5707583710001f", Link.class));
                linkList1.add(Id.create("5707583730007f", Link.class));
                linkList1.add(Id.create("317565150000f", Link.class));
                linkList1.add(Id.create("317565150001f", Link.class));
                linkList1.add(Id.create("317565150002f", Link.class));
                linkList1.add(Id.create("317565150004f", Link.class));
                linkList1.add(Id.create("317565150005f", Link.class));
                linkList1.add(Id.create("317565150006f", Link.class));
                linkList1.add(Id.create("317565150007f", Link.class));
                linkList1.add(Id.create("317565150008f", Link.class));
                linkList1.add(Id.create("317565150009f", Link.class));
                linkList1.add(Id.create("317565150015f", Link.class));
                linkList1.add(Id.create("317565150021f", Link.class));
                linkList1.add(Id.create("317565150025f", Link.class));
                linkList1.add(Id.create("317565150027f", Link.class));
                linkList1.add(Id.create("317565150028f", Link.class));
                linkList1.add(Id.create("317565150034f", Link.class));
                linkList1.add(Id.create("317565150037f", Link.class));
                linkList1.add(Id.create("317565150039f", Link.class));
                linkList1.add(Id.create("317565150042f", Link.class));
                linkList1.add(Id.create("5705260620001f", Link.class));
                linkList1.add(Id.create("5705260620004f", Link.class));
                linkList1.add(Id.create("5705260710001f", Link.class));
                linkList1.add(Id.create("317565100002f", Link.class));
                linkList1.add(Id.create("317565100006f", Link.class));
                linkList1.add(Id.create("317565100007f", Link.class));
                linkList1.add(Id.create("317565100009f", Link.class));
                linkList1.add(Id.create("317565100010f", Link.class));
                linkList1.add(Id.create("317565100011f", Link.class));
                linkList1.add(Id.create("317565100018f", Link.class));
                linkList1.add(Id.create("317565100020f", Link.class));
                linkList1.add(Id.create("317565100022f", Link.class));
                linkList1.add(Id.create("317565100023f", Link.class));
                linkList1.add(Id.create("5637147670004f", Link.class));
                linkList1.add(Id.create("5637147670005f", Link.class));
                linkList1.add(Id.create("5637147670008f", Link.class));
                linkList1.add(Id.create("5637147670010f", Link.class));
                linkList1.add(Id.create("5637147670011f", Link.class));
                linkList1.add(Id.create("5637147670013f", Link.class));
                linkList1.add(Id.create("1912048850000f", Link.class));
                linkList1.add(Id.create("1912048840002f", Link.class));
                linkList1.add(Id.create("1912048840005f", Link.class));
                linkList1.add(Id.create("1912048840006f", Link.class));
                linkList1.add(Id.create("1912048840007f", Link.class));
                linkList1.add(Id.create("1912048840009f", Link.class));
                linkList1.add(Id.create("1912048840011f", Link.class));
                linkList1.add(Id.create("1912048840014f", Link.class));
                linkList1.add(Id.create("5666803780001f", Link.class));
                linkList1.add(Id.create("5831437080006f", Link.class));
                linkList1.add(Id.create("5831437080008f", Link.class));
                linkList1.add(Id.create("433168420000f", Link.class));
                linkList1.add(Id.create("433168420001f", Link.class));
                linkList1.add(Id.create("433168420002f", Link.class));
                linkList1.add(Id.create("433168420005f", Link.class));
                linkList1.add(Id.create("433168420006f", Link.class));
                linkList1.add(Id.create("433168420007f", Link.class));
                linkList1.add(Id.create("433168420008f", Link.class));
                linkList1.add(Id.create("433168420018f", Link.class));
                linkList1.add(Id.create("433168420025f", Link.class));
                linkList1.add(Id.create("433168420026f", Link.class));
                linkList1.add(Id.create("433168420027f", Link.class));
                linkList1.add(Id.create("7799643160000f", Link.class));
                linkList1.add(Id.create("7591141750006f", Link.class));
                linkList1.add(Id.create("6568056490000f", Link.class));
                linkList1.add(Id.create("6568056490001f", Link.class));
                linkList1.add(Id.create("5719040720001f", Link.class));
                linkList1.add(Id.create("5719040720002f", Link.class));
                linkList1.add(Id.create("5719040720004f", Link.class));
                linkList1.add(Id.create("5719040720006f", Link.class));
                linkList1.add(Id.create("6888270010000f", Link.class));
                linkList1.add(Id.create("6888270010001f", Link.class));
                return linkList1;
        }


        //transit line 2

        public static final ArrayList<Id<Link>> getLinkList2() {
                linkList2.add(Id.create("773639480003f", Link.class));
                linkList2.add(Id.create("5551971890005f", Link.class));
                linkList2.add(Id.create("5551971890004f", Link.class));
                linkList2.add(Id.create("5551971890003f", Link.class));
                linkList2.add(Id.create("5551971890001f", Link.class));
                linkList2.add(Id.create("5577071660003f", Link.class));
                linkList2.add(Id.create("5577071660001f", Link.class));
                linkList2.add(Id.create("5577071650000f", Link.class));
                linkList2.add(Id.create("5606511090012f", Link.class));
                linkList2.add(Id.create("5606511090010f", Link.class));
                linkList2.add(Id.create("5606511090007f", Link.class));
                linkList2.add(Id.create("5606511090005f", Link.class));
                linkList2.add(Id.create("5606511090004f", Link.class));
                linkList2.add(Id.create("5606511090003f", Link.class));
                linkList2.add(Id.create("5606511090002f", Link.class));
                linkList2.add(Id.create("1912048890003f", Link.class));
                linkList2.add(Id.create("1912048840011f", Link.class));
                linkList2.add(Id.create("1912048840009f", Link.class));
                linkList2.add(Id.create("1912048840007f", Link.class));
                linkList2.add(Id.create("1912048840006f", Link.class));
                linkList2.add(Id.create("1912048840005f", Link.class));
                linkList2.add(Id.create("1912048840002f", Link.class));
                linkList2.add(Id.create("1912048850000f", Link.class));
                linkList2.add(Id.create("5637147670013f", Link.class));
                linkList2.add(Id.create("5637147670011f", Link.class));
                linkList2.add(Id.create("5637147670010f", Link.class));
                linkList2.add(Id.create("5637147670008f", Link.class));
                linkList2.add(Id.create("5637147670005f", Link.class));
                linkList2.add(Id.create("5637147670004f", Link.class));
                linkList2.add(Id.create("317565100023f", Link.class));
                linkList2.add(Id.create("317565100022f", Link.class));
                linkList2.add(Id.create("317565100020f", Link.class));
                linkList2.add(Id.create("317565100018f", Link.class));
                linkList2.add(Id.create("317565100011f", Link.class));
                linkList2.add(Id.create("317565100010f", Link.class));
                linkList2.add(Id.create("317565100009f", Link.class));
                linkList2.add(Id.create("317565100007f", Link.class));
                linkList2.add(Id.create("317565100006f", Link.class));
                linkList2.add(Id.create("317565100002f", Link.class));
                linkList2.add(Id.create("5705260710001f", Link.class));
                linkList2.add(Id.create("5705260620004f", Link.class));
                linkList2.add(Id.create("5705260620001f", Link.class));
                linkList2.add(Id.create("5719040700000f", Link.class));
                linkList2.add(Id.create("5705260720009f", Link.class));
                linkList2.add(Id.create("5705260720004f", Link.class));
                linkList2.add(Id.create("5705260720001f", Link.class));
                linkList2.add(Id.create("6156254680000f", Link.class));
                linkList2.add(Id.create("5637147690000f", Link.class));
                linkList2.add(Id.create("6156254620001f", Link.class));
                linkList2.add(Id.create("6156254620000f", Link.class));
                linkList2.add(Id.create("5666803800005f", Link.class));
                linkList2.add(Id.create("6156254710001f", Link.class));
                linkList2.add(Id.create("6156254710000f", Link.class));
                linkList2.add(Id.create("6156254730005f", Link.class));
                linkList2.add(Id.create("6156254730003f", Link.class));
                linkList2.add(Id.create("6156254730000f", Link.class));
                linkList2.add(Id.create("7502263930000f", Link.class));
                linkList2.add(Id.create("5714500900001f", Link.class));
                linkList2.add(Id.create("5714500900000f", Link.class));
                linkList2.add(Id.create("5606511100005f", Link.class));
                linkList2.add(Id.create("5714500940005f", Link.class));
                return linkList2;
        }


        //transit line 3

        public static final ArrayList<Id<Link>> getLinkList3() {
                linkList3.add(Id.create("5823673530001f", Link.class));
                linkList3.add(Id.create("5823673530003f", Link.class));
                linkList3.add(Id.create("5823673530005f", Link.class));
                linkList3.add(Id.create("5823673530006f", Link.class));
                linkList3.add(Id.create("5823673530007f", Link.class));
                linkList3.add(Id.create("6036480960000f", Link.class));
                linkList3.add(Id.create("6837664700000f", Link.class));
                linkList3.add(Id.create("6837664700001f", Link.class));
                linkList3.add(Id.create("773639460000f", Link.class));
                linkList3.add(Id.create("773639460007f", Link.class));
                linkList3.add(Id.create("773639460008f", Link.class));
                linkList3.add(Id.create("6897059630000f", Link.class));
                linkList3.add(Id.create("5705260650001f", Link.class));
                linkList3.add(Id.create("773639930002f", Link.class));
                linkList3.add(Id.create("5145215680000f", Link.class));
                linkList3.add(Id.create("5145215680001f", Link.class));
                linkList3.add(Id.create("5145215680002f", Link.class));
                linkList3.add(Id.create("5795078100002f", Link.class));
                linkList3.add(Id.create("5705260610000f", Link.class));
                linkList3.add(Id.create("5705260590000f", Link.class));
                linkList3.add(Id.create("5618596580000f", Link.class));
                linkList3.add(Id.create("5618596580003f", Link.class));
                linkList3.add(Id.create("5618596560000f", Link.class));
                linkList3.add(Id.create("5418873140000f", Link.class));
                linkList3.add(Id.create("5418873140001f", Link.class));
                linkList3.add(Id.create("5418873140001f", Link.class));
                linkList3.add(Id.create("5418873140002f", Link.class));
                linkList3.add(Id.create("5418873140004f", Link.class));
                linkList3.add(Id.create("773640050001f", Link.class));
                linkList3.add(Id.create("773640050003f", Link.class));
                linkList3.add(Id.create("773640050005f", Link.class));
                linkList3.add(Id.create("773640050007f", Link.class));
                linkList3.add(Id.create("5577071660001f", Link.class));
                linkList3.add(Id.create("5577071660003f", Link.class));
                linkList3.add(Id.create("5551971890001f", Link.class));
                linkList3.add(Id.create("5551971890003f", Link.class));
                linkList3.add(Id.create("5551971890004f", Link.class));
                linkList3.add(Id.create("5551971890005f", Link.class));
                linkList3.add(Id.create("773639480003f", Link.class));
                return linkList3;
        }



//        public static final List<Id<Link>> stopLinkId = List.of(Id.createLinkId("5823673530001f"), Id.createLinkId("5145215680000f"), Id.createLinkId("317565150006f"),
//                Id.createLinkId("1504018850006f"), Id.createLinkId("5705260710001f"), Id.createLinkId("317565100018f"), Id.createLinkId("5637147670004f"),
//                Id.createLinkId("5606511090002f"), Id.createLinkId("5831437080006f"), Id.createLinkId("6568056490001f"), Id.createLinkId("6888270010007f"), //11
//                Id.createLinkId("6140720190001f"), Id.createLinkId("6598332090009f"), Id.createLinkId("6044866480001f"), Id.createLinkId("5637147760010f"), //15
//                Id.createLinkId("5418873140000f"), Id.createLinkId("295304080021f"), Id.createLinkId("6866052960004f"), //18-need to edit the link
//                Id.createLinkId("5542886270001f"), Id.createLinkId("281215370005f"), Id.createLinkId("5714501110001f"), Id.createLinkId("6036480960000f"), //22-traffic signal
//                Id.createLinkId("1504018850002f"), Id.createLinkId("6156254680000f"), Id.createLinkId("317565150042f"), Id.createLinkId("773640050003f"),
//                Id.createLinkId("3120319040006f"), Id.createLinkId("1912048840014f"));



}
