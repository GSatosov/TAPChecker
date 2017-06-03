package View;

import Controller.GoogleDriveManager;
import Model.GlobalSettings;
import Model.LocalSettings;
import Model.Task;
import Model.Test;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by GSatosov on 5/31/2017.
 */
public class TaskEditorController implements Initializable {

    @FXML
    public ChoiceBox subjectsList;

    @FXML
    public ChoiceBox<String> tasksList;

    @FXML
    public TextField functionName;

    @FXML
    public TextField taskCode;

    @FXML
    public TextField timeLimit;

    @FXML
    public DatePicker deadline;

    @FXML
    public CheckBox hardDeadline;

    @FXML
    public CheckBox plagiarismCheck;

    @FXML
    public TextField additionalTest;

    @FXML
    public Button browse;

    @FXML
    public Button deleteTask;

    @FXML
    public Button saveTask;

    @FXML
    public TabPane inputs;

    private Task currentTask;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            HashMap<String, ArrayList<Task>> subjectsAndTasks;

            if (!LocalSettings.getInstance().editorHasBeenLaunched() || GlobalSettings.getInstance().getEditedTasksDate().compareTo(LocalSettings.getInstance().getEditedTasksDate()) > 0) {
                subjectsAndTasks = GoogleDriveManager.getTasksAndSubjects();
                LocalSettings.getInstance().setSubjectsAndTasks(subjectsAndTasks);
                try {
                    LocalSettings.saveSettings();
                } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                    e.printStackTrace();
                }
            } else {
                subjectsAndTasks = LocalSettings.getInstance().getSubjectsAndTasks();
            }


            inputs.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() == -1) return;
                if (newValue.intValue() ==  inputs.getTabs().size() - 1 && inputs.getTabs().get(newValue.intValue()).getText().equals("+")) {
                    addTabInput("+").setClosable(false);
                    inputs.getTabs().get(newValue.intValue()).setText((newValue.intValue() + 1) + "");
                    inputs.getSelectionModel().select(newValue.intValue());
                }
                else {
                    TaskEditorInputTabController taskEditorInputTabController = (TaskEditorInputTabController) inputs.getTabs().get(newValue.intValue()).getUserData();
                    if (taskEditorInputTabController.inputArea.getText().equals("")) {
                        if (taskEditorInputTabController.outputs.getTabs().size() == 0) {
                            taskEditorInputTabController.addOutputsListener(this);
                            ArrayList<ArrayList<String>> outputVariants;
                            if (newValue.intValue() < currentTask.getTestContents().size()) {
                                outputVariants = currentTask.getTestContents().get(newValue.intValue()).getOutputVariants();
                            }
                            else {
                                outputVariants = new ArrayList<>();
                                ArrayList<String> variant = new ArrayList<>();
                                variant.add("");
                                outputVariants.add(variant);
                            };
                            for (int j = 0; j < outputVariants.size(); j++) {
                                addTabOutput((j + 1) + "", newValue.intValue());
                                final TextArea outputArea = ((TaskEditorOutputTabController) taskEditorInputTabController.outputs.getTabs().get(j).getUserData()).outputArea;
                                outputVariants.get(j).forEach(out -> {
                                    outputArea.setText(outputArea.getText() + (!outputArea.getText().equals("") ? System.getProperty("line.separator") : "") + out);
                                });
                            }
                            addTabOutput("+", newValue.intValue());
                        }

                    }
                }
            });

            subjectsList.getItems().addAll(subjectsAndTasks.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new)));
            subjectsList.getSelectionModel().selectFirst();
            tasksList.getItems().addAll(subjectsAndTasks.get(subjectsList.getValue().toString()).stream().sorted(Comparator.comparing(Task::getTaskCode)).map(task -> task.getTaskCode() + ": " + task.getName()).collect(Collectors.toList()));

            subjectsList.setOnAction(event1 -> {
                tasksList.getItems().clear();
                tasksList.getItems().addAll(subjectsAndTasks.get(subjectsList.getValue().toString()).stream().sorted(Comparator.comparing(Task::getTaskCode)).map(task -> task.getTaskCode() + ": " + task.getName()).collect(Collectors.toList()));
                tasksList.getItems().add("New task...");
                fillFieldsWithTaskInformation(emptyTask());
                tasksList.getSelectionModel().selectLast();
            });
            tasksList.setOnAction(event1 -> {
                if (tasksList.getValue() != null) {
                    Task task;
                    if (tasksList.getSelectionModel().getSelectedIndex() != tasksList.getItems().size() - 1) {
                        task = subjectsAndTasks.get(subjectsList.getValue().toString()).stream().filter(t -> t.getName().equals(tasksList.getValue().toString().split(": ")[1])).findFirst().get();
                        functionName.setDisable(true);
                    } else {
                        task = emptyTask();
                        functionName.setDisable(false);
                    }
                    fillFieldsWithTaskInformation(task);
                }
            });
            tasksList.getItems().add("New task...");
            tasksList.getSelectionModel().selectLast();

            browse.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                File selectedFile = fileChooser.showOpenDialog(browse.getScene().getWindow());

                if (selectedFile != null) {
                    try {
                        byte[] encoded = Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath()));
                        additionalTest.setText(new String(encoded, Charset.defaultCharset()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            deleteTask.setOnAction(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you really want to delete this task?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        if (tasksList.getSelectionModel().getSelectedIndex() == tasksList.getItems().size() - 1) {
                            fillFieldsWithTaskInformation(emptyTask());
                        }
                        else {
                            try {
                                GoogleDriveManager.deleteTask(currentTask);
                                LocalSettings.getInstance().deleteTask(currentTask);
                                tasksList.getItems().clear();
                                tasksList.getItems().addAll(LocalSettings.getInstance().getSubjectsAndTasks().get(subjectsList.getValue().toString()).stream().sorted(Comparator.comparing(Task::getTaskCode)).map(task -> task.getTaskCode() + ": " + task.getName()).collect(Collectors.toList()));
                                tasksList.getItems().add("New task...");
                                tasksList.getSelectionModel().selectLast();
                            } catch (IOException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
            });

            saveTask.setOnAction(event -> {
                if (fieldsAreReady()) {
                    currentTask.setSubjectName(subjectsList.getValue().toString());
                    currentTask.setName(functionName.getText());
                    currentTask.setTestFields(Long.parseLong(timeLimit.getCharacters().toString()),
                            plagiarismCheck.isSelected(),
                            java.sql.Date.valueOf(deadline.getValue()),
                            taskCode.getCharacters().toString(),
                            hardDeadline.isSelected());

                    ArrayList<Test> tests = new ArrayList<>();

                    for (int i = 0; i < inputs.getTabs().size() - 1; i++) {
                        TaskEditorInputTabController taskEditorInputTabController = (TaskEditorInputTabController) inputs.getTabs().get(i).getUserData();
                        ArrayList<String> inputTest = Arrays.stream(taskEditorInputTabController.inputArea.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new));
                        ArrayList<ArrayList<String>> outputTests = new ArrayList<>();
                        for (int j = 0; j < taskEditorInputTabController.outputs.getTabs().size() - 1; j++) {
                            TaskEditorOutputTabController taskEditorOutputTabController = (TaskEditorOutputTabController) taskEditorInputTabController.outputs.getTabs().get(j).getUserData();
                            outputTests.add(Arrays.stream(taskEditorOutputTabController.outputArea.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new)));
                        }
                        Test test = new Test(inputTest, outputTests);
                        test.setApplyAdditionalTest(taskEditorInputTabController.applyAdditionalTest.isSelected());
                        tests.add(test);
                    }

                    currentTask.setTestContents(tests);
                    currentTask.setAdditionalTest(additionalTest.getText());

                    try {
                        LocalSettings.getInstance().updateTask(currentTask);
                        GoogleDriveManager.saveTask(currentTask);
                    } catch (IOException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    tasksList.getItems().clear();
                    tasksList.getItems().addAll(LocalSettings.getInstance().getSubjectsAndTasks().get(subjectsList.getValue().toString()).stream().sorted(Comparator.comparing(Task::getTaskCode)).map(task -> task.getTaskCode() + ": " + task.getName()).collect(Collectors.toList()));
                    tasksList.getItems().add("New task...");
                    tasksList.getSelectionModel().select(currentTask.getTaskCode() + ": " + currentTask.getName());
                }
            });

            additionalTest.textProperty().addListener((observable, oldValue, newValue) -> {
                inputs.getTabs().forEach(tab -> {
                    TaskEditorInputTabController taskEditorInputTabController = (TaskEditorInputTabController) tab.getUserData();
                    if (oldValue.equals("") && !newValue.equals("")) {
                        taskEditorInputTabController.applyAdditionalTest.setSelected(true);
                    }
                    else if (newValue.equals("") && !oldValue.equals("")) {
                        taskEditorInputTabController.applyAdditionalTest.setSelected(false);
                    }
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillFieldsWithTaskInformation(Task task) {
        currentTask = task;
        functionName.setText(currentTask.getName());
        taskCode.setText(currentTask.getTaskCode());
        plagiarismCheck.setSelected(currentTask.shouldBeCheckedForAntiPlagiarism());
        hardDeadline.setSelected(currentTask.hasHardDeadline());
        timeLimit.setText(currentTask.getTimeInMS() == 0 ? "" : Long.toString(currentTask.getTimeInMS()));
        deadline.setValue(currentTask.getDeadline() != null ? new java.sql.Date(currentTask.getDeadline().getTime()).toLocalDate() : null);
        additionalTest.setText(task.getAdditionalTest());
        ArrayList<Test> testContents = currentTask.getTestContents();

        inputs.getTabs().clear();
        for (int i = 0; i < testContents.size(); i++) {
            addTabInput((i + 1) + "");
            TaskEditorInputTabController taskEditorInputTabController = (TaskEditorInputTabController) inputs.getTabs().get(i).getUserData();
            if (taskEditorInputTabController.inputArea.getText().equals("")) {
                if (i < currentTask.getTestContents().size()) {
                    currentTask.getTestContents().get(i).getInput().forEach(in -> {
                        taskEditorInputTabController.inputArea.setText(taskEditorInputTabController.inputArea.getText() + (!taskEditorInputTabController.inputArea.getText().equals("") ? System.getProperty("line.separator") : "") + in);
                    });
                    taskEditorInputTabController.applyAdditionalTest.setSelected(
                        !currentTask.getAdditionalTest().equals("") && currentTask.getTestContents().get(i).hasAnAdditionalTest()
                    );
                }
                if (taskEditorInputTabController.outputs.getTabs().size() == 0) {
                    taskEditorInputTabController.addOutputsListener(this);
                    ArrayList<ArrayList<String>> outputVariants;
                    if (i < currentTask.getTestContents().size()) {
                        outputVariants = currentTask.getTestContents().get(i).getOutputVariants();
                    }
                    else {
                        outputVariants = new ArrayList<>();
                        ArrayList<String> variant = new ArrayList<>();
                        variant.add("");
                        outputVariants.add(variant);
                    }
                    for (int j = 0; j < outputVariants.size(); j++) {
                        addTabOutput((j + 1) + "", i);
                        final TextArea outputArea = ((TaskEditorOutputTabController) taskEditorInputTabController.outputs.getTabs().get(j).getUserData()).outputArea;
                        outputVariants.get(j).forEach(out -> {
                            outputArea.setText(outputArea.getText() + (!outputArea.getText().equals("") ? System.getProperty("line.separator") : "") + out);
                        });
                    }
                    addTabOutput("+", i);
                }
            }
        }
        inputs.getSelectionModel().select(0);

        addTabInput("+").setClosable(false);
    }

    private Tab addTabInput(String title) {
        try {
            FXMLLoader tabLoader = new FXMLLoader(this.getClass().getResource("TaskEditorInputTab.fxml"));
            Tab tab = tabLoader.load();
            tab.setText(title);
            tab.setUserData(tabLoader.getController());
            tab.setOnClosed(event -> {
                for (int i = 0; i < inputs.getTabs().size() - 1; i++) {
                    inputs.getTabs().get(i).setText((i + 1) + "");
                    inputs.getTabs().get(i).setClosable(inputs.getTabs().size() > 2);
                }
            });
            inputs.getTabs().add(tab);
            for (int i = 0; i < inputs.getTabs().size() - 1; i++) {
                inputs.getTabs().get(i).setClosable(inputs.getTabs().size() > 2);
            }
            return tab;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Tab addTabOutput(String title, int index) {
        try {
            TaskEditorInputTabController taskEditorInputTabController = (TaskEditorInputTabController) ((index != -1) ? inputs.getTabs().get(index).getUserData() : inputs.getSelectionModel().getSelectedItem().getUserData());
            FXMLLoader tabLoader = new FXMLLoader(this.getClass().getResource("TaskEditorOutputTab.fxml"));
            Tab tab = tabLoader.load();
            tab.setText(title);
            tab.setUserData(tabLoader.getController());
            tab.setOnClosed(event -> {
                for (int i = 0; i < taskEditorInputTabController.outputs.getTabs().size() - 1; i++) {
                    taskEditorInputTabController.outputs.getTabs().get(i).setText((i + 1) + "");
                    taskEditorInputTabController.outputs.getTabs().get(i).setClosable(taskEditorInputTabController.outputs.getTabs().size() > 2);
                }
            });
            taskEditorInputTabController.outputs.getTabs().add(tab);
            for (int i = 0; i < taskEditorInputTabController.outputs.getTabs().size() - 1; i++) {
                taskEditorInputTabController.outputs.getTabs().get(i).setClosable(taskEditorInputTabController.outputs.getTabs().size() > 2);
            }
            return tab;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Task emptyTask() {
        Task task = new Task();
        ArrayList<Test> tests = new ArrayList<>();
        tests.add(new Test(new ArrayList<>(), emptyOutputVariants()));
        task.setTestContents(tests);
        return task;
    }

    private ArrayList<ArrayList<String>> emptyOutputVariants() {
        ArrayList<ArrayList<String>> outputVariants = new ArrayList<>();
        outputVariants.add(new ArrayList<>());
        return outputVariants;
    }

    private boolean fieldsAreReady() {
        if (functionName.getText() == null || functionName.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for task name is empty!");
            alert.showAndWait();
            return false;
        }
        if (taskCode.getText() == null || taskCode.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for task code is empty!");
            alert.showAndWait();
            return false;
        }
        if (timeLimit.getText() == null || timeLimit.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for time limit is empty!");
            alert.showAndWait();
            return false;
        }
        if (!timeLimit.getText().matches("[0-9]+")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for time limit contains literals!");
            alert.showAndWait();
            return false;
        }
        if (deadline.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "You have not picked a deadline!");
            alert.showAndWait();
            return false;
        }
        return true;
    }

}
