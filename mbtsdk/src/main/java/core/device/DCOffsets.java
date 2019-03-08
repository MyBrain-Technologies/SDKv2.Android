package core.device;


import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by SophieZ on 28/02/2018.
 */
import android.support.annotation.Keep;

@Keep
public final class DCOffsets implements Serializable {

    private long timestamp;

    private float[] offset;

    public DCOffsets(long timestamp, float[] offset) {
        this.timestamp = timestamp;
        this.offset = offset;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float[] getOffset() {
        return offset;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setOffset(float[] offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "DCOffsets{" +
                "timestamp=" + timestamp +
                ", offset=" + Arrays.toString(offset) +
                '}';
    }
}
