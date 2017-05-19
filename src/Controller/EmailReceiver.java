package Controller;

import Model.Settings;
import Model.Student;
import Model.Task;
import Model.Test;

import javax.crypto.NoSuchPaddingException;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

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

    private static CountDownLatch downloadedMessagesCount;

    private static ConcurrentHashMap<Task, ArrayList<Test>> localTests;

    /*
    Receive all unread emails from teacher email
     */
    private static Message[] receiveEmails(Folder inbox) throws MessagingException {
        inbox.open(Folder.READ_WRITE);
        return inbox.getMessages();
    }

    public static boolean validate(String email, String password) {
        System.out.println("Credentials validation: Start...");
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        try {
            Store store = session.getStore();
            System.out.println("Credentials validation: Trying to connect...");
            store.connect(Settings.getHostByEmail(email), email, password);
        } catch (MessagingException e) {
            System.out.println("Credentials validation: Failed!");
            return false;
        }
        System.out.println("Credentials validation: Ok!");
        return true;
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
        String contentType = message.getContentType();
        ArrayList<Task> tasks = new ArrayList<>();
        if (contentType.contains("multipart")) {
            Multipart multiPart = (Multipart) message.getContent();
            for (int i = 0; i < multiPart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    String folder = Settings.getDataFolder() + "/" + Transliteration.cyr2lat(subject[2]) + "/" + Transliteration.cyr2lat(subject[1]) + "/" + Transliteration.cyr2lat(subject[0]) + "/" + (new SimpleDateFormat(Settings.getSourcesDateFormat())).format(message.getReceivedDate());
                    File dir = new File(folder);
                    dir.mkdirs();
                    File f = new File(folder + "/" + part.getFileName());
                    part.saveFile(f);
                    Task task = new Task(part.getFileName(), subject[2], f.getAbsolutePath(), message.getReceivedDate());
                    task.setAuthor(new Student(fullName, subject[1]));
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /*
    Retrieve messages data from inbox folder
     */
    static void retrieveMessagesData() throws IOException, MessagingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        Store store = session.getStore();
        store.connect(Settings.getInstance().getHost(), Settings.getEmail(), Settings.getPassword());
        Folder inbox = store.getFolder("INBOX");
        Message[] messages = receiveEmails(inbox);
        Date lastDateEmailChecking = Settings.getInstance().getLastDateEmailChecked();
        System.out.println(lastDateEmailChecking.toString());
        Date newDateEmailChecking = new Date();
        downloadedMessagesCount = new CountDownLatch(messages.length);
        localTests = new ConcurrentHashMap<>();
        Arrays.stream(messages).forEach(message -> {
            try {
                if (lastDateEmailChecking.compareTo(message.getReceivedDate()) < 0) {
                    (new Thread(Thread.currentThread().getThreadGroup(), () -> {
                        try {
                            ArrayList<Task> attachments = saveMessageAttachments(message);
                            attachments.forEach(att -> (new Thread(Thread.currentThread().getThreadGroup(), () -> {
                                if (localTests.containsKey(att)) {
                                    att.setTestContents(localTests.get(att));
                                    Task auxiliaryTask = localTests.keySet().stream().filter(key -> key.equals(att)).collect(Collectors.toList()).get(0);
                                    att.setTestFields(auxiliaryTask.getTimeInMS(),
                                            auxiliaryTask.shouldBeCheckedForAntiPlagiarism(),
                                            auxiliaryTask.getDeadline(),
                                            auxiliaryTask.getTaskCode(),
                                            auxiliaryTask.hasHardDeadline());
                                    if (att.getName().endsWith(".hs")) {
                                        General.getHaskellTasksQueue().add(att);
                                    } else {
                                        General.getJavaTasksQueue().add(att);
                                    }
                                    System.out.println("Task " + att.getName() + " added (" + ((new Date()).getTime() - General.getStartDate().getTime()) + " s).");
                                } else {
                                    ExponentialBackOff.execute(() -> {
                                        ArrayList<Test> cTests = GoogleDriveManager.getTests(att);
                                        localTests.put(att, cTests);
                                        att.setTestContents(cTests);
                                        System.out.println("Task " + att.getName() + " added (" + ((new Date()).getTime() - General.getStartDate().getTime()) + " s).");
                                        if (att.getName().endsWith(".hs")) {
                                            General.getHaskellTasksQueue().add(att);
                                        } else {
                                            General.getJavaTasksQueue().add(att);
                                        }
                                        return null;
                                    });
                                }
                            })).start());
                            downloadedMessagesCount.countDown();
                            if (downloadedMessagesCount.getCount() == 0) {
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
    }

}
