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


    public ArrayList<String> getInput() {
        return this.input;
    }

    public ArrayList<ArrayList<String>> getOutputVariants() {
        return this.outputVariants;
    }


    public Test(ArrayList<String> input, ArrayList<ArrayList<String>> outputVariants) {
        this.input = input;
        this.outputVariants = outputVariants;
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
        return "{input: " + this.input + ", output: " + this.outputVariants + "}";
    }
}

