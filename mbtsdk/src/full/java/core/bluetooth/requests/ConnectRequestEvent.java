package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;

/**
 * An event class when a connection request is being sent by the user.
 */
public class ConnectRequestEvent extends BluetoothRequests {
        private String name;

        public ConnectRequestEvent(String name){
            this.name = name;
        }

        public String getName(){
            return this.name;
        }
}