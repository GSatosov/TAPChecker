package Model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Support for multiple-output tests added.
 */
public class Tests {
    private Task task;
    private HashMap<String, ArrayList<String>> testContents;

    public HashMap<String, ArrayList<String>> getTestContents() {
        return testContents;
    }

    public Task getTask() {
        return task;
    }

    public Tests(Task task, HashMap<String, ArrayList<String>> testContents) {
        this.task = task;
        this.testContents = testContents;
    }
}
