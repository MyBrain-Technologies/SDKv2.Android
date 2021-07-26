package model

enum class EnumRecordingType {
    CALIBRATION,// : acquisition aimed to get a reference EEG measure of the subject (e.g. 30 second in melomind app)

    SESSION,// : acquisition done during a neurofeedback exercise in melomind app

    RAWDATA,// : EEG acquisition unrelated to melomind app (deprecated)

    STUDY,// : EEG acquisition done in the context of a neurosciences protocole (deprecated)

    ACQUISITION,// : EEG acquisition done in the context of a neurosciences protocole

    ADJUSTMENT,// : EEG acquisition done during headset adjustment step.

    RESTING_STATE_PRE_SESSION_EYES_CLOSED,// : EEG acquisition done during resting state before a session with eyes closed

    RESTING_STATE_PRE_SESSION_EYES_OPEN,// : EEG acquisition done during resting state before a session with eyes open

    RESTING_STATE_POST_SESSION_EYES_CLOSED ,//: EEG acquisition done during resting state after a session with eyes closed

    RESTING_STATE_POST_SESSION_EYES_OPEN ,//: EEG acquisition done during resting state after a session with eyes open
}