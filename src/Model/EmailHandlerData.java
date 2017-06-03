package Model;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.util.Date;
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
    private Date receivedDate;
    private Address[] receivedFrom;

    private String replyText;
    private String replySubject;

    private boolean skip = true;
    private boolean spam = false;
    private boolean reply = false;

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

    public boolean isSkip() {
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

    public void setStudentName(StringBuilder studentName) {
        this.studentName = studentName;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public EmailHandlerData(String emailSubject, StringBuilder subject, StringBuilder group, StringBuilder studentName, CountDownLatch emailHandlerLatch, Date receivedDate, Address[] receivedFrom) {
        this.emailSubject = emailSubject;
        this.subject = subject;
        this.group = group;
        this.studentName = studentName;
        this.emailHandlerLatch = emailHandlerLatch;
        this.receivedDate = receivedDate;
        this.receivedFrom = receivedFrom;
    }

    public boolean isSpam() {
        return spam;
    }

    public void setSpam(boolean spam) {
        this.spam = spam;
    }

    public boolean isReply() {
        return reply;
    }

    public void setReply(boolean reply) {
        this.reply = reply;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public Address[] getReceivedFrom() {
        return receivedFrom;
    }

    public void setReceivedFrom(Address[] receivedFrom) {
        this.receivedFrom = receivedFrom;
    }

    public String getReplySubject() {
        return replySubject;
    }

    public void setReplySubject(String replySubject) {
        this.replySubject = replySubject;
    }
}
