package eventbus.events;

import android.support.annotation.NonNull;

import config.SynchronisationConfig;

/**
 * Event posted when a OSC stream is initialized
 *
 * @author Sophie Zecri on 24/05/2018
 */
public class SynchronisationEvent { //Events are just POJO without any specific implementation

    public interface StreamingEvent{
        void InitEvent();
    }

    public static class StreamEvent {

    }

    /**
     * Event posted when a OSC stream is initialized
     *
     * @author Sophie Zecri on 24/05/2018
     */
    public static class InitEvent { //Events are just POJO without any specific implementation

        private SynchronisationConfig oscConfig;

        public InitEvent(@NonNull SynchronisationConfig oscConfig) {
            this.oscConfig = oscConfig;
        }

        /**
         * Gets the ip address and port to OSC stream
         */
        public SynchronisationConfig getSynchronisationConfig() {
            return oscConfig;
        }

    }
}

