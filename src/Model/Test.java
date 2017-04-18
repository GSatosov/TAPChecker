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

    public String getInput() {
        return this.input.get(0);
    }

    public ArrayList<String> getOutputVariants() {
        return this.outputVariants.stream().map(v -> v.get(0)).collect(Collectors.toCollection(ArrayList::new));
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

    public Test(ArrayList<String> input, ArrayList<ArrayList<String>> outputVariants, long time, String deadline, boolean antiPlagiarism) {
        this.input = input;
        this.outputVariants = outputVariants;
        this.time = time;
        this.antiPlagiarism = antiPlagiarism;
        this.deadline = deadline;
    }

    @Override
    public String toString() {
        return "{input: " + input + ", output: " + outputVariants + ", maximumOperatingTime: " + time + ", deadline: " + deadline + ", antiPlagiarism: " + antiPlagiarism + "}";
    }
}

