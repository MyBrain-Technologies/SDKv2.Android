package core.device.oad;

import android.support.annotation.Keep;

import command.OADCommands;
import core.device.model.FirmwareVersion;
import eventbus.events.FirmwareUpdateClientEvent;

/**
 * Contract used to receive a notification when an OAD event occurs
 */
@Keep
public interface OADContract {

    /**
     * Callback triggered when the OAD update process need to be started
     */
    void stopOADUpdate();

    /**
     * Callback triggered when the OAD update process need to be started
     */
    void requestFirmwareValidation(OADCommands.RequestFirmwareValidation requestFirmwareValidation);

    void transferPacket(OADCommands.SendPacket sendPacket);

    void notifyClient(FirmwareUpdateClientEvent event);

    void resetCacheAndKeys();

    void reconnect();

    /**
     * Return true if the current firmware version is equal to the input firmware version, false otherwise.
     * @return true if the current firmware version is equal to the input firmware version, false otherwise.
     */
    boolean compareFirmwareVersion(FirmwareVersion firmwareVersionExpected);
}