package Controller;

import Model.*;
import View.MainController;
import javafx.application.Platform;

import javax.crypto.NoSuchPaddingException;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
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
        MainController.println("Credentials validation: Start...");
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        try {
            Store store = session.getStore();
            MainController.println("Credentials validation: Trying to connect...");
            store.connect(GlobalSettings.getHostByEmail(email), email, password);
        } catch (MessagingException e) {
            MainController.println("Credentials validation: Failed!");
            return false;
        }
        MainController.println("Credentials validation: Ok!");
        return true;
    }


    /*
    Save all message attachments in data folder
     */
    private static ArrayList<Task> saveMessageAttachments(Message message, HashMap<String, ArrayList<Task>> tasksAndSubjects) throws IOException, MessagingException {
        String emailSubject = message.getSubject();

        // name, group, subject
        String[] emailSubjectSplitted = emailSubject.split(",");
        for (int i = 0; i < emailSubjectSplitted.length; i++) emailSubjectSplitted[i] = emailSubjectSplitted[i].trim();

        HashMap<String, ArrayList<String>> subjectsAndGroups = GlobalSettings.getInstance().getSubjectsAndGroups();

        StringBuilder finalSubject = new StringBuilder();
        String subjectFromEmail = "";
        final AtomicBoolean isCorrectSubject = new AtomicBoolean();

        List<String> subjects = new ArrayList<>(subjectsAndGroups.keySet());
        for (int i = 0; i < emailSubjectSplitted.length; i++) {
            String subject = emailSubjectSplitted[i];
            double subjectPercentEquality = 0;
            for (int j = 0; j < subjects.size(); j++) {
                String comparingSubject = subjects.get(j);
                if (comparingSubject.toLowerCase().equals(subject.toLowerCase())) {
                    isCorrectSubject.set(true);
                    finalSubject.setLength(0);
                    finalSubject.append(comparingSubject);
                    subjectFromEmail = subject;
                    break;
                } else {
                    double percentEquality = (1 - (double) DamerauLevenshteinDistance.compare(subject, comparingSubject) / Math.max(subject.length(), comparingSubject.length()));
                    if (percentEquality > subjectPercentEquality) {
                        finalSubject.setLength(0);
                        finalSubject.append(comparingSubject);
                        subjectFromEmail = subject;
                        subjectPercentEquality = percentEquality;
                    }
                }
            }
            if (isCorrectSubject.get()) break;
        }

        final StringBuilder finalGroup = new StringBuilder();
        String groupFromEmail = "";
        final AtomicBoolean isCorrectGroup = new AtomicBoolean();

        List<String> groups;
        if (isCorrectSubject.get()) {
            groups = subjectsAndGroups.get(finalSubject.toString());
        } else {
            groups = new ArrayList<>();
            subjectsAndGroups.values().forEach(groups::addAll);
        }
        for (int i = 0; i < emailSubjectSplitted.length; i++) {
            String group = emailSubjectSplitted[i].replaceAll(" ", "").replaceAll("А", "A").replaceAll("В", "B").replaceAll("С", "C").replaceAll("Е", "E").replaceAll("Н", "H").replaceAll("К", "K").replaceAll("М", "M").replaceAll("О", "O").replaceAll("Р", "P").replaceAll("Т", "T").replaceAll("Х", "X").replaceAll("У", "Y");
            ;
            double groupPercentEquality = 0;
            for (int j = 0; j < groups.size(); j++) {
                String comparingGroup = groups.get(j).replaceAll(" ", "").replaceAll("А", "A").replaceAll("В", "B").replaceAll("С", "C").replaceAll("Е", "E").replaceAll("Н", "H").replaceAll("К", "K").replaceAll("М", "M").replaceAll("О", "O").replaceAll("Р", "P").replaceAll("Т", "T").replaceAll("Х", "X").replaceAll("У", "Y");
                ;
                if (comparingGroup.toLowerCase().equals(group.toLowerCase())) {
                    isCorrectGroup.set(true);
                    finalGroup.setLength(0);
                    finalGroup.append(groups.get(j));
                    groupFromEmail = emailSubjectSplitted[i];
                    break;
                } else {
                    double percentEquality = (1 - (double) DamerauLevenshteinDistance.compare(group, comparingGroup) / Math.max(group.length(), comparingGroup.length()));
                    if (percentEquality > groupPercentEquality) {
                        finalGroup.setLength(0);
                        finalGroup.append(groups.get(j));
                        groupFromEmail = emailSubjectSplitted[i];
                        groupPercentEquality = percentEquality;
                    }
                }
            }
            if (isCorrectGroup.get()) break;
        }


        StringBuilder finalStudentName = new StringBuilder();
        String studentNameFromEmail = "";
        double studentNamePercentEquality = 0;
        final AtomicBoolean isCorrectStudentName = new AtomicBoolean();

        List<String> studentNames = new ArrayList<>(LocalSettings.getInstance().getResults().stream().filter(result -> !isCorrectSubject.get() || result.getSubject().equals(finalSubject.toString()))
                .filter(result -> !isCorrectGroup.get() || result.getGroup().equals(finalGroup.toString())).map(result -> result.getStudent().getName()).collect(Collectors.toSet()));

        for (int i = 0; i < emailSubjectSplitted.length; i++) {
            String[] nameParts = emailSubjectSplitted[i].toLowerCase().split(" ");
            for (int j = 0; j < nameParts.length; j++) {
                String[] namePartsByDash = nameParts[j].split("-");
                for (int k = 0; k < namePartsByDash.length; k++) {
                    namePartsByDash[k] = namePartsByDash[k].trim();
                    if (namePartsByDash[k].length() > 0)
                        namePartsByDash[k] = namePartsByDash[k].substring(0, 1).toUpperCase() + ((namePartsByDash[k].length() > 1) ? namePartsByDash[k].substring(1) : "");
                }
                String namePart = String.join("-", namePartsByDash);
                nameParts[j] = namePart;
            }
            String studentName = String.join(" ", nameParts);
            for (int j = 0; j < studentNames.size(); j++) {
                String comparingStudentName = studentNames.get(j);
                if (comparingStudentName.toLowerCase().equals(studentName.toLowerCase())) {
                    isCorrectStudentName.set(true);
                    finalStudentName.setLength(0);
                    finalStudentName.append(comparingStudentName);
                    studentNameFromEmail = studentName;
                    break;
                } else {
                    double percentEquality = (1 - (double) DamerauLevenshteinDistance.compare(studentName, comparingStudentName) / Math.max(studentName.length(), comparingStudentName.length()));
                    if (percentEquality > studentNamePercentEquality) {
                        finalStudentName.setLength(0);
                        finalStudentName.append(comparingStudentName);
                        studentNameFromEmail = studentName;
                        studentNamePercentEquality = percentEquality;
                    }
                }
            }
            if (isCorrectStudentName.get()) break;
        }

        if (!isCorrectStudentName.get() || studentNamePercentEquality < 85) {
            ArrayList<String> emailSubjectList = new ArrayList<>(Arrays.asList(emailSubjectSplitted));
            emailSubjectList.remove(subjectFromEmail);
            emailSubjectList.remove(groupFromEmail);
            if (emailSubjectList.size() > 0) {
                studentNameFromEmail = emailSubjectList.get(0);
                String[] nameParts = studentNameFromEmail.toLowerCase().split(" ");
                for (int j = 0; j < nameParts.length; j++) {
                    String[] namePartsByDash = nameParts[j].split("-");
                    for (int k = 0; k < namePartsByDash.length; k++) {
                        namePartsByDash[k] = namePartsByDash[k].trim();
                        if (namePartsByDash[k].length() > 0)
                            namePartsByDash[k] = namePartsByDash[k].substring(0, 1).toUpperCase() + ((namePartsByDash[k].length() > 1) ? namePartsByDash[k].substring(1) : "");
                    }
                    String namePart = String.join("-", namePartsByDash);
                    nameParts[j] = namePart;
                }
                finalStudentName.setLength(0);
                finalStudentName.append(String.join(" ", nameParts));
            } else finalStudentName.setLength(0);
        }

        CountDownLatch emailHandlerLatch = new CountDownLatch(1);

        EmailHandlerData emailHandlerData = new EmailHandlerData(emailSubject, finalSubject, finalGroup, finalStudentName, emailHandlerLatch, message.getReceivedDate(), message.getFrom());

        if (!isCorrectSubject.get() || !isCorrectGroup.get() || !isCorrectStudentName.get()) {
            Platform.runLater(() -> {
                MainController.showEmailHandlerWindow(emailHandlerData);
            });
        } else {
            emailHandlerLatch.countDown();
        }
        try {
            emailHandlerLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (emailHandlerData.isReply()) {
            reply(emailHandlerData);
        }

        if (emailHandlerData.isSpam()) {
            toSpam(message);
        }

        if (emailHandlerData.isSkip())
            return new ArrayList<>();
        else {
            String studentNameData = emailHandlerData.getStudentName().toString();
            String subjectData = emailHandlerData.getSubject().toString().replaceAll(" ", "_");
            String groupData = emailHandlerData.getGroup().toString().replaceAll(" ", "");
            String contentType = message.getContentType();
            ArrayList<Task> tasks = new ArrayList<>();
            if (contentType.contains("multipart")) {
                Multipart multiPart = (Multipart) message.getContent();
                for (int i = 0; i < multiPart.getCount(); i++) {
                    MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        if (tasksAndSubjects.containsKey(emailHandlerData.getSubject().toString()) && tasksAndSubjects.get(emailHandlerData.getSubject().toString()).stream().anyMatch(task -> {
                            try {
                                return task.getName().equals(part.getFileName().substring(0, part.getFileName().lastIndexOf(".")));
                            } catch (MessagingException e) {
                                e.printStackTrace();
                            }
                            return false;
                        })) {
                            String folder = GlobalSettings.getDataFolder() + "/" + Transliteration.cyr2lat(subjectData) + "/" + Transliteration.cyr2lat(groupData) + "/" + Transliteration.cyr2lat(studentNameData.replaceAll(" ", "_")) + "/" + (new SimpleDateFormat(GlobalSettings.getSourcesDateFormat())).format(message.getReceivedDate());
                            File dir = new File(folder);
                            dir.mkdirs();
                            File f = new File(folder + "/" + part.getFileName());
                            part.saveFile(f);
                            Task task = new Task(part.getFileName(), subjectData, f.getAbsolutePath(), message.getReceivedDate());
                            task.setAuthor(new Student(studentNameData, groupData));
                            tasks.add(task);
                        }
                    }
                }
            }
            return tasks;
        }
    }

    private static void toSpam(Message message) throws MessagingException {
        message.setFlag(Flags.Flag.DELETED, true);
    }

    private static void reply(EmailHandlerData emailHandlerData) throws MessagingException {
        messagesForReply.add(emailHandlerData);
    }

    private static ArrayList<EmailHandlerData> messagesForReply;
    private static CountDownLatch replyMessagesLatch;

    /*
    Retrieve messages data from inbox folder
     */
    static void retrieveMessagesData() throws IOException, MessagingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        Store store = session.getStore();
        store.connect(GlobalSettings.getHostImap(), GlobalSettings.getEmail(), GlobalSettings.getPassword());
        Folder inbox = store.getFolder("INBOX");
        Message[] messages = receiveEmails(inbox);
        Date lastDateEmailChecking = LocalSettings.getInstance().getLastDateEmailChecked();
        Date newDateEmailChecking = new Date();
        downloadedMessagesCount = new CountDownLatch(messages.length);
        localTests = new ConcurrentHashMap<>();
        messagesForReply = new ArrayList<>();
        replyMessagesLatch = new CountDownLatch(1);
        new Thread(Thread.currentThread().getThreadGroup(), () -> {
            try {
                replyMessagesLatch.await();

                Properties propsSmtp = new Properties();
                propsSmtp.put("mail.smtp.auth", "true");
                propsSmtp.put("mail.smtp.starttls.enable", "true");
                propsSmtp.put("mail.smtp.host", GlobalSettings.getHostSmtp());

                // Get the Session object.
                Session smtpSession = Session.getInstance(propsSmtp,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(GlobalSettings.getEmail(), GlobalSettings.getPassword());
                            }
                        });

                messagesForReply.forEach(emailHandlerData -> {
                    try {
                        Message message = new MimeMessage(smtpSession);
                        message.setFrom(new InternetAddress(GlobalSettings.getEmail()));
                        message.setRecipients(Message.RecipientType.TO, emailHandlerData.getReceivedFrom());
                        message.setSubject(emailHandlerData.getReplySubject());
                        message.setText(emailHandlerData.getReplyText());
                        Transport.send(message);
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        HashMap<String, ArrayList<Task>> subjectsAndTasks = LocalSettings.getInstance().getSubjectsAndTasks();
        subjectsAndTasks.keySet().forEach(key -> subjectsAndTasks.get(key).forEach(value -> {
            if (!localTests.containsKey(value))
                localTests.put(value, value.getTestContents());
        }));

        HashMap<String, ArrayList<Task>> tasksAndSubjects = GoogleDriveManager.getTasksAndSubjects();

        Arrays.stream(messages).forEach(message -> {
            try {
                if (lastDateEmailChecking.compareTo(message.getReceivedDate()) < 0) {
                    (new Thread(Thread.currentThread().getThreadGroup(), () -> {
                        try {
                            ArrayList<Task> attachments = saveMessageAttachments(message, tasksAndSubjects);
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
                                    MainController.println("Task " + att.getName() + " added (" + ((new Date()).getTime() - General.getStartDate().getTime()) + " ms).");
                                } else {
                                    ExponentialBackOff.execute(() -> {
                                        ArrayList<Test> cTests = GoogleDriveManager.getTests(att);
                                        localTests.put(att, cTests);
                                        att.setTestContents(cTests);
                                        MainController.println("Task " + att.getName() + " added (" + ((new Date()).getTime() - General.getStartDate().getTime()) + " ms).");
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
                                // TODO Delete that block before deploy
                                {
                                    // Left this way for testing purposes.
                                    LocalSettings.getInstance().setLastDateEmailChecked(new Date(0L));
                                    //LocalSettings.getInstance().getResults().clear();
                                }
                                /*
                                TODO uncomment that before deploy
                                LocalSettings.getInstance().setLastDateEmailChecked(newDateEmailChecking);
                                 */
                                LocalSettings.getInstance().saveSettings();
                                inbox.close(false);
                                replyMessagesLatch.countDown();
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
