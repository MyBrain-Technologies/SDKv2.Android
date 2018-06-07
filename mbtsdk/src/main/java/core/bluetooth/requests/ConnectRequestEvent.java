package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;

public class ConnectRequestEvent extends BluetoothRequests {
        private String name;

        public ConnectRequestEvent(String name){
            this.name = name;
        }

        public String getName(){
            return this.name;
        }
}