package core.bluetooth.requests;

/**
 * An event class when a connection request is being sent
 */
public class StartOrContinueConnectionRequestEvent extends BluetoothRequests {

        private boolean isClientUserRequest;

        public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest){
                this.isClientUserRequest = isClientUserRequest;
        }

        public boolean isClientUserRequest() {
                return isClientUserRequest;
        }
}