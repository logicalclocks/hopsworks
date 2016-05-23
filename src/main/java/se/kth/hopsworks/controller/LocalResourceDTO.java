package se.kth.hopsworks.controller;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;

/**
 * Class containing LocalResource properties required by YarnJobs.
 * 
 */
@XmlRootElement
public class LocalResourceDTO implements Serializable {
    private String name;
    private String path;
    private LocalResourceVisibility visibility;
    private LocalResourceType type;
    //User provided pattern is used if the LocalResource is of type Pattern
    private String pattern;

    public LocalResourceDTO() {
    }
    
    public LocalResourceDTO(String name, String path, LocalResourceVisibility visibility, LocalResourceType type, String pattern) {
        this.name = name;
        this.path = path;
        this.visibility = visibility;
        this.type = type;
        this.pattern = pattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalResourceVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(LocalResourceVisibility visibility) {
        this.visibility = visibility;
    }

    public LocalResourceType getType() {
        return type;
    }

    public void setType(LocalResourceType type) {
        this.type = type;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }



    
   
}
