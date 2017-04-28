package View;

import Model.Settings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

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

        login.setOnAction(event ->  {
            Settings.getInstance().setEmail(emailField.getText() + mailServer.getValue());
            Settings.getInstance().setPassword(pwField.getText());
            try {
                Settings.getInstance().saveSettings();
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
                e.printStackTrace();
            }
            pwField.clear();
            MainFrame.getPrimaryStage().setScene(MainFrame.getMainScene());
        });
    }
}
