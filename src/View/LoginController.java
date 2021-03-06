package View;

import Controller.EmailReceiver;
import Controller.GoogleDriveManager;
import Model.GlobalSettings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class LoginController implements Initializable {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField pwField;
    @FXML
    private ChoiceBox<String> mailServer;

    @FXML
    private Button login;
    @FXML
    private ProgressIndicator loginIndicator;

    private String getHostName(String email) {
        return email.substring(0, email.lastIndexOf("@"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginIndicator.setVisible(false);
        if (!GlobalSettings.getInstance().getEmail().isEmpty())
            emailField.setText(getHostName(GlobalSettings.getInstance().getEmail()));
        mailServer.setItems(FXCollections.observableArrayList("@gmail.com", "@mail.ru"));
        mailServer.setValue(mailServer.getItems().get(0));

        MainFrame.getPrimaryStage().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (mailServer.isFocused()) {
                    mailServer.show();
                } else {
                    login.fire();
                    event.consume();
                }
            }
        });

        login.setOnAction(event -> {
            login.setDisable(true);
            loginIndicator.setVisible(true);
            new Thread(() -> {
                String email = emailField.getText() + mailServer.getValue();
                String password = pwField.getText();
                if (!EmailReceiver.validate(email, password)) {
                    MainController.println("Failed: " + email + ", " + password);
                    login.setDisable(false);
                    loginIndicator.setVisible(false);
                    return;
                }
                try {
                    GoogleDriveManager.authorize();
                } catch (IOException e) {
                    MainController.println("Failed: Google credentials required.");
                    login.setDisable(false);
                    loginIndicator.setVisible(false);
                    return;
                }
                GlobalSettings.setEmail(email);
                GlobalSettings.setPassword(password);
                pwField.clear();
                loginIndicator.setVisible(false);
                login.setDisable(false);
                Platform.runLater(() -> MainFrame.setStagetoMain());
            }).start();
        });
    }
}
