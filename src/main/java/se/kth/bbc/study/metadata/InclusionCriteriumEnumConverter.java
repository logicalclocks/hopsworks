package se.kth.bbc.study.metadata;

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
