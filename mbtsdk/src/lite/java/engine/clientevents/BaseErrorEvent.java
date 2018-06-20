package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public interface BaseErrorEvent<U extends BaseException> {

    /**
     * Method called when an error occured during process execution
     * @param exception
     */
    void onError(U exception);
}
