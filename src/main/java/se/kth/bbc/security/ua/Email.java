package se.kth.bbc.security.ua;

import java.util.Date;
import javax.mail.*;
import javax.mail.internet.*;
import javax.annotation.Resource;
import javax.ejb.Stateless;
/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class Email {

    @Resource(lookup = "mail/BBCMail")
    private Session mailSession;

    public void sendEmail(String to, String subject, String body) {
        MimeMessage message = new MimeMessage(mailSession);
        try {

            message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
            InternetAddress[] address = {new InternetAddress(to)};
            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(subject);
            message.setContent(body, "text/html");
            
            // set the timestamp
            message.setSentDate(new Date());
            message.setText(body);
            Transport transport = mailSession.getTransport();  
            transport.connect();
            transport.send(message);
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
}
