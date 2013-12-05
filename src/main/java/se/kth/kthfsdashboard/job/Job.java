/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public Job() {
    }
    
    public Job(String executedBy, String name, String dateRun, long completionTime){
        byte[] contents = null;
        byte[] digest = null;
        String hashtext="";
        
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
    
    
}
