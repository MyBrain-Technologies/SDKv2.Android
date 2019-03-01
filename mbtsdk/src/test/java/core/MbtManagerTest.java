package core;

import android.bluetooth.BluetoothManager;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;

import core.bluetooth.MbtBluetoothManager;
import core.device.MbtDeviceManager;
import core.device.model.MbtDevice;
import core.eeg.MbtEEGManager;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;

import static org.junit.Assert.*;

public class MbtManagerTest {

    Context context;
    MbtManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new MbtManager(context);
    }

    @Test
    public void constructor_AllUnitsEnabled() {
        if(BuildConfig.BUILD_TYPE.equals("unitTests")){
            assertTrue(BuildConfig.BLUETOOTH_ENABLED);
            assertEquals(1, manager.getRegisteredModuleManagers().size());
            ArrayList<BaseModuleManager> modulesRegistered = new ArrayList<>(manager.getRegisteredModuleManagers());
            assertTrue(modulesRegistered.get(0) instanceof MbtBluetoothManager);
        }else if(BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("release")){
            assertTrue(BuildConfig.BLUETOOTH_ENABLED);
            assertTrue(BuildConfig.DEVICE_ENABLED);
            assertTrue(BuildConfig.EEG_ENABLED);
            assertEquals(3, manager.getRegisteredModuleManagers().size());
            ArrayList<BaseModuleManager> modulesRegistered = new ArrayList<>(manager.getRegisteredModuleManagers());
            assertTrue(modulesRegistered.get(0) instanceof MbtBluetoothManager || modulesRegistered.get(1) instanceof MbtBluetoothManager || modulesRegistered.get(2) instanceof MbtBluetoothManager);
            assertTrue(modulesRegistered.get(0) instanceof MbtDeviceManager || modulesRegistered.get(1) instanceof MbtDeviceManager || modulesRegistered.get(2) instanceof MbtDeviceManager);
            assertTrue(modulesRegistered.get(0) instanceof MbtEEGManager || modulesRegistered.get(1) instanceof MbtEEGManager || modulesRegistered.get(2) instanceof MbtEEGManager);
        }
    }
}