package Model;

/**
 * Created by Alexander Baranov on 27.04.2017.
 */

import javax.mail.MessagingException;
import java.io.IOException;
import java.text.ParseException;

@FunctionalInterface
public interface ExponentialBackOffFunction<T> {
    T execute() throws IOException, MessagingException, ParseException;
}
