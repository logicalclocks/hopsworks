package se.kth.bbc.project.metadata;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter("inclusionCriteriumEnumConverter")
public class InclusionCriteriumEnumConverter extends EnumConverter {

  public InclusionCriteriumEnumConverter() {
    super(InclusionCriteriumEnum.class);
  }

}
