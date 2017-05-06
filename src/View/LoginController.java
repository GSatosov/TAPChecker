package View;

import Controller.EmailReceiver;
import Model.Settings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginIndicator.setVisible(false);
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
                    System.out.println("Failed: " + email + ", " + password);
                    login.setDisable(false);
                    loginIndicator.setVisible(false);
                    return;
                }
                Settings.getInstance().setEmail(email);
                Settings.getInstance().setPassword(password);
                try {
                    Settings.getInstance().saveSettings();
                    pwField.clear();
                    loginIndicator.setVisible(false);
                    login.setDisable(false);
                    Platform.runLater(() -> MainFrame.setStagetoMain());
                } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
                    loginIndicator.setVisible(false);
                    login.setDisable(false);
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
