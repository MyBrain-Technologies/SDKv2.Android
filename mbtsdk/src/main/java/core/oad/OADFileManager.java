package core.oad;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.CRC32;

import utils.ConversionUtils;

/**
 * Class helper offering full parsing and preparation of the binary file to be tranfered via BLE
 */
public final class OADFileManager {
    private final static String TAG = OADFileManager.class.getName();

    private final static String BINARY_HOOK = "mm-ota-";
    private final static String FWVERSION_REGEX = "_";

    private static final int OAD_BLOCK_SIZE = 18;
    private static final int HAL_FLASH_WORD_SIZE = 4;
    private static final int OAD_CRC_OFFSET = 0x278;
    private static final int OAD_FILE_LENGTH_OFFSET = 0x274;
    private static final int OAD_FW_VERSION_OFFSET = 0x27C;
    private static final int TARGET_FLASH_PAGE_SIZE = 0x100;
    private static final int OAD_BUFFER_SIZE = 2 + OAD_BLOCK_SIZE;
    private static final int FILE_BUFFER_SIZE = 256000;
    private final byte[] mFileBuffer = new byte[FILE_BUFFER_SIZE];
    private final ArrayList<byte[]> mOadBuffer = new ArrayList<>();
    @NonNull
    private ProgInfo mProgInfo = new ProgInfo();
    private int mFileLength;
    private int crc32;
    @Nullable
    private String fwVersion;


    /**
     * Constructor in case the oad binary file is stored in the application's assets.
     * @param assetManager
     * @param filepath
     */
    public OADFileManager(@NonNull AssetManager assetManager, String filepath){
        if(loadFile(assetManager, filepath)){
            this.fwVersion = getFWVersionFromFile();
            createBufferFromBinaryFile();
        }
    }

    public OADFileManager(String filepath){

    }


    public static String[] getMostRecentFwVersion(Context context){
        String[] myFiles;
        String[] mostRecentFwVersion = new String[0];
        boolean isInit = false;

        try {
            myFiles = context.getAssets().list("");
            if(myFiles.length > 0) {
                final ArrayList<String> binFilesList = new ArrayList<String>();
                for (String myFile : myFiles) {
                    if (myFile.endsWith(".bin")) {
                        binFilesList.add(myFile);
                    }
                }
                if(binFilesList.isEmpty())
                    return null;

                for (int i = 0; i < binFilesList.size(); i++) {
                    String s = binFilesList.get(i);
                    if(!binFilesList.get(i).contains(BINARY_HOOK))
                        continue; // stopping this iteration as filename format is incorrect
                    int pos = s.indexOf(".bin");
                    String s2 = s.substring(BINARY_HOOK.length(), pos);
                    String[] version = s2.split(FWVERSION_REGEX);
                    if(!isInit){
                        mostRecentFwVersion = version;
                        isInit = true;
                    }else{
                        if(mostRecentFwVersion.length != version.length)
                            continue; //error in this iteration
                        for (int j = 0; j < version.length; j++) {
                            if(Integer.parseInt(version[j]) > Integer.parseInt(mostRecentFwVersion[j])){
                                mostRecentFwVersion = version;
                                break;
                            }
                        }
                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mostRecentFwVersion;
    }

    /**
     * This method writes the file length at the corresponding position in the file buffer. Not necessary to use it if post processing macro has already written the file length.
     */
    private void integrateFileLength(){
        ByteBuffer b = ByteBuffer.allocate(4);
        //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
        b.putInt(this.mFileLength);
        byte[] crc = b.array();
        for(int i=0; i<4 ; i++){
            mFileBuffer[OAD_FILE_LENGTH_OFFSET+i] = crc[3-i];
        }
    }

    /**
     * This method writes the CRC32 at the corresponding position in the file buffer. Not necessary to use it if post processing macro has already written the CRC32
     */
    private void integrateCRCToFile() {
        ByteBuffer b = ByteBuffer.allocate(4);
        //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
        b.putInt(this.crc32);
        byte[] crc = b.array();
        for(int i=0; i<4 ; i++){
            mFileBuffer[OAD_CRC_OFFSET+i] = crc[3-i];
        }
    }

    /**
     * This method computes the CRC32 over the file buffer. Only the 4 bytes reserved for the CRC32 storage are bypassed during the computation.
     * @return the CRC32 over the file buffer
     */
    private int computeCRC()
    {
        CRC32 x = new CRC32();
        int addr = 0, crc32 = 0xFFFFFFFF, nbByte = 0;
        byte[] tempProgBuffer;
        // ReadRequestEvent full APP code from extrernal Flash to compute CRC32 before comparison
        while (addr< getmFileLengthAsInteger())
        {
            if(addr < OAD_CRC_OFFSET)
                nbByte = (addr+TARGET_FLASH_PAGE_SIZE <= OAD_CRC_OFFSET)?TARGET_FLASH_PAGE_SIZE:OAD_CRC_OFFSET-addr;
            else // Jump 4 bytes ahead for a full word
                nbByte = (addr+TARGET_FLASH_PAGE_SIZE<= getmFileLengthAsInteger())?TARGET_FLASH_PAGE_SIZE: getmFileLengthAsInteger()-addr;

            // This copies a read page operation on firmware
            tempProgBuffer = new byte[nbByte];
            System.arraycopy(mFileBuffer, addr, tempProgBuffer, 0, nbByte);

            // Update CRC32 over the current buffer
            crc32 = crc32_Calculate(tempProgBuffer, nbByte, crc32);
            x.update(tempProgBuffer);

            // Move on
            if(addr < OAD_CRC_OFFSET){
                if((addr + TARGET_FLASH_PAGE_SIZE) <= OAD_CRC_OFFSET){
                    addr += TARGET_FLASH_PAGE_SIZE;
                }else{
                    addr = OAD_CRC_OFFSET + 4;
                }
            }
            else{

                addr+=nbByte;
            }

        }
        Log.i(TAG, "crc32 = " + Integer.toHexString(~crc32) + "      x.getValue = " + Long.toHexString(x.getValue()));
        return (~crc32);
    }

    /**
     * Computes local CRC32 over the data buffer in parameters
     * @param data
     * @param length
     * @param crc32
     * @return locally computed CRC32
     */
    private int crc32_Calculate(byte[] data, int length, int crc32)
    {
        int count;
        byte[] buffer;
        int tempChar;

        // Get a pointer to the start of the data and the number of bytes to
        // process.
        buffer = data;
        count = length;
        // Perform the algorithm on each byte in the supplied buffer using the
        // lookup table values calculated in InitCRC32Table().

        /*for (byte b : data) {
            crc32 = (crc32 >>> 8) ^ g_pui32CRC32Table[(crc32 ^ b) & 0xff];
        }*/

        for(int i = 0; i < length; i++){
            tempChar = data[i] & 0xFF;
            crc32 = (crc32 >> 8) ^ g_pui32CRC32Table[(crc32 & 0xFF) ^
                    + (tempChar)];
        }

        //
        // Return the result.
        //
        return(crc32);
    }

    /**
     *
     * @return the final oad buffer
     */
    @NonNull
    public ArrayList<byte[]> getmOadBuffer(){
        return mOadBuffer;
    }


    /**
     *
     * @return fw version read directly from file buffer
     */
    @Nullable
    private String getFWVersionFromFile(){
        String fwVersion = null;
        byte[] tempBuffer = new byte[4];
        System.arraycopy(mFileBuffer, OAD_FW_VERSION_OFFSET, tempBuffer, 0, 4);
        fwVersion = new String(tempBuffer);
        return fwVersion;
    }

    /**
     * This method creates the final buffer used for transfer. It creates chunks of the file buffer and stores them into an arraylist. it also adds the correct block index at the beginning of each block
     */
    private void createBufferFromBinaryFile(){
        mProgInfo.reset();
        byte[] tempBuffer;

        while(mProgInfo.iBlocks < mProgInfo.nBlocks){
            // Prepare block
            tempBuffer = new byte[OAD_BUFFER_SIZE];
            tempBuffer[0] = ConversionUtils.loUint16(mProgInfo.iBlocks);
            tempBuffer[1] = ConversionUtils.hiUint16(mProgInfo.iBlocks);
            if(mProgInfo.iBytes  + OAD_BLOCK_SIZE > getmFileLengthAsInteger()){
                int remainder = getmFileLengthAsInteger() - mProgInfo.iBytes;
                System.arraycopy(mFileBuffer, mProgInfo.iBytes, tempBuffer, 2, remainder);
                for(int i = remainder + 2; i < OAD_BUFFER_SIZE; i++){
                    tempBuffer[i] = (byte)0xFF;
                }
            }else{
                System.arraycopy(mFileBuffer, mProgInfo.iBytes, tempBuffer, 2, OAD_BLOCK_SIZE);
            }

            mProgInfo.iBlocks++;
            mProgInfo.iBytes += OAD_BLOCK_SIZE;
            mOadBuffer.add(tempBuffer);
        }
        Log.i(TAG, "buffer complete");

        //Resetting programInfo for the oad session
        mProgInfo.reset();
    }

    @Nullable
    public String getFwVersion() {
        return fwVersion;
    }


    @NonNull
    public ProgInfo getmProgInfo(){
        return mProgInfo;
    }


    /**
     * This class helps getting the number of blocks, the current blocks added to the final buffer, and the total number of bytes
     */
    public class ProgInfo {
        public int iBytes = 0; // Number of bytes programmed
        public short iBlocks = 0; // Number of blocks programmed
        public short nBlocks = 0; // Total number of blocks

        void reset() {
            iBytes = 0;
            iBlocks = 0;
            nBlocks = (short) ((getmFileLengthAsInteger() / (OAD_BLOCK_SIZE)) + ((getmFileLengthAsInteger()%OAD_BLOCK_SIZE) == 0 ? 0 : 1));
        }
    }

    /**
     * This method find the file named filepath from the application's assets.
     * @param assetManager
     * @param filepath
     * @return true if loading succeeded, false otherwise
     */
    private boolean loadFile(AssetManager assetManager, String filepath) {
        try {
            // ReadRequestEvent the file raw into a buffer
            InputStream stream;
            stream = assetManager.open(filepath);
            mFileLength = stream.read(mFileBuffer, 0, mFileBuffer.length);
            stream.close();
        } catch (IOException e) {
            // Handle exceptions here
            Log.e(TAG, "File open failed: " + filepath + "\n");
            return false;
        }

        return true;
    }


    /**
     *
     * @return file length as integer number
     */
    private int getmFileLengthAsInteger() {
        return mFileLength;
    }

    /**
     *
     * @return file length as byte array
     */
    public byte[] getmFileLengthAsByteArray(){
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putInt(this.mFileLength);
        return b.array();
    }

    /**
     *
     * @return fw version as a byte array
     */
    public byte[] getmFWVersionAsByteArray(){
        ByteBuffer b = ByteBuffer.allocate(2);
        for(int i = 0; i < 2; i++ ){
            b.put(this.mFileBuffer[OAD_FW_VERSION_OFFSET+i]);
        }
        return b.array();
    }

    /**
     *
     * @return fw version as a short value
     */
    public short getmFWVersionasShort(){
        ByteBuffer b = ByteBuffer.allocate(2);
        b.put(this.getmFWVersionAsByteArray());
        return b.getShort();
    }

    /**
     * static CRC32 table. Used in crc32 computation
     */
    @NonNull
    private static int[] g_pui32CRC32Table = {
            0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3,
            0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91,
            0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
            0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9, 0xfa0f3d63, 0x8d080df5,
            0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
            0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,
            0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599, 0xb8bda50f,
            0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d,
            0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433,
            0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01,
            0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457,
            0x65b0d9c6, 0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65,
            0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb,
            0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9,
            0x5005713c, 0x270241aa, 0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
            0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad,
            0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683,
            0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
            0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb, 0x196c3671, 0x6e6b06e7,
            0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
            0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
            0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef, 0x4669be79,
            0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f,
            0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d,
            0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713,
            0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21,
            0x86d3d2d4, 0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777,
            0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45,
            0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db,
            0xaed16a4a, 0xd9d65adc, 0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
            0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf,
            0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d,
    };
}
