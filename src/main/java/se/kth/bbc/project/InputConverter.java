package se.kth.bbc.project;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author roshan
 */
@FacesConverter("nameConverter")
public class InputConverter implements Converter {

  @Override
  public Object getAsObject(FacesContext facesContext, UIComponent component,
          String value) {
    if (value != null && value.trim().length() > 0) {
      ProjectMB service = (ProjectMB) facesContext.getExternalContext().
              getSessionMap().get("projectManagedBean");
      return service.getThemes().get(Integer.parseInt(value));
    } else {
      return null;
    }

  }

  @Override
  public String getAsString(FacesContext facesContext, UIComponent component,
          Object value) {
    if (value == null || value.equals("")) {
      return "";
    } else {
      return String.valueOf(((Theme) value).getId());
    }
  }

}
