package se.kth.bbc.study.metadata;

import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author stig
 */
@FacesConverter("collectionTypeEnumConverter")
public class CollectionTypeEnumConverter extends EnumConverter {

    public CollectionTypeEnumConverter() {
        super(CollectionTypeStudyDesignEnum.class);
    }

}
