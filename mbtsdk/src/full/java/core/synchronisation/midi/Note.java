package core.synchronisation.midi;

import android.support.annotation.Keep;

@Keep
public enum Note {
    A,
    B,
    C,
    D,
    E,
    F,
    G;

    public static Note convertIntegerToNote(int noteAsInteger){
        for (Note value : Note.values()){
            if(value.ordinal() == noteAsInteger)
                return value;
        }
        return null;
    }
}
