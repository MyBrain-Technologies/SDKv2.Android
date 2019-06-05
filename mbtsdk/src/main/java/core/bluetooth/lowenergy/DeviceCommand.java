package core.bluetooth.lowenergy;


import engine.SimpleRequestCallback;

public abstract class DeviceCommand {

        SimpleRequestCallback<byte[]> responseCallback;

        public SimpleRequestCallback<byte[]> getResponseCallback() {
            return responseCallback;
        }

    }



