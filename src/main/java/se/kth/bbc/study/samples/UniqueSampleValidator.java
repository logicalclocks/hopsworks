package se.kth.bbc.study.samples;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

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
    System.out.println("Validator called.");
    String sdf = value.toString();
    if(sampleFacade.existsSampleWithId(value.toString())){ 
			FacesMessage msg = 
				new FacesMessage(FacesMessage.SEVERITY_ERROR,"Non-unique id.", 
						"A sample with this ID already exists.");
      System.out.println("Non-unique id: aborting");
			throw new ValidatorException(msg); 
		}
  }
  
}
