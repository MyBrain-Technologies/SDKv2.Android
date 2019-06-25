package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public interface BaseErrorEvent<U extends BaseError> {

    /**
     * Method called when an error occured during process execution
     * @param error
     */
    void onError(U error, String additionalInfo);
}
