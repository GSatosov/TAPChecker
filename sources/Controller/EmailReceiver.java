package Controller;
import Model.Constants;
import javax.mail.*;
import javax.mail.search.FlagTerm;
import Model.Attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    private static Message[] receiveUnreadEmails(Folder inbox) {
        try {
            inbox.open(Folder.READ_WRITE);
            return inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return new Message[0];
    }

    /*
    Save all message attachments in data folder
     */
    private static void saveMessageAttachments(Message message) {
        try {
            // name, group, subject
            String[] subject = message.getSubject().split(",");

            System.out.println(subject.length);
            if (subject.length != 3) throw new Exception("There is an error in message subject!");
            ArrayList<Attachment> attachments = retrieveAttachments(message);
            attachments.forEach(attachment -> {
                File f = new File(Constants.dataFolder + "/" + subject[2].trim() + "/" + subject[1].trim() + "/" + subject[0].trim() + "/" + attachment.getFileName());
                f.mkdirs();
                try {
                    Files.copy(attachment.getStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    attachment.getStream().close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Retrieve messages data from inbox folder
     */
    public static void retrieveMessagesData() {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            Session session = Session.getInstance(props);
            Store store = session.getStore();
            store.connect(Constants.host, Constants.email, Constants.password);
            Folder inbox = store.getFolder("INBOX");

            Message[] messages = receiveUnreadEmails(inbox);
            Arrays.stream(messages).forEach(message -> saveMessageAttachments(message));

            inbox.close(false);
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    /*
    Retrieve attachments from message
     */
    private static ArrayList<Attachment> retrieveAttachments(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String)
            return new ArrayList<>();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            ArrayList<Attachment> result = new ArrayList<>();

            for (int i = 0; i < multipart.getCount(); i++) {
                result.addAll(retrieveAttachments(multipart.getBodyPart(i)));
            }
            return result;

        }
        return new ArrayList<>();
    }

    /*
    Retrieve attachments from message
     */
    private static ArrayList<Attachment> retrieveAttachments(BodyPart part) throws Exception {
        ArrayList<Attachment> result = new ArrayList<>();
        Object content = part.getContent();
        if (content instanceof InputStream || content instanceof String) {
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getFileName() != null && !part.getFileName().isEmpty())) {
                result.add(new Attachment(part.getFileName(), part.getInputStream()));
                return result;
            } else {
                return new ArrayList<>();
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
