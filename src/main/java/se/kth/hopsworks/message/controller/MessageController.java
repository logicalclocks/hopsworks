package se.kth.hopsworks.message.controller;

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
          = "/n /n------------------------------------------------------------ /n /n";
  public final int MAX_MESSAGE_SIZE = 65000;
  public final String MORE_MESSAGES = "Too many messages...";
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
    if (reply == null || reply.isEmpty()) {
      throw new IllegalArgumentException("Message is empty.");
    }
    if (reply.length() > MAX_MESSAGE_SIZE) {
      throw new IllegalArgumentException("Message too long.");
    }
    String replyMsg = reply + REPLY_SEPARATOR + msg.getContent();
    if (replyMsg.length() > MAX_MESSAGE_SIZE) {//size of text in mysql is 65535
      replyMsg = reply + REPLY_SEPARATOR + MORE_MESSAGES;
    }
    Message newMsg
            = new Message(user, msg.getFrom(), msg.getFrom(), now, replyMsg, true, false);
    messageFacade.create(newMsg);
    msg.setContent(replyMsg);
    messageFacade.edit(msg);
  }

  /**
   * Sends a message to a single user
   * <p>
   * @param to
   * @param from
   * @param msg
   * @param requestPath if the message is a request this will contain the path
   * to the requested dataset or project. 
   */
  public void send(Users to, Users from, String msg, String requestPath) {
    Date now = new Date();
    if (from == null) {
      throw new IllegalArgumentException("No sender specified.");
    }
    if (to == null) {
      throw new IllegalArgumentException("No recipient specified.");
    }
    if (msg == null || msg.isEmpty()) {
      throw new IllegalArgumentException("Message is empty.");
    }
    if (msg.length() > MAX_MESSAGE_SIZE) {
      throw new IllegalArgumentException("Message too long.");
    }
    Message newMsg
            = new Message(from, to, to, now, msg, true, false);
    newMsg.setPath(requestPath);
    messageFacade.create(newMsg);
  }

  /**
   * Sends message to multiple users.
   * <p>
   * @param recipients list of the receivers
   * @param from the sender
   * @param msg the message text
   * @param requestPath requestPath if the message is a request this will contain the path
   * to the requested dataset or project. 
   */
  public void sendToMany(List<Users> recipients, Users from, String msg, String requestPath) {
    Date now = new Date();
    if (from == null) {
      throw new IllegalArgumentException("No sender specified.");
    }
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
      messageFacade.create(newMsg);
    }
    
  }

}
