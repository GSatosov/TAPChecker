package Controller;

import Model.*;

import javax.crypto.NoSuchPaddingException;
import javax.mail.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Alexander Baranov on 03.03.2017.
 * <p>
 * Receive messages from e-mail, retrieve and save all messages attachments by the next hierarchy:
 * - subject 1/
 * - group 1/
 * - student name 1/
 * - task 1
 * ...
 * - student name 2/
 * ...
 * - group 2/
 * ...
 * - subject 2/
 * ...
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
        for (int i = 0; i < subject.length; i++)
            subject[i] = subject[i].trim();
        if (subject.length != 3)
            return new ArrayList<>();
        String fullName = subject[0];
        {
            // edit name
            String[] name = subject[0].toLowerCase().split(" ");
            subject[0] = "";
            subject[0] = name[0].substring(0, 1).toUpperCase() + name[0].substring(1) + "_" + name[1].substring(0, 1).toUpperCase() + name[1].substring(1);
        }
        {
            // edit group
            subject[1] = subject[1].toUpperCase();
            subject[1] = subject[1].replaceAll("А", "A").replaceAll("В", "B").replaceAll("С", "C").replaceAll("Е", "E").replaceAll("Н", "H").replaceAll("К", "K").replaceAll("М", "M").replaceAll("О", "O").replaceAll("Р", "P").replaceAll("Т", "T").replaceAll("Х", "X").replaceAll("У", "Y");
        }
        {
            // edit subject
            subject[2] = subject[2].toLowerCase().replaceAll(" ", "_");
            subject[2] = subject[2].substring(0, 1).toUpperCase() + subject[2].substring(1);
        }
        ArrayList<Attachment> attachments = retrieveAttachments(message);
        ArrayList<Task> tasks = new ArrayList<>();
        attachments.forEach(attachment -> {
            File f = new File(Settings.getInstance().getDataFolder() + "/" + subject[2] + "/" + subject[1] + "/" + subject[0] + "/" + attachment.getFileName());
            f.mkdirs();
            try {
                synchronized (fileSystemSemaphore) {
                    ExponentialBackOff.execute(() -> Files.copy(attachment.getStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING), 5);
                }
                attachment.getStream().close();
                tasks.add(new Task(attachment.getFileName(), subject[2], f.getAbsolutePath()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        tasks.forEach(task -> task.setAuthor(new Student(fullName, subject[1])));
        return tasks;
    }

    private static Object fileSystemSemaphore;

    /*
    Retrieve messages data from inbox folder
     */
    static ArrayList<Task> retrieveMessagesData() throws IOException, MessagingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
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
        downloadedMessagesCount = new AtomicInteger();
        localTests = new ConcurrentHashMap<>();
        fileSystemSemaphore = new Object();
        Arrays.stream(messages).forEach(message -> {
            try {
                if (lastDateEmailChecking.compareTo(message.getReceivedDate()) < 0) {
                    (new Thread(Thread.currentThread().getThreadGroup(), () -> {
                        try {
                            ArrayList<Task> attachments = saveMessageAttachments(message);
                            attachments.forEach(att -> {
                                (new Thread(Thread.currentThread().getThreadGroup(), () -> {
                                    if (localTests.containsKey(att.getName())) {
                                        att.setTestContents(localTests.get(att.getName()));
                                        General.getTasksQueue().add(att);
                                        System.out.println("Task " + att.getName() + " added (" + ((new Date()).getTime() - General.getStartDate().getTime()) + " s).");
                                    }
                                    else {
                                            ExponentialBackOff.execute(() -> {
                                                ArrayList<Test> cTests = GoogleDriveManager.getTests(att);
                                                localTests.put(att.getName(), cTests);
                                                att.setTestContents(cTests);
                                                System.out.println("Task " + att.getName() + " added (" + ((new Date()).getTime() - General.getStartDate().getTime()) + " s).");
                                                General.getTasksQueue().add(att);
                                                return null;
                                            });
                                    }
                                })).start();
                            });
                            if (downloadedMessagesCount.incrementAndGet() == messages.length) {
                                Settings.getInstance().setLastDateEmailChecked(new Date(0L));  // Left this way for testing purposes.
                                Settings.getInstance().saveSettings();
                                inbox.close(false);
                            }
                        } catch (IOException | MessagingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                            e.printStackTrace();
                        }
                    })).start();
                }
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });

        return tasks;
    }

    private static AtomicInteger downloadedMessagesCount;
    private static ConcurrentHashMap<String, ArrayList<Test>> localTests;
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
