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
    private volatile ArrayList<String> output;
    private Process haskellProcess;
    private boolean startedHaskellTesting;
    private boolean startedJavaTesting;
    private PrintStream cmdInput;
    private JavaCompiler compiler;
    private BufferedWriter haskellOutputWriter;
    private BufferedWriter javaOutputWriter;

    boolean startedHaskellTesting() {
        return this.startedHaskellTesting;
    }

    boolean startedJavaTesting() {
        return this.startedJavaTesting;
    }

    private Thread cmdOutput(InputStream stream) {
        output = new ArrayList<>();
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, "Cp866"))) {
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

    private Result haskellFailResult(String response, Task task) {
        logOutput(haskellOutputWriter, output);
        try {
            haskellOutputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        output.clear();
        return new Result(response, task);
    }

    void startHaskellTesting() {
        notInterrupted = true;
        try {
            haskellProcess = new ProcessBuilder("ghci").redirectErrorStream(true).start();
            cmdInput = new PrintStream(haskellProcess.getOutputStream());
            Thread haskellCmdThread = cmdOutput(haskellProcess.getInputStream());
            haskellCmdThread.start();
            while (true) { //On first launch ProcessBuilder takes a lot of time to execute first command.
                if (!output.isEmpty())
                    break;
                Thread.sleep(10);
            }
            if (output.get(0).startsWith("'ghci' is not"))
                throw new IOException();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Add ghci to your path before proceeding.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        output.clear();
        startedHaskellTesting = true;
    }

    Result handleHaskellTask(Task task) {
        String parentFolder = new File(task.getSourcePath()).getParent();
        File haskellOutput = new File(parentFolder + File.separator + task.getName().split("\\.")[0] + "Output.txt");
        try {
            haskellOutput.createNewFile();
            haskellOutputWriter = new BufferedWriter(new FileWriter(haskellOutput));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<Test> testContents = task.getTestContents();
        cmdInput.println(":l " + task.getSourcePath());
        try {
            haskellOutputWriter.write(":l " + task.getSourcePath());
            haskellOutputWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cmdInput.flush();
        int compilationTime = 0;
        while (true) {
            compilationTime++;
            if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Ok, modules loaded:"))
                break;
            else if (!output.isEmpty() && !(output.get(output.size() - 1).contains("[1 of 1]") || output.get(output.size() - 1).isEmpty()))
                return haskellFailResult("CE", task); //Compilation Error;
            if (compilationTime == 100) {
                finishHaskellTesting();
                startHaskellTesting();
                return haskellFailResult("SE", task); // System error — most likely an error on our end.
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            haskellOutputWriter.write("Starting testing process");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < testContents.size(); i++) {
            Test test = testContents.get(i);
            output.clear();
            String testCommand = test.getInput().get(0); //Haskell tasks do not support multi-line inputs.
            ArrayList<String> testOutputVariants = test.getOutputVariants().stream().map(v -> v.get(0)).collect(Collectors.toCollection(ArrayList::new));
            cmdInput.println(testCommand);
            try {
                haskellOutputWriter.write("Test № " + (i + 1) + ": " + testCommand);
                haskellOutputWriter.newLine();
                haskellOutputWriter.write("Expected:");
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            test.logOutputVariants(haskellOutputWriter);
            cmdInput.flush();
            int computationTime = 0;
            while (true) {
                if (output.size() > 0) break;
                if (computationTime >= test.getTime()) {
                    cmdInput.close();
                    finishHaskellTesting();
                    logOutput(haskellOutputWriter, output);
                    startHaskellTesting(); //Restart ghci if we encountered infinite input/ long computation.
                    return haskellFailResult("TL " + (i + 1), task); //Took too long to compute.
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                computationTime += 10;
            }
            try {
                haskellOutputWriter.write("Got:");
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (output.size() > 1 && output.get(1).startsWith("<interactive>"))
                return haskellFailResult("RE " + (i + 1), task);
            String response = output.get(0).split(" ", 2)[1]; // *>TaskName> Output
            try {
                haskellOutputWriter.write(response);
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!(testOutputVariants.contains("Error") && response.startsWith("*** Exception") && !response.startsWith("*** Exception: Prelude") // If exception is expected.
                    || testOutputVariants.contains(response)))
                return haskellFailResult("WA " + (i + 1), task);
        }
        try {
            haskellOutputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        output.clear();
        return new Result("OK", task);

    }

    void finishHaskellTesting() {
        Process taskKill;
        try {
            if (System.getProperty("os.name").startsWith("Windows"))
                taskKill = Runtime.getRuntime().exec("taskkill /F /IM ghc.exe");
            else
                taskKill = Runtime.getRuntime().exec("kill -9 ghc");
            taskKill.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        notInterrupted = false;
        haskellProcess.destroy();
        cmdInput.close();
    }

    void startJavaTesting() {
        compiler = ToolProvider.getSystemJavaCompiler();
        startedJavaTesting = true;
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

    Result handleJavaTask(Task task) {
        String parentFolder = new File(task.getSourcePath()).getParent();
        String taskName = task.getName().split("\\.")[0]; //Sum.java -> Sum
        File outputFile = new File(parentFolder + File.separator + taskName + "Output.txt");
        File errorFile = new File(parentFolder + File.separator + taskName + "Error.txt");
        removePackageStatementInJavaTasks(task);
        try {
            javaOutputWriter = new BufferedWriter(new FileWriter(outputFile));
            FileOutputStream errorStream = new FileOutputStream(outputFile);
            compiler.run(null, System.out, errorStream, task.getSourcePath());
            errorStream.close();
            BufferedReader br = new BufferedReader(new FileReader(outputFile));
            if (br.readLine() != null) {
                br.close();
                javaOutputWriter.close();
                return new Result("CE", task);
            }
            errorFile.createNewFile();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> compilationCommands = new ArrayList<>();
        compilationCommands.add("java");
        compilationCommands.add("-cp");
        compilationCommands.add(parentFolder);
        compilationCommands.add(taskName);
        ProcessBuilder pb = new ProcessBuilder(compilationCommands);
        File testInputFile = new File(parentFolder + File.separator + "testInput.txt");
        File testOutputFile = new File(parentFolder + File.separator + "testOutput.txt");
        try {
            testInputFile.createNewFile();
            testOutputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pb.redirectInput(testInputFile);
        pb.redirectOutput(testOutputFile);
        pb.redirectError(errorFile);
        ArrayList<Test> testContents = task.getTestContents();
        for (int i = 0; i < testContents.size(); i++) {
            Test test = testContents.get(i);
            try {
                javaOutputWriter.write("Test № " + (i + 1) + ":");
                javaOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(testInputFile)); //To clear input.
                FileWriter outputCleaner = new FileWriter(testOutputFile);
                outputCleaner.close(); //To clear output
                test.getInput().forEach(testLine -> {
                    try {
                        writer.write(testLine);
                        javaOutputWriter.write(testLine);
                        writer.newLine();
                        javaOutputWriter.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writer.close();
                Process javaProcess = pb.start();
                int computationTime = 0;
                javaOutputWriter.write("Expected:");
                javaOutputWriter.newLine();
                test.logOutputVariants(javaOutputWriter);
                while (javaProcess.isAlive()) {
                    if (computationTime >= test.getTime()) {
                        File jpsFile = new File(parentFolder + File.separator + taskName + "Jps.txt");
                        jpsFile.createNewFile();
                        ProcessBuilder jpsProcess = new ProcessBuilder("jps");
                        jpsProcess.redirectOutput(jpsFile).redirectError(jpsFile);
                        Process p = jpsProcess.start();
                        p.waitFor();
                        String pid = "";
                        List<String> processIds = Files.readAllLines(jpsFile.toPath()).stream().filter(a -> a.contains(taskName)).collect(Collectors.toList());
                        if (processIds.size() > 0)
                            pid = processIds.get(0).split(" ")[0];
                        Process taskKill;
                        if (System.getProperty("os.name").startsWith("Windows"))
                            taskKill = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                        else
                            taskKill = Runtime.getRuntime().exec("kill -9 " + pid);
                        taskKill.waitFor();
                        javaProcess.destroy();
                        jpsFile.delete();
                        return javaResult("TL " + (i + 1), task, testInputFile, testOutputFile, errorFile); //Time Limit.
                    }
                    Thread.sleep(10);
                    computationTime += 10;
                }
                javaOutputWriter.write("Got:");
                javaOutputWriter.newLine();
                if (errorFile.length() != 0) {
                    logOutput(javaOutputWriter, new ArrayList<>(Files.readAllLines(Paths.get(errorFile.getPath()))));
                    return javaResult("RE " + (i + 1), task, testInputFile, testOutputFile, errorFile); //Runtime Error
                }
                ArrayList<String> testOutput = new ArrayList<>(Files.readAllLines(Paths.get(testOutputFile.getPath())));
                logOutput(javaOutputWriter, testOutput);
                if (!test.getOutputVariants().contains(testOutput)) {
                    return javaResult("WA " + (i + 1), task, testInputFile, testOutputFile, errorFile);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return javaResult("OK", task, testInputFile, testOutputFile, errorFile);
    }

    private void logOutput(BufferedWriter writer, ArrayList<String> response) {
        response.forEach(line -> {
            try {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Result javaResult(String response, Task task, File inputFile, File outputFile, File errorFile) {
        try {
            javaOutputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        errorFile.delete();
        inputFile.delete();
        outputFile.delete();
        new File(task.getSourcePath().substring(0, task.getSourcePath().length() - 4) + "class").delete();
        return new Result(response, task);
    }

}

