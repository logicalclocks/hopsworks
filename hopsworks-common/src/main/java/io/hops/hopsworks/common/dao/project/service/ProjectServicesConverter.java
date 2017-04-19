package io.hops.hopsworks.common.dao.project.service;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

@FacesConverter(value = "projectServiceConverter")
public class ProjectServicesConverter extends EnumConverter {

  public ProjectServicesConverter() {
    super(ProjectServiceEnum.class);
  }
}
