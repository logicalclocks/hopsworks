package se.kth.bbc.project.services;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter(value = "projectServiceConverter")
public class ProjectServicesConverter extends EnumConverter {

  public ProjectServicesConverter() {
    super(ProjectServiceEnum.class);
  }
}
