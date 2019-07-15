package core.device.oad;

import android.support.annotation.NonNull;


public class OADManager {

    public static final int BUFFER_LENGTH = 14223;
    public static final int CHUNK_NB_BYTES = 14223;
    public static final int FILE_LENGTH_NB_BYTES = 4;
    public static final int FIRMWARE_VERSION_NB_BYTES = 2;

    public OADManager() {
    }

    public void notifyOADStateChanged(@NonNull OADState state) {

    }

    public void notifyOADStateChanged(@NonNull OADState state, String additionalInfo) {

    }
}
