package Model;

import java.io.InputStream;

/**
 * Created by Alexander Baranov on 03.03.2017.
 * <p>
 * Attachment model
 */
public class Attachment {
    private InputStream stream;
    private String fileName;

    public Attachment(String fileName, InputStream stream) {
        this.stream = stream;
        this.fileName = fileName;
    }

    public InputStream getStream() {
        return this.stream;
    }

    public String getFileName() {
        return this.fileName;
    }
}
