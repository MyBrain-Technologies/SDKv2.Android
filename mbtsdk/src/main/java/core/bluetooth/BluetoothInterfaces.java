package core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

import command.CommandInterface;

public class BluetoothInterfaces {

    interface IBluetooth extends IConnect{

    }

    interface IDataStream extends
            IBluetooth,
            IScan,
            IStream,
            IBattery,
            ICommand {

    }

    interface IAudioStream extends
            IBluetooth {

        //TODO add method for audio unit
        //IPlayable{
            //boolean play(track, volume, duration);
            //boolean pause();
            //boolean stop();
        // }
    }

    /**
     * Interface used to connect to or disconnect from a bluetooth peripheral device
     */
    interface IConnect {

        /**
         * ConnectRequestEvent to the peripheral device
         * @return true upon success, false otherwise
         */
        boolean connect(Context context, BluetoothDevice device);

        /**
         * Disconnect from the peripheral device
         * @return true upon success, false otherwise
         */
        boolean disconnect();


        /**
         * Method used to notify that the connection state has changed
         * @param newState The new bluetooth connection state. Refer to @{@link BtState}
         * for the complete list of states.
         */
        void notifyConnectionStateChanged(@NonNull final BtState newState);

        /**
         * @return whether or not the device is correctly connected, ie if current state is {@link BtState#CONNECTED_AND_READY}
         */
        boolean isConnected();

    }

    /**
     * Interface used to connect to or disconnect from a bluetooth peripheral device
     */
    interface IScan {

        /**
         * Start a classic discovery scan in order to find a bluetooth device
         * @param
         */
        boolean startScan();

        /**
         * Disconnect from the peripheral device
         */
        void stopScan();

    }

    /**
     * Created by Etienne on 08/02/2018.
     *
     * Interface used to start or stop streaming operations from a bluetooth peripheral device.
     * Streaming means here acquisition in real time of data from the peripheral, using supported bluetooth mechanisms.
     */
    public interface IStream {

        /**
         * Start a stream operation
         * @return
         */
        boolean startStream();

        /**
         * Stop a stream operation
         * @return
         */
        boolean stopStream();

        interface DataStreamListener{
            void onNewData();
        }

        /**
         * Called whenever the stream state has changed.
         * @param streamState the new StreamState
         */
        void notifyStreamStateChanged(StreamState streamState);

        /**
         *
         * @return true is streaming is in progress, false otherwise.
         */
        boolean isStreaming();
    }

    public interface ICommand {

        void sendCommand(CommandInterface.MbtCommand command);

    }

    public interface IBattery {

        boolean readBattery();

    }

    public interface IDeviceInfoMonitor{

        boolean readFwVersion();
        boolean readHwVersion();
        boolean readSerialNumber();
        boolean readModelNumber();

    }

}
