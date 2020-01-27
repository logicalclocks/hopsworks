/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.exceptions.UserException;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@RequestScoped
public class RecoverQRCode implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(RecoverQRCode.class.getName());
  
  private StreamedContent qrCode;
  private boolean keyError;
  
  @ManagedProperty("#{param.key}")
  private String key;
  
  @EJB
  protected AuditedUserAccountAction auditedUserAccountAction;
  
  @PostConstruct
  public void init() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    try {
      byte[] code = auditedUserAccountAction.recoverQRCodeByte(key, req);
      qrCode = new DefaultStreamedContent(new ByteArrayInputStream(code));
      keyError = false;
    } catch (EJBException | IllegalArgumentException | MessagingException e) {
      keyError = true;
      String detail = e.getCause() != null? e.getCause().getMessage() : "QR code recovery key validation error.";
      ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid key:", detail));
      LOGGER.log(Level.FINE, detail);
    } catch (UserException ue) {
      keyError = true;
      String detail = ue.getUsrMsg() != null ? ue.getUsrMsg() : ue.getErrorCode().getMessage();
      ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid key:", detail));
      LOGGER.log(Level.FINE, detail);
    }
  }
  
  public StreamedContent getQrCode() {
    return qrCode;
  }
  
  public void setQrCode(StreamedContent qrCode) {
    this.qrCode = qrCode;
  }
  
  public boolean isKeyError() {
    return keyError;
  }
  
  public void setKeyError(boolean keyError) {
    this.keyError = keyError;
  }
  
  public String getKey() {
    return key;
  }
  
  public void setKey(String key) {
    this.key = key;
  }
  
  public String cancel() {
    return ("welcome");
  }
}
