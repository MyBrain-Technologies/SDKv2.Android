package engine.clientevents;

import androidx.annotation.Keep;

/**
 * Base class for client operation exceptions.
 *
 * This class aims at being extended by more operation-specific client requests.
 *
 * This will be used in {@link BaseErrorEvent} callbacks.
 */
@Keep
public abstract class BaseException extends RuntimeException{
    private String exception;

    public BaseException(String exception){
        super(exception);
        this.exception = exception;

    }

    @Override
    public String toString(){
        return exception;
    }

}
