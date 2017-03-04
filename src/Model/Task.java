package Model;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Task {
    private Subject subject;
    private String name;
    private File source;
    Task (String name,Subject subject, File source){
        this.name = name;
        this.subject = subject;
        this.source = source;
    }
}
