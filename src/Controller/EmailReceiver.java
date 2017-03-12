package Controller;
import Model.Constants;
import javax.mail.*;
import javax.mail.search.FlagTerm;
import Model.Attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static Message[] receiveUnreadEmails(Folder inbox) throws MessagingException {
        inbox.open(Folder.READ_WRITE);
        return inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
    }

    /*
    Save all message attachments in data folder
     */
    private static void saveMessageAttachments(Message message) throws IOException, MessagingException {
        // name, group, subject
        String[] subject = message.getSubject().split(",");

        System.out.println(subject.length);
        if (subject.length != 3) {
            return;
            //throw new Exception("There is an error in message subject!");
        }
        ArrayList<Attachment> attachments = retrieveAttachments(message);
        attachments.forEach(attachment -> {
            File f = new File(Constants.DATA_FOLDER + "/" + subject[2].trim() + "/" + subject[1].trim() + "/" + subject[0].trim() + "/" + attachment.getFileName());
            f.mkdirs();
            try {
                Files.copy(attachment.getStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                attachment.getStream().close();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /*
    Retrieve messages data from inbox folder
     */
    public static void retrieveMessagesData() throws IOException, MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        Store store = session.getStore();
        store.connect(Constants.HOST, Constants.EMAIL, Constants.PASSWORD);
        Folder inbox = store.getFolder("INBOX");
        Message[] messages = receiveUnreadEmails(inbox);
        Arrays.stream(messages).forEach(message -> {
            try {
                saveMessageAttachments(message);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });

        inbox.close(false);
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
