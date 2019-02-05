package core.bluetooth.requests;

/**
 * An event class when a connection request is being sent by the user.
 */
public class ScanRequestEvent extends BluetoothRequests {
        private String name;

        public ScanRequestEvent(String name){
            this.name = name;
        }

        public String getName(){
            return this.name;
        }
}