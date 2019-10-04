package core.synchronisation.midi;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class MidiController {
    private static final String TAG = MidiController.class.getSimpleName();
    private static final byte NOTE_ON = (byte) 0x90;
    private static final byte NOTE_OFF = (byte) 0x80;

    private MidiDeviceInfo[] infos;

    private MidiManager midiManager;
    private MidiInputPort inputPort;
    private WeakReference<Context> contextWeakReference;
    private MidiObject previousMidiObject = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    MidiController(Context context){
        this.midiManager = (MidiManager)context.getSystemService(Context.MIDI_SERVICE);
        this.contextWeakReference = new WeakReference<>(context);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    void sendNote(MidiObject object){

        MidiOutput output;
        try {
            if(previousMidiObject != null){
                output = createMidiTrams(previousMidiObject, NOTE_OFF);
                send(output);
            }

            output = createMidiTrams(object, NOTE_ON);
            send(output);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MidiOutput createMidiTrams(@NonNull MidiObject midiObject, byte noteMode) {
        previousMidiObject = midiObject;

        ByteBuffer byteBuffer = ByteBuffer.allocate(32);

        byte[] buffer = new byte[32];
        int numBytes = 0;
        int channel = 3; // MIDI channels 1-16 are encoded as 0-15.
        buffer[numBytes++] = ((byte)(noteMode + (channel - 1))); // note on
        buffer[numBytes++] = (byte)midiObject.note; // pitch is middle C
        buffer[numBytes++] = (byte)midiObject.velocity; // max velocity

        // post is non-blocking
        return new MidiOutput(buffer, numBytes);
    }


    /**
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    void openDevice(final int portNumber, final OnDeviceOpenedListener onDeviceOpenedListener){
        MidiDeviceInfo[] infos = midiManager.getDevices();

        if(infos.length ==0)
            throw new RuntimeException("No device found");

        final MidiDeviceInfo info = infos[0];
        midiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device == null) {
                    Log.e(TAG, "could not open device " + info);
                } else {
                    inputPort = device.openInputPort(portNumber);
                    if(onDeviceOpenedListener != null)
                        onDeviceOpenedListener.onDeviceOpened(device);

                }
            }

        },new Handler(contextWeakReference.get().getMainLooper()));
    }




    @RequiresApi(api = Build.VERSION_CODES.M)
    private void send(MidiOutput output) throws IOException {
        if(inputPort == null)
            return;

        inputPort.send(output.midiMessage, 0, output.length);
    }

    public interface OnDeviceOpenedListener{
        void onDeviceOpened(MidiDevice device);

        void onFailedToOpenDevice();
    }

}
