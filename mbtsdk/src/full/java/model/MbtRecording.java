package model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import core.eeg.storage.MbtEEGPacket;


/**
 * Created by manon on 10/10/16.
 */
public class MbtRecording implements Serializable{
    @Nullable
    private final RecordInfo recordInfo;
    private final long recordingTime;
    private int nbPackets;
    private int firstPacketID;
    private ArrayList<ArrayList<Float>> qualities;
    private ArrayList<ArrayList<Float>> eegData;
    @Nullable
    private ArrayList<Float> status;
    private List<MbtEEGPacket> packetArrayList;

    MbtRecording() {
        this.recordingTime = -1;
        this.recordInfo = null;
        this.nbPackets = -1;
        this.firstPacketID = -1;

        qualities = new ArrayList<>();
        for (int i = 0 ; i < 2 ; i++) {
            qualities.add(new ArrayList<Float>()) ;
        }
        eegData = new ArrayList<>();
        for (int i = 0 ; i < 2 ; i++) {
            eegData.add(new ArrayList<Float>()) ;
        }
        this.status = new ArrayList<>(1);
    }

    MbtRecording(@NonNull final RecordInfo recordInfo, @NonNull final long recordingTime, @NonNull int nbPackets,
                 @NonNull int firstPacketID, ArrayList<ArrayList<Float>> qualities,
                 ArrayList<ArrayList<Float>> eegData, @NonNull final ArrayList<Float> status) {
        this.recordingTime = recordingTime;
        this.recordInfo = recordInfo;
        this.nbPackets = nbPackets;
        this.firstPacketID = firstPacketID;
        this.qualities = qualities;
        this.eegData = eegData;
        this.status = status;
    }

    MbtRecording(@NonNull final RecordInfo recordInfo, @NonNull final long recordingTime,
                 @NonNull int nbPackets,
                 @NonNull int firstPacketID, ArrayList<ArrayList<Float>> qualities,
                 ArrayList<ArrayList<Float>> eegData) {
        this.recordingTime = recordingTime;
        this.recordInfo = recordInfo;
        this.nbPackets = nbPackets;
        this.firstPacketID = firstPacketID;
        this.qualities = qualities;
        this.eegData = eegData;
        this.status = new ArrayList<>(nbPackets);
        for (int i = 0 ; i < nbPackets ; i++)
            status.set(i, 0.0f) ;
    }

    public MbtRecording(@NonNull final RecordInfo recordInfo, @NonNull final long recordingTime,
                        @NonNull List<MbtEEGPacket> packetArrayList, @NonNull final boolean hasStatus) {
        this.recordingTime = recordingTime;
        this.recordInfo = recordInfo;
        this.firstPacketID = 0;
        this.packetArrayList = packetArrayList;
        this.nbPackets = packetArrayList.size();
        if(hasStatus)
            this.status = null;
        else
            this.status = new ArrayList<>();
        qualities = new ArrayList<>();
        eegData = new ArrayList<>();
        for (int i = 0 ; i < 2 ; i++) {
            qualities.add(new ArrayList<Float>()) ;
            eegData.add(new ArrayList<Float>()) ;
        }
        transformPacketArrayListToDataArrayList(hasStatus);
    }

    private void transformPacketArrayListToDataArrayList(boolean hasStatus) {
        if(this.packetArrayList != null && !this.packetArrayList.isEmpty()){

            //Making sure status list is correctly initialized
            if(hasStatus && status == null)
                status = new ArrayList<>();

            for (MbtEEGPacket MbtEEGPacket : packetArrayList) {

                if(MbtEEGPacket.getQualities() != null){
                    for (int i = 0; i < MbtEEGPacket.getQualities().size(); i++){
                        if(i >= qualities.size())
                            qualities.add(new ArrayList<Float>());
                        qualities.get(i).add(MbtEEGPacket.getQualities().get(i));
                    }
                }

//                qualities.get(0).add((float)MbtEEGPacket.getQualities()getQualityChannel1());
//                qualities.get(1).add((float)MbtEEGPacket.getQualityChannel2());
                if(hasStatus)
                    status.addAll(MbtEEGPacket.getStatusData());

                for(int i = 0; i < MbtEEGPacket.getChannelsData().size(); i++){
                    if(i >= eegData.size())
                        eegData.add(new ArrayList<Float>());
                    for (Float aFloat : MbtEEGPacket.getChannelsData().get(i)) {

                        eegData.get(i).add(aFloat);
                    }
                }

//                for(int i = 0; i < MbtEEGPacket.getChannelsData().size(); i++){
//                    if(i==0 && hasStatus){
//                        for (Float aFloat : MbtEEGPacket.getChannelsData().get(i)) {
//                            status.add(aFloat);
//                        }
//                    }else{
//                        if(i >= eegData.size())
//                            eegData.add(new ArrayList<Float>());
//                        for (Float aFloat : MbtEEGPacket.getChannelsData().get(i)) {
//
//                            eegData.get(i).add(aFloat);
//                        }
//                    }
//
//                }

//                for (double v : MbtEEGPacket.getChannel1()) {
//                    eegData.get(0).add((float)v);
//                }
//                for (double v : MbtEEGPacket.getChannel2()) {
//                    eegData.get(1).add((float)v);
//                }
            }
//            if(hasStatus){
//                eegData.remove(0);
//                qualities.remove(0);
//            }
        }
    }

    @NonNull
    public final RecordInfo getRecordInfos() {return recordInfo;}

    @NonNull
    public final long getRecordingTime() {return recordingTime;}

    @NonNull
    public final int getNbPackets() {return nbPackets;}

    @NonNull
    public final int getFirstPacketsId() {return firstPacketID;}

    @NonNull
    public final ArrayList<ArrayList<Float>> getQualities() {return qualities;}

    @NonNull
    public final ArrayList<ArrayList<Float>> getEegData() {return  eegData;}

    @NonNull
    public final ArrayList<Float> getStatus() {return status;}

    public void setNbPackets(@NonNull final int nbPackets) {this.nbPackets = nbPackets;}

    public void setFirstPacketID(@NonNull final int firstPacketID) {this.firstPacketID = firstPacketID;}

}
