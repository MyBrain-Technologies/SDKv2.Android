package core.device.oad;

import android.support.annotation.Keep;

import command.OADCommands;

/**
     * Contract used to receive a notification when an OAD event occurs
     */
    @Keep
    public interface OADContract {

        /**
         * Callback triggered when the OAD update process need to be started
         */
        void startOADUpdate();

        /**
         * Callback triggered when the OAD update process need to be started
         */
        void stopOADUpdate();

        /**
         * Callback triggered when the OAD update process need to be started
         */
        void requestFirmwareValidation(OADCommands.RequestFirmwareValidation requestFirmwareValidation);

        void transferPacket(OADCommands.SendPacket sendPacket);
}