package command;

import org.junit.Test;

import engine.clientevents.BaseError;

import static org.junit.Assert.*;

public class DeviceCommandEventTest {

    @Test
    public void assembleCodes() {
        byte[] expected = new byte[]{0,1,2,3};
        byte[] result = DeviceCommandEvent.assembleCodes(new byte[]{0}, new byte[]{1,2,3});
        assertArrayEquals(expected, result);

        DeviceCommandEvent code = new DeviceStreamingCommands.StartEEGAcquisition().getIdentifier();
        expected = new byte[]{(byte) 0x3C,(byte)0x00,(byte)0x00,(byte)0x24,(byte)0x00,(byte)0x00,(byte)0x00 };
        result = DeviceCommandEvent.assembleCodes(
                DeviceCommandEvent.START_FRAME.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD_LENGTH.getAssembledCodes(),
                new byte[]{code.getIdentifierCode()},
                DeviceCommandEvent.COMPRESS.getAssembledCodes(),
                DeviceCommandEvent.PACKET_ID.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD.getAssembledCodes());
        assertArrayEquals(expected, result);

        code = new DeviceStreamingCommands.StopEEGAcquisition().getIdentifier();
        expected = new byte[]{(byte) 0x3C,(byte)0x00,(byte)0x00,(byte)0x25,(byte)0x00,(byte)0x00,(byte)0x00 };
        result = DeviceCommandEvent.assembleCodes(
                DeviceCommandEvent.START_FRAME.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD_LENGTH.getAssembledCodes(),
                new byte[]{code.getIdentifierCode()},
                DeviceCommandEvent.COMPRESS.getAssembledCodes(),
                DeviceCommandEvent.PACKET_ID.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD.getAssembledCodes());
        assertArrayEquals(expected, result);

        code = new DeviceStreamingCommands.EegConfig(new CommandInterface.CommandCallback<byte[]>() {
            @Override
            public void onError(CommandInterface.MbtCommand request, BaseError error, String additionalInfo) {

            }

            @Override
            public void onRequestSent(CommandInterface.MbtCommand request) {

            }

            @Override
            public void onResponseReceived(CommandInterface.MbtCommand request, byte[] response) {

            }
        }, true).getIdentifier();
        expected = new byte[]{(byte) 0x3C,(byte)0x00,(byte)0x00,(byte)0x0E,(byte)0x00,(byte)0x00,(byte)0x00 };
        result = DeviceCommandEvent.assembleCodes(
                DeviceCommandEvent.START_FRAME.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD_LENGTH.getAssembledCodes(),
                new byte[]{code.getIdentifierCode()},
                DeviceCommandEvent.COMPRESS.getAssembledCodes(),
                DeviceCommandEvent.PACKET_ID.getAssembledCodes(),
                DeviceCommandEvent.PAYLOAD.getAssembledCodes());
        assertArrayEquals(expected, result);
    }
}