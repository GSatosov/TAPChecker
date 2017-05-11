package Model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by GSatosov on 3/18/2017.
 */
public class Test {
    private ArrayList<String> input;
    private ArrayList<ArrayList<String>> outputVariants;
    private long time;
    private boolean antiPlagiarism;
    private Date deadline;

    public boolean hasHardDeadline() {
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

    public Date getDeadline() {
        return this.deadline;
    }

    public boolean getAntiPlagiarism() {
        return this.antiPlagiarism;
    }

    public Test(ArrayList<String> input, ArrayList<ArrayList<String>> outputVariants, long time, Date deadline, boolean antiPlagiarism, boolean hardDeadline) {
        this.input = input;
        this.outputVariants = outputVariants;
        this.time = time;
        this.antiPlagiarism = antiPlagiarism;
        this.deadline = deadline;
        this.hardDeadline = hardDeadline;
    }

    public void logOutputVariants(BufferedWriter writer) {
        this.outputVariants.subList(0, outputVariants.size() - 1).forEach(outputVariant -> {
            logList(writer, outputVariant);
            if (this.outputVariants.size() > 1)
                try {
                    writer.write("OR,");
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        });
        logList(writer, this.outputVariants.get(outputVariants.size() - 1));
    }

    public static void logList(BufferedWriter writer, ArrayList<String> list) {
        list.forEach(line -> {
            try {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public String toString() {
        return "{input: " + this.input + ", output: " + this.outputVariants + ", maximumOperatingTimeInMS: " + this.time +
                ", deadline: " + deadline + ", hasHardDeadline: " + hardDeadline + ", antiPlagiarism: " + antiPlagiarism + "}";
    }
}

