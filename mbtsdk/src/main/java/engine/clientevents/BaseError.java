package engine.clientevents;

import androidx.annotation.Keep;

/**
 * Base class for client operation errors.
 *
 * This class aims at being extended by more operation-specific client requests.
 *
 * This will be used in {@link BaseErrorEvent} callbacks.
 */
@Keep
public abstract class BaseError extends RuntimeException{
    private String domain;
    private int code;
    private String message;

    BaseError(String domain, int code, String exception){
        super(exception);
        this.domain = domain;
        this.code = code;
        this.message = exception;
    }

    @Override
    public String toString(){
        return "domain: "+domain
                + ", code: "+code
                + ", message: "+message;
    }
    
}
