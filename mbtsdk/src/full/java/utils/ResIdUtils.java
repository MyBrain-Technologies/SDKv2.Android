package utils;

import android.support.annotation.NonNull;

import java.lang.reflect.Field;

/**
 * Created by Etienne on 06/04/2017.
 */

public class ResIdUtils {
    public static int getResId(String resName, @NonNull Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
