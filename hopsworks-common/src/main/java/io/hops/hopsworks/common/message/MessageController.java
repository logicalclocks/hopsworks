/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.elasticsearch.common.Strings;
import io.hops.hopsworks.common.dao.message.Message;
import io.hops.hopsworks.common.dao.message.MessageFacade;
import io.hops.hopsworks.common.dao.user.Users;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
      messageFacade.update(newMsg);
    }

    msg.setContent(replyMsg);
    if (msg.getReplyToMsg() == null) {
      msg.setReplyToMsg(newMsg);
    }
    messageFacade.update(msg);
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
   * Sends a message to a single user
   * <p>
   * @param msg
   */
  public void send(Message msg) {
    Date now = new Date();
    if (msg.getTo() == null) {
      throw new IllegalArgumentException("No recipient specified.");
    }
    if (Strings.isNullOrEmpty(msg.getContent())) {
      throw new IllegalArgumentException("Message is empty.");
    }
    if (msg.getContent().length() > MAX_MESSAGE_SIZE) {
      throw new IllegalArgumentException("Message too long.");
    }
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(now);
    String dateAndWriter = "On " + date + ", " + msg.getFrom().getFname() + " "
            + msg.getFrom().getLname() + " wrote: <br><br>";
    msg.setDateSent(now);
    String message = REPLY_SEPARATOR + dateAndWriter + msg.getContent();
    msg.setContent(message);

    messageFacade.save(msg);
  }

  /**
   * Sends message to multiple users.
   * <p>
   * @param recipients list of the receivers
   * @param from the sender
   * @param subject
   * @param msg the message text
   * @param requestPath requestPath if the message is a request this will
   * contain the path
   * to the requested dataset or project.
   */
  public void sendToMany(List<Users> recipients, Users from, String subject,
          String msg,
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
              = new Message(from, u, now, msg, true, false); //  recipients, 
      newMsg.setPath(requestPath);
      newMsg.setSubject(subject);
      messageFacade.save(newMsg);
    }

  }

  /**
   * Removes a message entity from the persistent storage.
   *
   * @param msg
   */
  public void remove(Message msg) {
    messageFacade.remove(msg);
  }

}
