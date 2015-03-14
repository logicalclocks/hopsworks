package se.kth.bbc.lims;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import org.primefaces.context.RequestContext;

/**
 *
 * @author stig
 */
public class MessagesController {

    public static final String ERROR = "Error.";
    public static final String SUCCESS = "Success.";

    //TODO: replace all occurrences of similar methods elsewhere.
    public static void addInfoMessage(String message) {
        addInfoMessage(message, message, null);
    }

    public static void addInfoMessage(String summary, String mess) {
        addInfoMessage(summary, mess, null);
    }

    public static void addInfoMessage(String summary, String mess, String anchor) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, mess);
        FacesContext.getCurrentInstance().addMessage(anchor, message);
    }

    public static void addErrorMessage(String message) {
        addErrorMessage("Error", message, null);
    }

    public static void addErrorMessage(String summary, String message) {
        addErrorMessage(summary, message, null);
    }

    public static void addErrorMessage(String summary, String message, String anchor) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, message);
        FacesContext.getCurrentInstance().addMessage(anchor, errorMessage);
    }

    public static void addMessage(FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public static void addWarnMessage(String summary, String mess) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, summary, mess));
    }

    public static void addSecurityErrorMessage(String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR, message, "null"));

    }

    public static void addMessageToGrowl(String message) {

        RequestContext.getCurrentInstance().update("growl");
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

}
