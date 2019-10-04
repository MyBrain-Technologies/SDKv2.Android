package core.synchronisation.midi;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.os.Build;

import utils.LogUtils;


public class MidiProcessor {

    private MidiController controller;
    private MidiScaler midiScaler;

    public MidiProcessor(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            controller = new MidiController(context.getApplicationContext());
            controller.openDevice(0, new MidiController.OnDeviceOpenedListener() {
                @Override
                public void onDeviceOpened(MidiDevice device) {
//                    new Timer("testdesesmorts", true).scheduleAtFixedRate(task, 200, 1000);
                    LogUtils.d("MidiProcessor", "Device opened");
                }

                @Override
                public void onFailedToOpenDevice() {
                    LogUtils.e("MidiProcessor", "Device failed to be open");
                }
            });


        }
        midiScaler = new MidiScaler();

    }

    public void sendMidi(Note note, int velocity, Key key, int pitch, Scale scale) {

        midiScaler.setMidiScale(scale);
        midiScaler.setKey(key);
        midiScaler.setPitch(pitch);

        if(controller != null && midiScaler != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                LogUtils.e("MidiProcessor", "Sending note");
                controller.sendNote(midiScaler.callMeForMagic(note.ordinal(), velocity));
            }
    }
}
