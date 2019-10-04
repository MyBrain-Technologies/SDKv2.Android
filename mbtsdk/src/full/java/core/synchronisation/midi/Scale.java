package core.synchronisation.midi;


import android.support.annotation.Keep;

/**
 * How to play like Mozart
 */
@Keep
public enum Scale{

    MAJOR("major", new int[]{0, 2, 2, 1, 2, 2, 2, 1}),
    NATURAL_MINOR("natural minor", new int[]{0, 2, 1, 2, 2, 1, 2, 2});

    private String name;
    private int[] intervals;

    Scale(String name, int[] intervals) {
        this.name = name;
        this.intervals = intervals;
    }

    public int[] getIntervals() {
        return this.intervals;
    }

    public String getName() { return this.name;}

    @Override public String toString() {
        return this.name;
    }
}