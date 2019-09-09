package config;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import core.recording.Comment;
import core.recording.metadata.MelomindExerciseSource;
import core.recording.metadata.RecordType;
import core.recording.metadata.MelomindExerciseType;
import model.RecordInfo;

/**
 * Recording configuration is all the data required to store the EEG packets in a JSON file.
 * The JSON file content and file name can be defined using this object.
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
@Keep
public final class RecordConfig {

    //CONFIG RELATED TO THE LOCATION OF THE JSON FILE

    /**
     * Path of the folder where the JSON file that contains the recording can be stored
     * Default folder used is the root folder.
     */
    private String folder;

    /**
     * Boolean flag used to define if the JSON file has to be stored in the folder external to the application folder.
     * An external folder is located at the root of your Android device internal storage.
     * An internal folder is located in the Internal storage>Android>data folder.
     * Default value is false.
     */
    private boolean useExternalStorage;


    //CONFIG RELATED TO THE NAME OF THE JSON FILE

    /**
     * Name of the JSON file (format excluded) where the recording is stored.
     * Default name looks like yyyy-MM-dd_HH:mm:ss.SSS-projectname-devicename-subjectid-condition
     * Where :
     * yyyy-MM-dd_HH:mm:ss.SSS is the recording starting timestamp
     * devicename is the name of the EEG headset used to acquire the EEG
     * projectname is your application name defined in the Manifest file for the attribute android:name
     * subjectid is the identifier of the person who's EEG is recorded with the headset
     * condition is an additional information that provide more details of the recording condition
     */

    private String filename;

    /**
     * Name of your Android application
     * Default value is the name defined in your Manifest file for the attribute android:name
     */
    private String projectName;

    /**
     * Additional information that provide more details of the recording condition
     * Default value is "--".
     */
    private String condition;

    ///CONFIG RELATED TO THE NAME & CONTENT OF THE JSON FILE

    /**
     * Identifier of the person who's EEG is recorded.
     * Default value is anonymous.
     */
    private String subjectId;

    /**
     * Starting date and time of the recording
     * formatted as the following example: "yyyy-MM-dd_HH:mm:ss.SSS".
     */
    private long timestamp;

    ///CONFIG RELATED TO THE CONTENT OF THE JSON FILE

    /**
     * Optional Additional information stored in the header of the JSON file.
     */
    private ArrayList<Comment> headerComments;

    /**
     * Record information that holds the type & source of the EEG data, and the used signal processing algorithms version.
     * Default values are :
     * {@link RecordType#RAWDATA} for the record type
     * {@link MelomindExerciseType#DEFAULT} for the exercice type
     * {@link MelomindExerciseSource#DEFAULT} for the source
     */
    private RecordInfo recordInfo;

    /**
     * Recording parameters are additional optional data stored in the body of the JSON file.
     */
    private Bundle recordingParameters;

    private RecordConfig(String folder,
                         String filename,
                         String subjectId,
                         RecordInfo recordInfo,
                         String condition,
                         String projectName,
                         boolean useExternalStorage,
                         long timestamp,
                         ArrayList<Comment> comments,
                         Bundle recordingParameters){

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

    /**
     * Path of the folder where the JSON file that contains the recording can be stored.
     * Default folder used is the root folder.
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Name of the JSON file (format excluded) where the recording is stored.
     * Default name looks like yyyy-MM-dd_HH:mm:ss.SSS-projectname-devicename-subjectid-condition
     * Where :
     * yyyy-MM-dd_HH:mm:ss.SSS is the recording starting timestamp
     * devicename is the name of the EEG headset used to acquire the EEG
     * projectname is your application name defined in the Manifest file for the attribute android:name
     * subjectid is the identifier of the person who's EEG is recorded with the headset
     * condition is an additional information that provide more details of the recording condition
     */
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Boolean flag used to define if the JSON file has to be stored in the folder external to the application folder.
     * An external folder is located at the root of your Android device internal storage.
     * An internal folder is located in the Internal storage>Android>data folder.
     * Default value is false.
     */
    public boolean useExternalStorage() {
        return useExternalStorage;
    }

    /**
     * Return the identifier of the person who's EEG is recorded.
     * Default value is anonymous.
     */
    public String getSubjectId() {
        return subjectId;
    }

    /**
     * Optional Additional information stored in the header of the JSON file.
     */
    public ArrayList<Comment> getHeaderComments() {
        return headerComments;
    }

    /**
     * Starting date and time of the recording
     * formatted as the following example: "yyyy-MM-dd_HH:mm:ss.SSS".
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Record information that holds the type & source of the EEG data, and the used signal processing algorithms version.
     * Default values are :
     * {@link RecordType#RAWDATA} for the record type
     * {@link MelomindExerciseType#DEFAULT} for the exercice type
     * {@link MelomindExerciseSource#DEFAULT} for the source
     */
    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    /**
     * Name of your Android application
     * Default value is the name defined in your Manifest file for the attribute android:name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Additional information that provide more details of the recording condition
     * Default value is "--".
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Recording parameters are additional optional data stored in the body of the JSON file.
     */
    public Bundle getRecordingParameters() {
        return recordingParameters;
    }

    /**
     * Builder class to ease construction of the {@link RecordConfig} instance.
     */
    @Keep
    public static class Builder{

        /**
         * Path of the folder where the JSON file that contains the recording can be stored
         * Default folder used is the root folder.
         */
        private String folder = "";
        /**
         * Name of the JSON file (format excluded) where the recording is stored.
         * Default name looks like yyyy-MM-dd_HH:mm:ss.SSS-projectname-devicename-subjectid-condition
         * Where :
         * yyyy-MM-dd_HH:mm:ss.SSS is the recording starting timestamp
         * devicename is the name of the EEG headset used to acquire the EEG
         * projectname is your application name defined in the Manifest file for the attribute android:name
         * subjectid is the identifier of the person who's EEG is recorded with the headset
         * condition is an additional information that provide more details of the recording condition
         */
        private String filename;
        /**
         * Boolean flag used to define if the JSON file has to be stored in the folder external to the application folder.
         * An external folder is located at the root of your Android device internal storage.
         * An internal folder is located in the Internal storage>Android>data folder.
         * Default value is false.
         */
        private boolean useExternalStorage = false;
        /**
         * Starting date and time of the recording
         * formatted as the following example: "yyyy-MM-dd_HH:mm:ss.SSS".
         */
        private long timestamp;
        /**
         * Name of your Android application
         * Default value is the name defined in your Manifest file for the attribute android:name
         */
        private String projectName;

        /**
         * Identifier of the person who's EEG is recorded.
         * Default value is anonymous.
         */
        private String subjectID = "anonymous";
        /**
         * Additional information that provide more details of the recording condition
         * Default value is "--".
         */
        private String condition = "--";

        /**
         * Optional Additional information stored in the header of the JSON file.
         */
        private ArrayList<Comment> headerComments;
        /**
         * Record information that holds the type & source of the EEG data, and the used signal processing algorithms version.
         * Default values are :
         * {@link RecordType#RAWDATA} for the record type
         * {@link MelomindExerciseType#DEFAULT} for the exercice type
         * {@link MelomindExerciseSource#DEFAULT} for the source
         */
        private RecordInfo recordInfo;
        /**
         * Recording parameters are additional optional data stored in the body of the JSON file.
         */
        private Bundle recordingParameters;

        public Builder(Context context){
            this.timestamp = System.currentTimeMillis();
            this.projectName = context.getString(context.getApplicationInfo().labelRes);
            this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
        }

        /**
         * Path of the folder where the JSON file can be stored.
         * Default folder used is the root folder.
         */
        public Builder folder(@Nullable String folder){
            this.folder = folder;
            return this;
        }

        /**
         * Name of the JSON file (format excluded) where the recording is stored.
         * Default name looks like yyyy-MM-dd_HH:mm:ss.SSS-projectname-devicename-subjectid-condition
         * Where :
         * yyyy-MM-dd_HH:mm:ss.SSS is the recording starting timestamp
         * devicename is the name of the EEG headset used to acquire the EEG
         * projectname is your application name defined in the Manifest file for the attribute android:name
         * subjectid is the identifier of the person who's EEG is recorded with the headset
         * condition is an additional information that provide more details of the recording condition
         */
        public Builder filename(@Nullable String filename){
            this.filename = filename;
            return this;
        }

        /**
         * Optional Additional information stored in the header of the JSON file.
         */
        public Builder headerComments(@Nullable ArrayList<Comment> comments) {
            this.headerComments = comments;
            return this;
        }

        /**
         * Optional Additional information stored in the header of the JSON file.
         */
        public Builder headerComment(@Nullable Comment... comment) {
            if(this.headerComments == null)
                this.headerComments = new ArrayList<>(Arrays.asList(comment));
            else
                this.headerComments.addAll(Arrays.asList(comment));
            return this;
        }

        /**
         * Additional information that provide more details of the recording condition
         * Default value is "--".
         */
        public Builder condition(@Nullable String condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Identifier of the person who's EEG is recorded.
         * Default value is anonymous.
         */
        public Builder subjectID(@Nullable String subjectID) {
            this.subjectID = subjectID;
            return this;
        }

        /**
         * Define that the JSON file has to be stored in the folder external to the application folder.
         * An external folder is located at the root of your Android device internal storage.
         * An internal folder is located in the Internal storage>Android>data folder.
         * Default folder used is the internal folder.
         */
        public Builder useExternalStorage() {
            this.useExternalStorage = true;
            return this;
        }

        /**
         * Define that the JSON file has to be stored in the internal application folder.
         * An external folder is located at the root of your Android device internal storage.
         * An internal folder is located in the Internal storage>Android>data folder.
         * Default folder used is the internal folder.
         */
        public Builder useInternalStorage() {
            this.useExternalStorage = false;
            return this;
        }

        /**
         * Starting date and time of the recording
         * formatted as the following example: "yyyy-MM-dd_HH:mm:ss.SSS".
         */
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Name of your Android application
         * Default value is the name defined in your Manifest file for the attribute android:name
         */
        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        /**
         * Exercise type is the Melomind type of neurofeedback exercise
         * Default value is {@link MelomindExerciseType#DEFAULT}.
         */
        public Builder exerciceType(MelomindExerciseType exerciseType) {
            if(this.recordInfo == null)
                this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
            this.recordInfo.getRecordingType().setExerciseType(exerciseType);
            return this;
        }

        /**
         * Source is the Melomind type of program exercise
         * Default value is {@link MelomindExerciseSource#DEFAULT}.
         */
        public Builder source(MelomindExerciseSource source) {
            if(this.recordInfo == null)
                this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
            this.recordInfo.getRecordingType().setSource(source);
            return this;
        }

        /**
         * Record type is the type of task performed by the subject who's EEG is recorded
         * Default value is {@link RecordType#RAWDATA}.
         */
        public Builder recordType(RecordType recordType) {
            if(this.recordInfo == null)
                this.recordInfo = new RecordInfo(UUID.randomUUID().toString());
            this.recordInfo.getRecordingType().setRecordType(recordType);
            return this;
        }

        /**
         * Recording parameters are additional optional data stored in the body of the JSON file.
         */
        public Builder bodyParameters(@Nullable Bundle recordingParameters){
            this.recordingParameters = recordingParameters;
            return this;
        }


        @Nullable
        public RecordConfig create(){
            return new RecordConfig(
                    folder,
                    filename,
                    subjectID,
                    recordInfo,
                    condition,
                    projectName,
                    useExternalStorage,
                    timestamp,
                    headerComments,
                    recordingParameters);
        }
    }

    @Override
    public String toString() {
        return "RecordConfig{" +
                "folder='" + folder + '\'' +
                ", useExternalStorage=" + useExternalStorage +
                ", filename='" + filename + '\'' +
                ", projectName='" + projectName + '\'' +
                ", condition='" + condition + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", timestamp=" + timestamp +
                ", headerComments=" + headerComments +
                ", recordInfo=" + recordInfo +
                ", recordingParameters=" + recordingParameters +
                '}';
    }
}
