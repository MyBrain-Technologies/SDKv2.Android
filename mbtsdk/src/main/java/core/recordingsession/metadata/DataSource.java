package core.recordingsession.metadata;

/**
 * Source of EEG acquisition
 */
public enum DataSource {
    /**
     * Explain that the source of EEG is a free session of melomind application
     */
    FREESESSION,

    /**
     * Explain that the source of EEG is a session of melomind application's relax program
     */
    RELAX_PROGRAM,

    /**
     * Explain that the source of EEG is a default recording session
     */
    DEFAULT
}
