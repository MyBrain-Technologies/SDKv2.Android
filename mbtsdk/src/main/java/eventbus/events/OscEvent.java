package eventbus.events;

import android.support.annotation.NonNull;
import android.system.Os;

import config.OscConfig;
import core.eeg.storage.MbtEEGPacket;

/**
 * Event posted when a OSC stream is initialized
 *
 * @author Sophie Zecri on 24/05/2018
 */
public class OscEvent { //Events are just POJO without any specific implementation

    public static class StreamEvent {

    }

    /**
     * Event posted when a OSC stream is initialized
     *
     * @author Sophie Zecri on 24/05/2018
     */
    public static class InitEvent { //Events are just POJO without any specific implementation

        private OscConfig oscConfig;

        public InitEvent(@NonNull OscConfig oscConfig) {
            this.oscConfig = oscConfig;
        }

        /**
         * Gets the ip address and port to OSC stream
         */
        public OscConfig getOscConfig() {
            return oscConfig;
        }

    }
}

