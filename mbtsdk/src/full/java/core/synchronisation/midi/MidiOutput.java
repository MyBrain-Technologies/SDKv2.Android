package core.synchronisation.midi;

public class MidiOutput {
    byte[] midiMessage;
    int length;

    public MidiOutput(byte[] bytes, int length){
        this.midiMessage = bytes;
        this.length = length;

    }

}
