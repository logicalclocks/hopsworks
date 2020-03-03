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

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.exceptions.UserException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@SessionScoped
public class ResetPassword implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = Logger.getLogger(ResetPassword.class.getName());

  private String username;
  private String passwd1;
  private String passwd2;
  private String current;
  private SecurityQuestion question;
  private String answer;
  private Users people;
  private List<SecurityQuestion> securityQuestions;
  private String notes;

  @EJB
  protected AuditedUserAccountAction auditedUserAccountAction;
  @EJB
  private UserFacade userFacade;
  
  @PostConstruct
  public void init(){
    securityQuestions = Arrays.asList(SecurityQuestion.values());
    Collections.shuffle(securityQuestions);
  }
  
  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public SecurityQuestion[] getQuestions() {
    return SecurityQuestion.values();
  }

  public SecurityQuestion getQuestion() {
    return question;
  }

  public void setQuestion(SecurityQuestion question) {
    this.question = question;
  }

  public String getCurrent() {
    return current;
  }

  public void setCurrent(String current) {
    this.current = current;
  }

  public Users getPeople() {
    return people;
  }

  public void setPeople(Users people) {
    this.people = people;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswd1() {
    return passwd1;
  }

  public void setPasswd1(String passwd1) {
    this.passwd1 = passwd1;
  }

  public String getPasswd2() {
    return passwd2;
  }

  public void setPasswd2(String passwd2) {
    this.passwd2 = passwd2;
  }
  
  public List<SecurityQuestion> getSecurityQuestions() {
    return securityQuestions;
  }
  
  public void setSecurityQuestions(List<SecurityQuestion> securityQuestions) {
    this.securityQuestions = securityQuestions;
  }
  
  public String sendPasswordResetEmail() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    try {
      auditedUserAccountAction.sendPasswordRecoveryEmail(this.username, question, answer, req);
    } catch (MessagingException ex) {
      String detail = ex.getCause() != null? ex.getCause().getMessage() : "Failed to send recovery email.";
      MessagesController.addSecurityErrorMessage(detail);
      LOGGER.log(Level.FINE, null, detail);
      return ("");
    }  catch (UserException ue) {
      String detail = ue.getUsrMsg() != null ? ue.getUsrMsg() : ue.getErrorCode().getMessage();
      MessagesController.addSecurityErrorMessage(detail);
      LOGGER.log(Level.FINE, null, detail);
      return ("");
    }
    return ("password_sent");
  }

  public String changeProfilePassword() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = userFacade.findByEmail(req.getRemoteUser());

    if (people == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
      session.invalidate();
      return ("welcome");
    }

    try {
      auditedUserAccountAction.changePassword(people, current, passwd1, passwd2, req);
      return ("profile_password_changed");
    } catch (IllegalStateException | UserException ex) {
      MessagesController.addSecurityErrorMessage(ex.getMessage());
      return ("");
    }
  }

  public String returnMenu() {
    HttpSession sess = (HttpSession)
        FacesContext.getCurrentInstance().getExternalContext().getSession(false);
    if (sess != null) {
      sess.invalidate();
    }
    return ("welcome");
  }
}
