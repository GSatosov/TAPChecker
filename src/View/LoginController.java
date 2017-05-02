package View;

import Controller.EmailReceiver;
import Controller.GoogleDriveManager;
import Model.Settings;
import com.google.api.client.auth.oauth2.Credential;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mailServer.setItems(FXCollections.observableArrayList("@gmail.com", "@mail.ru"));
        mailServer.setValue(mailServer.getItems().get(0));

        login.setOnAction(event -> {
            login.setDisable(true);
            String email = emailField.getText() + mailServer.getValue();
            String password = pwField.getText();
            if (!EmailReceiver.validate(email, password)) {
                login.setDisable(false);
                return;
            }
            Settings.getInstance().setEmail(email);
            Settings.getInstance().setPassword(password);
            try {
                Settings.getInstance().saveSettings();
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
                e.printStackTrace();
            }
            pwField.clear();
            login.setDisable(false);
            MainFrame.getPrimaryStage().setScene(MainFrame.getMainScene());
        });
    }
}
