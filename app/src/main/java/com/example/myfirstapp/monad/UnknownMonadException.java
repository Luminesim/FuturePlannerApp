package com.example.myfirstapp.monad;

/**
 * Indicates a problem setting up a Monad.
 */
public class UnknownMonadException extends RuntimeException {
    public UnknownMonadException(String msg, Throwable t) {
        super(msg, t);
    }
    public UnknownMonadException(Throwable t) {
        super(t);
    }
    public UnknownMonadException(String msg) {
        super(msg);
    }
}
