package com.mybraintech.sdk.core.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @see MbtBleUtils
 */
@RunWith(Parameterized.class)
public class MbtBleUtilsTest {

    @SuppressWarnings("DefaultAnnotationParam")
    @Parameterized.Parameter(value = 0)
    public String inputName;

    @Parameterized.Parameter(value = 1)
    public Boolean isQPlus;

    @Parameterized.Parameter(value = 2)
    public Boolean isHyperion;

    @Parameterized.Parameters
    public static Collection<Object[]> initParameters() {
        return Arrays.asList(new Object[][]{
                {"qp_2220100000", true, false},

                {"qp_2220100001", false, true},
                {"mm_2220100001", false, false},

                {"qp_2220100002", false, true},
                {"qp_2220100003", false, true},

                {"qp_2220100004", true, false},

                {"qp_9999123456", false, true},
                {"mm_9999123456", false, false},

                {"qp_9999654321", false, true},

                {"qp_9991234567", true, false},
        });
    }

    @Test
    public void test_isQPlus_isHyperion() {
        BluetoothDevice device = mock(BluetoothDevice.class);
        when(device.getName()).thenReturn(inputName);

        assertEquals(isQPlus, MbtBleUtils.INSTANCE.isQPlus(device));
        assertEquals(isHyperion, MbtBleUtils.INSTANCE.isHyperion(device));
    }
}
