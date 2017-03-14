package Controller;
import Model.Settings;

import javax.crypto.NoSuchPaddingException;
import javax.mail.*;

import Model.Attachment;
import Model.Task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Alexander Baranov on 03.03.2017.
 *
 * Receive messages from e-mail, retrieve and save all messages attachments by the next hierarchy:
 *     - subject 1/
 *          - group 1/
 *              - student name 1/
 *                  - task 1
 *                  ...
 *              - student name 2/
 *                  ...
 *          - group 2/
 *              ...
 *      - subject 2/
 *          ...
 */
public class EmailReceiver {

    /*
    Receive all unread emails from teacher email
     */
    private static Message[] receiveEmails(Folder inbox) throws MessagingException {
        inbox.open(Folder.READ_WRITE);
        return inbox.getMessages();
    }

    /*
    Save all message attachments in data folder
     */
    private static ArrayList<Task> saveMessageAttachments(Message message) throws IOException, MessagingException {
        // name, group, subject
        String[] subject = message.getSubject().split(",");
        for (int i = 0; i < subject.length; i++) {
            subject[i] = subject[i].trim();
        }
        if (subject.length != 3) {
            return new ArrayList<>();
        }
        ArrayList<Attachment> attachments = retrieveAttachments(message);
        ArrayList<Task> tasks = new ArrayList<>();
        attachments.forEach(attachment -> {
            File f = new File(Settings.getInstance().getDataFolder() + "/" + subject[2] + "/" + subject[1] + "/" + subject[0] + "/" + attachment.getFileName());
            f.mkdirs();
            try {
                Files.copy(attachment.getStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                attachment.getStream().close();
                tasks.add(new Task(attachment.getFileName(), subject[2], f.getAbsolutePath()));
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return tasks;
    }

    /*
    Retrieve messages data from inbox folder
     */
    public static ArrayList<Task> retrieveMessagesData() throws IOException, MessagingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        Store store = session.getStore();
        store.connect(Settings.getInstance().getHost(), Settings.getInstance().getEmail(), Settings.getInstance().getPassword());
        Folder inbox = store.getFolder("INBOX");
        Message[] messages = receiveEmails(inbox);

        Date lastDateEmailChecking = Settings.getInstance().getLastDateEmailChecked();
        System.out.println(lastDateEmailChecking.toString());
        Date newDateEmailChecking = new Date();
        ArrayList<Task> tasks = new ArrayList<>();
        Arrays.stream(messages).forEach(message -> {
            try {
                if (lastDateEmailChecking.compareTo(message.getReceivedDate()) < 0)
                    tasks.addAll(saveMessageAttachments(message));
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
        Settings.getInstance().setLastDateEmailChecked(newDateEmailChecking);
        Settings.getInstance().saveSettings();

        inbox.close(false);
        return tasks;
    }

    /*
    Retrieve attachments from message
     */
    private static ArrayList<Attachment> retrieveAttachments(Message message) throws IOException, MessagingException {
        Object content = message.getContent();
        ArrayList<Attachment> result = new ArrayList<>();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                result.addAll(retrieveAttachments(multipart.getBodyPart(i)));
            }
        }

        return result;
    }

    /*
    Retrieve attachments from message
     */
    private static ArrayList<Attachment> retrieveAttachments(BodyPart part) throws IOException, MessagingException {
        ArrayList<Attachment> result = new ArrayList<>();
        Object content = part.getContent();
        if (content instanceof InputStream || content instanceof String) {
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getFileName() != null && !part.getFileName().isEmpty())) {
                result.add(new Attachment(part.getFileName(), part.getInputStream()));
            }
        }

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                result.addAll(retrieveAttachments(bodyPart));
            }
        }

        return result;
    }
}
