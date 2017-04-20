package View;

import Model.Settings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private Button login;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        login.setOnAction(event ->  {
            Settings.getInstance().setEmail(emailField.getText());
            Settings.getInstance().setPassword(pwField.getText());
            try {
                Settings.getInstance().saveSettings();
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
                e.printStackTrace();
            }
            MainFrame.getPrimaryStage().setScene(MainFrame.getMainScene());
        });
    }
}
