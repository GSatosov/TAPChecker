package Controller;

import Model.ExponentialBackOffFunction;
import View.MainController;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Arrays.asList;


final class ExponentialBackOff {
    private static final int delay = 1000;

    private static final List<Class<? extends Exception>> EXPECTED_COMMUNICATION_ERRORS = asList(
            SSLHandshakeException.class, SocketTimeoutException.class);

    private ExponentialBackOff() {

    }

    static <T> void execute(ExponentialBackOffFunction<T> fn) {
        for (int attempt = 0; attempt < 16; attempt++) {
            try {
                fn.execute();
                return;
            } catch (Exception e) {
                handleFailure(attempt, e);
            }
        }
        throw new RuntimeException("Failed to execute a function!");
    }

    private static void handleFailure(int attempt, Exception e) {
        if (e.getCause() != null && !EXPECTED_COMMUNICATION_ERRORS.contains(e.getCause().getClass())) {
            MainController.println("Exponential BackOff: not handled exception!");
            throw new RuntimeException(e);
        }
        doWait(attempt);
    }

    private static void doWait(int attempt) {
        try {
            int timeSlot = ThreadLocalRandom.current().nextInt(0, (int) Math.pow(2, attempt));
            int randomShift = ThreadLocalRandom.current().nextInt(0, timeSlot * delay / 10 + 1);
            Thread.sleep(timeSlot * delay + randomShift);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}