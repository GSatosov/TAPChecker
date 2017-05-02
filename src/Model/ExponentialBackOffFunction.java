package Model;

/**
 * Created by Alexander Baranov on 27.04.2017.
 */
import javax.mail.MessagingException;
import java.io.IOException;
import java.rmi.RemoteException;

@FunctionalInterface
public interface ExponentialBackOffFunction<T> {
    T execute() throws IOException, MessagingException;
}
