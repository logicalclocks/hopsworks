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
public class JobHeuristicDTO implements Serializable{
    
    private String message = "";              
    private String degreeOfSimilarity = "";   
    private int numberOfResults;           // number of history records that examined
    private String jobType = "";           // the Job Type
    private int estimatedTime;             // estimated complition time
    private int amMemory;                  // estimated Application Master Memory
    private int amVcores;
    private String rmQueue = "";
    private int numberOfexecutors;
    private int executorMemory;
    
    public JobHeuristicDTO(){
    }
    
    public JobHeuristicDTO(int numberOfResults, String jobType, String degreeOfSimilarity){
        this.numberOfResults = numberOfResults;
        this.jobType = jobType;  
        this.degreeOfSimilarity = degreeOfSimilarity;
    }

    /**
     * @return the numberOfResults
     */
    public int getNumberOfResults() {
        return numberOfResults;
    }

    /**
     * @param numberOfResults the numberOfResults to set
     */
    public void setNumberOfResults(int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @param jobType the jobType to set
     */
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the degreeOfSimilarity
     */
    public String getDegreeOfSimilarity() {
        return degreeOfSimilarity;
    }

    /**
     * @param degreeOfSimilarity the degreeOfSimilarity to set
     */
    public void setDegreeOfSimilarity(String degreeOfSimilarity) {
        this.degreeOfSimilarity = degreeOfSimilarity;
    }

    /**
     * @return the estimatedTime
     */
    public int getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * @param estimatedTime the estimatedTime to set
     */
    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    /**
     * @return the amMemory
     */
    public int getAmMemory() {
        return amMemory;
    }

    /**
     * @param amMemory the amMemory to set
     */
    public void setAmMemory(int amMemory) {
        this.amMemory = amMemory;
    }

    /**
     * @return the amVcores
     */
    public int getAmVcores() {
        return amVcores;
    }

    /**
     * @param amVcores the amVcores to set
     */
    public void setAmVcores(int amVcores) {
        this.amVcores = amVcores;
    }

    /**
     * @return the rmQueue
     */
    public String getRmQueue() {
        return rmQueue;
    }

    /**
     * @param rmQueue the rmQueue to set
     */
    public void setRmQueue(String rmQueue) {
        this.rmQueue = rmQueue;
    }

    /**
     * @return the numberOfexecutors
     */
    public int getNumberOfexecutors() {
        return numberOfexecutors;
    }

    /**
     * @param numberOfexecutors the numberOfexecutors to set
     */
    public void setNumberOfexecutors(int numberOfexecutors) {
        this.numberOfexecutors = numberOfexecutors;
    }

    /**
     * @return the executorMemory
     */
    public int getExecutorMemory() {
        return executorMemory;
    }

    /**
     * @param executorMemory the executorMemory to set
     */
    public void setExecutorMemory(int executorMemory) {
        this.executorMemory = executorMemory;
    }
 
    
}
