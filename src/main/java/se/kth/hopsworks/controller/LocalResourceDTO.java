package se.kth.hopsworks.controller;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class containing LocalResource properties required by YarnJobs.
 * 
 */
@XmlRootElement
public class LocalResourceDTO {
    
    List<LocalResourceAttrs> localResourcesAttrs;

    public LocalResourceDTO() {
    }

    public LocalResourceDTO(List<LocalResourceAttrs> localResourcesAttrs) {
        this.localResourcesAttrs = localResourcesAttrs;
    }

    public List<LocalResourceAttrs> getLocalResourcesAttrs() {
        return localResourcesAttrs;
    }

    public void setLocalResourcesAttrs(List<LocalResourceAttrs> localResourcesAttrs) {
        this.localResourcesAttrs = localResourcesAttrs;
    }

}
