package Model;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Tests implements Serializable {
    private Subject subject;
    private String functionToTest;
    private HashMap<String, ArrayList<String>> testContents;

    public Tests(Subject subject, String FunctionToTest) {
        this.functionToTest = FunctionToTest;
        this.subject = subject;
    }

    public Tests(Subject subject, String FunctionToTest, HashMap<String, ArrayList<String>> testContents) {
        this.functionToTest = FunctionToTest;
        this.subject = subject;
        this.testContents = testContents;
    }

    //TODO check if outputs are equal then add new variants if possible.
    private void addTests(String input, ArrayList<String> output) {
        if (!testContents.containsKey(input)) {
            testContents.put(input, output);
        }
    }

}
