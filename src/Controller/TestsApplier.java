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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for applying tests. As of 3/14 only Haskell support is implemented. As of 4/18 Java support is implemented as well.
 */
class TestsApplier {
    private volatile boolean notInterrupted;
    private volatile List<String> haskellOutput;
    private Process haskellProcess;
    private PrintStream haskellProcessInput;
    private JavaCompiler compiler;
    private BufferedWriter haskellOutputWriter;
    private BufferedWriter javaOutputWriter;

    private Thread cmdOutput(InputStream stream) {
        haskellOutput = Collections.synchronizedList(new ArrayList<String>());
        final boolean[] readyToCompileFiles = {false}; //...
        return new Thread(() -> {
            try {
                int curByte;
                String curLine = "";
                while (notInterrupted) {
                    curByte = stream.read();
                    if (curLine.equals("Prelude>")) {
                        if (!readyToCompileFiles[0]) {
                            haskellOutput.add(curLine);
                            readyToCompileFiles[0] = true;
                        }
                        curLine = "";
                    }
                    if (curLine.equals(System.getProperty("line.separator")))
                        curLine = "";
                    if (curLine.endsWith(System.getProperty("line.separator"))) { //Newline character.
                        haskellOutput.add(curLine.split(System.getProperty("line.separator"))[0]); //Output\r\n -> Output
                        curLine = "";
                    }
                    curLine = curLine.concat(Character.toString((char) curByte));
                }
                haskellOutput.add(curLine);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private Result haskellResult(String response, Task task) {
        haskellOutput.clear();
        try {
            haskellOutputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Result(response, task);
    }

    boolean startHaskellTesting() {
        notInterrupted = true;
        Date startDate = new Date();
        try {
            haskellProcess = new ProcessBuilder("ghci").redirectErrorStream(true).start();
            haskellProcessInput = new PrintStream(haskellProcess.getOutputStream());
            Thread haskellCmdThread = cmdOutput(haskellProcess.getInputStream());
            haskellCmdThread.start();
            while (true) { //On first launch ProcessBuilder takes a lot of time to execute first command.
                if (!haskellOutput.isEmpty() && haskellOutput.contains("Prelude>"))
                    break;
                if (new Date().getTime() > startDate.getTime() + 4000) { //Four seconds to boot ghci.
                    System.out.println("Something is wrong with your ghc.");
                    return false;
                }
            }
        } catch (IOException e) {
            System.out.println("The path to ghci is invalid.");
            return false;
        }
        System.out.println("Started ghci process.");
        return true;
    }

    Result handleHaskellTask(Task task) {
        String parentFolder = new File(task.getSourcePath()).getParent();
        String taskName = task.getName().split("\\.")[0];
        File haskellOutput = new File(parentFolder + File.separator + taskName + "Output.txt");
        try {
            haskellOutput.createNewFile();
            haskellOutputWriter = new BufferedWriter(new FileWriter(haskellOutput));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<Test> testContents = task.getTestContents();
        haskellProcessInput.println(":l " + task.getSourcePath());
        try {
            haskellOutputWriter.write("Starting testing process...");
            haskellOutputWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.haskellOutput.clear();
        haskellProcessInput.flush();
        int compilationTime = 0;
        while (true) {
            compilationTime++;
            if (!this.haskellOutput.isEmpty()) {
                if (this.haskellOutput.stream().filter(line -> line.startsWith("Ok, modules loaded:")).count() > 0)
                    break;
                if (this.haskellOutput.contains("Failed, modules loaded: none."))
                    return haskellResult("CE", task); //Compilation Error;
            }
            if (compilationTime == 200) {
                finishHaskellTesting();
                startHaskellTesting();
                return haskellResult("SE", task); // System error — most likely an error on our end.
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            haskellOutputWriter.write("Starting testing process");
            haskellOutputWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < testContents.size(); i++) {
            Test test = testContents.get(i);
            this.haskellOutput.clear();
            String testCommand = test.getInput().get(0); //Haskell tasks do not support multi-line inputs.
            ArrayList<String> testOutputVariants = test.getOutputVariants().stream().map(v -> v.get(0)).collect(Collectors.toCollection(ArrayList::new));
            haskellProcessInput.println(testCommand);
            try {
                haskellOutputWriter.write("Test № " + (i + 1) + ": " + testCommand);
                haskellOutputWriter.newLine();
                haskellOutputWriter.write("Expected:");
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            test.logOutputVariants(haskellOutputWriter);
            haskellProcessInput.flush();
            try {
                haskellOutputWriter.write("Got:");
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int computationTime = 0;
            while (true) {
                if (this.haskellOutput.size() > 0) break;
                if (computationTime >= task.getTimeInMS()) {
                    haskellProcessInput.close();
                    finishHaskellTesting();
                    Test.logList(haskellOutputWriter, this.haskellOutput);
                    startHaskellTesting(); //Restart ghci if we encountered infinite input/ long computation.
                    return haskellResult("TL " + (i + 1), task); //Took too long to compute.
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                computationTime += 10;
            }
            if (this.haskellOutput.size() > 1 && this.haskellOutput.get(1).startsWith("<interactive>")) {
                Test.logList(haskellOutputWriter, this.haskellOutput);
                return haskellResult("RE " + (i + 1), task);
            }
            String response = this.haskellOutput.get(0).split(" ", 2)[1]; // *>TaskName> Output
            if (test.hasAnAdditionalTest() & !task.getAdditionalTest().isEmpty()) {
                File additionalTestInput = new File(parentFolder + File.separator + taskName);
                try {
                    additionalTestInput.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileWriter writer = new FileWriter(additionalTestInput);
                    writer.write(response);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                switch (performAnAdditionalTest(task.getAdditionalTest(), additionalTestInput, taskName, test.getOutputVariants())) {
                    case 0:
                        continue;
                    case 1:
                        return haskellResult("WA " + (i + 1), task);
                    case 2: {
                        System.out.println("Your code for additional test in " + task.getName() + ", test №" + i + " is invalid.");
                        continue;
                    }
                }
                additionalTestInput.delete();
            }
            try {
                haskellOutputWriter.write(response);
                haskellOutputWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!(testOutputVariants.contains("Error") && response.startsWith("*** Exception") && !response.startsWith("*** Exception: Prelude") // If exception is expected.
                    || testOutputVariants.contains(response))) {
                return haskellResult("WA " + (i + 1), task);
            }
        }
        return haskellResult("OK", task);
    }

    void finishHaskellTesting() {
        notInterrupted = false;
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
        haskellProcess.destroy();
        haskellProcessInput.close();
        System.out.println("Closed ghc process.");
    }

    void startJavaTesting() {
        compiler = ToolProvider.getSystemJavaCompiler();
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
                outputCleaner.close(); //To clear Output
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
                    if (computationTime >= task.getTimeInMS()) {
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
                    Test.logList(javaOutputWriter, new ArrayList<>(Files.readAllLines(Paths.get(errorFile.getPath()))));
                    return javaResult("RE " + (i + 1), task, testInputFile, testOutputFile, errorFile); //Runtime Error
                }
                if (test.hasAnAdditionalTest() & !task.getAdditionalTest().isEmpty()) {
                    switch (performAnAdditionalTest(task.getAdditionalTest(), testOutputFile, taskName, test.getOutputVariants())) {
                        case 0:
                            continue;
                        case 1:
                            return javaResult("WA " + (i + 1), task, testInputFile, testOutputFile, errorFile);
                        case 2: {
                            System.out.println("Your code for additional test in " + task.getName() + ", test №" + i + " is invalid.");
                            continue;
                        }
                    }
                }
                ArrayList<String> testOutput = new ArrayList<>(Files.readAllLines(Paths.get(testOutputFile.getPath())));
                Test.logList(javaOutputWriter, testOutput);
                if (!test.getOutputVariants().contains(testOutput))
                    return javaResult("WA " + (i + 1), task, testInputFile, testOutputFile, errorFile);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return javaResult("OK", task, testInputFile, testOutputFile, errorFile);
    }

    private int performAnAdditionalTest(String code, File inputFile, String taskName, ArrayList<ArrayList<String>> outputVariants) {
        File codeFile = new File(inputFile.getParent() + File.separator + taskName + "Test.java");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(codeFile));
            writer.write(code);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File testErrorFile = new File(inputFile.getParent() + File.separator + taskName + "AdditionalError.txt");
        try {
            FileOutputStream errorStream = new FileOutputStream(testErrorFile);
            compiler.run(null, null, errorStream, codeFile.getPath());
            errorStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (testErrorFile.length() > 0) {
            codeFile.delete();
            testErrorFile.delete();
            return 2; //Got an error while compiling the test; undesirable outcome.
        }
        ArrayList<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-cp");
        commands.add(inputFile.getParent());
        commands.add(taskName + "Test");
        ProcessBuilder pb = new ProcessBuilder(commands);
        File testOutputFile = new File(inputFile.getParent() + File.separator + taskName + "AdditionalOutput.txt");
        pb.redirectError(testErrorFile);
        pb.redirectOutput(testOutputFile);
        pb.redirectInput(inputFile);
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        if (testErrorFile.length() > 0) {
            try {
                javaOutputWriter.write("Additional test runtime error:");
                javaOutputWriter.newLine();
                Test.logList(javaOutputWriter, new ArrayList<>(Files.readAllLines(Paths.get(testErrorFile.getPath()))));
            } catch (IOException e) {
                e.printStackTrace();
            }
            new File(inputFile.getParent() + File.separator + taskName + "Test.class").delete();
            codeFile.delete();
            testErrorFile.delete();
            return 1; //RE in additional test.
        }
        new File(inputFile.getParent() + File.separator + taskName + "Test.class").delete();
        codeFile.delete();
        testErrorFile.delete();
        try {
            ArrayList<String> output = new ArrayList<>(Files.readAllLines(Paths.get(testOutputFile.getPath())));
            Test.logList(javaOutputWriter, output);
            testOutputFile.delete();
            if (outputVariants.contains(output))
                return 0; //Everything is fine.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1; //WA in additional test.
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
