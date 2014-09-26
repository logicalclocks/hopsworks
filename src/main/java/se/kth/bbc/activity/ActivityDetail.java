package se.kth.bbc.activity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Utility class for loading Activity data into webpage. 
 * It reflects the main information needed to show in the 'show' tabs, created with results from database queries. 
 * As such it contains but getters and setters.
 * @author stig
 */
@Entity
public class ActivityDetail implements Serializable{
    
    @Id
    private Integer id;
    private String email;
    private String author;
    private String activity;
    private String studyName;
    @Temporal(TemporalType.TIMESTAMP)
    private Date myTimestamp;

    public ActivityDetail() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public Date getMyTimestamp() {
        return myTimestamp;
    }

    public void setMyTimestamp(Date myTimestamp) {
        this.myTimestamp = myTimestamp;
    }
    
    
}
