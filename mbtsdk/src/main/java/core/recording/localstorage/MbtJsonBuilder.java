package core.recording.localstorage;

import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import core.Indus5Singleton;
import core.device.model.MbtDevice;
import core.eeg.storage.MbtEEGPacket;
import core.recording.metadata.Comment;
import features.MbtAcquisitionLocations;
import model.LedSignal;
import model.MbtRecording;
import model.Position3D;
import model.RecordInfo;
import timber.log.Timber;

/**
 * A helper class containing statics methods to serialize and deserialize Objects.
 * <p>For now, this helper contains methods to serialize and deserialize <code>MBTSession</code>,
 * <code>MBTUser</code> and <code>MbtEEGPacket</code></p>
 * @author Manon Leterme
 * @version 1.0
 */

final class MbtJsonBuilder{

    private final static String TAG = MbtJsonBuilder.class.getName();

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
        final static String RECORDING_NUMBER_PREFIX = "0x";
        private final static String COMMENTS_KEY = "comments";
        private final static String COMMENT_DATE_KEY = "date";
        private final static String COMMENT_VALUE_KEY = "comment";
        private final static String EEG_PACKET_LENGTH_KEY = "eegPacketLength";
        private final static String SAMP_RATE_KEY = "sampRate";
        private final static String NB_CHANNELS_KEY = "nbChannels";
        private final static String ACQUISITION_LOCATION_KEY = "acquisitionLocation";
        private final static String REFERENCES_LOCATION_KEY = "referencesLocation";
        private final static String GROUND_LOCATION_KEY =  "groundsLocation";

    private final static String RECORDING_KEY = "recording";
        private final static String RECORD_ID_KEY = "recordID";
        private final static String RECORDING_TYPE_KEY = "recordingType";
            private final static String RECORD_TYPE_KEY = "recordType";
            private final static String SP_ALGO_VERSION_KEY = "spVersion";
            private final static String SOURCE_KEY = "source";
            private final static String DATA_TYPE_KEY = "dataType";
        private final static String RECORDING_TIME_KEY = "recordingTime";
        private final static String NB_PACKETS_KEY = "nbPackets";
        private final static String FIRST_PACKET_ID_KEY = "firstPacketId";
        private final static String QUALITIES_KEY = "qualities";
        private final static String CHANNEL_DATA_KEY = "channelData";
        private final static String STATUS_DATA_KEY = "statusData";
        private final static String RECORDING_PARAMS_KEY = "recordingParameters";

    @Nullable
    static final String serializeRecording(@NonNull final MbtDevice device,
                                                  @NonNull final MbtRecording recording,
                                                  @NonNull final int totalRecordingNb,
                                                  @Nullable final ArrayList<Comment> comments,
                                                  @Nullable final Bundle recordingParams,
                                                  @NonNull final String ownerId) {

        if (recording.getNbPackets() == 0)
            throw new IllegalArgumentException("No recording");

        try {
            final StringWriter stringWriter = new StringWriter();
            final JsonWriter jsonWriter = new JsonWriter(stringWriter);

            jsonWriter.beginObject();
            // BEGINNING OF MAIN JSON OBJECT
            jsonWriter.name(UUID_KEY)
                    .value(UUID.randomUUID().toString());

            jsonWriter.name(CONTEXT_KEY);
            jsonWriter.beginObject();

            jsonWriter.name(OWNER_ID_KEY)
                    .value(ownerId);

            if(recordingParams != null && recordingParams.getString(RI_ALGO_KEY) != null){
                jsonWriter.name(RI_ALGO_KEY)
                        .value(recordingParams.getString(RI_ALGO_KEY));
            }

            jsonWriter.endObject();

            jsonWriter.name(HEADER_KEY);
            jsonWriter.beginObject();   // beginning of "header"   object

            jsonWriter.name(DEVICE_INFO_KEY);
            jsonWriter.beginObject();   // beginning of "deviceInfo"        object

            jsonWriter.name(PRODUCT_NAME_KEY)
                    .value(device.getProductName());

            jsonWriter.name(HW_VERSION_KEY)
                    .value(device.getHardwareVersion() == null ?
                        "" : device.getHardwareVersion().toString());

            jsonWriter.name(FW_VERSION_KEY)
                    .value(device.getFirmwareVersion() == null ?
                            "" : device.getFirmwareVersion().toString());

            jsonWriter.name(SERIAL_NUMBER_KEY)
                    .value(device.getSerialNumber() == null ?
                        "" : device.getSerialNumber());

            jsonWriter.endObject();     // end of       "deviceInfo"        object

            jsonWriter.name(RECORDING_NUMBER_KEY)
                    .value(RECORDING_NUMBER_PREFIX+ String.format("%02X",totalRecordingNb));


            if(comments != null && !comments.isEmpty()){
                jsonWriter.name(COMMENTS_KEY);
                jsonWriter.beginArray();
                for (Comment comment : comments) {
                    jsonWriter.beginObject();

                    jsonWriter.name(COMMENT_DATE_KEY)
                            .value(comment.getTimestamp());

                    jsonWriter.name(COMMENT_VALUE_KEY)
                            .value(comment.getComment());
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }

            jsonWriter.name(EEG_PACKET_LENGTH_KEY)
                    .value(device.getEegPacketLength());

            jsonWriter.name(SAMP_RATE_KEY)
                    .value(device.getSampRate());

            jsonWriter.name(NB_CHANNELS_KEY)
                    .value(device.getNbChannels());

            jsonWriter.name(ACQUISITION_LOCATION_KEY);
            jsonWriter.beginArray();

            for (final MbtAcquisitionLocations local : device.getAcquisitionLocations()) {
                jsonWriter.value(local.toString());
            }
            jsonWriter.endArray();

            jsonWriter.name(REFERENCES_LOCATION_KEY);
            jsonWriter.beginArray();

            for (final MbtAcquisitionLocations local : device.getReferencesLocations()) {
                jsonWriter.value(local.toString());
            }
            jsonWriter.endArray();

            jsonWriter.name(GROUND_LOCATION_KEY);
            jsonWriter.beginArray();

            for (final MbtAcquisitionLocations local : device.getGroundsLocation()) {
                jsonWriter.value(local.toString());
            }
            jsonWriter.endArray();

            jsonWriter.endObject();     // end of       "header"   object

            jsonWriter.name(RECORDING_KEY);
            jsonWriter.beginObject();   // beginning of "recording"    object

            jsonWriter.name(RECORD_ID_KEY)
                    .value(recording.getRecordInfos().getRecordId());

            jsonWriter.name(RECORDING_TYPE_KEY);
            jsonWriter.beginObject();

            jsonWriter.name(RECORD_TYPE_KEY)
                    .value(recording.getRecordInfos().getRecordingType().getRecordType());

            jsonWriter.name(SP_ALGO_VERSION_KEY)
                    .value(recording.getRecordInfos().getRecordingType().getSPVersion());

            jsonWriter.name(SOURCE_KEY)
                    .value(recording.getRecordInfos().getRecordingType().getSource().toString());

            jsonWriter.name(DATA_TYPE_KEY)
                    .value(recording.getRecordInfos().getRecordingType().getExerciseType().toString());

            jsonWriter.endObject();

            jsonWriter.name(RECORDING_TIME_KEY)
                    .value(recording.getRecordingTime());

            jsonWriter.name(NB_PACKETS_KEY)
                    .value(recording.getNbPackets());

            jsonWriter.name(FIRST_PACKET_ID_KEY)
                    .value(recording.getFirstPacketsId());

            //----------------------------------------------------------------------------
            // recording error data
            //----------------------------------------------------------------------------
            if (Indus5Singleton.INSTANCE.isIndus5() && recording.getRecordingErrorData() != null) {
                Timber.d("serialize recording error data");

                jsonWriter.name("recordingErrorData");
                jsonWriter.beginObject();
                jsonWriter.name("missingEeg").value(recording.getRecordingErrorData().getMissingFrame());
                jsonWriter.name("zeroTime").value(recording.getRecordingErrorData().getZeroTimeNumber());
                jsonWriter.name("zeroSample").value(recording.getRecordingErrorData().getZeroSampleNumber());
                jsonWriter.endObject();
            }

            jsonWriter.name(QUALITIES_KEY);
            jsonWriter.beginArray();    // beginning of "qualities"         array

            for (int it = 0 ; it < recording.getQualities().size() ; it ++) {

                ArrayList<Float> channelsQualities = recording.getQualities().get(it);

                jsonWriter.beginArray(); // we generate an array for each row of the qualities matrix
                for (Float quality : channelsQualities){
                    jsonWriter.value((quality != null && quality != Float.NaN ) ? quality : null);
                }
                jsonWriter.endArray();  // and we close it here
            }
            jsonWriter.endArray();      // end of       "qualities"         array

            jsonWriter.name(CHANNEL_DATA_KEY);
            jsonWriter.beginArray();    // beginning of "channelData"       array

            for (ArrayList<Float> floats : recording.getEegData()) {
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

            if (Indus5Singleton.INSTANCE.isIndus5() && recording.getAccelerometerPositions() != null) {
                Timber.d("write IMS");

                jsonWriter.name("ims");
                jsonWriter.beginObject();    // beginning of "ims"

                jsonWriter.name("sampRate").value(100);
                jsonWriter.name("imsData");
                jsonWriter.beginArray();
                for (int i =0; i<recording.getAccelerometerPositions().size(); i++) {
                    Position3D position = recording.getAccelerometerPositions().get(i);
                    if (position != null) {
                        jsonWriter.beginArray(); // we generate an array for each row of the position data
                        jsonWriter.value(position.getX());
                        jsonWriter.value(position.getY());
                        jsonWriter.value(position.getZ());
                        jsonWriter.endArray();  // and we close it here
                    } else {
                        Timber.w("found null Position3D in IMS buffer");
                    }
                }
                jsonWriter.endArray();

                jsonWriter.endObject();
            }

            //----------------------------------------------------------------------------
            // start ppg
            //----------------------------------------------------------------------------

            if (Indus5Singleton.INSTANCE.isIndus5() && recording.getPpg() != null && !recording.getPpg().isEmpty()) {
                Timber.d("write PPG");

                jsonWriter.name("ppg");
                jsonWriter.beginObject();    // beginning of "ims"

                jsonWriter.name("sampRate").value(100);
                jsonWriter.name("sampNumber").value(recording.getPpg().get(0).size());
                jsonWriter.name("ppgData");
                jsonWriter.beginArray();
                for (int i = 0; i < recording.getPpg().size(); i++) {
                    jsonWriter.beginArray();
                    ArrayList<LedSignal> leds = recording.getPpg().get(i);
                    if (leds != null && !leds.isEmpty()) {
                        for (int j = 0; j < leds.size(); j++) {
                            if (leds.get(j) != null) {
                                jsonWriter.value(leds.get(j).getValue());
                            } else {
                                Timber.w("found null LedSignal in PPG buffer");
                            }
                        }

                    }
                    jsonWriter.endArray();
                }
                jsonWriter.endArray();

                jsonWriter.endObject();
            }

            jsonWriter.name(STATUS_DATA_KEY); // beginning of "statusData"       array
            jsonWriter.beginArray();

            if(recording.getStatus() != null){
                for (Float status : recording.getStatus()) {
                    jsonWriter.value(!Float.isNaN(status) ? status : null);
                }
            }
            jsonWriter.endArray(); // end of       "statusData"       array

            //Using JSONObject wrapper in order to automatically serialize in JSON the recordingParameters and then reinject it in the stringWriter
            if(recordingParams == null){
                jsonWriter.name(RECORDING_PARAMS_KEY)
                        .nullValue();
            } else{
                JSONObject json = new JSONObject();
                Set<String> mKeys = recordingParams.keySet();
                for (String mKey : mKeys) {
                    try {
                        json.put(mKey, JSONObject.wrap(recordingParams.get(mKey)));
                    } catch(JSONException e) {
                        //Handle exception here
                        Log.e(TAG, "Impossible to serialize bundle element with key " + mKey);
                    }
                }
                stringWriter.append(",");
                stringWriter.append("\"").append(RECORDING_PARAMS_KEY).append("\"").append(":");
                stringWriter.append(json.toString());
            }

            jsonWriter.endObject();     // end of       "recording"    object

            // END OF OF MAIN JSON OBJECT
            jsonWriter.endObject();   // end of MAIN JSON   object

            return stringWriter.toString();
        } catch (@NonNull final IOException ioe) {
            Log.e(TAG, "Error while serializing EEG data to JSON ->\n" + ioe.getMessage());
            return null;
        }
    }


    static MbtRecording convertEEGPacketsToRecording(@NonNull int nbChannels,
                                                            @NonNull RecordInfo recordInfo,
                                                            @NonNull long timestamp,
                                                            @NonNull List<MbtEEGPacket> eegPackets,
                                                            @NonNull boolean hasStatus){
        return new MbtRecording(nbChannels, recordInfo, timestamp, eegPackets, hasStatus);
    }

    static MbtRecording convertDataToRecording(@NonNull int nbChannels,
                                               @NonNull RecordInfo recordInfo,
                                               @NonNull long timestamp,
                                               @NonNull List<MbtEEGPacket> eegPackets,
                                               @NonNull final ArrayList<Position3D> accelerometerPositions,
                                               @NonNull final ArrayList<ArrayList<LedSignal>> ppg,
                                               @NonNull boolean hasStatus){
        return new MbtRecording(nbChannels, recordInfo, timestamp, eegPackets, accelerometerPositions, ppg, hasStatus);
    }
}
