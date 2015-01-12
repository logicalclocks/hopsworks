package se.kth.bbc.study.samples.validators;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import se.kth.bbc.study.samples.SampleFacade;

/**
 *
 * @author stig
 */
@ManagedBean
@RequestScoped
public class UniqueSampleValidator implements Validator{
  
  @EJB
  private SampleFacade sampleFacade;

  @Override
  public void validate(FacesContext context, UIComponent component, Object value)
          throws ValidatorException {
    if(sampleFacade.existsSampleWithId(value.toString())){ 
			FacesMessage msg = 
				new FacesMessage(FacesMessage.SEVERITY_ERROR,"Non-unique id.", 
						"A sample with this ID already exists.");
			throw new ValidatorException(msg); 
		}
  }
  
}
