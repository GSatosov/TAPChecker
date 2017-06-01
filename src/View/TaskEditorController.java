package View;

import Controller.GoogleDriveManager;
import Model.LocalSettings;
import Model.Task;
import Model.Test;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by GSatosov on 5/31/2017.
 */
class TaskEditorController {
    private Task curTask;
    private Integer currentTest;
    private Integer currentOutputVariant;
    private ComboBox<String> subjectsBox;
    private ComboBox<String> tasksBox;
    private GridPane comboBoxesPane = new GridPane();

    Stage getStage() {
        Stage testEditorStage = new Stage();
        GridPane taskPane = new GridPane();
        BorderPane mainPane = new BorderPane();
        GridPane testsGridPane = new GridPane();
        TextField taskCodeField = new TextField();
        TextField timeLimitField = new TextField();
        CheckBox antiPlagiarismCheckBox = new CheckBox();
        CheckBox hardDeadlineCheckbox = new CheckBox();
        DatePicker deadlinePicker = new DatePicker();
        curTask = emptyTask();
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
        taskPane.add(new Text("Enter the name of the subject"), 0, 1);
        taskPane.add(taskSubjectField, 1, 1);
        taskPane.add(new Text("Enter the name of the function to check"), 0, 2);
        taskPane.add(taskNameField, 1, 2);
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

        BorderPane testsPane = new BorderPane(); //Pane with test/output variants buttons and input/output text areas.
        GridPane bottomPane = new GridPane();
        bottomPane.add(new Text("Enter an additional test:"), 0, 1);
        TextField additionalTestField = new TextField();
        bottomPane.add(additionalTestField, 1, 1);
        Button saveAdditionalTestButton = new Button("Save Additional Test");
        saveAdditionalTestButton.setOnAction(event1 -> curTask.getTestContents().get(currentTest).setAdditionalTest(additionalTestField.getText()));
        bottomPane.add(saveAdditionalTestButton, 2, 1);
        Button saveTask = new Button("Save Task");
        saveTask.setOnAction(event1 -> {
            if (fieldsAreReady(timeLimitField, deadlinePicker, taskCodeField, taskNameField, taskSubjectField)) {
                curTask.setSubjectName(taskSubjectField.getText());
                curTask.setName(taskNameField.getText());
                curTask.setTestFields(Long.parseLong(timeLimitField.getCharacters().toString()),
                        antiPlagiarismCheckBox.isSelected(),
                        java.sql.Date.valueOf(deadlinePicker.getValue()),
                        taskCodeField.getCharacters().toString(),
                        hardDeadlineCheckbox.isSelected());
                updateCurrentTest(inputArea, outputArea);
                LocalSettings.getInstance().updateTest(curTask);
                constructComboBoxes(taskCodeField, taskSubjectField, taskNameField, inputArea, outputArea, antiPlagiarismCheckBox, hardDeadlineCheckbox, deadlinePicker, timeLimitField, additionalTestField, testsPane, testsGridPane);
                subjectsBox.getSelectionModel().select(curTask.getSubjectName());
                tasksBox.getSelectionModel().select(curTask.getTaskCode() + ": " + curTask.getName());
                taskSubjectField.setDisable(true);
                taskNameField.setDisable(true);
                try {
                    LocalSettings.saveSettings();
                    GoogleDriveManager.saveTask(curTask);
                } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
        });
        bottomPane.add(saveTask, 0, 2);
        Button deleteTaskButton = new Button("Delete Task");
        deleteTaskButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    //TODO write method for task deletion on Google Drive here
                    //TODO write method for task deletion from LocalSettings
                    System.out.println("Bang!");
                }
            });
        });
        bottomPane.add(deleteTaskButton, 4, 2);
        constructComboBoxes(taskCodeField, taskSubjectField, taskNameField, inputArea, outputArea, antiPlagiarismCheckBox, hardDeadlineCheckbox, deadlinePicker, timeLimitField, additionalTestField, testsPane, testsGridPane);
        comboBoxesPane = new GridPane();
        comboBoxesPane.add(subjectsBox, 0, 1);
        comboBoxesPane.add(tasksBox, 1, 1);
        BorderPane tasksBorderPane = new BorderPane();
        tasksBorderPane.setCenter(taskPane);
        tasksBorderPane.setBottom(testsPane);
        testsPane.setTop(showTestButtons(inputArea, outputArea, testsGridPane, additionalTestField, testsPane));
        testsPane.setCenter(testsGridPane);
        testsGridPane.add(inputArea, 0, 1);
        testsGridPane.add(outputArea, 1, 1);
        mainPane.setTop(comboBoxesPane);
        mainPane.setCenter(tasksBorderPane);
        mainPane.setBottom(bottomPane);
        testEditorStage.setScene(new Scene(mainPane, 500, 480));
        testEditorStage.setTitle("Task Editor");
        testEditorStage.setResizable(false);
        return testEditorStage;
    }

    private void constructComboBoxes(TextField taskCodeField,
                                     TextField taskSubjectField, TextField taskNameField,
                                     TextArea inputArea, TextArea outputArea,
                                     CheckBox antiPlagiarismCheckBox, CheckBox hardDeadlineCheckbox,
                                     DatePicker deadlinePicker, TextField timeLimitField,
                                     TextField additionalTestField, BorderPane testsPane,
                                     GridPane testsGridPane) {
        subjectsBox = new ComboBox<>();
        tasksBox = new ComboBox<>();

        try {
            HashMap<String, ArrayList<Task>> subjectsAndTasks;
            if (!LocalSettings.getInstance().editorHasBeenLaunched()) {
                subjectsAndTasks = GoogleDriveManager.getTasksAndSubjects();
                LocalSettings.getInstance().setSubjectsAndTasks(subjectsAndTasks);
            } else {
                subjectsAndTasks = LocalSettings.getInstance().getSubjectsAndTasks();
            }
            subjectsBox.getItems().addAll(subjectsAndTasks.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new)));
            subjectsBox.setOnAction(event1 -> {
                tasksBox.getSelectionModel().clearSelection();
                tasksBox.getItems().clear();
                if (!subjectsBox.getValue().equals("New Subject...")) {
                    tasksBox.getItems().addAll(subjectsAndTasks.get(subjectsBox.getValue()).stream().sorted(Comparator.comparing(Task::getTaskCode)).map(task -> task.getTaskCode() + ": " + task.getName()).collect(Collectors.toList()));
                }
                tasksBox.getItems().add("New Task...");
                fillFieldsWithTaskInformation(emptyTask(), taskCodeField, taskSubjectField, taskNameField, inputArea, outputArea, antiPlagiarismCheckBox, hardDeadlineCheckbox, deadlinePicker, timeLimitField,
                        additionalTestField, testsPane, testsGridPane);
                tasksBox.getSelectionModel().selectLast();
            });
            tasksBox.setOnAction(event1 -> {
                if (tasksBox.getValue() != null) {
                    Task task;
                    if (!subjectsBox.getValue().equals("New Subject...")) {
                        if (!tasksBox.getValue().equals("New Task...")) {
                            task = subjectsAndTasks.get(subjectsBox.getValue()).stream().filter(t -> t.getName().equals(tasksBox.getValue().split(": ")[1])).findFirst().get();
                            taskSubjectField.setDisable(true);
                            taskNameField.setDisable(true);
                        } else {
                            task = emptyTask();
                            task.setSubjectName(subjectsBox.getValue());
                            taskSubjectField.setDisable(true);
                            taskNameField.setDisable(false);
                        }
                    } else {
                        taskSubjectField.setDisable(false);
                        taskNameField.setDisable(false);
                        task = emptyTask();
                    }
                    curTask = task;
                    currentOutputVariant = 0;
                    currentTest = 0;
                    fillFieldsWithTaskInformation(task, taskCodeField, taskSubjectField, taskNameField, inputArea, outputArea, antiPlagiarismCheckBox, hardDeadlineCheckbox, deadlinePicker, timeLimitField,
                            additionalTestField, testsPane, testsGridPane);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        subjectsBox.getItems().add("New Subject...");
        tasksBox.getItems().add("New Task...");
        subjectsBox.getSelectionModel().selectLast();
        tasksBox.getSelectionModel().selectLast();
        comboBoxesPane.add(subjectsBox, 0, 1);
        comboBoxesPane.add(tasksBox, 1, 1);
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

    private void fillFieldsWithTaskInformation(Task task, TextField taskCodeField,
                                               TextField taskSubjectNameField, TextField taskNameField,
                                               TextArea input, TextArea output,
                                               CheckBox antiPlagiarism, CheckBox hasHardDeadline,
                                               DatePicker deadlinePicker, TextField timeLimitTextField,
                                               TextField additionalTestField, BorderPane mainPane,
                                               GridPane testsPane) {
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
            deadlinePicker.setValue(new java.sql.Date(task.getDeadline().getTime()).toLocalDate());
        else
            deadlinePicker.setValue(null);
        fillTextAreaWithConcatenatedList(task.getTestContents().get(0).getInput(), input);
        fillTextAreaWithConcatenatedList(task.getTestContents().get(0).getOutputVariants().get(0), output);
        additionalTestField.setText(task.getTestContents().get(0).getAdditionalTest());
        mainPane.setTop(showTestButtons(input, output, testsPane, additionalTestField, mainPane));
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


    private void updateCurrentTest(TextArea input, TextArea output) {
        Test testToBeUpdated = curTask.getTestContents().get(currentTest);
        testToBeUpdated.setInput(Arrays.stream(input.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new)));
        testToBeUpdated.setOutputVariant(Arrays.stream(output.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
    }

    private void fillTextAreaWithConcatenatedList(ArrayList<String> list, TextArea area) {
        if (list.isEmpty())
            area.clear();
        else {
            String identity = list.get(0);
            if (list.size() == 1)
                area.setText(identity);
            else {
                area.setText(list.subList(1, list.size()).stream().reduce(identity, (a, b) -> a.concat("\n").concat(b)));
            }
        }
    }

    //Buttons that getPane input of each test.
    private Button inputButton(HBox testButtons, int i, TextArea input, TextArea output, GridPane rightPane, TextField additionalTestField, BorderPane pane) {
        Test test = curTask.getTestContents().get(i);
        Button inputButton = new Button(Integer.toString(i + 1));
        inputButton.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                updateCurrentTest(input, output);
                currentOutputVariant = 0; //You switch to another test and first output variant is ready to be edited.
                fillTextAreaWithConcatenatedList(test.getInput(), input);
                fillTextAreaWithConcatenatedList(test.getOutputVariants().get(currentOutputVariant), output);
                updateButtonStyles(testButtons, currentTest, currentTest = Integer.parseInt(inputButton.getText()) - 1);
                rightPane.add(showOutputVariantsButtons(output, rightPane), 2, 1);
                additionalTestField.setText(test.getAdditionalTest());
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (curTask.getTestContents().size() == 1) { //Clearing first test.
                    curTask.getTestContents().get(currentTest).setOutputVariants(emptyOutputVariants());
                    curTask.getTestContents().get(currentTest).setInput(new ArrayList<>());
                } else {
                    updateCurrentTest(input, output);
                    curTask.getTestContents().remove(Integer.parseInt(inputButton.getText()) - 1);
                    if (currentTest == curTask.getTestContents().size())
                        currentTest--;
                }
                currentOutputVariant = 0;
                pane.setTop(showTestButtons(input, output, rightPane, additionalTestField, pane));
            }
        });
        inputButton.setStyle("-fx-base: #ffffff;");
        inputButton.setPrefHeight(30);
        inputButton.setPrefWidth(30);
        return inputButton;
    }

    private ScrollPane showTestButtons(TextArea input, TextArea output, GridPane mainPane, TextField additionalTestField, BorderPane curPane) {
        ScrollPane pane = new ScrollPane();
        HBox testButtons = new HBox();
        ArrayList<Test> tests = curTask.getTestContents();
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++)
            buttons.add(inputButton(testButtons, i, input, output, mainPane, additionalTestField, curPane));
        Button newTestButton = new Button("+");
        newTestButton.setOnAction(event -> {
            tests.add(new Test(new ArrayList<>(), emptyOutputVariants()));
            testButtons.getChildren().get(currentTest).setStyle("-fx-base: #ffffff;");
            updateCurrentTest(input, output);
            currentTest = testButtons.getChildren().size() - 1;
            currentOutputVariant = 0;
            input.clear();
            output.clear();
            additionalTestField.setText(tests.get(currentTest).getAdditionalTest());
            testButtons.getChildren().add(currentTest, inputButton(testButtons, currentTest, input, output, mainPane, additionalTestField, curPane));
            testButtons.getChildren().get(currentTest).setStyle("-fx-base: #b6e7c9;");
            mainPane.add(showOutputVariantsButtons(output, mainPane), 2, 1);
        });
        newTestButton.setStyle("-fx-base: #ffffff;");
        newTestButton.setPrefHeight(30);
        newTestButton.setPrefWidth(30);
        buttons.add(newTestButton);
        buttons.get(currentTest).setStyle("-fx-base: #b6e7c9;");
        testButtons.getChildren().addAll(buttons);
        fillTextAreaWithConcatenatedList(curTask.getTestContents().get(currentTest).getInput(), input);
        mainPane.add(showOutputVariantsButtons(output, mainPane), 2, 1);
        pane.setContent(testButtons);
        pane.setPrefHeight(45);
        pane.setPrefWidth(500);
        return pane;
    }

    //Buttons that getPane output of each test.
    private Button outputVariantButton(VBox outputButtons, Test test, int i, TextArea output, GridPane curPane) {
        Button outputVariantButton = new Button(Integer.toString(i + 1));
        outputVariantButton.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                test.setOutputVariant(Arrays.stream(output.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
                updateButtonStyles(outputButtons, currentOutputVariant, currentOutputVariant = Integer.parseInt(outputVariantButton.getText()) - 1);
                fillTextAreaWithConcatenatedList(test.getOutputVariants().get(currentOutputVariant), output);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (curTask.getTestContents().get(currentTest).getOutputVariants().size() == 1) {
                    curTask.getTestContents().get(currentTest).setOutputVariants(emptyOutputVariants());
                } else {
                    curTask.getTestContents().get(currentTest).setOutputVariant(Arrays.stream(output.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
                    curTask.getTestContents().get(currentTest).removeOutputVariant(Integer.parseInt(outputVariantButton.getText()) - 1);
                    if (currentOutputVariant == curTask.getTestContents().get(currentTest).getOutputVariants().size())
                        currentOutputVariant--;
                }
                curPane.add(showOutputVariantsButtons(output, curPane), 2, 1);
            }
        });
        outputVariantButton.setStyle("-fx-base: #ffffff;");
        outputVariantButton.setPrefWidth(30);
        outputVariantButton.setPrefHeight(30);
        return outputVariantButton;
    }

    private ScrollPane showOutputVariantsButtons(TextArea output, GridPane curPane) {
        ScrollPane pane = new ScrollPane();
        VBox outputButtons = new VBox();
        Test test = curTask.getTestContents().get(currentTest);
        ArrayList<Button> buttons = new ArrayList<>();
        ArrayList<ArrayList<String>> outputVariants = test.getOutputVariants();
        for (int i = 0; i < outputVariants.size(); i++)
            buttons.add(outputVariantButton(outputButtons, test, i, output, curPane));
        Button newOutputVariantButton = new Button("+");
        newOutputVariantButton.setOnAction(event -> {
            test.addOutputVariant(new ArrayList<>());
            test.setOutputVariant(Arrays.stream(output.getText().split("\n")).collect(Collectors.toCollection(ArrayList::new)), currentOutputVariant);
            outputButtons.getChildren().get(currentOutputVariant).setStyle("-fx-base: #ffffff;");
            currentOutputVariant = test.getOutputVariants().size() - 1;
            outputButtons.getChildren().add(currentOutputVariant, outputVariantButton(outputButtons, test, currentOutputVariant, output, curPane));
            outputButtons.getChildren().get(currentOutputVariant).setStyle("-fx-base: #b6e7c9;");
            output.clear();
        });
        newOutputVariantButton.setStyle("-fx-base: #ffffff;");
        newOutputVariantButton.setPrefWidth(30);
        newOutputVariantButton.setPrefHeight(30);
        buttons.add(newOutputVariantButton);
        buttons.get(currentOutputVariant).setStyle("-fx-base: #b6e7c9;");
        fillTextAreaWithConcatenatedList(curTask.getTestContents().get(currentTest).getOutputVariants().get(currentOutputVariant), output);
        outputButtons.getChildren().addAll(buttons);
        pane.setContent(outputButtons);
        pane.setMaxHeight(200);
        pane.setPrefWidth(45);
        return pane;
    }

}
