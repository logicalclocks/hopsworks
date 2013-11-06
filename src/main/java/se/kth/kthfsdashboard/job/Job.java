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

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class Job implements Serializable {
    private final String uniqueID;
    private String executedBy;
    private String name;
    private String dateRun;
    private int completionTime;
    
    public Job(String executedBy, String name, String dateRun, int completionTime){
        byte[] contents = null;
        byte[] digest = null;
        String hashtext="";
        
        try{
            contents = (executedBy+name+dateRun+completionTime).getBytes("UTF-8");
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

    public int getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }
    
    
}
