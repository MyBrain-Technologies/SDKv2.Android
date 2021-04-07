package mbtsdk.com.mybraintech.sdkv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import features.MbtAcquisitionLocations;

/**
 * Created by Etienne on 04/07/2017.
 */

public final class MBTConfig {

    public static Map<Integer, MbtAcquisitionLocations> locations;
    public static MbtAcquisitionLocations[] references;
    public static MbtAcquisitionLocations[] grounds;

    public static void loadConfig(String configName){
        MbtAcquisitionLocations[] locationsOrder;

        switch(configName){
            case "F":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.F3,
                        MbtAcquisitionLocations.C3,
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.O1,
                        MbtAcquisitionLocations.O2,
                        MbtAcquisitionLocations.P4,
                        MbtAcquisitionLocations.C4,
                        MbtAcquisitionLocations.F4};

                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){
                    locations.put(order, locationsOrder[order]);
                }

                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fz};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.AFz};

                break;

            case "L":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.CP5,
                        MbtAcquisitionLocations.C3,
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.POz,
                        MbtAcquisitionLocations.P4,
                        MbtAcquisitionLocations.C4,
                        MbtAcquisitionLocations.CP6,
                        MbtAcquisitionLocations.Fz};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fpz};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.AFz};
                break;

            case "C":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.FC1,
                        MbtAcquisitionLocations.FC2,
                        MbtAcquisitionLocations.Cz,
                        MbtAcquisitionLocations.C2,
                        MbtAcquisitionLocations.CP1,
                        MbtAcquisitionLocations.CP2,
                        MbtAcquisitionLocations.ACC,
                        MbtAcquisitionLocations.EXT1};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fpz};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fp2};
                break;

            case "F-V2":
            default:

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.Fp1,
                        MbtAcquisitionLocations.F3,
                        MbtAcquisitionLocations.C3,
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.Fp2,
                        MbtAcquisitionLocations.F4,
                        MbtAcquisitionLocations.C4,
                        MbtAcquisitionLocations.P4};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.A2};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fz};
                break;

                //Faurecia Renault 2019
            case "F-R":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.O2,
                        MbtAcquisitionLocations.P4,
                        MbtAcquisitionLocations.F4,
                        MbtAcquisitionLocations.Fp2,
                        MbtAcquisitionLocations.Fp1,
                        MbtAcquisitionLocations.F3,
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.O1
                };

                ArrayList<MbtAcquisitionLocations> displayedLocations = new ArrayList<>(Arrays.asList(
                        MbtAcquisitionLocations.Fp1,
                        MbtAcquisitionLocations.Fp2,
                        MbtAcquisitionLocations.F3,
                        MbtAcquisitionLocations.F4,
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.P4,
                        MbtAcquisitionLocations.O1,
                        MbtAcquisitionLocations.O2)
                );

                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){

                    locations.put( (displayedLocations.contains(locationsOrder[order])) ?
                                    displayedLocations.indexOf(locationsOrder[order]) : order,
                            locationsOrder[order]);
                }

                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.M1};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.M2};
                break;

            case "MBT":

                locationsOrder = new MbtAcquisitionLocations[]{
                            MbtAcquisitionLocations.O2,
                            MbtAcquisitionLocations.P4,
                            MbtAcquisitionLocations.F4,
                            MbtAcquisitionLocations.C4,
                            MbtAcquisitionLocations.O1,
                            MbtAcquisitionLocations.F3,
                            MbtAcquisitionLocations.C3,
                            MbtAcquisitionLocations.P3};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.FCz};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fp1, MbtAcquisitionLocations.Fp2};
                break;

            case "Custom A.V":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.Fp1,
                        MbtAcquisitionLocations.F3,
                        MbtAcquisitionLocations.C3,
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.Fp2,
                        MbtAcquisitionLocations.F4,
                        MbtAcquisitionLocations.Fz,
                        MbtAcquisitionLocations.P4};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length ; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.A2};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.Fz};
                break;

            case "MM":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.P4,};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.M1};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.M2};
                break;

            case "Q+":

                locationsOrder = new MbtAcquisitionLocations[]{
                        MbtAcquisitionLocations.P3,
                        MbtAcquisitionLocations.P4,
                        MbtAcquisitionLocations.F3,
                        MbtAcquisitionLocations.F4,};
                locations = new LinkedHashMap<>();
                for (int order = 0; order < locationsOrder.length; order++){
                    locations.put(order, locationsOrder[order]);
                }
                references = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.M1};
                grounds = new MbtAcquisitionLocations[]{MbtAcquisitionLocations.M2};
                break;

        }
    }

    public static String getConfigAsString() {
        if(locations == null || references == null || grounds == null)
            return null;

        return "MBTConfig: \n"
                + locations + " \n "
                + Arrays.toString(references)+ " \n "
                + Arrays.toString(grounds)+ " \n ";
    }

    /**
     * Return the locations in electronic defined order
     * @return
     */
    public static MbtAcquisitionLocations[] getLocationsInDisplayOrder() {
        MbtAcquisitionLocations[] locationsAsArray = new MbtAcquisitionLocations[locations.size()];
        for (Integer key : locations.keySet()) {
            locationsAsArray[key] = locations.get(key);
        }
        return locationsAsArray;
    }
    public static MbtAcquisitionLocations[] getLocationsInElectricalOrder() {
        MbtAcquisitionLocations[] locationsAsArray = new MbtAcquisitionLocations[locations.size()];
        int index = 0;
        for (Map.Entry<Integer, MbtAcquisitionLocations> location : locations.entrySet()) {
            locationsAsArray[index++] = location.getValue();
        }
        return locationsAsArray;
    }
}
