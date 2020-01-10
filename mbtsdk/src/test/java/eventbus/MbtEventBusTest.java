package eventbus;

import org.greenrobot.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import command.DeviceCommands;
import core.bluetooth.requests.CommandRequestEvent;
import engine.SimpleRequestCallback;



public class MbtEventBusTest {

    @Before
    public void setUp()  {

    }

    //todo test onEventCallback is triggered
    @Test
    public void postEventWithCallback() {
    }

    /**
     * Check that the postEvent posts the event given in input
     * and that no callback is registered if no calbback is given in input
     */
    @Test
    public void postEvent_CommandRequestEvent_noCallback_validEvent() {
        EventBus bus = Mockito.spy(EventBus.class);
        MbtEventBus.BUS = bus;
        SimpleRequestCallback callback = Mockito.mock(SimpleRequestCallback.class);
        CommandRequestEvent requestEvent = new CommandRequestEvent(new DeviceCommands.ConnectAudio());

        MbtEventBus.postEvent(requestEvent);
        Mockito.verify(bus).post(requestEvent);
        Mockito.verify(bus,Mockito.never()).register(callback);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the event given in input is null
     */
    @Test
    public void postEvent_CommandRequestEvent_noCallback_nullEvent() {
        EventBus bus = Mockito.spy(EventBus.class);

        MbtEventBus.postEvent(null);
        Mockito.verify(bus, Mockito.never()).post(null);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the event given in input is null
     */
    @Test
    public void postEvent_CommandRequestEvent_withCallback_nullEvent() {
        SimpleRequestCallback callback = new SimpleRequestCallback() {
            @Override
            public void onRequestComplete(Object object) {

            }
        };
        EventBus bus = Mockito.spy(EventBus.class);

        MbtEventBus.postEvent(null, callback);
        Mockito.verify(bus,Mockito.never()).register(callback);
        Mockito.verify(bus, Mockito.never()).post(null);
    }

    /**
     * Check that the postEvent does not post the event given in input
     * and that the callback given in input is not registered
     * if the callback AND the event given in input is null
     */
    @Test
    public void postEvent_CommandRequestEvent_withCallback_nullCallbackEvent() {
        EventBus bus = Mockito.spy(EventBus.class);

        MbtEventBus.postEvent(null, null);
        Mockito.verify(bus,Mockito.never()).register(null);
        Mockito.verify(bus, Mockito.never()).post(null);
    }



}