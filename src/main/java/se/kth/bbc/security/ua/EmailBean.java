package se.kth.bbc.security.ua;

import java.util.Date;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;
import javax.annotation.Resource;
import javax.ejb.Stateless;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class EmailBean{

    
    private static final Logger logger = Logger.getLogger(UserRegistration.class.getName());

    @Resource(lookup = "mail/BBCMail")
    private Session mailSession;

    public void sendEmail(String to, String subject, String body) throws MessagingException {
        
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
        InternetAddress[] address = {new InternetAddress(to)};
        message.setRecipients(Message.RecipientType.TO, address);
        message.setSubject(subject);
        String content = "Greetings!\n\nThere have been a password reset request on your behalf.\n\nPlease use the temporary password"
                + " sent to you as below. You will be required to change your passsword when you login first time.\n\n";        
        
        String current = "Pasword:" + body+ "\n\n\n";
        
        String ending ="If you have any questions please contact support@biobankcloud.com";
        
        String pass_mess = content + current + ending;
        message.setContent(pass_mess, "text/html");

        // set the timestamp
        message.setSentDate(new Date());
        message.setText(pass_mess);
        Transport.send(message);
    }
}
