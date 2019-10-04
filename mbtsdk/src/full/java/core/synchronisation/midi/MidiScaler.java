package core.synchronisation.midi;


import android.support.annotation.Keep;

@Keep
public class MidiScaler {

    //**********************************************************************************************
    // Attributes
    //**********************************************************************************************

    /**
     * Durqtion in ms
     */
    private static final long DURATION = 1000;
    /**
     * MIDI base note, corresponding to a C4
     */
    private static final int MIDI_BASE_NOTE = 60;

    // Between 0 (C) and 12 (B)
    private int baseNote;
    private Scale scale;
    private Key key;
    /**
     * La brioche de potch :D
     */
    private int pitch;
    

    //**********************************************************************************************
    // initialization
    //**********************************************************************************************

    /**
     * The default scale is MAJOR and the default base note is C4
     */
    MidiScaler() {
        this.scale = Scale.MAJOR;
        this.pitch = 0;
        this.key = Key.C;
        setBaseNoteFromKey(this.key);
    }

    /**
     * Because we think about you  <3 xoxo
     */
    MidiScaler(Scale scale, Key key, int pitch) {
        this.scale = scale;
        this.key = key;
        this.pitch = pitch;
        setBaseNoteFromKey(this.key);
    }

    //**********************************************************************************************
    // Getters / Setters
    //**********************************************************************************************

//    void setMidiBaseNote(int baseNote) {
//        this.baseNote = baseNote;
//    }

    void setKey(Key key){
        this.key = key;
        setBaseNoteFromKey(key);
    }

    int getMidiBaseNote() {
        return this.baseNote;
    }

    void setMidiScale(Scale scale) {
        this.scale = scale;
    }

    Scale getScale() {
        return this.scale;
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
    }


    //**********************************************************************************************
    // Processing
    //**********************************************************************************************

    private void setBaseNoteFromKey(Key key){
        this.baseNote = Key.valueOf(key.toString()).ordinal();
    }

    MidiObject createMidiObject(int relativeNote, int velocity) {
        int cumulatedIntervals = 0;
        for (int i = 0; i <= relativeNote; i++) {
            int intervalLength = this.scale.getIntervals().length;
            int modI = i % intervalLength;
            cumulatedIntervals += this.scale.getIntervals()[modI];
        }
        int midiNote = MIDI_BASE_NOTE + this.baseNote + cumulatedIntervals + this.pitch * 12;
        long duration = DURATION;

        return new MidiObject(midiNote, velocity, duration);
    }
}
