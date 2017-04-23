package Controller;

import Model.Result;
import Model.Task;
import Model.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for applying tests. As of 3/14 only Haskell support is implemented. As of 4/18 Java support is implemented as well. TODO handle exceptions better
 */
class TestsApplier {
    private volatile boolean notInterrupted;
    private ArrayList<String> output;
    private Process haskellProcess;
    private PrintStream cmdInput;
    private JavaCompiler compiler;

    private void clearFolder(Task task, File inputFile, File outputFile) {
        inputFile.delete();
        outputFile.delete();
        new File(task.getSourcePath().substring(0, task.getSourcePath().length() - 4) + "class").delete();
    }

    private void removePackageStatementInJavaTasks(Task task) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(task.getSourcePath()));
            lines = lines.stream().filter(line -> !line.trim().startsWith("package")).collect(Collectors.toList());
            File sourceFile = new File(task.getSourcePath());
            FileWriter writer = new FileWriter(sourceFile);
            lines.forEach(line -> {
                try {
                    writer.write(line + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread cmdOutput(InputStream stream) {
        output = new ArrayList<>();
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(stream))) {
                String ch;
                while (notInterrupted) {
                    ch = r.readLine();
                    if (ch == null) continue;
                    output.add(ch);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private void startHaskellProcess() {
        notInterrupted = true;
        try {
            haskellProcess = new ProcessBuilder("ghci").redirectErrorStream(true).start();
            cmdInput = new PrintStream(haskellProcess.getOutputStream());
            cmdOutput(haskellProcess.getInputStream()).start();
            while (true) { //On first launch ProcessBuilder takes a lot of time to execute first command.
                if (!output.isEmpty())
                    break;
                Thread.sleep(10);
            }
            if (output.get(0).startsWith("'ghci' is not recognized as an internal or external command"))
                throw new IOException();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Add ghci to your path before proceeding.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Result handleHaskellTask(Task task) {
        output.clear();
        ArrayList<Test> testContents = task.getTestContents();
        cmdInput.println(":l " + task.getSourcePath());
        cmdInput.flush();
        int compilationTime = 0;
        while (true) {
            compilationTime++;
            if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Ok, modules loaded:"))
                break;
            if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Failed, modules loaded: none."))
                return new Result("CE", task); //Compilation Error
            if (compilationTime == 100) {
                notInterrupted = false;
                cmdInput.close();
                haskellProcess.destroy();
                startHaskellProcess();
                return new Result("TL", task); // Took too long to compile.
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int testingScore = 0;
        int maxScore = testContents.size();
        for (Test test : testContents) {
            int beforeTesting = output.size();
            String testCommand = test.getInput().get(0); //Haskell tasks do not support multi-line inputs.
            ArrayList<String> testOutputVariants = test.getOutputVariants().stream().map(v -> v.get(0)).collect(Collectors.toCollection(ArrayList::new));
            cmdInput.println(testCommand);
            cmdInput.flush();
            int computationTime = 0;
            while (true) {
                computationTime++;
                if (output.size() > beforeTesting) break;
                if (computationTime == test.getTime() * 100) {
                    cmdInput.close();
                    try {
                        Runtime.getRuntime().exec("taskkill /F /IM ghc.exe");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startHaskellProcess(); //Restart ghci if we encountered infinite input/ long computation.
                    return new Result("TL", task); //Took too long to compute.
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            String response = output.get(beforeTesting).split(" ", 2)[1]; // *>TaskName> Output
            if (testOutputVariants.contains("Error") && response.startsWith("*** Exception") // If exception is expected.
                    || testOutputVariants.contains(response))
                testingScore++;
        }
        String taskResult = testingScore + "/" + maxScore;
        return new Result(taskResult, task);
    }

    ArrayList<Result> applyHaskellTests(ArrayList<Task> tasks) {
        startHaskellProcess();
        ArrayList<Result> results = tasks.stream().map(this::handleHaskellTask).collect(Collectors.toCollection(ArrayList::new));
        notInterrupted = false;
        haskellProcess.destroy();
        cmdInput.close();
        return results;
    }

    private Result handleJavaTask(Task task) {
        String parentFolder = new File(task.getSourcePath()).getParent();
        File errorFile = new File(parentFolder + "\\error.txt");
        removePackageStatementInJavaTasks(task);
        try {
            FileOutputStream errorStream = new FileOutputStream(errorFile);
            compiler.run(null, System.out, errorStream, task.getSourcePath());
            errorStream.close();
            BufferedReader br = new BufferedReader(new FileReader(errorFile));
            if (br.readLine() != null) {
                return new Result("CE", task);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> compilationCommands = new ArrayList<>();
        compilationCommands.add("java");
        compilationCommands.add("-cp");
        compilationCommands.add(parentFolder);
        compilationCommands.add(String.valueOf(task.getName().split("\\.")[0])); //Sum.java -> Sum
        ProcessBuilder pb = new ProcessBuilder(compilationCommands);
        File inputFile = new File(parentFolder + "\\input.txt");
        File outputFile = new File(parentFolder + "\\output.txt");
        try {
            inputFile.createNewFile();
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pb.redirectInput(inputFile);
        pb.redirectOutput(outputFile);
        pb.redirectError(errorFile);
        ArrayList<Test> testContents = task.getTestContents();
        int maxScore = testContents.size();
        int curScore = 0;
        for (Test test : testContents) {
            try {
                int computationTime = 0;
                FileWriter writer = new FileWriter(inputFile); //To clear input.
                FileWriter outputCleaner = new FileWriter(outputFile);
                outputCleaner.close(); //To clear output
                BufferedReader reader = new BufferedReader(new FileReader(outputFile));
                test.getInput().forEach(testInput -> {
                    try {
                        writer.write(testInput);
                        writer.write("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writer.close();
                Process javaProcess = pb.start();
                while (javaProcess.isAlive()) {
                    if (computationTime == test.getTime() * 100)
                        return new Result("TL", task); //Time Limit.
                    Thread.sleep(10);
                    computationTime++;
                }
                if (errorFile.length() != 0) {
                    reader.close(); //Close inputFile
                    clearFolder(task, inputFile, outputFile);
                    return new Result("IH", task); //Input Handling Error
                }
                ArrayList<String> testOutput = new ArrayList<>();
                String curString = reader.readLine();
                while (curString != null) {
                    testOutput.add(curString);
                    curString = reader.readLine();
                }
                reader.close();
                if (test.getOutputVariants().contains(testOutput))
                    curScore++;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        clearFolder(task, inputFile, outputFile);
        errorFile.delete();
        String taskResult = curScore + "/" + maxScore;
        if (curScore < maxScore)
            return new Result(taskResult, task);
        return new Result(curScore + "/" + maxScore, task);
    }

    ArrayList<Result> applyJavaTests(ArrayList<Task> tasks) {
        compiler = ToolProvider.getSystemJavaCompiler();
        return tasks.stream().map(this::handleJavaTask).collect(Collectors.toCollection(ArrayList::new));
    }
}
