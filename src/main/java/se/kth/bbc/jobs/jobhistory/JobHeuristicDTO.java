/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vasileios
 */

@XmlRootElement
public class JobHeuristicDTO implements Serializable{
    
    private String message = ""; 
    private String projectId = "";
    private String jobName = "";
    private String degreeOfSimilarity = "";   
    private int numberOfResults;           // number of history records that examined
    private String jobType = "";           // the Job Type
    private String estimatedTime;             // estimated complition time
    private String inputBlocks = "";
    private List<String> similarAppIds = new ArrayList<String>();
    private List<JobHeuristicDetailsDTO> jobHeuristicDetails = new ArrayList<JobHeuristicDetailsDTO>();
    
    public JobHeuristicDTO(){
    }
    
    public JobHeuristicDTO(int numberOfResults, String message, String estimatedTime, String projectId, String jobName, String jobType){
        this.numberOfResults = numberOfResults;
        this.message = message;  
        this.estimatedTime = estimatedTime;
        this.projectId = projectId;
        this.jobName = jobName;
        this.jobType = jobType;
    }
    
    public JobHeuristicDTO(int numberOfResults, String message, String estimatedTime, String degreeOfSimilarity, String inputBlocks){
        this.numberOfResults = numberOfResults;
        this.message = message;  
        this.estimatedTime = estimatedTime;
        this.degreeOfSimilarity = degreeOfSimilarity;
        this.inputBlocks = inputBlocks;
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
    public String getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * @param estimatedTime the estimatedTime to set
     */
    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    /**
     * @return the inputBlocks
     */
    public String getInputBlocks() {
        return inputBlocks;
    }

    /**
     * @param inputBlocks the inputBlocks to set
     */
    public void setInputBlocks(String inputBlocks) {
        this.inputBlocks = inputBlocks;
    }

    /**
     * @return the similarAppIds
     */
    public List<String> getSimilarAppIds() {
        return similarAppIds;
    }

    /**
     * @param similarAppIds the similarAppIds to set
     */
    public void setSimilarAppIds(List<String> similarAppIds) {
        this.similarAppIds = similarAppIds;
    }
    
    public void addSimilarAppId(List<JobsHistory> jobsHistory){
        Iterator<JobsHistory> itr = jobsHistory.iterator();
        
        while(itr.hasNext()) {
         JobsHistory element = itr.next();
            getSimilarAppIds().add(element.getAppId());
      }
    }

    /**
     * @return the jobHeuristicDetails
     */
    public List<JobHeuristicDetailsDTO> getJobHeuristicDetails() {
        return jobHeuristicDetails;
    }

    /**
     * @param jobHeuristicDetails the jobHeuristicDetails to set
     */
    public void setJobHeuristicDetails(List<JobHeuristicDetailsDTO> jobHeuristicDetails) {
        this.jobHeuristicDetails = jobHeuristicDetails;
    }
    
    public void addJobHeuristicDetails(JobHeuristicDetailsDTO jhDetail){
        getJobHeuristicDetails().add(jhDetail);
    }

    /**
     * @return the projectId
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * @param projectId the projectId to set
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName the jobName to set
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    

    
}
