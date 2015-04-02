package se.kth.bbc.jobs.adam;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter(value = "adamCommandConverter")
public class AdamCommandConverter implements Converter {

  @Override
  public Object getAsObject(FacesContext context, UIComponent component,
          String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    return AdamCommand.getFromCommand(value);
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component,
          Object value) {
    if (value == null || "".equals(value)) {
      return "";
    }
    if (!(value instanceof AdamCommand)) {
      throw new ConverterException("Object " + value
              + " is not of the expected type AdamCommand.");
    }
    return ((AdamCommand) value).getCommand();
  }

}
