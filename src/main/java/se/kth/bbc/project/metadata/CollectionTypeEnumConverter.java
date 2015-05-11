package se.kth.bbc.project.metadata;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter("collectionTypeEnumConverter")
public class CollectionTypeEnumConverter extends EnumConverter {

  public CollectionTypeEnumConverter() {
    super(CollectionTypeProjectDesignEnum.class);
  }

}
