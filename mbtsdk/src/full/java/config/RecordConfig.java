package config;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import core.recording.Comment;
import core.recording.metadata.MelomindExerciceSource;
import core.recording.metadata.RecordType;
import core.recording.metadata.MelomindExerciseType;
import model.RecordInfo;

/**
 * This class aims at configuring the stream process. It contains user configurable
 * parameters to specify how the streaming is going to be.
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
@Keep
public final class RecordConfig {

    private String folder;

    private String filename;

    private String subjectId;

    private ArrayList<Comment> headerComments;

    private boolean useExternalStorage;

    private long timestamp;

    private RecordInfo recordInfo;

    private String condition;

    private String projectName;

    private Bundle recordingParameters;

    private RecordConfig(String folder, String filename, String subjectId, RecordInfo recordInfo, String condition, String projectName, boolean useExternalStorage, long timestamp, ArrayList<Comment> comments, Bundle recordingParameters){
        this.folder = folder;
        this.filename = filename;
        this.subjectId = subjectId;
        this.timestamp = timestamp;
        this.useExternalStorage = useExternalStorage;
        this.recordInfo = recordInfo;
        this.headerComments = comments;
        this.condition = condition;
        this.projectName = projectName;
        this.recordingParameters = recordingParameters;
    }

    public String getFolder() {
        return folder;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public ArrayList<Comment> getHeaderComments() {
        return headerComments;
    }

    public boolean useExternalStorage() {
        return useExternalStorage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getCondition() {
        return condition;
    }

    public Bundle getRecordingParameters() {
        return recordingParameters;
    }

    /**
     * Builder class to ease construction of the {@link RecordConfig} instance.
     */
    @Keep
    public static class Builder{

        private String folder = "";
        private long timestamp;
        private String projectName;
        private String subjectID = "";
        private String condition = "";
        private String filename;
        private ArrayList<Comment> headerComments;
        private boolean useExternalStorage = false;
        private RecordInfo recordInfo;
        private Bundle recordingParameters;

        public Builder(Context context){
            this.timestamp = System.currentTimeMillis();
            this.projectName = context.getApplicationInfo().name;
            this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
        }

        public Builder folder(@Nullable String folder){
            this.folder = folder;
            return this;
        }
        public Builder filename(@Nullable String filename){
            this.filename = filename;
            return this;
        }

        public Builder headerComments(@Nullable ArrayList<Comment> comments) {
            this.headerComments = comments;
            return this;
        }

        public Builder headerComment(@Nullable Comment... comment) {
            if(this.headerComments == null)
                this.headerComments = new ArrayList<>(Arrays.asList(comment));
            else
                this.headerComments.addAll(Arrays.asList(comment));
            return this;
        }

        public Builder condition(@Nullable String condition) {
            this.condition = condition;
            return this;
        }

        public Builder subjectID(@Nullable String subjectID) {
            this.subjectID = subjectID;
            return this;
        }

        public Builder useExternalStorage() {
            this.useExternalStorage = true;
            return this;
        }

        public Builder useInternalStorage() {
            this.useExternalStorage = false;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder exerciceTyoe(MelomindExerciseType exerciseType) {
            if(this.recordInfo == null)
                this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
            this.recordInfo.getRecordingType().setExerciseType(exerciseType);
            return this;
        }

        public Builder source(MelomindExerciceSource source) {
            if(this.recordInfo == null)
                this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
            this.recordInfo.getRecordingType().setSource(source);
            return this;
        }

        public Builder recordType(RecordType recordType) {
            if(this.recordInfo == null)
                this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
            this.recordInfo.getRecordingType().setRecordType(recordType);
            return this;
        }

        public Builder parameters(@Nullable Bundle recordingParameters){
            this.recordingParameters = recordingParameters;
            return this;
        }


        @Nullable
        public RecordConfig create(){
            return new RecordConfig(folder, filename, subjectID, recordInfo, condition, projectName, useExternalStorage, timestamp, headerComments, recordingParameters);
        }
    }

}
