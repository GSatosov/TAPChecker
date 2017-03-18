package Model;

import java.util.ArrayList;

/**
 * Created by GSatosov on 3/18/2017.
 */
public class Test {
    private String input;
    private ArrayList<String> outputVariants;
    private long time;

    public String getInput() {
        return input;
    }

    public ArrayList<String> getOutputVariants() {
        return outputVariants;
    }

    public long getTime() {
        return time;
    }

    public Test(String input, ArrayList<String> outputVariants, long time) {
        this.input = input;
        this.outputVariants = outputVariants;
        this.time = time;
    }

    public Test(String input, ArrayList<String> outputVariants) {
        this.input = input;
        this.outputVariants = outputVariants;
    }

    @Override
    public String toString() {
        return input + " " + outputVariants;
    }
}

