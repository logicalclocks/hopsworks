package se.kth.bbc.study.samples;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter("materialTypeEnumConverter")
public class MaterialTypeEnumConverter extends EnumConverter {

  public MaterialTypeEnumConverter() {
    super(MaterialTypeEnum.class);
  }

}
