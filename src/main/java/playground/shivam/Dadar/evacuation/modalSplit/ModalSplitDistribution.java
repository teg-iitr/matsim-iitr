package playground.shivam.Dadar.evacuation.modalSplit;

import playground.shivam.Dadar.evacuation.DadarUtils;

public class ModalSplitDistribution {
    // traffic composition based on cmp mumbai 2016 pg 2-4 (figure 2-8)
    /*public static String getTravelModeFromTrafficComposition(double number) {
        if (number <= 29.1) return DadarUtils.MumbaiModeShareSplit2014.car.toString();
        else if (number > 29.1 && number <= 73.8) {
            return DadarUtils.MumbaiModeShareSplit2014.motorbike.toString();
        } else if (number > 73.8 && number <= 78.2) {
            return DadarUtils.MumbaiModeShareSplit2014.bus.toString();
        } else if (number > 78.2 && number <= 83.6) {
            return DadarUtils.MumbaiModeShareSplit2014.lcv.toString();
        } else if (number > 83.6 && number <= 89.7) {
            return DadarUtils.MumbaiModeShareSplit2014.truck.toString();
        } else if (number > 89.7 && number <= 99.4) {
            return DadarUtils.MumbaiModeShareSplit2014.auto.toString();
        } else if (number > 99.4 && number <= 99.6) {
            return DadarUtils.MumbaiModeShareSplit2014.bicycle.toString();
        } else return DadarUtils.MumbaiModeShareSplit2014.cart.toString();
    }*/

    // 2014 modal split for greater mumbai based on cmp mumbai 2016
    public static String getTravelModeFromModalSplit(double number) {
        if (number <= 18.2)
            return DadarUtils.MumbaiModeShareSplit2014.car.toString();
        else if (number > 18.2 && number <= 33) {
            return DadarUtils.MumbaiModeShareSplit2014.motorbike.toString();
        } else if (number > 33 && number <= 94.2) {
            return DadarUtils.MumbaiModeShareSplit2014.pt.toString();
        } else
            return DadarUtils.MumbaiModeShareSplit2014.auto.toString();
    }

    public static String getTravelModeFromMainModes(double number) {
        if (number <= 50)
            return DadarUtils.MumbaiModeShareSplit2014.car.toString();
        else
            return DadarUtils.MumbaiModeShareSplit2014.motorbike.toString();
    }
}
