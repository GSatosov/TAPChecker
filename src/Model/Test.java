package Model;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Created by GSatosov on 3/18/2017.
 */
public class Test {
    private ArrayList<String> input;
    private ArrayList<ArrayList<String>> outputVariants;
    private long time;
    private boolean antiPlagiarism;
    private String deadline;

    public boolean isHardDeadline() {
        return this.hardDeadline;
    }

    private boolean hardDeadline;

    public ArrayList<String> getInput() {
        return this.input;
    }

    public ArrayList<ArrayList<String>> getOutputVariants() {
        return this.outputVariants;
    }

    public long getTime() {
        return this.time;
    }

    public String getDeadline() {
        return this.deadline;
    }

    public boolean getAntiPlagiarism() {
        return this.antiPlagiarism;
    }

    public Test(ArrayList<String> input, ArrayList<ArrayList<String>> outputVariants, long time, String deadline, boolean antiPlagiarism, boolean hardDeadline) {
        this.input = input;
        this.outputVariants = outputVariants;
        this.time = time;
        this.antiPlagiarism = antiPlagiarism;
        this.deadline = deadline;
        this.hardDeadline = hardDeadline;
    }

    @Override
    public String toString() {
        return "{input: " + this.input + ", output: " + this.outputVariants + ", maximumOperatingTime: " + this.time +
                ", deadline: " + deadline + ", hasHardDeadline: " + hardDeadline + ", antiPlagiarism: " + antiPlagiarism + "}";
    }
}

