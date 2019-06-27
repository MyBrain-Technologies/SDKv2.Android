package utils;

import java.util.Arrays;

public class CommandUtils {

    /**
     * Return the data sent by the headset
     * in response to a characteristic writing operation
     * without the identifier code (first byte has been removed)
     */
    public static byte[] deserialize(byte[] response){
        return Arrays.copyOfRange(response, 1 ,response.length);
    }
}
