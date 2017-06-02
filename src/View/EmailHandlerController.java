package View;

import Model.EmailHandlerData;
import Model.GlobalSettings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Created by Alexander Baranov on 02.06.2017.
 */
public class EmailHandlerController implements Initializable {

    @FXML
    private TextField emailSubject;

    @FXML
    public ChoiceBox subjectsList;

    @FXML
    public ChoiceBox groupsList;

    @FXML
    public TextField nameField;

    @FXML
    public Button continueButton;

    @FXML
    public Button skipButton;


    private EmailHandlerData emailHandlerData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setUserData(EmailHandlerData emailHandlerData) {
        this.emailHandlerData = emailHandlerData;
        emailSubject.setText(this.emailHandlerData.getEmailSubject());
        HashMap<String, ArrayList<String>> subjectsAndGroups = GlobalSettings.getInstance().getSubjectsAndGroups();

        subjectsList.getItems().clear();
        subjectsList.getItems().addAll(subjectsAndGroups.keySet());

        subjectsList.setValue(this.emailHandlerData.getSubject().toString());

        subjectsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            groupsList.getItems().clear();
            groupsList.getItems().addAll(subjectsAndGroups.get(newValue.toString()));
            groupsList.setValue(groupsList.getItems().get(0));
        });

        groupsList.getItems().clear();
        groupsList.getItems().addAll(subjectsAndGroups.get(subjectsList.getValue().toString()));
        groupsList.setValue(this.emailHandlerData.getGroup().toString());

        nameField.setText(this.emailHandlerData.getStudentName().toString());

        this.continueButton.setOnAction(event -> {
            this.emailHandlerData.setSubject(new StringBuilder(subjectsList.getValue().toString()));
            this.emailHandlerData.setGroup(new StringBuilder(groupsList.getValue().toString()));
            this.emailHandlerData.setStudentName(new StringBuilder(nameField.getText()));
            this.emailHandlerData.setSkip(false);
            this.emailHandlerData.getEmailHandlerLatch().countDown();
            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
        });

        this.skipButton.setOnAction(event -> {
            this.emailHandlerData.getEmailHandlerLatch().countDown();
            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
        });

    }

}
