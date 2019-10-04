package core.synchronisation.midi;

public class MidiObject{
    int note;
    int velocity;
    long duration;


    public MidiObject(int note, int velocity, long duration) {
        this.note = note;
        this.velocity = velocity;
        this.duration = duration;
    }

    public int getNote() {
        return note;
    }

    public int getVelocity() {
        return velocity;
    }

    public long getDuration() {
        return duration;
    }
}
