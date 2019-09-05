package core.recording;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.device.model.MbtDevice;
import core.eeg.storage.MbtEEGPacket;
import model.MbtRecording;
import model.RecordInfo;
import utils.AsyncUtils;
import utils.LogUtils;
import utils.MbtJsonUtils;

import static utils.MbtJsonUtils.RECORDING_NUMBER_KEY;

public final class FileManager {
    private static final String TAG = FileManager.class.getName();

    private final static String FILENAME_SPLITTER = "-";
    private final static String FOLDER_SEPARATOR = "/";
    private final static String DATE_FORMAT_PATTERN = "yyyy-MM-dd_HH:mm:ss.SSS";
    private final static String FILE_FORMAT = ".json";
    private final static String RECORDING_FOLDER = "recording";

    /**
     * Create a new file name according to the name standardization decided by MBT.
     *
     * @param timestamp   the record start timestamp
     * @param projectName the project in which the record is started
     * @param deviceName  the device name
     * @param subjectID   the EEG owner ID
     * @param condition   the record condition
     * @return the filename as a string
     */
    public static String createFilename(@NonNull long timestamp,
                                        @NonNull String projectName,
                                        @NonNull String deviceName,
                                        @Nullable String subjectID,
                                        @Nullable String condition) {
        final Date date = new Date(timestamp);
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);

        return sdf.format(date) +
                FILENAME_SPLITTER +
                projectName +
                FILENAME_SPLITTER +
                deviceName +
                FILENAME_SPLITTER +
                subjectID +
                FILENAME_SPLITTER +
                condition;
    }

    /**
     * Retrieve (or create if not existing) the folder to store the recording
     * @param context
     * @param folder
     * @param useExternalStorage
     * @return the absolute path of the folder created
     */
    public static String createFolder(@NonNull final Context context,
                                      @Nullable String folder,
                                      boolean useExternalStorage)
    {
        File parent = new File(
                (useExternalStorage && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED ?
                        Environment.getExternalStorageDirectory()
                        : context.getFilesDir())
                        + FOLDER_SEPARATOR
                        + (folder != null ? folder : RECORDING_FOLDER));

        boolean parentExists = (parent.exists() || parent.mkdir());

        return (parentExists ? parent.getAbsolutePath() : null);
    }

    /**
     * Create a file to store the recording and delete
     * @param context
     * @param folder
     * @param useExternalStorage
     * @return
     */
    static File createFile(@NonNull final Context context,
                           @Nullable String folder,
                           @NonNull String filename,
                           boolean useExternalStorage)
    {
        String absolutePath = createFolder(context, folder, useExternalStorage);
        if (absolutePath == null){
            Log.e(TAG, "Impossible to create the folder: "+folder);
            return null;
        }

        File file = new File(absolutePath, filename + FILE_FORMAT);

        try {
            if(file.exists())
                file.delete();

            return (file.createNewFile() ? file : null);
        } catch (IOException e) {
            Log.e(TAG,"Failed to create file");
            return null;
        }
    }

    /**
     * Create and write recorded data into a new JSON file
     * @param context the context of the application
     * @param device The current device connected
     * @param totalRecordingInSession the current number of recording in sessions
     * @param comments A list of commentary with their timestamp
     * @return the absolute path of the file to save it in the Map
     */
    static String storeDataInFile(@NonNull final Context context,
                                  @NonNull File file,
                                  @NonNull String subjectId,
                                  @NonNull MbtDevice device,
                                  @Nullable List<MbtEEGPacket> eegPackets,
                                  @Nullable Bundle recordingParams,
                                  @NonNull final RecordInfo recordInfo,
                                  @NonNull final int totalRecordingInSession,
                                  @NonNull ArrayList<Comment> comments,
                                  @NonNull long timestamp){
        if(file == null) {
            LogUtils.e(TAG, "Error: Null file");
            return null;
        }

        try (final FileWriter fw = new FileWriter(file)) {
            MbtRecording recording = MbtJsonUtils.convertEEGPacketListToRecordings(recordInfo, timestamp, eegPackets, device.getInternalConfig().getStatusBytes() > 0);
            String json = MbtJsonUtils.serializeRecording(device, recording, totalRecordingInSession, comments, recordingParams, subjectId);
            fw.append(json);
            fw.close();

            //This block of code only serves to see the file in USB storage mode
            MediaScannerConnection.scanFile(
                    context.getApplicationContext(),
                    new String[]{file.getAbsolutePath()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.v("mediascan",
                                    "file " + path + " was scanned successfully: " + uri);
                        }
                    });

        } catch (IOException e) {
            Log.e("writeJsonFile", "Exception catch " + e.getMessage());
            return null;
        }

        return file.getAbsolutePath();
    }

    /**
     * Updates all current records in the map with
     * @param savedRecordings is the map that contains all the JSON previously created on the app
     */
    static void updateJSONWithCurrentRecordNb(Map<String, String> savedRecordings) {
        final Map<String, Long> savedMap = new HashMap<>();
        Map tmp = new HashMap(savedRecordings);
        tmp.keySet().removeAll(savedMap.keySet());
        savedMap.putAll(tmp);
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                for (String filepath : savedMap.keySet()) {
                    readJsonAsStreamAndUpdateRecordNb(filepath, savedMap.size());
                }
            }
        });
    }

    /**
     * Opens an already created JSON file as a text file in order to replace the current recording number with the new one
     * @param filename the current file to update
     * @param recordingNb the new recording number to write
     */
    public static synchronized void readJsonAsStreamAndUpdateRecordNb(String filename, int recordingNb) {
        char[] input = new char[2000];
        //final String recordStringHook = "\"recordingNb\":\"0x";
        final String recordStringHook = RECORDING_NUMBER_KEY+"\":\""+MbtJsonUtils.RECORDING_NUMBER_PREFIX;
        boolean replaced = false;
        File file = new File(filename);
        try {

            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while (!replaced) {
                bufferedReader.read(input);
                String inputAsString = new String(input);
                if (inputAsString.contains(recordStringHook)) {
                    replaced = true;
                    int pos = inputAsString.indexOf(recordStringHook);
                    Log.i(TAG, "recordingNb found at pos " + (pos + recordStringHook.length()));
                    String oldValue = inputAsString.substring(pos + recordStringHook.length(), pos + recordStringHook.length() + 2);
                    String newValue = String.format("%02X", recordingNb);
                    Log.d(TAG, "old value is " + oldValue + " new value is " + newValue);
                    String updatedInputAsString = inputAsString.replace(recordStringHook+oldValue,recordStringHook + newValue);
                    Log.d(TAG, "s2 is " + updatedInputAsString);
                    bufferedReader.close();
                    fileInputStream.close();
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                    randomAccessFile.seek(0);
                    randomAccessFile.write(updatedInputAsString.getBytes());
                    randomAccessFile.close();

                }else{
                    Log.e(TAG, "recordNb not found");
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error updating file");
        }
    }

}
