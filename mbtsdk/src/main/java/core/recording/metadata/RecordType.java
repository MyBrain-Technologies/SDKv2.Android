package core.recording.metadata;

import android.support.annotation.Keep;

@Keep
public enum RecordType {

    ADJUSTMENT,
    CALIBRATION,
    PRE_SESSION_RESTING_STATE_EYES_OPEN,
    PRE_SESSION_RESTING_STATE_EYES_CLOSED,
    SESSION,
    POST_SESSION_RESTING_STATE_EYES_OPEN,
    POST_SESSION_RESTING_STATE_EYES_CLOSED,
    RAWDATA,
    STUDY
}
