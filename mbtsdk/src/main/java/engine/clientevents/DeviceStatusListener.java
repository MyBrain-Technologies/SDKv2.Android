package engine.clientevents;

import android.support.annotation.Keep;

import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;

@Keep
public interface DeviceStatusListener<U extends BaseError> extends BaseErrorEvent<U>{
        /**
         * Callback indicating that the headset has entered in a new saturation state
         * @param saturation the new measured state.
         */
        void onSaturationStateChanged(SaturationEvent saturation);

        /**
         * Callback indicating that the headset has measured a new dc offset
         * @param dcOffsets the new measured offsets at a specific time.
         */
        void onNewDCOffsetMeasured(DCOffsetEvent dcOffsets);
    }
