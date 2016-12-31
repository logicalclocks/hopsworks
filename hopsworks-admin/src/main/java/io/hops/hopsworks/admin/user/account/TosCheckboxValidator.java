package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

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
