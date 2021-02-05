/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.integrations.UnmanagedStereotype;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.logging.Logger;

@UnmanagedStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UnmanagedPasswordRecovery implements PasswordRecovery {
  private static final Logger LOG = Logger.getLogger(UnmanagedPasswordRecovery.class.getName());

  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private EmailBean emailBean;

  @Override
  public void sendRecoveryNotification(Users user, String url, boolean isPassword,
          AuthController.CredentialsResetToken resetToken) throws MessagingException {
    String subject = UserAccountsEmailMessages.ACCOUNT_MOBILE_RECOVERY_SUBJECT;
    String encodedToken = securityUtils.urlEncode(resetToken.getToken());
    String msg = UserAccountsEmailMessages.buildQRRecoveryMessage(url, user.getUsername() + encodedToken,
            resetToken.getValidity());
    if (isPassword) {
      subject = UserAccountsEmailMessages.ACCOUNT_PASSWORD_RECOVERY_SUBJECT;
      msg = UserAccountsEmailMessages.buildPasswordRecoveryMessage(url, user.getUsername() + encodedToken,
              resetToken.getValidity());
    }
    emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, subject, msg);
  }
}
