package core.recording.metadata

import androidx.annotation.StringDef


/**
 * Session flow state. It is mostly used to make sure every step is done in the correct order.
 */
private const val RESTING = "RESTING"
private const val PRE_SESSION_RESTING = "PRE_SESSION_RESTING"
private const val POST_SESSION_RESTING = "POST_SESSION_RESTING"

const val INIT = "INIT"
const val IDLE = "IDLE"
const val ADJUSTMENT = "ADJUSTMENT"
const val PRE_SESSION_RESTING_STATE_EYES_OPEN = "PRE_SESSION_RESTING_STATE_EYES_OPEN"
const val PRE_SESSION_RESTING_STATE_EYES_CLOSED = "PRE_SESSION_RESTING_STATE_EYES_CLOSED"
const val POST_SESSION_RESTING_STATE_EYES_OPEN = "POST_SESSION_RESTING_STATE_EYES_OPEN"
const val POST_SESSION_RESTING_STATE_EYES_CLOSED = "POST_SESSION_RESTING_STATE_EYES_CLOSED"
const val CALIBRATION = "CALIBRATION"
const val EXERCISE = "SESSION"
const val ABORTED = "ABORTED"
const val COMPLETED = "COMPLETED"
const val RAWDATA = "RAWDATA"
const val STUDY = "STUDY"

@Retention(AnnotationRetention.SOURCE) //Enums have cleaner readability but are quite heavy
@StringDef(INIT, IDLE, ADJUSTMENT, PRE_SESSION_RESTING_STATE_EYES_OPEN, POST_SESSION_RESTING_STATE_EYES_OPEN, PRE_SESSION_RESTING_STATE_EYES_CLOSED, POST_SESSION_RESTING_STATE_EYES_CLOSED, CALIBRATION, EXERCISE, ABORTED, COMPLETED, RAWDATA, STUDY)
annotation class SessionStep

fun String.isResting() = contains(RESTING)
fun String.isPreResting() = contains(PRE_SESSION_RESTING)
fun String.isPostResting() = contains(POST_SESSION_RESTING)
