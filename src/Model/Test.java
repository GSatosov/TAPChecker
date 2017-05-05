package Model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

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

    public void logOutputVariants(BufferedWriter writer) {
        this.outputVariants.forEach(outputVariant -> {
            if (outputVariant.size() < 6) {
                outputVariant.subList(0, this.outputVariants.size() - 1).forEach(testOutputVariant -> {
                    try {
                        writer.write(testOutputVariant + " or,");
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    writer.write(outputVariant.get(outputVariant.size() - 1));
                    writer.newLine();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                outputVariant.subList(0, 5).forEach(testOutputVariant -> {
                    try {
                        writer.write(testOutputVariant + " or");
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        writer.write("...or " + (outputVariant.size() - 5) + " more.");
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public String toString() {
        return "{input: " + this.input + ", output: " + this.outputVariants + ", maximumOperatingTimeInMS: " + this.time +
                ", deadline: " + deadline + ", hasHardDeadline: " + hardDeadline + ", antiPlagiarism: " + antiPlagiarism + "}";
    }
}

