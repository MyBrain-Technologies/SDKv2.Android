package com.mybraintech.sdk.core.model;

import android.util.JsonWriter;

import androidx.annotation.NonNull;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

/**
 * A helper class containing statics methods to serialize and deserialize Objects.
 * <p>For now, this helper contains methods to serialize and deserialize <code>MBTSession</code>,
 * <code>MBTUser</code> and <code>MbtEEGPacket</code></p>
 *
 * @author Manon Leterme
 * @version 1.0
 */

final class MbtJsonBuilder2 {

    private final static String UUID_KEY = "uuidJsonFile";

    private final static String CONTEXT_KEY = "context";
    private final static String OWNER_ID_KEY = "ownerId";

    private final static String RI_ALGO_KEY = "riAlgo";

    private final static String HEADER_KEY = "header";
    private final static String DEVICE_INFO_KEY = "deviceInfo";
    private final static String PRODUCT_NAME_KEY = "productName";
    private final static String HW_VERSION_KEY = "hardwareVersion";
    private final static String FW_VERSION_KEY = "firmwareVersion";
    private final static String SERIAL_NUMBER_KEY = "uniqueDeviceIdentifier";
    final static String RECORDING_NUMBER_KEY = "recordingNb";
    private final static String COMMENTS_KEY = "comments";
    private final static String COMMENT_DATE_KEY = "date";
    private final static String COMMENT_VALUE_KEY = "comment";
    private final static String EEG_PACKET_LENGTH_KEY = "eegPacketLength";
    private final static String SAMP_RATE_KEY = "sampRate";
    private final static String NB_CHANNELS_KEY = "nbChannels";
    private final static String ACQUISITION_LOCATION_KEY = "acquisitionLocation";
    private final static String REFERENCES_LOCATION_KEY = "referencesLocation";
    private final static String GROUND_LOCATION_KEY = "groundsLocation";

    private final static String RECORDING_KEY = "recording";
    private final static String RECORD_ID_KEY = "recordID";
    private final static String RECORDING_TYPE_KEY = "recordingType";
    private final static String RECORD_TYPE_KEY = "recordType";
    private final static String SP_ALGO_VERSION_KEY = "spVersion";
    private final static String SOURCE_KEY = "source";
    private final static String DATA_TYPE_KEY = "dataType";
    private final static String RECORDING_TIME_KEY = "recordingTime";
    private final static String NB_PACKETS_KEY = "nbPackets";
    private final static String QUALITIES_KEY = "qualities";
    private final static String CHANNEL_DATA_KEY = "channelData";
    private final static String STATUS_DATA_KEY = "statusData";
    private final static String RECORDING_PARAMS_KEY = "recordingParameters";

    static boolean serializeRecording(
            @NonNull final String ownerId,
            @NonNull final KwakHeader kwakHeader,
            @NonNull final KwakRecording kwakRecording,
            @NonNull final EEGRecordingData recordingData,
            @NonNull final List<ThreeDimensionalPosition> imsBuffer,
            @NonNull final FileWriter fileWriter) {

        if (recordingData.eegData.size() == 0)
            throw new IllegalArgumentException("No recording");

        try {
            final JsonWriter jsonWriter = new JsonWriter(fileWriter);

            jsonWriter.beginObject();
            // BEGINNING OF MAIN JSON OBJECT
            jsonWriter.name(UUID_KEY)
                    .value(UUID.randomUUID().toString());

            jsonWriter.name(CONTEXT_KEY);
            jsonWriter.beginObject();

            jsonWriter.name(OWNER_ID_KEY)
                    .value(ownerId);

//            if(recordingParams != null && recordingParams.getRiAlgo() != null){
//                jsonWriter.name(RI_ALGO_KEY)
//                        .value(recordingParams.getRiAlgo());
//            }

            jsonWriter.endObject();

            jsonWriter.name(HEADER_KEY);
            jsonWriter.beginObject();   // beginning of "header"   object

            jsonWriter.name(DEVICE_INFO_KEY);
            jsonWriter.beginObject();   // beginning of "deviceInfo"        object

            DeviceInformation device = kwakHeader.getDeviceInfo();
            if (device == null) {
                throw new RuntimeException("device info not found");
            }

            jsonWriter.name(PRODUCT_NAME_KEY)
                    .value(device.getProductName());

            jsonWriter.name(HW_VERSION_KEY)
                    .value(device.getHardwareVersion());

            jsonWriter.name(FW_VERSION_KEY)
                    .value(device.getFirmwareVersion());

            jsonWriter.name(SERIAL_NUMBER_KEY)
                    .value(device.getUniqueDeviceIdentifier());

            jsonWriter.endObject();     // end of       "deviceInfo"        object

            jsonWriter.name(RECORDING_NUMBER_KEY)
                    .value(kwakHeader.getRecordingNb());

            List<Comment> comments = kwakHeader.getComments();
            if (comments != null && !comments.isEmpty()) {
                jsonWriter.name(COMMENTS_KEY);
                jsonWriter.beginArray();
                for (Comment comment : comments) {
                    jsonWriter.beginObject();

                    jsonWriter.name(COMMENT_DATE_KEY)
                            .value(comment.getTimestamp());

                    jsonWriter.name(COMMENT_VALUE_KEY)
                            .value(comment.getText());
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }

            jsonWriter.name(EEG_PACKET_LENGTH_KEY)
                    .value(kwakHeader.getEegPacketLength());

            jsonWriter.name(SAMP_RATE_KEY)
                    .value(kwakHeader.getSampleRate());

            jsonWriter.name(NB_CHANNELS_KEY)
                    .value(kwakHeader.getNbChannels());

            jsonWriter.name(ACQUISITION_LOCATION_KEY);
            jsonWriter.beginArray();

            for (final EnumAcquisitionLocation local : kwakHeader.getAcquisitionLocations()) {
                jsonWriter.value(local.toString());
            }
            jsonWriter.endArray();

            jsonWriter.name(REFERENCES_LOCATION_KEY);
            jsonWriter.beginArray();

            for (final EnumAcquisitionLocation local : kwakHeader.getReferenceLocations()) {
                jsonWriter.value(local.toString());
            }
            jsonWriter.endArray();

            jsonWriter.name(GROUND_LOCATION_KEY);
            jsonWriter.beginArray();

            for (final EnumAcquisitionLocation local : kwakHeader.getGroundLocations()) {
                jsonWriter.value(local.toString());
            }
            jsonWriter.endArray();

            jsonWriter.endObject();     // end of       "header"   object

            jsonWriter.name(RECORDING_KEY);
            jsonWriter.beginObject();   // beginning of "recording"    object

            jsonWriter.name(RECORD_ID_KEY)
                    .value(kwakRecording.getRecordID());

            jsonWriter.name(RECORDING_TYPE_KEY);
            jsonWriter.beginObject();

            KwakRecordingType kwakRecordingType = kwakRecording.getRecordingType();
            jsonWriter.name(RECORD_TYPE_KEY)
                    .value(kwakRecordingType.getRecordType().toString());

            jsonWriter.name(SP_ALGO_VERSION_KEY)
                    .value(kwakRecordingType.getSpVersion());

            jsonWriter.name(SOURCE_KEY)
                    .value(kwakRecordingType.getSource());

            jsonWriter.name(DATA_TYPE_KEY)
                    .value(kwakRecordingType.getDataType());

            jsonWriter.endObject();

            jsonWriter.name(RECORDING_TIME_KEY)
                    .value(kwakRecording.getRecordingTime());

            jsonWriter.name(NB_PACKETS_KEY)
                    .value(recordingData.getNbPackets());

            //----------------------------------------------------------------------------
            // recording error data
            //----------------------------------------------------------------------------
            if (recordingData.getRecordingErrorData() != null) {
                Timber.d("serialize recording error data");
                jsonWriter.name("recordingErrorData");
                jsonWriter.beginObject();
                jsonWriter.name("missingEegFrame").value(recordingData.getRecordingErrorData().getMissingFrame());
                jsonWriter.name("zeroTime").value(recordingData.getRecordingErrorData().getZeroTimeNumber());
                jsonWriter.name("zeroSample").value(recordingData.getRecordingErrorData().getZeroSampleNumber());
                jsonWriter.endObject();
            }

            jsonWriter.name(QUALITIES_KEY);
            jsonWriter.beginArray();    // beginning of "qualities"         array

            for (int it = 0; it < recordingData.getQualities().size(); it++) {

                List<Float> channelsQualities = recordingData.getQualities().get(it);

                jsonWriter.beginArray(); // we generate an array for each row of the qualities matrix
                for (Float quality : channelsQualities) {
                    jsonWriter.value((quality != null && quality != Float.NaN) ? quality : null);
                }
                jsonWriter.endArray();  // and we close it here
            }
            jsonWriter.endArray();      // end of       "qualities"         array

            jsonWriter.name(CHANNEL_DATA_KEY);
            jsonWriter.beginArray();    // beginning of "channelData"       array

            for (ArrayList<Float> floats : recordingData.getEegData()) {
                ArrayList<Float> channelSamples = floats;
                jsonWriter.beginArray(); // we generate an array for each row of the EEG data matrix

                for (Float sample : channelSamples)
                    jsonWriter.value(!Float.isNaN(sample) ? sample : null);

                jsonWriter.endArray();  // and we close it here
            }

            jsonWriter.endArray();      // end of       "channelData"       array

            //----------------------------------------------------------------------------
            // start ims
            //----------------------------------------------------------------------------

            if (imsBuffer != null && !imsBuffer.isEmpty()) {
                Timber.d("write IMS : size = " + imsBuffer.size());

                jsonWriter.name("ims");
                jsonWriter.beginObject();    // beginning of "ims"

                jsonWriter.name("sampRate").value(100);

                jsonWriter.name("imsData");
                jsonWriter.beginArray(); //start to write ims data (ims)

                //write X
                jsonWriter.beginArray(); //start to write X (x)
                for (int i = 0; i < imsBuffer.size(); i++) {
                    ThreeDimensionalPosition position = imsBuffer.get(i);
                    if (position != null) {
                        jsonWriter.value(position.getX());
                    } else {
                        Timber.w("found null Position3D in IMS buffer");
                    }
                }
                jsonWriter.endArray(); //end to write X (x)

                //write Y
                jsonWriter.beginArray(); //start to write Y (y)
                for (int i = 0; i < imsBuffer.size(); i++) {
                    ThreeDimensionalPosition position = imsBuffer.get(i);
                    if (position != null) {
                        jsonWriter.value(position.getY());
                    }
                }
                jsonWriter.endArray(); //end to write Y (y)

                //write Z
                jsonWriter.beginArray(); //start to write Z (z)
                for (int i = 0; i < imsBuffer.size(); i++) {
                    ThreeDimensionalPosition position = imsBuffer.get(i);
                    if (position != null) {
                        jsonWriter.value(position.getZ());
                    }
                }
                jsonWriter.endArray(); //end to write Z (z)

                jsonWriter.endArray(); //end writing ims (ims)

                jsonWriter.endObject();
            } else {
                Timber.d("json writing: no IMS");
            }

            //----------------------------------------------------------------------------
            // start ppg
            //----------------------------------------------------------------------------

//            if (Indus5Singleton.INSTANCE.isIndus5() && kwakRecording.getPpg() != null && !kwakRecording.getPpg().isEmpty()) {
//                Timber.d("write PPG");
//
//                jsonWriter.name("ppg");
//                jsonWriter.beginObject();    // beginning of "ims"
//
//                jsonWriter.name("sampRate").value(100);
//                jsonWriter.name("sampNumber").value(kwakRecording.getPpg().get(0).size());
//                jsonWriter.name("ppgData");
//                jsonWriter.beginArray();
//                for (int i = 0; i < kwakRecording.getPpg().size(); i++) {
//                    jsonWriter.beginArray();
//                    ArrayList<LedSignal> leds = kwakRecording.getPpg().get(i);
//                    if (leds != null && !leds.isEmpty()) {
//                        for (int j = 0; j < leds.size(); j++) {
//                            if (leds.get(j) != null) {
//                                jsonWriter.value(leds.get(j).getValue());
//                            } else {
//                                Timber.w("found null LedSignal in PPG buffer");
//                            }
//                        }
//
//                    }
//                    jsonWriter.endArray();
//                }
//                jsonWriter.endArray();
//
//                jsonWriter.endObject();
//            } else {
//                Timber.d("json writing: no PPG");
//            }

            jsonWriter.name(STATUS_DATA_KEY); // beginning of "statusData"       array
            jsonWriter.beginArray();
            for (Float status : recordingData.getStatusData()) {
                jsonWriter.value(!Float.isNaN(status) ? status : null);
            }
            jsonWriter.endArray(); // end of       "statusData"       array

            jsonWriter.endObject();     // end of       "recording"    object

            // END OF OF MAIN JSON OBJECT
            jsonWriter.endObject();   // end of MAIN JSON   object
            jsonWriter.close();

            return true;
        } catch (Exception e) {
            Timber.e("Error while serializing EEG data to JSON ->\n" + e.getMessage());
            return false;
        }
    }
}
