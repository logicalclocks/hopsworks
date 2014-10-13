package se.kth.bbc.study;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Utility class for loading Study data into webpage. 
 * It reflects the main information needed to show in the index page, created with results from database queries. 
 * As such it contains but getters and setters.
 * @author stig
 */
@Entity
public class StudyDetail implements Serializable{
    
    @Id
    private String studyName;
    private String email;
    private String creator;

    public StudyDetail(){
        
    }
    
    public StudyDetail(String studyName, String email, String creatorName) {
        this.studyName = studyName;
        this.email = email;
        this.creator = creatorName;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
    
    
}
