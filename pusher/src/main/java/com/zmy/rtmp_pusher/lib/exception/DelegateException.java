package com.zmy.rtmp_pusher.lib.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.PublicKey;

public class DelegateException extends Exception {
    private Exception exception;

    public DelegateException(Exception exception) {
        this.exception = getDelegate(exception);
    }

    public static Exception getDelegate(Exception e) {
        if (!(e instanceof DelegateException)) {
            return e;
        } else {
            return getDelegate(((DelegateException) e).exception);
        }
    }

    @Nullable
    @Override
    public String getMessage() {
        return exception.getMessage();
    }

    @Nullable
    @Override
    public String getLocalizedMessage() {
        return exception.getLocalizedMessage();
    }

    @Nullable
    @Override
    public Throwable getCause() {
        return exception.getCause();
    }

    @NonNull
    @Override
    public Throwable initCause(@Nullable Throwable cause) {
        return exception.initCause(cause);
    }

    @NonNull
    @Override
    public String toString() {
        return exception.toString();
    }

    @Override
    public void printStackTrace() {
        exception.printStackTrace();
    }

    @Override
    public void printStackTrace(@NonNull PrintStream s) {
        exception.printStackTrace(s);
    }

    @Override
    public void printStackTrace(@NonNull PrintWriter s) {
        exception.printStackTrace(s);
    }

    @NonNull
    @Override
    public Throwable fillInStackTrace() {
        return exception.fillInStackTrace();
    }

    @NonNull
    @Override
    public StackTraceElement[] getStackTrace() {
        return exception.getStackTrace();
    }

    @Override
    public void setStackTrace(@NonNull StackTraceElement[] stackTrace) {
        exception.setStackTrace(stackTrace);
    }
}
