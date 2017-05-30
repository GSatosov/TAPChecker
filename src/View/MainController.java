package View;

import Controller.General;
import Controller.GoogleDriveManager;
import Model.Result;
import Model.Task;
import Model.Test;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable {

    @FXML
    private TabPane resultsTable;

    @FXML
    private TabPane plagiary;

    @FXML
    private Button runTests;
    @FXML
    private Button switchTables;
    @FXML
    private Button settings;
    @FXML
    private Button editTests;
    private Integer currentTest;
    private Integer currentOutputVariant;
    private static Stage settingsFrame;

    static Stage getSettingsFrame() {
        return settingsFrame;
    }

    public void showResults(List<Result> results) {
        resultsTable.getTabs().clear();
        HashMap<String, ArrayList<Result>> resultsSplitSubjects = new HashMap<>();
        results.forEach(r -> {
            if (!resultsSplitSubjects.containsKey(r.getSubject())) {
                resultsSplitSubjects.put(r.getSubject(), new ArrayList<>());
            }
            resultsSplitSubjects.get(r.getSubject()).add(r);
        });
        resultsSplitSubjects.forEach((k, v) -> {
            TableView currentTable = new TableView();
            addColumn(currentTable, "ФИО");
            for (int i = 1; i < 100; i++) {
                addColumn(currentTable, i + "");
            }

            HashMap<String, ArrayList<Result>> groupResults = new HashMap<>();
            v.forEach(r -> {
                if (!groupResults.containsKey(r.getGroup())) {
                    groupResults.put(r.getGroup(), new ArrayList<>());
                }
                groupResults.get(r.getGroup()).add(r);
            });
            groupResults.forEach((kGroup, vGroup) -> {
                HashMap<String, String> groupHM = new HashMap<>();
                groupHM.put("ФИО", "Группа " + kGroup);
                ArrayList<String> tasks = new ArrayList((vGroup.stream().map(r -> r.getTask().getName()).collect(Collectors.toSet())));
                tasks.sort(String::compareTo);
                for (int i = 0; i < tasks.size(); i++) {
                    groupHM.put((i + 1) + "", tasks.get(i));
                }
                currentTable.getItems().add(groupHM);
                HashMap<String, ArrayList<String>> studentsResults = new HashMap<>();
                vGroup.forEach(gR -> {
                    ArrayList<String> resultsStudent = studentsResults.get(gR.getStudent().getName());
                    if (!studentsResults.containsKey(gR.getStudent().getName())) {
                        studentsResults.put(gR.getStudent().getName(), new ArrayList<String>());
                        resultsStudent = studentsResults.get(gR.getStudent().getName());
                        resultsStudent.add(gR.getStudent().getName());
                        for (int i = 0; i < tasks.size(); i++) resultsStudent.add("");
                    }
                    resultsStudent.set(tasks.indexOf(gR.getTask().getName()) + 1, gR.getMessage());
                });
                studentsResults.forEach((kStudent, vStudent) -> {
                    HashMap<String, String> studentHM = new HashMap<>();
                    studentHM.put("ФИО", vStudent.get(0));
                    for (int i = 1; i < vStudent.size(); i++) {
                        studentHM.put(i + "", vStudent.get(i));
                    }
                    currentTable.getItems().add(studentHM);
                });
                HashMap<String, String> emptyHM = new HashMap<>();
                emptyHM.put("ФИО", "");
                currentTable.getItems().add(emptyHM);
            });
            Tab tab = new Tab(k, currentTable);
            tab.setClosable(false);
            resultsTable.getTabs().add(tab);
        });
    }

    private static void addColumn(TableView table, String columnTitle) {
        TableColumn<Map, String> column = new TableColumn<>(columnTitle);
        column.setCellValueFactory(new MapValueFactory(columnTitle));
        column.setMinWidth(130);
        table.getColumns().add(column);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        runTests.setOnAction(event -> {
            runTests.setDisable(true);
            try {
                General.getResults(() -> runTests.setDisable(false), this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        switchTables.setOnAction(event -> {
            if (resultsTable.isVisible()) {
                resultsTable.setVisible(false);
                plagiary.setVisible(true);
            } else {
                resultsTable.setVisible(true);
                plagiary.setVisible(false);
            }
        });

        settings.setOnAction(event -> {
            settingsFrame = new Stage();
            try {
                settingsFrame.setScene(new Scene(new FXMLLoader(getClass().getResource("Settings.fxml")).load(), 600, 400));
            } catch (IOException e) {
                e.printStackTrace();
            }

            settingsFrame.initModality(Modality.WINDOW_MODAL);
            settingsFrame.initOwner(MainFrame.getPrimaryStage());
            settingsFrame.setTitle("GlobalSettings");
            settingsFrame.setResizable(false);
            settingsFrame.show();
        });
        editTests.setOnAction(event -> {
            Stage testEditorStage = new Stage();
            GridPane taskPane = new GridPane();
            BorderPane mainPane = new BorderPane();
            GridPane testsGridPane = new GridPane();
            TextField taskCodeField = new TextField();
            TextField timeLimitField = new TextField();
            CheckBox antiPlagiarismCheckBox = new CheckBox();
            CheckBox hardDeadlineCheckbox = new CheckBox();
            DatePicker deadlinePicker = new DatePicker();
            deadlinePicker.setConverter(new StringConverter<LocalDate>() { //Ensuring that we will get format we need, regardless of local settings.
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                @Override
                public String toString(LocalDate date) {
                    if (date != null) {
                        return dateFormatter.format(date);
                    } else {
                        return "";
                    }
                }

                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.isEmpty()) {
                        return LocalDate.parse(string, dateFormatter);
                    } else {
                        return null;
                    }
                }
            });
            TextField taskNameField = new TextField();
            TextField taskSubjectField = new TextField();
            taskPane.add(new Text("Enter the name of the function to check"), 0, 1);
            taskPane.add(taskNameField, 1, 1);
            taskPane.add(new Text("Enter the name of the subject"), 0, 2);
            taskPane.add(taskSubjectField, 1, 2);
            taskPane.add(new Text("Enter the code for this task:"), 0, 3);
            taskPane.add(taskCodeField, 1, 3);
            taskPane.add(new Text("Enter the time limit in ms:"), 0, 4);
            taskPane.add(timeLimitField, 1, 4);
            taskPane.add(new Text("Enter the deadline for this task:"), 0, 5);
            taskPane.add(deadlinePicker, 1, 5);
            taskPane.add(new Text("If the deadline has not been met, will the task net 0 points?"), 0, 6);
            taskPane.add(hardDeadlineCheckbox, 1, 6);
            taskPane.add(new Text("Check the task on plagiarism?"), 0, 7);
            taskPane.add(antiPlagiarismCheckBox, 1, 7);
            TextArea inputArea = new TextArea();
            TextArea outputArea = new TextArea();
            outputArea.setPrefWidth(200);
            outputArea.setPrefHeight(200);
            inputArea.setPrefHeight(200);
            inputArea.setPrefWidth(200);
            currentTest = 0;
            currentOutputVariant = 0;
            ArrayList<Test> newTests = new ArrayList<>();
            ArrayList<ArrayList<String>> outputVariants = new ArrayList<>();
            outputVariants.add(new ArrayList<>());
            newTests.add(new Test(new ArrayList<>(), outputVariants)); //Created test with one empty output variant.
            BorderPane testsPane = new BorderPane(); //Pane with test/output variants buttons and input/output text areas.
            GridPane bottomPane = new GridPane();

            bottomPane.add(new Text("Enter an additional test:"), 0, 1);
            TextField additionalTestField = new TextField();
            bottomPane.add(additionalTestField, 1, 1);
            Button saveAdditionalTestButton = new Button("Save Additional Test");
            saveAdditionalTestButton.setOnAction(event1 -> newTests.get(currentTest).setAdditionalTest(additionalTestField.getText()));
            bottomPane.add(saveAdditionalTestButton, 2, 1);
            Button deleteTestButton = new Button("Delete Test");
            deleteTestButton.setOnAction(event1 -> {
                if (newTests.size() == 1) { //Clearing first test.
                    outputVariants.clear();
                    outputVariants.add(new ArrayList<>());
                    newTests.get(currentTest).setOutputVariants(outputVariants);
                    newTests.get(currentTest).setInput(new ArrayList<>());
                } else {
                    newTests.remove((int) currentTest);
                    if (currentTest == newTests.size())
                        currentTest--;
                }
                currentOutputVariant = 0;
                testsPane.setTop(showTestButtons(newTests, inputArea, outputArea, testsPane, additionalTestField));
                testsPane.setRight(showOutputVariantsButtons(outputArea, newTests.get(currentTest)));
            });
            Button deleteOutputVariantButton = new Button("Delete Output Variant");
            deleteOutputVariantButton.setOnAction(event1 -> {
                if (newTests.get(currentTest).getOutputVariants().size() == 1) {
                    outputVariants.clear();
                    outputVariants.add(new ArrayList<>());
                    newTests.get(currentTest).setOutputVariants(outputVariants);
                    showOutputVariantsButtons(outputArea, newTests.get(currentTest));
                } else {
                    newTests.get(currentTest).removeOutputVariant(currentOutputVariant);
                    if (currentOutputVariant == newTests.get(currentTest).getOutputVariants().size())
                        currentOutputVariant--;
                    testsPane.setRight(showOutputVariantsButtons(outputArea, newTests.get(currentTest)));
                }
            });
            Button saveTask = new Button("Save Task");
            saveTask.setOnAction(event1 -> {
                if (fieldsAreReady(timeLimitField, deadlinePicker, taskCodeField, taskNameField, taskSubjectField)) {
                    Task task = new Task(taskNameField.getText(), taskSubjectField.getText(), null, null);
                    task.setTestFields(Long.parseLong(timeLimitField.getCharacters().toString()),
                            antiPlagiarismCheckBox.isSelected(),
                            java.sql.Date.valueOf(deadlinePicker.getValue()),
                            taskCodeField.getCharacters().toString(),
                            hardDeadlineCheckbox.isSelected());
                    fillCurrentTest(newTests, inputArea, outputArea);
                    task.setTestContents(newTests);
                    try {
                        GoogleDriveManager.saveTask(task);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            bottomPane.add(saveTask, 0, 2);
            bottomPane.add(deleteTestButton, 1, 2);
            bottomPane.add(deleteOutputVariantButton, 2, 2);
            ComboBox<String> subjectsBox = new ComboBox<>();
            ComboBox<String> tasksBox = new ComboBox<>();
            try {
                HashMap<String, ArrayList<Task>> subjectsAndTasks = GoogleDriveManager.getTasksAndSubjects();
                ArrayList<String> subjects = new ArrayList<>(subjectsAndTasks.keySet());
                subjectsBox.getItems().addAll(subjects);
                subjectsBox.setOnAction(event1 -> {
                    tasksBox.getSelectionModel().clearSelection();
                    tasksBox.getItems().clear();
                    if (!subjectsBox.getValue().equals("New Subject...")) {
                        tasksBox.getItems().addAll(subjectsAndTasks.get(subjectsBox.getValue()).stream().map(Task::getName).collect(Collectors.toList()));
                    }
                    tasksBox.getItems().add("New Task...");
                    fillFieldsWithTaskInformation(emptyTask(), taskCodeField, taskSubjectField, taskNameField, inputArea, outputArea, antiPlagiarismCheckBox, hardDeadlineCheckbox, deadlinePicker, timeLimitField,
                            additionalTestField, testsPane);
                    tasksBox.getSelectionModel().selectLast();
                });
                tasksBox.setOnAction(event1 -> {
                    if (tasksBox.getValue() != null) {
                        Task task;
                        if (!subjectsBox.getValue().equals("New Subject...")) {
                            if (!tasksBox.getValue().equals("New Task...")) {
                                task = subjectsAndTasks.get(subjectsBox.getValue()).stream().filter(t -> t.getName().equals(tasksBox.getValue())).findFirst().get();
                            } else {
                                task = emptyTask();
                                task.setSubjectName(subjectsBox.getValue());
                            }
                        } else {
                            task = emptyTask();
                        }
                        fillFieldsWithTaskInformation(task, taskCodeField, taskSubjectField, taskNameField, inputArea, outputArea, antiPlagiarismCheckBox, hardDeadlineCheckbox, deadlinePicker, timeLimitField,
                                additionalTestField, testsPane);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            subjectsBox.getItems().add("New Subject...");
            tasksBox.getItems().add("New Task...");
            subjectsBox.getSelectionModel().selectLast();
            tasksBox.getSelectionModel().selectLast();
            GridPane comboBoxesPane = new GridPane();
            comboBoxesPane.add(subjectsBox, 0, 1);
            comboBoxesPane.add(tasksBox, 1, 1);
            BorderPane tasksBorderPane = new BorderPane();
            tasksBorderPane.setCenter(taskPane);
            tasksBorderPane.setBottom(testsPane);
            testsPane.setCenter(testsGridPane);
            testsPane.setTop(showTestButtons(newTests, inputArea, outputArea, testsPane, additionalTestField));
            testsGridPane.add(inputArea, 0, 1);
            testsGridPane.add(outputArea, 1, 1);
            mainPane.setTop(comboBoxesPane);
            mainPane.setCenter(tasksBorderPane);
            mainPane.setBottom(bottomPane);
            testEditorStage.setScene(new Scene(mainPane, 500, 480));
            testEditorStage.setTitle("Task Editor");
            testEditorStage.show();
        });
    }

    private Task emptyTask() {
        Task task = new Task();
        ArrayList<Test> newTests = new ArrayList<>();
        ArrayList<ArrayList<String>> outputVariants = new ArrayList<>();
        outputVariants.add(new ArrayList<>());
        newTests.add(new Test(new ArrayList<>(), outputVariants));
        task.setTestContents(newTests);
        return task;
    }

    private void fillFieldsWithTaskInformation(Task task, TextField taskCodeField,
                                               TextField taskSubjectNameField, TextField taskNameField,
                                               TextArea input, TextArea output,
                                               CheckBox antiPlagiarism, CheckBox hasHardDeadline,
                                               DatePicker deadlinePicker, TextField timeLimitTextField,
                                               TextField additionalTestField,
                                               BorderPane pane) {
        taskSubjectNameField.setText(task.getSubjectName());
        taskNameField.setText(task.getName());
        taskCodeField.setText(task.getTaskCode());
        antiPlagiarism.setSelected(task.shouldBeCheckedForAntiPlagiarism());
        hasHardDeadline.setSelected(task.hasHardDeadline());
        if (task.getTimeInMS() == 0)
            timeLimitTextField.clear();
        else
            timeLimitTextField.setText(Long.toString(task.getTimeInMS()));
        if (task.getDeadline() != null)
            deadlinePicker.setValue(task.getDeadline().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        else
            deadlinePicker.setValue(null);
        fillTextAreaWithConcatenatedList(task.getTestContents().get(0).getInput(), input);
        fillTextAreaWithConcatenatedList(task.getTestContents().get(0).getOutputVariants().get(0), output);
        additionalTestField.setText(task.getTestContents().get(0).getAdditionalTest());
        pane.setTop(showTestButtons(task.getTestContents(), input, output, pane, additionalTestField));

    }

    private boolean fieldsAreReady(TextField timeLimit, DatePicker picker, TextField taskCode, TextField taskName, TextField taskSubjectName) {
        if (taskName.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for task name is empty.");
            alert.showAndWait();
            return false;
        }
        if (taskSubjectName.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for task subject is empty.");
            alert.showAndWait();
            return false;
        }
        if (taskCode.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for task code is empty.");
            alert.showAndWait();
            return false;
        }
        if (timeLimit.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for time limit is empty.");
            alert.showAndWait();
            return false;
        }
        if (!timeLimit.getText().matches("[0-9]+")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Field for time limit contains literals.");
            alert.showAndWait();
            return false;
        }
        if (picker.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "You have not picked a deadline");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    //Updates color of pressed button and previously pressed button.
    private void updateButtonStyles(Pane box, int previousIndex, int currentIndex) {
        box.getChildren().get(previousIndex).setStyle("-fx-base: #ffffff;");
        box.getChildren().get(currentIndex).setStyle("-fx-base: #b6e7c9;");
    }

    //Fills runTests with content found in input fields. Rewrite.
    private void fillCurrentTest(ArrayList<Test> newTests, TextArea inputField, TextArea outputField) {
        newTests.get(currentTest).setInput(Arrays.stream(inputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)));
        if (newTests.get(currentTest).getOutputVariants().size() == 1) {
            ArrayList<ArrayList<String>> outputVariants = new ArrayList<>();
            outputVariants.add(Arrays.stream(outputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)));
            newTests.get(currentTest).setOutputVariants(outputVariants);
        } else
            newTests.get(currentTest).setOutputVariant(Arrays.stream(outputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)),
                    currentOutputVariant);
    }

    private void updateCurrentTest(ArrayList<Test> tests, TextArea input, TextArea output) {
        Test testToBeUpdated = tests.get(currentTest);
        testToBeUpdated.setInput(Arrays.stream(input.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)));
        testToBeUpdated.setOutputVariant(Arrays.stream(output.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
    }

    private void fillTextAreaWithConcatenatedList(ArrayList<String> list, TextArea area) {
        if (list.isEmpty())
            area.clear();
        else {
            String identity = list.get(0);
            if (list.size() == 1)
                area.setText(identity);
            else {
                area.setText(list.subList(1, list.size()).stream().reduce(identity, (a, b) -> a.concat(System.getProperty("line.separator").concat(b))));
            }
        }
    }

    //Buttons that show input of each test.
    private Button inputButton(HBox testButtons, ArrayList<Test> tests, int i, TextArea input, TextArea output, BorderPane pane, TextField additionalTestField) {
        Test test = tests.get(i);
        Button inputButton = new Button(Integer.toString(i + 1));
        inputButton.setOnAction(event -> {
            updateCurrentTest(tests, input, output);
            currentOutputVariant = 0; //You switch to another test and first output variant is ready to be edited.
            fillTextAreaWithConcatenatedList(test.getInput(), input);
            fillTextAreaWithConcatenatedList(test.getOutputVariants().get(currentOutputVariant), output);
            pane.setRight(showOutputVariantsButtons(output, test));
            updateButtonStyles(testButtons, currentTest, currentTest = Integer.parseInt(inputButton.getText()) - 1);
            additionalTestField.setText(test.getAdditionalTest());
        });
        inputButton.setStyle("-fx-base: #ffffff;");
        return inputButton;
    }

    private HBox showTestButtons(ArrayList<Test> tests, TextArea input, TextArea output, BorderPane pane, TextField additionalTestField) {
        HBox testButtons = new HBox();
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++)
            buttons.add(inputButton(testButtons, tests, i, input, output, pane, additionalTestField));
        Button newTestButton = new Button("+");
        newTestButton.setOnAction(event -> {
            ArrayList<ArrayList<String>> newOutputVariants = new ArrayList<>();
            newOutputVariants.add(new ArrayList<>());
            tests.add(new Test(new ArrayList<>(), newOutputVariants));
            testButtons.getChildren().get(currentTest).setStyle("-fx-base: #ffffff;");
            updateCurrentTest(tests, input, output);
            currentTest = testButtons.getChildren().size() - 1;
            currentOutputVariant = 0;
            input.clear();
            output.clear();
            additionalTestField.setText(tests.get(currentTest).getAdditionalTest());
            testButtons.getChildren().add(currentTest, inputButton(testButtons, tests, currentTest, input, output, pane, additionalTestField));
            testButtons.getChildren().get(currentTest).setStyle("-fx-base: #b6e7c9;");
            pane.setRight(showOutputVariantsButtons(output, tests.get(currentTest)));
        });
        newTestButton.setStyle("-fx-base: #ffffff;");
        buttons.add(newTestButton);
        buttons.get(currentTest).setStyle("-fx-base: #b6e7c9;");
        testButtons.getChildren().addAll(buttons);
        pane.setRight(showOutputVariantsButtons(output, tests.get(currentTest)));
        return testButtons;
    }

    //Buttons that show output of each test.
    private Button outputVariantButton(VBox outputButtons, Test test, int i, TextArea output) {
        Button outputVariantButton = new Button(Integer.toString(i + 1));
        outputVariantButton.setOnAction(event -> {
            test.setOutputVariant(Arrays.stream(output.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
            fillTextAreaWithConcatenatedList(test.getOutputVariants().get(Integer.parseInt(outputVariantButton.getText()) - 1), output);
            updateButtonStyles(outputButtons, currentOutputVariant, currentOutputVariant = Integer.parseInt(outputVariantButton.getText()) - 1);
        });
        outputVariantButton.setStyle("-fx-base: #ffffff;");
        return outputVariantButton;
    }

    private VBox showOutputVariantsButtons(TextArea output, Test test) {
        VBox outputButtons = new VBox();
        ArrayList<Button> buttons = new ArrayList<>();
        ArrayList<ArrayList<String>> outputVariants = test.getOutputVariants();
        for (int i = 0; i < outputVariants.size(); i++)
            buttons.add(outputVariantButton(outputButtons, test, i, output));
        Button newOutputVariantButton = new Button("+");
        newOutputVariantButton.setOnAction(event -> {
            test.addOutputVariant(new ArrayList<>());
            test.setOutputVariant(Arrays.stream(output.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
            outputButtons.getChildren().get(currentOutputVariant).setStyle("-fx-base: #ffffff;");
            currentOutputVariant = test.getOutputVariants().size() - 1;
            outputButtons.getChildren().add(currentOutputVariant, outputVariantButton(outputButtons, test, currentOutputVariant, output));
            outputButtons.getChildren().get(currentOutputVariant).setStyle("-fx-base: #b6e7c9;");
            output.clear();
        });
        newOutputVariantButton.setStyle("-fx-base: #ffffff;");
        buttons.add(newOutputVariantButton);
        buttons.get(currentOutputVariant).setStyle("-fx-base: #b6e7c9;");
        outputButtons.getChildren().addAll(buttons);
        return outputButtons;
    }
}
