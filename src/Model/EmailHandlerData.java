package Model;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Alexander Baranov on 02.06.2017.
 */
public class EmailHandlerData {

    private String emailSubject;
    private StringBuilder subject;
    private StringBuilder group;
    private StringBuilder studentName;
    private CountDownLatch emailHandlerLatch;

    private boolean skip = true;

    public String getEmailSubject() {
        return emailSubject;
    }

    public StringBuilder getSubject() {
        return subject;
    }

    public StringBuilder getGroup() {
        return group;
    }

    public StringBuilder getStudentName() {
        return studentName;
    }

    public boolean getSkip() {
        return this.skip;
    }

    public CountDownLatch getEmailHandlerLatch() {
        return this.emailHandlerLatch;
    }

    public void setEmailHandlerLatch(CountDownLatch emailHandlerLatch) {
        this.emailHandlerLatch = emailHandlerLatch;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public void setSubject(StringBuilder subject) {
        this.subject = subject;
    }

    public void setGroup(StringBuilder group) {
        this.group = group;
    }

    public  void setStudentName(StringBuilder studentName) {
        this.studentName = studentName;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public EmailHandlerData(String emailSubject, StringBuilder subject, StringBuilder group, StringBuilder studentName, CountDownLatch emailHandlerLatch) {
        this.emailSubject = emailSubject;
        this.subject = subject;
        this.group = group;
        this.studentName = studentName;
        this.emailHandlerLatch = emailHandlerLatch;
    }

}
