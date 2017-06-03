package View;

import Model.EmailHandlerData;
import Model.GlobalSettings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.WindowEvent;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Created by Alexander Baranov on 03.06.2017.
 */
public class ReplyController implements Initializable {

    @FXML
    public TextArea emailBody;

    @FXML
    public Button reply;

    @FXML
    public Button cancel;
    public TextField emailSubject;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        cancel.setOnAction(event -> {
            ((Button)event.getSource()).getScene().getWindow().fireEvent(
                    new WindowEvent(
                            ((Button)event.getSource()).getScene().getWindow(),
                            WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        });

    }

    public void setData(EmailHandlerData emailHandlerData) {
        Date currentDate = new Date();
        String body = GlobalSettings.getInstance().getAutoresponderTemplate()
                .replaceAll(Pattern.quote("$emailSubject"), emailHandlerData.getEmailSubject())
                .replaceAll(Pattern.quote("$subject"), emailHandlerData.getSubject().toString())
                .replaceAll(Pattern.quote("$group"), emailHandlerData.getGroup().toString())
                .replaceAll(Pattern.quote("$name"), emailHandlerData.getStudentName().toString())
                .replaceAll(Pattern.quote("$date"), (new SimpleDateFormat("dd.MM.yyyy HH:mm")).format(emailHandlerData.getReceivedDate()))
                .replaceAll(Pattern.quote("$currentDate"), (new SimpleDateFormat("dd.MM.yyyy")).format(currentDate))
                .replaceAll(Pattern.quote("$currentTime"), (new SimpleDateFormat("HH:mm")).format(currentDate));

        emailBody.setText(body);

        String subject = GlobalSettings.getInstance().getAutoresponderEmailSubject()
                .replaceAll(Pattern.quote("$emailSubject"), emailHandlerData.getEmailSubject())
                .replaceAll(Pattern.quote("$subject"), emailHandlerData.getSubject().toString())
                .replaceAll(Pattern.quote("$group"), emailHandlerData.getGroup().toString())
                .replaceAll(Pattern.quote("$name"), emailHandlerData.getStudentName().toString())
                .replaceAll(Pattern.quote("$date"), (new SimpleDateFormat("dd.MM.yyyy HH:mm")).format(emailHandlerData.getReceivedDate()))
                .replaceAll(Pattern.quote("$currentDate"), (new SimpleDateFormat("dd.MM.yyyy")).format(currentDate))
                .replaceAll(Pattern.quote("$currentTime"), (new SimpleDateFormat("HH:mm")).format(currentDate));

        emailSubject.setText(subject);

        reply.setOnAction(event -> {
            emailHandlerData.setReply(true);
            emailHandlerData.setReplyText(emailBody.getText());
            emailHandlerData.setReplySubject(emailSubject.getText());
            ((Button)event.getSource()).getScene().getWindow().fireEvent(
                    new WindowEvent(
                            ((Button)event.getSource()).getScene().getWindow(),
                            WindowEvent.WINDOW_CLOSE_REQUEST
                    )
            );
        });


    }
}
