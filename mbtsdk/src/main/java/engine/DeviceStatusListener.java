package engine;

import core.device.DCOffsets;
import core.device.SaturationEvent;

public interface DeviceStatusListener extends ErrorEvent{
        /**
         * Callback indicating that the headset has entered in a new saturation state
         * @param saturation the new measured state.
         */
        void onSaturationStateChanged(SaturationEvent saturation);

        /**
         * Callback indicating that the headset has measured a new dc offset
         * @param dcOffsets the new measured offsets at a specific time.
         */
        void onNewDCOffsetMeasured(DCOffsets dcOffsets);
    }
