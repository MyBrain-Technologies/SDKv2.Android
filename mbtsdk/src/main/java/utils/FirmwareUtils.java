package utils;

import android.util.Log;

    /**
     * Created by Etienne on 2/26/2018.
     * This class wraps the fw version into three parts: main major and minor.
     * A valid fwVersion format is X.Y.Z where
     * X = main
     * Y = major
     * Z = minor
     * Each Xs Ys and Zs are numbers
     */
    public final class FirmwareUtils {
        public final static String TAG = FirmwareUtils.class.getSimpleName();

        private static final int HEADSET_STATUS_MIN_VERSION_MAIN = 1;
        private static final int HEADSET_STATUS_MIN_VERSION_MAJOR = 5;
        private static final int HEADSET_STATUS_MIN_VERSION_MINOR = 13;

        private static final int OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAIN = 1;
        private static final int OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAJOR = 5;
        private static final int OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MINOR = 10;

        private static final int BLE_BONDING_VERSION_MAIN = 1;
        private static final int BLE_BONDING_VERSION_MAJOR = 6;
        private static final int BLE_BONDING_VERSION_MINOR = 7;

        private static final int A2DP_FROM_HEADSET_VERSION_MAIN = 1;
        private static final int A2DP_FROM_HEADSET_VERSION_MAJOR = 6;
        private static final int A2DP_FROM_HEADSET_VERSION_MINOR = 7;

        private static final int WRITE_EXTERNAL_NAME_VERSION_MAIN = 1;
        private static final int WRITE_EXTERNAL_NAME_VERSION_MAJOR = 7;
        private static final int WRITE_EXTERNAL_NAME_VERSION_MINOR = 1;

        private final String minor;
        private final String major;
        private final String main;

        public FirmwareUtils(String fwVersion){
            String[] deviceFwVersion = fwVersion.split("\\.");
            if(deviceFwVersion.length < 3){
                main = null;
                major = null;
                minor = null;
                return;
            }


            main = deviceFwVersion[0];
            major = deviceFwVersion[1];
            minor = deviceFwVersion[2];
        }

        public String getMainVersionAsString(){
            return main;
        }

        public String getMajorVersionAsString(){
            return major;
        }

        public String getMinorVersionAsString(){
            return minor;
        }

        private int getMainVersionAsNumber(){
            return Integer.valueOf(main);
        }

        private int getMajorVersionAsNumber(){
            return Integer.valueOf(major);
        }

        private int getMinorVersionAsNumber(){
            return Integer.valueOf(minor);
        }

        public boolean isFwValidForFeature(FWFeature feature){
            boolean b = false;

            if(major == null || minor == null || main == null)
                return false;

            switch (feature){
                case HEADSET_STATUS:
                    b = verifyFeature(HEADSET_STATUS_MIN_VERSION_MAIN, HEADSET_STATUS_MIN_VERSION_MAJOR, HEADSET_STATUS_MIN_VERSION_MINOR);
                    break;

                case OAD_WITHOUT_CONNECTION_PRIORITY:
                    b = verifyFeature(OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAIN, OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MAJOR, OAD_WITHOUT_REQUEST_PRIORITY_HIGH_MIN_VERSION_MINOR);
                    break;

                case BLE_BONDING:
                    b = verifyFeature(BLE_BONDING_VERSION_MAIN, BLE_BONDING_VERSION_MAJOR, BLE_BONDING_VERSION_MINOR);
                    break;

                case A2DP_FROM_HEADSET:
                    b = verifyFeature(A2DP_FROM_HEADSET_VERSION_MAIN, A2DP_FROM_HEADSET_VERSION_MAJOR, A2DP_FROM_HEADSET_VERSION_MINOR);
                    break;

                case REGISTER_EXTERNAL_NAME:
                    b = verifyFeature(WRITE_EXTERNAL_NAME_VERSION_MAIN, WRITE_EXTERNAL_NAME_VERSION_MAJOR, WRITE_EXTERNAL_NAME_VERSION_MINOR);
                    break;

                default:
                    break;
            }
            LogUtils.d(TAG, "feature test for " + feature.toString() + " is " + (b ? "valid" : "invalid"));
            return b;
        }


        /**
         * Verifies if a specific feature can be enabled on the bt communication based on the firmware version.
         * @param featureMain the minimum value for the main, ie X in X.Y.Z nomenclature
         * @param featureMajor the minimum value for the major, ie Y in X.Y.Z nomenclature
         * @param featureMinor the minimum value for the minor, ie Z in X.Y.Z nomenclature
         */
        private boolean verifyFeature(int featureMain, int featureMajor, int featureMinor){
            if(checkMain(featureMain) < 0)
                return false;
            else if(checkMain(featureMain) > 0)
                return true;
            else if(checkMajor(featureMajor) < 0)
                return false;
            else if(checkMajor(featureMajor) > 0)
                return true;
            else return checkMinor(featureMinor);
        }

        /**
         * Compare the X parameter in the X.Y.Z version number to the minimum requested value for a specific feature.
         * @param featureMainVersion the minimum value for the specific feature
         * @return -1 if main is inferior to minimum value, 1 if superior or 0 if equal.
         */
        private int checkMain(int featureMainVersion){
            if(getMainVersionAsNumber() < featureMainVersion)
                return -1;
            else return getMainVersionAsNumber() > featureMainVersion ? 1 : 0;
        }

        /**
         * Compare the Y parameter in the X.Y.Z version number to the minimum requested value for a specific feature.
         * @param featureMajorVersion the minimum value for the specific feature
         * @return -1 if main is inferior to minimum value, 1 if superior or 0 if equal.
         */
        private int checkMajor(int featureMajorVersion){
            if(getMajorVersionAsNumber() < featureMajorVersion)
                return -1;
            else return getMajorVersionAsNumber() > featureMajorVersion ? 1 : 0;
        }

        /**
         * Compare the Z parameter in the X.Y.Z version number to the minimum requested value for a specific feature.
         * @param featureMinorVersion the minimum value for the specific feature
         * @return -1 if main is inferior to minimum value, 1 if superior or 0 if equal.
         */
        private boolean checkMinor(int featureMinorVersion){
            return getMinorVersionAsNumber() >= featureMinorVersion;
        }


        public enum FWFeature {
            HEADSET_STATUS,
            OAD_WITHOUT_CONNECTION_PRIORITY,
            BLE_BONDING,
            A2DP_FROM_HEADSET,
            REGISTER_EXTERNAL_NAME
        }

//        /**
//         * return true if the first firmware (fwVersion1) is older than or equal to the second firmware (fwVersion2), true otherwise
//         * @return
//         */
//        public static boolean compareFirmwareVersion(String fwVersion1, String fwVersion2){
//            Log.i(TAG, "firmware 1 ("+fwVersion1+") <= firmware 2 ("+fwVersion2+") ?");
//            String[] deviceFwVersion1 = fwVersion1.split("\\.");
//            String[] deviceFwVersion2 = fwVersion2.split("\\.");
//            if(deviceFwVersion1.length < 3 || deviceFwVersion2.length < 3 ){
//                return false;
//            }
//
//            return (Integer.parseInt(deviceFwVersion1[0])*100+Integer.parseInt(deviceFwVersion1[1])*10+Integer.parseInt(deviceFwVersion1[2])) <= (Integer.parseInt(deviceFwVersion2[0])*100+Integer.parseInt(deviceFwVersion2[1])*10+Integer.parseInt(deviceFwVersion2[2]));
//        }
    }

