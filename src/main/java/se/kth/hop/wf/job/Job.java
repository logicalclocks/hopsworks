/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf.job;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Entity
@Table(name="Jobs")
@NamedQueries({
    @NamedQuery(name = "Job.findAll", query = "SELECT c FROM Job c")
})

public class Job implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String uniqueID;
    private String executedBy;
    private String name;
    private String dateRun;
    private long completionTime;
    @Column(columnDefinition="text")
    private String graphDot;
    @Column(columnDefinition="text")
    private String tableJob;
    private boolean completed;

    public Job() {
    }
    
    public Job(String executedBy, String name, String dateRun, long completionTime){
        byte[] contents = null;
        byte[] digest = null;
        String hashtext="";
        completed=false;
        try{
            contents = (executedBy+name+dateRun).getBytes("UTF-8");
            digest = MessageDigest.getInstance("MD5").digest(contents);
            BigInteger bigint= new BigInteger(1,digest);
            hashtext = bigint.toString(16);
            
            while(hashtext.length()<32){
                hashtext = "0"+hashtext;
            }
        }
        catch(UnsupportedEncodingException ue){

        }
        catch(NoSuchAlgorithmException na){

        }
            
        this.executedBy=executedBy;
        this.name=name;
        this.dateRun=dateRun;
        this.completionTime=completionTime;
        
        this.uniqueID=(contents==null&&digest==null)?"null":hashtext;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateRun() {
        return dateRun;
    }

    public void setDateRun(String dateRun) {
        this.dateRun = dateRun;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }
    
     public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public String getGraphDot() {
        return graphDot;
    }

    public void setGraphDot(String graphDot) {
        this.graphDot = graphDot;
    }

    public String getTableJob() {
        return tableJob;
    }

    public void setTableJob(String tableJob) {
        this.tableJob = tableJob;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
        
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.uniqueID != null ? this.uniqueID.hashCode() : 0);
        hash = 59 * hash + (this.executedBy != null ? this.executedBy.hashCode() : 0);
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 59 * hash + (this.dateRun != null ? this.dateRun.hashCode() : 0);
        hash = 59 * hash + (int) (this.completionTime ^ (this.completionTime >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Job other = (Job) obj;
        if ((this.uniqueID == null) ? (other.uniqueID != null) : !this.uniqueID.equals(other.uniqueID)) {
            return false;
        }
        if ((this.executedBy == null) ? (other.executedBy != null) : !this.executedBy.equals(other.executedBy)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.dateRun == null) ? (other.dateRun != null) : !this.dateRun.equals(other.dateRun)) {
            return false;
        }
        if (this.completionTime != other.completionTime) {
            return false;
        }
        return true;
    }
    
    
    
}
