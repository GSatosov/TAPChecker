package Controller;

import Model.ExponentialBackOffFunction;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.util.List;

import static java.util.Arrays.asList;


final class ExponentialBackOff {
    private static final int[] FIBONACCI = new int[]{1, 1, 2, 3, 5, 8, 13, 21, 34};
    private static final List<Class<? extends Exception>> EXPECTED_COMMUNICATION_ERRORS = asList(
            SSLHandshakeException.class, SocketTimeoutException.class);

    private ExponentialBackOff() {

    }

    static <T> T execute(ExponentialBackOffFunction<T> fn) {
        return execute(fn, FIBONACCI.length);
    }

    private static <T> T execute(ExponentialBackOffFunction<T> fn, int times) {
        if (times > FIBONACCI.length) times = FIBONACCI.length;
        for (int attempt = 0; attempt < times; attempt++) {
            try {
                return fn.execute();
            } catch (Exception e) {
                handleFailure(attempt, e);
            }
        }
        throw new RuntimeException("Failed to execute a function.");
    }

    private static void handleFailure(int attempt, Exception e) {
        if (e.getCause() != null && !EXPECTED_COMMUNICATION_ERRORS.contains(e.getCause().getClass())) {
            System.out.println("Exponential BackOff: not handled exception.");
            throw new RuntimeException(e);
        }
        doWait(attempt);
    }

    private static void doWait(int attempt) {
        try {
            Thread.sleep(FIBONACCI[attempt] * 1000 + Math.round(Math.random() * FIBONACCI[attempt] * 500));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}