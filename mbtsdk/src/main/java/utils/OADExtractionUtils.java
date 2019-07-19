package utils;

import android.content.res.AssetManager;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Vincent on 19/08/2015.
 */
@Keep
public final class OADExtractionUtils {

    private final static String TAG = OADExtractionUtils.class.getName();;

    /**
     * Prefix included in the name of every OAD binary file name.
     */
    private final static String BINARY_FILE_HOOK = "mm-ota-";

    /**
     * Suffix included in the name of every OAD binary file name.
     */
    private final static String BINARY_FILE_FORMAT = ".bin";

    /**
     * Regular expression used in every OAD binary file name to separate each digit of the firmware version number.
     */
    private final static String FIRMWARE_VERSION_REGEX = "_";

    /**
     * Offset to take into account when you read the firmware version from the content of the OAD binary file.
     */
    private static final int FIRMWARE_VERSION_OFFSET = 0x27C;
    /**
     * Number of bytes allocated in the OAD binary file to store the firmware version.
     */
    private static final int FIRMWARE_VERSION_NB_BYTES = 4;

    /**
     * Number of bytes of the OAD file content.
     */
    private static final int BINARY_FILE_CONTENT_SIZE = 256000;

    /**
     * All the OAD binary files are located in a specific subdirectory of the assets directory
     */
    private static final String BINARY_FILES_DIRECTORY = "oad";

    /**
     * Extract the content of an OAD binary file that holds the firmware
     * @return the content of the file as a byte array
     */
    public static final String[] getAvailableFirmwareVersions(AssetManager assetManager) {
        ArrayList<String> availableFirmwareVersions = new ArrayList<>();
        try {
            for (String oadBinaryFileName : assetManager.list(BINARY_FILES_DIRECTORY)) {
                String firmwareVersion = extractFirmwareVersionFromFileName(oadBinaryFileName);
                availableFirmwareVersions.add(firmwareVersion);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (String[])availableFirmwareVersions.toArray();
    }

    /**
     * Extract the content of an OAD binary file that holds the firmware
     * @param assetManager is the assets where are stored the OAD binary files
     * @param filePath is the OAD binary file that holds the firmware
     * @return the content of the file as a byte array
     */
    public static final byte[] extractFileContent(AssetManager assetManager, @NonNull final String filePath) throws FileNotFoundException {
        if(filePath == null || filePath.isEmpty() || !fileExists(filePath))
            throw new FileNotFoundException("File path/name incorrect : "+filePath);

        if(!isValidFileFormat(filePath))
            throw new IllegalArgumentException("File name is invalid : it must starts with "+BINARY_FILE_HOOK+ " and ends with "+BINARY_FILE_FORMAT);

        byte[] fileContent = new byte[BINARY_FILE_CONTENT_SIZE];
        try {
            // Read the file raw into a buffer
            InputStream stream = assetManager.open(filePath);
            stream.read(fileContent, 0, fileContent.length);
            stream.close();
        } catch (IOException|NullPointerException e) {
            Log.e(TAG, "File open failed: " + filePath + "\n");
            return null;
        }

        return fileContent;
    }

    /**
     * Returns true if the OAD binary file given in input starts with {@link OADExtractionUtils#BINARY_FILE_HOOK}
     * and ends with {@link OADExtractionUtils#BINARY_FILE_FORMAT}
     * @param filename the file name to check
     * @return
     */
    public static boolean isValidFileFormat(String filename){
        return filename.startsWith(BINARY_FILE_HOOK) && filename.endsWith(BINARY_FILE_FORMAT);
    }

    public static String getFileNameForFirmwareVersion(String firmwareVersion){
        return BINARY_FILE_HOOK + firmwareVersion + BINARY_FILE_FORMAT;
    }

    /**
     * Extract the firmware version from the name of an OAD binary file that holds the firmware
     * @param filename is the OAD binary file that holds the firmware
     * @return the firmware version as a String
     */
    public static final String extractFirmwareVersionFromFileName(@NonNull final String filename) {
        if(filename == null && filename.isEmpty() && !isValidFileFormat(filename))
                return null;

        return filename
                .replace(BINARY_FILE_HOOK,"") //remove the "mm-ota-" prefix
                .replace(BINARY_FILE_FORMAT,"") //remove the ".bin" format
                .replace(FIRMWARE_VERSION_REGEX, VersionHelper.VERSION_SPLITTER); //replace the "_" digit splitter with a "." splitter
    }

    /**
     * Extract the firmware version from an OAD binary file that holds the firmware
     * @param assetManager is the assets where are stored the OAD binary files
     * @param filePath is the OAD binary file that holds the firmware
     * @return the firmware version as a String
     */
    public static final String extractFirmwareVersionFromContent(AssetManager assetManager, @NonNull final String filePath) throws FileNotFoundException {
        return extractFirmwareVersionFromContent(extractFileContent(assetManager, filePath));
    }

    /**
     * Extract the firmware version from the content of an OAD binary file that holds the firmware
     * @param content is the extracted content of the OAD binary file that holds the firmware
     * @return the firmware version as a String
     */
    public static final String extractFirmwareVersionFromContent(@NonNull final byte[] content){
        if (content == null)
            return null;

        byte[] tempFirmwareVersion = new byte[FIRMWARE_VERSION_NB_BYTES];
        System.arraycopy(content, FIRMWARE_VERSION_OFFSET, tempFirmwareVersion, 0, FIRMWARE_VERSION_NB_BYTES);
        return new String(tempFirmwareVersion);
    }

    public static final ArrayList<byte[]> extractOADPackets(@NonNull final byte[] content){
        if (content == null)
            return null;

        ArrayList<byte[]> packetsToSend = new ArrayList<>();
        //todo
        return packetsToSend;

    }

    private static boolean fileExists(String filePath){
        //todo
        return true;
    }
}
