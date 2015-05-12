/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import se.kth.bbc.security.auth.AccountStatusErrorMessages;

/**
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class TosCheckboxValidator implements Validator {

  public boolean check = false;

  public boolean isCheck() {
    return check;
  }

  public void setCheck(boolean check) {
    this.check = check;
  }

  @Override
  public void validate(FacesContext context, UIComponent component,
          Object value) throws ValidatorException {

    String cb = value.toString();

    if (!cb.equals("true")) {
      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.TOS_ERROR);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }
  }

}
