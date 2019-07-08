package utils;

/**
 * Utils class for bitwise operations
 * (bit masking, bitwise operators manipulations, bit shifts
 */
public class BitUtils {

    /**
     * Return true if the mask is present in the byte data
     * A mask or bitmask is data that is used for bitwise operations.
     * Using a mask, multiple bits in a byte can be set either on,
     * off or inverted from on to off (or vice versa) in a single bitwise operation.
     */
    public static boolean isMaskInByteData(byte mask, byte data){
        return (data & mask) == mask;
    }
}
