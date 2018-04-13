package core.device.acquisition;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;



/**
 *  Created by Sophie ZECRI on 06/04/2018
 *  Copyright (c) 2016 myBrain Technologies. All rights reserved.
 */

public class MbtDeviceAcquisition {

    private final String TAG = MbtDeviceAcquisition.class.getName();

    private int batteryReaderPeriod;

    private int currentBatteryLevel;

    private int currentSaturation;

    private int currentDCOffset;


    private DeviceAcquisitionListener deviceAcquisitionListener;

    public MbtDeviceAcquisition(){}

    public MbtDeviceAcquisition(int period) {
        this.batteryReaderPeriod = period;
    }

    public final void setDeviceAcquisitionListener(@Nullable final DeviceAcquisitionListener dataAcquisitionListener) {
        this.deviceAcquisitionListener = dataAcquisitionListener;
    }

    private void notifyDeviceIsReady(@NonNull final int level) {
        if (this.deviceAcquisitionListener != null)
            this.deviceAcquisitionListener.onDeviceReady(level);
    }

    public interface DeviceAcquisitionListener {
        @WorkerThread
        void onDeviceReady(@NonNull final int level);
    }

    private void readBattery(final boolean start) {

    }

    public void readSaturation(int newState) {

    }

    public void readDCOffset(int dcOffset) {

    }

    public int getBatteryReaderPeriod() {
        return batteryReaderPeriod;
    }

    public void setBatteryReaderPeriod(int batteryReaderPeriod) {
        this.batteryReaderPeriod = batteryReaderPeriod;
    }

    public int getLastKnownBatteryLevel() {
        return currentBatteryLevel;
    }

    public void setBatteryLevel(int currentBatteryLevel) {
        this.currentBatteryLevel = currentBatteryLevel;
    }

    public int getLastKnownSaturation() {
        return currentSaturation;
    }

    public void setSaturation(short currentSaturation) {
        this.currentSaturation = currentSaturation;
    }

    public int getLastKnownDCOffset() {
        return currentDCOffset;
    }

    public void setDCOffset(short currentDCOffset) {
        this.currentDCOffset = currentDCOffset;
    }
}

