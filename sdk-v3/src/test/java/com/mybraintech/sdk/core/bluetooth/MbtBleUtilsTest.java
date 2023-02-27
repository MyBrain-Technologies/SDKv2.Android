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
    public boolean isQPlus;

    @Parameterized.Parameter(value = 2)
    public boolean isHyperion;

    @Parameterized.Parameters
    public static Collection<Object[]> initParameters() {
        boolean IS_Q_PLUS = true;
        boolean NOT_Q_PLUS = false;
        boolean IS_HYPERION = true;
        boolean NOT_HYPERION = false;

        return Arrays.asList(new Object[][]{
                {"qp_2220100000", IS_Q_PLUS, NOT_HYPERION},

                {"qp_2220100001", NOT_Q_PLUS, IS_HYPERION},
                {"mm_2220100001", NOT_Q_PLUS, NOT_HYPERION},

                {"qp_2220100002", NOT_Q_PLUS, IS_HYPERION},
                {"qp_2220100003", NOT_Q_PLUS, IS_HYPERION},

                {"qp_2220100004", IS_Q_PLUS, NOT_HYPERION},

                {"qp_9999123456", NOT_Q_PLUS, IS_HYPERION},
                {"mm_9999123456", NOT_Q_PLUS, NOT_HYPERION},

                {"qp_9999654321", NOT_Q_PLUS, IS_HYPERION},

                {"qp_9991234567", IS_Q_PLUS, NOT_HYPERION},
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
