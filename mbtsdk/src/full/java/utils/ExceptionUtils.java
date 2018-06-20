package utils;

/**
 * Created by Vincent on 31/07/2015.
 */
public final class ExceptionUtils {
    /**
     * To throw an <code>IllegalArgumentException</code> when a variable is <code>null</code>
     */
    public final static class NullArg extends IllegalArgumentException {
        public NullArg(final String variable) {
            super(variable + " must NOT be NULL!)");
        }
    }

    /**
     * To thrown an <code>IllegalArgumentException</code> when a variable is <code>empty</code>
     */
    public final static class EmptyArg extends IllegalArgumentException {
        public EmptyArg(final String variable) {
            super(variable + " must NOT be EMPTY!)");
        }
    }

    /**
     * To throw an <code>IllegalArgumentException</code> when a variable is <code>null</code>
     * and/or <code>empty</code>
     */
    public final static class NullEmptyArg extends IllegalArgumentException {
        public NullEmptyArg(final String variable) {
            super(variable + " must NOT be NULL or EMPTY!");
        }
    }

    /**
     * To throw an <code>IllegalArgumentException</code> when a variable is negative
     */
    public final static class NegativeArg extends IllegalArgumentException {
        public NegativeArg(final String variable) {
            super(variable + " must NOT be NEGATIVE!");
        }
    }


}
