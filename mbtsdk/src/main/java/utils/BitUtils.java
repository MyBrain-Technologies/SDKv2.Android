package utils;

/**
 * Utils class for bitwise operations
 * (bit masking, bitwise operators manipulations, bit shifts
 */
public class BitUtils {

    /**
     * Apply the input mask mask to the input data and return the result value.
     * A mask or bitmask is data that is used for bitwise operations.
     * Using a mask, multiple bits in a byte can be set either on,
     * off or inverted from on to off (or vice versa) in a single bitwise operation.
     * mask & data means that all the bits of both numbers are compared one by one
     * and the resulting number is calculated based on values of the bits from numbers mask and data.
     * Bitwise AND is similar to logical AND in a sense that it results in 1 only
     * when the two compared bits are both equal to 1.
     * Otherwise, it results in 0.
     * @param mask mask to apply
     * @param data digits to mask
     * @return the the result value
     */
    public static int maskAND(int data, int mask){
        return (mask & data);
    }

    /**
     * Apply the input mask mask to the input data and return the result value.
     * A mask or bitmask is data that is used for bitwise operations.
     * Using a mask, multiple bits in a byte can be set either on,
     * off or inverted from on to off (or vice versa) in a single bitwise operation.
     * mask & data means that all the bits of both numbers are compared one by one
     * and the resulting number is calculated based on values of the bits from numbers mask and data.
     * Bitwise AND is similar to logical AND in a sense that it results in 1 only
     * when the two compared bits are both equal to 1.
     * Otherwise, it results in 0.
     * @param mask mask to apply
     * @param data digits to mask
     * @return the the result value
     */
    public static int maskOR(int data, int mask){
        return (mask | data);
    }

    /**
     * Apply the input mask mask to the input data and return the result value.
     * A mask or bitmask is data that is used for bitwise operations.
     * Using a mask, multiple bits in a byte can be set either on,
     * off or inverted from on to off (or vice versa) in a single bitwise operation.
     * mask & data means that all the bits of both numbers are compared one by one
     * and the resulting number is calculated based on values of the bits from numbers mask and data.
     * Bitwise AND is similar to logical AND in a sense that it results in 1 only
     * when the two compared bits are both equal to 1.
     * Otherwise, it results in 0.
     * @param mask mask to apply
     * @param data digits to mask
     * @return the the result value
     */
    public static byte maskAND(byte data, byte mask){
        return (byte) (mask & data);
    }

    /**
     * Apply the input mask mask to the input data and return the result value.
     * A mask or bitmask is data that is used for bitwise operations.
     * Using a mask, multiple bits in a byte can be set either on,
     * off or inverted from on to off (or vice versa) in a single bitwise operation.
     * mask & data means that all the bits of both numbers are compared one by one
     * and the resulting number is calculated based on values of the bits from numbers mask and data.
     * Bitwise AND is similar to logical AND in a sense that it results in 1 only
     * when the two compared bits are both equal to 1.
     * Otherwise, it results in 0.
     * @param mask mask to apply
     * @param data digits to mask
     * @return the the result value
     */
    public static byte maskOR(byte data, byte mask){
        return (byte) (mask | data);
    }

    /**
     * Apply the input shift to the input data and return the result value
     * The data digits are moved, or shifted, to the right
     * The signed left shift operator ">>" shifts a bit pattern to the right.
     * @param shift number of digit to move to the right
     * @param data digits to shift
     * @return the the result value
     */
    public static int shiftRight(int data, int shift) {
        return data >> shift;
    }

    /**
     * Apply the input shift to the input data and return the result value
     * The data digits are moved, or shifted, to the left
     * The signed left shift operator "<<" shifts a bit pattern to the left.
     * @param shift number of digit to move to the left
     * @param data digits to shift
     * @return the the result value
     */
    public static int shiftLeft(int data, int shift) {
        return data << shift;
    }

    /**
     * Apply the input shift to the input data and return the result value
     * The data digits are moved, or shifted, to the left
     * The signed left shift operator "<<" shifts a bit pattern to the left.
     * @param shift number of digit to move to the left
     * @param data digits to shift
     * @return the the result value
     */
    public static byte shiftLeft(byte data, byte shift) {
        return (byte) (data << shift);
    }


    /**
     * Return 1 if the masked data is equal to the mask,
     * false otherwise.
     * @param mask mask to apply
     * @param data digits to mask
     */
    public static boolean areIntegerEquals(int data, int mask){
        return maskAND(mask,data) == mask;
    }

    /**
     * Return 1 if the masked data is equal to the mask,
     * false otherwise.
     * @param mask mask to apply
     * @param data digits to mask
     */
    public static boolean areByteEquals(byte data, byte mask){
        return maskAND(mask,data) == mask;
    }


    /**
     * Returns true if the bit is set, otherwise returns false
     * @param mask the mask to apply
     * @param shift the number of digit to move to the left
     * @param dataToShift the digits to shift
     */
    public static boolean isBitSet(byte mask, byte dataToShift, int shift) {
        return ! isZero((byte) maskAND(mask, shiftLeft(dataToShift, shift)));
    }
    /**
     * Returns true if the bit is set, otherwise returns false
     */
    public static Float isBitSet(byte b, int bit)
    {
        if((b & (1 << bit)) != 0)
            return 1f;
        else
            return 0f;
    }


    /**
     * Return true if valueToCheck is 0
     * Return false if valueToCheck is 1
     * @param valueToCheck is the byte value to check
     * @return true if valueToCheck is 0, false otherwise
     */
    public static boolean isZero(byte valueToCheck){
        return valueToCheck == 0;
    }

    /**
     * Return 0x00 if boolean value is false
     * Return 0x01 if boolean value is true
     * @param booleanValue value to convert
     * @return converted value in byte
     */
    public static byte booleanToBit(boolean booleanValue) {
        return booleanValue ?
                (byte)0x01 :
                (byte)0x00;
    }

    /**
     * Return false if byte value is 0x00
     * Return true if boolean value is 0x01
     * @param byteValue value to convert
     * @return converted value as a boolean
     */
    public static boolean bitToBoolean(byte byteValue) {
        if(byteValue != 0x01 && byteValue != 0x00)
            return false;

        return byteValue == (byte)0x01;
    }
}
