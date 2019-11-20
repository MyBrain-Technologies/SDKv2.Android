package model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import core.eeg.storage.MbtEEGPacket;
import utils.MatrixUtils;


/**
 * Created by manon on 10/10/16.
 */
public class MbtRecording implements Serializable{

    @Nullable
    private RecordInfo recordInfo;
    private long recordingTime;
    private int nbPackets;
    private int firstPacketID;
    private ArrayList<ArrayList<Float>> qualities;
    private ArrayList<ArrayList<Float>> eegData;
    @Nullable
    private ArrayList<Float> status;

    public MbtRecording(@NonNull int nbChannels,
                        @NonNull final RecordInfo recordInfo,
                        @NonNull final long recordingTime,
                        @NonNull List<MbtEEGPacket> eegPackets,
                        @NonNull final boolean hasStatus) {

        if(eegPackets == null)
            return;

        this.recordingTime = recordingTime;
        this.recordInfo = recordInfo;
        this.firstPacketID = 0;
        this.nbPackets = eegPackets.size();

        this.eegData = new ArrayList<>();
        this.qualities = new ArrayList<>();
        for (int i = 0 ; i < nbChannels ; i++) {
            eegData.add(new ArrayList<>()) ;
            qualities.add(new ArrayList<>()) ;
        }
        if(hasStatus)
            this.status = new ArrayList<>();

        assemblePackets(hasStatus, eegPackets);
    }


    private void assemblePackets(boolean hasStatus, List<MbtEEGPacket> eegPackets){
        if(eegPackets != null && !eegPackets.isEmpty()) {

            for (MbtEEGPacket eegPacket : eegPackets) {

                //RAW EEG
                if(eegPacket.getChannelsData().size() != eegData.size() && //if matrix is inverted
                        eegPacket.getChannelsData().get(0).size() == eegData.size()) // if nb line of the input matrix = nb of column the output matrix
                    eegPacket.setChannelsData(MatrixUtils.invertFloatMatrix(eegPacket.getChannelsData()));

                for(int eegIndex = 0; eegIndex < eegPacket.getChannelsData().size(); eegIndex++){
//                    if(eegIndex >= eegData.size())
//                        eegData.add(new ArrayList<>());
                    for (Float rawEEG : eegPacket.getChannelsData().get(eegIndex)) {
                        eegData.get(eegIndex).add(rawEEG);
                    }
                }

                //QUALITIES
                if(eegPacket.getQualities() != null){
                    for (int qualityIndex = 0; qualityIndex < eegPacket.getQualities().size(); qualityIndex++){
                        if(qualityIndex >= qualities.size())
                            qualities.add(new ArrayList<>());
                        qualities.get(qualityIndex).add(eegPacket.getQualities().get(qualityIndex));
                    }
                }

                //STATUS
                if (eegPacket.getStatusData() != null && hasStatus)
                    status.addAll(eegPacket.getStatusData());

            }
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

    @Override
    public String toString() {
        return "MbtRecording{" +
                "recordInfo=" + recordInfo +
                ", recordingTime=" + recordingTime +
                ", nbPackets=" + nbPackets +
                ", firstPacketID=" + firstPacketID +
                ", qualities=" + (qualities != null ? (qualities.size()+"x"+qualities.get(0).size()) : "null")+
                ", eegData=" + (eegData != null && eegData.size() > 0 ? eegData.size()+"x"+eegData.get(0).size() : eegData)+
                ", status=" + (status != null ? status.size() : "null") +
                '}';
    }
}
