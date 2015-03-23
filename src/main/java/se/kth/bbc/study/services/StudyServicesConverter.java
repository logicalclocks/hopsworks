package se.kth.bbc.study.services;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter(value = "studyServiceConverter")
public class StudyServicesConverter extends EnumConverter {

  public StudyServicesConverter() {
    super(StudyServiceEnum.class);
  }
}
