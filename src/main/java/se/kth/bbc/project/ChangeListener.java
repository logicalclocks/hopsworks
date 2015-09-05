package se.kth.bbc.project;

import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

/**
 *
 * @author roshan
 */
public class ChangeListener implements ValueChangeListener {

  @Override
  public void processValueChange(ValueChangeEvent event) throws
          AbortProcessingException {
    ValueChangeMB teamRole = (ValueChangeMB) FacesContext.getCurrentInstance().
            getExternalContext().getSessionMap().get("valueChangeMB");
    teamRole.setNewTeamRole((ProjectRoleTypes) event.getNewValue());
  }
}
