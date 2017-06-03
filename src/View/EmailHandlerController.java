package View;

import Model.EmailHandlerData;
import Model.GlobalSettings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
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
    public Button spamButton;

    @FXML
    public TextField emailSubject;

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

    @FXML
    public Button replyButton;


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
            ((Button)event.getSource()).getScene().getWindow().fireEvent(
                    new WindowEvent(
                            ((Button)event.getSource()).getScene().getWindow(),
                            WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        });

        this.skipButton.setOnAction(event -> {
            ((Button)event.getSource()).getScene().getWindow().fireEvent(
                    new WindowEvent(
                            ((Button)event.getSource()).getScene().getWindow(),
                            WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        });

        this.spamButton.setOnAction(event -> {
            this.emailHandlerData.setSpam(true);
            ((Button)event.getSource()).getScene().getWindow().fireEvent(
                    new WindowEvent(
                            ((Button)event.getSource()).getScene().getWindow(),
                            WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        });

        this.replyButton.setOnAction(event -> {
            if (emailHandlerData.isReply()) {
                emailHandlerData.setReply(false);
                this.replyButton.setText("Reply");
            }
            else {
                Stage replyFrame = new Stage();
                FXMLLoader replyLoader = new FXMLLoader(getClass().getResource("Reply.fxml"));
                try {
                    emailHandlerData.setStudentName(new StringBuilder(nameField.getText()));
                    emailHandlerData.setSubject(new StringBuilder(subjectsList.getValue().toString()));
                    emailHandlerData.setGroup(new StringBuilder(groupsList.getValue().toString()));
                    Parent replyRoot = replyLoader.load();
                    ((ReplyController) replyLoader.getController()).setData(emailHandlerData);
                    replyFrame.setScene(new Scene(replyRoot));
                    replyFrame.setResizable(false);
                    replyFrame.initModality(Modality.WINDOW_MODAL);
                    replyFrame.initOwner(this.replyButton.getScene().getWindow());
                    replyFrame.setOnCloseRequest(onCloseEvent -> {
                        if (emailHandlerData.isReply()) {
                            this.replyButton.setText("Cancel reply");
                        }
                    });
                    replyFrame.setTitle("Reply");
                    replyFrame.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
