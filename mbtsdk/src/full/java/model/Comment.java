package model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by Etienne on 13/02/2017.
 */
@Keep
public class Comment implements Parcelable {

    @NonNull
    private String comment;
    @NonNull
    private long timestamp;
    @Nullable
    private String user;

    public Comment(@NonNull String comment, @NonNull long timestamp, @Nullable String user){
        this.comment = comment;
        this.timestamp = timestamp;
        this.user = user;
    }

    protected Comment(Parcel in) {
        comment = in.readString();
        timestamp = in.readLong();
        user = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(comment);
        dest.writeLong(timestamp);
        dest.writeString(user);
    }

    @SuppressWarnings("unused")
    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(@NonNull Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    @NonNull
    public String getComment(){
        return comment;
    }

    public long getTimestamp(){
        return timestamp;
    }

    @Nullable
    public String getUser(){
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Comment)) {
            return false;
        }
        Comment c = (Comment) o;
        if (timestamp != c.timestamp) {
            return false;
        }
        if (!TextUtils.equals(comment, c.comment) || !TextUtils.equals(user, c.user)) {
            return false;
        }
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return "Comment{" +
                "comment='" + comment + '\'' +
                ", timestamp=" + timestamp +
                ", user='" + user + '\'' +
                '}';
    }
}