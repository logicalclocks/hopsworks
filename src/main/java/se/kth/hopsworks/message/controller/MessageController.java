package se.kth.hopsworks.message.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import se.kth.hopsworks.message.Message;
import se.kth.hopsworks.message.MessageFacade;
import se.kth.hopsworks.user.model.Users;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MessageController {

  private final static Logger logger = Logger.getLogger(MessageController.class.
          getName());
  public final String REPLY_SEPARATOR
          = "<hr>";
  public final int MAX_MESSAGE_SIZE = 65000;
  public final String MORE_MESSAGES = "Too many messages to show...";
  @EJB
  private MessageFacade messageFacade;

  /**
   * Reply to a message by adding the reply to the original message and sending
   * a new one.
   * <p>
   * @param user that is sending the reply
   * @param msg that is replied to
   * @param reply the reply text
   */
  public void reply(Users user, Message msg, String reply) {
    Date now = new Date();
    if (msg.getFrom() == null) {
      throw new IllegalArgumentException("Message does not contain a sender.");
    }
    if (reply == null || reply.isEmpty()) {
      throw new IllegalArgumentException("Message is empty.");
    }
    if (reply.length() > MAX_MESSAGE_SIZE) {
      throw new IllegalArgumentException("Message too long.");
    }
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(msg.
            getDateSent());
    String dateAndWriter = "On " + date + ", " + user.getFname() + " " + user.
            getLname() + " wrote: <br><br>";
    String replyMsg = REPLY_SEPARATOR + dateAndWriter + reply + msg.getContent();
    if (replyMsg.length() > MAX_MESSAGE_SIZE) {//size of text in db is 65535
      replyMsg = REPLY_SEPARATOR + dateAndWriter + reply + MORE_MESSAGES;
    }
    boolean newMessage = false;
    Message newMsg = msg.getReplyToMsg();
    if (newMsg != null) {
      newMsg = messageFacade.findMessageById(newMsg.getId());
    } else {
      newMessage = true;
      newMsg = new Message(user, msg.getFrom(), now);
      newMsg.setReplyToMsg(msg);
    }

    newMsg.setUnread(true);
    newMsg.setDeleted(false);
    newMsg.setContent(replyMsg);
    String preview = user.getFname() + " replied ";
    if (msg.getSubject() != null) {
      preview = preview + "to  your " + msg.getSubject().toLowerCase();
    }
    newMsg.setPreview(preview);
    if (newMessage) {
      messageFacade.save(newMsg);
    } else {
      messageFacade.edit(newMsg);
    }

    msg.setContent(replyMsg);
    if (msg.getReplyToMsg() == null) {
      msg.setReplyToMsg(newMsg);
    }
    messageFacade.edit(msg);
  }

  /**
   * Sends a message to a single user
   * <p>
   * @param to
   * @param from
   * @param subject
   * @param preview
   * @param msg
   * @param requestPath if the message is a request this will contain the path
   * to the requested dataset or project.
   */
  public void send(Users to, Users from, String subject, String preview,
          String msg, String requestPath) {
    Date now = new Date();
    if (to == null) {
      throw new IllegalArgumentException("No recipient specified.");
    }
    if (msg == null || msg.isEmpty()) {
      throw new IllegalArgumentException("Message is empty.");
    }
    if (msg.length() > MAX_MESSAGE_SIZE) {
      throw new IllegalArgumentException("Message too long.");
    }
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(now);
    String dateAndWriter = "On " + date + ", " + from.getFname() + " " + from.
            getLname() + " wrote: <br><br>";
    String message = REPLY_SEPARATOR + dateAndWriter + msg;
    Message newMsg
            = new Message(from, to, now, message, true, false);
    newMsg.setPath(requestPath);
    newMsg.setSubject(subject);
    newMsg.setPreview(preview);
    messageFacade.save(newMsg);
  }

  /**
   * Sends message to multiple users.
   * <p>
   * @param recipients list of the receivers
   * @param from the sender
   * @param msg the message text
   * @param requestPath requestPath if the message is a request this will
   * contain the path
   * to the requested dataset or project.
   */
  public void sendToMany(List<Users> recipients, Users from, String msg,
          String requestPath) {
    Date now = new Date();
    if (recipients == null || recipients.isEmpty()) {
      throw new IllegalArgumentException("No recipient specified.");
    }
    if (msg == null || msg.isEmpty()) {
      throw new IllegalArgumentException("Message is empty.");
    }
    if (msg.length() > MAX_MESSAGE_SIZE) {
      throw new IllegalArgumentException("Message too long.");
    }
    for (Users u : recipients) {
      Message newMsg
              = new Message(from, u, recipients, now, msg, true, false);
      newMsg.setPath(requestPath);
      messageFacade.save(newMsg);
    }

  }

}
