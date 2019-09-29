package engine.clientevents;

import android.support.annotation.Keep;

/**
 * Created by Etienne on 08/02/2018.
 */

@Keep
public interface MbtClientEvents<E extends BaseError> extends BaseErrorEvent<E> {}
