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

package io.hops.hopsworks.admin.user.account;

import com.google.zxing.WriterException;
import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.QRCodeGenerator;
import io.hops.hopsworks.common.util.Settings;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@SessionScoped
public class RecoverySelector implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private static final Logger LOGGER = Logger.getLogger(RecoverySelector.class.getName());
  
  
  @EJB
  protected UsersController usersController;
  @EJB
  private UserFacade userFacade;

  @EJB
  private EmailBean email;

  @EJB
  private AccountAuditFacade am;
  
  @EJB
  private AuthController authController;

  private Users people;

  private String console;

  // Quick response code URL
  private String qrUrl = "Pass";
  private StreamedContent qrCode = null;

  private String uname;
  private String tmpCode;
  private String passwd;

  private final int passwordLength = Settings.PASSWORD_MIN_LENGTH;

  private int qrEnabled = -1;

  public int getQrEnabled() {
    return qrEnabled;
  }

  public void setQrEnabled(int qrEnabled) {
    this.qrEnabled = qrEnabled;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
  }

  public StreamedContent getQrCode() {
    return qrCode;
  }

  public void setQrCode(StreamedContent qrCode) {
    this.qrCode = qrCode;
  }

  public String getUname() {
    return uname;
  }

  public void setUname(String uname) {
    this.uname = uname;
  }

  public String getTmpCode() {
    return tmpCode;
  }

  public void setTmpCode(String tmpCode) {
    this.tmpCode = tmpCode;
  }

  public String getPasswd() {
    return passwd;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd;
  }

  public String getConsole() {
    return console;
  }

  public void setConsole(String console) {
    this.console = console;
  }

  public String redirect() {

    if (console.equals("Password")) {
      return "sec_question";
    }

    if (console.equals("Mobile")) {
      return "mobile_recovery";
    }
    
    return "";
  }

  /**
   * Register lost mobile lost device.
   * <p>
   * @return
   * @throws SocketException
   */
  public String sendQrCode() {

    people = userFacade.findByEmail(this.uname);
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
        getCurrentInstance().getExternalContext().getRequest();
    try {
      if (people != null && authController.checkPasswordAndStatus(people, passwd, httpServletRequest)) {

        // generate a randome secret of legth 6
        String random = SecurityUtils.getRandomPassword(passwordLength);
        usersController.updateSecret(people.getUid(), random);
        String message = UserAccountsEmailMessages.buildTempResetMessage(random);
        email.sendEmail(people.getEmail(), RecipientType.TO,UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);
        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(), AccountsAuditActions.SUCCESS.name(),
            "Reset QR code.", people, httpServletRequest);

        return "validate_code";
      } else {
        MessagesController.addSecurityErrorMessage(RESTCodes.UserErrorCode.INCORRECT_CREDENTIALS.getMessage());
        if (people != null) {
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(), AccountsAuditActions.FAILED.name(),
              "", people, httpServletRequest);
        }
        return "";
      }
    } catch (MessagingException ex) {
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(), AccountsAuditActions.FAILED.name(),
          "", people, httpServletRequest);

    } catch (UserException ex) {
      Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE, null, ex);
    }

    MessagesController.addSecurityErrorMessage(RESTCodes.GenericErrorCode.UNKNOWN_ERROR.getMessage());
    return "";
  }

  /**
   * Validate the temp code sent to user to reset the account.
   * <p>
   * @return
   */
  public String validateTmpCode() {
    qrCode = null;
    people = userFacade.findByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
        RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND.getMessage());
      return "";
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus().equals(UserAccountStatus.BLOCKED_ACCOUNT)) {
      MessagesController.addSecurityErrorMessage(
              RESTCodes.UserErrorCode.ACCOUNT_BLOCKED.getMessage());
      return "";
    }

    if (people.getStatus().equals(UserAccountStatus.DEACTIVATED_ACCOUNT)) {
      MessagesController.addSecurityErrorMessage(
              RESTCodes.UserErrorCode.ACCOUNT_DEACTIVATED.getMessage());
      return "";
    }

    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
        getCurrentInstance().getExternalContext().getRequest();
    if (people.getSecret() == null ? tmpCode == null : people.getSecret().
            equals(this.tmpCode)) {

      try {
        String otpSecret = SecurityUtils.calculateSecretKey();

        usersController.updateSecret(people.getUid(), otpSecret);
        qrCode = new DefaultStreamedContent(QRCodeGenerator.getQRCode(people.
                getEmail(), Settings.ISSUER, otpSecret), "image/png");
        qrEnabled = 1;
        am.registerAccountChange(people, AccountsAuditActions.QRCODE.name(), AccountsAuditActions.SUCCESS.name(),
            "QR code reset.", people, httpServletRequest);
        return "qrcode";

      } catch (IOException | WriterException | NoSuchAlgorithmException e) {
        Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE,
                null, e);
      }

    } else {
      int val = people.getFalseLogin();
      usersController.increaseLockNum(people.getUid(), val + 1);
      if (val > Settings.ALLOWED_FALSE_LOGINS) {
        usersController.changeAccountStatus(people.getUid(), "",
                UserAccountStatus.BLOCKED_ACCOUNT);
        try {
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                  AccountsAuditActions.SUCCESS.name(),
                  "Account bloecked due to many false attempts.", people, httpServletRequest);

          email.sendEmail(people.getEmail(), RecipientType.TO,
                  UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                  UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex1) {
          LOGGER.log(Level.SEVERE, null, ex1);
        }
      }

      MessagesController.addSecurityErrorMessage(RESTCodes.UserErrorCode.TMP_CODE_INVALID.getMessage());

      return "";
    }
    return "";
  }

  public String returnMenu() {

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = null;
    if (ctx != null) {
      sess = (HttpSession) ctx.getExternalContext().getSession(false);
    }

    if (sess != null) {
      sess.invalidate();
    }
    qrCode = null;
    return ("welcome");

  }
}
