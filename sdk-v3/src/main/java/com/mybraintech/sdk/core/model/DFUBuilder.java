package com.mybraintech.sdk.core.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import timber.log.Timber;

public class DFUBuilder {

    static public final int DFU_EXT_MEMORY_PAGE_SIZE = 0x100; // = 256 in decimal
    static private final int FILE_LENGTH_OFFSET = 8;
    static private final int OAD_PAYLOAD_PACKET_SIZE = 18;

    /**
     * Offset to take into account when you read the firmware version from the content of the OAD binary file.
     */
    static final int FIRMWARE_VERSION_OFFSET = 0x27C;

    /**
     * Number of bytes allocated in the OAD binary file to store the firmware version.
     */
    public static final int FIRMWARE_VERSION_NB_BYTES = 2;

    private byte[] formulatedFirmwareData;

    /**
     * @param binaryBytes must be multiples of 4
     */
    public void integrateFileLength(byte[] binaryBytes) throws IOException {
        if (binaryBytes.length % 4 != 0) {
            throw new IllegalArgumentException("binaryBytes size must be multiples of 4");
        }

        if (binaryBytes.length % DFU_EXT_MEMORY_PAGE_SIZE == 0) {
            formulatedFirmwareData = binaryBytes;
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(binaryBytes);
            int u32SpaceToFill = DFU_EXT_MEMORY_PAGE_SIZE - (binaryBytes.length % DFU_EXT_MEMORY_PAGE_SIZE);
            byte[] emptyCode = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00};
            for (int i = 0; i < u32SpaceToFill; i += 4) {
                /* Append empty code 0xFF,0xFF,0xFF,0x00 for each space to fill */
                outputStream.write(emptyCode);
            }
            formulatedFirmwareData = outputStream.toByteArray();
        }

        int length = formulatedFirmwareData.length;
        formulatedFirmwareData[FILE_LENGTH_OFFSET + 5] = rightShiftAndMask(length, 24);
        formulatedFirmwareData[FILE_LENGTH_OFFSET + 4] = rightShiftAndMask(length, 16);
        formulatedFirmwareData[FILE_LENGTH_OFFSET + 1] = rightShiftAndMask(length, 8);
        formulatedFirmwareData[FILE_LENGTH_OFFSET] = rightShiftAndMask(length, 0);
    }

    /**
     * @return right shift n bits and take one (right) byte
     */
    public byte rightShiftAndMask(int value, int shiftLength) {
        int shifted = value >> shiftLength;
        return (byte) (shifted % 256);
    }

    public byte[] getFormulatedFirmwareData() {
        return formulatedFirmwareData;
    }

    public int getDataBlockSize() {
        return (int) Math.ceil((float) formulatedFirmwareData.length / OAD_PAYLOAD_PACKET_SIZE);
    }

    /**
     * Extracts the firmware version from the content of an OAD binary file that holds the firmware
     *
     * @return the firmware version as a String
     */
    public byte[] getFirmwareVersion() {
//        boolean isTesting = true;
//        if (isTesting) {
//            return new byte[]{(byte) 0x01, 0x00};
//        }

        if (formulatedFirmwareData == null || formulatedFirmwareData.length < FIRMWARE_VERSION_OFFSET + FIRMWARE_VERSION_NB_BYTES)
            return new byte[0];

        ByteBuffer firmwareVersionExtracted = ByteBuffer.allocate(FIRMWARE_VERSION_NB_BYTES);
        for (int byteIndex = 0; byteIndex < FIRMWARE_VERSION_NB_BYTES; byteIndex++) {
            firmwareVersionExtracted.put(formulatedFirmwareData[FIRMWARE_VERSION_OFFSET + byteIndex]);
        }
        return firmwareVersionExtracted.array();
    }

    public byte[] getBlock(int index) {
        try {
            int startIndex = index * OAD_PAYLOAD_PACKET_SIZE;
            int maxLen = formulatedFirmwareData.length;
            if (startIndex >= maxLen) {
                return new byte[0];
            } else {
                int endIndex = Math.min(startIndex + OAD_PAYLOAD_PACKET_SIZE, maxLen);
                return Arrays.copyOfRange(formulatedFirmwareData, startIndex, endIndex);
            }
        } catch (Exception e) {
            Timber.e(e);
            return new byte[0];
        }
    }
}
