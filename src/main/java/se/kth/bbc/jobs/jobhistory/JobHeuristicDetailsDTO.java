/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vasileios
 */
@XmlRootElement
public class JobHeuristicDetailsDTO implements Serializable{
    
    private String appId;
    private String totalSeverity;
    private String totalDriverMemory;
    private String totalExecutorMemory;
    private String memoryForStorage;
    
    public JobHeuristicDetailsDTO(){}
    
    public JobHeuristicDetailsDTO(String appId, String totalSeverity){
        this.appId = appId;
        this.totalSeverity = totalSeverity;
    }
    
    public JobHeuristicDetailsDTO(String appId, String totalSeverity, String totalDriverMemory,
                                  String totalExecutorMemory, String memoryForStorage){
        this.appId = appId;
        this.totalSeverity = totalSeverity;
        this.totalDriverMemory = totalDriverMemory;
        this.totalExecutorMemory = totalExecutorMemory;
        this.memoryForStorage = memoryForStorage;
    }

    /**
     * @return the appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @param appId the appId to set
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return the totalSeverity
     */
    public String getTotalSeverity() {
        return totalSeverity;
    }

    /**
     * @param totalSeverity the totalSeverity to set
     */
    public void setTotalSeverity(String totalSeverity) {
        this.totalSeverity = totalSeverity;
    }

    /**
     * @return the totalDriverMemory
     */
    public String getTotalDriverMemory() {
        return totalDriverMemory;
    }

    /**
     * @param totalDriverMemory the totalDriverMemory to set
     */
    public void setTotalDriverMemory(String totalDriverMemory) {
        this.totalDriverMemory = totalDriverMemory;
    }

    /**
     * @return the totalExecutorMemory
     */
    public String getTotalExecutorMemory() {
        return totalExecutorMemory;
    }

    /**
     * @param totalExecutorMemory the totalExecutorMemory to set
     */
    public void setTotalExecutorMemory(String totalExecutorMemory) {
        this.totalExecutorMemory = totalExecutorMemory;
    }

    /**
     * @return the memoryForStorage
     */
    public String getMemoryForStorage() {
        return memoryForStorage;
    }

    /**
     * @param memoryForStorage the memoryForStorage to set
     */
    public void setMemoryForStorage(String memoryForStorage) {
        this.memoryForStorage = memoryForStorage;
    }
    
    
    
}
