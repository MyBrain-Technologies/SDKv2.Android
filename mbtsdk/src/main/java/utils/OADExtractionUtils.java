package utils;

import android.content.res.AssetManager;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.lang.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import core.device.oad.PacketCounter;

/**
 * Created by Vincent on 19/08/2015.
 */
@Keep
public final class OADExtractionUtils {

    private final static String TAG = OADExtractionUtils.class.getName();;

    /**
     * All the OAD binary files are located in a specific subdirectory of the assets directory
     */
    private static final String BINARY_FILES_DIRECTORY = "oad";

    /**
     * Prefix included in the name of every OAD binary file name.
     */
    private final static String BINARY_FILE_PREFIX = "mm-ota-";

    /**
     * Suffix included in the name of every OAD binary file name.
     */
    private final static String BINARY_FILE_FORMAT = ".bin";

    /**
     * Regular expression used in every OAD binary file name to separate each digit of the firmware version number.
     */
    private final static String FIRMWARE_VERSION_REGEX = "_";

    /**
     * Regular expression used in the version helper for file name to separate each digit of the firmware version number.
     */
    private final static String FIRMWARE_VERSION_HELPER_REGEX = VersionHelper.VERSION_SPLITTER.replace("\\","");

    /**
     * Expected number of bytes of the OAD binary file (chunks of the file that hold the firmware to install) to send to the headset device
     */
    public static final int EXPECTED_NB_BYTES_BINARY_FILE = 256000;

    /**
     * Offset to take into account when you read the firmware version from the content of the OAD binary file.
     */
    static final int FIRMWARE_VERSION_OFFSET = 0x27C;

    /**
     * Number of bytes allocated in the OAD binary file to store the firmware version.
     */
    public static final int FIRMWARE_VERSION_NB_BYTES = 2;

    /**
     * Number of bytes allocated for the number of packet to send to the current firmware
     */
    public static final int FILE_LENGTH_NB_BYTES = 2;

    /**
     * Expected number of packets of the OAD binary file (chunks of the file that hold the firmware to install) to send to the headset device
     */
    public static final short EXPECTED_NB_PACKETS_BINARY_FILE = 14223;

    /**
     * Size of the index of a packet of the OAD binary file (chunks of the file that hold the firmware to install)
     */
    public static final int OAD_INDEX_PACKET_SIZE = 2;

    /**
     * Size of the content of a packet of the OAD binary file (chunks of the file that hold the firmware to install)
     */
    public static final int OAD_PAYLOAD_PACKET_SIZE = 18;

    /**
     * Size of a packet of the OAD binary file (chunks of the file that hold the firmware to install)
     */
    public static final int OAD_PACKET_SIZE = OAD_INDEX_PACKET_SIZE + OAD_PAYLOAD_PACKET_SIZE;

    /**
     * Extract the content of an OAD binary file that holds the firmware
     * @return the content of the file as a byte array
     */
    public static final String[] getAvailableFirmwareVersions(@NonNull AssetManager assetManager) {
        if(assetManager == null)
            return null;

        ArrayList<String> availableFirmwareVersions = new ArrayList<>();
        try {
            for (String oadBinaryFileName : assetManager.list(BINARY_FILES_DIRECTORY)) {
                String firmwareVersion = extractFirmwareVersionFromFileName(oadBinaryFileName);
                availableFirmwareVersions.add(firmwareVersion);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] availableFirmwareVersionsAsArray = new String[availableFirmwareVersions.size()];
        for(int i = 0; i < availableFirmwareVersions.size() ; i ++){
            availableFirmwareVersionsAsArray[i] = availableFirmwareVersions.get(i);
        }
        return availableFirmwareVersionsAsArray;
    }

    /**
     * Extract the content of an OAD binary file that holds the firmware
     * @param inputStream is the input stream that read the OAD binary file to extract
     * @return the content of the file as a byte array
     */
    public static final byte[] extractFileContent(@NonNull final InputStream inputStream) throws IOException {
        if(inputStream == null)
            throw new FileNotFoundException("File path/name incorrect : "+inputStream);

        byte[] fileContent = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
            // Read the file raw into a buffer
            inputStream.read(fileContent, 0, fileContent.length);
            inputStream.close();

        return fileContent;
    }

    /**
     * Returns true if the OAD binary file given in input starts with {@link OADExtractionUtils#BINARY_FILE_PREFIX}
     * and ends with {@link OADExtractionUtils#BINARY_FILE_FORMAT}
     * @param filename the file name to check
     * @return
     */
    public static boolean isValidFileFormat(String filename){
        return filename.startsWith(BINARY_FILE_PREFIX) && filename.endsWith(BINARY_FILE_FORMAT);
    }

    /**
     * Returns the fle name of the OAD binary file that match the firmware version given in input
     * @param firmwareVersion the firmware version
     * @return the fle name of the OAD binary file that match the firmware version given in input
     */
    public static String getFilePathForFirmwareVersion(String firmwareVersion){
        return BINARY_FILES_DIRECTORY + "/" + BINARY_FILE_PREFIX + firmwareVersion.replace(FIRMWARE_VERSION_HELPER_REGEX, FIRMWARE_VERSION_REGEX) + BINARY_FILE_FORMAT;
    }

    /**
     * Extracts the firmware version from the name of an OAD binary file that holds the firmware
     * @param filename is the OAD binary file that holds the firmware
     * @return the firmware version as a String
     */
    public static final String extractFirmwareVersionFromFileName(@NonNull final String filename) {
        if(filename == null || filename.isEmpty() || !isValidFileFormat(filename.substring(filename.indexOf("/")+1)))
                return null;

        return filename.substring(filename.indexOf("/")+1)
                .replace(BINARY_FILE_PREFIX,"") //remove the "mm-ota-" prefix
                .replace(BINARY_FILE_FORMAT,"") //remove the ".bin" format
                .replace(FIRMWARE_VERSION_REGEX, FIRMWARE_VERSION_HELPER_REGEX); //replace the "_" digit splitter with a "." splitter
    }

    /**
     * Extracts the firmware version from the content of an OAD binary file that holds the firmware
     * @param content is the extracted content of the OAD binary file that holds the firmware
     * @return the firmware version as a String
     */
    public static final byte[] extractFirmwareVersionFromContent(@NonNull final byte[] content){
        if (content == null || content.length < FIRMWARE_VERSION_OFFSET + FIRMWARE_VERSION_NB_BYTES)
            return null;

        ByteBuffer firmwareVersionExtracted = ByteBuffer.allocate(FIRMWARE_VERSION_NB_BYTES);
        for(int byteIndex = 0; byteIndex < FIRMWARE_VERSION_NB_BYTES; byteIndex++ ){
            firmwareVersionExtracted.put(content[FIRMWARE_VERSION_OFFSET + byteIndex]);
        }
        return firmwareVersionExtracted.array();
    }

    /**
     * Chunks the OAD binary file content into small packets and stores them into a list
     * It also adds the correct block index at the beginning of each block
     */
    public static final ArrayList<byte[]> extractOADPackets(@NonNull final byte[] content){
        if (content == null)
            return null;

        PacketCounter packetCounter = new PacketCounter(getTotalNbPackets(OAD_PAYLOAD_PACKET_SIZE, content.length));
        ArrayList<byte[]> packetsToSend = new ArrayList<>();
        int totalNbBytes = 0;
        short packetIndex;

        do{
            packetIndex = packetCounter.getIndexOfNextPacket();
            byte[] packet = new byte[OAD_PACKET_SIZE]; //a packet is 2+18 bytes long
            Arrays.fill(packet, (byte)0xFF);
            //first 2 bytes are the index
            packet[0] = ConversionUtils.loUint16(packetIndex);
            packet[1] = ConversionUtils.hiUint16(packetIndex);
            //18 following bytes are the payload
            if(totalNbBytes + OAD_PAYLOAD_PACKET_SIZE > content.length){
                int remainder = content.length - totalNbBytes;
                System.arraycopy(content, totalNbBytes,
                        packet, OAD_INDEX_PACKET_SIZE, remainder);
//                    for(int i = remainder + OAD_INDEX_PACKET_SIZE; i < OAD_PACKET_SIZE; i++){
//                        packet[i] = (byte)0xFF;
//                    }
            }else {
                System.arraycopy(content, totalNbBytes,
                        packet, OAD_INDEX_PACKET_SIZE, OAD_PAYLOAD_PACKET_SIZE);
            }
            totalNbBytes += OAD_PAYLOAD_PACKET_SIZE;
            packetsToSend.add(packet);
        }while(packetIndex < packetCounter.getTotalNbPackets()-1);

        Log.i(TAG, "List of OAD packets ready");
        return packetsToSend;
    }


    public static boolean fileExistsInAssets(@NonNull AssetManager assetManager, String filepath){
        try {
            for (String file :  assetManager.list(BINARY_FILES_DIRECTORY)){
                if(file.equals(filepath.substring(filepath.indexOf("/")+1)));
                    return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Set the current position of the packet counter.
     * @param totalNbBytes the number of bytes to send
     * @param packetSize Size in bytes of each packet
     */
    public static short getTotalNbPackets(int packetSize, int totalNbBytes){
        return (short) (
                (totalNbBytes / packetSize)
                        + ((totalNbBytes % packetSize) == 0 ?
                        0 : 1));
    }
}
