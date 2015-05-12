package se.kth.bbc.project.samples.validators;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import se.kth.bbc.project.samples.SamplecollectionFacade;

/**
 *
 * @author stig
 */
@ManagedBean
@RequestScoped
public class UniqueCollectionIdValidator implements Validator {

  @EJB
  private SamplecollectionFacade collectionFacade;

  @Override
  public void validate(FacesContext context, UIComponent component, Object value)
          throws ValidatorException {
    if (collectionFacade.existsCollectionWithId(value.toString())) {
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Non-unique id.",
              "A collection with this ID already exists.");
      throw new ValidatorException(msg);
    }
  }

}
