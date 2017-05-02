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
    private volatile boolean startedGhci;
    private boolean sentHaskellTasks;
    private boolean sentJavaTasks;
    private PrintStream cmdInput;
    private JavaCompiler compiler;
    private BufferedWriter haskellOutputWriter;

    boolean sentHaskellTasks() {
        return sentHaskellTasks;
    }

    boolean sentJavaTasks() {
        return sentJavaTasks;
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
                    if (startedGhci) {
                        haskellOutputWriter.write(ch);
                        haskellOutputWriter.newLine();
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private Result haskellResult(String response, Task task) {
        try {
            haskellOutputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        output.clear();
        return new Result(response, task);
    }

    void startHaskellProcess() {
        notInterrupted = true;
        sentHaskellTasks = true;
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

    Result handleHaskellTask(Task task) {
        String parentFolder = new File(task.getSourcePath()).getParent();
        File haskellOutput = new File(parentFolder + File.separator + task.getName().split("\\.")[0] + "Output.txt");
        try {
            haskellOutput.createNewFile();
            haskellOutputWriter = new BufferedWriter(new FileWriter(haskellOutput));
        } catch (IOException e) {
            e.printStackTrace();
        }
        startedGhci = true;
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
            if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Failed, modules loaded: none.")) {
                try {
                    haskellOutputWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return haskellResult("CE", task); //Compilation Error
            }
            if (compilationTime == 100) {
                notInterrupted = false;
                cmdInput.close();
                haskellProcess.destroy();
                startedGhci = false;
                startHaskellProcess();
                try {
                    haskellOutputWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return haskellResult("TL", task); // Took too long to compile.
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int curTest = 1;
        for (Test test : testContents) {
            int beforeTesting = output.size();
            String testCommand = test.getInput().get(0); //Haskell tasks do not support multi-line inputs.
            ArrayList<String> testOutputVariants = test.getOutputVariants().stream().map(v -> v.get(0)).collect(Collectors.toCollection(ArrayList::new));
            cmdInput.println(testCommand);
            try {
                haskellOutputWriter.write(testCommand);
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            cmdInput.flush();
            int computationTime = 0;
            while (true) {
                if (output.size() > beforeTesting) break;
                if (computationTime >= test.getTime()) {
                    cmdInput.close();
                    try {
                        if (System.getProperty("os.name").startsWith("Windows"))
                            Runtime.getRuntime().exec("taskkill /F /IM ghc.exe");
                        else
                            Runtime.getRuntime().exec("kill -9 ghc");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startedGhci = false;
                    haskellProcess.destroy();
                    startHaskellProcess(); //Restart ghci if we encountered infinite input/ long computation.
                    try {
                        haskellOutputWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return haskellResult("TL " + curTest, task); //Took too long to compute.
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                computationTime += 10;
            }
            if (output.size() > beforeTesting + 1 && output.get(beforeTesting + 1).startsWith("<interactive>"))
                return haskellResult("RE " + curTest, task);
            String response = output.get(beforeTesting).split(" ", 2)[1]; // *>TaskName> Output
            if (testOutputVariants.contains("Error") && response.startsWith("*** Exception") && !response.startsWith("*** Exception: Prelude") // If exception is expected.
                    || testOutputVariants.contains(response))
                curTest++;
            else
                return haskellResult("WA " + curTest, task);
        }
        return haskellResult("OK", task);
    }

    void finishHaskellTesting() {
        notInterrupted = false;
        haskellProcess.destroy();
        cmdInput.close();
    }

    void startJavaTesting() {
        compiler = ToolProvider.getSystemJavaCompiler();
        sentJavaTasks = true;
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
        File errorFile = new File(parentFolder + File.separator + taskName + "Error.txt");
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
        compilationCommands.add(taskName);
        ProcessBuilder pb = new ProcessBuilder(compilationCommands);
        File inputFile = new File(parentFolder + File.separator + "input.txt");
        File outputFile = new File(parentFolder + File.separator + "output.txt");
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
        int curTest = 1;
        for (Test test : testContents) {
            try {
                FileWriter writer = new FileWriter(inputFile); //To clear input.
                FileWriter outputCleaner = new FileWriter(outputFile);
                outputCleaner.close(); //To clear output
                BufferedReader reader = new BufferedReader(new FileReader(outputFile));
                test.getInput().forEach(testLine -> {
                    try {
                        writer.write(testLine);
                        writer.write("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writer.close();
                Process javaProcess = pb.start();
                int computationTime = 0;
                while (javaProcess.isAlive()) {
                    if (computationTime >= test.getTime()) {
                        reader.close();
                        File jpsFile = new File(parentFolder + File.separator + taskName + "Jps.txt");
                        jpsFile.createNewFile();
                        ProcessBuilder jpsProcess = new ProcessBuilder("jps");
                        jpsProcess.redirectOutput(jpsFile).redirectError(jpsFile);
                        Process p = jpsProcess.start();
                        while (p.isAlive()) {
                            Thread.sleep(10);
                        }
                        String pid = "";
                        List<String> processIds = Files.readAllLines(jpsFile.toPath()).stream().filter(a -> a.contains(taskName)).collect(Collectors.toList());
                        if (processIds.size() > 0)
                            pid = processIds.get(0).split(" ")[0];
                        if (System.getProperty("os.name").startsWith("Windows"))
                            Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                        else
                            Runtime.getRuntime().exec("kill -9 " + pid);
                        Thread.sleep(100);
                        jpsFile.delete();
                        if (errorFile.delete())
                            System.out.println("Deleted");
                        clearFolderFromJavaFiles(task, inputFile, outputFile);
                        return new Result("TL " + curTest, task); //Time Limit.
                    }
                    Thread.sleep(10);
                    computationTime += 10;
                }
                if (errorFile.length() != 0) {
                    reader.close(); //Close inputFile
                    clearFolderFromJavaFiles(task, inputFile, outputFile);
                    return new Result("RE " + curTest, task); //Runtime Error
                }
                ArrayList<String> testOutput = new ArrayList<>();
                String curString = reader.readLine();
                while (curString != null) {
                    testOutput.add(curString);
                    curString = reader.readLine();
                }
                reader.close();
                if (test.getOutputVariants().contains(testOutput))
                    curTest++;
                else {
                    clearFolderFromJavaFiles(task, inputFile, outputFile);
                    return new Result("WA " + curTest, task);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        errorFile.delete();
        clearFolderFromJavaFiles(task, inputFile, outputFile);
        return new Result("OK", task);
    }

    private void clearFolderFromJavaFiles(Task task, File inputFile, File outputFile) {
        inputFile.delete();
        outputFile.delete();
        new File(task.getSourcePath().substring(0, task.getSourcePath().length() - 4) + "class").delete();
    }
}
