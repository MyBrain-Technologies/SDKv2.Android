package eventbus;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import command.DeviceCommands;
import core.bluetooth.requests.DeviceCommandRequestEvent;
import core.device.DeviceEvents;
import engine.SimpleRequestCallback;



public class EventBusManagerTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void registerOrUnregister() {
    }

    @Test
    public void postEvent() {
    }

    //todo test onEventCallback is triggered
    @Test
    public void postEventWithCallback() {
    }

    /**
     * Check that the postEvent posts the event given in input
     * and that the callback given in input is registered
     */
    @Test
    public void postEvent_DeviceCommandRequest_withCallback_validCallback() {
        EventBus bus = Mockito.spy(EventBus.class);
        EventBusManager.BUS = bus;
        EventBusManager.Callback callback = new EventBusManager.Callback<DeviceEvents.RawDeviceResponseEvent>(){
            @Override
            @Subscribe
            public Void onEventCallback(DeviceEvents.RawDeviceResponseEvent headsetRawResponse) {

                EventBusManager.registerOrUnregister(false, this);
                return null;
            }
        };
        DeviceCommandRequestEvent requestEvent = new DeviceCommandRequestEvent(new DeviceCommands.ConnectAudio(new SimpleRequestCallback<byte[]>() {
            @Override
            public void onRequestComplete(byte[] object) {

            }
        }));

        EventBusManager.postEvent(requestEvent, callback);
        Mockito.verify(bus).register(callback);
        Mockito.verify(bus).post(requestEvent);
    }

    /**
     * Check that the postEvent posts the event given in input
     * and that no callback is registered if no calbback is given in input
     */
    @Test
    public void postEvent_DeviceCommandRequest_noCallback_validEvent() {
        EventBus bus = Mockito.spy(EventBus.class);
        EventBusManager.BUS = bus;
        SimpleRequestCallback callback = Mockito.mock(SimpleRequestCallback.class);
        DeviceCommandRequestEvent requestEvent = new DeviceCommandRequestEvent(new DeviceCommands.ConnectAudio());

        EventBusManager.postEvent(requestEvent);
        Mockito.verify(bus).post(requestEvent);
        Mockito.verify(bus,Mockito.never()).register(callback);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the callback given in input is null
     */
    @Test
    public void postEvent_DeviceCommandRequest_withCallback_nullCallback() {
        SimpleRequestCallback callback = null;
        DeviceCommandRequestEvent requestEvent = new DeviceCommandRequestEvent(new DeviceCommands.ConnectAudio(callback));
        EventBus bus = Mockito.spy(EventBus.class);

        EventBusManager.postEvent(requestEvent);
        Mockito.verify(bus,Mockito.never()).register(callback);
        Mockito.verify(bus, Mockito.never()).post(requestEvent);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the event given in input is null
     */
    @Test
    public void postEvent_DeviceCommandRequest_noCallback_nullEvent() {
        DeviceCommandRequestEvent requestEvent = null;
        EventBus bus = Mockito.spy(EventBus.class);

        EventBusManager.postEvent(requestEvent);
        Mockito.verify(bus, Mockito.never()).post(requestEvent);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the event given in input is null
     */
    @Test
    public void postEvent_DeviceCommandRequest_withCallback_nullEvent() {
        SimpleRequestCallback callback = new SimpleRequestCallback() {
            @Override
            public void onRequestComplete(Object object) {

            }
        };
        DeviceCommandRequestEvent requestEvent = null;
        EventBus bus = Mockito.spy(EventBus.class);

        EventBusManager.postEvent(requestEvent, callback);
        Mockito.verify(bus,Mockito.never()).register(callback);
        Mockito.verify(bus, Mockito.never()).post(requestEvent);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the callback AND the event given in input is null
     */
    @Test
    public void postEvent_DeviceCommandRequest_withCallback_nullCallbackEvent() {
        SimpleRequestCallback callback = null;
        DeviceCommandRequestEvent requestEvent = null;
        EventBus bus = Mockito.spy(EventBus.class);

        EventBusManager.postEvent(requestEvent, callback);
        Mockito.verify(bus,Mockito.never()).register(callback);
        Mockito.verify(bus, Mockito.never()).post(requestEvent);
    }



}