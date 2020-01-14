package core.device.oad;

import androidx.annotation.Keep;

import command.OADCommands;
import core.device.model.MbtVersion;
import eventbus.events.FirmwareUpdateClientEvent;

/**
 * Contract used to perform an action in the contracter class when an OAD event occurs
 */
@Keep
public interface OADContract {

    /**
     * Method to call when the OAD update process need to get the firmware validation of the new firmware
     */
    void requestFirmwareValidation(OADCommands.RequestFirmwareValidation requestFirmwareValidation);

    /**
     * Method to call when the OAD update process need to send an OAD binary file packet to the current firmware
     * @param packetToSend the OAD packet to send
     */
    void transferPacket(byte[] packetToSend);

    /**
     * Method to call when the OAD update process state and/or progress changes,
     * or when the SDK raises an error.
     * @param event the OAD packet to send
     */
    void notifyClient(FirmwareUpdateClientEvent event);

    /**
     * Method to call when the OAD update process need to clear the mobile device bluetooth
     */
    void clearBluetooth();

    /**
     * Method to call when the OAD update process need to reconnect the updated headset device.
     */
    void reconnect(boolean reconnectAudio);

    /**
     * Method to call when the OAD update process need to get the firmware validation of the new firmware.
     * Return true if the current firmware version is equal to the input firmware version, false otherwise.
     * @return true if the current firmware version is equal to the input firmware version, false otherwise.
     */
    boolean verifyFirmwareVersion(MbtVersion mbtVersionExpected);

    /**
     * Method to call when the OAD update process is over.
     */
    void stopOADUpdate();
}