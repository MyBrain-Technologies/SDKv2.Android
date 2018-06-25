package utils;

/**
 * A helper class containing statics methods to serialize and deserialize Objects.
 * <p>For now, this helper contains methods to serialize and deserialize <code>MBTSession</code>,
 * <code>MBTUser</code> and <code>MbtEEGPacket</code></p>
 * @author Manon Leterme
 * @version 1.0
 */

public final class MbtJsonUtils {
    private final static String TAG = "MbtJsonUtils";

//    @Nullable
//    public static final String serializeEEGData(@NonNull final MbtDevice deviceInfo,
//                                                @NonNull final MbtRecording recordings,
//                                                @NonNull final int totalRecordingNb,
//                                                @NonNull final ArrayList<Comment> comments) {
//
//        if (recordings.getNbPackets() == 0)
//            throw new IllegalArgumentException("No recording");
//
//        try {
//            final StringWriter stringWriter = new StringWriter();
//            final JsonWriter jsonWriter = new JsonWriter(stringWriter);
//
//            jsonWriter.beginObject();
//            // BEGINNING OF MAIN JSON OBJECT
//            jsonWriter.name("uuidJsonFile");
//            jsonWriter.value(UUID.randomUUID().toString());
//
//            jsonWriter.name("header");
//            jsonWriter.beginObject();   // beginning of "header"   object
//
//            jsonWriter.name("deviceInfo");
//            jsonWriter.beginObject();   // beginning of "deviceInfo"        object
//
//            jsonWriter.name("productName");
//            jsonWriter.value(deviceInfo.getProductName());
//
//            jsonWriter.name("hardwareVersion");
//            if (deviceInfo.getHardwareVersion() == null)
//                jsonWriter.value("");
//            else
//                jsonWriter.value(deviceInfo.getHardwareVersion());
//
//            jsonWriter.name("firmwareVersion");
//            if (deviceInfo.getFirmwareVersion() == null)
//                jsonWriter.value("");
//            else
//                jsonWriter.value(deviceInfo.getFirmwareVersion());
//
//            jsonWriter.name("uniqueDeviceIdentifier");
//            if (deviceInfo.getSerialNumber() == null)
//                jsonWriter.value("");
//            else
//                jsonWriter.value(deviceInfo.getSerialNumber());
//
//            jsonWriter.endObject();     // end of       "deviceInfo"        object
//
//            jsonWriter.name("recordingNb");
//            jsonWriter.value("0x"+ String.format("%02X",totalRecordingNb));
//
//            jsonWriter.name("comments");
//            jsonWriter.beginArray();
//            if(!comments.isEmpty()){
//                for (Comment comment : comments) {
//                    jsonWriter.beginObject();
//                    jsonWriter.name("date");
//                    jsonWriter.value(comment.getTimestamp());
//                    jsonWriter.name("comment");
//                    jsonWriter.value(comment.getComment());
//                    jsonWriter.endObject();
//                }
//            }
//            jsonWriter.endArray();
//
//            jsonWriter.name("eegPacketLength");
//            jsonWriter.value(deviceInfo.getEegPacketLength());
//            jsonWriter.name("sampRate");
//            jsonWriter.value(deviceInfo.getSampRate());
//            jsonWriter.name("nbChannels");
//            jsonWriter.value(deviceInfo.getNbChannels());
//            jsonWriter.name("acquisitionLocation");
//            jsonWriter.beginArray();
//            for (final MbtAcquisitionLocations local : deviceInfo.getAcquisitionLocations()) {
//                jsonWriter.value(local.toString());
//            }
//            jsonWriter.endArray();
//            jsonWriter.name("referencesLocation");
//            jsonWriter.beginArray();
//            for (final MbtAcquisitionLocations local : deviceInfo.getReferencesLocations()) {
//                jsonWriter.value(local.toString());
//            }
//            jsonWriter.endArray();
//            jsonWriter.name("groundsLocation");
//            jsonWriter.beginArray();
//            for (final MbtAcquisitionLocations local : deviceInfo.getGroundsLocation()) {
//                jsonWriter.value(local.toString());
//            }
//            jsonWriter.endArray();
//
//            jsonWriter.endObject();     // end of       "header"   object
//
//            jsonWriter.name("recording");
//            jsonWriter.beginObject();   // beginning of "recording"    object
//
//            jsonWriter.name("recordID");
//            jsonWriter.value(recordings.getRecordInfos().getRecordId());
//
//            jsonWriter.name("recordingType");
//            jsonWriter.beginObject();
//
//            jsonWriter.name("recordType");
//            jsonWriter.value(recordings.getRecordInfos().getRecordingType().getRecordType().toString());
//            jsonWriter.name("spVersion");
//            jsonWriter.value(recordings.getRecordInfos().getRecordingType().getSPVersion());
//            jsonWriter.name("source");
//            jsonWriter.value(recordings.getRecordInfos().getRecordingType().getSource().toString());
//            jsonWriter.name("dataType");
//            jsonWriter.value(recordings.getRecordInfos().getRecordingType().getExerciseType().toString());
//            jsonWriter.endObject();
//
//            jsonWriter.name("recordingTime");
//            jsonWriter.value(recordings.getRecordingTime());
//
//            jsonWriter.name("nbPackets");
//            jsonWriter.value(recordings.getNbPackets());
//            jsonWriter.name("firstPacketId");
//            jsonWriter.value(recordings.getFirstPacketsId());
//
//            jsonWriter.name("qualities");
//            jsonWriter.beginArray();    // beginning of "qualities"         array
//            for (int it = 0 ; it < recordings.getQualities().size() ; it ++) {
//
//                ArrayList<Float> channelsQualities = recordings.getQualities().get(it);
//
//                jsonWriter.beginArray(); // we generate an array for each row of the qualities matrix
//                for (Float quality : channelsQualities){
//                    jsonWriter.value((quality != null && quality != Float.NaN ) ? quality : null);
//                }
//                jsonWriter.endArray();  // and we close it here
//            }
//            jsonWriter.endArray();      // end of       "qualities"         array
//
//            jsonWriter.name("channelData");
//            jsonWriter.beginArray();    // beginning of "channelData"       array
//
//            for (ArrayList<Float> floats : recordings.getEegData()) {
//                ArrayList<Float> channelSamples = floats;
//                jsonWriter.beginArray(); // we generate an array for each row of the EEG data matrix
//                for (Float sample : channelSamples)
//                    jsonWriter.value(!Float.isNaN(sample) ? sample : null);
//                jsonWriter.endArray();  // and we close it here
//            }
//
//            jsonWriter.endArray();      // end of       "channelData"       array
//
//            jsonWriter.name("statusData"); // beginning of "statusData"       array
//            jsonWriter.beginArray();
//            if(recordings.getStatus() != null){
//                for (Float status : recordings.getStatus()) {
//                    jsonWriter.value(!Float.isNaN(status) ? status : null);
//                }
//            }
//            jsonWriter.endArray(); // end of       "statusData"       array
//
//            jsonWriter.endObject();     // end of       "recording"    object
//
//            // END OF OF MAIN JSON OBJECT
//            jsonWriter.endObject();   // end of MAIN JSON   object
//
//            return stringWriter.toString();
//        } catch (@NonNull final IOException ioe) {
//            Log.e(TAG, "Error while serializing EEG data to JSON ->\n" + ioe.getMessage());
//            return null;
//        }
//    }
//
//
//    public static MbtRecording convertEEGPacketListToRecordings(@NonNull RecordInfo recordInfo, @NonNull long timestamp, @NonNull List<MbtEEGPacket> MbtEEGPacketArrayList, @NonNull boolean hasStatus){
//        return new MbtRecording(recordInfo, timestamp, MbtEEGPacketArrayList, hasStatus);
//    }
//

}
