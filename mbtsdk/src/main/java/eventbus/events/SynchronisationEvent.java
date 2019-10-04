package eventbus.events;

import core.synchronisation.midi.Key;
import core.synchronisation.midi.MidiScaler;
import core.synchronisation.midi.Note;
import core.synchronisation.midi.Scale;

/**
 * Event posted when a OSC stream is initialized
 *
 * @author Sophie Zecri on 24/05/2018
 */
public interface SynchronisationEvent { //Events are just POJO without any specific implementation


    /**
     * Event posted when a MIDI note is streamed
     *
     * @author Sophie Zecri on 24/05/2018
     */
     class MidiEvent {

        private Note note;
        private int velocity;
        private int pitch;
        private Key key;
        private Scale scale;


        public MidiEvent(Note note, int velocity, int pitch, Scale scale, Key key) {
            this.note = note;
            this.velocity = velocity;
            this.key = key;
            this.pitch = pitch;
            this.scale = scale;
        }

        public Note getNote() {
            return note;
        }

        public int getVelocity() {
            return velocity;
        }

        public int getPitch() {
            return pitch;
        }

        public Key getKey() {
            return key;
        }

        public Scale getScale() {
            return scale;
        }
    }
}

