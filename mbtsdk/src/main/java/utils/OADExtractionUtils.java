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

    private static final int OAD_FW_VERSION_OFFSET = 0x27C;
    static final int INDEX_FIRMWARE_VERSION = 4;
    private static final int FILE_CONTENT_SIZE = 256000;

    /**
     * Extract the content of an OAD binary file that holds the firmware
     * @param filePath is the OAD binary file that holds the firmware
     * @return the content of the file as a byte array
     */
    public static final byte[] extractFileContent(AssetManager assetManager, @NonNull final String filePath) throws FileNotFoundException {
        if(filePath == null || filePath.isEmpty() || !fileExists(filePath))
            throw new FileNotFoundException("File path/name incorrect : "+filePath);

        byte[] fileContent = new byte[FILE_CONTENT_SIZE];
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
     * Extract the firmware version from an OAD binary file that holds the firmware
     * @param content is the extracted content of the OAD binary file that holds the firmware
     * @return the firmware version as a String
     */
    public static final String extractFirmwareVersion(@NonNull final byte[] content){
        if (content == null)
            return null;

        byte[] tempFirmwareVersion = new byte[4];
        System.arraycopy(content, OAD_FW_VERSION_OFFSET, tempFirmwareVersion, 0, INDEX_FIRMWARE_VERSION);
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
