package ch.yanova.kolibri;

/**
 * Created by lekov on 5/7/17.
 */

public class KolibriException extends RuntimeException {
    public KolibriException() {
    }

    public KolibriException(String message) {
        super(message);
    }

    public KolibriException(String message, Throwable cause) {
        super(message, cause);
    }

    public KolibriException(Throwable cause) {
        super(cause);
    }
}
