/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholmai <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class UsernameValidator implements Validator {

    @EJB
    private UserManager mgr;

    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
            + "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";

    /**
     * Ensure the the username is available.
     *
     * @param context
     * @param component
     * @param value
     * @throws ValidatorException
     */
    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {

        String uname = value.toString();

        if (!isValidEmail(uname)) {
            FacesMessage facesMsg = new FacesMessage(
                    "Invalid email format");
            facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(facesMsg);
        }

        if (mgr.isUsernameTaken(uname)) {

            // if it's a profile change ignore 
            if (getRequest().getRemoteUser() != null) {
                User p = mgr.findByEmail(getRequest().getRemoteUser());
                if (p.getEmail().equals(uname)) {
                    ;
                } else {

                    FacesMessage facesMsg = new FacesMessage(
                            "Email is already taken");
                    facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
                    throw new ValidatorException(facesMsg);
                }
            } else {
                FacesMessage facesMsg = new FacesMessage(
                        "Email is already taken");
                facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(facesMsg);

            }

        }

    }

    public boolean isValidEmail(String u) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(u);
        return matcher.matches();

    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

}
