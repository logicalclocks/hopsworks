/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Stateless
public class EmailBean {

  private static final Logger LOG = Logger.getLogger(EmailBean.class.getName());

  @Resource(lookup = "mail/BBCMail")
  private Session mailSession;

  @Asynchronous
  public void sendEmail(String to, Message.RecipientType recipientType,
      String subject, String body) throws
      MessagingException, SendFailedException {

    MimeMessage message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
    InternetAddress[] address = {new InternetAddress(to)};
    message.setRecipients(recipientType, address);
    message.setSubject(subject);
    message.setContent(body, "text/html");

    // set the timestamp
    message.setSentDate(new Date());

    message.setText(body);
    try {
      Transport.send(message);
    } catch (MessagingException ex) {
      LOG.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  @Asynchronous
  public void sendEmails(String listAddrs, String subject, String body) throws MessagingException, SendFailedException {

    List<String> addrs = Arrays.asList(listAddrs.split("\\s*,\\s*"));

    boolean first = false;
    MimeMessage message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
    message.setSubject(subject);
    message.setContent(body, "text/html");
    message.setSentDate(new Date());
    message.setText(body);
    for (String addr : addrs) {

      InternetAddress[] address = {new InternetAddress(addr)};
      if (first) {
        message.setRecipients(Message.RecipientType.TO, address);
        first = false;
      } else {
        message.setRecipients(Message.RecipientType.CC, address);
      }

    }
    try {
      Transport.send(message);
    } catch (MessagingException ex) {
      LOG.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }
}
