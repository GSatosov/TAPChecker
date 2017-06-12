package View;

import Controller.GoogleSheetsManager;
import Model.GlobalSettings;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Arseniy Nazarov on 27.04.17.
 */
public class SettingsСontroller implements Initializable {

    @FXML
    public TextField emailSubject;

    @FXML
    TextField tableLink;
    @FXML
    Button saveLink;
    @FXML
    Button generateTableLink;

    @FXML
    ChoiceBox<String> subjectList;
    @FXML
    ChoiceBox<String> groupList;

    @FXML
    Button addSubject;
    @FXML
    Button removeSubject;

    @FXML
    Button addGroup;
    @FXML
    Button removeGroup;

    //Autoresponder settings tab
    @FXML
    ToggleButton autoresponderToggle;

    @FXML
    TextArea emailTemplate;
    @FXML
    TextArea wrongSubject;
    @FXML
    TextArea wrongGroup;
    @FXML
    TextArea wrongTask;

    @FXML
    Button addMasks;

    @FXML
    Button close;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        subjectList.getItems().addAll(GlobalSettings.getInstance().getSubjectsAndGroups().keySet());
        if (subjectList.getItems().size() > 0) {
            subjectList.setValue(subjectList.getItems().get(0));
            groupList.getItems().addAll(GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList.getItems().get(0)));
        }
        if (groupList.getItems().size() > 0) groupList.setValue(groupList.getItems().get(0));

        subjectList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            groupList.getItems().clear();
            groupList.getItems().addAll(GlobalSettings.getInstance().getSubjectsAndGroups().get(newValue));
            if (groupList.getItems().size() > 0) groupList.setValue(groupList.getItems().get(0));
        });

        tableLink.setText(GlobalSettings.getInstance().getResultsTableURL());

        saveLink.setOnAction(event -> {
            Pattern pattern = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
            Matcher matcher = pattern.matcher(tableLink.getText());
            if (matcher.find()) {
                GlobalSettings.getInstance().setResultsTableURL(tableLink.getText());
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Entered incorrect link to the table!");
                alert.showAndWait();
                tableLink.setText("");
            }
        });

        removeSubject.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning!");
            alert.setContentText("Delete selected subject?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                GlobalSettings.getInstance().getSubjectsAndGroups().remove(subjectList.getValue());
                subjectList.getItems().remove(subjectList.getValue());
            }
        });

        removeGroup.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning!");
            alert.setContentText("Delete selected group?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList.getValue()).remove(groupList.getValue());
                groupList.getItems().remove(groupList.getValue());
            }
        });

        addSubject.setOnAction((ActionEvent event) -> {
            try {
                Stage addItemStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AddItem.fxml"));
                Parent root = fxmlLoader.load();
                AddItemController addItemController = fxmlLoader.getController();
                addItemStage.setScene(new Scene(root));
                addItemStage.initModality(Modality.WINDOW_MODAL);
                addItemStage.initOwner(addSubject.getScene().getWindow());
                addItemStage.setTitle("Add subject");
                addItemController.addItemField.setPromptText("Enter the subject here...");
                addItemController.addButton.setOnAction(e -> {
                    GlobalSettings.getInstance().getSubjectsAndGroups().put(addItemController.addItemField.getText(), new ArrayList<>());
                    subjectList.getItems().add(addItemController.addItemField.getText());
                    addItemStage.close();
                });
                addItemStage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ENTER)
                        addItemController.addButton.fire();
                    else if (e.getCode() == KeyCode.ESCAPE)
                        addItemController.cancelButton.fire();
                });
                addItemController.cancelButton.setOnAction(e -> {
                    addItemStage.close();
                });
                addItemController.addButton.disableProperty().bind(Bindings.isEmpty(addItemController.addItemField.textProperty()));
                addItemStage.setResizable(false);
                addItemStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        addGroup.setOnAction(event -> {
            try {
                Stage addItemStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AddItem.fxml"));
                Parent root = fxmlLoader.load();
                AddItemController addItemController = fxmlLoader.getController();
                addItemStage.setScene(new Scene(root));
                addItemStage.initModality(Modality.WINDOW_MODAL);
                addItemStage.initOwner(addSubject.getScene().getWindow());
                addItemStage.setTitle("Add group");
                addItemController.addItemField.setPromptText("Enter the group here...");
                addItemController.addButton.setOnAction(e -> {
                    GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList.getValue()).add(addItemController.addItemField.getText());
                    groupList.getItems().add(addItemController.addItemField.getText());
                    addItemStage.close();
                });
                addItemStage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.ENTER)
                        addItemController.addButton.fire();
                    else if (e.getCode() == KeyCode.ESCAPE)
                        addItemController.cancelButton.fire();
                });
                addItemController.cancelButton.setOnAction(e -> {
                    addItemStage.close();
                });
                addItemController.addButton.disableProperty().bind(Bindings.isEmpty(addItemController.addItemField.textProperty()));
                addItemStage.setResizable(false);
                addItemStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        generateTableLink.setOnAction(event -> {
            try {
                tableLink.setText(GoogleSheetsManager.createSpreadsheet());
                GlobalSettings.getInstance().setResultsTableURL(tableLink.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        emailTemplate.setText(GlobalSettings.getInstance().getAutoresponderTemplate());
        emailSubject.setText(GlobalSettings.getInstance().getAutoresponderEmailSubject());

        emailTemplate.textProperty().addListener((observable, oldValue, newValue) -> GlobalSettings.getInstance().setAutoresponderTemplate(newValue));

        emailSubject.textProperty().addListener((observable, oldValue, newValue) -> GlobalSettings.getInstance().setAutoresponderEmailSubject(newValue));

        close.setOnAction(event -> {
            ((Button)event.getSource()).getScene().getWindow().fireEvent(
                    new WindowEvent(
                            ((Button)event.getSource()).getScene().getWindow(),
                            WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        });
    }
}
